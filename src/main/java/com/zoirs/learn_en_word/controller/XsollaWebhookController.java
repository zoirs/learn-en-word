package com.zoirs.learn_en_word.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/webhooks/xsolla")
public class XsollaWebhookController {

    @Autowired
    private ObjectMapper objectMapper;

    private static final Logger log = LoggerFactory.getLogger(XsollaWebhookController.class);
    @Value("${xsolla.XSOLLA_PROJECT_SECRET}")
    public String projectSecret;

    @PostMapping
    public ResponseEntity<String> handle(HttpServletRequest request) throws Exception {
        log.info("Received webhook request");
        String sigHeader = request.getHeader("Authorization"); // "Signature abcd..."
        String expectedPrefix = "Signature ";
        String signature = (sigHeader != null && sigHeader.startsWith(expectedPrefix))
                ? sigHeader.substring(expectedPrefix.length()) : "";

        // Получаем raw body как строку без повторной сериализации!
        String raw = request.getReader().lines().reduce("", (a,b) -> a + b);
        log.info("Got: {}", raw);

        // из Project settings > Webhooks
        String computed = org.apache.commons.codec.digest.DigestUtils.sha1Hex(raw + projectSecret);

        if (!computed.equals(signature)) return ResponseEntity.status(401).body("bad signature");

        Map<String, Object> stringObjectMap = jsonToMap(raw);
        if ("user_validation".equals(stringObjectMap.get("notification_type"))) {
            return ResponseEntity.status(204).build();
        }
        // TODO: распарсить raw JSON, проверить type:
        // - "payment" / "updated_subscription" / "cancel_subscription" и т.д.
        // Обновить статус подписки пользователя в вашей БД.
        return ResponseEntity.ok("OK");
    }

    public Map<String, Object> jsonToMap(String json) throws IOException {
        return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
    }
}
