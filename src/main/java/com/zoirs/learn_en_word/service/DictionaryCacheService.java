package com.zoirs.learn_en_word.service;

import com.zoirs.learn_en_word.api.dto.skyeng.Meaning;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DictionaryCacheService {

    private final SkyengDictionaryService skyengDictionaryService;
    private final WordRepository wordRepository;
    private final MeaningRepository meaningRepository;
    private final WordMapper wordMapper;

    @Cacheable(value = "wordSearchCache", key = "#search.toLowerCase()")
//   //  @Transactional(readOnly = true)
    public List<Word> searchWords(String search) {
        log.debug("Searching for words in cache or API: {}", search);

        // First try to find in local database
        List<WordEntity> cachedWords = wordRepository.findByTextContainingIgnoreCase(search);
        if (!cachedWords.isEmpty()) {
            log.debug("Found {} cached words matching: {}", cachedWords.size(), search);
            return wordMapper.toDtoList(cachedWords);
        }

        // If not found in cache, call the API
        log.debug("No cached results found for: {}. Calling Skyeng API...", search);
        List<Word> apiWords = skyengDictionaryService.searchWords(search);

        // Save to cache for future use
        if (!apiWords.isEmpty()) {
            log.debug("Saving {} words to cache for search: {}", apiWords.size(), search);
            saveWordsToCache(apiWords);
        }

        return apiWords;
    }

    @Cacheable(value = "meaningCache", key = "#ids")
   //  @Transactional(readOnly = true)
    public List<Meaning> getMeanings(String ids) {
        log.debug("Getting meanings from cache or API for IDs: {}", ids);

        // Split the comma-separated IDs
        List<String> idList = List.of(ids.split(","));

        // First try to find in local database
        List<MeaningEntity> cachedMeanings = meaningRepository.findByExternalIdIn(idList);
        List<String> foundIds = cachedMeanings.stream()
                .map(MeaningEntity::getExternalId)
                .map(String::valueOf)
                .toList();

        // Check if all requested IDs were found in cache
        List<String> missingIds = idList.stream()
                .filter(id -> !foundIds.contains(id))
                .collect(Collectors.toList());

        if (missingIds.isEmpty()) {
            log.debug("Found all {} meanings in cache", cachedMeanings.size());
            return wordMapper.toMeaningDtoList(cachedMeanings);
        }

        // If some meanings are missing, fetch them from the API
        log.debug("Found {} meanings in cache, missing {}. Calling Skyeng API...", 
                cachedMeanings.size(), missingIds.size());

        String missingIdsParam = String.join(",", missingIds);
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
                .map(dto -> wordMapper.toEntity(dto, null))
                .filter(Objects::nonNull)
                .forEach(meaning -> {
                    // Check if meaning already exists
                    Optional<MeaningEntity> existingMeaning = 
                            meaningRepository.findByExternalId(meaning.getExternalId());
                    if (existingMeaning.isPresent()) {
                        // Update existing meaning
                        MeaningEntity meaningToUpdate = existingMeaning.get();
                        wordMapper.updateMeaningFromDto(
                                wordMapper.toDto(meaning),
                                meaningToUpdate
                        );
                        meaningRepository.save(meaningToUpdate);
                    } else {
                        // Save new meaning
                        meaningRepository.save(meaning);
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
