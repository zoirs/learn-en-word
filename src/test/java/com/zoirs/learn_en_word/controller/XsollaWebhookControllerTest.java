package com.zoirs.learn_en_word.controller;

import com.zoirs.learn_en_word.entity.User;
import com.zoirs.learn_en_word.repository.UserRepository;
import com.zoirs.learn_en_word.service.UserService;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class XsollaWebhookControllerTest {

    @ParameterizedTest
    @ValueSource(strings = {"", ",\"email\":null", ",\"email\":\"\"", ",\"email\":\"  \"", ",\"email\":\"qq@qq.com\""})
    void validatesByIdRegardlessOfEmail(String emailField) throws Exception {
        assertEquals(204, handleValidation(emailField, Optional.of(new User())));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", ",\"email\":null", ",\"email\":\"\"", ",\"email\":\"qq@qq.com\""})
    void rejectsUnknownIdRegardlessOfEmail(String emailField) throws Exception {
        assertEquals(400, handleValidation(emailField, Optional.empty()));
    }

    @Test
    void savesEmailFromCheckout() throws Exception {
        User user = new User();
        user.setEmail("old@example.com");
        UserRepository repository = mock(UserRepository.class);
        assertEquals(204, handleValidation(",\"email\":\"  qq@qq.com  \"", Optional.of(user), repository));
        assertEquals("qq@qq.com", user.getEmail());
        verify(repository).save(user);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", ",\"email\":null", ",\"email\":\"\"", ",\"email\":\"  \""})
    void preservesEmailWhenNotProvided(String emailField) throws Exception {
        User user = new User();
        user.setEmail("old@example.com");
        UserRepository repository = mock(UserRepository.class);
        assertEquals(204, handleValidation(emailField, Optional.of(user), repository));
        assertEquals("old@example.com", user.getEmail());
        verify(repository, never()).save(any(User.class));
    }

    @Test
    void preservesEmailWhenCheckoutEmailBelongsToAnotherUser() throws Exception {
        User user = new User();
        user.setEmail("old@example.com");
        User otherUser = new User();
        otherUser.setId("other-id");
        UserRepository repository = mock(UserRepository.class);
        when(repository.findByEmail("qq@qq.com")).thenReturn(otherUser);
        assertEquals(204, handleValidation(",\"email\":\"qq@qq.com\"", Optional.of(user), repository));
        assertEquals("old@example.com", user.getEmail());
        verify(repository, never()).save(any(User.class));
    }

    private int handleValidation(String emailField, Optional<User> user) throws Exception {
        return handleValidation(emailField, user, mock(UserRepository.class));
    }

    private int handleValidation(String emailField, Optional<User> user, UserRepository repository) throws Exception {
        when(repository.findById("user-id")).thenReturn(user);
        UserService service = new UserService();
        ReflectionTestUtils.setField(service, "userRepository", repository);
        XsollaWebhookController controller = new XsollaWebhookController();
        ReflectionTestUtils.setField(controller, "userService", service);
        controller.projectSecret = "test-secret";

        String body = "{\"notification_type\":\"user_validation\",\"user\":{\"id\":\"user-id\""
                + emailField + "}}";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        request.addHeader("Authorization", "Signature " + DigestUtils.sha1Hex(body + controller.projectSecret));
        return controller.handle(request).getStatusCode().value();
    }
}
