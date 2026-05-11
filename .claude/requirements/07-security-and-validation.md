# TaskFlow — Security & Validation

## 1. Authentication Flow

TaskFlow uses JWT-based authentication with access + refresh token pairs and refresh token rotation.

### 1.1 Token Lifecycle

**Access Token:**
- Algorithm: HS256 (HMAC-SHA256) — symmetric key stored in `application.yml`
- Lifetime: 15 minutes (900 seconds)
- Stored: in-memory JavaScript variable on the frontend (Zustand auth store)
- Payload claims:
  ```json
  {
    "sub": "1",             // user ID as string
    "username": "alex_lead",
    "iat": 1718444400,      // issued at (epoch seconds)
    "exp": 1718445300       // expires at (epoch seconds)
  }
  ```
- Sent via: `Authorization: Bearer <token>` header on every authenticated request

**Refresh Token:**
- Format: 128-character cryptographically random string (generated with `SecureRandom`)
- Lifetime: 7 days (604800 seconds)
- Stored: httpOnly cookie (`refreshToken`) on the frontend — JavaScript cannot access it
- Stored on server: SHA-256 hash of the token in the `refresh_tokens` table
- Cookie attributes: `HttpOnly; Secure; SameSite=Strict; Path=/api/v1/auth; Max-Age=604800`

### 1.2 Token Refresh Flow (Rotation)

When the access token expires:

1. Frontend Axios interceptor catches the 401 response
2. Interceptor calls `POST /api/v1/auth/refresh` — the browser automatically sends the httpOnly cookie
3. Backend validates the refresh token:
   a. Hash the token from the cookie with SHA-256
   b. Look up the hash in `refresh_tokens` table
   c. Check `expires_at > NOW()`
   d. If valid: delete the old refresh token row, generate a new refresh token, insert the new hash, generate a new access token
   e. If invalid or expired: return 401
4. Backend responds with new `accessToken` in the body and sets a new `refreshToken` cookie
5. Frontend updates the auth store with the new access token
6. The original failed request is retried with the new access token

**Concurrency handling:** If multiple requests fail with 401 simultaneously, the frontend must queue them and only send one refresh request. All queued requests are retried after the single refresh completes. Implementation:

```typescript
let isRefreshing = false;
let failedQueue: Array<{ resolve: Function; reject: Function }> = [];

// In the 401 response interceptor:
if (!isRefreshing) {
  isRefreshing = true;
  try {
    const newToken = await refreshToken();
    authStore.setAccessToken(newToken);
    // Retry all queued requests
    failedQueue.forEach(({ resolve }) => resolve(newToken));
  } catch {
    failedQueue.forEach(({ reject }) => reject(error));
    authStore.logout();
    redirect('/login');
  } finally {
    failedQueue = [];
    isRefreshing = false;
  }
} else {
  // Queue this request
  return new Promise((resolve, reject) => {
    failedQueue.push({ resolve, reject });
  }).then(token => {
    originalRequest.headers.Authorization = `Bearer ${token}`;
    return axiosInstance(originalRequest);
  });
}
```

### 1.3 Logout Flow

1. Frontend calls `POST /api/v1/auth/logout`
2. Backend reads the refresh token from the cookie, hashes it, deletes the matching row from `refresh_tokens`
3. Backend clears the `refreshToken` cookie by setting `Max-Age=0`
4. Frontend clears the access token from the auth store
5. Frontend redirects to `/login`

### 1.4 Account Locking

After 5 consecutive failed login attempts for the same email address within a 15-minute window, the account is locked:

- The backend checks the `login_attempts` table: count rows where `email = ?` AND `success = false` AND `attempted_at > NOW() - INTERVAL '15 minutes'`
- If count >= 5, return 423 with `lockedUntil` timestamp
- The lock is time-based — no manual unlock needed. After 15 minutes, the failed attempts age out
- A successful login resets the counter (by inserting a `success = true` row)
- The frontend displays the remaining lock time: "Account locked. Try again in X minutes."

---

## 2. Password Security

### 2.1 Password Requirements

All of these must be met:

| Rule | Constraint |
|------|-----------|
| Minimum length | 8 characters |
| Maximum length | 72 characters (BCrypt limit) |
| Uppercase letter | At least 1 |
| Lowercase letter | At least 1 |
| Digit | At least 1 |
| Special character | At least 1 from: `!@#$%^&*()_+-=[]{}|;:',.<>?/~` |

Validation regex (for backend `@Pattern` annotation):
```
^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*()_+\-=\[\]{}|;:',.<>?/~`]).{8,72}$
```

### 2.2 Password Hashing

- Algorithm: BCrypt (via Spring Security's `BCryptPasswordEncoder`)
- Cost factor: 10 (default)
- The hashed password is always 60 characters, stored in a `VARCHAR(255)` column for future algorithm changes

### 2.3 Frontend Password Strength Indicator

The registration form shows real-time feedback as the user types:

```
✓ At least 8 characters
✗ Uppercase letter
✓ Lowercase letter
✓ Number
✗ Special character
```

Each rule is checked independently and shown with a green check or red cross. The "Create account" button is disabled until all five checks pass.

This is purely a UX convenience — the backend performs the same validation and is the authoritative gate.

---

## 3. Input Validation Rules

Every field validated on both frontend (client-side, for immediate UX feedback) and backend (server-side, authoritative). The backend validation is the source of truth. Frontend validation matches it but is advisory only.

### 3.1 User Fields

| Field | Type | Required | Min | Max | Pattern | Notes |
|-------|------|----------|-----|-----|---------|-------|
| username | string | yes | 3 | 50 | `^[a-zA-Z][a-zA-Z0-9_]*$` | Must start with a letter. Only letters, numbers, underscores. |
| email | string | yes | — | 100 | Standard email regex | Stored in lowercase. Validated with `@Email` (Jakarta). |
| password | string | yes | 8 | 72 | See password regex above | Only validated on registration, not on profile update (password change is out of scope for v1). |

### 3.2 Project Fields

| Field | Type | Required | Min | Max | Pattern | Notes |
|-------|------|----------|-----|-----|---------|-------|
| name | string | yes | 1 | 100 | — | Trimmed. Must not be blank (whitespace-only is rejected). |
| description | string | no | — | 1000 | — | Null allowed. Empty string treated as null. |
| confirmName | string | yes (on delete) | — | — | — | Must exactly match the project name (case-sensitive). |

### 3.3 Task Fields

| Field | Type | Required | Min | Max | Allowed Values | Notes |
|-------|------|----------|-----|-----|----------------|-------|
| title | string | yes (create) | 1 | 200 | — | Trimmed. Must not be blank. |
| description | string | no | — | 5000 | — | Null allowed. Empty string `""` clears the description. |
| status | string | no (create: always TODO) | — | — | `TODO`, `IN_PROGRESS`, `IN_REVIEW`, `DONE` | On create, always defaults to TODO. On update, any valid value. |
| priority | string | no | — | — | `LOW`, `MEDIUM`, `HIGH`, `URGENT` | Default: `MEDIUM`. |
| deadline | string | no | — | — | `YYYY-MM-DD` format | Null clears the deadline. Past dates are allowed (overdue tracking). |
| assigneeId | long | no | — | — | — | Must be a member of the task's project. Null or absent means unassigned. |
| labelIds | long[] | no (create only) | — | 5 items | — | Each label must belong to the same project. Max 5 labels. |

### 3.4 Label Fields

| Field | Type | Required | Min | Max | Pattern | Notes |
|-------|------|----------|-----|-----|---------|-------|
| name | string | yes | 1 | 50 | — | Trimmed. Unique per project (case-insensitive). |
| color | string | yes | 7 | 7 | `^#[0-9A-Fa-f]{6}$` | Hex color code. Frontend restricts to 12 predefined colors; backend validates format only. |

### 3.5 Comment Fields

| Field | Type | Required | Min | Max | Notes |
|-------|------|----------|-----|-----|-------|
| content | string | yes | 1 | 2000 | Trimmed. Must not be blank. |

### 3.6 Member Fields

