# TaskFlow — Tech Stack & Conventions

## Backend Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Language | Java | 24.0.2 |
| Framework | Spring Boot | 4.0.5 |
| Build tool | Gradle (Kotlin DSL) | 8.14+ |
| Packaging | JAR | — |
| Base package | `com.example.demo` | — |
| ORM | Spring Data JPA (Hibernate 7) | — |
| Database | PostgreSQL | 17.5 |
| Migrations | Flyway | 11 |
| Auth | Spring Security + JJWT | — |
| Password hashing | BCrypt (Spring Security) | — |
| Validation | Jakarta Validation (Hibernate Validator) | — |
| API docs | SpringDoc OpenAPI | — |
| Testing | JUnit 5 + Mockito + Testcontainers | — |
| Container | Docker | 28.5.1 (for PostgreSQL) |

---

## Frontend Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Language | TypeScript | 5.x (strict mode) |
| Framework | React | 19.x |
| Build tool | Vite | 6.x |
| Styling | Tailwind CSS | 4.x |
| Component library | shadcn/ui (Radix primitives) | — |
| Routing | React Router | 7.x |
| Server state | TanStack Query (React Query) | 5.x |
| Client state | Zustand | 5.x |
| HTTP client | Axios | — |
| Forms | React Hook Form + Zod | — |
| Drag-and-drop | @dnd-kit/core + @dnd-kit/sortable | — |
| Markdown | react-markdown | — |
| Date formatting | date-fns | — |
| Icons | Lucide React | — |
| Testing | Vitest + React Testing Library + Playwright | — |

---

## Backend Project Structure

