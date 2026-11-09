package com.memora.users.web.dto;

import com.memora.users.domain.Role;
import com.memora.users.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Schema(name = "UserResponse", description = "Public view of an account")
public record UserResponse(
        UUID id,
        String email,
        String displayName,
        String locale,
        String timeZone,
        Set<Role> roles,
        Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getLocale(),
                user.getTimeZone(),
                user.getRoles(),
                user.getCreatedAt());
    }
}
