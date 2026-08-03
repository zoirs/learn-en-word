package com.zoirs.learn_en_word.controller;

import com.zoirs.learn_en_word.entity.User;
import com.zoirs.learn_en_word.repository.UserRepository;
import com.zoirs.learn_en_word.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class NotificationTestControllerTest {

    @InjectMocks
    private NotificationTestController controller;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @Test
    void sendTestNotificationSendsNotificationToRequestedUser() throws Exception {
        User user = new User();
        user.setId("user-1");
        user.setFirebaseToken("firebase-token");
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        ResponseEntity<Boolean> response = controller.sendTestNotification("user-1");

        assertEquals(Boolean.TRUE, response.getBody());
        verify(notificationService).sendNotification(
                user,
                "Тестовое уведомление",
                "Это тестовое уведомление для проверки доставки."
        );
    }

    @Test
    void sendTestNotificationReturnsNotFoundWhenUserDoesNotExist() {
        when(userRepository.findById("missing-user")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.sendTestNotification("missing-user")
        );

        assertEquals(NOT_FOUND, exception.getStatusCode());
        verifyNoInteractions(notificationService);
    }

    @Test
    void sendTestNotificationReturnsBadRequestWhenFirebaseTokenIsMissing() {
        User user = new User();
        user.setId("user-1");
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.sendTestNotification("user-1")
        );

        assertEquals(BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(notificationService);
    }
}
