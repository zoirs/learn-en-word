package com.zoirs.learn_en_word.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.zoirs.learn_en_word.entity.User;
import com.zoirs.learn_en_word.model.MeaningEntity;
import com.zoirs.learn_en_word.repository.MeaningRepository;
import com.zoirs.learn_en_word.repository.UserRepository;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Optional;
import java.util.Random;

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
    public void sendHourlyQuizzes() {
        List<User> users = userRepository.findAll();
        for (User user : users) {
            if (StringUtils.isEmpty(user.getFirebaseToken()) || CollectionUtils.isEmpty(user.getNewWords())) {
                continue;
            }
            Optional<Integer> idO = user.getNewWords().stream()
                    .skip(new Random().nextInt(user.getNewWords().size()))
                    .findFirst();
            Optional<MeaningEntity> meaningO = meaningRepository.findById(idO.get());
            if (meaningO.isEmpty()) {
                continue;
            }
            try {
                sendNotification(user.getFirebaseToken(), "Помнишь слово \"" + meaningO.get().getText() + "\"?", "Переводится \"" + meaningO.get().getTranslationEntity().getText() + "\"");
            } catch (Exception e) {
                log.error("Error sending notification to user: {}", user.getId(), e);
            }
        }
    }
}