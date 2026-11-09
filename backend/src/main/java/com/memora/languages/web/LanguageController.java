package com.memora.languages.web;

import com.memora.languages.service.LanguageCatalogService;
import com.memora.languages.web.dto.LanguageResponse;
import com.memora.shared.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/languages")
@Tag(name = "Languages", description = "Catalogue of languages offered by the platform")
@Validated
public class LanguageController {

    private static final int MAX_PAGE_SIZE = 50;

    private final LanguageCatalogService languageCatalogService;

    public LanguageController(LanguageCatalogService languageCatalogService) {
        this.languageCatalogService = languageCatalogService;
    }

    @GetMapping
    @Operation(
            summary = "List available languages",
            description = "Paginated. Optionally filtered by name, native name or ISO code.")
    public PageResponse<LanguageResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(MAX_PAGE_SIZE) int size) {
        var pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        return PageResponse.from(languageCatalogService.list(search, pageable), LanguageResponse::from);
    }
}
