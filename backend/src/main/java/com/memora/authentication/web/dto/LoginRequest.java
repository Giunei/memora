package com.memora.authentication.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "LoginRequest")
public record LoginRequest(
        @NotBlank @Size(max = 254) @Schema(example = "ana@example.com") String email,
        @NotBlank @Size(max = 128) @Schema(example = "memora2026") String password) {}
