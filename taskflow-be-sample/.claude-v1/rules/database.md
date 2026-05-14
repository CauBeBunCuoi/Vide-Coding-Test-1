# Database Rules — TaskFlow Backend

## Engine & Migration Tool

- PostgreSQL 17.5 (Docker container)
- Flyway 11 for migrations — files in `src/main/resources/db/migration/`
- Naming: `V{number}__{description}.sql` (e.g., `V1__create_users.sql`)
- **Never modify an already-applied migration.** Create a new version instead.
- Hibernate `ddl-auto: validate` — Flyway owns schema, Hibernate only validates it.

## Enum Storage

Store enums as `VARCHAR` with `CHECK` constraints — **not** native PostgreSQL enum types. Allows adding values without ALTER TYPE.

```sql
status VARCHAR(20) NOT NULL CHECK (status IN ('TODO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE'))
```

JPA annotation: `@Enumerated(EnumType.STRING)`

Enums: `task_status` (TODO/IN_PROGRESS/IN_REVIEW/DONE), `task_priority` (LOW/MEDIUM/HIGH/URGENT), `member_role` (OWNER/MEMBER)

## Case-Insensitive Unique Constraints

Use functional indexes on `LOWER(column)`:

```sql
CREATE UNIQUE INDEX idx_users_username_lower ON users (LOWER(username));
CREATE UNIQUE INDEX idx_users_email_lower ON users (LOWER(email));
CREATE UNIQUE INDEX idx_labels_project_name ON labels (project_id, LOWER(name));
```

Store emails in lowercase. Compare usernames case-insensitively in application code.

## Tables

| Table | PK | Notable constraints |
|-------|----|---------------------|
| users | BIGINT AUTO_INCREMENT | UNIQUE LOWER(username), UNIQUE LOWER(email), password VARCHAR(255) BCrypt hash |
| projects | BIGINT AUTO_INCREMENT | FK owner_id → users (denormalized convenience; canonical ownership is project_members) |
| project_members | (project_id, user_id) composite | FK both; role CHECK; always ≥1 OWNER enforced in code |
| tasks | BIGINT AUTO_INCREMENT | FK project_id, assignee_id (nullable), created_by; status+priority CHECK |
| labels | BIGINT AUTO_INCREMENT | UNIQUE (project_id, LOWER(name)) |
| task_labels | (task_id, label_id) composite | FK both; max 5 per task enforced in code |
| comments | BIGINT AUTO_INCREMENT | FK task_id, author_id; content max 2000 enforced in code |
| refresh_tokens | BIGINT AUTO_INCREMENT | UNIQUE token_hash SHA-256; FK user_id |
| login_attempts | BIGINT AUTO_INCREMENT | No FK (email column only) |

## Application-Enforced Rules (not DB constraints)

- Project must always have ≥1 OWNER — checked in service before role change or removal
- `assignee_id` must be a project member — checked in service on create/update; set to NULL when member is removed
- Task may have at most 5 labels — checked in service before insert
- `projects.owner_id` stays in sync with OWNER row in `project_members` — update both in same transaction on ownership transfer

## Cascade Delete Order

**Delete a project:** comments → task_labels → tasks → labels → project_members → project (handle in code, single transaction)

**Delete a task:** comments (ON DELETE CASCADE) + task_labels (ON DELETE CASCADE) → task

**Delete a label:** task_labels (ON DELETE CASCADE) → label

**Delete a user:** refresh_tokens (ON DELETE CASCADE on FK); set assignee_id=NULL on tasks (in code); block if user is OWNER of any project

## Required Indexes

```
idx_users_username_lower         UNIQUE LOWER(username)
idx_users_email_lower            UNIQUE LOWER(email)
idx_tasks_project_status         (project_id, status)
idx_tasks_project_assignee       (project_id, assignee_id)
idx_tasks_project_priority       (project_id, priority)
idx_tasks_project_deadline       (project_id, deadline)
idx_tasks_created_by             (created_by)
idx_comments_task_created        (task_id, created_at)
idx_refresh_tokens_user          (user_id)
idx_refresh_tokens_expires       (expires_at)
idx_login_attempts_email_time    (email, attempted_at)
idx_pm_user                      (user_id) on project_members
idx_labels_project_name          UNIQUE (project_id, LOWER(name))
```

## Flyway Migration Plan

| File | Content |
|------|---------|
| V1__create_users.sql | users table |
| V2__create_projects.sql | projects |
| V3__create_project_members.sql | project_members composite PK |
| V4__create_tasks.sql | tasks |
| V5__create_labels.sql | labels |
| V6__create_task_labels.sql | task_labels join table |
| V7__create_comments.sql | comments |
| V8__create_refresh_tokens.sql | refresh_tokens |
| V9__create_login_attempts.sql | login_attempts |
| V10__create_indexes.sql | all non-unique indexes |
| V11__seed_data.sql | dev seed data |

Seed passwords: all `Test1234!` hashed BCrypt. Seed users: alex_lead (alex@example.com), sam_dev (sam@example.com), jordan_free (jordan@example.com).
