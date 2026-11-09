package com.memora.users.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(name = "UpdateProfileRequest")
public record UpdateProfileRequest(
        @NotBlank @Size(min = 2, max = 80) @Schema(example = "Ana Souza") String displayName,
        @NotBlank @Size(max = 16) @Pattern(regexp = "^[a-zA-Z]{2}(-[a-zA-Z0-9]{2,8})?$",
                        message = "must be a BCP-47 language tag such as pt-BR")
                @Schema(example = "pt-BR")
                String locale,
        @NotBlank @Size(max = 64) @Schema(example = "America/Sao_Paulo") String timeZone) {}
