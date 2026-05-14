# Screen Specifications

## Screen 1: Login Page
**Route:** `/login` | **Auth required:** No (redirect to `/` if already authenticated)
**API:** `POST /api/v1/auth/login`

Layout: centered card (max-width 400px), TaskFlow logo, Email field, Password field, "Log in" button (full-width), "Don't have an account? Register" link → `/register`.

Field behaviors:
- Email: `type="email"`, `autocomplete="email"`
- Password: `type="password"`, `autocomplete="current-password"`, show/hide toggle icon
- Submit on Enter from either field. Button shows spinner during call, disabled to prevent double-submit.

Error display (red alert box above button):
- Invalid credentials: "Invalid email or password"
- Account locked: "Account locked. Try again in X minutes." (calculate from `lockedUntil`)
- Network error: "Unable to connect. Please check your connection and try again."

---

## Screen 2: Register Page
**Route:** `/register` | **Auth required:** No (redirect to `/` if already authenticated)
**API:** `POST /api/v1/auth/register` → then auto-login via `POST /api/v1/auth/login`

Layout: same centered card. Fields: Username, Email, Password. Password strength indicator below password field. "Create account" button (full-width). "Already have an account? Log in" → `/login`.

Field behaviors:
- Username: inline validation on blur — constraints: 3–50 chars, letters/numbers/underscores, start with letter.
- Email: `type="email"`, inline validation on blur for format.
- Password: show/hide toggle. Real-time strength indicator (5 rules with ✓/✗):
  - At least 8 characters | Uppercase letter | Lowercase letter | Number | Special character
  - "Create account" button disabled until all 5 pass.

Success flow: register → auto-login with same credentials → redirect to `/` (dashboard).
If auto-login fails: redirect to `/login` with message "Account created! Please log in."

Errors (inline below field):
- 409 DUPLICATE_USERNAME → inline below username: "Username is already taken"
- 409 DUPLICATE_EMAIL → inline below email: "Email is already registered"

---

## Screen 3: Dashboard (Project List)
**Route:** `/` | **Auth required:** Yes
**API:** `GET /api/v1/projects`

Layout: "Projects" page title. "New project" button top-right. Project card grid (3 cols desktop / 2 tablet / 1 mobile). Pagination at bottom (12 per page).

Project card:
- Project name (1-line ellipsis), description (truncated 120 chars, 2 lines max)
- Bottom row: owner avatar/initials + username, member count, task count
- Clickable → `/projects/{id}`. Hover lift-shadow effect.

Empty state: folder icon + "No projects yet" + "Create your first project" button.

New project modal (triggered by "New project"):
- Fields: Name (required), Description (optional, textarea 3 rows)
- Buttons: Cancel (secondary), Create (primary)
- On success: close modal → redirect to `/projects/{newId}`

---

## Screen 4: Project Board (Kanban)
**Route:** `/projects/{id}` | **Auth required:** Yes + must be project member
**API:** `GET /api/v1/projects/{id}`, `GET /api/v1/projects/{id}/tasks`, `PATCH /api/v1/tasks/{id}`

**Project header:**
- Project name (large heading), description (collapsed to 1 line, "Show more" toggle)
- Action row: "Board" toggle (active) | "List" toggle | filter/search controls | "Settings" icon → `/projects/{id}/settings`
- Member avatar row: up to 5 overlapping circles, "+N" if more, click opens member list

**Kanban board (4 columns):**
- Column headers: human-readable status name + task count badge. Top accent border per status color.
- Columns scroll vertically independently. Horizontal scroll on mobile.
- "Add task" button (+ icon) at bottom of TODO column only.

**Task card:**
```
┌──────────────────────────────┐
│ [priority badge]  [labels…]  │
│ Task title (max 2 lines)     │
│                              │
│ 👤 sam_dev  📅 Jun 25  💬 3  │
└──────────────────────────────┘
```
- Priority badge: colored pill. Labels: colored dots (max 5, no text). Deadline: red text if overdue (date < today).
- Card click → opens task detail panel (slide-over).

**Drag-and-drop:**
- Drag card to different column → optimistic update → `PATCH /api/v1/tasks/{id}` with new status.
- On API failure: animate card back, show error toast.
- Drop target column shows highlighted border while dragging.
- Cards sorted by `createdAt desc` within column (no reordering within column).

**Filters bar (between header and board):**
- Status: multi-select pills (all active by default)
- Priority: dropdown multi-select
- Assignee: single-select dropdown (member list + "Unassigned")
- Label: multi-select with colored items
- Search: text input, debounced 300ms
- "Clear filters" link (only when any filter active)
- All filter state synced to URL: `?status=TODO,IN_PROGRESS&priority=HIGH&assigneeId=2&labelId=1,3&search=login`

**Board/List toggle:** board is default. Preference stored in localStorage per project.

---