| Field | Type | Required | Allowed Values | Notes |
|-------|------|----------|----------------|-------|
| email | string | yes (invite) | Standard email | Must belong to a registered user. |
| role | string | yes (role change), optional (invite) | `OWNER`, `MEMBER` | Default: `MEMBER` on invite. |

### 3.7 Backend Validation Implementation

Use Jakarta Validation annotations on request DTOs:

```java
public record CreateTaskRequest(
    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must be at most 200 characters")
    String title,

    @Size(max = 5000, message = "Description must be at most 5000 characters")
    String description,

    @ValidEnum(enumClass = TaskPriority.class, message = "Priority must be LOW, MEDIUM, HIGH, or URGENT")
    String priority,

    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate deadline,

    Long assigneeId,

    @Size(max = 5, message = "A task can have at most 5 labels")
    List<Long> labelIds
) {}
```

The `GlobalExceptionHandler` catches `MethodArgumentNotValidException` and maps it to the standard error format with per-field error messages:

```json
{
  "error": "VALIDATION_FAILED",
  "message": "One or more fields are invalid",
  "details": {
    "fieldErrors": [
      { "field": "title", "message": "Title is required" },
      { "field": "priority", "message": "Priority must be LOW, MEDIUM, HIGH, or URGENT" }
    ]
  }
}
```

### 3.8 Frontend Validation Implementation

Use Zod schemas that mirror the backend constraints:

```typescript
const createTaskSchema = z.object({
  title: z.string().trim().min(1, 'Title is required').max(200, 'Title must be at most 200 characters'),
  description: z.string().max(5000).optional(),
  priority: z.enum(['LOW', 'MEDIUM', 'HIGH', 'URGENT']).optional(),
  deadline: z.string().regex(/^\d{4}-\d{2}-\d{2}$/).optional().nullable(),
  assigneeId: z.number().positive().optional().nullable(),
  labelIds: z.array(z.number().positive()).max(5).optional(),
});
```

React Hook Form integrates with Zod via `@hookform/resolvers/zod`. Validation runs on blur for text fields and on change for selects.

---

## 4. Authorization Matrix

This matrix defines who can perform each action. The backend checks these permissions in the service layer before executing any operation.

### 4.1 Project-Level Permissions

| Action | OWNER | MEMBER | Non-member |
|--------|-------|--------|------------|
| View project | ✅ | ✅ | ❌ 403 |
| Update project name/description | ✅ | ❌ 403 | ❌ 403 |
| Delete project | ✅ | ❌ 403 | ❌ 403 |
| Invite member | ✅ | ❌ 403 | ❌ 403 |
| Remove member | ✅ | ❌ 403 (self: ✅ leave) | ❌ 403 |
| Change member role | ✅ | ❌ 403 | ❌ 403 |
| Leave project | ❌ (must transfer ownership) | ✅ | N/A |

### 4.2 Task-Level Permissions

All task operations require being a member of the task's project first.

| Action | Any project member | Task creator | Project OWNER |
|--------|-------------------|--------------|---------------|
| View task | ✅ | ✅ | ✅ |
| Create task | ✅ | N/A | ✅ |
| Update task (any field) | ✅ | ✅ | ✅ |
| Delete task | ❌ 403 | ✅ | ✅ |

### 4.3 Comment-Level Permissions

All comment operations require being a member of the task's project.

| Action | Any project member | Comment author | Project OWNER |
|--------|-------------------|----------------|---------------|
| View comments | ✅ | ✅ | ✅ |
| Add comment | ✅ | N/A | ✅ |
| Edit comment | ❌ 403 | ✅ | ❌ 403 |
| Delete comment | ❌ 403 | ✅ | ✅ |

### 4.4 Label-Level Permissions

All label operations require being a member of the label's project.

| Action | Any project member |
|--------|-------------------|
| View labels | ✅ |
| Create label | ✅ |
| Update label | ✅ |
| Delete label | ✅ |
| Attach label to task | ✅ |
| Detach label from task | ✅ |

### 4.5 Backend Permission Check Pattern

Every service method that modifies data follows this pattern:

