package com.zoirs.learn_en_word.repository;

import com.zoirs.learn_en_word.model.MeaningEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
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
    
    @Query("SELECT m FROM MeaningEntity m WHERE m.externalId IN :externalIds")
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    List<MeaningEntity> findByExternalIdIn(List<String> externalIds);
    
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    boolean existsByExternalId(Integer externalId);
}
