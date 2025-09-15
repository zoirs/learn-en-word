package com.zoirs.learn_en_word.service;

import com.zoirs.learn_en_word.api.dto.skyeng.Meaning;
import com.zoirs.learn_en_word.api.dto.skyeng.Word;
import com.zoirs.learn_en_word.entity.User;
import com.zoirs.learn_en_word.entity.UserWord;
import com.zoirs.learn_en_word.exception.ResourceNotFoundException;
import com.zoirs.learn_en_word.model.MeaningEntity;
import com.zoirs.learn_en_word.model.WordEntity;
import com.zoirs.learn_en_word.repository.MeaningRepository;
import com.zoirs.learn_en_word.repository.UserRepository;
import com.zoirs.learn_en_word.repository.UserWordRepository;
import com.zoirs.learn_en_word.repository.WordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WordService {
    
    private static final int MIN_WORDS_FOR_LEARNING = 10;
    
    private final MeaningRepository meaningRepository;
    private final WordRepository wordRepository;
    private final UserWordRepository userWordRepository;
    private final UserRepository userRepository;
    private final ChatGPTService chatGPTService;
    private final DictionaryCacheService dictionaryCacheService;
    
   //  @Transactional(readOnly = true)
//    public List<UserWord> getWordsForReview(Long userId) {
//        return userWordRepository.findWordsForReview(userId, LocalDateTime.now());
//    }
    
   //  @Transactional
//    public UserWord processAnswer(Long userId, Long wordId, boolean isCorrect) {
//        UserWord userWord = userWordRepository.findByUserIdAndWordId(userId, wordId)
//                .orElseThrow(() -> new ResourceNotFoundException("Word not found in user's learning list"));
//
//        if (isCorrect) {
//            userWord.updateCorrectAnswer();
//        } else {
//            userWord.updateWrongAnswer();
//        }
//
//        // Update status based on learning stage
//        if (userWord.getLearningStage() >= 5) {
//            userWord.setStatus(UserWord.LearningStatus.MASTERED);
//        } else if (userWord.getLearningStage() >= 3) {
//            userWord.setStatus(UserWord.LearningStatus.REVIEW);
//        }
//
//        return userWordRepository.save(userWord);
//    }
    
   //  @Transactional
//    public void addWordsToLearning(Long userId, Integer wordId, Integer meaningId) {
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
//
//        WordEntity word = wordRepository.findByExternalId(wordId)
//                .orElseThrow(() -> new ResourceNotFoundException("Word not found"));
//        MeaningEntity meaning = meaningRepository.findByExternalId(meaningId)
//                .orElseThrow(() -> new ResourceNotFoundException("m not found"));
//        userWordRepository.save(new UserWord(user, word, meaning));
//    }
    
//   //  @Transactional
//    public void ensureEnoughWordsForLearning(Long userId) {
//        long learningWordsCount = userWordRepository.countLearningWordsByUserId(userId);
//
//        if (learningWordsCount < MIN_WORDS_FOR_LEARNING) {
//            try {
//                // Get user's known and learning words
//                Set<String> knownWords = userWordRepository.findByUserIdAndStatus(
//                    userId, UserWord.LearningStatus.MASTERED
//                ).stream()
//                .map(uw -> uw.getWord().getText())
//                .collect(Collectors.toSet());
//
//                Set<String> learningWords = userWordRepository.findByUserIdAndStatus(
//                    userId, UserWord.LearningStatus.LEARNING
//                ).stream()
//                .map(uw -> uw.getWord().getText())
//                .collect(Collectors.toSet());
//
//                // Get word suggestions from ChatGPT
//                Set<String> suggestedWordStrings = chatGPTService.suggestNewWords(
//                    knownWords,
//                    learningWords
//                );
//
//                if (!suggestedWordStrings.isEmpty()) {
//                    for (String word : suggestedWordStrings) {
//                        List<Meaning> meanings = dictionaryCacheService.searchWords(word);
//                        for (Meaning meaning : meanings) {
//                            addWordsToLearning(userId, meaning.getWordId(), meaning.getId());// todo как выбрать правильный id
//                        }
//                    }
//                }
//            } catch (Exception e) {
//                // If ChatGPT is not available, fall back to database
////                List<com.zoirs.learn_en_word.model.WordEntity> newWords = wordRepository.findNewWordsForUser(userId);
////                if (!newWords.isEmpty()) {
////                    addWordsToLearning(userId, newWords);
////                }
//                log.error("Got error while getting new words:", e);
//            }
//        }
//    }
    
   //  @Transactional(readOnly = true)
//    public UserWord getUserWordProgress(Long userId, Long wordId) {
//        return userWordRepository.findByUserIdAndWordId(userId, wordId)
//                .orElseThrow(() -> new ResourceNotFoundException("Word not found in user's learning list"));
//    }
//
//    public List<UserWord> getUserWords(Long userId) {
//        return userWordRepository.findByUserId(userId);
//    }
}
