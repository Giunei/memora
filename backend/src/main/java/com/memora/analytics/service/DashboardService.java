package com.memora.analytics.service;

import com.memora.analytics.web.dto.DashboardResponse;
import com.memora.gamification.service.UserProgressService;
import com.memora.gamification.web.dto.ProgressResponse;
import com.memora.languages.service.UserLanguageService;
import com.memora.languages.web.dto.UserLanguageResponse;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Assembles the dashboard snapshot.
 *
 * <p>Read-only composition over the owning modules rather than its own queries,
 * so there is a single definition of what "progress" means. When the reviews
 * module adds aggregates heavy enough to need projections, those belong here.
 */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final UserProgressService userProgressService;
    private final UserLanguageService userLanguageService;

    public DashboardService(UserProgressService userProgressService, UserLanguageService userLanguageService) {
        this.userProgressService = userProgressService;
        this.userLanguageService = userLanguageService;
    }

    public DashboardResponse summarise(UUID userId) {
        return new DashboardResponse(
                ProgressResponse.from(userProgressService.require(userId)),
                userLanguageService.listForUser(userId).stream()
                        .map(UserLanguageResponse::from)
                        .toList());
    }
}
