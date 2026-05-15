package haonguyen.taskflow_be.service;

import haonguyen.taskflow_be.dto.request.LoginRequest;
import haonguyen.taskflow_be.dto.request.RegisterRequest;
import haonguyen.taskflow_be.entity.User;
import haonguyen.taskflow_be.exception.BusinessRuleException;
import haonguyen.taskflow_be.exception.DuplicateResourceException;
import haonguyen.taskflow_be.repository.LoginAttemptRepository;
import haonguyen.taskflow_be.repository.RefreshTokenRepository;
import haonguyen.taskflow_be.repository.UserRepository;
import haonguyen.taskflow_be.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock LoginAttemptRepository loginAttemptRepository;
    @Mock JwtTokenProvider jwtTokenProvider;
    @Mock LoginAttemptService loginAttemptService;

    PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4); // low cost for tests
    AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository, refreshTokenRepository, loginAttemptRepository,
                passwordEncoder, jwtTokenProvider, loginAttemptService, 604800000L);
    }

    // ── register ─────────────────────────────────────────────────────────────

    @Test
    void register_savesUserAndReturnsResponse() {
        when(userRepository.existsByUsernameIgnoreCase("alice")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("alice@example.com")).thenReturn(false);

        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            return u;
        });

        var req = new RegisterRequest("alice", "alice@example.com", "Test1234!");
        var response = authService.register(req);

        assertThat(response.username()).isEqualTo("alice");
        assertThat(response.email()).isEqualTo("alice@example.com");
        verify(userRepository).save(argThat(u -> u.getEmail().equals("alice@example.com")));
    }

    @Test
    void register_throwsDuplicateUsername_whenUsernameTaken() {
        when(userRepository.existsByUsernameIgnoreCase("alice")).thenReturn(true);

        var req = new RegisterRequest("alice", "alice@example.com", "Test1234!");
        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Username is already taken");
    }

    @Test
    void register_throwsDuplicateEmail_whenEmailTaken() {
        when(userRepository.existsByUsernameIgnoreCase("alice")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("alice@example.com")).thenReturn(true);

        var req = new RegisterRequest("alice", "alice@example.com", "Test1234!");
        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Email is already registered");
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    void login_returnsTokens_onValidCredentials() {
        var user = new User();
        user.setEmail("alex@example.com");
        user.setPassword(passwordEncoder.encode("Test1234!"));

        when(loginAttemptRepository.countFailedAttempts(anyString(), any(Instant.class))).thenReturn(0L);
        when(userRepository.findByEmailIgnoreCase("alex@example.com")).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateAccessToken(any())).thenReturn("access-token-123");
        when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(900000L);
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = authService.login(new LoginRequest("alex@example.com", "Test1234!"));

        assertThat(result.tokenResponse().accessToken()).isEqualTo("access-token-123");
        assertThat(result.rawRefreshToken()).isNotBlank();
    }

    @Test
    void login_throwsBusinessRule_onWrongPassword() {
        var user = new User();
        user.setEmail("alex@example.com");
        user.setPassword(passwordEncoder.encode("Test1234!"));

        when(loginAttemptRepository.countFailedAttempts(anyString(), any(Instant.class))).thenReturn(0L);
        when(userRepository.findByEmailIgnoreCase("alex@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("alex@example.com", "WrongPass!")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    void login_throwsAccountLocked_whenTooManyFailures() {
        when(loginAttemptRepository.countFailedAttempts(anyString(), any(Instant.class))).thenReturn(5L);

        assertThatThrownBy(() -> authService.login(new LoginRequest("alex@example.com", "any")))
                .isInstanceOf(haonguyen.taskflow_be.exception.AccountLockedException.class);
    }
}
