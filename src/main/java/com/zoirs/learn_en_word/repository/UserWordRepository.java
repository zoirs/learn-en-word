package com.zoirs.learn_en_word.repository;

import com.zoirs.learn_en_word.entity.UserWord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserWordRepository extends JpaRepository<UserWord, Long> {
    
    @Query("SELECT uw FROM UserWord uw " +
           "WHERE uw.user.id = :userId " +
           "AND uw.status = 'LEARNING' " +
           "AND uw.nextReviewDate <= :currentDate " +
           "ORDER BY uw.learningStage, uw.nextReviewDate")
    List<UserWord> findWordsForReview(@Param("userId") Long userId, 
                                    @Param("currentDate") LocalDateTime currentDate);
    
    @Query("SELECT COUNT(uw) FROM UserWord uw WHERE uw.user.id = :userId AND uw.status = 'LEARNING'")
    int countLearningWordsByUserId(@Param("userId") Long userId);
    
    boolean existsByUserIdAndWordId(Long userId, Long wordId);

    Optional<UserWord> findByUserIdAndWordId(Long userId, Long wordId);
}
