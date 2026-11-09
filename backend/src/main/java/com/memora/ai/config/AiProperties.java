package com.memora.ai.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

/**
 * AI provider configuration bound from {@code memora.ai.*} (ADR 0003).
 *
 * <p>The API key arrives from the {@code GEMINI_API_KEY} environment variable and
 * is never written to a response. {@link #operational()} is the single predicate
 * the rest of the application asks: enabled *and* actually configured, since a
 * flag flipped on without a key is not usable.
 */
@Validated
@ConfigurationProperties(prefix = "memora.ai")
public record AiProperties(
        boolean enabled,
        @NotBlank String provider,
        @NotNull Duration requestTimeout,
        @Min(1) int dailyRequestLimitPerUser,
        @NotNull @Valid Gemini gemini) {

    public record Gemini(String apiKey, @NotBlank String model, @NotBlank String baseUrl) {}

    public boolean operational() {
        return enabled && StringUtils.hasText(gemini.apiKey());
    }
}
