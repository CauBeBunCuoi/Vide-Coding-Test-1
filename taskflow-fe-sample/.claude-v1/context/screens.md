# Screens — TaskFlow Frontend

## Screen 1: Login Page

**Route:** `/login` | **Auth:** No (redirect to `/` if already authenticated)
**API:** `POST /api/v1/auth/login`

- Centered card (max-width 400px), logo at top
- Fields: Email (`type="email"`), Password (show/hide toggle)
- Submit on Enter; button shows spinner + disabled during call
- Errors displayed as red alert box above button:
  - 401 INVALID_CREDENTIALS → "Invalid email or password"
  - 423 ACCOUNT_LOCKED → "Account locked. Try again in X minutes." (calc from `lockedUntil`)
  - Network error → "Unable to connect. Please check your connection and try again."
- Link: "Don't have an account? Register" → `/register`

---

## Screen 2: Register Page

**Route:** `/register` | **Auth:** No (redirect to `/` if authenticated)
**API:** `POST /api/v1/auth/register` then `POST /api/v1/auth/login` (auto-login)

- Same centered card layout as login
- Fields: Username, Email, Password
- Password strength indicator shows 5 requirements in real-time (✓/✗): 8+ chars, uppercase, lowercase, number, special char. Submit disabled until all pass.
- On success: auto-login → redirect to `/`; if auto-login fails → redirect to `/login` with "Account created! Please log in."
- Inline field errors: 409 DUPLICATE_USERNAME → below username; 409 DUPLICATE_EMAIL → below email
- Link: "Already have an account? Log in" → `/login`

---

## Screen 3: Dashboard (Project List)

**Route:** `/` | **Auth:** Yes
**API:** `GET /api/v1/projects`

- Page title "Projects" + "New project" button (top-right)
- Project card grid: 3 cols desktop, 2 tablet, 1 mobile. 12 cards/page with pagination.
- Card shows: name, description (max 120 chars), owner avatar+username, member count, task count. Click → `/projects/{id}`
- Empty state: "No projects yet" + "Create your first project" button
- "New project" → modal with Name (required) + Description (optional) fields → on success navigate to new project

---

## Screen 4: Project Board (Kanban)

**Route:** `/projects/{id}` | **Auth:** Yes + member
**API:** `GET /api/v1/projects/{id}`, `GET /api/v1/projects/{id}/tasks`, `PATCH /api/v1/tasks/{id}` (drag-drop)

**Project header:** name, description (collapsible), Board/List toggle, filter bar, member avatars, Settings icon → `/projects/{id}/settings`

**Kanban board:** 4 columns — TODO ("To Do"), IN_PROGRESS ("In Progress"), IN_REVIEW ("In Review"), DONE ("Done"). Each column: header with task count, vertically scrollable, "Add task" button at bottom of TODO only.

**Task card:**
- Priority badge (colored pill), label dots (top)
- Title (max 2 lines, ellipsis)
- Bottom: assignee initials, deadline (red if overdue = past date), comment count

**Drag-and-drop:** optimistic update on drop → `PATCH` with new status. On fail: animate back to original column + error toast.

**Filters bar:** status (multi-select pills), priority (dropdown multi), assignee (dropdown single, includes "Unassigned"), label (dropdown multi), text search (debounced 300ms), "Clear filters" link. Active filters sync to URL query params.

**Board vs List toggle:** stored in localStorage per project.

Non-member accessing project URL → 403 page.

---

## Screen 5: Project List View

**Route:** `/projects/{id}?view=list` | **Auth:** Yes + member

Same header + filters as board. Content is a sortable table:
- Columns: Title (sortable), Status (sortable), Priority (sortable), Assignee, Deadline (sortable, red if overdue), Labels, Comments
- Click column header to sort (toggle asc/desc). Sort synced to URL: `?sort=deadline,asc`
- 20 rows/page. Row click → task detail panel.

---

## Screen 6: Task Detail Panel (Slide-Over)

**Trigger:** Click task card or row
**API:** `GET/PATCH /api/v1/tasks/{id}`, `GET/POST /api/v1/tasks/{id}/comments`, `POST/DELETE /api/v1/tasks/{id}/labels/{id}`

