# TaskFlow — API Contract

## General Conventions

Base URL: `/api/v1`

All requests and responses use JSON (`Content-Type: application/json`).

Timestamps are ISO 8601 in UTC: `2025-06-15T10:30:00Z`.

Date-only fields (e.g., deadline) use `YYYY-MM-DD` format: `2025-06-15`.

Authenticated endpoints require the header: `Authorization: Bearer <accessToken>`.

IDs are 64-bit integers (Java `Long`, TypeScript `number`).

Nullable fields are omitted from responses when null, unless stated otherwise.

---

## Pagination

List endpoints that support pagination accept these query parameters:

| Parameter | Type | Default | Constraints |
|-----------|------|---------|-------------|
| page | integer | 0 | 0-indexed |
| size | integer | 20 | min 1, max 100 |

Paginated responses wrap the result:

```json
{
  "content": [ ... ],
  "page": 0,
  "size": 20,
  "totalElements": 45,
  "totalPages": 3
}
```

---

## Shared Response Shapes

These shapes are embedded in endpoint responses. They are defined here once to avoid repetition.

**UserSummary** — used wherever a user reference appears (owner, assignee, author, member):
```json
{
  "id": 1,
  "username": "alex_lead"
}
```

**LabelSummary** — used in task responses and label lists:
```json
{
  "id": 1,
  "name": "frontend",
  "color": "#3B82F6"
}
```

---

## 1. Auth Endpoints

### POST /api/v1/auth/register

Register a new user account.

**Request body:**
```json
{
  "username": "alex_lead",
  "email": "alex@example.com",
  "password": "Str0ng!Pass"
}
```

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| username | string | yes | 3–50 chars, alphanumeric and underscores only, must start with a letter |
| email | string | yes | valid email format, max 100 chars |
| password | string | yes | 8–72 chars, at least 1 uppercase, 1 lowercase, 1 digit, 1 special character |

**Response 201 Created:**
```json
{
  "id": 1,
  "username": "alex_lead",
  "email": "alex@example.com",
  "createdAt": "2025-06-15T10:30:00Z"
}
```

**Error responses:**
- 400 — validation errors (see error format below)
- 409 — `{ "error": "DUPLICATE_USERNAME", "message": "Username is already taken" }` or `{ "error": "DUPLICATE_EMAIL", "message": "Email is already registered" }`

---

### POST /api/v1/auth/login

Authenticate and receive tokens.

**Request body:**
```json
{
  "email": "alex@example.com",
  "password": "Str0ng!Pass"
}
```

| Field | Type | Required |
|-------|------|----------|
| email | string | yes |
| password | string | yes |

**Response 200 OK:**

Response body:
```json
{
  "accessToken": "eyJhbGciOi...",
  "expiresIn": 900
}
```

Additionally, the server sets an httpOnly cookie:
```
Set-Cookie: refreshToken=<token>; HttpOnly; Secure; SameSite=Strict; Path=/api/v1/auth; Max-Age=604800
```

| Body field | Description |
|------------|-------------|
| accessToken | JWT, 15-minute lifetime |
| expiresIn | Token lifetime in seconds (900) |

**Error responses:**
- 401 — `{ "error": "INVALID_CREDENTIALS", "message": "Invalid email or password" }`
- 423 — `{ "error": "ACCOUNT_LOCKED", "message": "Account locked due to too many failed attempts. Try again in 15 minutes.", "lockedUntil": "2025-06-15T10:45:00Z" }`

---

### POST /api/v1/auth/refresh

Exchange a valid refresh token for a new access token. Uses refresh token rotation — old refresh token is invalidated, new one is issued.

**Request:** No body. The refresh token is read from the httpOnly cookie.

**Response 200 OK:**

Response body:
```json
{
  "accessToken": "eyJhbGciOi...",
  "expiresIn": 900
}
```

Server also sets a new refreshToken cookie (same attributes, new value).

**Error responses:**
- 401 — `{ "error": "INVALID_REFRESH_TOKEN", "message": "Refresh token is invalid or expired" }`

---

### POST /api/v1/auth/logout

Invalidate the current refresh token and clear the cookie.

**Request:** No body. Uses the refresh token from the cookie.

**Response 204 No Content** (no body)

Server clears the refreshToken cookie:
```
Set-Cookie: refreshToken=; HttpOnly; Secure; SameSite=Strict; Path=/api/v1/auth; Max-Age=0
```

This endpoint always returns 204 even if the refresh token is already invalid — logout is idempotent.

---

## 2. User Endpoints

