package com.memora.authentication.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.memora.authentication.web.dto.LoginRequest;
import com.memora.authentication.web.dto.RegisterRequest;
import com.memora.gamification.service.UserProgressService;
import com.memora.shared.error.ResourceAlreadyExistsException;
import com.memora.users.domain.User;
import com.memora.users.domain.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String EMAIL = "ana@example.com";
    private static final String RAW_PASSWORD = "memora2026";
    private static final String STORED_HASH = "{bcrypt}$2a$10$storedhashvalue";

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProgressService userProgressService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        // The service hashes a throwaway value in its constructor to equalise
        // login timing, so the encoder has to answer before construction.
        when(passwordEncoder.encode(anyString())).thenReturn(STORED_HASH);
        authService = new AuthService(
                userRepository, userProgressService, passwordEncoder, jwtService, refreshTokenService);
    }

    @Test
    @DisplayName("registration creates the account, its progress record and a session")
    void registersNewAccount() {
        when(userRepository.existsByEmailIgnoreCase(EMAIL)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(refreshTokenService.issueNewFamily(any(User.class)))
                .thenReturn(new RefreshTokenService.IssuedToken("refresh-value", Instant.now().plusSeconds(60)));
        when(jwtService.issueAccessToken(any(User.class))).thenReturn("access-token");
        when(jwtService.accessTokenTtl()).thenReturn(Duration.ofMinutes(15));

        AuthService.AuthSession session = authService.register(
                new RegisterRequest(EMAIL, RAW_PASSWORD, "Ana Souza", null, null));

        assertThat(session.accessToken()).isEqualTo("access-token");
        assertThat(session.refreshToken()).isEqualTo("refresh-value");
        assertThat(session.user().getEmail()).isEqualTo(EMAIL);
        verify(userProgressService).initialiseFor(session.user());
        verify(passwordEncoder).encode(RAW_PASSWORD);
    }

    @Test
    @DisplayName("registration applies the default locale and time zone when omitted")
    void appliesDefaultPreferences() {
        when(userRepository.existsByEmailIgnoreCase(EMAIL)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(refreshTokenService.issueNewFamily(any(User.class)))
                .thenReturn(new RefreshTokenService.IssuedToken("refresh-value", Instant.now().plusSeconds(60)));
        when(jwtService.accessTokenTtl()).thenReturn(Duration.ofMinutes(15));

        User created = authService
                .register(new RegisterRequest(EMAIL, RAW_PASSWORD, "Ana Souza", null, null))
                .user();

        assertThat(created.getLocale()).isEqualTo("pt-BR");
        assertThat(created.getTimeZone()).isEqualTo("UTC");
    }

    @Test
    @DisplayName("an email is normalised before the uniqueness check, so casing cannot bypass it")
    void rejectsDuplicateEmailRegardlessOfCasing() {
        when(userRepository.existsByEmailIgnoreCase(EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                        new RegisterRequest("  Ana@Example.COM ", RAW_PASSWORD, "Ana Souza", null, null)))
                .isInstanceOf(ResourceAlreadyExistsException.class);

        verify(userRepository, never()).save(any(User.class));
        verify(userProgressService, never()).initialiseFor(any(User.class));
    }

    @Test
    @DisplayName("login succeeds and opens a new refresh-token family")
    void logsInWithCorrectPassword() {
        User user = User.register(EMAIL, STORED_HASH, "Ana Souza", "pt-BR", "UTC");
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(RAW_PASSWORD, STORED_HASH)).thenReturn(true);
        when(refreshTokenService.issueNewFamily(user))
                .thenReturn(new RefreshTokenService.IssuedToken("refresh-value", Instant.now().plusSeconds(60)));
        when(jwtService.issueAccessToken(user)).thenReturn("access-token");
        when(jwtService.accessTokenTtl()).thenReturn(Duration.ofMinutes(15));

        AuthService.AuthSession session = authService.login(new LoginRequest(EMAIL, RAW_PASSWORD));

        assertThat(session.accessToken()).isEqualTo("access-token");
        assertThat(session.user()).isSameAs(user);
    }

    @Test
    @DisplayName("an unknown email still costs one hash verification, so timing reveals nothing")
    void equalisesTimingForUnknownEmail() {
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest(EMAIL, RAW_PASSWORD)))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(passwordEncoder).matches(eq(RAW_PASSWORD), eq(STORED_HASH));
        verify(refreshTokenService, never()).issueNewFamily(any(User.class));
    }

    @Test
    void rejectsWrongPassword() {
        User user = User.register(EMAIL, STORED_HASH, "Ana Souza", "pt-BR", "UTC");
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(RAW_PASSWORD, STORED_HASH)).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest(EMAIL, RAW_PASSWORD)))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(refreshTokenService, never()).issueNewFamily(any(User.class));
    }

    @Test
    @DisplayName("a correct password on a non-active account still does not open a session")
    void rejectsInactiveAccount() {
        User suspended = mock(User.class);
        when(suspended.getPasswordHash()).thenReturn(STORED_HASH);
        when(suspended.isActive()).thenReturn(false);
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(suspended));
        when(passwordEncoder.matches(RAW_PASSWORD, STORED_HASH)).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequest(EMAIL, RAW_PASSWORD)))
                .isInstanceOf(AccountNotActiveException.class);

        verify(refreshTokenService, never()).issueNewFamily(any(User.class));
    }

    @Test
    void refreshRotatesTheTokenAndIssuesANewAccessToken() {
        User user = User.register(EMAIL, STORED_HASH, "Ana Souza", "pt-BR", "UTC");
        when(refreshTokenService.rotate("presented")).thenReturn(new RefreshTokenService.RotationResult(
                user, new RefreshTokenService.IssuedToken("rotated-value", Instant.now().plusSeconds(60))));
        when(jwtService.issueAccessToken(user)).thenReturn("fresh-access-token");
        when(jwtService.accessTokenTtl()).thenReturn(Duration.ofMinutes(15));

        AuthService.AuthSession session = authService.refresh("presented");

        assertThat(session.accessToken()).isEqualTo("fresh-access-token");
        assertThat(session.refreshToken()).isEqualTo("rotated-value");
        verify(refreshTokenService, never()).issueNewFamily(any(User.class));
    }
}
