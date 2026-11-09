package com.memora.analytics.web.dto;

import com.memora.gamification.web.dto.ProgressResponse;
import com.memora.languages.web.dto.UserLanguageResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Everything the dashboard renders, in one request.
 *
 * <p>Scope note: this payload intentionally contains only values the platform
 * actually records today. Review-queue size, accuracy and estimated retention
 * arrive with the reviews module — the dashboard renders explicit empty states
 * for them rather than being fed placeholder numbers.
 */
@Schema(name = "DashboardResponse", description = "Aggregated snapshot for the dashboard")
public record DashboardResponse(ProgressResponse progress, List<UserLanguageResponse> languages) {}
