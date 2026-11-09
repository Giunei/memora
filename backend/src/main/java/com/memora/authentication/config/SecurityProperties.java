package com.memora.authentication.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Security configuration bound from {@code memora.security.*}.
 *
 * <p>Validated at startup so a missing or too-short signing secret fails the
 * boot instead of producing tokens an attacker could forge.
 */
@Validated
@ConfigurationProperties(prefix = "memora.security")
public record SecurityProperties(
        @NotNull @Valid Jwt jwt,
        @NotNull Duration refreshTokenTtl,
        boolean cookieSecure,
        List<String> allowedOrigins) {

    public record Jwt(
            @NotBlank
                    @Size(min = 32, message = "must be at least 32 characters to key HMAC-SHA256")
                    String secret,
            @NotBlank String issuer,
            @NotNull Duration accessTokenTtl) {}

    /** An unset property binds to {@code null}; callers want "no extra origins". */
    public List<String> allowedOrigins() {
        return allowedOrigins == null ? List.of() : allowedOrigins.stream()
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();
    }
}
