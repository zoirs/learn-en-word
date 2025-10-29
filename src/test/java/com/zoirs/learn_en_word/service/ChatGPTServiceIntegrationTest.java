package com.zoirs.learn_en_word.service;

import com.zoirs.learn_en_word.TestApplicationRunner;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest(classes = TestApplicationRunner.class)
@ActiveProfiles("test")
class ChatGPTServiceIntegrationTest {

    @Autowired
    private ChatGPTService chatGPTService;

    @Test
    void suggestNewWords_WithValidInput_ReturnsListOfWords() {
        // Given
        Set<String> knownWords = Set.of( "explain", "cancel", "clarify", "career", "prefer", "almost", "coherent", "tell", "run", "simple", "attempt", "concern", "debate", "mother", "solution", "substantiate", "school", "justify", "create", "implement", "plan", "reduce", "imply", "like", "impact", "water", "airport", "stop", "job", "support", "dog", "facilitate", "maybe", "paradigm", "advice", "book", "house", "holiday", "yesterday", "hungry", "apple", "notion", "define", "relationship", "policy", "undermine", "homework", "efficient", "chair", "argue", "message", "clean", "despite", "integrate", "help", "furthermore", "nervous", "borrow", "behaviour", "invite", "customer");
//        Set<String> knownWords = Set.of( "advice", "career", "cancel", "customer", "explain", "prefer", "nervous", "borrow", "relationship", "almost","efficient", "impact", "solution", "concern", "despite", "attempt", "policy", "reduce", "behaviour", "debate");
        Set<String> learningWords = new HashSet<>(Set.of("contemplate", "elucidate", "synthesize", "imply", "justify", "notion", "facilitate"));

        for (int i = 0; i < 5; i++) {
            Set<String> result = chatGPTService.suggestNewWords(knownWords, learningWords);
//            log.info("Suggested words: {}", String.join(",", result));
            learningWords.addAll(result);
        }


    }

}
