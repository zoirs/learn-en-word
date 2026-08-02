package com.zoirs.learn_en_word.repository;

import com.zoirs.learn_en_word.entity.UserProgressSyncSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserProgressSyncSnapshotRepository extends JpaRepository<UserProgressSyncSnapshot, String> {
}
