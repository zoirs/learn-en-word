package com.zoirs.learn_en_word.dto.chatgpt;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatGPTRequest {
    private String model = "gpt-3.5-turbo";
    private List<Message> messages;
    private double temperature = 0.7;

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
