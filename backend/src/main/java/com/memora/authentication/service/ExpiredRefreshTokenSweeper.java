package com.memora.authentication.service;

import com.memora.authentication.domain.RefreshTokenRepository;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deletes refresh tokens that can no longer be used.
 *
 * <p>Rotation creates one row per refresh, so without this the table grows for
 * the lifetime of the deployment. Expired rows carry no audit value — the token
 * value itself was never stored.
 */
@Component
public class ExpiredRefreshTokenSweeper {

    private static final Logger log = LoggerFactory.getLogger(ExpiredRefreshTokenSweeper.class);

    private final RefreshTokenRepository refreshTokenRepository;

    public ExpiredRefreshTokenSweeper(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Scheduled(cron = "0 15 3 * * *")
    @Transactional
    public void sweep() {
        int deleted = refreshTokenRepository.deleteExpiredBefore(Instant.now());
        if (deleted > 0) {
            log.info("Deleted {} expired refresh tokens", deleted);
        }
    }
}
