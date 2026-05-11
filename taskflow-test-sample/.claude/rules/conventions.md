# Test Conventions — TaskFlow

## Test Types & Tools

| Type | Tool | Scope |
|------|------|-------|
| Backend unit | JUnit 5 + Mockito | Service methods with mocked repositories |
| Backend controller | JUnit 5 + `@WebMvcTest` | HTTP status, request validation, response shape |
| Backend repository | JUnit 5 + `@DataJpaTest` + Testcontainers | Custom JPQL queries against real PostgreSQL |
| Backend integration | JUnit 5 + `@SpringBootTest` + Testcontainers | Full request → DB → response flow |
| Frontend component | Vitest + React Testing Library | Render, user interaction, DOM assertions |
| Frontend hooks | Vitest + `renderHook` | Custom hook behavior in isolation |
| E2E | Playwright | Full browser tests against running stack |

## Backend Test File Location

Mirror source structure under `src/test/java/com/example/demo/`:
```
service/TaskServiceTest.java          ← unit test
controller/TaskControllerTest.java    ← @WebMvcTest
repository/TaskRepositoryTest.java    ← @DataJpaTest
integration/TaskIntegrationTest.java  ← @SpringBootTest
```

## Backend Test Types in Detail

**Service unit tests (`service/`):**
- Mock all repositories with `@Mock` / `Mockito.mock()`
- Test happy path + all error branches (permission denied, not found, business rule violations)
- Do not test persistence — that belongs in repository tests

**Controller tests (`controller/`):**
- `@WebMvcTest` with `@MockBean` service
- Test: correct HTTP status code, request validation rejection (400 on bad input), response body shape
- Do not test business logic

**Repository tests (`repository/`):**
- `@DataJpaTest` + Testcontainers (real PostgreSQL — not H2)
- Test custom `@Query` methods and non-trivial derived queries
- Flyway migrations + seed data run automatically

**Integration tests (`integration/`):**
- `@SpringBootTest` + Testcontainers
- Full request → service → DB → response
- Use seed data (alex_lead/Test1234!, sam_dev/Test1234!, jordan_free/Test1234!)

**Never mock the database in repository or integration tests.**

## Backend Test Commands

```bash
./gradlew test                                                    # All tests
./gradlew test --tests "com.example.demo.service.TaskServiceTest" # Single class
./gradlew test --tests "com.example.demo.service.*"              # Package
./gradlew test --info                                             # Verbose output
```

Requires Docker running (Testcontainers spins up PostgreSQL automatically).

## Frontend Test File Location

Co-locate component tests next to the component:
```
src/features/board/
├── KanbanBoard.tsx
├── KanbanBoard.test.tsx   ← Vitest + RTL
├── TaskCard.tsx
└── TaskCard.test.tsx
```

E2E tests in top-level `tests/`:
```
tests/
├── auth.spec.ts
├── projects.spec.ts
├── tasks.spec.ts
└── comments.spec.ts
```

## Frontend Test Commands

```bash
npm run test                              # Vitest (watch mode off)
npm run test:watch                        # Watch mode
npm run test:coverage                     # Coverage report
npx playwright test                       # E2E (full stack must be running)
npx playwright test --headed              # Headed mode (see browser)
npx playwright test tests/auth.spec.ts   # Specific file
```

## General Rules

- **Never mock the database** in repository or integration tests — use Testcontainers
- **Do not retry mutations** in tests — retrying risks duplicate writes
- **Queries retry up to 3 times** with backoff; configure `retry: false` for 403/404 tests
- Every new service method needs a unit test
- Every new API endpoint needs a controller test
- Every new E2E test scenario should be added to `.claude/context/test-scope.md`
