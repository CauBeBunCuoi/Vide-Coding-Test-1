# UI Layout + Auth — Backend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the complete auth backend in `taskflow-be-sample/` — 5 auth endpoints (register, login, refresh, logout, me), Spring Security + JWT, BCrypt password hashing, account locking, and full V11 seed data covering all tables.

**Architecture:** Stateless JWT. Access token (HS256, 15 min) in Authorization header. Refresh token (128-char random, SHA-256 hashed in DB) as httpOnly cookie. `JwtAuthenticationFilter` validates the access token on every request and populates `SecurityContext`. `AuthService` handles all auth logic including account locking after 5 failed attempts in 15 minutes.

**Tech Stack:** Java 21, Spring Boot 4.0.6, Spring Security 6, JJWT 0.12.6, Spring Data JPA, Flyway 11, PostgreSQL 17.5, JUnit 5 + Mockito

---

## File Map

| File | Action | Responsibility |
|---|---|---|
| `src/test/java/.../HashGeneratorTest.java` | Create | One-time utility — generates BCrypt hash for seed data |
| `src/main/java/.../entity/User.java` | Modify | JPA entity for `users` table |
| `src/main/java/.../entity/RefreshToken.java` | Modify | JPA entity for `refresh_tokens` table |
| `src/main/java/.../entity/LoginAttempt.java` | Modify | JPA entity for `login_attempts` table |
| `src/main/java/.../repository/UserRepository.java` | Modify | `findByEmailIgnoreCase`, existence checks |
| `src/main/java/.../repository/RefreshTokenRepository.java` | Modify | Find by hash, delete by user |
| `src/main/java/.../repository/LoginAttemptRepository.java` | Modify | Count failed attempts in window |
| `src/main/java/.../dto/response/TokenResponse.java` | Modify | `{ accessToken, expiresIn }` |
| `src/main/java/.../dto/response/UserResponse.java` | Modify | `{ id, username, email, createdAt }` |
| `src/main/java/.../dto/response/ErrorResponse.java` | Modify | `{ error, message, details }` |
| `src/main/java/.../dto/request/RegisterRequest.java` | Modify | Validated registration fields |
| `src/main/java/.../dto/request/LoginRequest.java` | Modify | `{ email, password }` |
| `src/main/java/.../mapper/UserMapper.java` | Modify | `User` → `UserResponse` |
| `src/main/java/.../exception/ResourceNotFoundException.java` | Modify | 404 with error code |
| `src/main/java/.../exception/DuplicateResourceException.java` | Modify | 409 with error code |
| `src/main/java/.../exception/AccessDeniedException.java` | Modify | 403 with error code |
| `src/main/java/.../exception/AccountLockedException.java` | Modify | 423 with `lockedUntil` |
| `src/main/java/.../exception/BusinessRuleException.java` | Modify | 400 with error code |
| `src/main/java/.../exception/GlobalExceptionHandler.java` | Modify | `@RestControllerAdvice` maps all exceptions |
| `src/main/java/.../security/UserPrincipal.java` | Modify | `UserDetails` wrapping `User` entity |
| `src/main/java/.../security/CustomUserDetailsService.java` | Modify | Load by email or ID |
| `src/main/java/.../security/JwtTokenProvider.java` | Modify | Generate + validate access tokens |
| `src/main/java/.../security/JwtAuthenticationFilter.java` | Modify | Extract token → validate → set `SecurityContext` |
| `src/main/java/.../config/SecurityConfig.java` | Modify | Filter chain + `PasswordEncoder` + `AuthenticationManager` |
| `src/main/java/.../config/WebConfig.java` | Modify | CORS: allow `http://localhost:5177` with credentials |
| `src/main/resources/db/migration/V11__seed_data.sql` | Modify | Complete seed data for all tables |
| `src/main/java/.../service/AuthService.java` | Modify | register, login, refresh, logout |
| `src/main/java/.../controller/AuthController.java` | Modify | 4 auth endpoints |
| `src/main/java/.../service/UserService.java` | Modify | `getMe(userId)` |
| `src/main/java/.../controller/UserController.java` | Modify | `GET /api/v1/users/me` |
| `src/test/java/.../service/AuthServiceTest.java` | Create | Unit tests for AuthService |
| `src/test/java/.../controller/AuthControllerTest.java` | Create | `@WebMvcTest` for AuthController |