```java
public TaskResponse updateTask(Long taskId, UpdateTaskRequest request, Long authenticatedUserId) {
    // 1. Load the entity
    Task task = taskRepository.findById(taskId)
        .orElseThrow(() -> new ResourceNotFoundException("TASK_NOT_FOUND", "Task not found"));

    // 2. Check project membership
    if (!projectMemberRepository.existsByProjectIdAndUserId(task.getProject().getId(), authenticatedUserId)) {
        throw new AccessDeniedException("NOT_PROJECT_MEMBER", "You are not a member of this project");
    }

    // 3. Check action-specific permissions (e.g., for delete)
    // if (!task.getCreatedBy().getId().equals(authenticatedUserId) && !isProjectOwner(...)) {
    //     throw new AccessDeniedException("NOT_TASK_OWNER", "Only the task creator or project owner can delete");
    // }

    // 4. Execute the operation
    // ...
}
```

The `authenticatedUserId` comes from the `JwtAuthenticationFilter`, which extracts the `sub` claim from the JWT and sets it on the `SecurityContext`. Controllers access it via `@AuthenticationPrincipal UserPrincipal principal` and pass `principal.getId()` to services.

---

## 5. Spring Security Configuration

### 5.1 SecurityConfig

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())  // Disabled because we use JWT, not session cookies
            .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/v1/auth/register",
                    "/api/v1/auth/login",
                    "/api/v1/auth/refresh",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/actuator/health"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}
```

### 5.2 JWT Filter

The `JwtAuthenticationFilter` runs on every request:

1. Extract `Authorization` header
2. If absent or not `Bearer `, skip (let the request proceed unauthenticated — `permitAll` routes will pass, others will get 401)
3. Parse and validate the JWT
4. If valid, extract `sub` (user ID) and create a `UserPrincipal` on the SecurityContext
5. If expired or invalid, skip (do not throw — let Spring Security handle the 401)

### 5.3 CORS Policy

For development (with Vite proxy, CORS is not needed). For production or when frontend runs on a different port:

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of("http://localhost:5173"));
    config.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    config.setAllowCredentials(true);  // Required for httpOnly cookie (refresh token)
    config.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", config);
    return source;
}
```

Key points:
- `allowCredentials = true` is mandatory because the refresh token is sent as a cookie
- `allowedOrigins` must be explicit (not `*`) when credentials are enabled
- `PATCH` must be in allowedMethods since we use it for updates

---

## 6. Rate Limiting

Apply rate limiting at the API gateway or Spring level to prevent abuse.

| Endpoint group | Rate limit | Window |
|----------------|-----------|--------|
| `POST /auth/login` | 10 requests per email | 15 minutes |
| `POST /auth/register` | 5 requests per IP | 1 hour |
| `POST /auth/refresh` | 30 requests per user | 15 minutes |
| All other authenticated endpoints | 100 requests per user | 1 minute |

When rate limit is exceeded, return:
```
HTTP 429 Too Many Requests
Retry-After: 30          ← seconds until the limit resets

{
  "error": "RATE_LIMITED",
  "message": "Too many requests. Please try again later."
}
```

For v1, implement rate limiting using an in-memory map (e.g., Bucket4j or a simple `ConcurrentHashMap` with token bucket). Move to Redis-backed rate limiting if the application scales to multiple instances.

---

## 7. Security Headers

The backend should set these response headers on all API responses:

```
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
Cache-Control: no-store
```

These are configured in the Spring Security filter chain. The frontend SPA is served by Vite in dev and by a static server in production — the static server should set these headers on HTML responses:

```
Content-Security-Policy: default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:;
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
Referrer-Policy: strict-origin-when-cross-origin
```

---

## 8. Data Sanitization

**Backend:**
- All string inputs are trimmed before validation and storage
- HTML tags are NOT stripped from task descriptions and comments (they support markdown rendering), but the frontend renders them via `react-markdown` which sanitizes by default (no raw `dangerouslySetInnerHTML`)
- SQL injection is prevented by JPA parameterized queries — never concatenate user input into JPQL strings

**Frontend:**
- Never use `dangerouslySetInnerHTML`
- Use `react-markdown` for rendering markdown content — it sanitizes HTML by default
- User-provided text in other contexts (titles, labels, usernames) is rendered as text nodes, not as HTML
