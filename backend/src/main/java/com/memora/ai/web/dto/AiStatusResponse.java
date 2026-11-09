package com.memora.ai.web.dto;

import com.memora.ai.config.AiProperties;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * What the client is allowed to know about the AI provider.
 *
 * <p>Carries no credential. The client uses {@code available} to decide whether
 * to offer content-generation affordances at all, instead of surfacing a button
 * that is guaranteed to fail.
 */
@Schema(name = "AiStatusResponse", description = "Availability of optional AI features")
public record AiStatusResponse(
        @Schema(example = "false", description = "True only when AI is both enabled and configured")
                boolean available,
        @Schema(example = "gemini") String provider,
        @Schema(example = "gemini-2.5-flash") String model,
        @Schema(example = "20", description = "Per-user daily generation limit") int dailyRequestLimit) {

    public static AiStatusResponse from(AiProperties properties) {
        return new AiStatusResponse(
                properties.operational(),
                properties.provider(),
                properties.gemini().model(),
                properties.dailyRequestLimitPerUser());
    }
}
