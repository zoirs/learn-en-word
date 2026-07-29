package com.zoirs.learn_en_word.service;

import com.zoirs.learn_en_word.entity.User;
import com.zoirs.learn_en_word.entity.UserProgressSyncSnapshot;
import com.zoirs.learn_en_word.model.MeaningEntity;
import com.zoirs.learn_en_word.repository.MeaningRepository;
import com.zoirs.learn_en_word.repository.UserProgressSyncSnapshotRepository;
import com.zoirs.learn_en_word.repository.UserRepository;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RetentionNotificationService {

    private static final Logger log = LoggerFactory.getLogger(RetentionNotificationService.class);
    private static final int EARLY_NOTIFICATION_HOUR = 18;
    private static final int LATE_NOTIFICATION_HOUR = 19;
    private static final int DYNAMIC_NOTIFICATION_WORDS_COUNT = 3;
    private static final List<NotificationContent> STATIC_NOTIFICATIONS = List.of(
            new NotificationContent(
                    "Английский за 2 минуты",
                    "Повторите 5 слов — этого достаточно на сегодня"
            ),
            new NotificationContent(
                    "5 слов ждут повторения",
                    "Пройдите короткую тренировку, пока они не забылись"
            ),
            new NotificationContent(
                    "Есть свободная минута?",
                    "Её хватит, чтобы повторить несколько слов"
            )
    );

    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final MeaningRepository meaningRepository;
    private final UserProgressSyncSnapshotRepository userProgressSyncSnapshotRepository;
    private final Map<String, LocalDate> lastNotificationDates = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public RetentionNotificationService(
            NotificationService notificationService,
            UserRepository userRepository,
            MeaningRepository meaningRepository,
            UserProgressSyncSnapshotRepository userProgressSyncSnapshotRepository
    ) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.meaningRepository = meaningRepository;
        this.userProgressSyncSnapshotRepository = userProgressSyncSnapshotRepository;
    }

    @Scheduled(cron = "0 10 * * * *")
    @Transactional
    public void sendRetentionNotifications() {
        OffsetDateTime activeSince = OffsetDateTime.now().minusWeeks(2);
        log.info("Started retention notification job, activeSince={}", activeSince);

        List<String> activeUserIds = userProgressSyncSnapshotRepository.findBySyncedAtGreaterThanEqual(activeSince)
                .stream()
                .map(UserProgressSyncSnapshot::getUserId)
                .toList();
        List<User> users = userRepository.findAllById(activeUserIds);

        int sentCount = 0;
        int errorCount = 0;
        for (User user : users) {
            if (!isEligibleForNotification(user)) {
                continue;
            }

            try {
                List<String> learningWords = getRandomLearningWords(user);
                NotificationContent notification = selectRandomNotification(learningWords);
                notificationService.sendNotification(
                        user.getFirebaseToken(),
                        notification.title(),
                        notification.body()
                );
                lastNotificationDates.put(user.getId(), getUserLocalDateTime(user).toLocalDate());
                sentCount++;
                log.info(
                        "Retention notification sent to user: {} {}, experimentHour={}",
                        user.getId(),
                        user.getUsername(),
                        resolveNotificationHour(user)
                );
            } catch (Exception e) {
                errorCount++;
                if (e.toString().contains("Requested entity was not found")) {
                    log.error("Error sending retention notification to user: {} {}", user.getId(), e.toString());
                } else {
                    log.error("Error sending retention notification to user: {}", user.getId(), e);
                }
            }
        }

        log.info("Finished retention notification job, sent={}, errors={}", sentCount, errorCount);
    }

    private boolean isEligibleForNotification(User user) {
        return StringUtils.isNotEmpty(user.getFirebaseToken())
                && !CollectionUtils.isEmpty(user.getLearningWords())
                && isNotificationEnabled(user)
                && wasCreatedBeforeToday(user)
                && isNotificationTime(user)
                && !wasNotificationSentToday(user);
    }

    private boolean isNotificationEnabled(User user) {
        return user.getDailyNotifications() == null || user.getDailyNotifications() > 0;
    }

    private boolean isNotificationTime(User user) {
        return getUserLocalDateTime(user).getHour() == resolveNotificationHour(user);
    }

    int resolveNotificationHour(User user) {
        return Math.floorMod(user.getId().hashCode(), 2) == 0
                ? EARLY_NOTIFICATION_HOUR
                : LATE_NOTIFICATION_HOUR;
    }

    boolean wasCreatedBeforeToday(User user) {
        if (user.getCreatedAt() == null) {
            return true;
        }

        OffsetDateTime userLocalDateTime = getUserLocalDateTime(user);
        LocalDate createdDate = user.getCreatedAt()
                .withOffsetSameInstant(userLocalDateTime.getOffset())
                .toLocalDate();
        return createdDate.isBefore(userLocalDateTime.toLocalDate());
    }

    private boolean wasNotificationSentToday(User user) {
        LocalDate lastNotificationDate = lastNotificationDates.get(user.getId());
        return getUserLocalDateTime(user).toLocalDate().equals(lastNotificationDate);
    }

    private List<String> getRandomLearningWords(User user) {
        if (user.getLearningWords().size() < DYNAMIC_NOTIFICATION_WORDS_COUNT) {
            return List.of();
        }

        List<Integer> learningWordIds = new ArrayList<>(user.getLearningWords());
        Collections.shuffle(learningWordIds, random);
        List<Integer> selectedIds = learningWordIds.stream()
                .limit(DYNAMIC_NOTIFICATION_WORDS_COUNT)
                .toList();

        return meaningRepository.findByExternalIdIn(selectedIds).stream()
                .map(this::formatLearningWord)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .limit(DYNAMIC_NOTIFICATION_WORDS_COUNT)
                .toList();
    }

    private String formatLearningWord(MeaningEntity meaning) {
        if (StringUtils.isBlank(meaning.getText())) {
            return "";
        }
        if (StringUtils.isNotBlank(meaning.getPrefix())) {
            return meaning.getPrefix() + " " + meaning.getText();
        }
        return meaning.getText();
    }

    NotificationContent selectRandomNotification(List<String> learningWords) {
        List<NotificationContent> notifications = buildNotificationOptions(learningWords);
        return notifications.get(random.nextInt(notifications.size()));
    }

    List<NotificationContent> buildNotificationOptions(List<String> learningWords) {
        List<NotificationContent> notifications = new ArrayList<>(STATIC_NOTIFICATIONS);
        if (learningWords.size() == DYNAMIC_NOTIFICATION_WORDS_COUNT) {
            notifications.add(new NotificationContent(
                    "Есть свободная минутка?",
                    "Слова " + String.join(", ", learningWords) + ", которые пора повторить"
            ));
        }
        return notifications;
    }

    private OffsetDateTime getUserLocalDateTime(User user) {
        Integer timezoneOffset = user.getTimezoneOffset();
        if (timezoneOffset == null) {
            return OffsetDateTime.now();
        }
        return OffsetDateTime.now(ZoneOffset.ofHours(timezoneOffset));
    }

    record NotificationContent(String title, String body) {
    }
}
