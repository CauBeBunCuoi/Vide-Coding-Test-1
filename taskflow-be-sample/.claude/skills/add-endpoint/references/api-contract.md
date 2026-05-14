# API Contract Reference

Base URL: `/api/v1` | Format: JSON | Auth header: `Authorization: Bearer <accessToken>`
Timestamps: ISO 8601 UTC. Date-only: `YYYY-MM-DD`. IDs: `Long` (64-bit integer).

## Shared Shapes
**UserSummary:** `{ "id": 1, "username": "alex_lead" }`
**LabelSummary:** `{ "id": 1, "name": "frontend", "color": "#3B82F6" }`
**ProjectSummary:** `{ "id": 1, "name": "TaskFlow MVP" }`

---

## 1. Auth Endpoints

### POST /api/v1/auth/register
Request: `{ username, email, password }`
- username: 3–50 chars, `^[a-zA-Z][a-zA-Z0-9_]*$`
- email: valid format, max 100 chars
- password: 8–72 chars, ≥1 uppercase/lowercase/digit/special char

Response 201: `{ id, username, email, createdAt }`
Errors: 400 VALIDATION_FAILED | 409 DUPLICATE_USERNAME | 409 DUPLICATE_EMAIL

### POST /api/v1/auth/login
Request: `{ email, password }`
Response 200: `{ accessToken, expiresIn: 900 }` + httpOnly cookie `refreshToken` (7 days, SameSite=Strict, Path=/api/v1/auth)
Errors: 401 INVALID_CREDENTIALS | 423 ACCOUNT_LOCKED (includes `lockedUntil`)

### POST /api/v1/auth/refresh
Request: no body (reads refreshToken from httpOnly cookie). Rotation: old token invalidated, new one issued.
Response 200: `{ accessToken, expiresIn: 900 }` + new refreshToken cookie
Error: 401 INVALID_REFRESH_TOKEN

### POST /api/v1/auth/logout
Request: no body (reads cookie). Always returns 204 even if token already invalid (idempotent).
Response 204 + clears refreshToken cookie (Max-Age=0)

---

## 2. User Endpoints

### GET /api/v1/users/me
Response 200: `{ id, username, email, createdAt }`

### PATCH /api/v1/users/me
Request (all optional): `{ username?, email? }` — same constraints as registration
Response 200: updated user object
Errors: 400 VALIDATION_FAILED | 409 DUPLICATE_USERNAME | 409 DUPLICATE_EMAIL

---

## 3. Project Endpoints

### GET /api/v1/projects
Query: `page` (default 0), `size` (default 12), `sort` (default `createdAt,desc`, allowed: `createdAt`, `name`)
Response 200: paginated list of `{ id, name, description, owner: UserSummary, memberCount, taskCount, createdAt }`

### POST /api/v1/projects
Request: `{ name (1–100 chars, required), description (max 1000, optional) }`
Response 201: project object (same shape as list item)

### GET /api/v1/projects/{projectId}
Response 200: `{ id, name, description, owner: UserSummary, members: [...MemberDetail], taskCounts: {TODO, IN_PROGRESS, IN_REVIEW, DONE}, createdAt }`
Errors: 403 NOT_PROJECT_MEMBER | 404 PROJECT_NOT_FOUND

### PATCH /api/v1/projects/{projectId}
Request (all optional): `{ name?, description? }` — owner only
Response 200: updated project detail object
Errors: 403 NOT_PROJECT_OWNER | 404

### DELETE /api/v1/projects/{projectId}
Request: `{ confirmName }` — must exactly match project name (case-sensitive)
Response 204. Cascade: project_members → tasks → task_labels → comments → labels → project
Errors: 400 CONFIRMATION_MISMATCH | 403 NOT_PROJECT_OWNER

---

## 4. Member Endpoints

### GET /api/v1/projects/{projectId}/members
Response 200: array (not paginated) of `{ id, username, email, role, joinedAt }`. Owner first, then by joinedAt asc.

### POST /api/v1/projects/{projectId}/members
Request: `{ email, role? (default MEMBER) }` — owner only
Response 201: `{ id, username, email, role, joinedAt }`
Errors: 403 NOT_PROJECT_OWNER | 404 USER_NOT_FOUND | 409 ALREADY_MEMBER

### PATCH /api/v1/projects/{projectId}/members/{userId}
Request: `{ role: "OWNER" | "MEMBER" }` — owner only
Response 200: updated member object
Errors: 400 LAST_OWNER | 403 NOT_PROJECT_OWNER | 404

### DELETE /api/v1/projects/{projectId}/members/{userId}
Owner removing member, OR member removing themselves (leave project).
Response 204. Side effect: all task assignments in this project for the removed user set to NULL.
Errors: 400 OWNER_CANNOT_LEAVE | 403 NOT_PROJECT_OWNER (when removing someone else) | 404

---

## 5. Task Endpoints

### GET /api/v1/projects/{projectId}/tasks
Query params:
- `status`: comma-separated (`TODO,IN_PROGRESS,IN_REVIEW,DONE`)
- `priority`: comma-separated (`LOW,MEDIUM,HIGH,URGENT`)
- `assigneeId`: long (use `0` for unassigned)
- `labelId`: comma-separated label IDs (task must have ALL specified labels)
- `search`: case-insensitive search in title + description (escape `%` and `_` in LIKE)
- `sort`: `field,direction` — fields: `createdAt`, `deadline`, `priority`, `title`. Null deadlines last.
- `page`, `size` (default 20)

