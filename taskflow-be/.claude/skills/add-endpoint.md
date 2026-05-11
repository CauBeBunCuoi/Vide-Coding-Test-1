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
7. Add endpoint to `.claude/context/api-list.md`
8. Check `.claude/rules/api.md` for naming and response format conventions

> **Template note:** `database.md`, `api-list.md`, and `api.md` above are example names. Your project's files may be named differently — check what exists in `.claude/rules/` and `.claude/context/` and reference the correct files.
