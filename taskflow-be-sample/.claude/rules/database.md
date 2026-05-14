# Database Rules

## Enums
Store as `VARCHAR` with CHECK constraints (not PG native enums) — easier to add values later.
Map in JPA with `@Enumerated(EnumType.STRING)`.
- `task_status`: `TODO` | `IN_PROGRESS` | `IN_REVIEW` | `DONE`
- `task_priority`: `LOW` | `MEDIUM` | `HIGH` | `URGENT`
- `member_role`: `OWNER` | `MEMBER`

## Case-Insensitive Uniqueness
- `username`: unique index on `LOWER(username)`. Compare lowercased in lookups.
- `email`: unique index on `LOWER(email)`. Always store in lowercase.
- `label.name`: unique constraint on `(project_id, LOWER(name))`.

## Flyway Migrations
- File naming: `V{n}__{description}.sql` (e.g. `V12__add_column_to_tasks.sql`)
- Never modify an already-applied migration — create a new version instead.
- Use `IF NOT EXISTS` where possible for idempotency.
- Migrations run automatically on startup via Spring Boot + Flyway auto-configuration.

## Ownership Invariant
`projects.owner_id` is denormalized from `project_members` (role=OWNER). Both must stay in sync.
When ownership transfers: update `owner_id` AND the `project_members` role in the same transaction.

## Application-Code Enforcement (Not DB Constraints)
These are enforced in the service layer, not via DB constraints:
- Assignee on a task must be a member of the task's project.
- A task has at most 5 labels (check `COUNT(task_labels WHERE task_id=?)` before insert).
- A project must always have at least one OWNER (check before role change or removal).
- Member removal → set `assignee_id = NULL` on that user's tasks scoped to the removed project only.

## Cascade Delete Order
**Delete project:** comments → task_labels → tasks → labels → project_members → project
**Delete task:** comments → task_labels → task
**Delete label:** task_labels → label
Use `ON DELETE CASCADE` on FK constraints where safe. Use application code in a transaction for conditional logic (e.g., member removal → unassign tasks scoped to that project).

## Refresh Tokens
Store SHA-256 hash of the raw token, never the raw value.
On rotation: delete old row, insert new hash row in the same operation.
Scheduled job cleans up expired rows (also `login_attempts` older than 24h).

## updated_at
Refresh on every mutation. Use `@PreUpdate` via an `Auditable` base entity or set explicitly in service methods: `entity.setUpdatedAt(Instant.now())`.

## Key Indexes
Beyond PKs and unique constraints, these explicit indexes are required:
- `tasks(project_id, status)` — board view
- `tasks(project_id, assignee_id)` — filter by assignee
- `tasks(project_id, priority)` — sort/filter by priority
- `tasks(project_id, deadline)` — sort by deadline
- `tasks(created_by)` — delete permission check
- `comments(task_id, created_at)` — list comments chronologically
- `refresh_tokens(user_id)`, `refresh_tokens(expires_at)` — lookup + cleanup
- `login_attempts(email, attempted_at)` — account lock check
- `project_members(user_id)` — "list my projects" query
