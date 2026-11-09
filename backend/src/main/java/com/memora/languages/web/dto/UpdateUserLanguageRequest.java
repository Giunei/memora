package com.memora.languages.web.dto;

import com.memora.languages.domain.CefrLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(name = "UpdateUserLanguageRequest")
public record UpdateUserLanguageRequest(
        @NotNull @Schema(example = "A2") CefrLevel currentLevel,
        @NotNull @Schema(example = "C1") CefrLevel targetLevel,
        boolean primaryFocus) {}
