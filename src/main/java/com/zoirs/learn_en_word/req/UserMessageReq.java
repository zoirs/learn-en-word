package com.zoirs.learn_en_word.req;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

public record UserMessageReq(
        @NotNull
        @JsonProperty("user_id")
        String userId,
        @NotNull
        String message,
        @NotNull
        @JsonProperty("additional_info")
        String additionalInfo
) {
}
