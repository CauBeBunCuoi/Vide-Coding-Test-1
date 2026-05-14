---
name: manage-background-jobs
description: How to add or modify a background job in this project
---

# How to Manage a Background Job

1. Create or update the job class in `src/jobs/<job-name>.job.ts`
2. Register the job in the scheduler configuration
3. Define what the job reads and writes — document it in `references/background-jobs.md`
4. Write a unit test with mocked dependencies for the job logic
5. Check `.claude/rules/conventions.md` for naming conventions

## References
- `references/background-jobs.md` — all existing background jobs, their triggers, and DB access patterns

> **Template note:** Rule file names above are examples. Check what exists in `.claude/rules/` and reference the correct files.
