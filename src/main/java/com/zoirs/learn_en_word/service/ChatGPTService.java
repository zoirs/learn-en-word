package com.zoirs.learn_en_word.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zoirs.learn_en_word.client.ChatGPTClient;
import com.zoirs.learn_en_word.dto.chatgpt.ChatGPTRequest;
import com.zoirs.learn_en_word.dto.chatgpt.ChatGPTResponse;
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

    @Value("${openai.api.model:gpt-3.5-turbo}")
    private String model;

    private static final String SYSTEM_PROMPT = "You are a helpful English language learning assistant. " +
            "Your task is to suggest new English words for a learner based on the words they already know and are currently learning. " +
            "Provide the response as a comma-separated list of words only, without any additional text or explanations. " +
            "Each word should be in its base form (e.g., 'run' instead of 'ran' or 'running'). " +
            "The words should be relevant to the user's level, which can be inferred from the words they know and are learning. " +
            "Aim for words that are slightly more advanced than the ones they're currently learning.";

    private static final String ASSESSMENT_PROMPT = "You are an English language proficiency assessment assistant. " +
            "Your task is to select a set of words that will help determine the user's English proficiency level. " +
            "The words should cover a range of difficulty levels and topics. " +
            "Provide the response as a comma-separated list of words only, without any additional text or explanations.";

//    private static final String EVALUATION_PROMPT = "You are an English language proficiency evaluation assistant. " +
//            "Your task is to evaluate the user's responses to the assessment words and determine their English proficiency level. " +
//            "Provide the response as a JSON object with the following structure: {\"level\": \"BEGINNER/INTERMEDIATE/ADVANCED\", \"suggested_words\": [\"word1\", \"word2\"]}";

    public Set<String> suggestNewWords(Set<String> knownWords, Set<String> learningWords) {
        try {
            String prompt = String.format(
                    "I know these words well: %s. I'm currently learning these words: %s. " +
                    "Please suggest new words for me to learn next.",
                    String.join(", ", knownWords),
                    String.join(", ", learningWords)
            );

            ChatGPTRequest request = new ChatGPTRequest(
                    model,
                    List.of(
                            ChatGPTRequest.Message.systemMessage(SYSTEM_PROMPT),
                            ChatGPTRequest.Message.userMessage(prompt)
                    ),
                    0.7
            );

            ResponseEntity<ChatGPTResponse> response = chatGPTClient.generateResponse(request);
            
            if (response.getBody() != null && 
                response.getBody().getChoices() != null && 
                !response.getBody().getChoices().isEmpty()) {
                
                String content = response.getBody().getChoices().get(0).getMessage().getContent();
                return  Set.of(content.split("\\s*,\\s*"));
            }
            return Collections.emptySet();
            
        } catch (Exception e) {
            log.error("Error while getting word suggestions from ChatGPT", e);
            return Collections.emptySet();
        }
    }

    public List<String> getInitialAssessmentWords() {
        try {
            ChatGPTRequest request = new ChatGPTRequest(
                    model,
                    List.of(
                            ChatGPTRequest.Message.systemMessage(ASSESSMENT_PROMPT),
                            ChatGPTRequest.Message.userMessage("Please provide a set of words to assess English proficiency level.")
                    ),
                    0.7
            );

            ResponseEntity<ChatGPTResponse> response = chatGPTClient.generateResponse(request);
            
            if (response.getBody() != null && 
                response.getBody().getChoices() != null && 
                !response.getBody().getChoices().isEmpty()) {
                
                String content = response.getBody().getChoices().get(0).getMessage().getContent();
                return Arrays.asList(content.split("\\s*,\\s*"));
            }
            return Collections.emptyList();
            
        } catch (Exception e) {
            log.error("Error while getting assessment words from ChatGPT", e);
            return Collections.emptyList();
        }
    }

//    public Map<String, Object> evaluateAssessment(Map<String, Boolean> responses) {
//        try {
//            StringBuilder responseString = new StringBuilder();
//            for (Map.Entry<String, Boolean> entry : responses.entrySet()) {
//                responseString.append(entry.getKey()).append(":").append(entry.getValue()).append("; ");
//            }
//
//            String prompt = String.format(
//                    "The user responded to the assessment words as follows: %s. " +
//                    "Please evaluate their proficiency level and suggest appropriate words for their level.",
//                    responseString.toString()
//            );
//
//            ChatGPTRequest request = new ChatGPTRequest(
//                    model,
//                    List.of(
//                            ChatGPTRequest.Message.systemMessage(EVALUATION_PROMPT),
//                            ChatGPTRequest.Message.userMessage(prompt)
//                    ),
//                    0.7
//            );
//
//            ResponseEntity<ChatGPTResponse> response = chatGPTClient.generateResponse(request);
//
//            if (response.getBody() != null &&
//                response.getBody().getChoices() != null &&
//                !response.getBody().getChoices().isEmpty()) {
//
//                String content = response.getBody().getChoices().get(0).getMessage().getContent();
//                return parseEvaluationResponse(content);
//            }
//            return Collections.emptyMap();
//
//        } catch (Exception e) {
//            log.error("Error while evaluating assessment results from ChatGPT", e);
//            return Collections.emptyMap();
//        }
//    }
//
//    private Map<String, Object> parseEvaluationResponse(String content) {
//        try {
//            return new ObjectMapper().readValue(content, Map.class);
//        } catch (Exception e) {
//            log.error("Error parsing evaluation response: " + content, e);
//            return Collections.emptyMap();
//        }
//    }
}
