package com.zoirs.learn_en_word.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import com.zoirs.learn_en_word.entity.SubscriptionPaymentType;
import com.zoirs.learn_en_word.entity.User;
import com.zoirs.learn_en_word.model.MeaningEntity;
import com.zoirs.learn_en_word.model.TranslationEntity;
import com.zoirs.learn_en_word.repository.MeaningRepository;
import com.zoirs.learn_en_word.repository.UserRepository;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.zoirs.learn_en_word.service.DatabaseWordSuggestionService.SEARCHABLE_PART_OF_SPEECH_CODES;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final int DEFAULT_DAILY_NOTIFICATIONS = 3;
    private static final int NOTIFICATION_START_HOUR = 10;
    private static final int NOTIFICATION_END_HOUR = 21;
    private static final int SUBSCRIPTION_OFFER_INTERVAL_DAYS = 3;
    private static final int NOTIFICATION_WORDS_COUNT = 3;
    // FCM allows 4096-byte payloads; reserve space for payload keys and JSON overhead.
    private static final int MAX_NOTIFICATION_TEXT_BYTES = 3_500;
    private static final String FREE_NOTIFICATION_TITLE = "Повторяйте слова, не открывая приложение";
    private static final String SUBSCRIPTION_PROMPT = "Такие уведомления доступны по подписке";
    private final Map<String, DailyNotificationCounter> dailyNotificationCounters = new ConcurrentHashMap<>();

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private MeaningRepository meaningRepository;

    public void sendNotification(String token, String title, String body) throws Exception {
        Message message = Message.builder()
                .setToken(token)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .build();

        String response = FirebaseMessaging.getInstance().send(message);
        log.info("Notification sent: {}", response);
    }

    public void sendNotification(User user, String title, String body) throws Exception {
        String firebaseToken = user.getFirebaseToken();
        try {
            sendNotification(firebaseToken, title, body);
        } catch (FirebaseMessagingException e) {
            handleFirebaseMessagingException(user, firebaseToken, e);
            throw e;
        }
    }

    void handleFirebaseMessagingException(
            User user,
            String firebaseToken,
            FirebaseMessagingException messagingException
    ) {
        if (messagingException.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
            clearUnregisteredToken(user, firebaseToken, messagingException);
        }
    }

    private void clearUnregisteredToken(
            User user,
            String firebaseToken,
            FirebaseMessagingException messagingException
    ) {
        try {
            int clearedTokens = userRepository.clearFirebaseToken(user.getId(), firebaseToken);
            if (clearedTokens > 0) {
                user.setFirebaseToken(null);
                log.warn("Removed unregistered Firebase token for user: {}", user.getId());
            } else {
                log.info("Firebase token was already changed for user: {}", user.getId());
            }
        } catch (RuntimeException cleanupException) {
            messagingException.addSuppressed(cleanupException);
            log.error("Failed to remove unregistered Firebase token for user: {}", user.getId(), cleanupException);
        }
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void sendWordReviewNotifications() {
        OffsetDateTime activeSince = OffsetDateTime.now().minusWeeks(2);
        log.info("Started word review notification job, activeSince={}", activeSince);

        List<User> users = userRepository.findRecentlyActive(activeSince);

        int sentCount = 0;
        int errorCount = 0;
        for (User user : users) {
            if (StringUtils.isEmpty(user.getFirebaseToken())
                    || CollectionUtils.isEmpty(user.getLearningWords())) {
                continue;
            }

            boolean paidSubscription = hasPaidSubscription(user);
            int dailyNotificationLimit = resolveDailyNotificationLimit(user);
            if (dailyNotificationLimit <= 0) {
                continue;
            }

            if (paidSubscription) {
                if (CollectionUtils.isEmpty(user.getNewWords())
                        || isDailyNotificationLimitReached(user, dailyNotificationLimit)
                        || !isNotificationHour(user, dailyNotificationLimit)) {
                    continue;
                }
            } else if (!isSubscriptionOfferDue(user) || !isNotificationHour(user, 1)) {
                continue;
            }

            List<MeaningEntity> meanings = selectNotificationMeanings(user);
            if (meanings.isEmpty()) {
                continue;
            }
            try {
                Optional<NotificationContent> notificationContent = buildNotificationContent(
                        meanings,
                        paidSubscription
                );
                if (notificationContent.isEmpty()) {
                    continue;
                }
                NotificationContent notification = notificationContent.get();
                log.info("Sending notification to user: {} {}", user.getId(), user.getUsername());

                sendNotification(user, notification.title(), notification.body());
                if (paidSubscription) {
                    incrementDailyNotificationCount(user);
                } else {
                    user.setLastSubscriptionOfferNotificationAt(OffsetDateTime.now(ZoneOffset.UTC));
                }
                sentCount++;
            } catch (Exception e) {
                errorCount++;
                if (e.toString().contains("Requested entity was not found")) {
                    log.error("Error sending notification to user: {} {}", user.getId(), e.toString());
                } else {
                    log.error("Error sending notification to user: {}", user.getId(), e);
                }
            }
        }
        log.info("Finished word review notification job, sent={}, errors={}", sentCount, errorCount);
    }

    Optional<NotificationContent> buildNotificationContent(
            List<MeaningEntity> meanings,
            boolean paidSubscription
    ) {
        String title = paidSubscription
                ? "Время повторить слова"
                : FREE_NOTIFICATION_TITLE;
        List<String> wordTranslations = selectWordTranslationsThatFit(
                meanings,
                title,
                paidSubscription
        );
        if (wordTranslations.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new NotificationContent(
                title,
                buildNotificationBody(wordTranslations, paidSubscription)
        ));
    }

    List<MeaningEntity> selectNotificationMeanings(User user) {
        List<Integer> learningWordIds = new ArrayList<>(user.getLearningWords());
        List<MeaningEntity> learningMeanings = meaningRepository.findByExternalIdIn(learningWordIds);
        List<MeaningEntity> wordMeanings = learningMeanings.stream()
                .filter(meaning -> SEARCHABLE_PART_OF_SPEECH_CODES.contains(meaning.getPartOfSpeechCode()))
                .collect(Collectors.toCollection(ArrayList::new));
        Collections.shuffle(wordMeanings);
        return wordMeanings.stream()
                .limit(NOTIFICATION_WORDS_COUNT)
                .toList();
    }

    private List<String> selectWordTranslationsThatFit(
            List<MeaningEntity> meanings,
            String title,
            boolean paidSubscription
    ) {
        List<String> selectedTranslations = new ArrayList<>();
        for (MeaningEntity meaning : meanings) {
            if (selectedTranslations.size() >= NOTIFICATION_WORDS_COUNT) {
                break;
            }

            String translation = formatWordTranslation(meaning);
            List<String> candidateTranslations = new ArrayList<>(selectedTranslations);
            candidateTranslations.add(translation);
            String candidateBody = buildNotificationBody(candidateTranslations, paidSubscription);
            if (getUtf8Size(title) + getUtf8Size(candidateBody) <= MAX_NOTIFICATION_TEXT_BYTES) {
                selectedTranslations.add(translation);
            }
        }
        return selectedTranslations;
    }

    private String buildNotificationBody(List<String> wordTranslations, boolean paidSubscription) {
        String body = String.join("\n", wordTranslations);
        if (!paidSubscription) {
            body += "\n—\n" + SUBSCRIPTION_PROMPT;
        }
        return body;
    }

    private int getUtf8Size(String value) {
        return value.getBytes(StandardCharsets.UTF_8).length;
    }

    private String formatWordTranslation(MeaningEntity meaning) {
        TranslationEntity translation = meaning.getTranslationEntity();
        StringBuilder wordTranslation = new StringBuilder();
        if (StringUtils.isNotEmpty(meaning.getPrefix())) {
            wordTranslation.append(meaning.getPrefix())
                    .append(" ")
                    .append(meaning.getText());
        } else {
            wordTranslation.append(StringUtils.capitalize(meaning.getText()));
        }
        return wordTranslation.append(" - ").append(translation.getText()).toString();
    }

    private boolean hasPaidSubscription(User user) {
        return user.getPaymentType() == SubscriptionPaymentType.REVENUE_CAT
                || user.getPaymentType() == SubscriptionPaymentType.XSOLLA;
    }

    private boolean isSubscriptionOfferDue(User user) {
        OffsetDateTime lastNotificationAt = user.getLastSubscriptionOfferNotificationAt();
        return lastNotificationAt == null
                || !lastNotificationAt.plusDays(SUBSCRIPTION_OFFER_INTERVAL_DAYS)
                .isAfter(OffsetDateTime.now(ZoneOffset.UTC));
    }

    private int resolveDailyNotificationLimit(User user) {
        return user.getDailyNotifications() == null ? DEFAULT_DAILY_NOTIFICATIONS : user.getDailyNotifications();
    }

    private boolean isNotificationHour(User user, int dailyNotificationLimit) {
        int localHour = getUserLocalDateTime(user).getHour();
        return getDailyNotificationHours(dailyNotificationLimit).contains(localHour);
    }

    private Set<Integer> getDailyNotificationHours(int dailyNotificationLimit) {
        int windowHours = NOTIFICATION_END_HOUR - NOTIFICATION_START_HOUR + 1;
        int notificationsCount = Math.min(dailyNotificationLimit, windowHours);
        Set<Integer> hours = new LinkedHashSet<>();
        if (notificationsCount <= 0) {
            return hours;
        }
        if (notificationsCount == 1) {
            hours.add((NOTIFICATION_START_HOUR + NOTIFICATION_END_HOUR) / 2);
            return hours;
        }
        for (int i = 0; i < notificationsCount; i++) {
            int hour = NOTIFICATION_START_HOUR
                    + (int) Math.round((double) (NOTIFICATION_END_HOUR - NOTIFICATION_START_HOUR) * i / (notificationsCount - 1));
            hours.add(hour);
        }
        return hours;
    }

    private boolean isDailyNotificationLimitReached(User user, int dailyNotificationLimit) {
        DailyNotificationCounter counter = dailyNotificationCounters.get(user.getId());
        LocalDate today = getUserLocalDate(user);
        return counter != null && counter.date.equals(today) && counter.count >= dailyNotificationLimit;
    }

    private void incrementDailyNotificationCount(User user) {
        LocalDate today = getUserLocalDate(user);
        dailyNotificationCounters.compute(user.getId(), (userId, counter) -> {
            if (counter == null || !counter.date.equals(today)) {
                return new DailyNotificationCounter(today, 1);
            }
            return new DailyNotificationCounter(today, counter.count + 1);
        });
    }

    private LocalDate getUserLocalDate(User user) {
        return getUserLocalDateTime(user).toLocalDate();
    }

    private OffsetDateTime getUserLocalDateTime(User user) {
        Integer timezoneOffset = user.getTimezoneOffset();
        if (timezoneOffset == null) {
            return OffsetDateTime.now();
        }
        return OffsetDateTime.now(ZoneOffset.ofHours(timezoneOffset));
    }

    private record DailyNotificationCounter(LocalDate date, int count) {
    }

    record NotificationContent(String title, String body) {
    }
}
