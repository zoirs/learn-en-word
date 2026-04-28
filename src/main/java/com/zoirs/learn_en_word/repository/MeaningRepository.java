package com.zoirs.learn_en_word.repository;

import com.zoirs.learn_en_word.model.MeaningEntity;
import org.springframework.data.domain.Pageable;
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

    @Query("""
       SELECT m
       FROM MeaningEntity m
       WHERE m.text = :text AND m.frequencyPercent IS NOT NULL
       ORDER BY m.frequencyPercent DESC
       """)
    List<MeaningEntity> findByText(String text);

    @Query(value = """
            SELECT DISTINCT ON (m.text) m.*
            FROM meanings m
            WHERE m.text IN (:texts)
              AND m.frequency_percent IS NOT NULL
              AND m.frequency_percent = (
                  SELECT MAX(m2.frequency_percent)
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
            SELECT *
            FROM (
                SELECT DISTINCT ON (m.text) m.*
                FROM meanings m
                WHERE m.difficulty_level = :difficultyLevel
                  AND m.external_id NOT IN (:excludedExternalIds)
                  AND LOWER(m.text) NOT IN (:excludedTexts)
                  AND m.text IS NOT NULL
                  AND COALESCE(m.part_of_speech_code, '') <> 'ph'
                  AND m.frequency_percent IS NOT NULL
                  AND COALESCE(m.is_valid, true) = true
                  AND EXISTS (
                      SELECT 1
                      FROM examples e
                      WHERE e.meaning_id = m.id
                        AND e.text IS NOT NULL
                        AND BTRIM(e.text) <> ''
                  )
                ORDER BY m.text, m.frequency_percent DESC, RANDOM()
            ) suggested_meanings
            ORDER BY RANDOM()
            LIMIT :limit
            """, nativeQuery = true)
    List<MeaningEntity> findSuggestionsByDifficultyLevel(
            @Param("difficultyLevel") int difficultyLevel,
            @Param("excludedExternalIds") Set<Integer> excludedExternalIds,
            @Param("excludedTexts") Set<String> excludedTexts,
            @Param("limit") int limit
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
