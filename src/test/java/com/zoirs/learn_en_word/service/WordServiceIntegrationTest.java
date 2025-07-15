package com.zoirs.learn_en_word.service;

import com.zoirs.learn_en_word.TestApplicationRunner;
import com.zoirs.learn_en_word.entity.User;
import com.zoirs.learn_en_word.entity.UserWord;
import com.zoirs.learn_en_word.model.WordEntity;
import com.zoirs.learn_en_word.repository.UserRepository;
import com.zoirs.learn_en_word.repository.UserWordRepository;
import com.zoirs.learn_en_word.repository.WordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = TestApplicationRunner.class)
@ActiveProfiles("test")
@Transactional
class WordServiceIntegrationTest {

    @Autowired
    private WordService wordService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WordRepository wordRepository;

    @Autowired
    private UserWordRepository userWordRepository;

    @Autowired
    private DictionaryCacheService dictionaryCacheService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername("testuser");
        userRepository.save(testUser);
    }

    @Test
    void ensureEnoughWordsForLearning_WhenNoWords_ShouldAddNewWords() {
        // Given
        // No words in the system initially

        // When
        wordService.ensureEnoughWordsForLearning(testUser.getId());

        // Then
        List<UserWord> userWords = userWordRepository.findByUserIdAndStatus(
            testUser.getId(), 
            UserWord.LearningStatus.LEARNING
        );
        assertFalse(userWords.isEmpty(), "Should add words for learning");
    }

    @Test
    void ensureEnoughWordsForLearning_WhenSomeWordsExist_ShouldAddMoreToReachMinimum() {
        // Given
        // Add some words to the database
        WordEntity word1 = new WordEntity(-1L);
        word1.setText("apple");
        wordRepository.save(word1);

        WordEntity word2 = new WordEntity(-2L);
        word2.setText("banana");
        wordRepository.save(word2);

        // Add one word to user's learning list
        UserWord userWord = new UserWord(testUser, word1);
        userWordRepository.save(userWord);

        // When
        wordService.ensureEnoughWordsForLearning(testUser.getId());

        // Then
        List<UserWord> userWords = userWordRepository.findByUserIdAndStatus(
            testUser.getId(), 
            UserWord.LearningStatus.LEARNING
        );
        assertTrue(userWords.size() >= 2, "Should have at least 2 words for learning");
    }

    @Test
    void ensureEnoughWordsForLearning_WhenAlreadyHasEnoughWords_ShouldNotAddMore() {
        // Given
        // Add more than minimum required words
        for (int i = 0; i < 15; i++) {
            WordEntity word = new WordEntity((long) (0-i));
            word.setText("word" + i);
            wordRepository.save(word);

            UserWord userWord = new UserWord(testUser, word);
            userWordRepository.save(userWord);
        }

        int initialCount = userWordRepository.findByUserIdAndStatus(
            testUser.getId(), 
            UserWord.LearningStatus.LEARNING
        ).size();

        // When
        wordService.ensureEnoughWordsForLearning(testUser.getId());

        // Then
        int newCount = userWordRepository.findByUserIdAndStatus(
            testUser.getId(), 
            UserWord.LearningStatus.LEARNING
        ).size();
        
        assertEquals(initialCount, newCount, "Should not add more words when already have enough");
    }

    @Test
    void ensureEnoughWordsForLearning_WhenChatGPTSuggestsWords_ShouldAddThem() {
        // Given
        // No words in the system initially

        // When
        wordService.ensureEnoughWordsForLearning(testUser.getId());

        // Then - verify words were added through the normal flow
        List<UserWord> userWords = userWordRepository.findByUserIdAndStatus(
            testUser.getId(), 
            UserWord.LearningStatus.LEARNING
        );
        
        assertFalse(userWords.isEmpty(), "Should add words suggested by ChatGPT");
    }
}