Base package: `haonguyen.taskflow_be`  
Source root: `src/main/java/haonguyen/taskflow_be/`  
Test root: `src/test/java/haonguyen/taskflow_be/`

---

### Task 1: Generate BCrypt hash for seed data (2 min)

> This hash is used in Task 11. Run this test first before implementing SecurityConfig.

**Files:**
- Create: `src/test/java/haonguyen/taskflow_be/HashGeneratorTest.java`

- [ ] **Step 1: Create `HashGeneratorTest.java`**

```java
package haonguyen.taskflow_be;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class HashGeneratorTest {

    @Test
    void printHashForSeedData() {
        var encoder = new BCryptPasswordEncoder(10);
        var hash = encoder.encode("Test1234!");
        System.out.println("=== BCrypt hash for Test1234! ===");
        System.out.println(hash);
        System.out.println("=================================");
    }
}
```

- [ ] **Step 2: Run the test and capture the hash**

```bash
./gradlew test --tests "haonguyen.taskflow_be.HashGeneratorTest" --info 2>&1 | findstr /C:"BCrypt hash" /C:"2a$10"
```

On Linux/Mac use: `grep -A1 "BCrypt hash"`

Expected output (format — actual value will differ):
```
=== BCrypt hash for Test1234! ===
$2a$10$<22-char-salt><31-char-hash>
```

- [ ] **Step 3: Copy the hash**

Paste it into a scratch note — you will use it in Task 11, Step 1.  
The hash looks like: `$2a$10$...` (total 60 characters).

---

### Task 2: User entity + repository (3 min)

**Files:**
- Modify: `src/main/java/haonguyen/taskflow_be/entity/User.java`
- Modify: `src/main/java/haonguyen/taskflow_be/repository/UserRepository.java`

- [ ] **Step 1: Implement `User.java`**

```java
package haonguyen.taskflow_be.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Instant getCreatedAt() { return createdAt; }
}
```

- [ ] **Step 2: Implement `UserRepository.java`**

```java
package haonguyen.taskflow_be.repository;

import haonguyen.taskflow_be.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByUsernameIgnoreCase(String username);
    boolean existsByEmailIgnoreCase(String email);
}
```

- [ ] **Step 3: Commit**
```bash
git add src/main/java/haonguyen/taskflow_be/entity/User.java src/main/java/haonguyen/taskflow_be/repository/UserRepository.java
git commit -m "feat(be): implement User entity and UserRepository"
```

---

### Task 3: RefreshToken + LoginAttempt entities and repositories (3 min)

**Files:**
- Modify: `src/main/java/haonguyen/taskflow_be/entity/RefreshToken.java`
- Modify: `src/main/java/haonguyen/taskflow_be/entity/LoginAttempt.java`
- Modify: `src/main/java/haonguyen/taskflow_be/repository/RefreshTokenRepository.java`
- Modify: `src/main/java/haonguyen/taskflow_be/repository/LoginAttemptRepository.java`

- [ ] **Step 1: Implement `RefreshToken.java`**

```java
package haonguyen.taskflow_be.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
}
```

- [ ] **Step 2: Implement `LoginAttempt.java`**

```java
package haonguyen.taskflow_be.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "login_attempts")
public class LoginAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(name = "attempted_at", nullable = false)
    private Instant attemptedAt;

    @Column(nullable = false)
    private boolean success;

    @PrePersist
    void onCreate() { if (attemptedAt == null) attemptedAt = Instant.now(); }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Instant getAttemptedAt() { return attemptedAt; }
    public void setAttemptedAt(Instant t) { this.attemptedAt = t; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
}
```

- [ ] **Step 3: Implement `RefreshTokenRepository.java`**

```java
package haonguyen.taskflow_be.repository;

import haonguyen.taskflow_be.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.user.id = :userId")
    void deleteByUserId(Long userId);

    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.expiresAt < :now")
    void deleteAllExpired(Instant now);
}
```

- [ ] **Step 4: Implement `LoginAttemptRepository.java`**

```java
package haonguyen.taskflow_be.repository;

import haonguyen.taskflow_be.entity.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.Instant;

public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {

    @Query("SELECT COUNT(a) FROM LoginAttempt a " +
           "WHERE LOWER(a.email) = LOWER(:email) " +
           "AND a.success = false " +
           "AND a.attemptedAt > :since")
    long countFailedAttempts(String email, Instant since);
}
```

