# Domain Model — TaskFlow

## Enums

Stored as `VARCHAR` with `CHECK` constraints (not native PG enums).

- `task_status`: `TODO` | `IN_PROGRESS` | `IN_REVIEW` | `DONE`
- `task_priority`: `LOW` | `MEDIUM` | `HIGH` | `URGENT`
- `member_role`: `OWNER` | `MEMBER`

---

## Tables

### users
| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT |
| username | VARCHAR(50) | NOT NULL, UNIQUE LOWER(username) |
| email | VARCHAR(100) | NOT NULL, UNIQUE LOWER(email), stored lowercase |
| password | VARCHAR(255) | NOT NULL — BCrypt hash (60 chars actual, 255 for future-proofing) |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() |

### projects
| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT |
| name | VARCHAR(100) | NOT NULL |
| description | TEXT | NULLABLE |
| owner_id | BIGINT | FK → users.id, NOT NULL — denormalized; canonical ownership in project_members |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT NOW() — refreshed on PATCH |

### project_members
| Column | Type | Constraints |
|--------|------|-------------|
| project_id | BIGINT | FK → projects.id, NOT NULL |
| user_id | BIGINT | FK → users.id, NOT NULL |
| role | VARCHAR(10) | NOT NULL, CHECK (role IN ('OWNER','MEMBER')), DEFAULT 'MEMBER' |
| joined_at | TIMESTAMP | NOT NULL, DEFAULT NOW() |

Composite PK: `(project_id, user_id)`

### tasks
| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT |
| title | VARCHAR(200) | NOT NULL |
| description | TEXT | NULLABLE |
| status | VARCHAR(20) | NOT NULL, CHECK (...), DEFAULT 'TODO' |
| priority | VARCHAR(10) | NOT NULL, CHECK (...), DEFAULT 'MEDIUM' |
| deadline | DATE | NULLABLE — date only, no time |
| project_id | BIGINT | FK → projects.id, NOT NULL |
| assignee_id | BIGINT | FK → users.id, NULLABLE — must be project member (enforced in code) |
| created_by | BIGINT | FK → users.id, NOT NULL — never updated after creation |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT NOW() — refreshed on every update |

### labels
| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT |
| name | VARCHAR(50) | NOT NULL |
| color | VARCHAR(7) | NOT NULL — #RRGGBB format, validated in code |
| project_id | BIGINT | FK → projects.id, NOT NULL |

Unique: `(project_id, LOWER(name))`

### task_labels
| Column | Type | Constraints |
|--------|------|-------------|
| task_id | BIGINT | FK → tasks.id, NOT NULL |
| label_id | BIGINT | FK → labels.id, NOT NULL |

Composite PK: `(task_id, label_id)`. Max 5 labels per task enforced in code.

### comments
| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT |
| content | TEXT | NOT NULL — max 2000 chars enforced in code |
| task_id | BIGINT | FK → tasks.id, NOT NULL |
| author_id | BIGINT | FK → users.id, NOT NULL — never updated after creation |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT NOW() |

`edited` is derived: `updated_at != created_at` → API returns `edited: true`.

### refresh_tokens
| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT |
| token_hash | VARCHAR(64) | UNIQUE, NOT NULL — SHA-256 hash of actual token |
| user_id | BIGINT | FK → users.id, NOT NULL, ON DELETE CASCADE |
| expires_at | TIMESTAMP | NOT NULL |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() |

### login_attempts
| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT |
| email | VARCHAR(100) | NOT NULL |
| attempted_at | TIMESTAMP | NOT NULL, DEFAULT NOW() |
| success | BOOLEAN | NOT NULL |

No FK — email not necessarily a registered user (failed attempts for non-existent accounts).

---

## Relationships

```
users 1──< projects             (owner_id — denormalized)
users M>──< projects            (via project_members)
projects 1──< tasks             (project_id)
users 1──< tasks                (assignee_id, nullable)
users 1──< tasks                (created_by)
tasks M>──< labels              (via task_labels)
projects 1──< labels            (project_id)
tasks 1──< comments             (task_id)
users 1──< comments             (author_id)
users 1──< refresh_tokens       (user_id)
```

---

## JPA Entity Mapping Notes

- `ProjectMember` uses `@EmbeddedId ProjectMemberId(projectId, userId)`
- `TaskLabel` uses `@EmbeddedId TaskLabelId(taskId, labelId)`
- All enums: `@Enumerated(EnumType.STRING)`
- `updated_at` fields: use `@PreUpdate` on a base `Auditable` entity, or set manually in every service update method
- `assignee_id` nullable FK: `@ManyToOne(optional = true) @JoinColumn(name = "assignee_id")`