```
src/
├── main/
│   ├── java/com/example/demo/
│   │   ├── DemoApplication.java              ← Spring Boot entry point
│   │   │
│   │   ├── config/
│   │   │   ├── SecurityConfig.java           ← Spring Security filter chain, CORS, CSRF
│   │   │   ├── JwtConfig.java                ← JWT secret, expiration values from properties
│   │   │   └── WebConfig.java                ← Global CORS mapping
│   │   │
│   │   ├── controller/
│   │   │   ├── AuthController.java           ← /api/v1/auth/*
│   │   │   ├── UserController.java           ← /api/v1/users/*
│   │   │   ├── ProjectController.java        ← /api/v1/projects/*
│   │   │   ├── MemberController.java         ← /api/v1/projects/{id}/members/*
│   │   │   ├── TaskController.java           ← /api/v1/projects/{id}/tasks/* and /api/v1/tasks/*
│   │   │   ├── LabelController.java          ← /api/v1/projects/{id}/labels/* and /api/v1/labels/*
│   │   │   ├── TaskLabelController.java      ← /api/v1/tasks/{id}/labels/*
│   │   │   └── CommentController.java        ← /api/v1/tasks/{id}/comments/* and /api/v1/comments/*
│   │   │
│   │   ├── dto/
│   │   │   ├── request/
│   │   │   │   ├── RegisterRequest.java
│   │   │   │   ├── LoginRequest.java
│   │   │   │   ├── UpdateUserRequest.java
│   │   │   │   ├── CreateProjectRequest.java
│   │   │   │   ├── UpdateProjectRequest.java
│   │   │   │   ├── DeleteProjectRequest.java    ← contains confirmName field
│   │   │   │   ├── InviteMemberRequest.java
│   │   │   │   ├── UpdateMemberRoleRequest.java
│   │   │   │   ├── CreateTaskRequest.java
│   │   │   │   ├── UpdateTaskRequest.java
│   │   │   │   ├── CreateLabelRequest.java
│   │   │   │   ├── UpdateLabelRequest.java
│   │   │   │   ├── AttachLabelRequest.java       ← contains labelId field
│   │   │   │   └── CreateCommentRequest.java
│   │   │   │
│   │   │   └── response/
│   │   │       ├── AuthResponse.java             ← accessToken + expiresIn
│   │   │       ├── UserResponse.java
│   │   │       ├── UserSummaryResponse.java      ← id + username only
│   │   │       ├── ProjectListResponse.java      ← for GET /projects list items
│   │   │       ├── ProjectDetailResponse.java    ← for GET /projects/{id} with members and taskCounts
│   │   │       ├── MemberResponse.java
│   │   │       ├── TaskResponse.java
│   │   │       ├── LabelResponse.java            ← includes taskCount
│   │   │       ├── LabelSummaryResponse.java     ← id + name + color (embedded in TaskResponse)
│   │   │       ├── CommentResponse.java
│   │   │       ├── PageResponse.java             ← generic paginated wrapper
│   │   │       └── ErrorResponse.java            ← error + message + details
│   │   │
│   │   ├── entity/
│   │   │   ├── User.java
│   │   │   ├── Project.java
│   │   │   ├── ProjectMember.java
│   │   │   ├── ProjectMemberId.java              ← composite key class
│   │   │   ├── Task.java
│   │   │   ├── Label.java
│   │   │   ├── TaskLabel.java
│   │   │   ├── TaskLabelId.java                  ← composite key class
│   │   │   ├── Comment.java
│   │   │   ├── RefreshToken.java
│   │   │   └── LoginAttempt.java
│   │   │
│   │   ├── enums/
│   │   │   ├── TaskStatus.java
│   │   │   ├── TaskPriority.java
│   │   │   └── MemberRole.java
│   │   │
│   │   ├── exception/
│   │   │   ├── GlobalExceptionHandler.java       ← @ControllerAdvice
│   │   │   ├── ResourceNotFoundException.java
│   │   │   ├── DuplicateResourceException.java
│   │   │   ├── AccessDeniedException.java
│   │   │   ├── AccountLockedException.java
│   │   │   └── BusinessRuleException.java        ← for things like LAST_OWNER, ASSIGNEE_NOT_MEMBER
│   │   │
│   │   ├── mapper/
│   │   │   ├── UserMapper.java
│   │   │   ├── ProjectMapper.java
│   │   │   ├── TaskMapper.java
│   │   │   ├── LabelMapper.java
│   │   │   ├── CommentMapper.java
│   │   │   └── MemberMapper.java
│   │   │
│   │   ├── repository/
│   │   │   ├── UserRepository.java
│   │   │   ├── ProjectRepository.java
│   │   │   ├── ProjectMemberRepository.java
│   │   │   ├── TaskRepository.java
│   │   │   ├── LabelRepository.java
│   │   │   ├── TaskLabelRepository.java
│   │   │   ├── CommentRepository.java
│   │   │   ├── RefreshTokenRepository.java
│   │   │   └── LoginAttemptRepository.java
│   │   │
│   │   ├── security/
│   │   │   ├── JwtTokenProvider.java             ← generate, parse, validate tokens
│   │   │   ├── JwtAuthenticationFilter.java      ← OncePerRequestFilter
│   │   │   └── UserPrincipal.java                ← implements UserDetails
│   │   │
│   │   └── service/
│   │       ├── AuthService.java
│   │       ├── UserService.java
│   │       ├── ProjectService.java
│   │       ├── MemberService.java
│   │       ├── TaskService.java
│   │       ├── LabelService.java
│   │       ├── TaskLabelService.java
│   │       └── CommentService.java
│   │
│   └── resources/
│       ├── application.yml                       ← main config
│       ├── application-dev.yml                   ← dev profile overrides
│       ├── application-test.yml                  ← test profile (Testcontainers PG)
│       └── db/migration/
│           ├── V1__create_users.sql
│           ├── V2__create_projects.sql
│           ├── ... (see database design doc)
│           └── V11__seed_data.sql
│
└── test/
    └── java/com/example/demo/
        ├── controller/                           ← @WebMvcTest integration tests
        ├── service/                              ← unit tests with mocks
        ├── repository/                           ← @DataJpaTest with Testcontainers
        └── integration/                          ← full integration tests
```

---

## Frontend Project Structure

