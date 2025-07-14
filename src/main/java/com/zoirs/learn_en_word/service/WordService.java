package com.zoirs.learn_en_word.service;

import com.zoirs.learn_en_word.entity.User;
import com.zoirs.learn_en_word.entity.UserWord;
import com.zoirs.learn_en_word.entity.Word;
import com.zoirs.learn_en_word.exception.ResourceNotFoundException;
import com.zoirs.learn_en_word.model.WordEntity;
import com.zoirs.learn_en_word.repository.UserRepository;
import com.zoirs.learn_en_word.repository.UserWordRepository;
import com.zoirs.learn_en_word.repository.WordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WordService {
    
    private static final int MIN_WORDS_FOR_LEARNING = 10;
    
    private final WordRepository wordRepository;
    private final UserWordRepository userWordRepository;
    private final UserRepository userRepository;
    private final ChatGPTService chatGPTService;
    
    @Transactional(readOnly = true)
    public List<UserWord> getWordsForReview(Long userId) {
        return userWordRepository.findWordsForReview(userId, LocalDateTime.now());
    }
    
    @Transactional
    public UserWord processAnswer(Long userId, Long wordId, boolean isCorrect) {
        UserWord userWord = userWordRepository.findByUserIdAndWordId(userId, wordId)
                .orElseThrow(() -> new ResourceNotFoundException("Word not found in user's learning list"));
        
        if (isCorrect) {
            userWord.updateCorrectAnswer();
        } else {
            userWord.updateWrongAnswer();
        }
        
        // Update status based on learning stage
        if (userWord.getLearningStage() >= 5) {
            userWord.setStatus(UserWord.LearningStatus.MASTERED);
        } else if (userWord.getLearningStage() >= 3) {
            userWord.setStatus(UserWord.LearningStatus.REVIEW);
        }
        
        return userWordRepository.save(userWord);
    }
    
    @Transactional
    public void addWordsToLearning(Long userId, List<Word> words) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        words.stream()
                .filter(word -> !userWordRepository.existsByUserIdAndWordId(userId, word.getId()))
                .map(word -> new UserWord(user, word))
                .forEach(userWordRepository::save);
    }
    
    @Transactional
    public void ensureEnoughWordsForLearning(Long userId) {
        long learningWordsCount = userWordRepository.countLearningWordsByUserId(userId);
        
        if (learningWordsCount < MIN_WORDS_FOR_LEARNING) {
            List<WordEntity> newWords = wordRepository.findNewWordsForUser(userId);
            
            if (newWords.isEmpty()) {
                // If no new words in DB, try to fetch from ChatGPT
                List<Word> suggestedWords = chatGPTService.suggestNewWords(MIN_WORDS_FOR_LEARNING);
                wordRepository.saveAll(suggestedWords);
                newWords = suggestedWords;
            }
            
            if (!newWords.isEmpty()) {
                addWordsToLearning(userId, newWords);
            }
        }
    }
    
    @Transactional(readOnly = true)
    public UserWord getUserWordProgress(Long userId, Long wordId) {
        return userWordRepository.findByUserIdAndWordId(userId, wordId)
                .orElseThrow(() -> new ResourceNotFoundException("Word not found in user's learning list"));
    }
}
