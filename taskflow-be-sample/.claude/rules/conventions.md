# Backend Conventions

## Naming
| Element | Convention | Example |
|---------|-----------|---------|
| Classes | PascalCase | `TaskController`, `CreateTaskRequest` |
| Methods | camelCase | `findByProjectId`, `createTask` |
| Constants | SCREAMING_SNAKE | `MAX_LABELS_PER_TASK = 5` |
| Database columns | snake_case | `created_at`, `project_id` |
| Enum values | SCREAMING_SNAKE | `IN_PROGRESS`, `HIGH` |
| REST paths | kebab-case, plural nouns | `/api/v1/projects/{projectId}/tasks` |
| Path variables | camelCase | `{projectId}`, `{taskId}` |
| DTO classes | Purpose + type suffix | `CreateTaskRequest`, `TaskResponse` |
| Service methods | verb + noun | `createTask`, `findTaskById`, `updateTask`, `deleteTask` |
| Repository methods | Spring Data naming | `findByProjectIdAndStatus`, `existsByProjectIdAndUserId` |
| Config properties | kebab-case | `jwt.access-token-expiration` |

## Layer Responsibilities
- **Controller:** extract params → call service → map to DTO → return status. No logic.
- **Service:** all business rules, permission checks, repository calls. Returns entities.
- **Repository:** Spring Data JPA interfaces only. Use `@Query` for non-trivial queries. Prefer JPQL over native SQL.
- **Mapper:** static methods or `@Component` classes. One mapper per aggregate root. Converts entity → DTO.

## Permission Check Pattern in Services
```java
// 1. Load entity (throw ResourceNotFoundException if absent)
Task task = taskRepository.findById(taskId)
    .orElseThrow(() -> new ResourceNotFoundException("TASK_NOT_FOUND", "Task not found"));

// 2. Check project membership
if (!projectMemberRepository.existsByProjectIdAndUserId(task.getProject().getId(), userId)) {
    throw new AccessDeniedException("NOT_PROJECT_MEMBER", "You are not a member of this project");
}

// 3. Check action-specific permission (e.g., delete requires task creator or project owner)
// 4. Execute operation
```
`authenticatedUserId` comes from `@AuthenticationPrincipal UserPrincipal principal` in the controller, passed to service as a parameter.

## JWT
- Access token: HS256, 15-min lifetime. Claims: `sub` (user ID as string), `username`, `iat`, `exp`.
- Refresh token: 128-char cryptographically random string (`SecureRandom`). Store SHA-256 hash in DB.
- `JwtAuthenticationFilter`: runs on every request, extracts `sub`, sets `UserPrincipal` on `SecurityContext`. Does NOT throw on invalid token — lets Spring Security return 401.

## Password
BCrypt via `BCryptPasswordEncoder`, cost factor 10.
Requirements: 8–72 chars, ≥1 uppercase, ≥1 lowercase, ≥1 digit, ≥1 special char.
Regex: `^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*()_+\-=\[\]{}|;:',.<>?/~` + "`" + `]).{8,72}$`

## Account Locking
Check `login_attempts` table: count rows where `email = ?` AND `success = false` AND `attempted_at > NOW() - INTERVAL '15 minutes'`.
If count >= 5, return 423 with `lockedUntil` timestamp.
Insert a `success = true` row on successful login (resets the counter naturally).

## Testing Structure
- Service tests: Mockito, test business logic in isolation
- Controller tests: `@WebMvcTest` with mocked services, test HTTP behavior (status codes, request validation, response shapes)
- Repository tests: `@DataJpaTest` with Testcontainers, test custom queries
- Integration tests: `@SpringBootTest` with Testcontainers, test full request → response flow

Test files mirror source structure under `src/test/java/haonguyen/taskflow_be/`.

## Git
Branch naming: `feature/auth-login`, `feature/kanban-board`, `fix/token-refresh-loop`, `chore/update-deps`.
Commit messages: conventional commits — `feat(tasks): add drag-and-drop status update`, `fix(auth): prevent token refresh race condition`.
