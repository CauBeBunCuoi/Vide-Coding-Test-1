# API Rules — TaskFlow Backend

## General Conventions

- Base URL: `/api/v1`
- All requests and responses: JSON (`Content-Type: application/json`)
- Auth header: `Authorization: Bearer <accessToken>`
- Timestamps: ISO 8601 UTC (`2025-06-15T10:30:00Z`)
- Date-only fields (deadline): `YYYY-MM-DD`
- IDs: 64-bit Long. Nullable fields omitted from responses when null (unless stated otherwise).

## Pagination

Endpoints that paginate accept `page` (0-indexed, default 0) and `size` (default 20, max 100).

Response wrapper:
```json
{ "content": [...], "page": 0, "size": 20, "totalElements": 45, "totalPages": 3 }
```

In Spring: controllers accept `page`+`size` params → `PageRequest.of(page, size)` → service returns `Page<Entity>` → controller maps to `PageResponse<DTO>`.

## Error Response Format

Every error uses this exact shape — never deviate:
```json
{ "error": "ERROR_CODE", "message": "Human-readable message", "details": { ... } }
```

- `error`: SCREAMING_SNAKE_CASE — used programmatically by frontend (switch statements)
- `message`: user-friendly sentence — can be displayed directly in the UI
- `details`: optional; used for validation errors: `{ "fieldErrors": [{ "field": "title", "message": "Title is required" }] }`

## HTTP Status Codes

| Status | When |
|--------|------|
| 200 | Success with body (read, update) |
| 201 | Resource created |
| 204 | Success, no body (delete, logout) |
| 400 | Validation failure OR business rule violation |
| 401 | Missing/invalid access token; invalid refresh token |
| 403 | Authenticated but lacks permission |
| 404 | Resource not found |
| 409 | Conflict (duplicate unique constraint) |
| 423 | Account locked |
| 429 | Rate limit exceeded |
| 500 | Unexpected server error |

## JWT & Token Flow

- Access token: HS256, 15-min lifetime. Payload: `{ sub: userId, username, iat, exp }`.
- Refresh token: 128-char random string. Store **SHA-256 hash** in DB (`refresh_tokens.token_hash`). Never store raw value.
- Cookie: `HttpOnly; Secure; SameSite=Strict; Path=/api/v1/auth; Max-Age=604800`
- Rotation: on each refresh, delete old token row + insert new one.

Permitted without auth:
```
/api/v1/auth/register
/api/v1/auth/login
/api/v1/auth/refresh
/swagger-ui/**
/v3/api-docs/**
/actuator/health
```

## CORS Configuration

```java
config.setAllowedOrigins(List.of("http://localhost:5173"));
config.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
config.setAllowCredentials(true);  // REQUIRED for httpOnly cookie
// allowedOrigins must NOT be * when allowCredentials=true
```

`PATCH` must be included (used for all update operations).

## Security Headers

Set on all API responses:
```
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
Cache-Control: no-store
```

## Rate Limits

| Endpoint group | Limit | Window |
|----------------|-------|--------|
| POST /auth/login | 10 req per email | 15 min |
| POST /auth/register | 5 req per IP | 1 hour |
| POST /auth/refresh | 30 req per user | 15 min |
| All other authenticated | 100 req per user | 1 min |

On 429: include `Retry-After: <seconds>` header.

## Account Locking

After 5 consecutive failed login attempts for the same email within 15 minutes → return 423 ACCOUNT_LOCKED with `lockedUntil`.
Check: count `login_attempts` where `email=?` AND `success=false` AND `attempted_at > NOW()-15min`.
Lock is time-based — no manual unlock needed.

## Error Code Catalog

| Code | Status | Domain |
|------|--------|--------|
| VALIDATION_FAILED | 400 | Input validation (any) |
| INVALID_CREDENTIALS | 401 | Auth |
| INVALID_REFRESH_TOKEN | 401 | Auth |
| ACCOUNT_LOCKED | 423 | Auth |
| DUPLICATE_USERNAME | 409 | Users |
| DUPLICATE_EMAIL | 409 | Users |
| PROJECT_NOT_FOUND | 404 | Projects |
| NOT_PROJECT_MEMBER | 403 | Projects |
| NOT_PROJECT_OWNER | 403 | Projects |
| CONFIRMATION_MISMATCH | 400 | Projects |
| USER_NOT_FOUND | 404 | Members |
| ALREADY_MEMBER | 409 | Members |
| LAST_OWNER | 400 | Members |
| OWNER_CANNOT_LEAVE | 400 | Members |
| TASK_NOT_FOUND | 404 | Tasks |
| NOT_TASK_OWNER | 403 | Tasks |
| ASSIGNEE_NOT_MEMBER | 400 | Tasks |
| LABEL_NOT_IN_PROJECT | 400 | Tasks/Labels |
| TOO_MANY_LABELS | 400 | Tasks/Labels |
| DUPLICATE_LABEL_NAME | 409 | Labels |
| LABEL_ALREADY_ATTACHED | 409 | Task-Labels |
| NOT_COMMENT_AUTHOR | 403 | Comments |
| NOT_AUTHORIZED | 403 | Comments |
| RATE_LIMITED | 429 | Global |
| INTERNAL_ERROR | 500 | Global |
