package com.memora.languages.web.dto;

import com.memora.languages.domain.Language;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(name = "LanguageResponse", description = "A language available on the platform")
public record LanguageResponse(
        UUID id,
        @Schema(example = "en") String code,
        @Schema(example = "English") String name,
        @Schema(example = "English") String nativeName,
        @Schema(example = "🇬🇧") String flagEmoji) {

    public static LanguageResponse from(Language language) {
        return new LanguageResponse(
                language.getId(),
                language.getCode(),
                language.getName(),
                language.getNativeName(),
                language.getFlagEmoji());
    }
}
