---
name: write-e2e-test
description: How to write an end-to-end test for TaskFlow using Playwright
---

# How to Write an E2E Test (Playwright)

E2E tests run against the full stack — both the Spring Boot backend and the Vite frontend must be running.

## Setup

Start the full stack before running E2E tests:
```bash
# 1. PostgreSQL (if not running)
docker compose up -d

# 2. Backend (wait for "Started DemoApplication")
cd taskflow-be-sample
./gradlew bootRun --args='--spring.profiles.active=dev'

# 3. Frontend
cd taskflow-fe-sample
npm run dev
```

---

## Running Tests

```bash
# Run all E2E tests
npx playwright test

# Run in headed mode (see the browser)
npx playwright test --headed

# Run a specific file
npx playwright test tests/auth.spec.ts

# Run a specific test by title
npx playwright test --grep "should login successfully"
```

---

## File Location

E2E test files live in a top-level `tests/` directory:
```
tests/
├── auth.spec.ts
├── projects.spec.ts
├── tasks.spec.ts
└── comments.spec.ts
```

Test files mirror the feature they cover.

---

## Writing a Test

```typescript
import { test, expect } from '@playwright/test';

test.describe('Login', () => {
  test('should login successfully with valid credentials', async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel('Email').fill('alex@example.com');
    await page.getByLabel('Password').fill('Test1234!');
    await page.getByRole('button', { name: 'Log in' }).click();
    await expect(page).toHaveURL('/');
    await expect(page.getByText('Projects')).toBeVisible();
  });

  test('should show error on invalid credentials', async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel('Email').fill('alex@example.com');
    await page.getByLabel('Password').fill('WrongPassword!');
    await page.getByRole('button', { name: 'Log in' }).click();
    await expect(page.getByText('Invalid email or password')).toBeVisible();
  });
});
```

---

## Guidelines

1. **Use seed data** for test setup — the DB is pre-seeded with alex/sam/jordan (pwd: `Test1234!`) + sample projects and tasks. Do not create accounts programmatically in most tests.

2. **Cover the happy path + at least one error path** per test file.

3. **Target user-visible text and roles** (`getByRole`, `getByLabel`, `getByText`) — not CSS selectors or test IDs unless necessary.

4. **Verify both navigation and content** after a key action (e.g., check URL changed AND page content reflects the change).

5. **After writing a new E2E test**, add the scenario to `.claude/context/test-scope.md` if it covers a flow not already listed.

> Check `.claude/rules/conventions.md` for backend/frontend unit test structure and commands.
