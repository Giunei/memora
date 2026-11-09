package com.memora.analytics.web;

import com.memora.analytics.service.DashboardService;
import com.memora.analytics.web.dto.DashboardResponse;
import com.memora.shared.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "Aggregated view for the home screen")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    @Operation(
            summary = "Read the dashboard snapshot",
            description = "One request for progress and language enrolments, to avoid a fan-out on page load.")
    public DashboardResponse summary(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal) {
        return dashboardService.summarise(principal.id());
    }
}
