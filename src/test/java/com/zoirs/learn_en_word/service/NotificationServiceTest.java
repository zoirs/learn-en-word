package com.zoirs.learn_en_word.service;

import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.zoirs.learn_en_word.entity.User;
import com.zoirs.learn_en_word.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @InjectMocks
    private NotificationService notificationService;

    @Mock
    private UserRepository userRepository;

    @Test
    void handleFirebaseMessagingExceptionClearsUnregisteredToken() {
        User user = createUser();
        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
        when(exception.getMessagingErrorCode()).thenReturn(MessagingErrorCode.UNREGISTERED);
        when(userRepository.clearFirebaseToken("user-1", "stale-token")).thenReturn(1);

        notificationService.handleFirebaseMessagingException(user, "stale-token", exception);

        assertNull(user.getFirebaseToken());
        verify(userRepository).clearFirebaseToken("user-1", "stale-token");
    }

    @Test
    void handleFirebaseMessagingExceptionKeepsTokenForOtherErrors() {
        User user = createUser();
        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
        when(exception.getMessagingErrorCode()).thenReturn(MessagingErrorCode.INTERNAL);

        notificationService.handleFirebaseMessagingException(user, "stale-token", exception);

        assertEquals("stale-token", user.getFirebaseToken());
        verifyNoInteractions(userRepository);
    }

    @Test
    void sendHourlyQuizzesLoadsUsersByRecentActivity() {
        OffsetDateTime expectedActiveSince = OffsetDateTime.now().minusWeeks(2);
        when(userRepository.findRecentlyActive(any(OffsetDateTime.class)))
                .thenReturn(List.of());

        notificationService.sendHourlyQuizzes();

        ArgumentCaptor<OffsetDateTime> activeSinceCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(userRepository).findRecentlyActive(activeSinceCaptor.capture());
        assertTrue(activeSinceCaptor.getValue().isAfter(expectedActiveSince.minusSeconds(1)));
    }

    private User createUser() {
        User user = new User();
        user.setId("user-1");
        user.setFirebaseToken("stale-token");
        return user;
    }
}
