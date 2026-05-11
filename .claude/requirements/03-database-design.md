# TaskFlow — Database Design

## Database Engine

PostgreSQL 17.5, managed via Docker container (see development workflows doc for setup).

Schema migrations managed by Flyway 11 in code-first mode. Migration files live in `src/main/resources/db/migration/`.

---

## Enums

These are stored as PostgreSQL `VARCHAR` with CHECK constraints (not native PG enums) to allow easier migration when adding values.

**task_status**: `TODO` | `IN_PROGRESS` | `IN_REVIEW` | `DONE`

**task_priority**: `LOW` | `MEDIUM` | `HIGH` | `URGENT`

**member_role**: `OWNER` | `MEMBER`

In JPA, map these as `@Enumerated(EnumType.STRING)`.

---

## Tables

### users

Stores registered user accounts.

| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT |
| username | VARCHAR(50) | UNIQUE, NOT NULL |
| email | VARCHAR(100) | UNIQUE, NOT NULL |
| password | VARCHAR(255) | NOT NULL (BCrypt hash, 60 chars actual) |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() |

Notes:
- `password` stores a BCrypt hash. The 255-length column accommodates future algorithm changes.
- `username` uniqueness is case-insensitive. Enforce via a unique index on `LOWER(username)`.
- `email` uniqueness is case-insensitive. Enforce via a unique index on `LOWER(email)`. Store in lowercase.

---

### projects

Top-level organizational unit.

| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT |
| name | VARCHAR(100) | NOT NULL |
| description | TEXT | NULLABLE |
| owner_id | BIGINT | FK → users.id, NOT NULL |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT NOW() |

Notes:
- `owner_id` is a denormalized convenience reference. The canonical ownership is in `project_members` with role=OWNER. Both must be kept in sync.
- On project creation, insert a corresponding `project_members` row with role=OWNER.
- If ownership is transferred (role change), update `owner_id` to match the new OWNER.
- `updated_at` is refreshed on any PATCH to the project.

---

### project_members

Join table linking users to projects with a role.

| Column | Type | Constraints |
|--------|------|-------------|
| project_id | BIGINT | FK → projects.id, NOT NULL |
| user_id | BIGINT | FK → users.id, NOT NULL |
| role | VARCHAR(10) | NOT NULL, CHECK (role IN ('OWNER', 'MEMBER')), DEFAULT 'MEMBER' |
| joined_at | TIMESTAMP | NOT NULL, DEFAULT NOW() |

Composite PK: `(project_id, user_id)`

Notes:
- On DELETE of a project_member row, set `assignee_id = NULL` on all tasks in that project where `assignee_id` = the removed user's id. This is handled in application code, not a DB cascade, because it requires scoping to the specific project.
- A project must always have at least one OWNER. Enforce this in application code before allowing role changes or removals.

---

### tasks

Individual work items within a project.

| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT |
| title | VARCHAR(200) | NOT NULL |
| description | TEXT | NULLABLE |
| status | VARCHAR(20) | NOT NULL, CHECK (status IN ('TODO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE')), DEFAULT 'TODO' |
| priority | VARCHAR(10) | NOT NULL, CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')), DEFAULT 'MEDIUM' |
| deadline | DATE | NULLABLE |
| project_id | BIGINT | FK → projects.id, NOT NULL |
| assignee_id | BIGINT | FK → users.id, NULLABLE |
| created_by | BIGINT | FK → users.id, NOT NULL |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT NOW() |

Notes:
- `assignee_id` is nullable — unassigned tasks are valid.
- `deadline` is DATE-only (no time component). The frontend should display it in the user's local timezone, but storage is timezone-agnostic since it's just a date.
- `assignee_id` should reference a user who is a member of the task's project. This is enforced in application code (not a DB constraint) because the relationship is indirect (task → project → project_members → user).
- `updated_at` is refreshed on any field change (including status changes via drag-and-drop).
- `created_by` is never updated after creation.
- On DELETE CASCADE from projects: all tasks in the project are deleted.

---

### labels

Color-coded tags scoped to a project.

| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT |
| name | VARCHAR(50) | NOT NULL |
| color | VARCHAR(7) | NOT NULL (hex format: #RRGGBB) |
| project_id | BIGINT | FK → projects.id, NOT NULL |

Unique constraint: `(project_id, LOWER(name))` — label names are unique per project, case-insensitive.

Notes:
- `color` is validated in application code to match `^#[0-9A-Fa-f]{6}$`.
- On DELETE CASCADE from projects: all labels in the project are deleted.

---

### task_labels

Join table linking tasks to labels (many-to-many).

| Column | Type | Constraints |
|--------|------|-------------|
| task_id | BIGINT | FK → tasks.id, NOT NULL |
| label_id | BIGINT | FK → labels.id, NOT NULL |

Composite PK: `(task_id, label_id)`

Notes:
- Application code enforces max 5 labels per task.
- On DELETE CASCADE from tasks: label associations are removed.
- On DELETE CASCADE from labels: task associations are removed.

---

### comments

Discussion threads on tasks.

| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT |
| content | TEXT | NOT NULL |
| task_id | BIGINT | FK → tasks.id, NOT NULL |
| author_id | BIGINT | FK → users.id, NOT NULL |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT NOW() |

Notes:
- `content` max length (2000 chars) is enforced in application code.
- When `updated_at` differs from `created_at`, the comment has been edited. The API exposes this as a boolean `edited` field.
- On DELETE CASCADE from tasks: all comments on the task are deleted.
- `author_id` is never updated after creation.

---

### refresh_tokens

Stores active refresh tokens for JWT rotation and revocation.

| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT |
| token_hash | VARCHAR(64) | UNIQUE, NOT NULL (SHA-256 hash of the actual token) |
| user_id | BIGINT | FK → users.id, NOT NULL |
| expires_at | TIMESTAMP | NOT NULL |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() |

Notes:
- Store a SHA-256 hash of the refresh token, not the raw value. This way, even if the database is compromised, tokens cannot be used.
- On token rotation (refresh): delete the old row, insert a new one.
- On logout: delete the row matching the token hash.
- A scheduled job should clean up expired tokens periodically (e.g., nightly).
- On DELETE CASCADE from users: all refresh tokens for the user are deleted.

---

### login_attempts

Tracks failed login attempts for account locking.

| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT |
| email | VARCHAR(100) | NOT NULL |
| attempted_at | TIMESTAMP | NOT NULL, DEFAULT NOW() |
| success | BOOLEAN | NOT NULL |

Notes:
- On each login attempt, insert a row.
- To check if account is locked: count rows where `email = ?` AND `success = false` AND `attempted_at > NOW() - INTERVAL '15 minutes'`. If count >= 5, account is locked.
- On successful login, no need to delete failed attempts — they will naturally age out. But insert a success row so the count resets.
- A scheduled job should clean up rows older than 24 hours.

---

## Relationships Summary

```
users 1 ──< projects                (owner_id)
users M >──< projects               (via project_members)
projects 1 ──< tasks                (project_id)
users 1 ──< tasks                   (assignee_id, nullable)
users 1 ──< tasks                   (created_by)
tasks M >──< labels                 (via task_labels)
projects 1 ──< labels               (project_id)
tasks 1 ──< comments                (task_id)
users 1 ──< comments                (author_id)
users 1 ──< refresh_tokens          (user_id)
```

---

## Indexes

Beyond the primary keys, unique constraints, and foreign keys (which Hibernate auto-indexes on some DBs but PG does not auto-index FK columns), create these explicit indexes:

| Table | Index | Columns | Rationale |
|-------|-------|---------|-----------|
| users | idx_users_username_lower | `LOWER(username)` UNIQUE | Case-insensitive username lookup |
| users | idx_users_email_lower | `LOWER(email)` UNIQUE | Case-insensitive email lookup and login |
| tasks | idx_tasks_project_status | `(project_id, status)` | Board view: list tasks by project and status column |
| tasks | idx_tasks_project_assignee | `(project_id, assignee_id)` | Filter tasks by assignee within a project |
| tasks | idx_tasks_project_priority | `(project_id, priority)` | Sort/filter by priority within a project |
| tasks | idx_tasks_project_deadline | `(project_id, deadline)` | Sort by deadline within a project |
| tasks | idx_tasks_created_by | `(created_by)` | Delete permission check (is user the creator?) |
| comments | idx_comments_task_created | `(task_id, created_at)` | List comments for a task in chronological order |
| refresh_tokens | idx_refresh_tokens_user | `(user_id)` | Lookup/cleanup tokens by user |
| refresh_tokens | idx_refresh_tokens_expires | `(expires_at)` | Cleanup job: delete expired tokens |
| login_attempts | idx_login_attempts_email_time | `(email, attempted_at)` | Lock check: count recent failures |
| project_members | idx_pm_user | `(user_id)` | "List my projects" query — find all projects for a user |
| labels | idx_labels_project_name | `(project_id, LOWER(name))` UNIQUE | Duplicate label name check within a project |

---

## Flyway Migration Plan

Each migration is a versioned SQL file. Naming convention: `V{number}__{description}.sql`

| File | Content |
|------|---------|
| V1__create_users.sql | users table |
| V2__create_projects.sql | projects table with FK to users |
| V3__create_project_members.sql | project_members table with composite PK |
| V4__create_tasks.sql | tasks table with all FKs and CHECK constraints |
| V5__create_labels.sql | labels table with unique constraint |
| V6__create_task_labels.sql | task_labels join table |
| V7__create_comments.sql | comments table |
| V8__create_refresh_tokens.sql | refresh_tokens table |
| V9__create_login_attempts.sql | login_attempts table |
| V10__create_indexes.sql | All non-unique indexes listed above |
| V11__seed_data.sql | Development seed data (see below) |

Each migration must be idempotent-safe — use `IF NOT EXISTS` where possible. Never modify a migration file after it has been applied — create a new version instead.

---

## Seed Data (V11)

Development seed data for manual testing and frontend development.

**Users** (all passwords are `Test1234!` hashed with BCrypt):

| id | username | email |
|----|----------|-------|
| 1 | alex_lead | alex@example.com |
| 2 | sam_dev | sam@example.com |
| 3 | jordan_free | jordan@example.com |

**Projects:**

| id | name | owner_id |
|----|------|----------|
| 1 | TaskFlow MVP | 1 |
| 2 | Marketing Site | 1 |

**Project Members:**

| project_id | user_id | role |
|------------|---------|------|
| 1 | 1 | OWNER |
| 1 | 2 | MEMBER |
| 1 | 3 | MEMBER |
| 2 | 1 | OWNER |
| 2 | 3 | MEMBER |

**Labels (for project 1):**

| id | name | color | project_id |
|----|------|-------|------------|
| 1 | frontend | #3B82F6 | 1 |
| 2 | backend | #10B981 | 1 |
| 3 | urgent | #EF4444 | 1 |
| 4 | design | #8B5CF6 | 1 |

**Tasks (for project 1):**

| id | title | status | priority | deadline | project_id | assignee_id | created_by |
|----|-------|--------|----------|----------|------------|-------------|------------|
| 1 | Design login page | DONE | HIGH | 2025-06-20 | 1 | 2 | 1 |
| 2 | Implement JWT auth | IN_PROGRESS | URGENT | 2025-06-25 | 1 | 2 | 1 |
| 3 | Create project model | TODO | MEDIUM | 2025-07-01 | 1 | NULL | 1 |
| 4 | Write API tests | TODO | MEDIUM | NULL | 1 | 3 | 1 |
| 5 | Setup CI pipeline | IN_REVIEW | LOW | 2025-07-10 | 1 | 3 | 1 |

**Task Labels:**

| task_id | label_id |
|---------|----------|
| 1 | 1 |
| 1 | 4 |
| 2 | 2 |
| 2 | 3 |
| 3 | 2 |
| 4 | 2 |

**Comments (on task 2):**

| id | content | task_id | author_id |
|----|---------|---------|-----------|
| 1 | Should we use symmetric or asymmetric keys for JWT? | 2 | 2 |
| 2 | Let's go with asymmetric (RS256) for production, symmetric (HS256) for dev. | 2 | 1 |
| 3 | Makes sense. I'll implement HS256 for now and parameterize the algorithm. | 2 | 2 |

---

## Cascade Delete Summary

When deleting entities, cascades must happen in this order to satisfy FK constraints:

**Delete a project:**
1. Delete comments (on tasks in this project)
2. Delete task_labels (on tasks in this project)
3. Delete tasks (in this project)
4. Delete labels (in this project)
5. Delete project_members (for this project)
6. Delete the project

**Delete a task:**
1. Delete comments (on this task)
2. Delete task_labels (for this task)
3. Delete the task

**Delete a label:**
1. Delete task_labels (for this label)
2. Delete the label

**Delete a user:**
1. Delete refresh_tokens (for this user)
2. Set `assignee_id = NULL` on tasks where this user is assigned
3. Further cleanup depends on business rules — in TaskFlow, users should not be deleted if they own projects. Block user deletion in application code if the user is an OWNER of any project.

These cascades should be configured as `ON DELETE CASCADE` on the FK constraints where safe (task_labels, comments referencing tasks, refresh_tokens referencing users). For cases requiring conditional logic (member removal → unassign tasks), handle in application code within a transaction.
