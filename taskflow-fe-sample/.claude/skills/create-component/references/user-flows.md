# User Flows & Navigation

## Navigation Map
```
/login ──────────────────────── Login page (public only)
/register ───────────────────── Register page (public only)

/ ───────────────────────────── Dashboard (project list)
  └── [click project card]
      │
      ├── /projects/{id} ─────── Project board (Kanban, default view)
      │   ├── ?view=list ──────── Project list view (table)
      │   ├── [click task card] ─ Task detail panel (slide-over, no route change)
      │   └── [click Settings]
      │       └── /projects/{id}/settings ── Project settings
      │
/profile ────────────────────── User profile

[All routes under / require auth — unauthenticated → redirect to /login]
[After login, redirect to / (dashboard)]
[Task detail panel is an overlay — URL stays on /projects/{id}]
```

---

## Route Guards
- `ProtectedRoute`: wraps all authenticated routes. If `!isAuthenticated` → redirect to `/login`.
- `PublicOnlyRoute`: wraps `/login` and `/register`. If `isAuthenticated` → redirect to `/`.
- Project routes (403): if user navigates to a project they're not a member of → show 403 page.

---

## User Flow: Registration
1. User navigates to `/register`
2. Fills Username, Email, Password (real-time strength indicator)
3. "Create account" enabled once all 5 password rules pass
4. On submit: `POST /api/v1/auth/register`
   - 409 DUPLICATE_USERNAME → inline error below username field
   - 409 DUPLICATE_EMAIL → inline error below email field
   - 400 VALIDATION_FAILED → inline per-field errors
5. On success: auto-login (`POST /api/v1/auth/login`) → redirect to `/`
6. If auto-login fails: redirect to `/login` with "Account created! Please log in."

---

## User Flow: Login
1. User navigates to `/login`
2. Enters email + password
3. `POST /api/v1/auth/login`
   - 401 INVALID_CREDENTIALS → red alert box "Invalid email or password"
   - 423 ACCOUNT_LOCKED → red alert box "Account locked. Try again in X minutes."
4. On success: store `accessToken` in Zustand `authStore`, refresh token set as httpOnly cookie
5. Redirect to `/` (dashboard)

---

## User Flow: Token Refresh (Automatic)
1. Axios response interceptor catches 401 on any request
2. If not already refreshing: call `POST /api/v1/auth/refresh` (sends httpOnly cookie automatically)
3. On success: update `authStore.accessToken`, retry all queued 401 requests with new token
4. On failure: clear `authStore`, redirect to `/login`
5. Concurrent 401s use a queue (`isRefreshing` flag) to prevent multiple simultaneous refresh calls

---

## User Flow: Create Project
1. From dashboard, click "New project"
2. Modal opens with Name (required) + Description (optional) fields
3. `POST /api/v1/projects`
4. On success: modal closes → redirect to `/projects/{newId}` (empty board)

---

## User Flow: Create Task
1. From project board, click "+ Add task" in the TODO column
2. Quick-create form or modal: title (required), optional priority/deadline/assignee/labels
3. `POST /api/v1/projects/{projectId}/tasks`
4. On success: new task card appears in TODO column (optimistic update). Detail panel opens.

---

## User Flow: Update Task Status (Drag-and-Drop)
1. User drags task card from one Kanban column to another
2. Optimistic update: card moves immediately in UI
3. `PATCH /api/v1/tasks/{taskId}` with `{ status: "new_status" }`
4. On success: card stays in new column, `updatedAt` refreshed
5. On failure: card animates back to original column, error toast shown

---

## User Flow: Update Task Status (Detail Panel)
1. User opens task detail panel (click card)
2. Changes Status dropdown
3. `PATCH /api/v1/tasks/{taskId}` fires immediately
4. Board updates to move card to the correct column on next refetch

---

## User Flow: Task Detail — Inline Edit
1. Click any editable field in task panel (title, description, status, priority, assignee, deadline, labels)
2. Text fields: type → auto-save debounced 500ms after last keystroke, plus on blur
3. Select/date: save immediately on change
4. "Saving…" indicator near top of panel → "Saved" for 2 seconds
5. Labels: toggle via picker dropdown → attach (POST) or detach (DELETE) immediately

---

## User Flow: Add Comment
1. Scroll to bottom of task detail panel
2. Type in comment textarea
3. "Send" button enabled when input is non-blank
4. Ctrl+Enter to send
5. `POST /api/v1/tasks/{taskId}/comments`
6. Optimistic update: comment appears immediately at bottom of list

---

## User Flow: Invite Member
1. Owner navigates to `/projects/{id}/settings` → Members section
2. Enters email in invite form → "Invite"
3. `POST /api/v1/projects/{projectId}/members`
4. 404 USER_NOT_FOUND → inline error: "No account found with this email"
5. 409 ALREADY_MEMBER → inline error: "This user is already a member"
6. On success: new member appears in list immediately

---

## User Flow: Delete Project
1. Owner in Settings → General section → "Delete project"
2. Confirmation modal: "Delete [project name]? This will permanently delete all tasks, comments, and labels."
3. User must type the exact project name to confirm
4. `DELETE /api/v1/projects/{projectId}` with `{ confirmName }`
5. 400 CONFIRMATION_MISMATCH → inline error: "Project name does not match." Modal stays open.
6. On success: redirect to `/` (dashboard)

---

## User Flow: Leave Project
1. Member (non-owner) in Settings → Members section → "Leave project" on self row
2. Confirmation dialog: "Leave [project name]? Your task assignments will be cleared."
3. `DELETE /api/v1/projects/{projectId}/members/{userId}` (userId = self)
4. On success: redirect to `/` (dashboard). Project no longer appears in dashboard.

---

## Error Handling by Flow

### When navigating to a project after being removed
`GET /api/v1/projects/{id}` returns 403 NOT_PROJECT_MEMBER → show 403 page.

### When viewing a deleted task
Mutation or fetch returns 404 TASK_NOT_FOUND → toast "This task has been deleted." → close panel → invalidate tasks query.

### When current user loses project membership mid-session
Next API call returns 403 NOT_PROJECT_MEMBER → toast "You are no longer a member of this project." → navigate to `/`.

### Network errors
No `error.response` from Axios → toast "Unable to connect. Please check your connection and try again." Mutations: no auto-retry. Queries: retry 3× with exponential backoff (1s, 2s, 4s). After 3 failures: persistent banner at top of page.

---

## Responsive Behavior Notes
| Breakpoint | Key changes |
|------------|-------------|
| Desktop ≥1024px | Full layout |
| Tablet 768–1023px | Project grid: 2 cols. Board: horizontal scroll. Task panel: 400px. |
| Mobile <768px | Project grid: 1 col. Board: horizontal scroll (one column visible). Task panel: full-screen. Filters: collapse into "Filters" button. |
