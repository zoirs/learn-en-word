package com.zoirs.learn_en_word.req;

public record InitSessionReq(
        String userId, String fireBaseToken, Integer timezoneOffset
) {
}