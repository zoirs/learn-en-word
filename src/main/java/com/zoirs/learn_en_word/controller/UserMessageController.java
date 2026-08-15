package com.zoirs.learn_en_word.controller;

import com.zoirs.learn_en_word.req.UserMessageReq;
import com.zoirs.learn_en_word.service.UserMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user-messages")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Messages", description = "API for saving messages received from users")
public class UserMessageController {

    private final UserMessageService userMessageService;

    @PostMapping
    @Operation(summary = "Save a user message")
    public ResponseEntity<Void> save(@RequestBody UserMessageReq req) {
        log.info("Received user message: userId={}, message={}, additionalInfo={}",
                req.userId(), req.message(), req.additionalInfo());
        userMessageService.save(req.userId(), req.message(), req.additionalInfo());
        return ResponseEntity.status(201).build();
    }
}