Response 200: paginated list of full task objects (see shape below)
Errors: 403 NOT_PROJECT_MEMBER | 404 PROJECT_NOT_FOUND

**Task object shape:**
```json
{
  "id": 1, "title": "...", "description": "...", "status": "IN_PROGRESS", "priority": "HIGH",
  "deadline": "2025-07-01", "project": ProjectSummary, "assignee": UserSummary,
  "createdBy": UserSummary, "labels": [LabelSummary],
  "commentCount": 5, "createdAt": "...", "updatedAt": "..."
}
```

### POST /api/v1/projects/{projectId}/tasks
Request: `{ title (1–200, required), description (max 5000), priority (default MEDIUM), deadline (YYYY-MM-DD), assigneeId (must be member), labelIds (max 5, must belong to project) }`
Status always created as TODO.
Response 201: full task object
Errors: 400 ASSIGNEE_NOT_MEMBER | 400 LABEL_NOT_IN_PROJECT | 400 TOO_MANY_LABELS

### GET /api/v1/tasks/{taskId}
Response 200: full task object. User must be member of task's project.
Errors: 403 NOT_PROJECT_MEMBER | 404 TASK_NOT_FOUND

### PATCH /api/v1/tasks/{taskId}
Request (all optional): `{ title?, description? (empty string clears it), status?, priority?, deadline? (null clears it), assigneeId? (null unassigns) }`
Any project member can update. `updatedAt` refreshed on any change.
Response 200: full task object
Errors: 400 ASSIGNEE_NOT_MEMBER | 403 NOT_PROJECT_MEMBER | 404 TASK_NOT_FOUND

### DELETE /api/v1/tasks/{taskId}
Only task creator or project owner. Cascade: task_labels + comments.
Response 204
Errors: 403 NOT_TASK_OWNER | 404 TASK_NOT_FOUND

---

## 6. Label Endpoints

### GET /api/v1/projects/{projectId}/labels
Response 200: array (not paginated) of `{ id, name, color, taskCount }`. Sorted alphabetically.

### POST /api/v1/projects/{projectId}/labels
Request: `{ name (1–50 chars), color (^#[0-9A-Fa-f]{6}$) }` — any project member
Response 201: `{ id, name, color, taskCount: 0 }`
Error: 409 DUPLICATE_LABEL_NAME

### PATCH /api/v1/labels/{labelId}
Request (all optional): `{ name?, color? }` — any member of label's project
Response 200: updated label with taskCount
Error: 409 DUPLICATE_LABEL_NAME

### DELETE /api/v1/labels/{labelId}
Response 204. Side effect: all task_labels entries for this label are deleted.

---

## 7. Task-Label Endpoints

### POST /api/v1/tasks/{taskId}/labels
Request: `{ labelId }` — must belong to same project as task. Max 5 labels per task.
Response 201: full task object with updated labels array
Errors: 400 LABEL_NOT_IN_PROJECT | 400 TOO_MANY_LABELS | 409 LABEL_ALREADY_ATTACHED

### DELETE /api/v1/tasks/{taskId}/labels/{labelId}
Response 204
Error: 404 (label not attached to this task)

---

## 8. Comment Endpoints

### GET /api/v1/tasks/{taskId}/comments
Query: `page` (default 0), `size` (default 20). Sorted oldest first (natural conversation order).
Response 200: paginated list of `{ id, content, author: UserSummary, edited: boolean, createdAt, updatedAt }`
`edited` is true when `updatedAt != createdAt`.

### POST /api/v1/tasks/{taskId}/comments
Request: `{ content (1–2000 chars, must not be blank) }` — any member of task's project
Response 201: comment object

### PATCH /api/v1/comments/{commentId}
Request: `{ content (1–2000 chars) }` — author only. Sets `updatedAt`, `edited` becomes true.
Response 200: updated comment object
Error: 403 NOT_COMMENT_AUTHOR

### DELETE /api/v1/comments/{commentId}
Author or project owner.
Response 204
Error: 403 NOT_AUTHORIZED

---

## Error Code Catalog

### Auth
VALIDATION_FAILED (400) | INVALID_CREDENTIALS (401) | INVALID_REFRESH_TOKEN (401) | DUPLICATE_USERNAME (409) | DUPLICATE_EMAIL (409) | ACCOUNT_LOCKED (423, includes `lockedUntil`) | RATE_LIMITED (429)

### Projects
PROJECT_NOT_FOUND (404) | NOT_PROJECT_MEMBER (403) | NOT_PROJECT_OWNER (403) | CONFIRMATION_MISMATCH (400)

### Members
USER_NOT_FOUND (404) | ALREADY_MEMBER (409) | LAST_OWNER (400) | OWNER_CANNOT_LEAVE (400)

### Tasks
TASK_NOT_FOUND (404) | NOT_TASK_OWNER (403) | ASSIGNEE_NOT_MEMBER (400) | LABEL_NOT_IN_PROJECT (400) | TOO_MANY_LABELS (400)

### Labels
DUPLICATE_LABEL_NAME (409) | LABEL_ALREADY_ATTACHED (409)

### Comments
NOT_COMMENT_AUTHOR (403) | NOT_AUTHORIZED (403)

### Global
UNAUTHORIZED (401) | INTERNAL_ERROR (500)
