# User Flows — TaskFlow

## Authentication Flows

### Register Flow
1. User navigates to `/register`
2. Fills Username, Email, Password — real-time password strength indicator (5 rules)
3. Submit → `POST /api/v1/auth/register` (201)
4. Auto-login → `POST /api/v1/auth/login` (200)
5. Access token stored in Zustand auth store (memory only). Refresh token arrives as httpOnly cookie.
6. Redirect to `/` (dashboard)
7. If auto-login fails → redirect to `/login` with message "Account created! Please log in."

### Login Flow
1. User navigates to `/login`
2. Enters email + password → `POST /api/v1/auth/login`
3. On success: store `accessToken` in memory (Zustand), `refreshToken` set as httpOnly cookie by server
4. Redirect to `/`
5. On 401 INVALID_CREDENTIALS → red alert above button
6. On 423 ACCOUNT_LOCKED → red alert with remaining minutes (calc from `lockedUntil`)

### Token Refresh Flow (Rotation)
1. Axios response interceptor catches 401 on any API call
2. If `isRefreshing = false`: set flag, call `POST /api/v1/auth/refresh` (browser sends cookie automatically)
3. If refresh succeeds: update Zustand with new access token; resolve all queued requests with new token
4. If refresh fails: clear auth state, redirect to `/login`
5. If `isRefreshing = true` when another 401 arrives: queue the request — retry after single refresh completes
6. Server rotates the refresh token on every refresh (old token is invalidated, new cookie set)

### Logout Flow
1. User clicks "Logout" in navbar dropdown
2. Call `POST /api/v1/auth/logout` (sends httpOnly cookie)
3. Server invalidates refresh token + clears cookie (Max-Age=0)
4. Clear `accessToken` from Zustand auth store
5. Redirect to `/login`

---

## Navigation Map

```
/login ──── Login page (public only — redirects to / if authenticated)
/register ── Register page (public only)

/ ──────────── Dashboard (project list) [protected]
  └── click card
      ├── /projects/{id} ──── Project board (Kanban or list view) [member only]
      │   ├── click task ──── Task detail slide-over (no route change)
      │   └── click Settings
      │       └── /projects/{id}/settings ──── Project settings [member]
/profile ──── User profile [protected]

All routes under / require authentication → unauthenticated users redirect to /login
Non-members visiting /projects/{id} → 403 page
```

---

## Key User Flows

### Create Project
1. Dashboard → "New project" button
2. Modal opens: Name (required, 1–100 chars) + Description (optional, max 1000 chars)
3. Submit → `POST /api/v1/projects` (201)
4. Modal closes, navigate to `/projects/{newId}` (empty board)

### View & Filter Kanban Board
1. Navigate to `/projects/{id}` → `GET /api/v1/projects/{id}` + `GET /api/v1/projects/{id}/tasks`
2. Board shows 4 columns with task cards
3. Apply filters (status, priority, assignee, label, search) → updates URL query params + re-fetches tasks
4. Toggle list view → `?view=list`, preference stored in localStorage

### Create Task
1. Click "Add task" button at bottom of TODO column
2. Form: Title (required), Description, Priority (default MEDIUM), Deadline, Assignee (project members), Labels (up to 5)
3. Submit → `POST /api/v1/projects/{id}/tasks` (201) — task always starts as TODO
4. Task detail panel opens showing new task. Board updates.

### Update Task via Drag-and-Drop
1. User drags task card from one column to another
2. Optimistic update: card moves immediately, board re-renders
3. `PATCH /api/v1/tasks/{id}` with `{ status: "NEW_STATUS" }`
4. On success: settle (invalidate tasks query for fresh data)
5. On failure: roll back optimistic update (card animates back), show error toast

### Edit Task Fields (Detail Panel)
- Text fields (title, description): save on blur, debounced 500ms. "Saving…" → "Saved" indicator.
- Select/date fields (status, priority, assignee, deadline): immediate save on change.
- Labels: toggle via `POST /api/v1/tasks/{id}/labels` (attach) or `DELETE /api/v1/tasks/{id}/labels/{labelId}` (detach). Debounce 200ms.

### Manage Project Members
1. Navigate to `/projects/{id}/settings` → Members section
2. Owner: invite by email (`POST /api/v1/projects/{id}/members`), change role (PATCH), remove (DELETE)
3. Member: "Leave project" (`DELETE /api/v1/projects/{id}/members/{myUserId}`)
4. On removal/leave: their task assignments cleared (backend sets assignee=null). Tasks remain.

### Delete Project
1. Settings → General → "Delete project" danger button (owner only)
2. Confirmation dialog — user must type project name exactly
3. `DELETE /api/v1/projects/{id}` with `{ confirmName }` → 204
4. Cascade: all tasks, comments, labels, members deleted
5. Redirect to `/`

---

## Optimistic Update Pattern

Used for: drag-and-drop status changes, all inline task field edits in the detail panel.

```
onMutate → cancel outgoing queries + snapshot previous data + apply optimistic update
onError  → restore snapshot from context
onSettled → invalidate affected query keys (refetch fresh data)
```

---

## Auth State

- `accessToken`: stored in memory via Zustand `authStore`. Never in localStorage.
- `refreshToken`: httpOnly cookie (JavaScript cannot access). Sent automatically by browser on `/api/v1/auth/*` paths.
- `isAuthenticated`: derived from whether accessToken is non-null in the store.
- **ProtectedRoute**: redirects to `/login` if `isAuthenticated = false`
- **PublicOnlyRoute**: redirects to `/` if `isAuthenticated = true` (login, register pages)
