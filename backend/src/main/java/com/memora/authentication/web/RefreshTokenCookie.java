package com.memora.authentication.web;

import com.memora.authentication.config.SecurityProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Builds and reads the refresh-token cookie.
 *
 * <p>The attributes are the security control, not a detail:
 *
 * <ul>
 *   <li>{@code HttpOnly} - unreadable from JavaScript, so XSS cannot exfiltrate it.
 *   <li>{@code SameSite=Lax} - not sent on cross-site POSTs, which is what makes
 *       CSRF against the refresh endpoint ineffective even with CSRF tokens off.
 *   <li>{@code Path=/api/auth} - the browser only attaches it to the three
 *       endpoints that need it, never to ordinary API calls.
 *   <li>{@code Secure} - driven by configuration so local HTTP works while any
 *       HTTPS deployment gets the flag.
 * </ul>
 */
@Component
public class RefreshTokenCookie {

    public static final String NAME = "memora_refresh_token";
    private static final String PATH = "/api/auth";

    private final boolean secure;

    public RefreshTokenCookie(SecurityProperties properties) {
        this.secure = properties.cookieSecure();
    }

    public ResponseCookie issue(String value, Duration maxAge) {
        return baseBuilder(value).maxAge(maxAge).build();
    }

    /** A same-named, immediately-expiring cookie, which is how a cookie is deleted. */
    public ResponseCookie clear() {
        return baseBuilder("").maxAge(Duration.ZERO).build();
    }

    public Optional<String> readFrom(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst();
    }

    private ResponseCookie.ResponseCookieBuilder baseBuilder(String value) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path(PATH);
    }
}
