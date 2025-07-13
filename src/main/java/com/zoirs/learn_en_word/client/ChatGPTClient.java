package com.zoirs.learn_en_word.client;

import com.zoirs.learn_en_word.dto.chatgpt.ChatGPTRequest;
import com.zoirs.learn_en_word.dto.chatgpt.ChatGPTResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange
public interface ChatGPTClient {
    @PostExchange
    ResponseEntity<ChatGPTResponse> generateResponse(@RequestBody ChatGPTRequest request);
}
