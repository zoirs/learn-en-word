package com.zoirs.learn_en_word.req;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

public record UserProgressSyncCheckResponse(
        boolean exists,
        @JsonProperty("user_id")
        String userId,
        String email,
        @JsonProperty("schema_version")
        Integer schemaVersion,
        @JsonProperty("word_progress_count")
        Integer wordProgressCount,
        @JsonProperty("client_updated_at")
        OffsetDateTime clientUpdatedAt,
        @JsonProperty("server_updated_at")
        OffsetDateTime serverUpdatedAt
) {
}
