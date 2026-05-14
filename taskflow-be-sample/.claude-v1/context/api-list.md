# API List — TaskFlow

Base URL: `/api/v1`
All requests/responses: JSON. Auth required: `Authorization: Bearer <accessToken>`.
Timestamps: ISO 8601 UTC. Date-only (deadline): `YYYY-MM-DD`. IDs: 64-bit Long.

## Shared Shapes

**UserSummary**: `{ "id": 1, "username": "alex_lead" }`
**LabelSummary**: `{ "id": 1, "name": "frontend", "color": "#3B82F6" }`
**Paginated response**: `{ "content": [...], "page": 0, "size": 20, "totalElements": 45, "totalPages": 3 }`
Paginated endpoints accept `page` (0-indexed, default 0) and `size` (default 20, max 100).

---

## Auth

| Method | Path | Auth | Status |
|--------|------|------|--------|
| POST | /auth/register | No | 201 |
| POST | /auth/login | No | 200 |
| POST | /auth/refresh | Cookie only | 200 |
| POST | /auth/logout | Cookie only | 204 |

**Register** body: `{ username, email, password }` → `{ id, username, email, createdAt }`
Errors: 400 VALIDATION_FAILED, 409 DUPLICATE_USERNAME, 409 DUPLICATE_EMAIL

**Login** body: `{ email, password }` → `{ accessToken, expiresIn: 900 }` + sets httpOnly `refreshToken` cookie
Errors: 401 INVALID_CREDENTIALS, 423 ACCOUNT_LOCKED (+ `lockedUntil` timestamp)

**Refresh**: No body (uses cookie) → `{ accessToken, expiresIn }` + new cookie. Rotates token.
Errors: 401 INVALID_REFRESH_TOKEN

**Logout**: No body → 204. Clears cookie. Always succeeds (idempotent).

---

## Users

| Method | Path | Auth | Status |
|--------|------|------|--------|
| GET | /users/me | Yes | 200 |
| PATCH | /users/me | Yes | 200 |

User shape: `{ id, username, email, createdAt }`
PATCH body (all optional): `{ username?, email? }` → returns updated user
Errors: 400 VALIDATION_FAILED, 409 DUPLICATE_USERNAME, 409 DUPLICATE_EMAIL

---

## Projects

| Method | Path | Auth | Status |
|--------|------|------|--------|
| GET | /projects | Yes | 200 paginated (default 12/page) |
| POST | /projects | Yes | 201 |
| GET | /projects/{projectId} | Member | 200 |
| PATCH | /projects/{projectId} | Owner | 200 |
| DELETE | /projects/{projectId} | Owner | 204 |

**List** query params: `page`, `size`, `sort` (createdAt,desc or name — default createdAt,desc)
**List item shape**: `{ id, name, description, owner: UserSummary, memberCount, taskCount, createdAt }`
**Detail shape**: adds `members: [{ id, username, email, role, joinedAt }]` + `taskCounts: { TODO, IN_PROGRESS, IN_REVIEW, DONE }`
**Create** body: `{ name* (1–100), description? (max 1000) }` — creator auto-becomes OWNER
**Update** body (all optional): `{ name?, description? }`
**Delete** body: `{ confirmName* }` — must exactly match project name. Cascade-deletes all project data.
Errors: 403 NOT_PROJECT_MEMBER, 403 NOT_PROJECT_OWNER, 404 PROJECT_NOT_FOUND, 400 CONFIRMATION_MISMATCH

---

## Members

| Method | Path | Auth | Status |
|--------|------|------|--------|
| GET | /projects/{projectId}/members | Member | 200 array (not paginated) |
| POST | /projects/{projectId}/members | Owner | 201 |
| PATCH | /projects/{projectId}/members/{userId} | Owner | 200 |
| DELETE | /projects/{projectId}/members/{userId} | Owner or self | 204 |

Member shape: `{ id, username, email, role, joinedAt }` — owner returned first, rest sorted joinedAt asc
**Invite** body: `{ email*, role? (default MEMBER) }`
**Role change** body: `{ role* }` — OWNER or MEMBER
**Delete/leave** side effect: sets `assignee_id = NULL` on all their tasks in this project
Errors: 403 NOT_PROJECT_OWNER, 404 USER_NOT_FOUND, 409 ALREADY_MEMBER, 400 LAST_OWNER, 400 OWNER_CANNOT_LEAVE

