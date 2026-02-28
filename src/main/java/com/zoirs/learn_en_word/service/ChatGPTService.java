package com.zoirs.learn_en_word.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zoirs.learn_en_word.client.ChatGPTClient;
import com.zoirs.learn_en_word.dto.chatgpt.ChatGPTRequest;
import com.zoirs.learn_en_word.dto.chatgpt.ChatGPTResponse;
import com.zoirs.learn_en_word.dto.chatgpt.WordsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatGPTService {

    private final ChatGPTClient chatGPTClient;

//    @Value("${openai.api.model:gpt-4o-mini}")
    private static final String model = "gpt-4.1";

    Map<String, Object> schema = Map.of(
            "type", "object",
            "properties", Map.of(
                    "easier", Map.of(
                            "type", "array",
                            "items", Map.of("type", "string"),
                            "minItems", 2,
                            "maxItems", 2
                    ),
                    "same", Map.of(
                            "type", "array",
                            "items", Map.of("type", "string"),
                            "minItems", 3,
                            "maxItems", 3
                    ),
                    "harder", Map.of(
                            "type", "array",
                            "items", Map.of("type", "string"),
                            "minItems", 4,
                            "maxItems", 4
                    )
            ),
            "required", List.of("easier", "same", "harder"),
            "additionalProperties", false
    );
    private static final String SYSTEM_PROMPT = """
            You are a helpful English language learning assistant.
            Your task is to suggest new English words for a learner based on the words they already know and the words they are currently learning.
            Each word must follow these rules:
            1. Avoid rare, academic words.
            2. Avoid synonyms and near-synonyms of each other and of the user’s known/learning words.
            3. Suggest words in three groups:
            - 3 words that are slightly easier than the current learning words,
            - 3 words at approximately the same level as the current learning words,
            - 3 words that are slightly more advanced.
            4. Each group must contain 1 noun, 1 verb, 1 adverb or adjective.
            """;

    public Set<String> suggestNewWords(Set<String> knownWords, Set<String> learningWords) {
        try {
            String prompt = String.format(
                    "I know these words well: %s. I'm currently learning these words: %s. " +
                    "Please suggest new words for me to learn next.",
                    String.join(", ", knownWords),
                    String.join(", ", learningWords)
            );

            Map<String, Object> responseFormat = Map.of(
                    "type", "json_schema",
                    "json_schema", Map.of(
                            "name", "word_suggestion",
                            "schema", schema
                    )
            );
            ChatGPTRequest request = new ChatGPTRequest(
                    model,
                    List.of(
                            ChatGPTRequest.Message.systemMessage(SYSTEM_PROMPT),
                            ChatGPTRequest.Message.userMessage(prompt)
                    ),
                    0.7,
                    responseFormat
            );
            log.info("Request: {}", prompt);
            ResponseEntity<ChatGPTResponse> response = chatGPTClient.generateResponse(request);
            
            if (response.getBody() != null && 
                response.getBody().getChoices() != null && 
                !response.getBody().getChoices().isEmpty()) {

                String content = response.getBody().getChoices().get(0).getMessage().getContent();
                log.info("Response: {}", content);
                WordsResponse wordsResponse = new ObjectMapper().readValue(content, WordsResponse.class);
                return  new HashSet<>(wordsResponse.getWords());
            }
            return Collections.emptySet();
            
        } catch (Exception e) {
            log.error("Error while getting word suggestions from ChatGPT", e);
            return Collections.emptySet();
        }
    }
}
