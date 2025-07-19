package com.zoirs.learn_en_word.entity;

import com.zoirs.learn_en_word.model.MeaningEntity;
import com.zoirs.learn_en_word.model.WordEntity;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_words")
@Data
@NoArgsConstructor
public class UserWord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "word_id", nullable = false)
    private WordEntity word;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meaning_id") //todo , nullable = false
    private MeaningEntity meaning;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LearningStatus status = LearningStatus.LEARNING;
    
    private int correctAnswers = 0;
    private int wrongAnswers = 0;
    private LocalDateTime nextReviewDate = LocalDateTime.now();
    private int learningStage = 0; // Used for spaced repetition
    
    public UserWord(User user, WordEntity word, MeaningEntity meaning) {
        this.user = user;
        this.word = word;
        this.meaning = meaning;
    }
    
    public void updateCorrectAnswer() {
        correctAnswers++;
        learningStage++;
        updateNextReviewDate();
    }
    
    public void updateWrongAnswer() {
        wrongAnswers++;
        learningStage = Math.max(0, learningStage - 1);
        updateNextReviewDate();
    }
    
    private void updateNextReviewDate() {
        // Simple spaced repetition algorithm
        int daysToAdd = 0;
        switch (learningStage) {
            case 0: daysToAdd = 1; break;  // 1 day
            case 1: daysToAdd = 3; break;  // 3 days
            case 2: daysToAdd = 7; break;  // 1 week
            case 3: daysToAdd = 14; break; // 2 weeks
            case 4: daysToAdd = 30; break; // 1 month
            default: daysToAdd = 60;       // 2 months
        }
        nextReviewDate = LocalDateTime.now().plusDays(daysToAdd);
    }
    
    public enum LearningStatus {
        LEARNING,   // Currently learning
        REVIEW,     // In review
        MASTERED    // Successfully learned
    }
}
