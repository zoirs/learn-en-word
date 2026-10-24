package com.zoirs.learn_en_word.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.zoirs.learn_en_word.dto.XsollaPlansResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class XsollaPlansService {
    private final RestTemplate restTemplate;
    private final String merchantId;
    private final String apiKey;
    private final String projectId;
    private final List<String> planIds;

    public XsollaPlansService(@Value("${xsolla.XSOLLA_MERCHANT_ID}") String merchantId,
                              @Value("${xsolla.XSOLLA_API_KEY}") String apiKey,
                              @Value("${xsolla.XSOLLA_PROJECT_ID}") String projectId,
                              @Value("${xsolla.plan-ids:}") List<String> planIds) {
        this.merchantId = merchantId;
        this.apiKey = apiKey;
        this.projectId = projectId;
        this.planIds = planIds;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        restTemplate = new RestTemplate(factory);
    }

    public XsollaPlansResponse getPlans() {
        require(planIds != null && !planIds.isEmpty());
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(merchantId, apiKey);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        List<XsollaPlansResponse.Plan> plans = new ArrayList<>();
        for (String configuredId : planIds) {
            require(configuredId != null && !configuredId.isBlank());
            String planId = configuredId.trim();
            UpstreamPlan[] response = restTemplate.exchange(
                    "https://api.xsolla.com/merchant/v2/projects/{projectId}/subscriptions/plans?external_id={planId}&limit=2",
                    HttpMethod.GET, new HttpEntity<>(headers), UpstreamPlan[].class, projectId, planId).getBody();
            require(response != null && response.length == 1 && response[0] != null);
            UpstreamPlan plan = response[0];
            require(planId.equals(plan.externalId()) && plan.charge() != null);
            Charge charge = plan.charge();
            UpstreamPrice price = null;
            if (charge.prices() != null) {
                price = charge.prices().stream().filter(p -> p != null && "RUB".equals(p.currency()))
                        .findFirst().orElse(null);
            }
            if (price == null && "RUB".equals(charge.currency())) {
                price = new UpstreamPrice(charge.amount(), charge.currency());
            }
            require(price != null && price.amount() != null && price.amount().signum() >= 0);
            TimePeriod period = charge.period();
            require(period != null && period.value() != null && period.type() != null);
            require(switch (period.type()) {
                case "day" -> period.value() >= 1 && period.value() <= 366;
                case "month" -> period.value() >= 1 && period.value() <= 12;
                case "lifetime" -> period.value() == 0;
                default -> false;
            });
            require(plan.name() != null);
            String name = plan.name().get("en");
            if (name == null || name.isBlank()) name = plan.name().get("ru");
            require(name != null && !name.isBlank());
            int trialDays = 0;
            if (plan.trial() != null) {
                require("day".equals(plan.trial().type()) && plan.trial().value() != null
                        && plan.trial().value() >= 0);
                trialDays = plan.trial().value();
            }
            BigDecimal amount = price.amount();
            String formattedAmount = amount.setScale(Math.max(2, amount.scale())).toPlainString();
            plans.add(new XsollaPlansResponse.Plan(planId, name,
                    new XsollaPlansResponse.Price(formattedAmount, price.currency()),
                    new XsollaPlansResponse.Period(period.value(), period.type()), trialDays));
        }
        return new XsollaPlansResponse("RU", plans);
    }

    private static void require(boolean condition) {
        if (!condition) throw new IllegalArgumentException("Incomplete Xsolla subscription plan data");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UpstreamPlan(@JsonProperty("external_id") String externalId, Map<String, String> name,
                               Charge charge, TimePeriod trial) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Charge(BigDecimal amount, String currency, List<UpstreamPrice> prices, TimePeriod period) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UpstreamPrice(BigDecimal amount, String currency) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TimePeriod(Integer value, String type) {}
}
