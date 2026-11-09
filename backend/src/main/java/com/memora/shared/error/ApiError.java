package com.memora.shared.error;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/**
 * The single error shape returned by every endpoint. Clients branch on
 * {@code code}, not on the human-readable {@code message}.
 */
@Schema(name = "ApiError", description = "Standard error payload")
public record ApiError(
        @Schema(example = "2026-11-08T21:30:00Z") Instant timestamp,
        @Schema(example = "400") int status,
        @Schema(example = "VALIDATION_FAILED") ErrorCode code,
        @Schema(example = "One or more fields are invalid") String message,
        @Schema(example = "/api/auth/register") String path,
        @Schema(description = "Present only for field-level validation failures")
        List<FieldViolation> violations) {

    public static ApiError of(ErrorCode code, String message, String path) {
        return new ApiError(Instant.now(), code.status().value(), code, message, path, null);
    }

    public static ApiError of(ErrorCode code, String message, String path, List<FieldViolation> violations) {
        return new ApiError(
                Instant.now(),
                code.status().value(),
                code,
                message,
                path,
                violations == null || violations.isEmpty() ? null : violations);
    }

    @Schema(name = "FieldViolation", description = "A single rejected field")
    public record FieldViolation(
            @Schema(example = "email") String field,
            @Schema(example = "must be a well-formed email address") String message) {}
}
