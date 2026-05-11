# API List

<!-- List every API endpoint with its method, path, purpose, and key request/response notes.
     This is the source of truth for what the backend exposes. -->

## Auth
| Method | Path | Purpose |
|--------|------|---------|
| POST | /auth/login | Authenticate user, return JWT |
| POST | /auth/logout | Invalidate token |
| POST | /auth/refresh | Refresh access token |

## Tasks
| Method | Path | Purpose |
|--------|------|---------|
| GET | /tasks | List tasks (paginated, filterable) |
| POST | /tasks | Create a new task |
| GET | /tasks/:id | Get task detail |
| PATCH | /tasks/:id | Update task fields |
| DELETE | /tasks/:id | Soft-delete a task |

## Users
| Method | Path | Purpose |
|--------|------|---------|
| GET | /users/me | Get current user profile |
| PATCH | /users/me | Update profile |