- Right-side slide-over panel, 480px wide (full-screen mobile)
- Semi-transparent backdrop; close via X button, Escape, or backdrop click
- Panel scrollable vertically

**Content (top to bottom):**
1. **Title** — inline-editable (click to edit, blur/Enter to save)
2. **Meta row** — "Created by [user] · [date] · Last updated [relative]"
3. **Field grid** (2 cols desktop): Status (dropdown, immediate save), Priority (dropdown), Assignee (dropdown, project members + Unassigned), Deadline (date picker, clear button, red if overdue)
4. **Labels** — colored pills + "+" picker (shows all project labels with checkboxes). Hidden when 5 attached.
5. **Description** — markdown textarea. Click Edit → textarea + Save/Cancel. Rendered via react-markdown. Empty: "Add a description…" placeholder.
6. **Comments section** — "Comments (N)" header; "Load earlier comments" button if >20; list oldest-first; new comment textarea + Send (Ctrl+Enter). Three-dot menu on hover for author (Edit/Delete) or project owner (Delete only). Inline editing on edit. Confirmation on delete.
7. **Delete task** button (bottom, danger style) — visible only to task creator or project owner. Confirmation dialog with task title.

**Auto-save:** text fields debounced 500ms; select/date fields immediate. "Saving…" / "Saved" indicator near panel top.

---

## Screen 7: Project Settings

**Route:** `/projects/{id}/settings` | **Auth:** Yes + member
**API:** Multiple project/member/label endpoints

Three stacked sections:

**General:** Name + Description inputs (owner editable, member read-only). "Save changes" button (shown only if modified). "Delete project" danger button (owner only) — confirmation dialog requires typing project name.

**Members:** Member table (username, email, role, joined date). Owner sees: role dropdown + remove button per member (not on self-row), invite form (email input + "Invite"). Non-owner sees: "Leave project" button on their own row (MEMBER only).

**Labels:** Label list (colored dot + name + task count + edit/delete per label). "Add label" form at top: name input + 12-color palette picker. Edit inline. Delete confirmation shows task count. Duplicate name error inline.

---

## Screen 8: Profile Page

**Route:** `/profile` | **Auth:** Yes
**API:** `GET/PATCH /api/v1/users/me`

- Large initials avatar (deterministic color from username hash)
- Editable fields: Username, Email
- "Save changes" button (disabled until a field is changed)
- Inline errors per field for 409 duplicates

---

## Error Pages

| Page | Trigger | Message | CTA |
|------|---------|---------|-----|
| 403 | Non-member visiting project URL | "You don't have access to this project" | "Go to Dashboard" |
| 404 | Any non-existent route | "Page not found" | "Go to Dashboard" |

**Global network error:** toast "Connection lost. Retrying…" with exponential backoff (1s, 2s, 4s, max 3). After 3 failures: persistent banner "Unable to connect to the server."

---

## Component Inventory

| Component | Used in |
|-----------|---------|
| Button | everywhere — variants: primary, secondary, danger, ghost; sizes: sm, md; loading spinner |
| Input | forms — label, placeholder, error slot, optional icon |
| TextArea | description, comments, project description — auto-growing, character counter |
| Select | task fields, filters — single-select and multi-select, searchable |
| DatePicker | task deadline — calendar popup, clear button |
| Modal | project create, confirmations — focus-trapped, Escape + backdrop close |
| SlideOver | task detail — 480px, focus-trapped |
| Toast | success/error feedback — auto-dismiss (success 5s, error 8s), bottom-right, max 3 stacked |
| Avatar | user display — initials-based, sizes sm/md/lg, deterministic color from username hash |
| Badge | priority, status — colored pill |
| LabelPill | task labels — colored, removable variant |
| EmptyState | dashboard, board, comments — icon + message + optional CTA |
| Pagination | project list, task list, comments |
| ConfirmDialog | delete actions |
| Skeleton | loading states — animate-pulse placeholders |

---

## Responsive Breakpoints

| Breakpoint | Width |
|------------|-------|
| Desktop | ≥1024px |
| Tablet | 768–1023px |
| Mobile | <768px |

Mobile specifics: board horizontal scroll (one column visible), task panel full-screen, filters collapse to "Filters" button, project grid 1 column.