- [ ] **Step 5: Commit**
```bash
git add src/main/java/haonguyen/taskflow_be/entity/ src/main/java/haonguyen/taskflow_be/repository/RefreshTokenRepository.java src/main/java/haonguyen/taskflow_be/repository/LoginAttemptRepository.java
git commit -m "feat(be): implement auth-related entities and repositories"
```

---

### Task 4: DTOs + UserMapper (3 min)

**Files:**
- Modify: `src/main/java/haonguyen/taskflow_be/dto/response/TokenResponse.java`
- Modify: `src/main/java/haonguyen/taskflow_be/dto/response/UserResponse.java`
- Modify: `src/main/java/haonguyen/taskflow_be/dto/response/ErrorResponse.java`
- Modify: `src/main/java/haonguyen/taskflow_be/dto/request/RegisterRequest.java`
- Modify: `src/main/java/haonguyen/taskflow_be/dto/request/LoginRequest.java`
- Modify: `src/main/java/haonguyen/taskflow_be/mapper/UserMapper.java`

- [ ] **Step 1: Implement `TokenResponse.java`**

```java
package haonguyen.taskflow_be.dto.response;

public record TokenResponse(String accessToken, long expiresIn) {}
```

- [ ] **Step 2: Implement `UserResponse.java`**

```java
package haonguyen.taskflow_be.dto.response;

import java.time.Instant;

public record UserResponse(Long id, String username, String email, Instant createdAt) {}
```

- [ ] **Step 3: Implement `ErrorResponse.java`**

```java
package haonguyen.taskflow_be.dto.response;

public record ErrorResponse(String error, String message, Object details) {
    public record FieldError(String field, String message) {}
}
```

- [ ] **Step 4: Implement `RegisterRequest.java`**

```java
package haonguyen.taskflow_be.dto.request;

import jakarta.validation.constraints.*;

public record RegisterRequest(
    @NotBlank
    @Size(min = 3, max = 50)
    @Pattern(
        regexp = "^[a-zA-Z][a-zA-Z0-9_]{2,49}$",
        message = "Username must be 3–50 characters, letters/numbers/underscores, starting with a letter"
    )
    String username,

    @NotBlank
    @Email
    @Size(max = 100)
    String email,

    @NotBlank
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()\\-_=+\\[\\]{}|;:'\",.<>?/~`\\\\]).{8,72}$",
        message = "Password must be 8–72 characters with uppercase, lowercase, number, and special character"
    )
    String password
) {}
```

- [ ] **Step 5: Implement `LoginRequest.java`**

```java
package haonguyen.taskflow_be.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank String email,
    @NotBlank String password
) {}
```

- [ ] **Step 6: Implement `UserMapper.java`**

```java
package haonguyen.taskflow_be.mapper;

import haonguyen.taskflow_be.dto.response.UserResponse;
import haonguyen.taskflow_be.entity.User;

public class UserMapper {
    private UserMapper() {}

    public static UserResponse toResponse(User user) {
        return new UserResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getCreatedAt()
        );
    }
}
```

- [ ] **Step 7: Commit**
```bash
git add src/main/java/haonguyen/taskflow_be/dto/ src/main/java/haonguyen/taskflow_be/mapper/UserMapper.java
git commit -m "feat(be): implement auth DTOs and UserMapper"
```

---

### Task 5: Exception classes + GlobalExceptionHandler (4 min)

**Files:**
- Modify: `src/main/java/haonguyen/taskflow_be/exception/ResourceNotFoundException.java`
- Modify: `src/main/java/haonguyen/taskflow_be/exception/DuplicateResourceException.java`
- Modify: `src/main/java/haonguyen/taskflow_be/exception/AccessDeniedException.java`
- Modify: `src/main/java/haonguyen/taskflow_be/exception/AccountLockedException.java`
- Modify: `src/main/java/haonguyen/taskflow_be/exception/BusinessRuleException.java`
- Modify: `src/main/java/haonguyen/taskflow_be/exception/GlobalExceptionHandler.java`

- [ ] **Step 1: Implement the five exception classes**

`ResourceNotFoundException.java`:
```java
package haonguyen.taskflow_be.exception;

