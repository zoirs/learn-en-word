package com.zoirs.learn_en_word.service;

import com.zoirs.learn_en_word.entity.SubscriptionPaymentType;
import com.zoirs.learn_en_word.entity.User;
import com.zoirs.learn_en_word.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public User getOrCreateUser(String email, String id) {
        User user = userRepository.findByEmail(email);
        if (user != null) {
            return user;
        }
        User newUser = new User();
        if (!StringUtils.isEmpty(id)) {
            newUser.setId(id);
        }
        newUser.setEmail(email);
        newUser.setUsername(email);
        return userRepository.save(newUser);
    }

    public User get(String email, String id) {
        if (email == null && id == null) {
            return null;
        }

        User user;
        if (email != null && id != null) {
            user = userRepository.findByEmailAndId(email, id);
            if (user != null) {
                return user;
            }
        }
        if (email != null) {
            user = userRepository.findByEmail(email);
            if (user != null) {
                return user;
            }
        }
        return null;
    }

    @Transactional
    public User createOrUpdatePaymentType(String email, String id, SubscriptionPaymentType paymentType) {
        User user = userRepository.findByEmailAndId(email, id);
        if (user != null) {
            user.setPaymentType(paymentType);
            return userRepository.save(user);
        }
        log.info("Impossible case, User not found, creating new user");
        User newUser = new User();
        newUser.setEmail(email);
        if (!StringUtils.isEmpty(id)) {
            newUser.setId(id);
        }
        newUser.setPaymentType(paymentType);
        return userRepository.save(newUser);

    }
}
