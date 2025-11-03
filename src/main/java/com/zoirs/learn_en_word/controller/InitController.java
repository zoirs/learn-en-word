package com.zoirs.learn_en_word.controller;

import com.google.common.base.MoreObjects;
import com.zoirs.learn_en_word.req.InitSessionReq;
import com.zoirs.learn_en_word.service.NotificationService;
import com.zoirs.learn_en_word.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/session")
public class InitController {

    private static final Logger log = LoggerFactory.getLogger(InitController.class);

    @Autowired
    private NotificationService notificationService;
    @Autowired
    private UserService userService;

    //используется
    @PostMapping("/init")
    public ResponseEntity<Boolean> createToken(@RequestBody InitSessionReq req) {
        log.info("Init user {}", req);
        int count = MoreObjects.firstNonNull(req.dailyNotifications(), 5);
        userService.initUser(req.userId(), req.fireBaseToken(), req.timezoneOffset(), count);
        return ResponseEntity.ok(true);
    }

    //используется
    @GetMapping("/test")
    public ResponseEntity<Boolean> test() {
        log.info("test notify");
        notificationService.sendHourlyQuizzes();
        return ResponseEntity.ok(true);
    }
}
