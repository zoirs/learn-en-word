package com.zoirs.learn_en_word.service;

import com.zoirs.learn_en_word.entity.SubscriptionPaymentType;
import com.zoirs.learn_en_word.entity.User;
import com.zoirs.learn_en_word.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public User getOrCreateUser(String email, String id) {
        if (StringUtils.isNotEmpty(email)) {
            User user = userRepository.findByEmail(email);
            if (user != null) {
                return user;
            }
        }
        Optional<User> userO = userRepository.findById(id);
        if (userO.isPresent()) {
            return userO.get();
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
        newUser.setUsername(email);
        newUser.setPaymentType(paymentType);
        return userRepository.save(newUser);

    }

    public void initUser(String id, String fireBaseToken, String timezone) {
        Optional<User> userO = userRepository.findById(id);
        if (userO.isPresent()) {
            User user = userO.get();
            log.info("User {} updated firebase token and timezone", id);
            boolean isNeedSave = false;
            if (StringUtils.isNotEmpty(fireBaseToken) && !fireBaseToken.equals(user.getFirebaseToken())) {
                user.setFirebaseToken(fireBaseToken);
                isNeedSave = true;
            }
            if (StringUtils.isNotEmpty(timezone) && !timezone.equals(user.getTimezone())) {
                user.setTimezone(timezone);
                isNeedSave = true;
            }
            if (isNeedSave) {
                userRepository.save(user);
            }
        } else {
            User newUser = new User();
            newUser.setId(id);
            newUser.setUsername(id);
            newUser.setFirebaseToken(fireBaseToken);
            userRepository.save(newUser);
        }
    }

    private static boolean isNeedUpdate(String fireBaseToken, String timezone, User user) {
        return StringUtils.isNotEmpty(fireBaseToken) && !fireBaseToken.equals(user.getFirebaseToken())
                || StringUtils.isNotEmpty(timezone) && !timezone.equals(user.getTimezone());
    }

    public void updateUserWords(String userId, Set<Integer> knownWords, Set<Integer> learningWords, Set<Integer> newWords) {
        Optional<User> userO = userRepository.findById(userId);
        if (userO.isPresent()) {
            log.info("User id present");
            User user = userO.get();
            user.setKnownWords(new HashSet<>(knownWords));
            user.setLearningWords(new HashSet<>(learningWords));
            user.setNewWords(new HashSet<>(newWords));
            userRepository.save(user);
        }
    }
}
