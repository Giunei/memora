package com.memora.gamification.web;

import com.memora.gamification.service.UserProgressService;
import com.memora.gamification.web.dto.ProgressResponse;
import com.memora.gamification.web.dto.UpdateDailyGoalRequest;
import com.memora.shared.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/progress")
@Tag(name = "Progress", description = "XP, level, streak and the daily study goal")
public class ProgressController {

    private final UserProgressService userProgressService;

    public ProgressController(UserProgressService userProgressService) {
        this.userProgressService = userProgressService;
    }

    @GetMapping
    @Operation(summary = "Read my progress")
    public ProgressResponse get(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal) {
        return ProgressResponse.from(userProgressService.require(principal.id()));
    }

    @PutMapping("/daily-goal")
    @Operation(
            summary = "Change my daily study goal",
            description = "The only progress value a client may set. XP, level and streak are backend-owned.")
    public ProgressResponse updateDailyGoal(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody UpdateDailyGoalRequest request) {
        return ProgressResponse.from(
                userProgressService.changeDailyGoal(principal.id(), request.dailyGoalMinutes()));
    }
}
