package com.zoirs.learn_en_word.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.zoirs.learn_en_word.entity.User;
import com.zoirs.learn_en_word.entity.UserProgressSyncSnapshot;
import com.zoirs.learn_en_word.model.MeaningEntity;
import com.zoirs.learn_en_word.model.TranslationEntity;
import com.zoirs.learn_en_word.repository.MeaningRepository;
import com.zoirs.learn_en_word.repository.UserProgressSyncSnapshotRepository;
import com.zoirs.learn_en_word.repository.UserRepository;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final int DEFAULT_DAILY_NOTIFICATIONS = 3;
    private static final int NOTIFICATION_START_HOUR = 10;
    private static final int NOTIFICATION_END_HOUR = 21;
    private final Map<String, DailyNotificationCounter> dailyNotificationCounters = new ConcurrentHashMap<>();

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private MeaningRepository meaningRepository;
    @Autowired
    private UserProgressSyncSnapshotRepository userProgressSyncSnapshotRepository;

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

    // @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void sendHourlyQuizzes() {
        OffsetDateTime activeSince = OffsetDateTime.now().minusWeeks(2);
        log.info("Started hourly quiz notification job, activeSince={}", activeSince);

        List<String> activeUserIds = userProgressSyncSnapshotRepository.findBySyncedAtGreaterThanEqual(activeSince)
                .stream()
                .map(UserProgressSyncSnapshot::getUserId)
                .toList();
        List<User> users = userRepository.findAllById(activeUserIds);

        int sentCount = 0;
        int errorCount = 0;
        for (User user : users) {
            if (StringUtils.isEmpty(user.getFirebaseToken())
                    || CollectionUtils.isEmpty(user.getNewWords())
                    || CollectionUtils.isEmpty(user.getLearningWords())) {
                continue;
            }
            int dailyNotificationLimit = resolveDailyNotificationLimit(user);
            if (dailyNotificationLimit <= 0 || isDailyNotificationLimitReached(user, dailyNotificationLimit)) {
                continue;
            }
            if (!isNotificationHour(user, dailyNotificationLimit)) {
                continue;
            }
            List<Integer> ids = user.getLearningWords().stream()
                    .skip(new Random().nextInt(user.getLearningWords().size()))
                    .limit(new Random().nextInt(2) + 1)
                    .toList();
            List<MeaningEntity> meanings = meaningRepository.findByExternalIdIn(ids);
            if (meanings.isEmpty()) {
                continue;
            }
            log.info("Sending notification to user: {} {}", user.getId(), user.getUsername());
            try {
                String body = meanings.stream().map(m -> {
                    TranslationEntity translation = m.getTranslationEntity();
                    StringBuilder wordTranslation = new StringBuilder();
                    if (StringUtils.isNotEmpty(m.getPrefix())) {
                        wordTranslation.append(m.getPrefix())
                                .append(" ")
                                .append(m.getText());
                    } else {
                        wordTranslation.append(StringUtils.capitalize(m.getText()));
                    }
                    wordTranslation.append(" - ").append(translation.getText());
                    return wordTranslation.toString();
                }).collect(Collectors.joining("\n"));
                String title = "Время повторить слова";

                sendNotification(user.getFirebaseToken(), title, body);
                incrementDailyNotificationCount(user);
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
        log.info("Finished hourly quiz notification job, sent={}, errors={}", sentCount, errorCount);
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
}
