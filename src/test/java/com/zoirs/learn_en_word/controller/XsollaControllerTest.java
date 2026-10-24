package com.zoirs.learn_en_word.controller;

import com.zoirs.learn_en_word.entity.User;
import com.zoirs.learn_en_word.req.CreateTokenReq;
import com.zoirs.learn_en_word.service.UserService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class XsollaControllerTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "user@example.com"})
    void createsTokenWithOptionalEmail(String email) {
        assertCreatesToken(email, null, null);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "UA,USD",
            "US,NULL",
            "NULL,EUR",
            "'',''",
            "'  ','  '"
    }, nullValues = "NULL")
    void keepsRussianSettingsRegardlessOfRequestCountryAndCurrency(String country, String currency) {
        assertCreatesToken(null, country, currency);
    }

    private void assertCreatesToken(String email, String country, String currency) {
        XsollaController controller = new XsollaController();
        UserService service = mock(UserService.class);
        User user = new User();
        user.setId("android-id:test");
        when(service.getOrCreateUser(email, user.getId())).thenReturn(user);
        ReflectionTestUtils.setField(controller, "userService", service);
        ReflectionTestUtils.setField(controller, "merchantId", "123");
        ReflectionTestUtils.setField(controller, "apiKey", "test-key");
        ReflectionTestUtils.setField(controller, "projectId", "456");

        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(controller, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        var expectation = server.expect(requestTo("https://api.xsolla.com/merchant/v2/merchants/123/token"))
                .andExpect(jsonPath("$.user.id.value").value(user.getId()))
                .andExpect(jsonPath("$.user.country.value").value("RU"))
                .andExpect(jsonPath("$.settings.currency").value("RUB"));
        if (email == null || email.isBlank()) {
            expectation.andExpect(jsonPath("$.user.email").doesNotExist());
        } else {
            expectation.andExpect(jsonPath("$.user.email.value").value(email));
        }
        expectation.andRespond(withSuccess("{\"token\":\"test-token\"}", MediaType.APPLICATION_JSON));

        var response = controller.createToken(new CreateTokenReq(user.getId(), email, "plan",
                "https://example.com/return", true, country, currency));

        assertEquals("test-token", response.getBody().get("token"));
        server.verify();
    }
}
