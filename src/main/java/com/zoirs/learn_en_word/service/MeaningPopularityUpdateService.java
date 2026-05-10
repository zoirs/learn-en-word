package com.zoirs.learn_en_word.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.zoirs.learn_en_word.model.MeaningEntity;
import com.zoirs.learn_en_word.repository.MeaningRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
public class MeaningPopularityUpdateService {

    private static final Logger log = LoggerFactory.getLogger(MeaningPopularityUpdateService.class);
    private static final String MEANINGS_ENDPOINT = "/api/v1/meanings";
    private static final int DEFAULT_WORD_ID_BATCH_SIZE = 100;

    private final RestTemplate restTemplate;
    private final MeaningRepository meaningRepository;
    private final AtomicBoolean startupUpdateStarted = new AtomicBoolean(false);

    @Value("${skyeng.gateway.base-url:https://dictionary-gateway.skyeng.ru}")
    private String gatewayBaseUrl;

    @Value("${skyeng.gateway.popularity-update.min-request-delay-ms:1000}")
    private long minRequestDelayMs;

    @Value("${skyeng.gateway.popularity-update.max-request-delay-ms:10000}")
    private long maxRequestDelayMs;

    @Value("${skyeng.gateway.popularity-update.error-delay-ms:120000}")
    private long errorDelayMs;

    @Scheduled(
            initialDelayString = "${skyeng.gateway.popularity-update.startup-delay-ms:30000}",
            fixedDelay = Long.MAX_VALUE
    )
    public void updateAllPopularitiesOnStartup() {
        if (!startupUpdateStarted.compareAndSet(false, true)) {
            return;
        }

        CompletableFuture.runAsync(this::runStartupUpdate);
    }

    private void runStartupUpdate() {
        log.info("Starting meaning popularity update");
        UpdateResult result = updateAllPopularities();
        log.info(
                "Finished meaning popularity update. processedWordIds={}, updatedMeanings={}, failedWordIds={}",
                result.getProcessedWordIds(),
                result.getUpdatedMeanings(),
                result.getFailedWordIds()
        );
    }

    public UpdateResult updateAllPopularities() {
        return updateAllPopularities(DEFAULT_WORD_ID_BATCH_SIZE);
    }

    private UpdateResult updateAllPopularities(int wordIdBatchSize) {
        int lastWordId = 0;
        int processedWordIds = 0;
        int updatedMeanings = 0;
        int failedWordIds = 0;

        while (true) {
            List<Integer> wordIds = meaningRepository.findDistinctWordIdsAfter(lastWordId, wordIdBatchSize);
            if (wordIds.isEmpty()) {
                break;
            }

            for (Integer wordId : wordIds) {
                lastWordId = wordId;
                processedWordIds++;

                try {
                    updatedMeanings += updatePopularitiesForWordId(wordId);
                } catch (Exception e) {
                    failedWordIds++;
                    log.error("Failed to update meaning popularity for wordId {}", wordId, e);
                    sleepAfterFailedRequest();
                } finally {
                    sleepBeforeNextRequest();
                }
            }
        }

        return new UpdateResult(processedWordIds, updatedMeanings, failedWordIds);
    }

    private void sleepBeforeNextRequest() {
        if (maxRequestDelayMs <= 0) {
            return;
        }

        long minDelayMs = Math.max(0, minRequestDelayMs);
        long maxDelayMs = Math.max(minDelayMs, maxRequestDelayMs);
        long requestDelayMs = ThreadLocalRandom.current().nextLong(minDelayMs, maxDelayMs + 1);

        try {
            Thread.sleep(requestDelayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Meaning popularity update was interrupted", e);
        }
    }

    private void sleepAfterFailedRequest() {
        if (errorDelayMs <= 0) {
            return;
        }

        try {
            Thread.sleep(errorDelayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Meaning popularity update was interrupted", e);
        }
    }

    private int updatePopularitiesForWordId(Integer wordId) {
        if (wordId == null) {
            return 0;
        }

        Map<Integer, Double> popularities = loadPopularities(wordId);
        if (popularities.isEmpty()) {
            log.info("No popularity data found for wordId {}", wordId);
            return 0;
        }

        List<MeaningEntity> meanings = meaningRepository.findByWordIdAndIsValidTrue(wordId);
        int updated = 0;

        for (MeaningEntity meaning : meanings) {
            Double popularity = popularities.get(meaning.getExternalId());
            if (popularity != null && !Objects.equals(meaning.getPopularity(), popularity)) {
                meaning.setPopularity(popularity);
                updated++;
            }
        }

        if (updated > 0) {
            meaningRepository.saveAll(meanings);
        }

        log.info("Updated popularity for wordId {}: {}", wordId, updated);
        return updated;
    }

    private Map<Integer, Double> loadPopularities(Integer wordId) {
        String url = UriComponentsBuilder.fromHttpUrl(gatewayBaseUrl + MEANINGS_ENDPOINT)
                .queryParam("wordId", wordId)
                .queryParam("subjectArea", "english")
                .toUriString();

        ResponseEntity<List<GatewayPartOfSpeechMeanings>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                }
        );
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("Skyeng gateway returned status " + response.getStatusCode());
        }

        List<GatewayPartOfSpeechMeanings> body = response.getBody() != null
                ? response.getBody()
                : Collections.emptyList();

        Map<Integer, Double> result = new HashMap<>();
        for (GatewayPartOfSpeechMeanings group : body) {
            if (group.meanings == null) {
                continue;
            }
            for (GatewayMeaning meaning : group.meanings) {
                if (meaning.id != null && meaning.popularity != null) {
                    result.put(meaning.id, meaning.popularity);
                }
            }
        }
        return result;
    }

    @lombok.Value
    public static class UpdateResult {
        int processedWordIds;
        int updatedMeanings;
        int failedWordIds;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GatewayPartOfSpeechMeanings {
        private List<GatewayMeaning> meanings;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GatewayMeaning {
        @JsonProperty("id")
        private Integer id;
        @JsonProperty("popularity")
        private Double popularity;
    }
}
