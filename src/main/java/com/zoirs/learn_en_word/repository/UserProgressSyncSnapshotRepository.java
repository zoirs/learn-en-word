package com.zoirs.learn_en_word.repository;

import com.zoirs.learn_en_word.entity.UserProgressSyncSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface UserProgressSyncSnapshotRepository extends JpaRepository<UserProgressSyncSnapshot, String> {

    List<UserProgressSyncSnapshot> findBySyncedAtGreaterThanEqual(OffsetDateTime syncedAt);
}
