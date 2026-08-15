package com.zoirs.learn_en_word.controller;

import com.zoirs.learn_en_word.req.UserMessageReq;
import com.zoirs.learn_en_word.service.UserMessageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserMessageControllerTest {

    @InjectMocks
    private UserMessageController controller;

    @Mock
    private UserMessageService userMessageService;

    @Test
    void savePassesRequestFieldsToService() {
        UserMessageReq req = new UserMessageReq("user-1", "message text", "additional data");

        ResponseEntity<Void> response = controller.save(req);

        assertEquals(201, response.getStatusCode().value());
        verify(userMessageService).save("user-1", "message text", "additional data");
    }
}
