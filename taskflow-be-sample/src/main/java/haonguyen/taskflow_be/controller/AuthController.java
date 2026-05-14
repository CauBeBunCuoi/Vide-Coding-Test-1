package haonguyen.taskflow_be.controller;

import haonguyen.taskflow_be.dto.request.LoginRequest;
import haonguyen.taskflow_be.dto.request.RegisterRequest;
import haonguyen.taskflow_be.dto.response.TokenResponse;
import haonguyen.taskflow_be.dto.response.UserResponse;
import haonguyen.taskflow_be.exception.BusinessRuleException;
import haonguyen.taskflow_be.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.Duration;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;
    private final long refreshTokenExpiration;

    public AuthController(AuthService authService,
                          @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration) {
        this.authService = authService;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(201).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletResponse response) {
        var result = authService.login(request);
        setRefreshCookie(response, result.rawRefreshToken());
        return ResponseEntity.ok(result.tokenResponse());
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @CookieValue(name = "refreshToken", required = false) String rawToken,
            HttpServletResponse response) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BusinessRuleException("MISSING_REFRESH_TOKEN", "Refresh token is missing");
        }
        var result = authService.refresh(rawToken);
        setRefreshCookie(response, result.rawRefreshToken());
        return ResponseEntity.ok(result.tokenResponse());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "refreshToken", required = false) String rawToken,
            HttpServletResponse response) {
        authService.logout(rawToken);
        clearRefreshCookie(response);
        return ResponseEntity.noContent().build();
    }

    private void setRefreshCookie(HttpServletResponse response, String value) {
        response.setHeader(HttpHeaders.SET_COOKIE,
                ResponseCookie.from("refreshToken", value)
                        .httpOnly(true)
                        .secure(false)
                        .path("/api/v1/auth")
                        .maxAge(Duration.ofMillis(refreshTokenExpiration))
                        .sameSite("Lax")
                        .build()
                        .toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        response.setHeader(HttpHeaders.SET_COOKIE,
                ResponseCookie.from("refreshToken", "")
                        .httpOnly(true)
                        .secure(false)
                        .path("/api/v1/auth")
                        .maxAge(0)
                        .sameSite("Lax")
                        .build()
                        .toString());
    }
}
