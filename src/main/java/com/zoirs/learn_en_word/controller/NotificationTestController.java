package com.zoirs.learn_en_word.controller;

import com.zoirs.learn_en_word.entity.User;
import com.zoirs.learn_en_word.repository.UserRepository;
import com.zoirs.learn_en_word.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification Test", description = "API for testing push notification delivery")
public class NotificationTestController {

    private static final Logger log = LoggerFactory.getLogger(NotificationTestController.class);
    private static final String TEST_NOTIFICATION_TITLE = "Тестовое уведомление";
    private static final String TEST_NOTIFICATION_BODY = "Это тестовое уведомление для проверки доставки.";

    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @PostMapping("/test/{userId}")
    @Operation(summary = "Send a test notification to a specific user")
    public ResponseEntity<Boolean> sendTestNotification(@PathVariable String userId) throws Exception {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "User not found"));

        if (StringUtils.isBlank(user.getFirebaseToken())) {
            throw new ResponseStatusException(BAD_REQUEST, "User does not have a Firebase token");
        }

        log.info("Sending test notification to user: {}", userId);
        notificationService.sendNotification(
                user,
                TEST_NOTIFICATION_TITLE,
                TEST_NOTIFICATION_BODY
        );

        return ResponseEntity.ok(true);
    }
}
