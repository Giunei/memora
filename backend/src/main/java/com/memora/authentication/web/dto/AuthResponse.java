package com.memora.authentication.web.dto;

import com.memora.users.web.dto.UserResponse;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The authenticated session as the client sees it.
 *
 * <p>The refresh token is deliberately absent: it travels only in an HttpOnly
 * cookie, so it never passes through JavaScript (ADR 0002).
 */
@Schema(name = "AuthResponse")
public record AuthResponse(
        @Schema(description = "Bearer access token; keep in memory only, never in storage")
                String accessToken,
        @Schema(example = "Bearer") String tokenType,
        @Schema(example = "900", description = "Access token lifetime in seconds") long expiresIn,
        UserResponse user) {

    public static AuthResponse of(String accessToken, long expiresInSeconds, UserResponse user) {
        return new AuthResponse(accessToken, "Bearer", expiresInSeconds, user);
    }
}
