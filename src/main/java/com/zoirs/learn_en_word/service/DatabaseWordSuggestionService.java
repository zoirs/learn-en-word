package com.zoirs.learn_en_word.service;

import com.zoirs.learn_en_word.model.MeaningEntity;
import com.zoirs.learn_en_word.repository.MeaningRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class DatabaseWordSuggestionService {

    private static final int WORDS_PER_LEVEL = 2;

    private final MeaningRepository meaningRepository;

    public Set<String> suggestNewWords(Set<Integer> knownWords, Set<Integer> learningWords) {
        Set<Integer> knownWordIds = normalizeIds(knownWords);
        Set<Integer> learningWordIds = normalizeIds(learningWords);
        Set<Integer> excludedExternalIds = Stream.concat(knownWordIds.stream(), learningWordIds.stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (excludedExternalIds.isEmpty()) {
            log.info("Skipping database word suggestions because known and learning word ids are empty");
            return Collections.emptySet();
        }

        List<MeaningEntity> currentMeanings = meaningRepository.findByExternalIdIn(new ArrayList<>(excludedExternalIds));
        Integer currentLevel = calculateCurrentLevel(currentMeanings, learningWordIds);
        if (currentLevel == null) {
            log.info("Skipping database word suggestions because current difficulty level cannot be calculated");
            return Collections.emptySet();
        }

        Set<String> excludedTexts = currentMeanings.stream()
                .map(MeaningEntity::getText)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(text -> !text.isBlank())
                .map(text -> text.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        Set<String> easier = findWordsByLevel(currentLevel - 1, excludedExternalIds, excludedTexts);
        Set<String> same = findWordsByLevel(currentLevel, excludedExternalIds, excludedTexts);
        Set<String> harder = findWordsByLevel(currentLevel + 1, excludedExternalIds, excludedTexts);

        Set<String> suggestions = new LinkedHashSet<>();
        suggestions.addAll(easier);
        suggestions.addAll(same);
        suggestions.addAll(harder);
        log.info("Database word suggestions: level={}, easier={}, same={}, harder={}",
                currentLevel, easier, same, harder);
        return suggestions;
    }

    private Integer calculateCurrentLevel(List<MeaningEntity> meanings, Set<Integer> learningWordIds) {
        List<Integer> learningLevels = meanings.stream()
                .filter(meaning -> learningWordIds.contains(meaning.getExternalId()))
                .map(MeaningEntity::getDifficultyLevel)
                .filter(Objects::nonNull)
                .toList();
        if (!learningLevels.isEmpty()) {
            return roundedAverage(learningLevels);
        }

        List<Integer> allLevels = meanings.stream()
                .map(MeaningEntity::getDifficultyLevel)
                .filter(Objects::nonNull)
                .toList();
        if (allLevels.isEmpty()) {
            return null;
        }
        return roundedAverage(allLevels);
    }

    private int roundedAverage(List<Integer> levels) {
        return (int) Math.round(levels.stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElseThrow());
    }

    private Set<String> findWordsByLevel(int difficultyLevel, Set<Integer> excludedExternalIds, Set<String> excludedTexts) {
        if (difficultyLevel <= 0) {
            return Collections.emptySet();
        }
        return meaningRepository.findSuggestionsByDifficultyLevel(
                        difficultyLevel,
                        excludedExternalIds,
                        excludedTexts.isEmpty() ? Set.of("__no_excluded_text__") : excludedTexts,
                        WORDS_PER_LEVEL
                ).stream()
                .map(MeaningEntity::getText)
                .filter(Objects::nonNull)
                .filter(word -> !word.isBlank())
                .filter(word -> !excludedTexts.contains(word.toLowerCase(Locale.ROOT)))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<Integer> normalizeIds(Set<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptySet();
        }
        return ids.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
