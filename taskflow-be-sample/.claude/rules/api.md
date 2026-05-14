# API Rules

## Base URL & Format
Base URL: `/api/v1`. All requests and responses are JSON (`Content-Type: application/json`).
Timestamps: ISO 8601 UTC (`2025-06-15T10:30:00Z`). Date-only fields: `YYYY-MM-DD`.
IDs are 64-bit integers (Java `Long`, TypeScript `number`).
Nullable fields are omitted from responses when null, unless stated otherwise.

## Authentication
All non-public endpoints require: `Authorization: Bearer <accessToken>`.
Public endpoints (no auth required): `/api/v1/auth/register`, `/api/v1/auth/login`, `/api/v1/auth/refresh`, Swagger UI paths, `/actuator/health`.

## HTTP Status Codes
| Status | Meaning |
|--------|---------|
| 200 | Success — resource returned or updated |
| 201 | Created — new resource |
| 204 | No Content — delete, logout (no body) |
| 400 | Bad Request — validation error or business rule violation |
| 401 | Unauthorized — missing/invalid access token |
| 403 | Forbidden — authenticated but lacks permission |
| 404 | Not Found |
| 409 | Conflict — duplicate resource (unique constraint) |
| 423 | Locked — account locked after too many failed login attempts |
| 429 | Too Many Requests — rate limit exceeded |
| 500 | Internal Server Error |

## Error Response Shape
Every error uses this exact shape:
```json
{
  "error": "ERROR_CODE",
  "message": "Human-readable description suitable for display",
  "details": { "fieldErrors": [{ "field": "title", "message": "Title is required" }] }
}
```
- `error`: always SCREAMING_SNAKE_CASE (for programmatic frontend handling)
- `message`: user-friendly, can be shown directly in a toast or alert
- `details`: optional, only present for validation errors

## Pagination
Query params: `page` (0-indexed, default 0), `size` (default 20, max 100).
Response wrapper for all list endpoints:
```json
{ "content": [...], "page": 0, "size": 20, "totalElements": 45, "totalPages": 3 }
```
Use Spring Data `Pageable`. Controllers construct `PageRequest`. Services return `Page<Entity>`. Controllers map to `PageResponse<DTO>`.

## Controller Layer Rules
Controllers must ONLY:
1. Extract path variables and query params
2. Call the service method
3. Map result to a response DTO
4. Return HTTP response with correct status code

No business logic or direct repository access in controllers.
Services receive request DTOs. Services return entities. Mapping to DTOs in controllers or mappers.

## Validation
Use Jakarta Validation annotations (`@NotBlank`, `@Size`, `@Pattern`, `@Email`) on request DTO fields.
`GlobalExceptionHandler` catches `MethodArgumentNotValidException` → returns `VALIDATION_FAILED` with per-field errors.
Never throw raw Spring exceptions from service code — always wrap in domain exceptions.

## Exception Hierarchy
| Exception | HTTP Status | When to use |
|-----------|------------|-------------|
| `BusinessRuleException` | 400 | LAST_OWNER, ASSIGNEE_NOT_MEMBER, TOO_MANY_LABELS, CONFIRMATION_MISMATCH, etc. |
| `ResourceNotFoundException` | 404 | TASK_NOT_FOUND, PROJECT_NOT_FOUND, USER_NOT_FOUND |
| `DuplicateResourceException` | 409 | DUPLICATE_USERNAME, DUPLICATE_EMAIL, DUPLICATE_LABEL_NAME, ALREADY_MEMBER |
| `AccessDeniedException` | 403 | NOT_PROJECT_MEMBER, NOT_PROJECT_OWNER, NOT_TASK_OWNER, NOT_COMMENT_AUTHOR |
| `AccountLockedException` | 423 | ACCOUNT_LOCKED (includes `lockedUntil` timestamp) |

## Security Headers
Set on all API responses: `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Cache-Control: no-store`.

## CORS
- `allowCredentials = true` (required for httpOnly refresh token cookie)
- `allowedOrigins` must be explicit — never `*` when credentials are enabled
- `PATCH` must be in `allowedMethods`
- Allowed headers: `Authorization`, `Content-Type`

## Rate Limiting
| Endpoint | Limit | Window |
|----------|-------|--------|
| `POST /auth/login` | 10 requests per email | 15 min |
| `POST /auth/register` | 5 requests per IP | 1 hour |
| `POST /auth/refresh` | 30 requests per user | 15 min |
| All other authenticated endpoints | 100 requests per user | 1 min |

Return 429 with `Retry-After` header (seconds until reset).

## Shared Response Shapes
**UserSummary** (embedded in task, comment, member responses): `{ "id": 1, "username": "alex_lead" }`
**LabelSummary** (embedded in task responses): `{ "id": 1, "name": "frontend", "color": "#3B82F6" }`
**ProjectSummary** (embedded in task responses): `{ "id": 1, "name": "TaskFlow MVP" }`