public class ResourceNotFoundException extends RuntimeException {
    private final String errorCode;
    public ResourceNotFoundException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    public String getErrorCode() { return errorCode; }
}
```

`DuplicateResourceException.java`:
```java
package haonguyen.taskflow_be.exception;

public class DuplicateResourceException extends RuntimeException {
    private final String errorCode;
    public DuplicateResourceException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    public String getErrorCode() { return errorCode; }
}
```

`AccessDeniedException.java`:
```java
package haonguyen.taskflow_be.exception;

public class AccessDeniedException extends RuntimeException {
    private final String errorCode;
    public AccessDeniedException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    public String getErrorCode() { return errorCode; }
}
```

`AccountLockedException.java`:
```java
package haonguyen.taskflow_be.exception;

import java.time.Instant;

public class AccountLockedException extends RuntimeException {
    private final Instant lockedUntil;
    public AccountLockedException(Instant lockedUntil) {
        super("Account is locked due to too many failed login attempts");
        this.lockedUntil = lockedUntil;
    }
    public Instant getLockedUntil() { return lockedUntil; }
}
```

`BusinessRuleException.java`:
```java
package haonguyen.taskflow_be.exception;

public class BusinessRuleException extends RuntimeException {
    private final String errorCode;
    public BusinessRuleException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    public String getErrorCode() { return errorCode; }
}
```

- [ ] **Step 2: Implement `GlobalExceptionHandler.java`**

```java
package haonguyen.taskflow_be.exception;

import haonguyen.taskflow_be.dto.response.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ex.getErrorCode(), ex.getMessage(), null));
    }

    @ExceptionHandler(DuplicateResourceException.class)
    ResponseEntity<ErrorResponse> handleDuplicate(DuplicateResourceException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(ex.getErrorCode(), ex.getMessage(), null));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(ex.getErrorCode(), ex.getMessage(), null));
    }

    @ExceptionHandler(AccountLockedException.class)
    ResponseEntity<ErrorResponse> handleLocked(AccountLockedException ex) {
        return ResponseEntity.status(423)
                .body(new ErrorResponse(
                        "ACCOUNT_LOCKED",
                        ex.getMessage(),
                        Map.of("lockedUntil", ex.getLockedUntil().toString())
                ));
    }

    @ExceptionHandler(BusinessRuleException.class)
    ResponseEntity<ErrorResponse> handleBusinessRule(BusinessRuleException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(ex.getErrorCode(), ex.getMessage(), null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(e -> new ErrorResponse.FieldError(e.getField(), e.getDefaultMessage()))
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        "VALIDATION_FAILED",
                        "Validation failed",
                        Map.of("fieldErrors", fieldErrors)
                ));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred", null));
    }
}
```

- [ ] **Step 3: Commit**
```bash
git add src/main/java/haonguyen/taskflow_be/exception/
git commit -m "feat(be): implement exception hierarchy and GlobalExceptionHandler"
```

---

### Task 6: UserPrincipal + CustomUserDetailsService (3 min)

**Files:**
- Modify: `src/main/java/haonguyen/taskflow_be/security/UserPrincipal.java`
- Modify: `src/main/java/haonguyen/taskflow_be/security/CustomUserDetailsService.java`

- [ ] **Step 1: Implement `UserPrincipal.java`**

```java
package haonguyen.taskflow_be.security;

import haonguyen.taskflow_be.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.List;

public class UserPrincipal implements UserDetails {
    private final Long id;
    private final String email;
    private final String actualUsername;
    private final String password;

    public UserPrincipal(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.actualUsername = user.getUsername();
        this.password = user.getPassword();
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getActualUsername() { return actualUsername; }

    // Spring Security uses getUsername() for the principal name — we use email
    @Override public String getUsername() { return email; }
    @Override public String getPassword() { return password; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return List.of(); }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
```

- [ ] **Step 2: Implement `CustomUserDetailsService.java`**

```java
package haonguyen.taskflow_be.security;

import haonguyen.taskflow_be.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        var user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
        return new UserPrincipal(user);
    }

