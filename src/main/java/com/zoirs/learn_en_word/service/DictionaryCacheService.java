package com.zoirs.learn_en_word.service;

import com.zoirs.learn_en_word.api.dto.skyeng.Meaning;
import com.zoirs.learn_en_word.api.dto.skyeng.MeaningShort;
import com.zoirs.learn_en_word.api.dto.skyeng.MeaningWithSimilarTranslation;
import com.zoirs.learn_en_word.api.dto.skyeng.Word;
import com.zoirs.learn_en_word.api.service.SkyengDictionaryService;
import com.zoirs.learn_en_word.mapper.WordMapper;
import com.zoirs.learn_en_word.model.MeaningEntity;
import com.zoirs.learn_en_word.model.WordEntity;
import com.zoirs.learn_en_word.repository.WordRepository;
import com.zoirs.learn_en_word.repository.MeaningRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DictionaryCacheService {

    private final SkyengDictionaryService skyengDictionaryService;
    private final WordRepository wordRepository;
    private final MeaningRepository meaningRepository;
    private final WordMapper wordMapper;

    public List<Word> searchWords1(String search) {
        return skyengDictionaryService.searchWords(search);
    }


    @Cacheable(value = "wordSearchCache", key = "#search.toLowerCase()")
//   //  @Transactional(readOnly = true)
    public List<Meaning> searchWords(String search) {
        log.debug("Searching for words in cache or API: {}", search);

        // First try to find in local database
        List<MeaningEntity> cachedWords = meaningRepository.findByText(search);
        if (!cachedWords.isEmpty()) {
            log.debug("Found {} cached words matching: {}", cachedWords.size(), search);
            return wordMapper.toMeaningDtoList(cachedWords);
        }

        // If not found in cache, call the API
        log.debug("No cached results found for: {}. Calling Skyeng API...", search);
        List<Word> apiWords = skyengDictionaryService.searchWords(search);

        // Save to cache for future use
        if (!apiWords.isEmpty()) {
//            log.debug("Saving {} words to cache for search: {}", apiWords.size(), search);
            List<Word> matchedWords = apiWords.stream()
                    .filter(apiWord -> search.equals(apiWord.getText()))
                    .toList();
            if (!matchedWords.isEmpty()) {
                Optional<MeaningShort> meaningO = matchedWords.getFirst().getMeanings().stream().findFirst();
                if (meaningO.isPresent()) {
                    MeaningShort meaning = meaningO.get();
                    List<Meaning> meanings = skyengDictionaryService.getMeanings(String.valueOf(meaning.getId()));
                    saveMeaningsToCache(meanings);
                    for (Meaning m : meanings) {
                        if (m.getMeaningsWithSimilarTranslation() == null) {
                            continue;
                        }
                        m.getMeaningsWithSimilarTranslation().stream()
                                .filter(q -> m.getId().equals(q.getMeaningId()))
                                .map(MeaningWithSimilarTranslation::getFrequencyPercent)
                                .findFirst()
                                .ifPresent(s -> {
                                    try {
                                        int percent = (int) Double.parseDouble(s);
                                        m.setFrequencyPercent(percent);
                                    } catch (Exception e) {
                                        log.error("Cant parse FrequencyPercent " + s, e);
                                    }
                                });
                    }
                    return meanings;
                }
            }
        }
        return new ArrayList<>();
    }

    @Cacheable(value = "meaningCache", key = "#ids")
   //  @Transactional(readOnly = true)
    public List<Meaning> getMeanings(String ids) {
        log.debug("Getting meanings from cache or API for IDs: {}", ids);

        // Split the comma-separated IDs
        List<Integer> numbers = Arrays.stream(ids.split(","))
                .map(String::trim)
                .map(Integer::parseInt)
                .collect(Collectors.toList());

        // First try to find in local database
        List<MeaningEntity> cachedMeanings = meaningRepository.findByExternalIdIn(numbers);
        List<Integer> foundIds = cachedMeanings.stream()
                .map(MeaningEntity::getExternalId)
                .toList();

        // Check if all requested IDs were found in cache
        List<Integer> missingIds = numbers.stream()
                .filter(id -> !foundIds.contains(id))
                .collect(Collectors.toList());

        if (missingIds.isEmpty()) {
            log.debug("Found all {} meanings in cache", cachedMeanings.size());
            return wordMapper.toMeaningDtoList(cachedMeanings);
        }

        // If some meanings are missing, fetch them from the API
        log.debug("Found {} meanings in cache, missing {}. Calling Skyeng API...", 
                cachedMeanings.size(), missingIds.size());

        String missingIdsParam = missingIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        List<Meaning> apiMeanings =
                skyengDictionaryService.getMeanings(missingIdsParam);

        // Save the new meanings to cache
        if (!apiMeanings.isEmpty()) {
            log.debug("Saving {} new meanings to cache", apiMeanings.size());
            saveMeaningsToCache(apiMeanings);

            // Convert DTOs to entities for the response
            List<MeaningEntity> newMeanings = apiMeanings.stream()
                    .map(dto -> wordMapper.toEntity(dto, null))
                    .collect(Collectors.toList());

            // Combine cached and new meanings
            List<MeaningEntity> allMeanings = new ArrayList<>(cachedMeanings);
            allMeanings.addAll(newMeanings);

            return wordMapper.toMeaningDtoList(allMeanings);
        }

        return wordMapper.toMeaningDtoList(cachedMeanings);
    }

    @Transactional
    public void saveWordsToCache(List<Word> words) {
        if (words == null || words.isEmpty()) {
            return;
        }

        log.debug("Saving {} words to database cache", words.size());
        
        // Process words in batches to improve performance
        int batchSize = 50;
        for (int i = 0; i < words.size(); i += batchSize) {
            int end = Math.min(words.size(), i + batchSize);
            List<Word> batch = words.subList(i, end);
            processBatch(batch);
        }
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void processBatch(List<Word> batch) {
        for (Word word : batch) {
            try {
                WordEntity wordEntity = wordMapper.toEntity(word);
                if (wordEntity == null) continue;
                
                Optional<WordEntity> existingWord = wordRepository.findByText(wordEntity.getText());//todo by id

                if (existingWord.isPresent()) {
                    // Update existing word with retry logic
                    WordEntity existing = existingWord.get();
                    // Update only if the version hasn't changed
                    wordMapper.updateWordFromDto(wordMapper.toDto(wordEntity), existing);
                    wordRepository.saveAndFlush(existing);
                } else {
                    // Save new word
                    wordRepository.save(wordEntity);
                }
            } catch (Exception e) {
                log.warn("Failed to process word '{}': {}", word.getText(), e.getMessage());
                // Continue with next word in case of error
            }
        }
    }

    //  @Transactional
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
                        MeaningEntity entity = wordMapper.toEntity(meaning, null);
                        meaningRepository.save(entity);
                    }
                });
    }

    @CacheEvict(allEntries = true, cacheNames = {"wordSearchCache", "meaningCache"})
    public void clearAllCaches() {
        log.info("Cleared all caches");
    }

    public void searchWordsForUser(String word, Long userId) {

    }
}