### GET /api/v1/users/me

Get the authenticated user's profile.

**Response 200 OK:**
```json
{
  "id": 1,
  "username": "alex_lead",
  "email": "alex@example.com",
  "createdAt": "2025-06-15T10:30:00Z"
}
```

---

### PATCH /api/v1/users/me

Update the authenticated user's profile.

**Request body** (all fields optional, include only fields to update):
```json
{
  "username": "alex_new_name",
  "email": "alex_new@example.com"
}
```

| Field | Type | Constraints |
|-------|------|-------------|
| username | string | same constraints as registration |
| email | string | same constraints as registration |

**Response 200 OK:** Returns the updated user object (same shape as GET /users/me).

**Error responses:**
- 400 — validation errors
- 409 — duplicate username or email

---

## 3. Project Endpoints

### GET /api/v1/projects

List all projects the authenticated user belongs to (as owner or member).

**Query parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| page | integer | 0 | Page number |
| size | integer | 12 | Page size (max 100) |
| sort | string | `createdAt,desc` | Sort field and direction. Allowed fields: `createdAt`, `name` |

**Response 200 OK:**
```json
{
  "content": [
    {
      "id": 1,
      "name": "TaskFlow MVP",
      "description": "Project management tool for small teams",
      "owner": { "id": 1, "username": "alex_lead" },
      "memberCount": 3,
      "taskCount": 12,
      "createdAt": "2025-06-15T10:30:00Z"
    }
  ],
  "page": 0,
  "size": 12,
  "totalElements": 1,
  "totalPages": 1
}
```

---

### POST /api/v1/projects

Create a new project. The authenticated user becomes the owner.

**Request body:**
```json
{
  "name": "TaskFlow MVP",
  "description": "Project management tool for small teams"
}
```

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| name | string | yes | 1–100 chars, trimmed, must not be blank |
| description | string | no | max 1000 chars |

**Response 201 Created:**
```json
{
  "id": 1,
  "name": "TaskFlow MVP",
  "description": "Project management tool for small teams",
  "owner": { "id": 1, "username": "alex_lead" },
  "memberCount": 1,
  "taskCount": 0,
  "createdAt": "2025-06-15T10:30:00Z"
}
```

---

### GET /api/v1/projects/{projectId}

Get project details including member list and task count breakdown.

**Response 200 OK:**
```json
{
  "id": 1,
  "name": "TaskFlow MVP",
  "description": "Project management tool for small teams",
  "owner": { "id": 1, "username": "alex_lead" },
  "members": [
    { "id": 1, "username": "alex_lead", "email": "alex@example.com", "role": "OWNER", "joinedAt": "2025-06-15T10:30:00Z" },
    { "id": 2, "username": "sam_dev", "email": "sam@example.com", "role": "MEMBER", "joinedAt": "2025-06-16T09:00:00Z" }
  ],
  "taskCounts": {
    "TODO": 3,
    "IN_PROGRESS": 5,
    "IN_REVIEW": 2,
    "DONE": 2
  },
  "createdAt": "2025-06-15T10:30:00Z"
}
```

**Error responses:**
- 403 — `{ "error": "NOT_PROJECT_MEMBER", "message": "You are not a member of this project" }`
- 404 — `{ "error": "PROJECT_NOT_FOUND", "message": "Project not found" }`

---

### PATCH /api/v1/projects/{projectId}

Update project details. Owner only.

**Request body** (all fields optional):
```json
{
  "name": "TaskFlow v2",
  "description": "Updated description"
}
```

| Field | Type | Constraints |
|-------|------|-------------|
| name | string | 1–100 chars, trimmed, must not be blank |
| description | string | max 1000 chars |

**Response 200 OK:** Returns the updated project object (same shape as GET /projects/{id}).

**Error responses:**
- 403 — `{ "error": "NOT_PROJECT_OWNER", "message": "Only the project owner can perform this action" }`
- 404 — project not found

---

### DELETE /api/v1/projects/{projectId}

Delete a project and all associated data. Owner only.

