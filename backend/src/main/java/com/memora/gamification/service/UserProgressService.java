package com.memora.gamification.service;

import com.memora.gamification.domain.UserProgress;
import com.memora.gamification.domain.UserProgressRepository;
import com.memora.shared.error.BusinessRuleException;
import com.memora.shared.error.ResourceNotFoundException;
import com.memora.users.domain.User;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserProgressService {

    private final UserProgressRepository userProgressRepository;

    public UserProgressService(UserProgressRepository userProgressRepository) {
        this.userProgressRepository = userProgressRepository;
    }

    /** Creates the progress record that every account must have from the start. */
    @Transactional
    public UserProgress initialiseFor(User user) {
        return userProgressRepository.save(UserProgress.initialFor(user));
    }

    public UserProgress require(UUID userId) {
        return userProgressRepository
                .findByUserId(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Progress for user", userId));
    }

    @Transactional
    public UserProgress changeDailyGoal(UUID userId, int minutes) {
        UserProgress progress = require(userId);
        try {
            progress.changeDailyGoal(minutes);
        } catch (IllegalArgumentException exception) {
            throw new BusinessRuleException(exception.getMessage());
        }
        return progress;
    }
}
