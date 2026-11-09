package com.memora.authentication.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(name = "RegisterRequest")
public record RegisterRequest(
        @NotBlank @Email @Size(max = 254) @Schema(example = "ana@example.com") String email,
        @NotBlank
                @Size(min = 8, max = 128)
                @Pattern(
                        regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
                        message = "must contain at least one letter and one digit")
                @Schema(example = "memora2026")
                String password,
        @NotBlank @Size(min = 2, max = 80) @Schema(example = "Ana Souza") String displayName,
        @Size(max = 16) @Schema(example = "pt-BR", description = "Optional BCP-47 tag; defaults to pt-BR")
                String locale,
        @Size(max = 64)
                @Schema(example = "America/Sao_Paulo", description = "Optional IANA zone; defaults to UTC")
                String timeZone) {}
