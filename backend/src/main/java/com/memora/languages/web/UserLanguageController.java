package com.memora.languages.web;

import com.memora.languages.service.UserLanguageService;
import com.memora.languages.web.dto.EnrolLanguageRequest;
import com.memora.languages.web.dto.UpdateUserLanguageRequest;
import com.memora.languages.web.dto.UserLanguageResponse;
import com.memora.shared.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/languages")
@Tag(name = "My languages", description = "The authenticated user's language enrolments")
public class UserLanguageController {

    private final UserLanguageService userLanguageService;

    public UserLanguageController(UserLanguageService userLanguageService) {
        this.userLanguageService = userLanguageService;
    }

    @GetMapping
    @Operation(
            summary = "List my language enrolments",
            description = "Not paginated: enrolments are capped at ten per account.")
    public List<UserLanguageResponse> list(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal) {
        return userLanguageService.listForUser(principal.id()).stream()
                .map(UserLanguageResponse::from)
                .toList();
    }

    @GetMapping("/{enrolmentId}")
    @Operation(summary = "Read one enrolment")
    public UserLanguageResponse get(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID enrolmentId) {
        return UserLanguageResponse.from(userLanguageService.requireForUser(enrolmentId, principal.id()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Start studying a language")
    public UserLanguageResponse enrol(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody EnrolLanguageRequest request) {
        return UserLanguageResponse.from(userLanguageService.enrol(principal.id(), request));
    }

    @PutMapping("/{enrolmentId}")
    @Operation(summary = "Update levels or change the active focus")
    public UserLanguageResponse update(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID enrolmentId,
            @Valid @RequestBody UpdateUserLanguageRequest request) {
        return UserLanguageResponse.from(userLanguageService.update(principal.id(), enrolmentId, request));
    }

    @DeleteMapping("/{enrolmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Stop studying a language")
    public void remove(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID enrolmentId) {
        userLanguageService.remove(principal.id(), enrolmentId);
    }
}
