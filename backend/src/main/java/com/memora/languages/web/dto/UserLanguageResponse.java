package com.memora.languages.web.dto;

import com.memora.languages.domain.CefrLevel;
import com.memora.languages.domain.UserLanguage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(name = "UserLanguageResponse", description = "One of the authenticated user's language enrolments")
public record UserLanguageResponse(
        UUID id,
        LanguageResponse language,
        CefrLevel currentLevel,
        CefrLevel targetLevel,
        boolean primaryFocus,
        Instant startedAt,
        @Schema(description = "Null until the first completed study session") Instant lastStudiedAt) {

    public static UserLanguageResponse from(UserLanguage enrolment) {
        return new UserLanguageResponse(
                enrolment.getId(),
                LanguageResponse.from(enrolment.getLanguage()),
                enrolment.getCurrentLevel(),
                enrolment.getTargetLevel(),
                enrolment.isPrimaryFocus(),
                enrolment.getStartedAt(),
                enrolment.getLastStudiedAt());
    }
}
