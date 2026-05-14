# Teammate C — Tester Workspace

# Rules
@import .claude/rules/conventions.md

## Project Context

**Backend tests:** JUnit 5 + Mockito (unit) + `@WebMvcTest` (controller) + `@DataJpaTest` + Testcontainers (repository) + `@SpringBootTest` + Testcontainers (integration). Requires Docker. Run: `./gradlew test` from `taskflow-be-sample/`.

**Frontend tests:** Vitest + React Testing Library (unit/component, co-located with source files) + Playwright (E2E in top-level `tests/`). Run: `npm run test` and `npx playwright test`.

**E2E requirement:** Both backend and frontend must be running. Seed data available: alex@example.com, sam@example.com, jordan@example.com — all password `Test1234!`.

**Coverage targets:** Service layer: every happy path + every error branch. Controller layer: every endpoint (status code + response shape). Integration: all P0 features. E2E: auth flow, create project, create task, drag-and-drop status change.

**Never mock the database** in repository or integration tests — always Testcontainers.
