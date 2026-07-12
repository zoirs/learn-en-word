package com.zoirs.learn_en_word.repository;

import com.zoirs.learn_en_word.model.MeaningEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.QueryHint;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface MeaningRepository extends JpaRepository<MeaningEntity, Integer> {
    
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Optional<MeaningEntity> findByExternalId(Integer externalId);
    
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    List<MeaningEntity> findByWordId(Integer wordId);

    @Query(value = """
            SELECT DISTINCT word_id
            FROM meanings
            WHERE word_id IS NOT NULL
              AND word_id > :lastWordId
            ORDER BY word_id
            LIMIT :limit
            """, nativeQuery = true)
    List<Integer> findDistinctWordIdsAfter(@Param("lastWordId") int lastWordId,
                                           @Param("limit") int limit);

    @Query("""
       SELECT m
       FROM MeaningEntity m
       WHERE m.text = :text AND m.popularity IS NOT NULL
       ORDER BY m.popularity DESC
       """)
    List<MeaningEntity> findByText(String text);

    @Query(value = """
            SELECT DISTINCT ON (m.text) m.*
            FROM meanings m
            WHERE m.text IN (:texts)
              AND m.popularity IS NOT NULL
              AND m.popularity = (
                  SELECT MAX(m2.popularity)
                  FROM meanings m2
                  WHERE m2.text = m.text
              )
            ORDER BY m.text, RANDOM()
            """, nativeQuery = true)
    List<MeaningEntity> findByTextIn(Set<String> texts);

    @Query("SELECT m FROM MeaningEntity m WHERE m.externalId IN :externalIds")
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
//    @EntityGraph(attributePaths = "exampleEntities")
    List<MeaningEntity> findByExternalIdIn(List<Integer> externalIds);

    @Query(value = """
            WITH candidate_meanings AS (
                SELECT m.id, m.word_id, m.difficulty_level, m.text, m.popularity
                FROM meanings m
                WHERE m.external_id NOT IN (:excludedExternalIds)
                  AND LOWER(m.text) NOT IN (:excludedTexts)
                  AND m.text IS NOT NULL
                  AND LENGTH(BTRIM(m.text)) < :maxTextLengthExclusive
                  AND m.part_of_speech_code IN ('j', 'n', 'r', 'v')
                  AND m.popularity IS NOT NULL
                  AND m.wordfreq_frequency > 0
                  AND m.popularity >= :minPopularity
                  AND EXISTS (
                      SELECT 1
                      FROM examples e
                      WHERE e.meaning_id = m.id
                        AND e.text IS NOT NULL
                        AND BTRIM(e.text) <> ''
                  )
            ),
            best_word_meaning_ids AS (
                SELECT id
                FROM (
                    SELECT cm.id,
                           cm.difficulty_level,
                           ROW_NUMBER() OVER (
                               PARTITION BY cm.word_id
                               ORDER BY cm.popularity DESC, RANDOM()
                   ) AS word_rank
                    FROM candidate_meanings cm
                ) ranked_word_meanings
                WHERE word_rank = 1
                  AND difficulty_level = :difficultyLevel
            )
            SELECT m.*
            FROM meanings m
            JOIN best_word_meaning_ids bwm ON bwm.id = m.id
            ORDER BY RANDOM()
            LIMIT :limit
            """, nativeQuery = true)
    List<MeaningEntity> findSuggestionsByDifficultyLevel(
            @Param("difficultyLevel") int difficultyLevel,
            @Param("excludedExternalIds") Set<Integer> excludedExternalIds,
            @Param("excludedTexts") Set<String> excludedTexts,
            @Param("minPopularity") double minPopularity,
            @Param("maxTextLengthExclusive") int maxTextLengthExclusive,
            @Param("limit") int limit
    );

    @Query(value = """
            WITH learning_words AS (
                SELECT DISTINCT LOWER(BTRIM(m.text)) AS word
                FROM meanings m
                WHERE m.external_id IN (:learningExternalIds)
                  AND m.part_of_speech_code IN ('j', 'n', 'r', 'v')
                  AND m.text IS NOT NULL
                  AND BTRIM(m.text) <> ''
            ),
            phrase_candidates AS (
                SELECT m.external_id,
                       LOWER(m.text) AS normalized_text,
                       CASE
                           WHEN JSONB_TYPEOF(m.wordfreq_tokens) = 'array'
                           THEN JSONB_ARRAY_LENGTH(m.wordfreq_tokens)
                       END AS token_count,
                       m.popularity
                FROM meanings m
                WHERE m.external_id IS NOT NULL
                  AND m.external_id NOT IN (:excludedExternalIds)
                  AND m.part_of_speech_code IN (:phrasePartOfSpeechCodes)
                  AND JSONB_TYPEOF(m.wordfreq_tokens) = 'array'
            ),
            phrases_by_word AS (
                SELECT first_phrase.external_id AS first_phrase_id,
                       second_phrase.external_id AS second_phrase_id,
                       third_phrase.external_id AS third_phrase_id
                FROM learning_words lw
                LEFT JOIN LATERAL (
                    SELECT pc.external_id
                    FROM phrase_candidates pc
                    WHERE POSITION(lw.word IN pc.normalized_text) > 0
                      AND pc.token_count BETWEEN 2 AND 3
                    ORDER BY pc.popularity DESC NULLS LAST, RANDOM()
                    LIMIT 1
                ) first_phrase ON TRUE
                LEFT JOIN LATERAL (
                    SELECT pc.external_id
                    FROM phrase_candidates pc
                    WHERE POSITION(lw.word IN pc.normalized_text) > 0
                      AND pc.token_count BETWEEN 3 AND 4
                      AND (first_phrase.external_id IS NULL OR pc.external_id <> first_phrase.external_id)
                    ORDER BY pc.popularity DESC NULLS LAST, RANDOM()
                    LIMIT 1
                ) second_phrase ON TRUE
                LEFT JOIN LATERAL (
                    SELECT pc.external_id
                    FROM phrase_candidates pc
                    WHERE POSITION(lw.word IN pc.normalized_text) > 0
                      AND pc.token_count BETWEEN 5 AND 6
                      AND (first_phrase.external_id IS NULL OR pc.external_id <> first_phrase.external_id)
                      AND (second_phrase.external_id IS NULL OR pc.external_id <> second_phrase.external_id)
                    ORDER BY pc.popularity DESC NULLS LAST, RANDOM()
                    LIMIT 1
                ) third_phrase ON TRUE
            ),
            phrase_ids AS (
                SELECT DISTINCT phrase_id AS external_id
                FROM phrases_by_word
                CROSS JOIN LATERAL (VALUES
                    (first_phrase_id),
                    (second_phrase_id),
                    (third_phrase_id)
                ) selected_phrases(phrase_id)
                WHERE phrase_id IS NOT NULL
            )
            SELECT m.*
            FROM meanings m
            WHERE m.external_id IN (SELECT external_id FROM phrase_ids)
            """, nativeQuery = true)
    List<MeaningEntity> findPhrasesForLearningWords(
            @Param("learningExternalIds") Set<Integer> learningExternalIds,
            @Param("excludedExternalIds") Set<Integer> excludedExternalIds,
            @Param("phrasePartOfSpeechCodes") Set<String> phrasePartOfSpeechCodes
    );
    
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    boolean existsByExternalId(Integer externalId);
    
    @Query("SELECT MAX(m.externalId) FROM MeaningEntity m WHERE m.autoloaded = true")
    Optional<Long> findMaxExternalIdByAutoloadedTrue();

    @Query(value = """
    SELECT g.id
    FROM generate_series(:fromId, :toId) g(id)
    LEFT JOIN meanings e ON e.external_id = g.id
    WHERE e.external_id IS NULL
    ORDER BY g.id
    """, nativeQuery = true)
    List<Long> findMissingIds(@Param("fromId") long fromId,
                              @Param("toId") long toId);
}
