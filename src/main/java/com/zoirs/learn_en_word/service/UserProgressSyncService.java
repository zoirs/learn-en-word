package com.zoirs.learn_en_word.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zoirs.learn_en_word.entity.SubscriptionPaymentType;
import com.zoirs.learn_en_word.entity.User;
import com.zoirs.learn_en_word.entity.UserProgressSyncSnapshot;
import com.zoirs.learn_en_word.repository.UserProgressSyncSnapshotRepository;
import com.zoirs.learn_en_word.repository.UserRepository;
import com.zoirs.learn_en_word.req.UserProgressSyncCheckResponse;
import com.zoirs.learn_en_word.req.UserProgressSyncReq;
import com.zoirs.learn_en_word.req.UserProgressSyncResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProgressSyncService {

    private static final Set<String> LEARNING_STATUSES = Set.of("learning", "toRepeat");

    private final UserService userService;
    private final UserRepository userRepository;
    private final UserProgressSyncSnapshotRepository snapshotRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public UserProgressSyncResponse sync(UserProgressSyncReq req) {
        validate(req);

        String requestedUserId = req.sync().userId();
        User user = userService.getOrCreateUser(req.user() != null ? req.user().email() : null, requestedUserId);
        String userId = user.getId();
        mergeUserData(user, req);
        userRepository.save(user);

        OffsetDateTime now = OffsetDateTime.now();
        UserProgressSyncSnapshot snapshot = snapshotRepository.findById(userId)
                .orElseGet(UserProgressSyncSnapshot::new);
        snapshot.setUserId(userId);
        snapshot.setSchemaVersion(req.schemaVersion());
        snapshot.setClientUpdatedAt(req.sync().clientUpdatedAt());
        snapshot.setSyncedAt(now);
        snapshot.setWordProgressCount(sizeOf(req.wordProgress()));
        snapshot.setDailySessionCount(sizeOf(req.dailySessions()));
        snapshot.setPayloadJson(serialize(req));
        snapshotRepository.save(snapshot);

        log.info("Synced progress snapshot for userId={}, words={}, sessions={}",
                userId, snapshot.getWordProgressCount(), snapshot.getDailySessionCount());

        return new UserProgressSyncResponse("ok", userId, now);
    }

    @Transactional(readOnly = true)
    public JsonNode loadSnapshot(String userId) {
        if (StringUtils.isBlank(userId)) {
            throw new ResponseStatusException(BAD_REQUEST, "userId is required");
        }

        UserProgressSyncSnapshot snapshot = snapshotRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Progress snapshot not found"));

        try {
            return objectMapper.readTree(snapshot.getPayloadJson());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize stored progress snapshot", e);
        }
    }

    @Transactional(readOnly = true)
    public UserProgressSyncCheckResponse checkSync(String userId, String email) {
        if (StringUtils.isBlank(userId) && StringUtils.isBlank(email)) {
            throw new ResponseStatusException(BAD_REQUEST, "userId or email is required");
        }

        Optional<UserProgressSyncSnapshot> snapshotOptional = Optional.empty();
        User user = null;

        if (StringUtils.isNotBlank(userId)) {
            snapshotOptional = snapshotRepository.findById(userId);
            if (snapshotOptional.isPresent()) {
                user = userRepository.findById(userId).orElse(null);
            }
        }

        if (snapshotOptional.isEmpty() && StringUtils.isNotBlank(email)) {
            user = userRepository.findByEmail(email);
            if (user != null) {
                snapshotOptional = snapshotRepository.findById(user.getId());
            }
        }

        if (snapshotOptional.isEmpty()) {
            return new UserProgressSyncCheckResponse(false, userId, email, null, null, null, null);
        }

        UserProgressSyncSnapshot snapshot = snapshotOptional.get();
        if (user == null) {
            user = userRepository.findById(snapshot.getUserId()).orElse(null);
        }

        return new UserProgressSyncCheckResponse(
                true,
                snapshot.getUserId(),
                user != null ? user.getEmail() : email,
                snapshot.getSchemaVersion(),
                snapshot.getWordProgressCount(),
                snapshot.getClientUpdatedAt(),
                snapshot.getSyncedAt()
        );
    }

    private void mergeUserData(User user, UserProgressSyncReq req) {
        UserProgressSyncReq.UserBlock userBlock = req.user();
        UserProgressSyncReq.SettingsBlock settingsBlock = req.settings();

        if (userBlock != null && StringUtils.isNotBlank(userBlock.email())) {
            user.setEmail(userBlock.email());
        }
        if (userBlock != null && StringUtils.isNotBlank(userBlock.subscriptionStatus())) {
            user.setPaymentType(parsePaymentType(userBlock.subscriptionStatus()));
        }
        if (settingsBlock != null && settingsBlock.dailyNotifications() != null) {
            user.setDailyNotifications(settingsBlock.dailyNotifications());
        }

        user.setKnownWords(extractMeaningIdsByStatus(req.wordProgress(), Set.of("learned")));
        user.setLearningWords(extractMeaningIdsByStatus(req.wordProgress(), LEARNING_STATUSES));
        user.setNewWords(extractMeaningIdsByStatus(req.wordProgress(), Set.of("newWord")));
    }

    private Set<Integer> extractMeaningIdsByStatus(List<UserProgressSyncReq.WordProgressItem> items, Set<String> statuses) {
        if (items == null || items.isEmpty()) {
            return new HashSet<>();
        }
        Set<Integer> ids = new HashSet<>();
        for (UserProgressSyncReq.WordProgressItem item : items) {
            if (item == null || item.meaningId() == null || StringUtils.isBlank(item.status())) {
                continue;
            }
            if (statuses.contains(item.status())) {
                ids.add(item.meaningId());
            }
        }
        return ids;
    }

    private SubscriptionPaymentType parsePaymentType(String rawValue) {
        String normalized = rawValue.trim().toUpperCase(Locale.ROOT);
        if (!EnumSet.allOf(SubscriptionPaymentType.class).stream().map(Enum::name).toList().contains(normalized)) {
            log.warn("Unknown subscription_status value received: {}", rawValue);
            return SubscriptionPaymentType.NONE;
        }
        return SubscriptionPaymentType.valueOf(normalized);
    }

    private int sizeOf(List<?> list) {
        return list == null ? 0 : list.size();
    }

    private String serialize(UserProgressSyncReq req) {
        try {
            return objectMapper.writeValueAsString(req);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize user progress sync payload", e);
        }
    }

    private void validate(UserProgressSyncReq req) {
        if (req == null || req.sync() == null || StringUtils.isBlank(req.sync().userId())) {
            throw new ResponseStatusException(BAD_REQUEST, "sync.user_id is required");
        }
    }
}