---

## Tasks

| Method | Path | Auth | Status |
|--------|------|------|--------|
| GET | /projects/{projectId}/tasks | Member | 200 paginated (default 20/page) |
| POST | /projects/{projectId}/tasks | Member | 201 |
| GET | /tasks/{taskId} | Member of task's project | 200 |
| PATCH | /tasks/{taskId} | Member | 200 |
| DELETE | /tasks/{taskId} | Creator or project Owner | 204 |

**List** query params: `status` (comma-sep: TODO,IN_PROGRESS,IN_REVIEW,DONE), `priority` (comma-sep: LOW,MEDIUM,HIGH,URGENT), `assigneeId` (Long; 0=unassigned), `labelId` (comma-sep; task must match ALL), `search` (case-insensitive in title+description), `sort` (field,dir — fields: createdAt/deadline/priority/title; null deadlines always last), `page`, `size`

**Task shape**: `{ id, title, description, status, priority, deadline, project: {id,name}, assignee: UserSummary|null, createdBy: UserSummary, labels: LabelSummary[], commentCount, createdAt, updatedAt }`

**Create** body: `{ title* (1–200), description?, priority? (default MEDIUM), deadline? (YYYY-MM-DD), assigneeId?, labelIds? (max 5) }` — status always TODO; created_by = authenticated user

**Update** body (all optional): `{ title?, description?, status?, priority?, deadline?, assigneeId? }` — send `null` to clear deadline/assignee; send `""` to clear description

**Delete** cascade: task_labels + comments for this task
Errors: 400 ASSIGNEE_NOT_MEMBER, 400 LABEL_NOT_IN_PROJECT, 400 TOO_MANY_LABELS, 403 NOT_TASK_OWNER, 404 TASK_NOT_FOUND

---

## Labels

| Method | Path | Auth | Status |
|--------|------|------|--------|
| GET | /projects/{projectId}/labels | Member | 200 array (not paginated, sorted alpha) |
| POST | /projects/{projectId}/labels | Member | 201 |
| PATCH | /labels/{labelId} | Member of label's project | 200 |
| DELETE | /labels/{labelId} | Member of label's project | 204 |

Label shape: `{ id, name, color, taskCount }`
**Create** body: `{ name* (1–50), color* (#RRGGBB hex) }`
**Delete** cascade: all task_label associations
Errors: 409 DUPLICATE_LABEL_NAME

---

## Task Labels

| Method | Path | Auth | Status |
|--------|------|------|--------|
| POST | /tasks/{taskId}/labels | Member | 201 — returns updated full task object |
| DELETE | /tasks/{taskId}/labels/{labelId} | Member | 204 |

**Attach** body: `{ labelId* }` — label must belong to same project as task
Errors: 400 LABEL_NOT_IN_PROJECT, 400 TOO_MANY_LABELS, 409 LABEL_ALREADY_ATTACHED

---

## Comments

| Method | Path | Auth | Status |
|--------|------|------|--------|
| GET | /tasks/{taskId}/comments | Member | 200 paginated (oldest first) |
| POST | /tasks/{taskId}/comments | Member | 201 |
| PATCH | /comments/{commentId} | Author only | 200 |
| DELETE | /comments/{commentId} | Author or project Owner | 204 |

Comment shape: `{ id, content, author: UserSummary, edited, createdAt, updatedAt }`
`edited` = true when updatedAt differs from createdAt
**Create** body: `{ content* (1–2000) }`
**Edit** body: `{ content* }` — refreshes updatedAt, sets edited=true
Errors: 403 NOT_COMMENT_AUTHOR, 403 NOT_AUTHORIZED

---

## Standard Error Format

```json
{ "error": "ERROR_CODE", "message": "Human-readable message", "details": { ... } }
```

Validation 400: `details.fieldErrors = [{ "field": "title", "message": "Title is required" }]`

Status codes: 200 OK, 201 Created, 204 No Content, 400 Bad Request, 401 Unauthorized, 403 Forbidden, 404 Not Found, 409 Conflict, 423 Locked, 429 Too Many Requests, 500 Internal Server Error
