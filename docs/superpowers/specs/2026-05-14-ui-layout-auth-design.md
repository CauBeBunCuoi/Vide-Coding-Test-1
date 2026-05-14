# UI Layout + Auth — Design Spec

**Date:** 2026-05-14  
**Status:** Approved

---

## Scope

Two parallel workstreams:
- **Teammate A (Frontend):** AppLayout, AuthLayout, Navbar, Axios client, auth store, ProtectedRoute/PublicOnlyRoute, LoginForm, RegisterForm, routing
- **Teammate B (Backend):** Spring Security + JWT, 5 auth endpoints, full V11 seed data

Working directories: `taskflow-fe-sample/` (frontend) · `taskflow-be-sample/` (backend)

---

## Architecture

```
Browser (localhost:5177)
  └── Vite dev server
        └── /api/* proxy → localhost:8087
                            └── Spring Boot
                                  └── PostgreSQL (localhost:1432 / DB: TaskFlowDB)
```

**Auth tokens:**
- Access token: JWT HS256, 15-min TTL, stored in Zustand memory only
- Refresh token: 128-char random string, SHA-256 hash stored in DB, sent as httpOnly cookie

**CORS:** Backend allows `http://localhost:5177` with `credentials: true`.

**Axios:** `baseURL = import.meta.env.VITE_BACKEND_REST_API_URL` (= `http://localhost:8087/api/v1`), `withCredentials: true`.

---

## API Contract

| Endpoint | Request | Response | Notes |
|---|---|---|---|
| `POST /api/v1/auth/register` | `{ username, email, password }` | 201 + `UserResponse` | Creates user only |
| `POST /api/v1/auth/login` | `{ email, password }` | 200 + `TokenResponse` | Sets httpOnly `refreshToken` cookie |
| `POST /api/v1/auth/refresh` | — | 200 + `TokenResponse` | Reads cookie; rotates it |
| `POST /api/v1/auth/logout` | — | 204 | Deletes DB record; clears cookie |
| `GET /api/v1/users/me` | — | 200 + `UserResponse` | Requires Bearer token |

**TokenResponse:** `{ accessToken: string, expiresIn: number }` (expiresIn = 900000 ms)

**UserResponse:** `{ id: number, username: string, email: string, createdAt: string }`

**ErrorResponse:** `{ error: string, message: string, details?: { fieldErrors?: [{field, message}] } }`

---

## Frontend Auth Flow

1. Login → `POST /auth/login` → store `accessToken` + call `GET /users/me` → `setAuth(token, user)` → redirect to `/`
2. Register → `POST /auth/register` → then `POST /auth/login` → same as above (or redirect to `/login` on auto-login failure)
3. Logout → `POST /auth/logout` → `logout()` store → redirect to `/login`
4. 401 on any request → attempt `POST /auth/refresh` → retry original → on refresh failure: `logout()` + redirect

---

## Seeding Strategy

**Mechanism:** Flyway (tracked, runs once). V11 is rewritten since DB not yet initialized.  
**Future additions:** Create `V12__...sql` — never modify V11 again.

**V11 seeds:**
- 3 users: alex_lead, sam_dev, jordan_free (all password `Test1234!`)
- 2 projects: "TaskFlow Demo" (all 3 members), "API Design" (alex_lead only)
- 6 tasks in TaskFlow Demo spanning all 4 Kanban columns
- 3 labels: frontend, backend, bug
- 4 task-label associations
- 3 comments on tasks

---

## UI Layout

**AppLayout:** fixed 56px navbar + scrollable content area (max-width 1280px, px-6, pt-14)

**AuthLayout:** full-screen centered card (max-w-[400px], white bg, rounded border)

**Navbar:** logo left → `/` | user avatar circle right → dropdown (Profile, Logout)

**Design tokens** (CSS custom properties in `index.css`):

```
--color-primary: #2563EB      --color-primary-hover: #1D4ED8
--color-danger: #DC2626       --color-danger-hover: #B91C1C
--color-success: #16A34A      --color-warning: #D97706
--color-gray-50: #F9FAFB      --color-gray-100: #F3F4F6
--color-gray-200: #E5E7EB     --color-gray-500: #6B7280
--color-gray-700: #374151     --color-gray-900: #111827
```
