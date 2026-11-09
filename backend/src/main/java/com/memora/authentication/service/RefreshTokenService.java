package com.memora.authentication.service;

import com.memora.authentication.config.SecurityProperties;
import com.memora.authentication.domain.RefreshToken;
import com.memora.authentication.domain.RefreshTokenRepository;
import com.memora.users.domain.User;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Issues, rotates and revokes refresh tokens.
 *
 * <p>Tokens are opaque 256-bit random values. The database only ever sees their
 * SHA-256 digest. Because the value is already high-entropy, a plain digest is
 * the correct choice here — a password hash such as bcrypt protects low-entropy
 * secrets against brute force and would only add latency to every refresh.
 */
@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
    private static final int TOKEN_BYTES = 32;

    private final RefreshTokenRepository refreshTokenRepository;
    private final Duration refreshTokenTtl;
    private final TransactionTemplate requiresNew;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            SecurityProperties properties,
            PlatformTransactionManager transactionManager) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenTtl = properties.refreshTokenTtl();
        this.requiresNew = new TransactionTemplate(transactionManager);
        this.requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public Duration refreshTokenTtl() {
        return refreshTokenTtl;
    }

    /** Starts a new rotation family. Called on registration and login. */
    @Transactional
    public IssuedToken issueNewFamily(User user) {
        return persist(user, UUID.randomUUID());
    }

    /**
     * Consumes the presented token and issues its successor within the same
     * family.
     *
     * @throws InvalidRefreshTokenException if the token is unknown, expired,
     *     revoked, or already consumed. The last case means a token was replayed,
     *     which is treated as a compromise: the whole family is revoked so the
     *     legitimate session is forced to re-authenticate too.
     */
    @Transactional
    public RotationResult rotate(String presentedToken) {
        Instant now = Instant.now();
        RefreshToken stored = refreshTokenRepository
                .findByTokenHashWithUser(hash(presentedToken))
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token is not recognised"));

        if (stored.isConsumed()) {
            // Must commit independently: this method (and AuthService.refresh)
            // throw afterwards, which would otherwise roll the revocation back
            // and leave a stolen token's family usable.
            revokeFamilyNow(stored.getFamilyId());
            log.warn(
                    "Replayed refresh token detected for user {}; revoked token family {}",
                    stored.getUser().getId(),
                    stored.getFamilyId());
            throw new InvalidRefreshTokenException("Refresh token has already been used");
        }
        if (!stored.isUsableAt(now)) {
            throw new InvalidRefreshTokenException("Refresh token is expired or revoked");
        }

        User user = stored.getUser();
        if (!user.isActive()) {
            revokeFamilyNow(stored.getFamilyId());
            throw new AccountNotActiveException();
        }

        stored.consume(now);
        IssuedToken next = persist(user, stored.getFamilyId());
        return new RotationResult(user, next);
    }

    /** Revokes the family the presented token belongs to. Unknown tokens are ignored. */
    @Transactional
    public void revoke(String presentedToken) {
        refreshTokenRepository
                .findByTokenHashWithUser(hash(presentedToken))
                .ifPresent(token -> refreshTokenRepository.revokeFamily(token.getFamilyId(), Instant.now()));
    }

    @Transactional
    public void revokeAllForUser(UUID userId) {
        refreshTokenRepository.revokeAllForUser(userId, Instant.now());
    }

    private void revokeFamilyNow(UUID familyId) {
        requiresNew.executeWithoutResult(
                status -> refreshTokenRepository.revokeFamily(familyId, Instant.now()));
    }

    private IssuedToken persist(User user, UUID familyId) {
        byte[] raw = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(raw);
        String value = encoder.encodeToString(raw);
        Instant expiresAt = Instant.now().plus(refreshTokenTtl);
        refreshTokenRepository.save(RefreshToken.issue(user, hash(value), familyId, expiresAt));
        return new IssuedToken(value, expiresAt);
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required but unavailable", exception);
        }
    }

    public record IssuedToken(String value, Instant expiresAt) {}

    public record RotationResult(User user, IssuedToken token) {}
}
