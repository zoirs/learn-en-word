package com.zoirs.learn_en_word.repository;

import com.zoirs.learn_en_word.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    User findByEmail(String email);


    User findByEmailAndId(String email, String id);

    @Modifying
    @Transactional
    @Query("""
            UPDATE User u
            SET u.firebaseToken = null
            WHERE u.id = :userId
              AND u.firebaseToken = :firebaseToken
            """)
    int clearFirebaseToken(
            @Param("userId") String userId,
            @Param("firebaseToken") String firebaseToken
    );

    @Query("""
            SELECT u
            FROM User u
            WHERE u.createdAt >= :createdAfter
              AND NOT EXISTS (
                  SELECT s.userId
                  FROM UserProgressSyncSnapshot s
                  WHERE s.userId = u.id
              )
            """)
    List<User> findRecentlyCreatedWithoutProgressSync(@Param("createdAfter") OffsetDateTime createdAfter);
}
