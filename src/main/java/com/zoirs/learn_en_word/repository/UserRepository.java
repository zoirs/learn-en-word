package com.zoirs.learn_en_word.repository;

import com.zoirs.learn_en_word.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    User findByEmail(String email);


    User findByEmailAndId(String email, String id);

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
