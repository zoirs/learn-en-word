package com.zoirs.learn_en_word.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
@RequestMapping("/api/xsolla")
public class XsollaController {

    private static final Logger log = LoggerFactory.getLogger(XsollaController.class);
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${xsolla.XSOLLA_MERCHANT_ID}")
    private String merchantId;
    @Value("${xsolla.XSOLLA_API_KEY}")
    private String apiKey;
    @Value("${xsolla.XSOLLA_PROJECT_ID}")
    private String projectId;

    @PostMapping("/token")
    public ResponseEntity<Map<String, Object>> createToken(@RequestBody CreateTokenReq req) {
        log.info("Creating token for user {}", req.userId);
        String url = "https://api.xsolla.com/merchant/v2/merchants/" + merchantId + "/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(merchantId, apiKey);

        Map<String, Object> body = Map.of(
                "user", Map.of(
                        "id", Map.of("value", req.userId),
                        "email", Map.of("value", req.email)
                ),
                "settings", Map.of(
                        "project_id", Integer.parseInt(projectId),
                        "return_url", req.returnUrl,
                        "mode", req.sandbox ? "sandbox" : "live"
                ),
                "purchase", Map.of(
                        "subscription", Map.of("plan_id", req.planId)
                )
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        Map<String, Object> response =
                restTemplate.postForObject(url, entity, Map.class);
        log.info("Token created: {}", response);

        return ResponseEntity.ok(response);
    }

    public static record CreateTokenReq(
            String userId,
            String email,
            String planId,
            String returnUrl,
            boolean sandbox
    ) {}
}
