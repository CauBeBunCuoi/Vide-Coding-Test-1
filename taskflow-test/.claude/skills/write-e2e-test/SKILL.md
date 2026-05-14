---
name: write-e2e-test
description: How to write an end-to-end test in this project
---

# How to Write an E2E Test

1. Create the test file in `tests/e2e/<feature>.e2e.ts`
2. Use the shared `testClient` from `tests/helpers/client.ts` — do not create a new HTTP client
3. Seed required data using helpers in `tests/helpers/seed.ts` before the test
4. Clean up seeded data in `afterEach` using `teardown()` helper
5. Assert both the response status and the response body shape
6. Check `.claude/rules/conventions.md` for naming and file structure conventions
7. Add the scenario to `references/test-scope.md` if it covers a new flow

## References
- `references/test-scope.md` — what is in/out of scope and current coverage targets
- `.claude/rules/conventions.md` — test naming and file structure conventions

> **Template note:** Rule file names above are examples. Check what exists in `.claude/rules/` and reference the correct files.
