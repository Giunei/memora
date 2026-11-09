package com.memora.authentication.web;

import com.memora.authentication.service.AuthService;
import com.memora.authentication.service.AuthService.AuthSession;
import com.memora.authentication.service.InvalidRefreshTokenException;
import com.memora.authentication.web.dto.AuthResponse;
import com.memora.authentication.web.dto.LoginRequest;
import com.memora.authentication.web.dto.RegisterRequest;
import com.memora.shared.error.ApiError;
import com.memora.users.web.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import java.time.Instant;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Session endpoints. All four are public: they are how a client obtains or drops
 * credentials in the first place.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Registration and session lifecycle")
@SecurityRequirements // these endpoints take no bearer token
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenCookie refreshTokenCookie;

    public AuthController(AuthService authService, RefreshTokenCookie refreshTokenCookie) {
        this.authService = authService;
        this.refreshTokenCookie = refreshTokenCookie;
    }

    @PostMapping("/register")
    @Operation(
            summary = "Create an account and open a session",
            description = "Also sets the HttpOnly refresh-token cookie.")
    @ApiResponse(responseCode = "201", description = "Account created")
    @ApiResponse(
            responseCode = "409",
            description = "Email already registered",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return respondWithSession(authService.register(request), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    @Operation(summary = "Open a session with email and password")
    @ApiResponse(
            responseCode = "401",
            description = "Invalid credentials",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return respondWithSession(authService.login(request), HttpStatus.OK);
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Exchange the refresh cookie for a new access token",
            description = """
                    Takes no request body: the refresh token is read from the HttpOnly cookie.
                    The presented token is consumed and replaced, so each cookie works once.
                    The SPA also calls this on start-up to restore a session.
                    """)
    @ApiResponse(
            responseCode = "401",
            description = "Missing, expired, revoked or already-used refresh token",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest request) {
        String presented = refreshTokenCookie
                .readFrom(request)
                .orElseThrow(() -> new InvalidRefreshTokenException("No refresh token cookie present"));
        return respondWithSession(authService.refresh(presented), HttpStatus.OK);
    }

    @PostMapping("/logout")
    @Operation(
            summary = "End the session",
            description = "Revokes the whole refresh-token family and clears the cookie. Idempotent.")
    @ApiResponse(responseCode = "204", description = "Session ended")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        refreshTokenCookie.readFrom(request).ifPresent(authService::logout);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.clear().toString())
                .build();
    }

    private ResponseEntity<AuthResponse> respondWithSession(AuthSession session, HttpStatus status) {
        ResponseCookie cookie = refreshTokenCookie.issue(
                session.refreshToken(), remainingLifetime(session.refreshTokenExpiresAt()));
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(AuthResponse.of(
                        session.accessToken(),
                        session.accessTokenTtl().toSeconds(),
                        UserResponse.from(session.user())));
    }

    /**
     * The cookie's Max-Age must track the token's real expiry rather than the
     * configured TTL, so a rotated token does not outlive its row.
     */
    private Duration remainingLifetime(Instant expiresAt) {
        Duration remaining = Duration.between(Instant.now(), expiresAt);
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }
}
