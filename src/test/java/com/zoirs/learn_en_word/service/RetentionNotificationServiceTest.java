package com.zoirs.learn_en_word.service;

import com.zoirs.learn_en_word.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RetentionNotificationServiceTest {

    private final RetentionNotificationService retentionNotificationService =
            new RetentionNotificationService(null, null, null, null);

    @Test
    void buildNotificationOptionsWithoutThreeLearningWordsReturnsStaticOptions() {
        List<RetentionNotificationService.NotificationContent> options =
                retentionNotificationService.buildNotificationOptions(List.of("apple", "table"));

        assertEquals(List.of(
                new RetentionNotificationService.NotificationContent(
                        "Английский за 2 минуты",
                        "Повторите 5 слов — этого достаточно на сегодня"
                ),
                new RetentionNotificationService.NotificationContent(
                        "5 слов ждут повторения",
                        "Пройдите короткую тренировку, пока они не забылись"
                ),
                new RetentionNotificationService.NotificationContent(
                        "Есть свободная минута?",
                        "Её хватит, чтобы повторить несколько слов"
                )
        ), options);
    }

    @Test
    void buildNotificationOptionsWithThreeLearningWordsAddsDynamicOption() {
        List<RetentionNotificationService.NotificationContent> options =
                retentionNotificationService.buildNotificationOptions(List.of("apple", "table", "improve"));

        assertEquals(4, options.size());
        assertEquals(
                new RetentionNotificationService.NotificationContent(
                        "Есть свободная минутка?",
                        "Слова apple, table, improve, которые пора повторить"
                ),
                options.get(3)
        );
    }

    @Test
    void selectRandomNotificationAlwaysReturnsOneOfAvailableOptions() {
        List<String> learningWords = List.of("apple", "table", "improve");
        List<RetentionNotificationService.NotificationContent> options =
                retentionNotificationService.buildNotificationOptions(learningWords);

        for (int i = 0; i < 100; i++) {
            assertTrue(options.contains(retentionNotificationService.selectRandomNotification(learningWords)));
        }
    }

    @Test
    void resolveNotificationHourAssignsStableExperimentGroup() {
        User firstUser = new User();
        firstUser.setId("user-1");
        User secondUser = new User();
        secondUser.setId("user-2");

        int firstHour = retentionNotificationService.resolveNotificationHour(firstUser);
        int secondHour = retentionNotificationService.resolveNotificationHour(secondUser);

        assertTrue(firstHour == 18 || firstHour == 19);
        assertTrue(secondHour == 18 || secondHour == 19);
        assertNotEquals(firstHour, secondHour);
        assertEquals(firstHour, retentionNotificationService.resolveNotificationHour(firstUser));
    }

    @Test
    void onlyRetentionServiceIsScheduledAtMinuteTen() throws NoSuchMethodException {
        Scheduled oldSchedule = NotificationService.class
                .getMethod("sendHourlyQuizzes")
                .getAnnotation(Scheduled.class);
        Scheduled retentionSchedule = RetentionNotificationService.class
                .getMethod("sendRetentionNotifications")
                .getAnnotation(Scheduled.class);

        assertNull(oldSchedule);
        assertEquals("0 10 * * * *", retentionSchedule.cron());
    }

    @Test
    void wasCreatedBeforeTodayRejectsUserCreatedToday() {
        User user = new User();
        user.setTimezoneOffset(3);
        user.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));

        assertFalse(retentionNotificationService.wasCreatedBeforeToday(user));
    }

    @Test
    void wasCreatedBeforeTodayAcceptsUserFromPreviousDay() {
        User user = new User();
        user.setTimezoneOffset(3);
        user.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC).minusDays(2));

        assertTrue(retentionNotificationService.wasCreatedBeforeToday(user));
    }

    @Test
    void wasCreatedBeforeTodayAcceptsLegacyUserWithoutCreationDate() {
        User user = new User();
        user.setCreatedAt(null);

        assertTrue(retentionNotificationService.wasCreatedBeforeToday(user));
    }
}