    public UserDetails loadById(Long id) {
        var user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + id));
        return new UserPrincipal(user);
    }
}
```

- [ ] **Step 3: Commit**
```bash
git add src/main/java/haonguyen/taskflow_be/security/UserPrincipal.java src/main/java/haonguyen/taskflow_be/security/CustomUserDetailsService.java
git commit -m "feat(be): implement UserPrincipal and CustomUserDetailsService"
```

---

### Task 7: JwtTokenProvider (4 min)

**Files:**
- Modify: `src/main/java/haonguyen/taskflow_be/security/JwtTokenProvider.java`

- [ ] **Step 1: Implement `JwtTokenProvider.java`**

```java
package haonguyen.taskflow_be.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {
    private final SecretKey key;
    private final long accessTokenExpiration;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = accessTokenExpiration;
    }

    public String generateAccessToken(UserPrincipal principal) {
        var now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(principal.getId()))
                .claim("username", principal.getActualUsername())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessTokenExpiration))
                .signWith(key)
                .compact();
    }

    public Long extractUserId(String token) {
        return Long.parseLong(parseClaims(token).getSubject());
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
```

- [ ] **Step 2: Commit**
```bash
git add src/main/java/haonguyen/taskflow_be/security/JwtTokenProvider.java
git commit -m "feat(be): implement JwtTokenProvider"
```

---

### Task 8: JwtAuthenticationFilter (3 min)

**Files:**
- Modify: `src/main/java/haonguyen/taskflow_be/security/JwtAuthenticationFilter.java`

- [ ] **Step 1: Implement `JwtAuthenticationFilter.java`**

```java
package haonguyen.taskflow_be.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider,
                                   CustomUserDetailsService userDetailsService) {
        this.tokenProvider = tokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        var token = extractToken(request);
        if (StringUtils.hasText(token) && tokenProvider.validateToken(token)) {
            var userId = tokenProvider.extractUserId(token);
            var userDetails = userDetailsService.loadById(userId);
            var auth = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        chain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        var bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
```

- [ ] **Step 2: Commit**
```bash
git add src/main/java/haonguyen/taskflow_be/security/JwtAuthenticationFilter.java
git commit -m "feat(be): implement JwtAuthenticationFilter"
```

---

### Task 9: SecurityConfig + WebConfig (4 min)

**Files:**
- Modify: `src/main/java/haonguyen/taskflow_be/config/SecurityConfig.java`
- Modify: `src/main/java/haonguyen/taskflow_be/config/WebConfig.java`

- [ ] **Step 1: Implement `SecurityConfig.java`**

```java
package haonguyen.taskflow_be.config;

import haonguyen.taskflow_be.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
```

- [ ] **Step 2: Implement `WebConfig.java`**

```java
package haonguyen.taskflow_be.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:5177")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
```

- [ ] **Step 3: Commit**
```bash
git add src/main/java/haonguyen/taskflow_be/config/SecurityConfig.java src/main/java/haonguyen/taskflow_be/config/WebConfig.java
git commit -m "feat(be): configure Spring Security filter chain and CORS"
```

---

### Task 10: Complete V11 seed data (4 min)

> Prerequisite: Use the BCrypt hash captured in Task 1, Step 2.

**Files:**
- Modify: `src/main/resources/db/migration/V11__seed_data.sql`

- [ ] **Step 1: Replace the contents of `V11__seed_data.sql`**

Replace `$PASTE_HASH_FROM_TASK_1_HERE$` with the `$2a$10$...` hash you captured in Task 1.  
All 3 users share the same hash since they all use password `Test1234!`.

```sql
-- V11__seed_data.sql
-- Dev seed data — all passwords are Test1234!
-- BCrypt hash generated by running HashGeneratorTest (cost factor 10)

-- ── Users ─────────────────────────────────────────────────────────────────────
INSERT INTO users (username, email, password) VALUES
    ('alex_lead',   'alex@example.com',   '$PASTE_HASH_FROM_TASK_1_HERE$'),
    ('sam_dev',     'sam@example.com',    '$PASTE_HASH_FROM_TASK_1_HERE$'),
    ('jordan_free', 'jordan@example.com', '$PASTE_HASH_FROM_TASK_1_HERE$');

-- ── Projects ──────────────────────────────────────────────────────────────────
INSERT INTO projects (name, description, owner_id) VALUES
    ('TaskFlow Demo', 'Sample project for development',
     (SELECT id FROM users WHERE username = 'alex_lead')),
    ('API Design',    'REST API design and documentation',
     (SELECT id FROM users WHERE username = 'alex_lead'));

-- ── Project members ───────────────────────────────────────────────────────────
INSERT INTO project_members (project_id, user_id, role) VALUES
    ((SELECT id FROM projects WHERE name = 'TaskFlow Demo'),
     (SELECT id FROM users WHERE username = 'alex_lead'),   'OWNER'),
    ((SELECT id FROM projects WHERE name = 'TaskFlow Demo'),
     (SELECT id FROM users WHERE username = 'sam_dev'),     'MEMBER'),
    ((SELECT id FROM projects WHERE name = 'TaskFlow Demo'),
     (SELECT id FROM users WHERE username = 'jordan_free'), 'MEMBER'),
    ((SELECT id FROM projects WHERE name = 'API Design'),
     (SELECT id FROM users WHERE username = 'alex_lead'),   'OWNER');

-- ── Tasks (6 in TaskFlow Demo, all 4 columns covered) ─────────────────────────
INSERT INTO tasks (title, status, priority, project_id, assignee_id, created_by) VALUES
    ('Set up authentication', 'TODO', 'HIGH',
     (SELECT id FROM projects WHERE name = 'TaskFlow Demo'),
     (SELECT id FROM users WHERE username = 'alex_lead'),
     (SELECT id FROM users WHERE username = 'alex_lead')),

    ('Design kanban UI mockups', 'TODO', 'MEDIUM',
     (SELECT id FROM projects WHERE name = 'TaskFlow Demo'),
     (SELECT id FROM users WHERE username = 'sam_dev'),
     (SELECT id FROM users WHERE username = 'alex_lead')),

    ('Implement database schema', 'IN_PROGRESS', 'HIGH',
     (SELECT id FROM projects WHERE name = 'TaskFlow Demo'),
     (SELECT id FROM users WHERE username = 'sam_dev'),
     (SELECT id FROM users WHERE username = 'alex_lead')),

    ('Code review login module', 'IN_REVIEW', 'MEDIUM',
     (SELECT id FROM projects WHERE name = 'TaskFlow Demo'),
     (SELECT id FROM users WHERE username = 'alex_lead'),
     (SELECT id FROM users WHERE username = 'sam_dev')),

    ('Set up project repository', 'DONE', 'LOW',
     (SELECT id FROM projects WHERE name = 'TaskFlow Demo'),
     (SELECT id FROM users WHERE username = 'alex_lead'),
     (SELECT id FROM users WHERE username = 'alex_lead')),

    ('Write API documentation', 'DONE', 'MEDIUM',
     (SELECT id FROM projects WHERE name = 'TaskFlow Demo'),
     (SELECT id FROM users WHERE username = 'jordan_free'),
     (SELECT id FROM users WHERE username = 'alex_lead'));

-- ── Labels ────────────────────────────────────────────────────────────────────
INSERT INTO labels (name, color, project_id) VALUES
    ('frontend', '#3B82F6', (SELECT id FROM projects WHERE name = 'TaskFlow Demo')),
    ('backend',  '#10B981', (SELECT id FROM projects WHERE name = 'TaskFlow Demo')),
    ('bug',      '#EF4444', (SELECT id FROM projects WHERE name = 'TaskFlow Demo'));

-- ── Task labels ───────────────────────────────────────────────────────────────
INSERT INTO task_labels (task_id, label_id) VALUES
    ((SELECT id FROM tasks WHERE title = 'Set up authentication'),
     (SELECT id FROM labels WHERE name = 'backend'
       AND project_id = (SELECT id FROM projects WHERE name = 'TaskFlow Demo'))),

    ((SELECT id FROM tasks WHERE title = 'Design kanban UI mockups'),
     (SELECT id FROM labels WHERE name = 'frontend'
       AND project_id = (SELECT id FROM projects WHERE name = 'TaskFlow Demo'))),

    ((SELECT id FROM tasks WHERE title = 'Implement database schema'),
     (SELECT id FROM labels WHERE name = 'backend'
       AND project_id = (SELECT id FROM projects WHERE name = 'TaskFlow Demo'))),

    ((SELECT id FROM tasks WHERE title = 'Code review login module'),
     (SELECT id FROM labels WHERE name = 'backend'
       AND project_id = (SELECT id FROM projects WHERE name = 'TaskFlow Demo')));

-- ── Comments ──────────────────────────────────────────────────────────────────
INSERT INTO comments (content, task_id, author_id) VALUES
    ('Database schema looks good, just need to add the indexes.',
     (SELECT id FROM tasks WHERE title = 'Implement database schema'),
     (SELECT id FROM users WHERE username = 'alex_lead')),

    ('Working on the Flyway migrations now.',
     (SELECT id FROM tasks WHERE title = 'Implement database schema'),
     (SELECT id FROM users WHERE username = 'sam_dev')),

    ('Looks good to me. Just fix the typo on line 12.',
     (SELECT id FROM tasks WHERE title = 'Code review login module'),
     (SELECT id FROM users WHERE username = 'alex_lead'));
```

- [ ] **Step 2: Commit**
```bash
git add src/main/resources/db/migration/V11__seed_data.sql
git commit -m "feat(be): complete V11 seed data for all tables"
```

---

### Task 11: AuthService (5 min)

**Files:**
- Modify: `src/main/java/haonguyen/taskflow_be/service/AuthService.java`

- [ ] **Step 1: Implement `AuthService.java`**

```java
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
```

- [ ] **Step 2: Commit**
```bash
git add src/main/java/haonguyen/taskflow_be/service/AuthService.java
git commit -m "feat(be): implement AuthService with register, login, refresh, logout"
```

---

### Task 12: AuthController + UserController (4 min)

**Files:**
- Modify: `src/main/java/haonguyen/taskflow_be/controller/AuthController.java`
- Modify: `src/main/java/haonguyen/taskflow_be/service/UserService.java`
- Modify: `src/main/java/haonguyen/taskflow_be/controller/UserController.java`

- [ ] **Step 1: Implement `AuthController.java`**

```java
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
```

- [ ] **Step 2: Implement `UserService.java`**

```java
package haonguyen.taskflow_be.service;

import haonguyen.taskflow_be.dto.response.UserResponse;
import haonguyen.taskflow_be.exception.ResourceNotFoundException;
import haonguyen.taskflow_be.mapper.UserMapper;
import haonguyen.taskflow_be.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse getMe(Long userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "User not found"));
        return UserMapper.toResponse(user);
    }
}
```

- [ ] **Step 3: Implement `UserController.java`**

```java
package haonguyen.taskflow_be.controller;

