package com.zoirs.learn_en_word.controller;

import com.zoirs.learn_en_word.service.XsollaPlansService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.List;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class XsollaPlansControllerTest {
    private MockRestServiceServer server;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        XsollaPlansService service = new XsollaPlansService("123", "key", "456", List.of("monthly", "yearly"));
        server = MockRestServiceServer.bindTo((RestTemplate) ReflectionTestUtils.getField(service, "restTemplate")).build();
        XsollaController controller = new XsollaController();
        ReflectionTestUtils.setField(controller, "plansService", service);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"US", "RU", "UA"})
    void returnsRussianOffersRegardlessOfCountry(String country) throws Exception {
        expectPlan("monthly").andRespond(withSuccess(plan("monthly",
                "\"amount\":9.99,\"currency\":\"USD\",\"prices\":[{\"amount\":321.45,\"currency\":\"RUB\"}]", 1, 5), MediaType.APPLICATION_JSON));
        expectPlan("yearly").andRespond(withSuccess(plan("yearly",
                "\"amount\":3000,\"currency\":\"RUB\"", 12, 7), MediaType.APPLICATION_JSON));
        var request = get("/api/xsolla/plans");
        if (country != null) request.param("country", country);
        mvc.perform(request).andExpect(status().isOk())
                .andExpect(content().json("""
                        {"country":"RU","plans":[
                          {"planId":"monthly","name":"Premium monthly","price":{"amount":"321.45","currency":"RUB"},"period":{"value":1,"unit":"month"},"trialDays":5},
                          {"planId":"yearly","name":"Premium yearly","price":{"amount":"3000.00","currency":"RUB"},"period":{"value":12,"unit":"month"},"trialDays":7}
                        ]}
                        """, true));
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"[]", "null", "not-json",
            "[{\"external_id\":\"monthly\"}]"})
    void rejectsMissingOrMalformedPlans(String body) throws Exception {
        expectPlan("monthly").andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
        assertUnavailable();
    }

    @Test
    void rejectsWholeResponseWhenSecondPlanHasNoRubPrice() throws Exception {
        expectPlan("monthly").andRespond(withSuccess(plan("monthly", "\"amount\":250,\"currency\":\"RUB\"", 1, 3), MediaType.APPLICATION_JSON));
        expectPlan("yearly").andRespond(withSuccess(plan("yearly", "\"amount\":25,\"currency\":\"USD\"", 12, 3), MediaType.APPLICATION_JSON));
        assertUnavailable();
    }

    @Test
    void handlesUpstreamFailure() throws Exception {
        expectPlan("monthly").andRespond(withServerError());
        assertUnavailable();
    }

    @Test
    void handlesNetworkFailure() throws Exception {
        expectPlan("monthly").andRespond(withException(new IOException("Timeout")));
        assertUnavailable();
    }

    private org.springframework.test.web.client.ResponseActions expectPlan(String id) {
        return server.expect(requestTo("https://api.xsolla.com/merchant/v2/projects/456/subscriptions/plans?external_id=" + id + "&limit=2"))
                .andExpect(method(HttpMethod.GET)).andExpect(header("Authorization", "Basic MTIzOmtleQ=="));
    }

    private void assertUnavailable() throws Exception {
        mvc.perform(get("/api/xsolla/plans")).andExpect(status().isBadGateway())
                .andExpect(content().json("""
                        {"code":"XSOLLA_PLANS_UNAVAILABLE","message":"Не удалось загрузить предложения подписки"}
                        """, true));
        server.verify();
    }

    private String plan(String id, String prices, int months, int trialDays) {
        return """
                [{"id":42,"external_id":"%s","name":{"en":"Premium %s"},
                  "charge":{%s,"period":{"value":%d,"type":"month"}},
                  "trial":{"value":%d,"type":"day"}}]
                """.formatted(id, id, prices, months, trialDays);
    }
}
