package com.memora.languages.domain;

import com.memora.shared.domain.BaseEntity;
import com.memora.users.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A user's enrolment in one language.
 *
 * <p>Holds the per-language learning context — where the learner is and where
 * they are heading — which is independent for each language they study.
 */
@Entity
@Table(name = "user_languages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserLanguage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "language_id", nullable = false)
    private Language language;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_level", nullable = false, length = 2)
    private CefrLevel currentLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_level", nullable = false, length = 2)
    private CefrLevel targetLevel;

    /** At most one per user; enforced by a partial unique index. */
    @Column(name = "primary_focus", nullable = false)
    private boolean primaryFocus;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    /** Set by the learning engine when a session completes; null until then. */
    @Column(name = "last_studied_at")
    private Instant lastStudiedAt;

    public static UserLanguage enrol(
            User user, Language language, CefrLevel currentLevel, CefrLevel targetLevel, boolean primaryFocus) {
        UserLanguage enrolment = new UserLanguage();
        enrolment.user = user;
        enrolment.language = language;
        enrolment.currentLevel = currentLevel;
        enrolment.targetLevel = targetLevel;
        enrolment.primaryFocus = primaryFocus;
        enrolment.startedAt = Instant.now();
        return enrolment;
    }

    public void changeLevels(CefrLevel currentLevel, CefrLevel targetLevel) {
        this.currentLevel = currentLevel;
        this.targetLevel = targetLevel;
    }

    public void setAsPrimaryFocus(boolean primaryFocus) {
        this.primaryFocus = primaryFocus;
    }
}
