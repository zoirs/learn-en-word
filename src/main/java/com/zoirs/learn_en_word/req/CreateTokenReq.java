package com.zoirs.learn_en_word.req;

public record CreateTokenReq(
        String email,
        String planId,
        String returnUrl,
        boolean sandbox
) {
}