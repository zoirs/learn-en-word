package com.zoirs.learn_en_word.repository;

import com.zoirs.learn_en_word.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    User findByEmail(String email);


    User findByEmailAndId(String email, String id);
}
