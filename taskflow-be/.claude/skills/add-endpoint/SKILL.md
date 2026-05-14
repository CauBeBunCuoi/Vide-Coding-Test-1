---
name: add-endpoint
description: How to add a new REST endpoint in this project
---

# How to Add a New Endpoint

1. Define the route in `src/routes/<resource>.routes.ts`
2. Create or update the controller in `src/controllers/<resource>.controller.ts`
3. Add business logic in `src/services/<resource>.service.ts`
4. Add DB queries in `src/repositories/<resource>.repository.ts`
5. Define request/response types in `src/types/<resource>.types.ts`
6. Write a migration if schema changes — see `.claude/rules/database.md`
7. Add the new endpoint to `references/api-list.md`
8. Check `.claude/rules/api.md` for naming and response format conventions

## References
- `references/api-list.md` — current list of all endpoints
- `references/background-jobs.md` — background jobs that may be affected by schema changes
- `.claude/rules/api.md` — API naming and response format conventions
- `.claude/rules/database.md` — migration standards

> **Template note:** Rule file names above are examples. Check what exists in `.claude/rules/` and reference the correct files.
