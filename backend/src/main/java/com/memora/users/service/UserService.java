package com.memora.users.service;

import com.memora.shared.error.BusinessRuleException;
import com.memora.shared.error.ResourceNotFoundException;
import com.memora.users.domain.User;
import com.memora.users.domain.UserRepository;
import com.memora.users.web.dto.UpdateProfileRequest;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User require(UUID userId) {
        return userRepository
                .findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
    }

    @Transactional
    public User updateProfile(UUID userId, UpdateProfileRequest request) {
        if (!isKnownTimeZone(request.timeZone())) {
            throw new BusinessRuleException("Unknown time zone: " + request.timeZone());
        }
        User user = require(userId);
        user.changeDisplayName(request.displayName());
        user.changePreferences(request.locale(), request.timeZone());
        return user;
    }

    private boolean isKnownTimeZone(String timeZone) {
        return ZoneId.getAvailableZoneIds().contains(timeZone);
    }
}
