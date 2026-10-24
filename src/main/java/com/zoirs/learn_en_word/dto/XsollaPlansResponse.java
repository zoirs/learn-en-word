package com.zoirs.learn_en_word.dto;

import java.util.List;

public record XsollaPlansResponse(String country, List<Plan> plans) {
    public record Plan(String planId, String name, Price price, Period period, int trialDays) {}
    public record Price(String amount, String currency) {}
    public record Period(int value, String unit) {}
}
