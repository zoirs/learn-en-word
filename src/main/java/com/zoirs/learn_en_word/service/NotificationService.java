package com.zoirs.learn_en_word.service;

import com.google.common.base.MoreObjects;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.zoirs.learn_en_word.entity.User;
import com.zoirs.learn_en_word.model.ExampleEntity;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
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

    @Scheduled(cron = "0 0 10-22 * * *")
    @Transactional
    public void sendHourlyQuizzes() {
        List<User> users = userRepository.findAll();
        for (User user : users) {
            if (StringUtils.isEmpty(user.getFirebaseToken()) || CollectionUtils.isEmpty(user.getNewWords())) {
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
                        wordTranslation.append(StringUtils.capitalize(m.getPrefix()))
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
            } catch (Exception e) {
                log.error("Error sending notification to user: {}", user.getId(), e);
            }
        }
    }
}