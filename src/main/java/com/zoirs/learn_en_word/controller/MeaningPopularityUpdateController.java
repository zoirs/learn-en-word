package com.zoirs.learn_en_word.controller;

import com.zoirs.learn_en_word.service.MeaningPopularityUpdateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/meaning-popularity")
@RequiredArgsConstructor
@Tag(name = "Meaning Popularity", description = "API for updating Skyeng meaning popularity")
public class MeaningPopularityUpdateController {

    private final MeaningPopularityUpdateService meaningPopularityUpdateService;

    @PostMapping("/update")
    @Operation(summary = "Start meaning popularity update from the specified word id")
    public ResponseEntity<StartUpdateResponse> startUpdate(@RequestParam int lastWordId) {
        boolean started = meaningPopularityUpdateService.startUpdateAllPopularities(lastWordId);
        if (!started) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new StartUpdateResponse(false, lastWordId, "Meaning popularity update is already running"));
        }

        return ResponseEntity.accepted()
                .body(new StartUpdateResponse(true, lastWordId, "Meaning popularity update started"));
    }

    public record StartUpdateResponse(boolean started, int lastWordId, String message) {
    }
}
