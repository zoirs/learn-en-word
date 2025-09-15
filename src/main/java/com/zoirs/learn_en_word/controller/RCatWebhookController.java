package com.zoirs.learn_en_word.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zoirs.learn_en_word.entity.SubscriptionPaymentType;
import com.zoirs.learn_en_word.service.UserService;
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
    private UserService userService;

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
        JSONObject event = message.optJSONObject("event");
        if (event == null) {
            log.warn("No event object in payload");
            return ResponseEntity.badRequest().body("{\"error\": \"missing event\"}");
        }

        String userId = event.optString("app_user_id", null);

        JSONObject subscriberAttributes = event.optJSONObject("subscriber_attributes");
        String email = null;
        if (subscriberAttributes != null) {
            JSONObject emailObject = subscriberAttributes.optJSONObject("$email");
            if (emailObject != null) {
                email = emailObject.optString("value", null);
            }
        }

        log.info("User ID: {}", userId);
        log.info("Email: {}", email);
        userService.createOrUpdatePaymentType(email, userId, SubscriptionPaymentType.REVENUE_CAT);
        return ResponseEntity.ok("");
    }
}
