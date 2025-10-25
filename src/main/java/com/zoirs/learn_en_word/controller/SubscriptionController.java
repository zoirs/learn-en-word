package com.zoirs.learn_en_word.controller;

import com.zoirs.learn_en_word.entity.SubscriptionPaymentType;
import com.zoirs.learn_en_word.entity.User;
import com.zoirs.learn_en_word.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/subscription")
public class SubscriptionController {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionController.class);
    @Autowired
    private UserService userService;

    // используется
    @PostMapping("/check")
    public ResponseEntity<Map<String, String>> check(String email,
                                                     @RequestParam(required = false) String userId) {
        log.info("Checking subscription for user {}, {}", email, userId);
        User user = userService.get(email, userId);
        String subscription = (user != null && user.getPaymentType() != null) ? user.getPaymentType().name() : SubscriptionPaymentType.NONE.name();
        return ResponseEntity.ok(Map.of("subscribed", subscription));
    }
}

