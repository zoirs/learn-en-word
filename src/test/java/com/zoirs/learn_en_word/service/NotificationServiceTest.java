package com.zoirs.learn_en_word.service;

import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.zoirs.learn_en_word.entity.SubscriptionPaymentType;
import com.zoirs.learn_en_word.entity.User;
import com.zoirs.learn_en_word.model.MeaningEntity;
import com.zoirs.learn_en_word.model.TranslationEntity;
import com.zoirs.learn_en_word.repository.MeaningRepository;
import com.zoirs.learn_en_word.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Spy
    @InjectMocks
    private NotificationService notificationService;

    @Mock
    private UserRepository userRepository;
    @Mock
    private MeaningRepository meaningRepository;

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
    void buildPaidNotificationContentIncludesThreeWords() {
        NotificationService.NotificationContent notification = notificationService
                .buildNotificationContent(List.of(
                        createMeaning("apple", "яблоко"),
                        createMeaning("table", "стол"),
                        createMeaning("window", "окно")
                ), true)
                .orElseThrow();

        assertEquals(
                new NotificationService.NotificationContent(
                        "Время повторить слова",
                        "Apple - яблоко\nTable - стол\nWindow - окно"
                ),
                notification
        );
    }

    @Test
    void buildSubscriptionOfferNotificationContentIncludesThreeWordsAndPrompt() {
        NotificationService.NotificationContent notification = notificationService
                .buildNotificationContent(List.of(
                        createMeaning("apple", "яблоко"),
                        createMeaning("table", "стол"),
                        createMeaning("window", "окно")
                ), false)
                .orElseThrow();

        assertEquals(
                new NotificationService.NotificationContent(
                        "Повторяйте слова, не открывая приложение",
                        "Apple - яблоко\nTable - стол\nWindow - окно\n—\nТакие уведомления доступны по подписке"
                ),
                notification
        );
    }

    @Test
    void selectNotificationMeaningsExcludesPhrasesBeforeApplyingLimit() {
        User user = createNotificationUser("user-1", SubscriptionPaymentType.REVENUE_CAT);
        user.setLearningWords(Set.of(1, 2, 3, 4, 5));
        when(meaningRepository.findByExternalIdIn(any())).thenReturn(List.of(
                createMeaning("ball and chain", "ноша", "ph"),
                createMeaning("apple", "яблоко", "n"),
                createMeaning("quickly", "быстро", "r"),
                createMeaning("useful", "полезный", "j"),
                createMeaning("learn", "учить", "v")
        ));

        List<MeaningEntity> meanings = notificationService.selectNotificationMeanings(user);

        assertEquals(3, meanings.size());
        assertTrue(meanings.stream().noneMatch(meaning -> "ph".equals(meaning.getPartOfSpeechCode())));
        assertTrue(meanings.stream().allMatch(
                meaning -> Set.of("j", "n", "r", "v").contains(meaning.getPartOfSpeechCode())
        ));
    }

    @Test
    void buildNotificationContentOmitsAWordThatDoesNotFit() {
        NotificationService.NotificationContent notification = notificationService
                .buildNotificationContent(List.of(
                        createMeaning("apple", "яблоко"),
                        createMeaning("table", "стол"),
                        createMeaning("oversized", "я".repeat(4_000))
                ), true)
                .orElseThrow();

        assertEquals(
                new NotificationService.NotificationContent(
                        "Время повторить слова",
                        "Apple - яблоко\nTable - стол"
                ),
                notification
        );
    }

    @Test
    void scheduledJobLoadsUsersByRecentActivity() {
        OffsetDateTime expectedActiveSince = OffsetDateTime.now().minusWeeks(2);
        when(userRepository.findRecentlyActive(any(OffsetDateTime.class)))
                .thenReturn(List.of());

        notificationService.sendWordReviewNotifications();

        ArgumentCaptor<OffsetDateTime> activeSinceCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(userRepository).findRecentlyActive(activeSinceCaptor.capture());
        assertTrue(activeSinceCaptor.getValue().isAfter(expectedActiveSince.minusSeconds(1)));
    }

    @Test
    void scheduledJobRunsAtTheStartOfEveryHour() throws NoSuchMethodException {
        Scheduled schedule = NotificationService.class
                .getMethod("sendWordReviewNotifications")
                .getAnnotation(Scheduled.class);

        assertEquals("0 0 * * * *", schedule.cron());
    }

    @Test
    void scheduledJobSendsSubscriptionOfferToUsersWithoutPaidSubscription() throws Exception {
        User userWithoutSubscription = createNotificationUser("user-without-subscription", null);
        User userWithFreeSubscription = createNotificationUser("user-with-free-subscription", SubscriptionPaymentType.NONE);
        userWithFreeSubscription.setLastSubscriptionOfferNotificationAt(
                OffsetDateTime.now(ZoneOffset.UTC).minusDays(3)
        );
        when(userRepository.findRecentlyActive(any(OffsetDateTime.class)))
                .thenReturn(List.of(userWithoutSubscription, userWithFreeSubscription));
        when(meaningRepository.findByExternalIdIn(any())).thenReturn(List.of(
                createMeaning("apple", "яблоко"),
                createMeaning("table", "стол"),
                createMeaning("window", "окно")
        ));
        doNothing().when(notificationService)
                .sendNotification(any(User.class), anyString(), anyString());

        notificationService.sendWordReviewNotifications();

        verify(notificationService, times(2)).sendNotification(
                any(User.class),
                anyString(),
                anyString()
        );
        assertNotNull(userWithoutSubscription.getLastSubscriptionOfferNotificationAt());
        assertNotNull(userWithFreeSubscription.getLastSubscriptionOfferNotificationAt());
    }

    @Test
    void scheduledJobDoesNotRepeatSubscriptionOfferBeforeThreeDays() {
        User freeUser = createNotificationUser("free-user", SubscriptionPaymentType.NONE);
        freeUser.setLastSubscriptionOfferNotificationAt(OffsetDateTime.now(ZoneOffset.UTC).minusDays(2));
        when(userRepository.findRecentlyActive(any(OffsetDateTime.class)))
                .thenReturn(List.of(freeUser));

        notificationService.sendWordReviewNotifications();

        verifyNoInteractions(meaningRepository);
    }

    @Test
    void scheduledJobProcessesBothPaidSubscriptionTypes() throws Exception {
        User revenueCatUser = createNotificationUser("revenue-cat-user", SubscriptionPaymentType.REVENUE_CAT);
        User xsollaUser = createNotificationUser("xsolla-user", SubscriptionPaymentType.XSOLLA);
        when(userRepository.findRecentlyActive(any(OffsetDateTime.class)))
                .thenReturn(List.of(revenueCatUser, xsollaUser));
        when(meaningRepository.findByExternalIdIn(any())).thenReturn(List.of(
                createMeaning("apple", "яблоко"),
                createMeaning("table", "стол"),
                createMeaning("window", "окно")
        ));
        doNothing().when(notificationService)
                .sendNotification(any(User.class), anyString(), anyString());

        notificationService.sendWordReviewNotifications();

        verify(meaningRepository, times(2)).findByExternalIdIn(any());
        verify(notificationService, times(2)).sendNotification(
                any(User.class),
                anyString(),
                anyString()
        );
        assertNull(revenueCatUser.getLastSubscriptionOfferNotificationAt());
        assertNull(xsollaUser.getLastSubscriptionOfferNotificationAt());
    }

    private User createUser() {
        User user = new User();
        user.setId("user-1");
        user.setFirebaseToken("stale-token");
        return user;
    }

    private User createNotificationUser(String id, SubscriptionPaymentType paymentType) {
        User user = new User();
        user.setId(id);
        user.setFirebaseToken("firebase-token");
        user.setPaymentType(paymentType);
        user.setNewWords(Set.of(1));
        user.setLearningWords(Set.of(1, 2, 3));
        user.setDailyNotifications(12);
        user.setTimezoneOffset(15 - OffsetDateTime.now(ZoneOffset.UTC).getHour());
        return user;
    }

    private MeaningEntity createMeaning(String word, String translationText) {
        return createMeaning(word, translationText, "n");
    }

    private MeaningEntity createMeaning(String word, String translationText, String partOfSpeechCode) {
        MeaningEntity meaning = new MeaningEntity();
        meaning.setText(word);
        meaning.setPartOfSpeechCode(partOfSpeechCode);
        TranslationEntity translation = new TranslationEntity();
        translation.setText(translationText);
        meaning.setTranslation(translation);
        return meaning;
    }
}
