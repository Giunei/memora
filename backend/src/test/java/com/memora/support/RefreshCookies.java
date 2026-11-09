package com.memora.support;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.Cookie;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Reads the refresh-token cookie straight out of the {@code Set-Cookie} header.
 *
 * <p>The production code writes the header itself, so parsing the raw value is
 * what actually verifies the attributes a test cares about (HttpOnly, SameSite,
 * Path) rather than trusting a container's cookie abstraction.
 */
public final class RefreshCookies {

    public static final String NAME = "memora_refresh_token";

    private RefreshCookies() {}

    public static String requireValue(MockHttpServletResponse response) {
        return value(response).orElseThrow(() ->
                new AssertionError("No " + NAME + " cookie was set on the response"));
    }

    public static Optional<String> value(MockHttpServletResponse response) {
        return header(response).map(header -> {
            String withoutName = header.substring(header.indexOf('=') + 1);
            int end = withoutName.indexOf(';');
            return end < 0 ? withoutName : withoutName.substring(0, end);
        });
    }

    public static Optional<String> header(MockHttpServletResponse response) {
        return response.getHeaders(HttpHeaders.SET_COOKIE).stream()
                .filter(header -> header.startsWith(NAME + "="))
                .findFirst();
    }

    public static Cookie asRequestCookie(String value) {
        return new Cookie(NAME, value);
    }

    /** Asserts the hardening attributes described in ADR 0002. */
    public static void assertHardened(MockHttpServletResponse response) {
        String header = header(response).orElseThrow(() -> new AssertionError("No refresh cookie present"));
        assertThat(header)
                .as("refresh cookie must be unreadable from JavaScript")
                .contains("HttpOnly");
        assertThat(header)
                .as("refresh cookie must not travel on cross-site requests")
                .contains("SameSite=Lax");
        assertThat(header)
                .as("refresh cookie must be scoped to the auth endpoints")
                .contains("Path=/api/auth");
    }
}
