# Superpower Prompt Plan — TaskFlow (vide-coding-test-1)

This is the **project-specific** version of the Superpowers + Vibe Template prompt playbook,
adapted for the TaskFlow stack:

- **Frontend (Teammate A):** React 19 + TypeScript + Vite + Tailwind CSS + shadcn/ui — workspace `taskflow-fe-sample/`
- **Backend (Teammate B):** Java 21 + Spring Boot 4 + PostgreSQL 17.5 + Flyway — workspace `taskflow-be-sample/`
- **Tester (Teammate C):** JUnit 5 + Testcontainers + Vitest + Playwright — workspace `taskflow-test-sample/`
- **Agents:** use the `.example.md` files directly — no real agent files are created

> **Constraint:** Do NOT create anything new inside `.claude/`. Plans go in `docs/superpowers/plans/`.
> **Constraint:** All subproject work targets `-sample` folders (`taskflow-fe-sample/`, `taskflow-be-sample/`, `taskflow-test-sample/`).

---

## 0 — Rejoin an existing session

```
Run the init-team pipeline.

Spawn each teammate using the .example.md files:
- Teammate A from .claude/agents/vide-coding-test-1-agent_frontend.example.md — works in taskflow-fe-sample/
- Teammate B from .claude/agents/vide-coding-test-1-agent_backend.example.md — works in taskflow-be-sample/
- Teammate C from .claude/agents/vide-coding-test-1-agent_tester.example.md — works in taskflow-test-sample/

Team is already set up — re-spawn and confirm roles before continuing.
Note: ignore the default workspace paths in the agent files; direct each teammate to their -sample folder.
```

---

## 1 — Start a new feature

> Triggers: `superpowers:brainstorming`

```
I want to build [feature name].

[One paragraph describing what it does, who uses it, and any constraints you already know.]

Use superpowers:brainstorming before anything else. Ask me questions one at a time.
Do not touch any code until I approve the design.
```

**TaskFlow feature examples — copy and adapt one:**

```
I want to add task comments with edit and delete.
Members can post comments on any task in their project.
Only the comment author can edit or delete their own comment.
Comment content renders as Markdown.

Use superpowers:brainstorming before anything else. Ask me questions one at a time.
Do not touch any code until I approve the design.
```

```
I want to add label filtering on the Kanban board.
Users can select one or more labels from a dropdown and the board shows only matching tasks.
Filter state must persist in the URL so it can be shared.

Use superpowers:brainstorming before anything else. Ask me questions one at a time.
Do not touch any code until I approve the design.
```

```
I want to add task assignment notifications.
When a task is assigned to a user, they see an in-app notification badge.
Clicking the badge opens a slide-over with the task details.

Use superpowers:brainstorming before anything else. Ask me questions one at a time.
Do not touch any code until I approve the design.
```

---

## 2 — Approve spec and create plans

> Triggers: `superpowers:writing-plans`

```
The spec looks good. Approve it.

Use superpowers:writing-plans to create three separate plans:
- One for Teammate A (frontend work only — taskflow-fe-sample/)
- One for Teammate B (backend work only — taskflow-be-sample/)
- One for Teammate C (tests and verification only — taskflow-test-sample/)

Save each plan to docs/superpowers/plans/YYYY-MM-DD-[feature]-[role].md.
Tasks must be 2–5 minutes each with exact file paths and verification steps.

Teammate A uses the create-component skill from taskflow-fe-sample/.claude/skills/create-component/.
Teammate B uses the add-endpoint skill from taskflow-be-sample/.claude/skills/add-endpoint/.
Teammate C writes tests targeting both taskflow-be-sample/ and taskflow-fe-sample/.
```

---

## 3 — Execute (delegate to teammates)

> Triggers: `superpowers:subagent-driven-development`

```
Plans are approved. Use superpowers:subagent-driven-development to delegate each plan
to the correct teammate using the .example.md agent files:

- docs/superpowers/plans/[date]-[feature]-frontend.md
  → Spawn from .claude/agents/vide-coding-test-1-agent_frontend.example.md
  → Direct to work in taskflow-fe-sample/ (override default workspace path)

- docs/superpowers/plans/[date]-[feature]-backend.md
  → Spawn from .claude/agents/vide-coding-test-1-agent_backend.example.md
  → Direct to work in taskflow-be-sample/ (override default workspace path)

- docs/superpowers/plans/[date]-[feature]-test.md
  → Spawn from .claude/agents/vide-coding-test-1-agent_tester.example.md
  → Direct to work in taskflow-test-sample/ (override default workspace path)

Each teammate must use superpowers:test-driven-development during their tasks.
Run all three in parallel. Do not pause between tasks unless blocked.
```

---

## 4 — Unblock a stuck teammate

> Triggers: `superpowers:systematic-debugging`

```
Teammate [A/B/C] is stuck on [describe the problem briefly].

Use superpowers:systematic-debugging to find the root cause.
Do not change any code until the root cause is confirmed.
```

**Common TaskFlow debug scenarios:**

```
Teammate B is stuck on a 403 when calling POST /api/v1/projects/{projectId}/tasks.
The user is a project member but the endpoint returns NOT_PROJECT_MEMBER.

Use superpowers:systematic-debugging to find the root cause.
Do not change any code until the root cause is confirmed.
```

```
Teammate A is stuck on a TanStack Query cache not updating after a task status drag-and-drop.
The optimistic update applies but onSettled invalidation is not triggering a refetch.

Use superpowers:systematic-debugging to find the root cause.
Do not change any code until the root cause is confirmed.
```

