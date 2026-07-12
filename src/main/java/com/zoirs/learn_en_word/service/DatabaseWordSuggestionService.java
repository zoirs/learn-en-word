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

    private static final int MAX_TEXT_LENGTH_EXCLUSIVE = 20;
    private static final double MIN_POPULARITY = 1d;
    private static final Set<String> SEARCHABLE_PART_OF_SPEECH_CODES = Set.of("j", "n", "r", "v");
    private static final Set<String> PHRASE_PART_OF_SPEECH_CODES = Set.of("ph", "phi");

    private final MeaningRepository meaningRepository;

    public Set<Integer> suggestNewWords(Set<Integer> knownWords, Set<Integer> learningWords) {
        return suggestNewWords(knownWords, learningWords, SEARCHABLE_PART_OF_SPEECH_CODES);
    }

    public Set<Integer> suggestNewWords(
            Set<Integer> knownWords,
            Set<Integer> learningWords,
            Set<String> requestedPartOfSpeechCodes
    ) {
        Set<Integer> knownWordIds = normalizeIds(knownWords);
        Set<Integer> learningWordIds = normalizeIds(learningWords);
        Set<Integer> excludedExternalIds = Stream.concat(knownWordIds.stream(), learningWordIds.stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (excludedExternalIds.isEmpty()) {
            log.info("Skipping database word suggestions because known and learning word ids are empty");
            return Collections.emptySet();
        }

        List<MeaningEntity> allCurrentMeanings = meaningRepository.findByExternalIdIn(new ArrayList<>(excludedExternalIds));
        List<MeaningEntity> currentMeanings = allCurrentMeanings.stream()
                .filter(meaning -> SEARCHABLE_PART_OF_SPEECH_CODES.contains(meaning.getPartOfSpeechCode()))
                .toList();
        Set<Integer> searchableLearningWordIds = currentMeanings.stream()
                .filter(meaning -> learningWordIds.contains(meaning.getExternalId()))
                .map(MeaningEntity::getExternalId)
                .collect(Collectors.toSet());
        Set<Integer> suggestions = new LinkedHashSet<>();
        Set<String> requestedPhraseCodes = normalizePartOfSpeechCodes(requestedPartOfSpeechCodes).stream()
                .filter(PHRASE_PART_OF_SPEECH_CODES::contains)
                .collect(Collectors.toSet());

        if (!requestedPhraseCodes.isEmpty() && !searchableLearningWordIds.isEmpty()) {
            List<MeaningEntity> phrases = meaningRepository.findPhrasesForLearningWords(
                    searchableLearningWordIds, excludedExternalIds, requestedPhraseCodes);
            suggestions.addAll(toExternalIds(phrases));
            log.info("Database phrase suggestions: {}", toLogWords(phrases));
        }

        Integer currentLevel = calculateCurrentLevel(currentMeanings, learningWordIds);
        if (currentLevel == null) {
            log.info("Skipping database word suggestions because current difficulty level cannot be calculated");
            return suggestions;
        }
        WordSuggestionLimits.GroupLimits limits = WordSuggestionLimits
                .forLearningWordsCount(searchableLearningWordIds.size())
                .database();

        Set<String> excludedTexts = currentMeanings.stream()
                .map(MeaningEntity::getText)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(text -> !text.isBlank())
                .map(text -> text.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        List<MeaningEntity> easier = findWordsByLevel(currentLevel - 1, excludedExternalIds, excludedTexts, limits.easier());
        List<MeaningEntity> same = findWordsByLevel(currentLevel, excludedExternalIds, excludedTexts, limits.same());
        List<MeaningEntity> harder = findWordsByLevel(currentLevel + 1, excludedExternalIds, excludedTexts, limits.harder());

        suggestions.addAll(toExternalIds(easier));
        suggestions.addAll(toExternalIds(same));
        suggestions.addAll(toExternalIds(harder));
        log.info("Database word suggestions: level={}, easier={}, same={}, harder={}",
                currentLevel, toLogWords(easier), toLogWords(same), toLogWords(harder));
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

    private List<MeaningEntity> findWordsByLevel(
            int difficultyLevel,
            Set<Integer> excludedExternalIds,
            Set<String> excludedTexts,
            int limit
    ) {
        if (difficultyLevel <= 0 || limit <= 0) {
            return Collections.emptyList();
        }
//        log.info("Finding words by level: difficultyLevel={}, excludedExternalIds={}, excludedTexts={}",
//                difficultyLevel, excludedExternalIds, excludedTexts);
        return meaningRepository.findSuggestionsByDifficultyLevel(
                        difficultyLevel,
                        excludedExternalIds,
                        excludedTexts.isEmpty() ? Set.of("__no_excluded_text__") : excludedTexts,
                        MIN_POPULARITY,
                        MAX_TEXT_LENGTH_EXCLUSIVE,
                        limit
                ).stream()
                .filter(meaning -> meaning.getText() != null)
                .filter(meaning -> !meaning.getText().trim().isBlank())
                .filter(meaning -> !excludedTexts.contains(meaning.getText().trim().toLowerCase(Locale.ROOT)))
                .toList();
    }

    private List<Integer> toExternalIds(List<MeaningEntity> meanings) {
        return meanings.stream()
                .map(MeaningEntity::getExternalId)
                .filter(Objects::nonNull)
                .toList();
    }

    private List<String> toLogWords(List<MeaningEntity> meanings) {
        return meanings.stream()
                .map(meaning -> "%s(externalId=%s, id=%s, level=%s)".formatted(
                        meaning.getText(),
                        meaning.getExternalId(),
                        meaning.getId(),
                        meaning.getDifficultyLevel()
                ))
                .filter(Objects::nonNull)
                .toList();
    }

    private Set<Integer> normalizeIds(Set<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptySet();
        }
        return ids.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> normalizePartOfSpeechCodes(Set<String> partOfSpeechCodes) {
        if (partOfSpeechCodes == null) {
            return SEARCHABLE_PART_OF_SPEECH_CODES;
        }
        return partOfSpeechCodes.stream()
                .filter(Objects::nonNull)
                .map(code -> code.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }
}
