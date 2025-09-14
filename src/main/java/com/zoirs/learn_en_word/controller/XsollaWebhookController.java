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
import org.json.JSONObject;

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

        if (!computed.equals(signature)) {
            return ResponseEntity.badRequest().body("{\"error\": {\"code\": \"INVALID_SIGNATURE\",\"message\": \"Invalid signature\"}}");
        }

        JSONObject message = new JSONObject(raw);
        String notificationType = message.getString("notification_type");
        switch (notificationType) {
            case "user_validation":
                JSONObject user = message.getJSONObject("user");
                if (user.getString("id").startsWith("test_xsolla")) {
                    log.info("User validation return 400");
                    return ResponseEntity.badRequest().body("{\"error\": {\"code\": \"INVALID_USER\",\"message\": \"Invalid user\"}}");
                } else {
                    log.info("User validation return 204");
                    return ResponseEntity.status(204).build();
                }
            case "payment":
                return ResponseEntity.ok("{\"status\": \"Payment processed successfully\"}");
            case "refund":
                return ResponseEntity.status(200).build();
            default:
                return ResponseEntity.status(200).build();
        }
        // TODO: распарсить raw JSON, проверить type:
        // - "payment" / "updated_subscription" / "cancel_subscription" и т.д.
        // Обновить статус подписки пользователя в вашей БД.
//        return ResponseEntity.ok("OK");
    }

    public Map<String, Object> jsonToMap(String json) throws IOException {
        return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
    }
}
