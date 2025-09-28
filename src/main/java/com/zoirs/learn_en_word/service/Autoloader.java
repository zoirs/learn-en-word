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

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class Autoloader {

    private static final Logger log = LoggerFactory.getLogger(Autoloader.class);
    @Autowired
    private SkyengDictionaryService skyengDictionaryService;

    @Autowired
    private WordMapper wordMapper;

    @Autowired
    private MeaningRepository meaningRepository;

    boolean stopWorking = false;

    @Scheduled(fixedDelay = 60_000) // 60000 milliseconds = 1 minute
    public void runEveryMinute() {
        if (stopWorking) {
            return;
        }
        Optional<Long> maxIdO = meaningRepository.findMaxExternalIdByAutoloadedTrue();
        long maxId;
        if (maxIdO.isPresent()) {
            maxId = maxIdO.get();
        } else {
            maxId = 0;
        }
        String ids = sequence(maxId, 20);

        log.info("Try load {}", ids);
        try {
            List<Meaning> meanings = skyengDictionaryService.getMeanings(ids);
            if (meanings.isEmpty()) {
                log.info("No new meanings found {}", ids);
                stopWorking = true;
                return;
            }
            saveMeaningsToCache(meanings);
        }catch (Exception e) {
            log.error("Error loading meanings", e);
            stopWorking = true;
        }
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

    public void saveMeaningsToCache(List<Meaning> meanings) {
        if (meanings == null || meanings.isEmpty()) {
            return;
        }

        log.debug("Saving {} meanings to database cache", meanings.size());

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
                    }
                });
    }
}