```
src/
├── main.tsx                            ← React entry point, renders App
├── App.tsx                             ← Router setup, global providers
│
├── api/
│   ├── client.ts                       ← Axios instance with interceptors (auth, refresh)
│   ├── auth.ts                         ← register, login, refresh, logout
│   ├── users.ts                        ← getMe, updateMe
│   ├── projects.ts                     ← CRUD projects
│   ├── members.ts                      ← CRUD members
│   ├── tasks.ts                        ← CRUD tasks
│   ├── labels.ts                       ← CRUD labels
│   ├── taskLabels.ts                   ← attach/detach labels
│   └── comments.ts                     ← CRUD comments
│
├── components/
│   ├── ui/                             ← shadcn/ui components (Button, Input, Select, etc.)
│   ├── Avatar.tsx
│   ├── Badge.tsx
│   ├── LabelPill.tsx
│   ├── EmptyState.tsx
│   ├── ConfirmDialog.tsx
│   ├── Skeleton.tsx
│   ├── Toast.tsx (via Sonner or custom)
│   └── Pagination.tsx
│
├── features/
│   ├── auth/
│   │   ├── LoginForm.tsx
│   │   ├── RegisterForm.tsx
│   │   ├── useAuth.ts                  ← login/logout/register mutations
│   │   └── PasswordStrength.tsx
│   │
│   ├── projects/
│   │   ├── ProjectCard.tsx
│   │   ├── ProjectGrid.tsx
│   │   ├── CreateProjectModal.tsx
│   │   └── useProjects.ts             ← queries and mutations
│   │
│   ├── board/
│   │   ├── KanbanBoard.tsx
│   │   ├── KanbanColumn.tsx
│   │   ├── TaskCard.tsx
│   │   ├── FilterBar.tsx
│   │   └── useTasks.ts
│   │
│   ├── tasks/
│   │   ├── TaskDetailPanel.tsx          ← the slide-over panel
│   │   ├── TaskFieldGrid.tsx
│   │   ├── TaskDescription.tsx
│   │   ├── TaskLabelPicker.tsx
│   │   └── useTaskMutations.ts
│   │
│   ├── comments/
│   │   ├── CommentList.tsx
│   │   ├── CommentItem.tsx
│   │   ├── CommentInput.tsx
│   │   └── useComments.ts
│   │
│   ├── members/
│   │   ├── MemberList.tsx
│   │   ├── InviteMemberForm.tsx
│   │   └── useMembers.ts
│   │
│   ├── labels/
│   │   ├── LabelManager.tsx
│   │   ├── LabelForm.tsx
│   │   ├── ColorPicker.tsx
│   │   └── useLabels.ts
│   │
│   └── settings/
│       ├── ProjectGeneralForm.tsx
│       └── ProjectSettingsPage.tsx
│
├── hooks/
│   ├── useDebounce.ts
│   ├── useClickOutside.ts
│   └── useKeyboard.ts                 ← Escape key handling, etc.
│
├── layouts/
│   ├── AppLayout.tsx                   ← Navbar + page content wrapper
│   ├── AuthLayout.tsx                  ← Centered card layout for login/register
│   └── Navbar.tsx
│
├── pages/
│   ├── LoginPage.tsx
│   ├── RegisterPage.tsx
│   ├── DashboardPage.tsx
│   ├── ProjectPage.tsx                 ← board + list views
│   ├── ProjectSettingsPage.tsx
│   ├── ProfilePage.tsx
│   ├── NotFoundPage.tsx
│   └── ForbiddenPage.tsx
│
├── stores/
│   └── authStore.ts                    ← Zustand store: accessToken, user, isAuthenticated
│
├── types/
│   ├── api.ts                          ← API response types (User, Project, Task, etc.)
│   ├── enums.ts                        ← TaskStatus, TaskPriority, MemberRole
│   └── pagination.ts                   ← PageResponse<T> generic
│
└── utils/
    ├── cn.ts                           ← clsx + tailwind-merge utility
    ├── formatDate.ts                   ← date formatting helpers (relative time, absolute)
    └── avatarColor.ts                  ← deterministic color from username hash
```

---

## Naming Conventions

### Backend (Java / Spring Boot)

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
| Packages | lowercase | `com.example.demo.controller` |
| Config properties | kebab-case | `jwt.access-token-expiration` |

### Frontend (TypeScript / React)

| Element | Convention | Example |
|---------|-----------|---------|
| Components | PascalCase | `TaskCard.tsx`, `KanbanBoard.tsx` |
| Hooks | camelCase with `use` prefix | `useTasks.ts`, `useDebounce.ts` |
| Utilities | camelCase | `formatDate.ts`, `cn.ts` |
| Types/Interfaces | PascalCase | `Task`, `CreateTaskRequest` |
| Enum-like constants | SCREAMING_SNAKE in a const object | `TaskStatus.IN_PROGRESS` |
| API functions | camelCase verb+noun | `createTask`, `fetchTasks`, `updateTask` |
| CSS classes | Tailwind utilities (no custom CSS files) | `className="flex items-center gap-2"` |
| Props interfaces | Component name + `Props` | `TaskCardProps`, `FilterBarProps` |
| Query keys | `[resource, ...params]` array | `['tasks', projectId, filters]` |
| Store slices | camelCase noun | `authStore` |
| Event handlers | `handle` + event | `handleSubmit`, `handleDragEnd` |
| Boolean props/state | `is`/`has`/`can` prefix | `isLoading`, `hasMore`, `canDelete` |

