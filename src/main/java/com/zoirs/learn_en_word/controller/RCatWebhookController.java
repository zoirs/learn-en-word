package com.zoirs.learn_en_word.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhooks/revenuecat")
public class RCatWebhookController {

    @Autowired
    private ObjectMapper objectMapper;

    private static final Logger log = LoggerFactory.getLogger(RCatWebhookController.class);

    @PostMapping
    public ResponseEntity<String> handle(HttpServletRequest request) throws Exception {
        String auth = request.getHeader("Authorization");
        log.info("auth: {}", auth);
        String raw = request.getReader().lines().reduce("", (a, b) -> a + b);
        log.info("Got: {}", raw);
        JSONObject message = new JSONObject(raw);
        log.info("Message: {}", message);
        return ResponseEntity.ok("");
    }
}
