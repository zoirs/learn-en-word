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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
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

    @Scheduled(cron = "0 0 10-22 * * *")
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
        int skippedCount = 0;
        int errorCount = 0;
        for (User user : users) {
            if (StringUtils.isEmpty(user.getFirebaseToken())
                    || CollectionUtils.isEmpty(user.getNewWords())
                    || CollectionUtils.isEmpty(user.getLearningWords())) {
                skippedCount++;
                continue;
            }
            List<Integer> ids = user.getLearningWords().stream()
                    .skip(new Random().nextInt(user.getLearningWords().size()))
                    .limit(new Random().nextInt(2) + 1)
                    .toList();
            List<MeaningEntity> meanings = meaningRepository.findByExternalIdIn(ids);
            if (meanings.isEmpty()) {
                skippedCount++;
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
        log.info("Finished hourly quiz notification job, activeSnapshots={}, activeUsers={}, sent={}, skipped={}, errors={}",
                activeUserIds.size(), users.size(), sentCount, skippedCount, errorCount);
    }
}