import haonguyen.taskflow_be.dto.response.UserResponse;
import haonguyen.taskflow_be.security.UserPrincipal;
import haonguyen.taskflow_be.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(userService.getMe(principal.getId()));
    }
}
```

- [ ] **Step 4: Commit**
```bash
git add src/main/java/haonguyen/taskflow_be/controller/AuthController.java src/main/java/haonguyen/taskflow_be/service/UserService.java src/main/java/haonguyen/taskflow_be/controller/UserController.java
git commit -m "feat(be): implement AuthController and UserController"
```

---

### Task 13: AuthService unit tests (5 min)

**Files:**
- Create: `src/test/java/haonguyen/taskflow_be/service/AuthServiceTest.java`

- [ ] **Step 1: Create `AuthServiceTest.java`**

```java
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

    PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4); // low cost for tests
    AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository, refreshTokenRepository, loginAttemptRepository,
                passwordEncoder, jwtTokenProvider, 604800000L);
    }

    // ── register ─────────────────────────────────────────────────────────────

    @Test
    void register_savesUserAndReturnsResponse() {
        when(userRepository.existsByUsernameIgnoreCase("alice")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("alice@example.com")).thenReturn(false);

        var saved = new User();
        saved.setUsername("alice");
        saved.setEmail("alice@example.com");
        // Simulate @PrePersist via reflection or just accept null createdAt in test
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
```

- [ ] **Step 2: Run tests**
```bash
./gradlew test --tests "haonguyen.taskflow_be.service.AuthServiceTest"
```
Expected: PASS (6 tests)

- [ ] **Step 3: Commit**
```bash
git add src/test/java/haonguyen/taskflow_be/service/AuthServiceTest.java
git commit -m "test(be): add AuthService unit tests"
```

---

### Task 14: AuthController @WebMvcTest (5 min)

**Files:**
- Create: `src/test/java/haonguyen/taskflow_be/controller/AuthControllerTest.java`

- [ ] **Step 1: Create `AuthControllerTest.java`**

```java
package haonguyen.taskflow_be.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import haonguyen.taskflow_be.dto.response.TokenResponse;
import haonguyen.taskflow_be.dto.response.UserResponse;
import haonguyen.taskflow_be.exception.BusinessRuleException;
import haonguyen.taskflow_be.security.JwtAuthenticationFilter;
import haonguyen.taskflow_be.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.time.Instant;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean AuthService authService;
    @MockBean JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void register_returns201_onSuccess() throws Exception {
        var userResponse = new UserResponse(1L, "alice", "alice@example.com", Instant.now());
        when(authService.register(any())).thenReturn(userResponse);

        var body = Map.of("username", "alice", "email", "alice@example.com", "password", "Test1234!");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.email").value("alice@example.com"));
    }

    @Test
    void register_returns400_onMissingFields() throws Exception {
        var body = Map.of("email", "alice@example.com");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void login_returns200WithToken_onValidCredentials() throws Exception {
        var token = new TokenResponse("access-token-123", 900000L);
        when(authService.login(any())).thenReturn(new AuthService.AuthResult(token, "raw-refresh"));

        var body = Map.of("email", "alice@example.com", "password", "Test1234!");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token-123"))
                .andExpect(jsonPath("$.expiresIn").value(900000));
    }

    @Test
    void login_returns400_onInvalidCredentials() throws Exception {
        when(authService.login(any()))
                .thenThrow(new BusinessRuleException("INVALID_CREDENTIALS", "Invalid email or password"));

        var body = Map.of("email", "alice@example.com", "password", "wrong");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"));
    }

    @Test
    void logout_returns204() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isNoContent());
    }
}
```

- [ ] **Step 2: Run tests**
```bash
./gradlew test --tests "haonguyen.taskflow_be.controller.AuthControllerTest"
```
Expected: PASS (5 tests)

- [ ] **Step 3: Commit**
```bash
git add src/test/java/haonguyen/taskflow_be/controller/AuthControllerTest.java
git commit -m "test(be): add AuthController WebMvcTest"
```

---

### Task 15: Verify end-to-end (5 min)

> Prerequisite: PostgreSQL running on `localhost:1432` with DB `TaskFlowDB` (user: `postgres`, pass: `Banana100`).

- [ ] **Step 1: Run all tests**
```bash
./gradlew test
```
Expected: All tests pass (skip `TaskflowBeApplicationTests` contextLoads if DB is not up — that's fine for now).

- [ ] **Step 2: Start the application**
```bash
./gradlew bootRun --args="--spring.profiles.active=dev"
```
Expected output:
```
Started TaskflowBeApplication in x.xxx seconds
```
Flyway will apply V1–V11 on the first run.

- [ ] **Step 3: Verify Swagger UI loads**

Open `http://localhost:8087/swagger-ui.html` in the browser.  
Expected: Swagger UI shows endpoints under `/api/v1/auth` and `/api/v1/users`.

