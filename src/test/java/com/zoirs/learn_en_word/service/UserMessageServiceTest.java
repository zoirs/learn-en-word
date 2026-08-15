package com.zoirs.learn_en_word.service;

import com.zoirs.learn_en_word.entity.UserMessage;
import com.zoirs.learn_en_word.repository.UserMessageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserMessageServiceTest {

    @InjectMocks
    private UserMessageService userMessageService;

    @Mock
    private UserMessageRepository userMessageRepository;

    @Test
    void saveStoresMessageWithCurrentDate() {
        OffsetDateTime beforeSave = OffsetDateTime.now(ZoneOffset.UTC);

        userMessageService.save("user-1", "message text", "additional data");

        OffsetDateTime afterSave = OffsetDateTime.now(ZoneOffset.UTC);
        ArgumentCaptor<UserMessage> captor = ArgumentCaptor.forClass(UserMessage.class);
        verify(userMessageRepository).save(captor.capture());

        UserMessage savedMessage = captor.getValue();
        assertEquals("user-1", savedMessage.getUserId());
        assertEquals("message text", savedMessage.getMessage());
        assertEquals("additional data", savedMessage.getAdditionalInfo());
        assertFalse(savedMessage.getMessageDate().isBefore(beforeSave));
        assertFalse(savedMessage.getMessageDate().isAfter(afterSave));
    }
}
