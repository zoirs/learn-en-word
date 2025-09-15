package com.zoirs.learn_en_word.controller;

import com.zoirs.learn_en_word.entity.User;
import com.zoirs.learn_en_word.req.CreateTokenReq;
import com.zoirs.learn_en_word.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
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

    @Autowired
    private UserService userService;

    @PostMapping("/token")
    public ResponseEntity<Map<String, Object>> createToken(@RequestBody CreateTokenReq req) {
        log.info("Creating token for user {}", req);
        User user = userService.getOrCreateUser(req.email(), null);
        String id = user.getId();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(merchantId, apiKey);

        Map<String, Object> body = Map.of(
                "user", Map.of(
                        "id", Map.of("value", id),
                        "email", Map.of("value", req.email()),
                        "country",  Map.of("value", "RU")
                ),
                "settings", Map.of(
                        "project_id", Integer.parseInt(projectId),
                        "return_url", req.returnUrl(),
                        "mode", req.sandbox() ? "sandbox" : "live",
//                        "payment_method", 1380, //банковские карты
                        "currency", "RUB",
                        "language", "ru"
                ),
                "purchase", Map.of(
                        "subscription", Map.of("plan_id", req.planId())
                )
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        String url = "https://api.xsolla.com/merchant/v2/merchants/" + merchantId + "/token";
        Map<String, Object> response =
                restTemplate.postForObject(url, entity, Map.class);
        log.info("Token created: {}", response);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/pay-return")
    public ResponseEntity<Void> handlePayReturn(
            @RequestParam(required = false) String status,
            @RequestParam(required = false, name = "invoice_id") String invoiceId) {

        // deep link для мобильного приложения
        String redirectUrl = String.format(
                "comzoirshelloflutter://callback/pay-return?status=%s&invoice_id=%s",
                status != null ? status : "unknown",
                invoiceId != null ? invoiceId : "none"
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(redirectUrl));
        return new ResponseEntity<>(headers, HttpStatus.FOUND); // 302 Redirect
    }
}