**Request body:**
```json
{
  "confirmName": "TaskFlow MVP"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| confirmName | string | yes | Must exactly match the project name |

**Response 204 No Content**

**Error responses:**
- 400 — `{ "error": "CONFIRMATION_MISMATCH", "message": "Project name does not match" }`
- 403 — not project owner

Cascade deletes: project_members → tasks → task_labels → comments → labels (all associated records for this project).

---

## 4. Member Endpoints

### GET /api/v1/projects/{projectId}/members

List all members of a project. Any project member can call this.

**Response 200 OK:**
```json
[
  {
    "id": 1,
    "username": "alex_lead",
    "email": "alex@example.com",
    "role": "OWNER",
    "joinedAt": "2025-06-15T10:30:00Z"
  },
  {
    "id": 2,
    "username": "sam_dev",
    "email": "sam@example.com",
    "role": "MEMBER",
    "joinedAt": "2025-06-16T09:00:00Z"
  }
]
```

This endpoint is not paginated — member lists are expected to be small (max ~10).

Owner is always returned first, remaining members sorted by joinedAt ascending.

---

### POST /api/v1/projects/{projectId}/members

Invite a user to the project by email. Owner only.

**Request body:**
```json
{
  "email": "sam@example.com",
  "role": "MEMBER"
}
```

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| email | string | yes | valid email format |
| role | string | no | `OWNER` or `MEMBER`, default `MEMBER` |

**Response 201 Created:**
```json
{
  "id": 2,
  "username": "sam_dev",
  "email": "sam@example.com",
  "role": "MEMBER",
  "joinedAt": "2025-06-16T09:00:00Z"
}
```

**Error responses:**
- 403 — not project owner
- 404 — `{ "error": "USER_NOT_FOUND", "message": "No account found with this email" }`
- 409 — `{ "error": "ALREADY_MEMBER", "message": "This user is already a member of this project" }`

---

### PATCH /api/v1/projects/{projectId}/members/{userId}

Change a member's role. Owner only.

**Request body:**
```json
{
  "role": "OWNER"
}
```

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| role | string | yes | `OWNER` or `MEMBER` |

**Response 200 OK:** Returns the updated member object.

**Error responses:**
- 400 — `{ "error": "LAST_OWNER", "message": "Cannot change role — this is the only owner of the project" }`
- 403 — not project owner
- 404 — member not found in this project

---

### DELETE /api/v1/projects/{projectId}/members/{userId}

Remove a member from the project (owner only), or leave the project (self).

If `userId` matches the authenticated user and their role is MEMBER, this acts as "leave project."

**Response 204 No Content**

**Error responses:**
- 400 — `{ "error": "OWNER_CANNOT_LEAVE", "message": "Project owner cannot leave. Transfer ownership or delete the project." }`
- 403 — not project owner (when trying to remove someone else)
- 404 — member not found in this project

Side effect: when a member is removed, all their task assignments within this project are set to `assignee_id = NULL`. The tasks themselves are not deleted.

---

## 5. Task Endpoints

### GET /api/v1/projects/{projectId}/tasks

List tasks in a project with filtering, searching, and sorting.

**Query parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| status | string | (all) | Comma-separated: `TODO,IN_PROGRESS,IN_REVIEW,DONE` |
| priority | string | (all) | Comma-separated: `LOW,MEDIUM,HIGH,URGENT` |
| assigneeId | long | (all) | Filter by assignee. Use `0` for unassigned tasks. |
| labelId | string | (all) | Comma-separated label IDs. Task must have ALL specified labels. |
| search | string | (none) | Case-insensitive search in title and description |
| sort | string | `createdAt,desc` | Format: `field,direction`. Fields: `createdAt`, `deadline`, `priority`, `title`. Direction: `asc`, `desc`. For `priority` sort, order is URGENT > HIGH > MEDIUM > LOW. For `deadline` sort, null deadlines are placed last regardless of direction. |
| page | integer | 0 | Page number |
| size | integer | 20 | Page size (max 100) |

**Response 200 OK:**
```json
{
  "content": [
    {
      "id": 1,
      "title": "Implement login page",
      "description": "Build the login form with email and password fields",
      "status": "IN_PROGRESS",
      "priority": "HIGH",
      "deadline": "2025-07-01",
      "project": { "id": 1, "name": "TaskFlow MVP" },
      "assignee": { "id": 2, "username": "sam_dev" },
      "createdBy": { "id": 1, "username": "alex_lead" },
      "labels": [
        { "id": 1, "name": "frontend", "color": "#3B82F6" },
        { "id": 3, "name": "urgent", "color": "#EF4444" }
      ],
      "commentCount": 5,
      "createdAt": "2025-06-16T14:00:00Z",
      "updatedAt": "2025-06-17T09:30:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 12,
  "totalPages": 1
}
```

**Error responses:**
- 403 — not a project member
- 404 — project not found

Note: the `project` field in each task response contains only id and name (ProjectSummary shape). This is useful when tasks are viewed across contexts, and it keeps each task response self-contained.

---

### POST /api/v1/projects/{projectId}/tasks

Create a new task. Any project member.

**Request body:**
```json
{
  "title": "Implement login page",
  "description": "Build the login form with email and password fields",
  "priority": "HIGH",
  "deadline": "2025-07-01",
  "assigneeId": 2,
  "labelIds": [1, 3]
}
```

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| title | string | yes | 1–200 chars, trimmed, must not be blank |
| description | string | no | max 5000 chars |
| priority | string | no | `LOW`, `MEDIUM`, `HIGH`, `URGENT`. Default: `MEDIUM` |
| deadline | string | no | `YYYY-MM-DD` format |
| assigneeId | long | no | Must be a member of this project |
| labelIds | long[] | no | Max 5 labels. Each must belong to this project. |

Task is always created with `status = "TODO"` — this field is not settable at creation time.

`created_by` is automatically set to the authenticated user.

**Response 201 Created:** Returns the full task object (same shape as in list response).

**Error responses:**
- 400 — validation errors
- 400 — `{ "error": "ASSIGNEE_NOT_MEMBER", "message": "Assignee is not a member of this project" }`
- 400 — `{ "error": "LABEL_NOT_IN_PROJECT", "message": "One or more labels do not belong to this project" }`
- 400 — `{ "error": "TOO_MANY_LABELS", "message": "A task can have at most 5 labels" }`

---

### GET /api/v1/tasks/{taskId}

Get full details of a single task.

The authenticated user must be a member of the project that this task belongs to.

**Response 200 OK:** Same shape as task objects in the list response.

**Error responses:**
- 403 — not a member of the task's project
- 404 — `{ "error": "TASK_NOT_FOUND", "message": "Task not found" }`

---

### PATCH /api/v1/tasks/{taskId}

Update task fields. Any member of the task's project.

**Request body** (all fields optional, include only fields to update):
```json
{
  "title": "Updated title",
  "description": "Updated description",
  "status": "IN_PROGRESS",
  "priority": "URGENT",
  "deadline": "2025-07-15",
  "assigneeId": 3
}
```

| Field | Type | Constraints |
|-------|------|-------------|
| title | string | 1–200 chars, trimmed, must not be blank |
| description | string | max 5000 chars. Send empty string `""` to clear. |
| status | string | `TODO`, `IN_PROGRESS`, `IN_REVIEW`, `DONE` |
| priority | string | `LOW`, `MEDIUM`, `HIGH`, `URGENT` |
| deadline | string | `YYYY-MM-DD` format. Send `null` to clear. |
| assigneeId | long | Must be a project member. Send `null` to unassign. |

On any successful update, `updatedAt` is refreshed to the current timestamp.

**Response 200 OK:** Returns the updated full task object.

**Error responses:**
- 400 — validation errors, assignee not member
- 403 — not a member of the task's project
- 404 — task not found

---

### DELETE /api/v1/tasks/{taskId}

Delete a task. Only the task creator or a project owner.

**Response 204 No Content**

Cascade deletes: task_labels and comments associated with this task.

**Error responses:**
- 403 — `{ "error": "NOT_TASK_OWNER", "message": "Only the task creator or project owner can delete this task" }`
- 404 — task not found

---

## 6. Label Endpoints

### GET /api/v1/projects/{projectId}/labels

List all labels in a project.

**Response 200 OK:**
```json
[
  {
    "id": 1,
    "name": "frontend",
    "color": "#3B82F6",
    "taskCount": 4
  },
  {
    "id": 2,
    "name": "backend",
    "color": "#10B981",
    "taskCount": 7
  }
]
```

This endpoint is not paginated — label lists are expected to be small.

Sorted alphabetically by name.

---

### POST /api/v1/projects/{projectId}/labels

Create a new label. Any project member.

**Request body:**
```json
{
  "name": "frontend",
  "color": "#3B82F6"
}
```

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| name | string | yes | 1–50 chars, trimmed |
| color | string | yes | Hex color, must match `^#[0-9A-Fa-f]{6}$` |

**Response 201 Created:**
```json
{
  "id": 1,
  "name": "frontend",
  "color": "#3B82F6",
  "taskCount": 0
}
```

**Error responses:**
- 400 — validation errors
- 409 — `{ "error": "DUPLICATE_LABEL_NAME", "message": "A label with this name already exists in this project" }`

---

### PATCH /api/v1/labels/{labelId}

Update a label. Any member of the label's project.

**Request body** (all fields optional):
```json
{
  "name": "front-end",
  "color": "#2563EB"
}
```

**Response 200 OK:** Returns the updated label object (with taskCount).

**Error responses:**
- 409 — duplicate label name within the same project

---

### DELETE /api/v1/labels/{labelId}

Delete a label. Any member of the label's project.

**Response 204 No Content**

Side effect: all task_labels entries referencing this label are deleted (label is detached from all tasks).

---

## 7. Task-Label Endpoints

### POST /api/v1/tasks/{taskId}/labels

Attach a label to a task.

**Request body:**
```json
{
  "labelId": 1
}
```

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| labelId | long | yes | Must belong to the same project as the task |

**Response 201 Created:** Returns the updated task object (full shape, with updated labels array).

**Error responses:**
- 400 — `{ "error": "LABEL_NOT_IN_PROJECT", "message": "This label does not belong to the task's project" }`
- 400 — `{ "error": "TOO_MANY_LABELS", "message": "A task can have at most 5 labels" }`
- 409 — `{ "error": "LABEL_ALREADY_ATTACHED", "message": "This label is already attached to the task" }`

---

### DELETE /api/v1/tasks/{taskId}/labels/{labelId}

Detach a label from a task.

**Response 204 No Content**

**Error responses:**
- 404 — label is not attached to this task

---

## 8. Comment Endpoints

### GET /api/v1/tasks/{taskId}/comments

List comments on a task. Paginated, oldest first.

**Query parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| page | integer | 0 | Page number |
| size | integer | 20 | Page size (max 100) |

**Response 200 OK:**
```json
{
  "content": [
    {
      "id": 1,
      "content": "Should we use a modal or a full page for login?",
      "author": { "id": 2, "username": "sam_dev" },
      "edited": false,
      "createdAt": "2025-06-17T09:00:00Z",
      "updatedAt": "2025-06-17T09:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

The `edited` field is `true` when `updatedAt` differs from `createdAt`. The frontend uses this to show the "(edited)" indicator.

Default sort: `createdAt,asc` (oldest first — natural conversation order).

---

### POST /api/v1/tasks/{taskId}/comments

Add a comment to a task. Any member of the task's project.

**Request body:**
```json
{
  "content": "Should we use a modal or a full page for login?"
}
```

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| content | string | yes | 1–2000 chars, trimmed, must not be blank |

**Response 201 Created:** Returns the comment object (same shape as in list response).

---

### PATCH /api/v1/comments/{commentId}

Edit a comment. Author only.

**Request body:**
```json
{
  "content": "Updated comment text"
}
```

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| content | string | yes | 1–2000 chars, trimmed, must not be blank |

On edit, `updatedAt` is refreshed and `edited` will return `true`.

**Response 200 OK:** Returns the updated comment object.

**Error responses:**
- 403 — `{ "error": "NOT_COMMENT_AUTHOR", "message": "Only the comment author can edit this comment" }`

---

### DELETE /api/v1/comments/{commentId}

Delete a comment. Author or project owner.

**Response 204 No Content**

**Error responses:**
- 403 — `{ "error": "NOT_AUTHORIZED", "message": "Only the comment author or project owner can delete this comment" }`

---

## Error Response Format

All error responses use this consistent shape:

```json
{
  "error": "ERROR_CODE",
  "message": "Human-readable description",
  "details": { ... }
}
```

| Field | Type | Always present | Description |
|-------|------|----------------|-------------|
| error | string | yes | Machine-readable error code (SCREAMING_SNAKE_CASE) |
| message | string | yes | Human-readable message (can be shown in UI) |
| details | object | no | Additional context, used for validation errors |

**Validation error example (400):**
```json
{
  "error": "VALIDATION_FAILED",
  "message": "One or more fields are invalid",
  "details": {
    "fieldErrors": [
      { "field": "username", "message": "Username must be between 3 and 50 characters" },
      { "field": "email", "message": "Email format is invalid" }
    ]
  }
}
```

**Standard HTTP status usage:**
| Status | Meaning |
|--------|---------|
| 200 | Success — resource returned or updated |
| 201 | Created — new resource created |
| 204 | No Content — success with no response body (delete, logout) |
| 400 | Bad Request — validation error or business rule violation |
| 401 | Unauthorized — missing or invalid access token |
| 403 | Forbidden — authenticated but lacks permission |
| 404 | Not Found — resource does not exist |
| 409 | Conflict — duplicate resource (unique constraint) |
| 423 | Locked — account locked after too many failed login attempts |
| 429 | Too Many Requests — rate limit exceeded |
| 500 | Internal Server Error — unexpected server failure |
