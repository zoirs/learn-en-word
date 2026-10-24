package com.zoirs.learn_en_word.controller;

import com.zoirs.learn_en_word.entity.User;
import com.zoirs.learn_en_word.req.CreateTokenReq;
import com.zoirs.learn_en_word.service.UserService;
import com.zoirs.learn_en_word.service.XsollaPlansService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.util.HashMap;
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

    @Autowired
    private XsollaPlansService plansService;

    @GetMapping("/plans")
    public ResponseEntity<?> getPlans(@RequestParam(name = "country", required = false) String country) {
        log.info("Loading Xsolla plans, requested country: {}", country == null ? null
                : country.replace('\r', '_').replace('\n', '_'));
        try {
            return ResponseEntity.ok(plansService.getPlans());
        } catch (RestClientException | IllegalArgumentException e) {
            log.warn("Unable to load Xsolla subscription plans ({})", e.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                    "code", "XSOLLA_PLANS_UNAVAILABLE",
                    "message", "Не удалось загрузить предложения подписки"));
        }
    }

    @PostMapping("/token")
    public ResponseEntity<Map<String, Object>> createToken(@RequestBody CreateTokenReq req) {
        log.info("Creating token for user {}", req);
        User user = userService.getOrCreateUser(req.email(), req.userId());
        String id = user.getId();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(merchantId, apiKey);

        Map<String, Object> xsollaUser = new HashMap<>();
        xsollaUser.put("id", Map.of("value", id));
        xsollaUser.put("country", Map.of("value", "RU"));
        String email = StringUtils.trimToNull(req.email());
        if (email != null) {
            xsollaUser.put("email", Map.of("value", email));
        }

        Map<String, Object> settings = new HashMap<>(Map.of(
                "project_id", Integer.parseInt(projectId),
                "return_url", req.returnUrl(),
                "currency", "RUB",
                "language", "ru"
        ));
        if (req.sandbox()) {
            settings.put("mode", "sandbox");
        }

        Map<String, Object> body = Map.of(
                "user", xsollaUser,
                "settings", settings,
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
