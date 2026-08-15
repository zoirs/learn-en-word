package com.zoirs.learn_en_word.service;

import com.zoirs.learn_en_word.entity.UserMessage;
import com.zoirs.learn_en_word.repository.UserMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class UserMessageService {

    private final UserMessageRepository userMessageRepository;

    @Transactional
    public void save(String userId, String message, String additionalInfo) {
        UserMessage userMessage = new UserMessage();
        userMessage.setUserId(userId);
        userMessage.setMessage(message);
        userMessage.setAdditionalInfo(additionalInfo);
        userMessage.setMessageDate(OffsetDateTime.now(ZoneOffset.UTC));
        userMessageRepository.save(userMessage);
    }
}
