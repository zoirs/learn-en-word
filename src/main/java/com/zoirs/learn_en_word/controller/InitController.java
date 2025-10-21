package com.zoirs.learn_en_word.controller;

import com.zoirs.learn_en_word.req.InitSessionReq;
import com.zoirs.learn_en_word.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/session")
public class InitController {

    private static final Logger log = LoggerFactory.getLogger(InitController.class);

    @Autowired
    private UserService userService;

    @PostMapping("/init")
    public ResponseEntity<Boolean> createToken(@RequestBody InitSessionReq req) {
        log.info("Init user {}", req);
        userService.initUser(req.userId(), req.fireBaseToken(), req.timezone());
        return ResponseEntity.ok(true);
    }
}
