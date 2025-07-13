package com.zoirs.learn_en_word.repository;

import com.zoirs.learn_en_word.model.WordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import jakarta.persistence.QueryHint;
import java.util.List;
import java.util.Optional;

@Repository
public interface WordRepository extends JpaRepository<WordEntity, Long> {
    
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Optional<WordEntity> findByText(String text);
    
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    boolean existsByText(String text);
    
    @Query("SELECT w FROM WordEntity w WHERE LOWER(w.text) LIKE LOWER(concat('%', :search, '%'))")
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    List<WordEntity> findByTextContainingIgnoreCase(String search);
}
