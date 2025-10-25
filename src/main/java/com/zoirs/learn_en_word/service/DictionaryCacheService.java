package com.zoirs.learn_en_word.service;

import com.zoirs.learn_en_word.api.dto.skyeng.Meaning;
import com.zoirs.learn_en_word.api.dto.skyeng.Word;
import com.zoirs.learn_en_word.api.service.SkyengDictionaryService;
import com.zoirs.learn_en_word.mapper.WordMapper;
import com.zoirs.learn_en_word.model.MeaningEntity;
import com.zoirs.learn_en_word.repository.MeaningRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DictionaryCacheService {

    private final SkyengDictionaryService skyengDictionaryService;
    private final MeaningRepository meaningRepository;
    private final WordMapper wordMapper;

    public List<Word> searchWords1(String search) {
        return skyengDictionaryService.searchWords(search);
    }

    public List<Meaning> searchWords(Set<String> search) {
        log.debug("Searching for words in cache or API: {}", search);

        List<MeaningEntity> cachedWords = meaningRepository.findByTextIn(search);
        if (!cachedWords.isEmpty()) {
            if (cachedWords.size() != search.size()) {
                log.error("Cached results size {} does not match search size {}", cachedWords.size(), search.size());
                search.stream()
                        .filter(q -> cachedWords.stream().noneMatch(w -> w.getText().equals(q)))
                        .forEach(q -> log.error("Word {} not found in cache", q));
            }
            return wordMapper.toMeaningDtoList(cachedWords);
        }
        log.error("No cached results found for: {}", search);
        return new ArrayList<>();
    }

    //  @Transactional(readOnly = true)
    public List<Meaning> getMeanings(String ids) {
        log.debug("Getting meanings from cache or API for IDs: {}", ids);

        // Split the comma-separated IDs
        List<Integer> numbers = Arrays.stream(ids.split(","))
                .map(String::trim)
                .map(Integer::parseInt)
                .collect(Collectors.toList());

        // First try to find in local database
        return getMeanings(numbers);
    }

    public List<Meaning> getMeanings(List<Integer> numbers) {
        List<MeaningEntity> cachedMeanings = meaningRepository.findByExternalIdIn(numbers);

        if (cachedMeanings.size() != numbers.size()) {
            log.error("Cached results for ids size {} does not match search size {}", cachedMeanings.size(), numbers.size());
            numbers.stream()
                    .filter(q -> cachedMeanings.stream().noneMatch(w -> w.getExternalId().equals(q)))
                    .forEach(q -> log.error("Word id {} not found in cache", q));
        }
        return wordMapper.toMeaningDtoList(cachedMeanings);
    }
}
