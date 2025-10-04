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

@Repository
public interface MeaningRepository extends JpaRepository<MeaningEntity, Long> {
    
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

    @Query("SELECT m FROM MeaningEntity m WHERE m.externalId IN :externalIds")
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
//    @EntityGraph(attributePaths = "exampleEntities")
    List<MeaningEntity> findByExternalIdIn(List<String> externalIds);
    
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
