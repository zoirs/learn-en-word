package com.zoirs.learn_en_word.req;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.List;

public record UserProgressSyncReq(
        @JsonProperty("schema_version")
        Integer schemaVersion,
        SyncBlock sync,
        UserBlock user,
        SettingsBlock settings,
        @JsonProperty("word_progress")
        List<WordProgressItem> wordProgress,
        @JsonProperty("daily_sessions")
        List<DailySessionItem> dailySessions
) {
    public record SyncBlock(
            @JsonProperty("user_id")
            String userId,
            @JsonProperty("client_updated_at")
            OffsetDateTime clientUpdatedAt
    ) {
    }

    public record UserBlock(
            String email,
            String name,
            Integer level,
            @JsonProperty("is_level_assessment_complete")
            Boolean isLevelAssessmentComplete,
            @JsonProperty("subscription_status")
            String subscriptionStatus
    ) {
    }

    public record SettingsBlock(
            @JsonProperty("words_per_session_new")
            Integer wordsPerSessionNew,
            @JsonProperty("words_per_session_review")
            Integer wordsPerSessionReview,
            @JsonProperty("words_per_session_check")
            Integer wordsPerSessionCheck,
            @JsonProperty("daily_session_limit")
            Integer dailySessionLimit,
            @JsonProperty("daily_notifications")
            Integer dailyNotifications
    ) {
    }

    public record WordProgressItem(
            @JsonProperty("meaning_id")
            Integer meaningId,
            String status,
            @JsonProperty("last_reviewed_at")
            OffsetDateTime lastReviewedAt,
            @JsonProperty("correct_answers")
            Integer correctAnswers,
            @JsonProperty("added_at")
            OffsetDateTime addedAt,
            @JsonProperty("incorrect_streak")
            Integer incorrectStreak,
            @JsonProperty("current_interval_days")
            Integer currentIntervalDays,
            @JsonProperty("next_review_time")
            OffsetDateTime nextReviewTime,
            @JsonProperty("consecutive_correct_session")
            Integer consecutiveCorrectSession,
            @JsonProperty("consecutive_correct_global")
            Integer consecutiveCorrectGlobal,
            @JsonProperty("last_review_time")
            OffsetDateTime lastReviewTime,
            @JsonProperty("total_reviews")
            Integer totalReviews,
            @JsonProperty("last_results")
            List<Boolean> lastResults
    ) {
    }

    public record DailySessionItem(
            String id,
            @JsonProperty("traine_type")
            String traineType,
            @JsonProperty("day_local")
            String dayLocal,
            Integer progress,
            @JsonProperty("meaning_ids")
            List<Integer> meaningIds
    ) {
    }
}
