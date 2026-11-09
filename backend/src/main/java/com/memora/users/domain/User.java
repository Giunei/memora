package com.memora.users.domain;

import com.memora.shared.domain.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A platform account.
 *
 * <p>State changes go through intention-revealing methods rather than generic
 * setters, so the invariants (normalised email, non-empty role set) cannot be
 * bypassed by callers.
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Column(nullable = false, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 80)
    private String displayName;

    @Column(nullable = false, length = 16)
    private String locale;

    @Column(name = "time_zone", nullable = false, length = 64)
    private String timeZone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    /**
     * Eager by design: authorities are needed on every authenticated request and
     * the set holds at most a couple of values, so a join beats a second query.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private Set<Role> roles = EnumSet.noneOf(Role.class);

    public static User register(String email, String passwordHash, String displayName, String locale, String timeZone) {
        User user = new User();
        user.email = normaliseEmail(email);
        user.passwordHash = passwordHash;
        user.displayName = displayName.trim();
        user.locale = locale;
        user.timeZone = timeZone;
        user.status = UserStatus.ACTIVE;
        user.roles = EnumSet.of(Role.USER);
        return user;
    }

    public void changeDisplayName(String displayName) {
        this.displayName = displayName.trim();
    }

    public void changePreferences(String locale, String timeZone) {
        this.locale = locale;
        this.timeZone = timeZone;
    }

    public void changePassword(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public Set<Role> getRoles() {
        return Collections.unmodifiableSet(roles);
    }

    public static String normaliseEmail(String email) {
        return email.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
