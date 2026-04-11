package com.zoirs.learn_en_word.dto.chatgpt;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class ChatGPTRequest {
    private String model = "gpt-5.4-mini";

    private List<Message> messages;
    private double temperature = 0.7;
    private Object response_format;

    public ChatGPTRequest(List<Message> messages, double temperature, Object response_format) {
        this.messages = messages;
        this.temperature = temperature;
        this.response_format = response_format;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        private String role;
        private String content;

        public static Message systemMessage(String content) {
            return new Message("system", content);
        }

        public static Message userMessage(String content) {
            return new Message("user", content);
        }
    }
}
