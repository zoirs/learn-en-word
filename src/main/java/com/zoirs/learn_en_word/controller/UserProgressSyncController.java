package com.zoirs.learn_en_word.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.zoirs.learn_en_word.req.UserProgressSyncCheckResponse;
import com.zoirs.learn_en_word.req.UserProgressSyncReq;
import com.zoirs.learn_en_word.req.UserProgressSyncResponse;
import com.zoirs.learn_en_word.service.UserProgressSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user-progress")
@RequiredArgsConstructor
@Tag(name = "User Progress Sync", description = "API for syncing user progress snapshots from client devices")
public class UserProgressSyncController {

    private final UserProgressSyncService userProgressSyncService;

    @PostMapping("/sync")
    @Operation(summary = "Sync user progress snapshot")
    public ResponseEntity<UserProgressSyncResponse> sync(@RequestBody UserProgressSyncReq req) {
        return ResponseEntity.ok(userProgressSyncService.sync(req));
    }

    @GetMapping("/sync/{userId}")
    @Operation(summary = "Load latest synced user progress snapshot")
    public ResponseEntity<JsonNode> load(@PathVariable String userId) {
        return ResponseEntity.ok(userProgressSyncService.loadSnapshot(userId));
    }

    @GetMapping("/check")
    @Operation(summary = "Check whether a synced user progress snapshot exists")
    public ResponseEntity<UserProgressSyncCheckResponse> check(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String email
    ) {
        return ResponseEntity.ok(userProgressSyncService.checkSync(userId, email));
    }
}
