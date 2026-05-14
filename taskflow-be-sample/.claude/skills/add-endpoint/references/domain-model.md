# Domain Model & Database Schema

PostgreSQL 17.5. Migrations via Flyway 11 (files in `src/main/resources/db/migration/`).

## Enums (stored as VARCHAR + CHECK)
- `task_status`: `TODO` | `IN_PROGRESS` | `IN_REVIEW` | `DONE`
- `task_priority`: `LOW` | `MEDIUM` | `HIGH` | `URGENT`
- `member_role`: `OWNER` | `MEMBER`

---

## Tables

### users
| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT |
| username | VARCHAR(50) | UNIQUE (case-insensitive), NOT NULL |
| email | VARCHAR(100) | UNIQUE (case-insensitive), NOT NULL — stored lowercase |
| password | VARCHAR(255) | NOT NULL (BCrypt hash) |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() |

Unique indexes: `idx_users_username_lower` on `LOWER(username)`, `idx_users_email_lower` on `LOWER(email)`.

### projects
| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT |
| name | VARCHAR(100) | NOT NULL |
| description | TEXT | NULLABLE |
| owner_id | BIGINT | FK → users.id, NOT NULL |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT NOW() |

`owner_id` is denormalized — canonical ownership is `project_members(role=OWNER)`. Must keep in sync when ownership transfers (update both in same transaction).

### project_members
| Column | Type | Constraints |
|--------|------|-------------|
| project_id | BIGINT | FK → projects.id, NOT NULL |
| user_id | BIGINT | FK → users.id, NOT NULL |
| role | VARCHAR(10) | NOT NULL, CHECK (`OWNER`\|`MEMBER`), DEFAULT `MEMBER` |
| joined_at | TIMESTAMP | NOT NULL, DEFAULT NOW() |

Composite PK: `(project_id, user_id)`. Index: `idx_pm_user` on `(user_id)`.
On member removal: set `assignee_id = NULL` on that user's tasks scoped to this project (application code, not DB cascade).
Always at least one OWNER per project — enforced in application code before role changes.

### tasks
| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT |
| title | VARCHAR(200) | NOT NULL |
| description | TEXT | NULLABLE |
| status | VARCHAR(20) | NOT NULL, CHECK, DEFAULT `TODO` |
| priority | VARCHAR(10) | NOT NULL, CHECK, DEFAULT `MEDIUM` |
| deadline | DATE | NULLABLE |
| project_id | BIGINT | FK → projects.id, NOT NULL |
| assignee_id | BIGINT | FK → users.id, NULLABLE |
| created_by | BIGINT | FK → users.id, NOT NULL |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT NOW() |

`assignee_id` must be a project member — enforced in application code.
`deadline` is DATE-only (no time component).
`updated_at` refreshed on every field change.

Indexes: `idx_tasks_project_status (project_id, status)`, `idx_tasks_project_assignee (project_id, assignee_id)`, `idx_tasks_project_priority (project_id, priority)`, `idx_tasks_project_deadline (project_id, deadline)`, `idx_tasks_created_by (created_by)`.

### labels
| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT |
| name | VARCHAR(50) | NOT NULL |
| color | VARCHAR(7) | NOT NULL (hex `#RRGGBB`) |
| project_id | BIGINT | FK → projects.id, NOT NULL |

Unique: `(project_id, LOWER(name))`. Index: `idx_labels_project_name`.

### task_labels
| Column | Constraints |
|--------|-------------|
| task_id | FK → tasks.id, NOT NULL |
| label_id | FK → labels.id, NOT NULL |

Composite PK: `(task_id, label_id)`. Max 5 labels per task enforced in application code.
ON DELETE CASCADE from both tasks and labels.

### comments
| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT |
| content | TEXT | NOT NULL (max 2000 chars enforced in application code) |
| task_id | BIGINT | FK → tasks.id, NOT NULL |
| author_id | BIGINT | FK → users.id, NOT NULL |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT NOW() |

`edited` is computed: `updatedAt != createdAt`. Index: `idx_comments_task_created (task_id, created_at)`.

### refresh_tokens
| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT |
| token_hash | VARCHAR(64) | UNIQUE, NOT NULL (SHA-256 of raw token) |
| user_id | BIGINT | FK → users.id, NOT NULL |
| expires_at | TIMESTAMP | NOT NULL |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() |

Indexes: `idx_refresh_tokens_user (user_id)`, `idx_refresh_tokens_expires (expires_at)`.

### login_attempts
| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT |
| email | VARCHAR(100) | NOT NULL |
| attempted_at | TIMESTAMP | NOT NULL, DEFAULT NOW() |
| success | BOOLEAN | NOT NULL |

Index: `idx_login_attempts_email_time (email, attempted_at)`.
Lock check: count rows where `email=?` AND `success=false` AND `attempted_at > NOW() - INTERVAL '15 minutes'`. If ≥5, locked.

---

## Relationships
```
users 1──< projects              (owner_id)
users M>──< projects             (via project_members)
projects 1──< tasks              (project_id)
users 1──< tasks                 (assignee_id, nullable)
users 1──< tasks                 (created_by)
tasks M>──< labels               (via task_labels)
projects 1──< labels             (project_id)
tasks 1──< comments              (task_id)
users 1──< comments              (author_id)
users 1──< refresh_tokens        (user_id)
```

---

## Flyway Migration Files
V1__create_users.sql → V2__create_projects.sql → V3__create_project_members.sql → V4__create_tasks.sql → V5__create_labels.sql → V6__create_task_labels.sql → V7__create_comments.sql → V8__create_refresh_tokens.sql → V9__create_login_attempts.sql → V10__create_indexes.sql → V11__seed_data.sql

Seed users (password `Test1234!`): alex_lead (alex@example.com), sam_dev (sam@example.com), jordan_free (jordan@example.com).

---

## Cascade Delete Order
**Delete project:** comments → task_labels → tasks → labels → project_members → project
**Delete task:** comments → task_labels → task
**Delete label:** task_labels → label
**Remove member:** set `assignee_id = NULL` on tasks in that project (application code in transaction)

---

## Data Consistency Invariants
- Every project has ≥1 OWNER in project_members
- `projects.owner_id` matches the OWNER in project_members (sync in same transaction on ownership transfer)
- Task assignee is a member of the task's project
- Task labels all belong to the same project as the task
- Task has at most 5 labels
- No two labels in same project share a name (case-insensitive)
- No two users share username or email (case-insensitive)
- Refresh tokens stored as SHA-256 hashes
