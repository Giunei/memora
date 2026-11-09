package com.memora.authentication.service;

import com.memora.authentication.config.SecurityProperties;
import com.memora.users.domain.Role;
import com.memora.users.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/** Issues and verifies the short-lived access token described in ADR 0002. */
@Service
public class JwtService {

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLES = "roles";

    private final SecretKey signingKey;
    private final String issuer;
    private final Duration accessTokenTtl;

    public JwtService(SecurityProperties properties) {
        this.signingKey = Keys.hmacShaKeyFor(properties.jwt().secret().getBytes(StandardCharsets.UTF_8));
        this.issuer = properties.jwt().issuer();
        this.accessTokenTtl = properties.jwt().accessTokenTtl();
    }

    public String issueAccessToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(issuer)
                .subject(user.getId().toString())
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenTtl)))
                .claim(CLAIM_EMAIL, user.getEmail())
                .claim(CLAIM_ROLES, user.getRoles().stream().map(Enum::name).toList())
                .signWith(signingKey)
                .compact();
    }

    public Duration accessTokenTtl() {
        return accessTokenTtl;
    }

    /**
     * Verifies signature, issuer and expiry.
     *
     * @return the token payload, or empty for any token that is not currently
     *     valid. Callers treat "invalid" and "expired" identically: the request
     *     proceeds unauthenticated and the client is expected to refresh.
     */
    public Optional<AccessTokenPayload> verify(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(new AccessTokenPayload(
                    UUID.fromString(claims.getSubject()),
                    claims.get(CLAIM_EMAIL, String.class),
                    readRoles(claims)));
        } catch (JwtException | IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private Set<Role> readRoles(Claims claims) {
        List<?> raw = claims.get(CLAIM_ROLES, List.class);
        if (raw == null) {
            return Set.of();
        }
        Set<Role> roles = EnumSet.noneOf(Role.class);
        for (Object value : raw) {
            try {
                roles.add(Role.valueOf(String.valueOf(value)));
            } catch (IllegalArgumentException ignored) {
                // A role that no longer exists in this version grants nothing.
            }
        }
        return roles;
    }

    public record AccessTokenPayload(UUID userId, String email, Set<Role> roles) {}
}
