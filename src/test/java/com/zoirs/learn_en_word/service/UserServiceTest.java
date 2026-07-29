package com.zoirs.learn_en_word.service;

import com.zoirs.learn_en_word.entity.User;
import com.zoirs.learn_en_word.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
    }
}
