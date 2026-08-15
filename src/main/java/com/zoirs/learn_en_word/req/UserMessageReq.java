package com.zoirs.learn_en_word.req;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UserMessageReq(
        @JsonProperty("user_id")
        String userId,
        String message,
        @JsonProperty("additional_info")
        String additionalInfo
) {
}
