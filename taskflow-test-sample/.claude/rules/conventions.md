# Test Conventions

## Naming

| Test type | File pattern | Example |
|-----------|-------------|---------|
| JUnit service unit | `*ServiceTest.java` | `TaskServiceTest.java` |
| JUnit controller | `*ControllerTest.java` | `TaskControllerTest.java` |
| JUnit repository | `*RepositoryTest.java` | `TaskRepositoryTest.java` |
| JUnit integration | `*IntegrationTest.java` | `CreateTaskIntegrationTest.java` |
| Vitest component | `*.test.tsx` co-located with source | `TaskCard.test.tsx` |
| Playwright E2E | `*.spec.ts` in `tests/` | `auth.spec.ts`, `kanban.spec.ts` |

Method names: `should_<result>_when_<condition>` (JUnit), `renders <what> when <condition>` (Vitest), `user can <action>` (Playwright).

## Backend Test Structure

Mirror source structure under `src/test/java/haonguyen/taskflow_be/`:
```
src/test/java/haonguyen/taskflow_be/
├── service/        ← Mockito unit tests (no Spring context)
├── controller/     ← @WebMvcTest (Spring MVC slice, mock service)
├── repository/     ← @DataJpaTest + Testcontainers
└── integration/    ← @SpringBootTest + Testcontainers (full stack)
```

## Database in Tests

**Never mock the database** in repository or integration tests. Always use Testcontainers with a real PostgreSQL image. `@DataJpaTest` and `@SpringBootTest` tests must use `@Testcontainers` + `@Container` with `PostgreSQLContainer`.

Mocking is only allowed in service-layer tests (`@ExtendWith(MockitoExtension.class)`).

## Coverage Targets

| Layer | Required coverage |
|---|---|
| Service | Every happy path + every error/exception branch |
| Controller | Every endpoint: correct status code + response shape |
| Repository | All custom `@Query` methods |
| Integration | All P0 features (auth, projects, tasks, labels, comments) |
| E2E (Playwright) | Auth flow, create project, create task, drag-and-drop status change |

## Frontend Test Patterns

Vitest + React Testing Library: co-locate test files with source (`src/features/<area>/<Component>.test.tsx`).
- Test rendered output and user interactions, not implementation details
- Use `userEvent` over `fireEvent`
- Mock API functions in `src/api/` with `vi.mock()`
- Do not mock TanStack Query internals — wrap components in `QueryClientProvider` with a real `QueryClient`

## Playwright E2E

Tests live in `tests/` at the frontend root (`taskflow-fe-sample/tests/`).
Seed accounts for all E2E tests (password `Test1234!`): alex@example.com, sam@example.com, jordan@example.com.
Both backend (`localhost:8080`) and frontend (`localhost:5173`) must be running before E2E tests start.

## Test Run Commands

```
# Backend (from taskflow-be-sample/)
./gradlew test                  # requires Docker

# Frontend unit/component (from taskflow-fe-sample/)
npm run test

# Frontend E2E (from taskflow-fe-sample/, full stack must be running)
npx playwright test
```
