package com.zoirs.learn_en_word.service;

import com.zoirs.learn_en_word.entity.SubscriptionPaymentType;
import com.zoirs.learn_en_word.entity.User;
import com.zoirs.learn_en_word.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Test
    void initUser_WhenDailyNotificationsIsNull_StoresRequestedValue() {
        User user = new User();
        user.setId("user-1");
        user.setDailyNotifications(null);
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        userService.initUser("user-1", null, null, 5);

        assertEquals(5, user.getDailyNotifications());
        verify(userRepository).save(user);
    }

    @Test
    void initUser_WhenUserDataIsUnchanged_UpdatesLastSessionTime() {
        OffsetDateTime previousSessionAt = OffsetDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        User user = new User();
        user.setId("user-1");
        user.setDailyNotifications(5);
        user.setLastSessionAt(previousSessionAt);
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        userService.initUser("user-1", null, null, 5);

        assertNotNull(user.getLastSessionAt());
        assertTrue(user.getLastSessionAt().isAfter(previousSessionAt));
        verify(userRepository).save(user);
    }

    @Test
    void initUser_WhenUserDoesNotExist_StoresTimezoneOffset() {
        when(userRepository.findById("user-1")).thenReturn(Optional.empty());

        userService.initUser("user-1", "firebase-token", 3, 5);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals("user-1", savedUser.getId());
        assertEquals("firebase-token", savedUser.getFirebaseToken());
        assertEquals(3, savedUser.getTimezoneOffset());
        assertEquals(5, savedUser.getDailyNotifications());
        assertNotNull(savedUser.getCreatedAt());
        assertNotNull(savedUser.getLastSessionAt());
    }

    @Test
    void createOrUpdatePaymentType_WhenEmailIsNull_UsesIdAsUsername() {
        String id = "$RCAnonymousID:083a7c4b512d4d69b6cc563e99951f67";
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        userService.createOrUpdatePaymentType(null, id, SubscriptionPaymentType.REVENUE_CAT);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals(id, savedUser.getId());
        assertEquals(id, savedUser.getUsername());
        assertEquals(SubscriptionPaymentType.REVENUE_CAT, savedUser.getPaymentType());
    }

    @Test
    void createOrUpdatePaymentType_WhenEmailIsNull_UpdatesUserFoundById() {
        String id = "$RCAnonymousID:083a7c4b512d4d69b6cc563e99951f67";
        User user = new User();
        user.setId(id);
        user.setUsername(id);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        userService.createOrUpdatePaymentType(null, id, SubscriptionPaymentType.REVENUE_CAT);

        assertEquals(SubscriptionPaymentType.REVENUE_CAT, user.getPaymentType());
        verify(userRepository).save(user);
    }
}
