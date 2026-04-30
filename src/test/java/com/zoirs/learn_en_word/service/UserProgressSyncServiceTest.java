package com.zoirs.learn_en_word.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zoirs.learn_en_word.entity.User;
import com.zoirs.learn_en_word.entity.UserProgressSyncSnapshot;
import com.zoirs.learn_en_word.repository.UserProgressSyncSnapshotRepository;
import com.zoirs.learn_en_word.repository.UserRepository;
import com.zoirs.learn_en_word.req.UserProgressSyncCheckResponse;
import com.zoirs.learn_en_word.req.UserProgressSyncReq;
import com.zoirs.learn_en_word.req.UserProgressSyncResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProgressSyncServiceTest {

    private static final String USER_ID = "sync-user-1";
    private static final String EMAIL = "sync-user-1@example.com";

    private UserProgressSyncService userProgressSyncService;

    @Mock
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProgressSyncSnapshotRepository snapshotRepository;

    @BeforeEach
    void setUp() {
        userProgressSyncService = new UserProgressSyncService(
                userService,
                userRepository,
                snapshotRepository,
                new ObjectMapper().registerModule(new JavaTimeModule())
        );
    }

    @Test
    void sync_ReturnsSameServerUpdatedAtAsCheck() {
        User user = new User();
        user.setId(USER_ID);
        user.setEmail(EMAIL);
        user.setUsername(EMAIL);

        AtomicReference<UserProgressSyncSnapshot> savedSnapshot = new AtomicReference<>();
        when(userService.getOrCreateUser(eq(EMAIL), eq(USER_ID))).thenReturn(user);
        when(snapshotRepository.findById(USER_ID))
                .thenAnswer(invocation -> Optional.ofNullable(savedSnapshot.get()));
        when(snapshotRepository.save(any(UserProgressSyncSnapshot.class)))
                .thenAnswer(invocation -> {
                    UserProgressSyncSnapshot snapshot = invocation.getArgument(0);
                    savedSnapshot.set(snapshot);
                    return snapshot;
                });
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        UserProgressSyncResponse syncResponse = userProgressSyncService.sync(syncRequest());
        UserProgressSyncCheckResponse checkResponse = userProgressSyncService.checkSync(USER_ID, null);

        assertEquals(syncResponse.serverUpdatedAt(), checkResponse.serverUpdatedAt());
        assertEquals(0, syncResponse.serverUpdatedAt().getNano() % 1_000_000);
    }

    private UserProgressSyncReq syncRequest() {
        return new UserProgressSyncReq(
                1,
                new UserProgressSyncReq.SyncBlock(
                        USER_ID,
                        OffsetDateTime.parse("2026-04-29T10:15:30Z")
                ),
                new UserProgressSyncReq.UserBlock(
                        EMAIL,
                        "Sync User",
                        1,
                        true,
                        "none"
                ),
                new UserProgressSyncReq.SettingsBlock(
                        10,
                        20,
                        30,
                        3,
                        1
                ),
                List.of(),
                List.of()
        );
    }
}