---

## Code Patterns

### Backend Patterns

**Controller layer** — thin controllers. Controllers only:
1. Extract path variables and query params
2. Call the service method
3. Map the result to a response DTO
4. Return the HTTP response with correct status code

No business logic in controllers. No direct repository access from controllers.

**Service layer** — all business logic lives here:
1. Validate business rules (e.g., assignee must be a project member)
2. Check permissions (e.g., only project owner can delete)
3. Perform the operation via repository
4. Return entity or throw a custom exception

Services receive request DTOs or primitive parameters. Services return entities. Mapping to response DTOs happens in the controller or mapper layer.

**Repository layer** — Spring Data JPA interfaces only. Use `@Query` for anything beyond simple derived queries. Never use native queries unless absolutely necessary (prefer JPQL).

**Mapper layer** — static methods or Spring `@Component` classes that convert between entities and DTOs. One mapper per aggregate root. Example:

```java
public class TaskMapper {
    public static TaskResponse toResponse(Task task) {
        // Map entity fields to response DTO
        // Include nested UserSummaryResponse for assignee, createdBy
        // Include List<LabelSummaryResponse> for labels
    }
}
```

**Exception handling** — all custom exceptions extend `RuntimeException`. The `GlobalExceptionHandler` (`@ControllerAdvice`) catches them and returns the standard `ErrorResponse` format. Never throw raw Spring exceptions from service code — wrap them in domain exceptions.

**Pagination** — use Spring Data `Pageable`. Controllers accept `page` and `size` parameters and construct a `PageRequest`. Services return `Page<Entity>`. Controllers map to `PageResponse<DTO>`.

### Frontend Patterns

**API layer** — each resource file (`api/tasks.ts`) exports plain async functions that call the Axios client and return typed responses. These functions are consumed by TanStack Query hooks.

**Query hooks** — each feature has a hook file (e.g., `useTasks.ts`) that wraps TanStack Query's `useQuery` and `useMutation`:

```typescript
export function useTasks(projectId: number, filters: TaskFilters) {
  return useQuery({
    queryKey: ['tasks', projectId, filters],
    queryFn: () => fetchTasks(projectId, filters),
  });
}

export function useUpdateTask() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ taskId, data }: { taskId: number; data: UpdateTaskRequest }) =>
      updateTask(taskId, data),
    onMutate: async ({ taskId, data }) => {
      // Optimistic update logic
    },
    onError: (err, variables, context) => {
      // Rollback optimistic update
    },
    onSettled: () => {
      // Invalidate queries to refetch
    },
  });
}
```

**Optimistic updates** — used for all drag-and-drop status changes and inline field edits in the task detail panel. The pattern is:
1. `onMutate`: cancel outgoing queries, snapshot previous data, apply optimistic update
2. `onError`: restore snapshot
3. `onSettled`: invalidate the query to refetch fresh data

**Auth flow** — the Axios instance has two interceptors:
1. **Request interceptor**: attaches `Authorization: Bearer <accessToken>` from the Zustand auth store
2. **Response interceptor**: on 401, attempt token refresh. If refresh succeeds, retry the original request. If refresh fails, clear auth state and redirect to `/login`. Uses a queue to prevent multiple concurrent refresh calls.

**Route guards** — a `ProtectedRoute` wrapper component checks `isAuthenticated` from the auth store. If false, redirects to `/login`. A `PublicOnlyRoute` wrapper does the opposite for login/register (redirects to `/` if already authenticated).

---

## Git Conventions

**Branch naming:**
- Feature: `feature/auth-login`, `feature/kanban-board`
- Bugfix: `fix/token-refresh-loop`
- Chore: `chore/update-dependencies`

**Commit messages:** conventional commits format:
```
feat(tasks): add drag-and-drop status update
fix(auth): prevent token refresh race condition
chore(deps): update Spring Boot to 4.0.5
test(comments): add delete comment integration test
```

**PR requirements:**
- Title follows conventional commit format
- Description includes what changed and why
- All tests pass
- No lint errors
- At least one reviewer approval (when working with humans)
