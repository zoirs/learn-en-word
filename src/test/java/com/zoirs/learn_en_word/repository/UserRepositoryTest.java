package com.zoirs.learn_en_word.repository;

import com.zoirs.learn_en_word.LearnEnWordApplication;
import com.zoirs.learn_en_word.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
@ContextConfiguration(classes = LearnEnWordApplication.class)
@Sql(statements = {
        """
        CREATE TABLE users (
            id VARCHAR(255) PRIMARY KEY,
            username VARCHAR(255) NOT NULL UNIQUE,
            email VARCHAR(255) UNIQUE,
            firebase_token VARCHAR(255),
            timezone_offset INTEGER,
            daily_notifications INTEGER,
            created_at TIMESTAMP WITH TIME ZONE,
            last_session_at TIMESTAMP WITH TIME ZONE,
            last_subscription_offer_notification_at TIMESTAMP WITH TIME ZONE,
            known_words INTEGER ARRAY,
            learning_words INTEGER ARRAY,
            new_words INTEGER ARRAY,
            payment_type INTEGER
        );
        """
})
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void findRecentlyActiveFallsBackToLastSessionWhenCreatedAtIsNull() {
        OffsetDateTime activeSince = OffsetDateTime.of(2026, 7, 19, 12, 0, 0, 0, ZoneOffset.UTC);

        userRepository.saveAll(List.of(
                user("recently-created", activeSince.plusDays(1), null),
                user("recent-session", null, activeSince.plusDays(1)),
                user("old-created", activeSince.minusDays(1), activeSince.plusDays(1))
        ));

        List<String> candidateIds = userRepository.findRecentlyActive(activeSince).stream()
                .map(User::getId)
                .sorted()
                .toList();

        assertEquals(List.of("recent-session", "recently-created"), candidateIds);
    }

    private User user(String id, OffsetDateTime createdAt, OffsetDateTime lastSessionAt) {
        User user = new User();
        user.setId(id);
        user.setUsername(id);
        user.setCreatedAt(createdAt);
        user.setLastSessionAt(lastSessionAt);
        return user;
    }
}
