package com.zoirs.learn_en_word.service;

import com.zoirs.learn_en_word.api.dto.skyeng.Meaning;
import com.zoirs.learn_en_word.api.service.SkyengDictionaryService;
import com.zoirs.learn_en_word.mapper.WordMapper;
import com.zoirs.learn_en_word.model.MeaningEntity;
import com.zoirs.learn_en_word.repository.MeaningRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
public class Autoloader2 {

    private static final Logger log = LoggerFactory.getLogger(Autoloader2.class);
    @Autowired
    private SkyengDictionaryService skyengDictionaryService;

    @Autowired
    private WordMapper wordMapper;

    @Autowired
    private MeaningRepository meaningRepository;

    boolean stopWorking = false;

    int errors = 0;
    int count = 20;

    int limit = 20;
    int offset = 0;


    @Scheduled(fixedDelay = 20_000) // 60000 milliseconds = 1 minute
    public void runEveryMinute() {
        if (offset < 270000L) {
            return;
        }
        List<Long> result = getMissingIds();

        String ids = result.stream().map(String::valueOf)
                .collect(Collectors.joining(","));

        log.info("Try load {}", ids);
        try {
            List<Meaning> meanings = skyengDictionaryService.getMeanings(ids);
            if (meanings.isEmpty()) {
                log.info("No new meanings found {}", ids);
                return;
            }
            boolean isSaved = saveMeaningsToCache(meanings);
            if (!isSaved) {
                count = count + 10;
            } else {
                count = 20;
            }
            errors = 0;
        } catch (Exception e) {
            errors++;
            log.error("Error loading meanings " + errors, e);
            sleep(errors * 120_000);
        }
    }

    private List<Long> getMissingIds() {
        List<Long> result = new ArrayList<>();
        while (result.isEmpty()) {
            log.info("Try get missing ids from {} {}", offset, limit);
            result = meaningRepository.findMissingIds(offset, offset + limit);
            offset += limit;
        }
        return result;
    }

    public static String sequence(long start, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(start + i);
        }
        return sb.toString();
    }

    public boolean saveMeaningsToCache(List<Meaning> meanings) {
        if (meanings == null || meanings.isEmpty()) {
            return false;
        }

        log.debug("Saving {} meanings to database cache", meanings.size());
        AtomicBoolean isSaved = new AtomicBoolean(false);
        meanings.stream()
                .filter(Objects::nonNull)
                .forEach(meaning -> {
                    // Check if meaning already exists
                    Optional<MeaningEntity> existingMeaning =
                            meaningRepository.findByExternalId(meaning.getId());
                    if (existingMeaning.isEmpty()) {
                        // Save new meaning
                        log.info("Load {} {}", meaning.getId(), meaning.getText());
                        MeaningEntity entity = wordMapper.toEntity(meaning, null);
                        entity.setAutoloaded(true);
                        meaningRepository.save(entity);
                        isSaved.set(true);
                    }
                });
        return isSaved.get();
    }

    private static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {

        }
    }
}
