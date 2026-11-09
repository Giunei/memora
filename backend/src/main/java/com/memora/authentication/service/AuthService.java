package com.memora.authentication.service;

import com.memora.gamification.service.UserProgressService;
import com.memora.shared.error.ResourceAlreadyExistsException;
import com.memora.users.domain.User;
import com.memora.users.domain.UserRepository;
import com.memora.authentication.web.dto.LoginRequest;
import com.memora.authentication.web.dto.RegisterRequest;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Registration, login, session refresh and logout. */
@Service
public class AuthService {

    private static final String DEFAULT_LOCALE = "pt-BR";
    private static final String DEFAULT_TIME_ZONE = "UTC";

    private final UserRepository userRepository;
    private final UserProgressService userProgressService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    /**
     * A throwaway hash verified when the email is unknown, so that a failed login
     * costs the same time whether or not the account exists. Without it, response
     * latency reveals which addresses are registered.
     */
    private final String timingEqualisationHash;

    public AuthService(
            UserRepository userRepository,
            UserProgressService userProgressService,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.userProgressService = userProgressService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.timingEqualisationHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Transactional
    public AuthSession register(RegisterRequest request) {
        String email = User.normaliseEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResourceAlreadyExistsException("An account with this email already exists");
        }

        User user = userRepository.save(User.register(
                email,
                passwordEncoder.encode(request.password()),
                request.displayName(),
                StringUtils.hasText(request.locale()) ? request.locale() : DEFAULT_LOCALE,
                StringUtils.hasText(request.timeZone()) ? request.timeZone() : DEFAULT_TIME_ZONE));

        // Every account owns a progress record from the moment it exists, so the
        // dashboard never has to invent a default.
        userProgressService.initialiseFor(user);

        return startSession(user);
    }

    @Transactional
    public AuthSession login(LoginRequest request) {
        User user = userRepository
                .findByEmailIgnoreCase(User.normaliseEmail(request.email()))
                .orElse(null);

        if (user == null) {
            passwordEncoder.matches(request.password(), timingEqualisationHash);
            throw new InvalidCredentialsException();
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        if (!user.isActive()) {
            throw new AccountNotActiveException();
        }
        return startSession(user);
    }

    @Transactional
    public AuthSession refresh(String presentedRefreshToken) {
        RefreshTokenService.RotationResult rotation = refreshTokenService.rotate(presentedRefreshToken);
        User user = rotation.user();
        return new AuthSession(
                jwtService.issueAccessToken(user),
                jwtService.accessTokenTtl(),
                rotation.token().value(),
                rotation.token().expiresAt(),
                user);
    }

    @Transactional
    public void logout(String presentedRefreshToken) {
        refreshTokenService.revoke(presentedRefreshToken);
    }

    private AuthSession startSession(User user) {
        RefreshTokenService.IssuedToken refreshToken = refreshTokenService.issueNewFamily(user);
        return new AuthSession(
                jwtService.issueAccessToken(user),
                jwtService.accessTokenTtl(),
                refreshToken.value(),
                refreshToken.expiresAt(),
                user);
    }

    public record AuthSession(
            String accessToken,
            Duration accessTokenTtl,
            String refreshToken,
            Instant refreshTokenExpiresAt,
            User user) {}
}
