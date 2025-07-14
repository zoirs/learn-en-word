package com.zoirs.learn_en_word.repository;

import com.zoirs.learn_en_word.entity.Word;
import com.zoirs.learn_en_word.model.WordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import jakarta.persistence.QueryHint;
import java.util.List;
import java.util.Optional;

@Repository
public interface WordRepository extends JpaRepository<Word, Long> {
    
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Optional<WordEntity> findByEnglishWord(String englishWord);
    
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    boolean existsByEnglishWord(String englishWord);
    
    @Query("SELECT w FROM Word w WHERE w.id NOT IN (SELECT uw.word.id FROM UserWord uw WHERE uw.user.id = :userId)")
    List<WordEntity> findNewWordsForUser(Long userId);
    
    @Query("SELECT w FROM WordEntity w WHERE LOWER(w.text) LIKE LOWER(concat('%', :search, '%'))")
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    List<WordEntity> findByTextContainingIgnoreCase(String search);
}
