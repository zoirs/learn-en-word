package com.zoirs.learn_en_word.service;

import com.zoirs.learn_en_word.model.MeaningEntity;
import com.zoirs.learn_en_word.repository.MeaningRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatabaseWordSuggestionServiceTest {

    private DatabaseWordSuggestionService databaseWordSuggestionService;

    @Mock
    private MeaningRepository meaningRepository;

    @BeforeEach
    void setUp() {
        databaseWordSuggestionService = new DatabaseWordSuggestionService(meaningRepository);
    }

    @Test
    void suggestNewWords_SelectsWordsByConfiguredLimits() {
        when(meaningRepository.findByExternalIdIn(anyList()))
                .thenReturn(List.of(
                        meaning(1, "known", 2),
                        meaning(2, "learning-one", 3),
                        meaning(3, "learning-two", 3)
                ));
        when(meaningRepository.findSuggestionsByDifficultyLevel(eq(2), anySet(), anySet(), anyDouble(), eq(20), eq(2)))
                .thenReturn(List.of(meaning(10, "easy-one", 2), meaning(11, "easy-two", 2)));
        when(meaningRepository.findSuggestionsByDifficultyLevel(eq(3), anySet(), anySet(), anyDouble(), eq(20), eq(3)))
                .thenReturn(List.of(meaning(12, "same-one", 3), meaning(13, "same-two", 3)));
        when(meaningRepository.findSuggestionsByDifficultyLevel(eq(4), anySet(), anySet(), anyDouble(), eq(20), eq(4)))
                .thenReturn(List.of(meaning(14, "hard-one", 4), meaning(15, "hard-two", 4), meaning(16, "hard-three", 4), meaning(17, "hard-four", 4)));

        Set<Integer> result = databaseWordSuggestionService.suggestNewWords(Set.of(1), Set.of(2, 3));

        assertEquals(Set.of(10, 11, 12, 13, 14, 15, 16, 17), result);
    }

    @Test
    void suggestNewWords_WithEmptyInput_ReturnsEmptySetWithoutRepositoryCalls() {
        Set<Integer> result = databaseWordSuggestionService.suggestNewWords(Set.of(), Set.of());

        assertTrue(result.isEmpty());
        verifyNoInteractions(meaningRepository);
    }

    @Test
    void suggestNewWords_WhenLearningLevelsMissing_FallsBackToAllKnownLevels() {
        when(meaningRepository.findByExternalIdIn(anyList()))
                .thenReturn(List.of(
                        meaning(1, "known-one", 2),
                        meaning(2, "known-two", 2),
                        meaning(3, "learning", null)
                ));
        when(meaningRepository.findSuggestionsByDifficultyLevel(eq(1), anySet(), anySet(), anyDouble(), eq(20), eq(2)))
                .thenReturn(List.of(meaning(10, "easy", 1)));
        when(meaningRepository.findSuggestionsByDifficultyLevel(eq(2), anySet(), anySet(), anyDouble(), eq(20), eq(3)))
                .thenReturn(List.of(meaning(11, "same", 2)));
        when(meaningRepository.findSuggestionsByDifficultyLevel(eq(3), anySet(), anySet(), anyDouble(), eq(20), eq(4)))
                .thenReturn(List.of(meaning(12, "hard", 3)));

        Set<Integer> result = databaseWordSuggestionService.suggestNewWords(Set.of(1, 2), Set.of(3));

        assertEquals(Set.of(10, 11, 12), result);
    }

    @Test
    void suggestNewWords_NormalizesRepositoryResults() {
        when(meaningRepository.findByExternalIdIn(anyList()))
                .thenReturn(List.of(meaning(1, "known", 2)));
        when(meaningRepository.findSuggestionsByDifficultyLevel(eq(1), anySet(), anySet(), anyDouble(), eq(20), eq(2)))
                .thenReturn(List.of());
        when(meaningRepository.findSuggestionsByDifficultyLevel(eq(2), anySet(), anySet(), anyDouble(), eq(20), eq(3)))
                .thenReturn(List.of(
                        meaning(10, " useful ", 2),
                        meaning(11, "", 2),
                        meaning(12, "known", 2)
                ));
        when(meaningRepository.findSuggestionsByDifficultyLevel(eq(3), anySet(), anySet(), anyDouble(), eq(20), eq(4)))
                .thenReturn(List.of());

        Set<Integer> result = databaseWordSuggestionService.suggestNewWords(Set.of(1), Set.of());

        assertEquals(Set.of(10), result);
    }

    @Test
    void suggestNewWords_FiltersCurrentMeaningsAndAddsRequestedPhrases() {
        when(meaningRepository.findByExternalIdIn(anyList()))
                .thenReturn(List.of(
                        meaning(1, "known", 2, "n"),
                        meaning(2, "learn", 2, "v"),
                        meaning(3, "ignored phrase", 5, "ph")
                ));
        when(meaningRepository.findPhrasesForLearningWords(eq(Set.of(2)), eq(Set.of(1, 2, 3)), eq(Set.of("ph", "phi"))))
                .thenReturn(List.of(meaning(20, "learn by heart", 2, "ph")));
        when(meaningRepository.findSuggestionsByDifficultyLevel(eq(1), anySet(), anySet(), anyDouble(), eq(20), eq(2)))
                .thenReturn(List.of());
        when(meaningRepository.findSuggestionsByDifficultyLevel(eq(2), anySet(), anySet(), anyDouble(), eq(20), eq(3)))
                .thenReturn(List.of());
        when(meaningRepository.findSuggestionsByDifficultyLevel(eq(3), anySet(), anySet(), anyDouble(), eq(20), eq(4)))
                .thenReturn(List.of());

        Set<Integer> result = databaseWordSuggestionService.suggestNewWords(
                Set.of(1), Set.of(2, 3), Set.of("ph", "phi"));

        assertEquals(Set.of(20), result);
        verify(meaningRepository).findPhrasesForLearningWords(
                Set.of(2), Set.of(1, 2, 3), Set.of("ph", "phi"));
    }

    private MeaningEntity meaning(Integer externalId, String text, Integer difficultyLevel) {
        return meaning(externalId, text, difficultyLevel, "n");
    }

    private MeaningEntity meaning(Integer externalId, String text, Integer difficultyLevel, String partOfSpeechCode) {
        MeaningEntity meaning = meaning(externalId, text, difficultyLevel, 0.000001d, 3.0d, 0.000001d);
        meaning.setPartOfSpeechCode(partOfSpeechCode);
        return meaning;
    }

    private MeaningEntity meaning(
            Integer externalId,
            String text,
            Integer difficultyLevel,
            Double wordfreqFrequency,
            Double wordfreqZipf,
            Double wordfreqMinFrequency
    ) {
        MeaningEntity meaning = new MeaningEntity();
        meaning.setExternalId(externalId);
        meaning.setText(text);
        meaning.setDifficultyLevel(difficultyLevel);
        meaning.setPartOfSpeechCode("n");
        meaning.setWordfreqFrequency(wordfreqFrequency);
        meaning.setWordfreqZipf(wordfreqZipf);
        meaning.setWordfreqMinFrequency(wordfreqMinFrequency);
        return meaning;
    }
}
