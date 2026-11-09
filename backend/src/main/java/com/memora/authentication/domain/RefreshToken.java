package com.memora.authentication.domain;

import com.memora.shared.domain.BaseEntity;
import com.memora.users.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A single link in a refresh-token rotation chain.
 *
 * <p>Only the SHA-256 digest of the token is stored, so the row is useless to
 * anyone who reads the table. {@code familyId} ties together every token issued
 * from one login: if a consumed token is presented again — the signature of a
 * stolen token being replayed — the whole family is revoked at once.
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    public static RefreshToken issue(User user, String tokenHash, UUID familyId, Instant expiresAt) {
        RefreshToken token = new RefreshToken();
        token.user = user;
        token.tokenHash = tokenHash;
        token.familyId = familyId;
        token.expiresAt = expiresAt;
        return token;
    }

    public void consume(Instant at) {
        this.consumedAt = at;
    }

    public void revoke(Instant at) {
        if (this.revokedAt == null) {
            this.revokedAt = at;
        }
    }

    public boolean isConsumed() {
        return consumedAt != null;
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpiredAt(Instant moment) {
        return expiresAt.isBefore(moment);
    }

    /** Usable exactly once, while unconsumed, unrevoked and unexpired. */
    public boolean isUsableAt(Instant moment) {
        return !isConsumed() && !isRevoked() && !isExpiredAt(moment);
    }
}
