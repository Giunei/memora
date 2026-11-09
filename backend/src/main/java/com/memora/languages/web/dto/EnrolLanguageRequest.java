package com.memora.languages.web.dto;

import com.memora.languages.domain.CefrLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(name = "EnrolLanguageRequest", description = "Start studying a language")
public record EnrolLanguageRequest(
        @NotBlank @Size(max = 10) @Schema(example = "en", description = "ISO 639-1 code") String languageCode,
        @NotNull @Schema(example = "A1") CefrLevel currentLevel,
        @NotNull @Schema(example = "B2") CefrLevel targetLevel,
        @Schema(
                        example = "true",
                        description = "Make this the active focus. The first enrolment always becomes the focus.")
                boolean primaryFocus) {}