```
Teammate C's Testcontainers test hangs and never connects to PostgreSQL.
Docker is running. The test uses @SpringBootTest with Testcontainers.

Use superpowers:systematic-debugging to find the root cause.
Do not change any code until the root cause is confirmed.
```

---

## 5 — Request code review

> Triggers: `superpowers:requesting-code-review`

```
All tasks are done. Use superpowers:requesting-code-review against the
original spec at docs/superpowers/plans/[spec-file].md.

Block on any critical issues. Do not proceed to merge until all critical issues are resolved.
```

---

## 6 — Handle review feedback

> Triggers: `superpowers:receiving-code-review`

```
Review feedback has come in. Use superpowers:receiving-code-review
before making any changes. Do not implement anything until we have agreed on the approach.
```

---

## 7 — Verify before closing

> Triggers: `superpowers:verification-before-completion`

```
Use superpowers:verification-before-completion. Confirm everything is actually working —
do not take the implementer's word for it. Run the tests yourself and check the output.
```

**TaskFlow verification commands:**

```
Backend (taskflow-be-sample/):
  ./gradlew test
  Requires Docker running for Testcontainers.

Frontend (taskflow-fe-sample/):
  npm run test          (Vitest unit + component)
  npx playwright test   (E2E — requires full stack running)

Full stack for E2E:
  Backend:  ./gradlew bootRun --args='--spring.profiles.active=dev'  → http://localhost:8080
  Frontend: npm run dev                                               → http://localhost:5173

Seed accounts (password Test1234!):
  alex@example.com  (alex_lead  — project OWNER)
  sam@example.com   (sam_dev    — project MEMBER)
  jordan@example.com (jordan_free — project MEMBER)
```

---

## 8 — Finish the branch

> Triggers: `superpowers:finishing-a-development-branch`

```
All checks passed. Use superpowers:finishing-a-development-branch
and present the options: merge to main, open a PR, or discard.
```

---

## Bonus — Add new requirements mid-project

```
New requirements have been added to .claude/requirements/.
Run the apply-requirements pipeline (v3) to distribute them to
taskflow-fe-sample/, taskflow-be-sample/, and taskflow-test-sample/.

Do not re-run init-team or init-worktrees.
Do not modify anything in .claude/requirements/ — it is read-only.
```

---

## Bonus — Custom skill for this project

```
Use superpowers:writing-skills to create a new skill called [skill-name]
for [describe what it should enforce].

Save it to [subproject-sample]/.claude/skills/[skill-name]/SKILL.md.
Do NOT create new folders inside .claude/ at the project root.
Test it with a subagent before reporting done.
```

**Example for this project:**

```
Use superpowers:writing-skills to create a new skill called implement-feature
that covers the end-to-end flow: backend endpoint → frontend API call → component → test.

Save it to taskflow-fe-sample/.claude/skills/implement-feature/SKILL.md.
Do NOT create new folders inside .claude/ at the project root.
Test it with a subagent before reporting done.
```

---

## Project Quick Reference

### Agent files (use .example.md — do not create real copies)

| Teammate | Agent file | Workspace (override in task) |
|---|---|---|
| A — Frontend | `.claude/agents/vide-coding-test-1-agent_frontend.example.md` | `taskflow-fe-sample/` |
| B — Backend | `.claude/agents/vide-coding-test-1-agent_backend.example.md` | `taskflow-be-sample/` |
| C — Tester | `.claude/agents/vide-coding-test-1-agent_tester.example.md` | `taskflow-test-sample/` |

### Existing skills

| Subproject | Skill | Path |
|---|---|---|
| `taskflow-fe-sample` | `create-component` | `.claude/skills/create-component/SKILL.md` |
| `taskflow-be-sample` | `add-endpoint` | `.claude/skills/add-endpoint/SKILL.md` |

### Key tech facts

| Area | Detail |
|---|---|
| Backend base package | `haonguyen.taskflow_be` |
| Backend layer order | `controller/ → service/ → repository/` (no logic in controllers) |
| API base URL | `/api/v1` |
| Error shape | `{ "error": "SCREAMING_SNAKE", "message": "...", "details": {...} }` |
| Auth | Access token in Zustand (memory) + httpOnly refresh cookie |
| Frontend API layer | `src/api/` → TanStack Query hooks → components (never call API directly from components) |
| Migrations | Flyway, files in `taskflow-be-sample/src/main/resources/db/migration/` |
| DB enums | Stored as `VARCHAR` + CHECK constraints (not PG native enums) |

### Cascade delete order (for tasks touching DELETE endpoints)

```
Project delete:  comments → task_labels → tasks → labels → project_members → project
Task delete:     comments → task_labels → task
Label delete:    task_labels → label
Remove member:   set assignee_id=NULL on that user's tasks in the project (app code, same tx)
```

---

## Rules to remember

- Always mention the full `superpowers:skill-name` — never abbreviated
- Never skip brainstorming, even for small features
- "Fix this bug" → use `superpowers:systematic-debugging`, not `superpowers:brainstorming`
- Skills auto-trigger but explicit names are more reliable
- Always override teammate working directory to the `-sample` folder in the task prompt — the `.example.md` files default to non-sample paths
- Never create files inside `.claude/` at the project root — subproject `.claude/` folders are fine
- Your CLAUDE.md rules always override Superpowers skills
