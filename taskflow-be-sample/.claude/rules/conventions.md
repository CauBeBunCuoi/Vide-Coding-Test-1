# Backend Conventions — TaskFlow

## Tech Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Language | Java | 24.0.2 |
| Framework | Spring Boot | 4.0.5 |
| Build | Gradle (Kotlin DSL) | 8.14+ |
| ORM | Spring Data JPA (Hibernate 7) | — |
| Database | PostgreSQL | 17.5 |
| Migrations | Flyway | 11 |
| Auth | Spring Security + JJWT | — |
| Validation | Jakarta Validation (Hibernate Validator) | — |
| Testing | JUnit 5 + Mockito + Testcontainers | — |

Base package: `com.example.demo`

## Project Structure

```
src/main/java/com/example/demo/
├── config/          SecurityConfig, JwtConfig, WebConfig
├── controller/      AuthController, ProjectController, TaskController, MemberController, LabelController, TaskLabelController, CommentController, UserController
├── dto/
│   ├── request/     CreateTaskRequest, UpdateTaskRequest, InviteMemberRequest, ...
│   └── response/    TaskResponse, ProjectDetailResponse, PageResponse<T>, ErrorResponse, UserSummaryResponse, ...
├── entity/          User, Project, ProjectMember, Task, Label, TaskLabel, Comment, RefreshToken, LoginAttempt
├── enums/           TaskStatus, TaskPriority, MemberRole
├── exception/       GlobalExceptionHandler (@RestControllerAdvice), ResourceNotFoundException, DuplicateResourceException, AccessDeniedException, AccountLockedException, BusinessRuleException
├── mapper/          TaskMapper, ProjectMapper, LabelMapper, CommentMapper, MemberMapper, UserMapper
├── repository/      TaskRepository, ProjectRepository, ProjectMemberRepository, LabelRepository, ...
├── security/        JwtTokenProvider, JwtAuthenticationFilter (OncePerRequestFilter), UserPrincipal (implements UserDetails)
└── service/         TaskService, ProjectService, MemberService, AuthService, LabelService, TaskLabelService, CommentService, UserService
```

## Naming Conventions

| Element | Convention | Example |
|---------|-----------|---------|
| Classes | PascalCase | `TaskController`, `CreateTaskRequest` |
| Methods | camelCase | `findByProjectId`, `createTask` |
| Constants | SCREAMING_SNAKE | `MAX_LABELS_PER_TASK = 5` |
| DB columns | snake_case | `created_at`, `project_id` |
| Enum values | SCREAMING_SNAKE | `IN_PROGRESS`, `HIGH` |
| REST paths | kebab-case, plural nouns | `/api/v1/projects/{projectId}/tasks` |
| Path variables | camelCase | `{projectId}`, `{taskId}` |
| DTO classes | Purpose + type suffix | `CreateTaskRequest`, `TaskResponse` |
| Service methods | verb + noun | `createTask`, `findTaskById`, `deleteTask` |
| Repository methods | Spring Data naming | `findByProjectIdAndStatus`, `existsByProjectIdAndUserId` |
| Packages | lowercase | `com.example.demo.controller` |
| Config properties | kebab-case | `jwt.access-token-expiration` |

## Layered Architecture

**Controllers** — thin only:
1. Extract path vars, query params, request body
2. Call the service method
3. Map entity to response DTO via mapper
4. Return `ResponseEntity` with correct HTTP status

No business logic in controllers. No direct repository calls from controllers.

**Services** — all business logic:
1. Load entity → throw `ResourceNotFoundException` if missing
2. Check membership/ownership → throw `AccessDeniedException`
3. Validate business rules → throw `BusinessRuleException`
4. Execute via repository
5. Return entity (not DTO) — mapping happens in controller/mapper

**Repositories** — Spring Data JPA interfaces only. Use JPQL `@Query` for complex queries. Avoid native SQL.

**Mappers** — static methods or `@Component` classes. Entity → response DTO. No business logic.

## Exception Hierarchy

All custom exceptions extend `RuntimeException`. `GlobalExceptionHandler` maps them:

| Exception | HTTP | Common codes |
|-----------|------|-------------|
| `BusinessRuleException` | 400 | LAST_OWNER, ASSIGNEE_NOT_MEMBER, TOO_MANY_LABELS, CONFIRMATION_MISMATCH |
| `ResourceNotFoundException` | 404 | TASK_NOT_FOUND, PROJECT_NOT_FOUND, USER_NOT_FOUND |
| `DuplicateResourceException` | 409 | DUPLICATE_USERNAME, DUPLICATE_EMAIL, DUPLICATE_LABEL_NAME |
| `AccessDeniedException` | 403 | NOT_PROJECT_MEMBER, NOT_PROJECT_OWNER, NOT_TASK_OWNER |
| `AccountLockedException` | 423 | ACCOUNT_LOCKED (includes `lockedUntil`) |

Never throw raw Spring exceptions from service code — always wrap in domain exceptions.

## JWT & Security Pattern

- HS256, secret from `application.yml`. Access token 15 min, refresh token 7 days.
- Refresh token stored as SHA-256 hash. Never store the raw value.
- `JwtAuthenticationFilter` extracts `sub` (userId) from JWT → sets `UserPrincipal` on `SecurityContext`
- Controllers access user via `@AuthenticationPrincipal UserPrincipal principal`, pass `principal.getId()` to services

## Validation Pattern

```java
public record CreateTaskRequest(
    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must be at most 200 characters")
    String title,

    @ValidEnum(enumClass = TaskPriority.class)
    String priority
) {}
```

`GlobalExceptionHandler` catches `MethodArgumentNotValidException` → returns `ErrorResponse` with `details.fieldErrors`.

## Git Conventions

Branch naming: `feature/auth-login`, `fix/token-refresh-loop`, `chore/update-deps`

Commits (conventional format):
```
feat(tasks): add drag-and-drop status update
fix(auth): prevent token refresh race condition
test(comments): add delete comment integration test
```
