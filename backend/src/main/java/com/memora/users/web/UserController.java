package com.memora.users.web;

import com.memora.shared.security.AuthenticatedUser;
import com.memora.users.service.UserService;
import com.memora.users.web.dto.UpdateProfileRequest;
import com.memora.users.web.dto.UserResponse;
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
@RequestMapping("/api/users")
@Tag(name = "Users", description = "The authenticated user's account")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    @Operation(summary = "Read the authenticated user's profile")
    public UserResponse me(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal) {
        return UserResponse.from(userService.require(principal.id()));
    }

    @PutMapping("/me")
    @Operation(summary = "Update display name, locale and time zone")
    public UserResponse updateProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        return UserResponse.from(userService.updateProfile(principal.id(), request));
    }
}
