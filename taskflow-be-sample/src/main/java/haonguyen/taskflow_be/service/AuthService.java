package haonguyen.taskflow_be.service;

import haonguyen.taskflow_be.dto.request.LoginRequest;
import haonguyen.taskflow_be.dto.request.RegisterRequest;
import haonguyen.taskflow_be.dto.response.TokenResponse;
import haonguyen.taskflow_be.dto.response.UserResponse;
import haonguyen.taskflow_be.entity.LoginAttempt;
import haonguyen.taskflow_be.entity.RefreshToken;
import haonguyen.taskflow_be.entity.User;
import haonguyen.taskflow_be.exception.AccountLockedException;
import haonguyen.taskflow_be.exception.BusinessRuleException;
import haonguyen.taskflow_be.exception.DuplicateResourceException;
import haonguyen.taskflow_be.mapper.UserMapper;
import haonguyen.taskflow_be.repository.LoginAttemptRepository;
import haonguyen.taskflow_be.repository.RefreshTokenRepository;
import haonguyen.taskflow_be.repository.UserRepository;
import haonguyen.taskflow_be.security.JwtTokenProvider;
import haonguyen.taskflow_be.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;

@Service
@Transactional
public class AuthService {

    // Returned from login/refresh so the controller can set the cookie
    public record AuthResult(TokenResponse tokenResponse, String rawRefreshToken) {}

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_WINDOW_MINUTES = 15;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final LoginAttemptRepository loginAttemptRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final long refreshTokenExpiration;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            LoginAttemptRepository loginAttemptRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.loginAttemptRepository = loginAttemptRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new DuplicateResourceException("DUPLICATE_USERNAME", "Username is already taken");
        }
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new DuplicateResourceException("DUPLICATE_EMAIL", "Email is already registered");
        }
        var user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.password()));
        return UserMapper.toResponse(userRepository.save(user));
    }

    public AuthResult login(LoginRequest request) {
        var since = Instant.now().minus(LOCK_WINDOW_MINUTES, ChronoUnit.MINUTES);
        long failedCount = loginAttemptRepository.countFailedAttempts(request.email(), since);
        if (failedCount >= MAX_FAILED_ATTEMPTS) {
            throw new AccountLockedException(since.plus(LOCK_WINDOW_MINUTES, ChronoUnit.MINUTES));
        }

        var userOpt = userRepository.findByEmailIgnoreCase(request.email());
        boolean success = userOpt.isPresent()
                && passwordEncoder.matches(request.password(), userOpt.get().getPassword());

        recordAttempt(request.email(), success);

        if (!success) {
            throw new BusinessRuleException("INVALID_CREDENTIALS", "Invalid email or password");
        }

        return issueTokens(userOpt.get());
    }

    public AuthResult refresh(String rawToken) {
        var hash = hashToken(rawToken);
        var stored = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new BusinessRuleException("INVALID_REFRESH_TOKEN", "Invalid refresh token"));

        if (stored.getExpiresAt().isBefore(Instant.now())) {
            refreshTokenRepository.delete(stored);
            throw new BusinessRuleException("REFRESH_TOKEN_EXPIRED", "Refresh token has expired");
        }

        var user = stored.getUser();
        refreshTokenRepository.delete(stored);
        return issueTokens(user);
    }

    public void logout(String rawToken) {
        if (rawToken != null && !rawToken.isBlank()) {
            refreshTokenRepository.findByTokenHash(hashToken(rawToken))
                    .ifPresent(refreshTokenRepository::delete);
        }
    }

    private AuthResult issueTokens(User user) {
        var principal = new UserPrincipal(user);
        var accessToken = jwtTokenProvider.generateAccessToken(principal);

        var rawRefresh = generateRawToken();
        var rt = new RefreshToken();
        rt.setUser(user);
        rt.setTokenHash(hashToken(rawRefresh));
        rt.setExpiresAt(Instant.now().plusMillis(refreshTokenExpiration));
        refreshTokenRepository.save(rt);

        return new AuthResult(new TokenResponse(accessToken, 900000L), rawRefresh);
    }

    private void recordAttempt(String email, boolean success) {
        var attempt = new LoginAttempt();
        attempt.setEmail(email.toLowerCase());
        attempt.setSuccess(success);
        loginAttemptRepository.save(attempt);
    }

    private String generateRawToken() {
        var bytes = new byte[96];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String raw) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
