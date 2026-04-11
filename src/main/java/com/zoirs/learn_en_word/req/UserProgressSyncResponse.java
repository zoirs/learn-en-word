package com.zoirs.learn_en_word.req;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

public record UserProgressSyncResponse(
        String status,
        @JsonProperty("user_id")
        String userId,
        @JsonProperty("server_updated_at")
        OffsetDateTime serverUpdatedAt
) {
}