- [ ] **Step 4: Test register via Swagger**

POST `/api/v1/auth/register` with:
```json
{ "username": "testuser", "email": "test@example.com", "password": "Test1234!" }
```
Expected: 201 with `{ "id": 4, "username": "testuser", "email": "test@example.com", "createdAt": "..." }`

- [ ] **Step 5: Test login via Swagger**

POST `/api/v1/auth/login` with:
```json
{ "email": "alex@example.com", "password": "Test1234!" }
```
Expected: 200 with `{ "accessToken": "eyJ...", "expiresIn": 900000 }`  
Response headers should include `Set-Cookie: refreshToken=...; HttpOnly; Path=/api/v1/auth`

- [ ] **Step 6: Test GET /users/me**

Copy the `accessToken` from Step 5. In Swagger, authorize with `Bearer <token>`.  
GET `/api/v1/users/me`.  
Expected: 200 with alex_lead's user info.

- [ ] **Step 7: Test duplicate username error**

POST `/api/v1/auth/register` with `username: "alex_lead"`.  
Expected: 409 with `{ "error": "DUPLICATE_USERNAME", "message": "Username is already taken" }`

- [ ] **Step 8: Final commit**
```bash
git add -A
git commit -m "feat(be): complete auth backend — register, login, refresh, logout, GET /users/me"
```