## Screen 5: Project List View (Table)
**Route:** `/projects/{id}?view=list` | **Auth required:** Yes + project member
**API:** same as board view

Same header and filters as board. Content area is a sortable table:
| Column | Sortable | Content |
|--------|----------|---------|
| Title | Yes | Clickable → opens detail panel |
| Status | Yes | Status badge pill |
| Priority | Yes | Priority badge pill |
| Assignee | No | Avatar/initials + username |
| Deadline | Yes | Date or "—". Red if overdue. |
| Labels | No | Colored dots |
| Comments | No | Count number |

Clicking sortable header sorts; click again toggles asc/desc. Default: createdAt desc.
Sort state synced to URL: `?sort=deadline,asc`. Paginated: 20 rows/page.

---

## Screen 6: Task Detail Panel (Slide-Over)
**Trigger:** click task card (board) or row (list)
**API:** `GET /api/v1/tasks/{id}`, `PATCH /api/v1/tasks/{id}`, `GET/POST /api/v1/tasks/{id}/comments`, `POST/DELETE /api/v1/tasks/{id}/labels/{id}`

Layout: right-side slide-over, 480px wide (full-screen mobile). Semi-transparent backdrop over board. Scrollable. Close: X button, Escape, or backdrop click.

**Panel structure (top → bottom):**

1. **Title** — inline-editable. Click to edit, blur/Enter to save. PATCH with `{ title }`.

2. **Meta row** — read-only: "Created by alex_lead · Jun 16, 2025 · Last updated 2h ago"

3. **Field grid** (2 cols desktop, stacked mobile):
   | Field | Control | Behavior |
   |-------|---------|----------|
   | Status | Dropdown | Saves immediately on change |
   | Priority | Dropdown | Saves immediately |
   | Assignee | Dropdown | Project members + "Unassigned". Shows avatar + username. Saves immediately. |
   | Deadline | Date picker | Clear button to remove. Shows red if overdue. Saves immediately. |

4. **Labels** — colored pills with names. "+" button → label picker dropdown (project labels with checkboxes). Toggle: attach (POST) or detach (DELETE). "+" hidden when 5 labels attached.

5. **Description** — rendered markdown. "Edit" button → textarea with markdown toolbar (bold, italic, bullets, code). "Save" + "Cancel". If empty: "Add a description…" clickable placeholder.

6. **Divider**

7. **Comments section:**
   - Header: "Comments (N)"
   - "Load earlier comments" button at top if >20 comments exist
   - Comment list (oldest first):
     - Author avatar + username + relative timestamp + "(edited)" if edited
     - Three-dot menu on hover (author sees Edit/Delete; project owner sees Delete on others' comments)
     - Edit mode: inline textarea + Save/Cancel
     - Delete: confirmation "Delete this comment?"
   - New comment: textarea at bottom + "Send" button. Ctrl+Enter to send. Disabled when blank.

8. **Delete task** — danger text button at bottom. Only visible to task creator or project owner. Confirmation dialog with task title.

**Auto-save:** text fields debounced 500ms; selects/date immediate. "Saving…" → "Saved" (2s) indicator near panel top.

---

## Screen 7: Project Settings
**Route:** `/projects/{id}/settings` | **Auth required:** Yes + project member
**API:** `GET /api/v1/projects/{id}`, `PATCH /api/v1/projects/{id}`, member endpoints, label endpoints

Layout: "← Back to board" link. "Project Settings" title. Three stacked sections:

**Section 1: General**
- Name: text input (editable by owner only, read-only for members)
- Description: textarea (editable by owner only)
- "Save changes" button (only shown when fields modified)
- "Delete project" danger button at bottom (owner only) — confirmation dialog requires typing project name

**Section 2: Members**
- Table: Username | Email | Role | Joined | Actions
- Actions (owner only): role dropdown (OWNER/MEMBER) + Remove button (not for owner themselves)
- Self row (non-owner): "Leave project" button
- Invite form at top (owner only): email input + "Invite" button. Errors inline below form.

**Section 3: Labels**
- List: colored dot + name + task count + edit + delete buttons
- Edit: inline (name + color picker). Delete: confirmation with task count.
- "Add label" form: name input + 12-color palette grid + "Create" button. Duplicate name error inline.

---

## Screen 8: Profile Page
**Route:** `/profile` | **Auth required:** Yes
**API:** `GET /api/v1/users/me`, `PATCH /api/v1/users/me`

Layout: "Profile" title. Large initials circle (first letter of username). Editable: Username, Email. "Save changes" button (disabled until a field changes). Inline errors for 409 duplicate username/email.

---

## Error & Special Screens

**403 Page:** "You don't have access to this project" + "Go to Dashboard" button.
**404 Page:** "Page not found" + "Go to Dashboard" button.
**Network Error (global):** toast "Connection lost. Retrying…" → auto-retry 3× exponential backoff → persistent banner if all fail.
