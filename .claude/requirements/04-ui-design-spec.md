# TaskFlow — UI Design & Screen Specification

## Design Principles

1. **Clarity over density** — Every screen has one primary action. Secondary actions are available but visually subdued.
2. **Immediate feedback** — All mutations use optimistic updates with error rollback. Loading states, error states, and empty states are designed for every data-fetching view.
3. **Keyboard accessible** — All interactive elements are reachable via Tab. Modals and panels trap focus. Escape closes overlays.

---

## Color Palette

All colors use CSS custom properties for future theming.

| Token | Hex | Usage |
|-------|-----|-------|
| --color-primary | #2563EB | Buttons, links, active states |
| --color-primary-hover | #1D4ED8 | Button hover |
| --color-danger | #DC2626 | Delete buttons, error text, overdue badges |
| --color-danger-hover | #B91C1C | Danger button hover |
| --color-success | #16A34A | Success toasts, DONE column accent |
| --color-warning | #D97706 | Overdue deadline warning, IN_REVIEW column accent |
| --color-gray-50 | #F9FAFB | Page background |
| --color-gray-100 | #F3F4F6 | Card background, input background |
| --color-gray-200 | #E5E7EB | Borders, dividers |
| --color-gray-500 | #6B7280 | Secondary text, placeholders |
| --color-gray-700 | #374151 | Primary text body |
| --color-gray-900 | #111827 | Headings |

**Priority badge colors:**

| Priority | Background | Text |
|----------|-----------|------|
| URGENT | #FEE2E2 | #991B1B |
| HIGH | #FEF3C7 | #92400E |
| MEDIUM | #DBEAFE | #1E40AF |
| LOW | #F3F4F6 | #374151 |

**Status column accent colors** (thin top border on each Kanban column):

| Status | Color |
|--------|-------|
| TODO | #6B7280 |
| IN_PROGRESS | #2563EB |
| IN_REVIEW | #D97706 |
| DONE | #16A34A |

**Label color palette** (12 predefined options for label creation):

```
#EF4444  (red)
#F97316  (orange)
#F59E0B  (amber)
#84CC16  (lime)
#10B981  (emerald)
#06B6D4  (cyan)
#3B82F6  (blue)
#6366F1  (indigo)
#8B5CF6  (violet)
#EC4899  (pink)
#78716C  (stone)
#1F2937  (dark gray)
```

---

## Typography

Font stack: `Inter, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif`

| Element | Size | Weight | Line Height |
|---------|------|--------|-------------|
| Page title | 24px | 700 | 32px |
| Section heading | 18px | 600 | 28px |
| Card title | 14px | 500 | 20px |
| Body text | 14px | 400 | 20px |
| Small/meta text | 12px | 400 | 16px |
| Button text | 14px | 500 | 20px |
| Input text | 14px | 400 | 20px |
| Input label | 12px | 500 | 16px |

---

## Layout Structure

**App shell:**
```
┌─────────────────────────────────────────────────┐
│  Top Navbar (56px height, fixed)                │
├─────────────────────────────────────────────────┤
│                                                 │
│  Page Content (scrollable)                      │
│                                                 │
│                                                 │
└─────────────────────────────────────────────────┘
```

**Top Navbar contents:**
- Left: TaskFlow logo/wordmark (links to dashboard)
- Right: User avatar/initials circle → dropdown menu with "Profile" and "Logout"
- On auth pages (login, register): navbar shows only the logo, no user menu

**Page content area:**
- Max width: 1280px, centered horizontally
- Horizontal padding: 24px (desktop), 16px (mobile)
- Vertical padding: 24px top

---

## Screen Specifications

### Screen 1: Login Page

**Route:** `/login`
**Auth required:** No (redirect to `/` if already authenticated)
**API calls:** `POST /api/v1/auth/login`

Layout:
- Centered card (max-width 400px) on a neutral background
- TaskFlow logo at the top of the card
- Two input fields: Email, Password
- "Log in" primary button (full width)
- Below button: "Don't have an account? Register" link → navigates to `/register`
- Error message appears above the button as a red alert box

Field behaviors:
- Email: type="email", autocomplete="email"
- Password: type="password", autocomplete="current-password", show/hide toggle icon
- Submit on Enter key from either field
- Button shows loading spinner during API call, disabled to prevent double-submit

Error display:
- Invalid credentials: red alert box with text "Invalid email or password"
- Account locked: red alert box with text "Account locked. Try again in X minutes." (calculate remaining minutes from `lockedUntil` in the response)
- Network error: red alert box with text "Unable to connect. Please check your connection and try again."

---

### Screen 2: Register Page

**Route:** `/register`
**Auth required:** No (redirect to `/` if already authenticated)
**API calls:** `POST /api/v1/auth/register`, then `POST /api/v1/auth/login`

Layout:
- Same centered card layout as login
- Three input fields: Username, Email, Password
- Password strength indicator below the password field
- "Create account" primary button (full width)
- Below button: "Already have an account? Log in" link → navigates to `/login`

Field behaviors:
- Username: autocomplete="username". Inline validation on blur: "Username must be 3–50 characters, letters, numbers, and underscores only, starting with a letter."
- Email: type="email", autocomplete="email". Inline validation on blur for format.
- Password: type="password", autocomplete="new-password", show/hide toggle.
  - Strength indicator shows which requirements are met/unmet in real-time as user types:
    - ✓/✗ At least 8 characters
    - ✓/✗ Uppercase letter
    - ✓/✗ Lowercase letter
    - ✓/✗ Number
    - ✓/✗ Special character
  - Button is disabled until all requirements are met.

Success flow:
- On successful registration, automatically call the login endpoint with the same credentials
- On successful login, redirect to `/` (dashboard)
- If auto-login fails for any reason, redirect to `/login` with a success message: "Account created! Please log in."

Error display:
- Duplicate username: inline error below username field: "Username is already taken"
- Duplicate email: inline error below email field: "Email is already registered"
- These come from 409 responses — map `DUPLICATE_USERNAME` and `DUPLICATE_EMAIL` error codes to the correct field

---

### Screen 3: Dashboard (Project List)

**Route:** `/` (root)
**Auth required:** Yes
**API calls:** `GET /api/v1/projects`

Layout:
- Page title: "Projects"
- "New project" primary button in the top-right of the title row
- Grid of project cards: 3 columns on desktop, 2 on tablet, 1 on mobile
- Pagination at the bottom (12 per page)

Project card contents:
- Project name (truncated with ellipsis after 1 line)
- Description (truncated to 120 chars, gray text, max 2 lines)
- Bottom row: owner avatar/initials + username, member count icon + number, task count icon + number
- Card is clickable — navigates to `/projects/{id}`
- Subtle hover effect (lift shadow)

Empty state (no projects):
- Illustration or icon (folder with plus sign)
- "No projects yet"
- "Create your first project to get started"
- "Create project" button

New project creation:
- Clicking "New project" opens a modal dialog
- Fields: Name (required, text input), Description (optional, text area, 3 rows)
- Buttons: "Cancel" (secondary), "Create" (primary)
- On success: modal closes, user is redirected to `/projects/{newId}` (the new project's board)

---

### Screen 4: Project Board (Kanban)

**Route:** `/projects/{id}`
**Auth required:** Yes + must be project member
**API calls:** `GET /api/v1/projects/{id}`, `GET /api/v1/projects/{id}/tasks`, `PATCH /api/v1/tasks/{id}` (drag-and-drop)

This is the main working screen. It has a project header area and the task board below.

**Project header:**
- Project name (large heading)
- Project description (gray, below the name, collapsed to 1 line with "Show more" toggle if longer)
- Action row: "Board" toggle (active), "List" toggle, filter/search controls, "Settings" icon button → navigates to `/projects/{id}/settings`
- Member avatar row: show up to 5 member avatars/initials as overlapping circles, "+N" if more, clicking opens member list

**Kanban board:**
- 4 columns: TODO, IN_PROGRESS, IN_REVIEW, DONE
- Each column has a header: status name (human-readable: "To Do", "In Progress", "In Review", "Done") + task count badge
- Columns scroll vertically independently if content overflows
- Horizontal scroll on the board container if viewport is narrow (mobile)
- "Add task" button (+ icon) at the bottom of the TODO column only

**Task card (within a column):**
```
┌──────────────────────────────┐
│ [priority badge]  [labels…]  │
│ Task title text here         │
│                              │
│ 👤 sam_dev  📅 Jun 25  💬 3  │
└──────────────────────────────┘
```

- Priority badge: small colored pill with text (URGENT, HIGH, MEDIUM, LOW)
- Labels: colored dots (no text) — max 5 dots, in the top-right area
- Title: max 2 lines, truncated with ellipsis
- Bottom row: assignee initials (or empty if unassigned), deadline date (or empty), comment count (or hidden if 0)
- Deadline is red text if the date is in the past (overdue)
- Card click opens task detail panel (see Screen 6)

**Drag-and-drop:**
- User can drag any card from one column to another
- On drop: immediately move the card visually (optimistic update), then call `PATCH /api/v1/tasks/{id}` with the new status
- If PATCH fails: animate the card back to its original column, show error toast
- While dragging: drop target column shows a highlighted border
- Cards within a column are sorted by `createdAt desc` (newest at top) — reordering within a column is not supported

**Filters bar** (between project header and board):
- Status filter: multi-select pills (all 4 statuses, all active by default)
- Priority filter: dropdown multi-select
- Assignee filter: dropdown single-select with member list + "Unassigned" option
- Label filter: dropdown multi-select with colored label items
- Search: text input with search icon, debounced 300ms
- "Clear filters" text button (appears only when any filter is active)
- Active filters update URL query parameters: `?status=TODO,IN_PROGRESS&priority=HIGH&assigneeId=2&labelId=1,3&search=login`

**Board view vs. List view toggle:**
- Toggle between board (default) and list view
- Preference is stored in localStorage per project
- List view is described below (Screen 5)

---

### Screen 5: Project List View (Table)

**Route:** `/projects/{id}?view=list`
**Auth required:** Yes + must be project member
**API calls:** Same as board view

Same project header and filters as board view, but content area is a table:

| Column | Width | Sortable | Content |
|--------|-------|----------|---------|
| Title | flex | Yes | Task title (clickable → opens detail panel) |
| Status | 120px | Yes | Status badge pill |
| Priority | 100px | Yes | Priority badge pill |
| Assignee | 140px | No | Avatar/initials + username |
| Deadline | 100px | Yes | Date or "—" if none. Red if overdue. |
| Labels | 120px | No | Colored dots |
| Comments | 60px | No | Count number |

- Clicking a sortable column header sorts by that column. Click again to toggle asc/desc. Default: createdAt desc.
- Sort state synced with URL params: `?sort=deadline,asc`
- Paginated: 20 rows per page, pagination controls at bottom
- Row click opens task detail panel (same as card click on board)

---

### Screen 6: Task Detail Panel (Slide-Over)

**Trigger:** Click a task card (board) or task row (list)
**API calls:** `GET /api/v1/tasks/{id}`, `PATCH /api/v1/tasks/{id}`, `GET /api/v1/tasks/{id}/comments`, `POST /api/v1/tasks/{id}/comments`, `POST /api/v1/tasks/{id}/labels`, `DELETE /api/v1/tasks/{id}/labels/{id}`

Layout:
- Slide-over panel from the right side, 480px wide (full-screen on mobile)
- Backdrop: semi-transparent dark overlay on the board/list
- Close: X button in top-right, Escape key, or backdrop click
- Panel is scrollable vertically

**Panel structure (top to bottom):**

1. **Title** — inline-editable text field. Click to edit, blur or Enter to save. `PATCH` with `{ title }`.

2. **Meta row** — read-only: "Created by alex_lead · Jun 16, 2025 · Last updated 2h ago"

3. **Field grid** (2 columns on desktop, stacked on mobile):

   | Field | Control type | Behavior |
   |-------|-------------|----------|
   | Status | Dropdown select | Options: To Do, In Progress, In Review, Done. Saves immediately on change. |
   | Priority | Dropdown select | Options: Low, Medium, High, Urgent. Saves immediately. |
   | Assignee | Dropdown select | Options: project members + "Unassigned". Shows avatar + username. Saves immediately. |
   | Deadline | Date picker | Calendar popup. Clear button to remove deadline. Saves immediately. Shows red text if overdue. |

4. **Labels** — horizontal row of colored pills with label name. "+" button opens a label picker dropdown showing all project labels with checkboxes. Toggling a label calls `POST` (attach) or `DELETE` (detach). Max 5 labels — "+" button is hidden when 5 are attached.

5. **Description** — markdown text area. Click "Edit" to switch to edit mode (textarea with markdown toolbar: bold, italic, bullet list, code). "Save" and "Cancel" buttons in edit mode. Rendered description uses basic markdown rendering (bold, italic, lists, code blocks, links). If empty, show placeholder: "Add a description…" as a clickable area.

6. **Divider line**

7. **Comments section:**
   - Header: "Comments (N)"
   - If more than 20 comments, "Load earlier comments" button at the top of the list
   - Comment list (chronological, oldest first):
     ```
     ┌──────────────────────────────────────┐
     │ 👤 sam_dev           2 hours ago      │
     │ Comment content text here.            │
     │                          (edited)     │
     │                          ⋮ (menu)     │
     └──────────────────────────────────────┘
     ```
   - Three-dot menu (⋮) appears on hover, only for author's own comments (or project owner):
     - Author sees: "Edit", "Delete"
     - Project owner sees: "Delete" (on other people's comments)
   - Edit mode: replaces comment text with a textarea + "Save" / "Cancel" buttons
   - Delete: confirmation dialog "Delete this comment?"
   - New comment input at the bottom: textarea + "Send" button. Ctrl+Enter to send. Button disabled when textarea is empty or whitespace-only.

8. **Delete task** — at the very bottom: "Delete task" danger text button. Only visible if the user is the task creator or a project owner. Confirmation dialog with task title.

**Auto-save behavior:**
- Text fields (title, description): save on blur, debounced 500ms while typing
- Select/date fields: save immediately on change
- All saves show a brief "Saving…" indicator near the top of the panel, then "Saved" for 2 seconds

---

### Screen 7: Project Settings

**Route:** `/projects/{id}/settings`
**Auth required:** Yes + must be project member
**API calls:** `GET /api/v1/projects/{id}`, `PATCH /api/v1/projects/{id}`, `GET /api/v1/projects/{id}/members`, `POST/PATCH/DELETE member endpoints`, `GET /api/v1/projects/{id}/labels`, `POST/PATCH/DELETE label endpoints`

Layout:
- Back link: "← Back to board" → navigates to `/projects/{id}`
- Page title: "Project Settings"
- Three sections stacked vertically:

**Section 1: General**
- Name: text input (editable by owner only, read-only for members)
- Description: text area (editable by owner only)
- "Save changes" button (only shown if fields were modified)
- "Delete project" danger button at the bottom (owner only). Confirmation dialog requires typing the project name.

**Section 2: Members**
- Table/list of members:
  | Username | Email | Role | Joined | Actions |
  |----------|-------|------|--------|---------|
  - Actions column (visible to owner only):
    - Role dropdown: OWNER / MEMBER
    - Remove button (not shown for the owner themselves)
  - For non-owner viewing: no actions column, just the list
  - Self row: shows "Leave project" button (only for MEMBER role)
- Invite form at the top (owner only): email text input + "Invite" button
- Error messages appear inline below the invite form

**Section 3: Labels**
- Grid or list of labels:
  - Each label: colored dot + name + task count + edit button + delete button
  - Edit: inline editing of name and color picker
  - Delete: confirmation dialog showing task count "Delete label 'frontend'? It will be removed from 4 tasks."
- "Add label" form at the top: name input + color picker (12-color palette grid) + "Create" button
- Error for duplicate name shown inline

---

### Screen 8: Profile Page

**Route:** `/profile`
**Auth required:** Yes
**API calls:** `GET /api/v1/users/me`, `PATCH /api/v1/users/me`

Layout:
- Page title: "Profile"
- Avatar: large initials circle (first letter of username, uppercase)
- Editable fields: Username, Email
- "Save changes" button (disabled until a field is changed)
- Error display: inline per field (duplicate username/email from 409 responses)

---

### Error and Special State Screens

**403 Forbidden Page:**
- Route: any project route where user is not a member
- Message: "You don't have access to this project"
- "Go to Dashboard" button

**404 Not Found Page:**
- Route: any non-existent route
- Message: "Page not found"
- "Go to Dashboard" button

**Network Error State (global):**
- If any API call fails due to network issues, show a toast notification: "Connection lost. Retrying…"
- Auto-retry with exponential backoff (1s, 2s, 4s), max 3 retries
- After 3 failures, show persistent banner at top of page: "Unable to connect to the server. Please check your connection."

---

## Responsive Breakpoints

| Breakpoint | Width | Layout changes |
|------------|-------|----------------|
| Desktop | ≥1024px | Full layout as described above |
| Tablet | 768–1023px | Project grid: 2 columns. Board columns: horizontally scrollable. Task detail panel: 400px wide. |
| Mobile | <768px | Project grid: 1 column. Board: horizontal scroll, one column visible at a time. Task detail panel: full-screen overlay. Filters collapse into a "Filters" button that opens a dropdown. Settings sections stack vertically. |

Navbar on mobile:
- Logo left, hamburger menu right (or just avatar menu right — no sidebar, so hamburger may not be needed)
- User menu: tap avatar → dropdown with "Profile" and "Logout"

---

## Navigation Map

```
/login ─────────────────────── Login page
/register ──────────────────── Register page

/ ──────────────────────────── Dashboard (project list)
  └── [click project card]
      │
      ├── /projects/{id} ──── Project board (Kanban)
      │   ├── [click task] ── Task detail panel (slide-over, not a separate route)
      │   ├── ?view=list ──── Project list view
      │   └── [click Settings]
      │       │
      │       └── /projects/{id}/settings ── Project settings
      │
/profile ───────────────────── User profile

[All routes under / require authentication — unauthenticated users are redirected to /login]
[After login, user is redirected to / (dashboard)]
[The task detail panel is an overlay, not a route change — the URL stays on /projects/{id}]
```

---

## Component Inventory

These are the reusable components that appear across multiple screens:

| Component | Used in | Description |
|-----------|---------|-------------|
| Button | everywhere | Variants: primary, secondary, danger, ghost. Sizes: sm, md. Loading state with spinner. |
| Input | forms | Text input with label, placeholder, error message slot, optional icon. |
| TextArea | task description, comments, project description | Auto-growing textarea with character counter. |
| Select | task fields, filters | Dropdown with single-select and multi-select variants. Supports search within options. |
| DatePicker | task deadline | Calendar popup triggered by clicking the input. Clear button. |
| Modal | project create, confirmations | Centered overlay with title, body, action buttons. Focus-trapped. Closes on Escape and backdrop click. |
| SlideOver | task detail | Right-side panel, 480px. Focus-trapped. Closes on Escape and backdrop. |
| Toast | success/error feedback | Auto-dismiss after 5 seconds. Types: success (green), error (red), info (blue). Stacks if multiple. Position: bottom-right. |
| Avatar | member displays | Circular initial-based avatar. Sizes: sm (24px), md (32px), lg (48px). Background color derived from username hash. |
| Badge | priority, status | Small pill with background color + text. |
| LabelPill | task labels | Colored pill with label name text. Removable variant (with X button). |
| EmptyState | dashboard, board, comments | Icon/illustration + message + optional CTA button. |
| Pagination | project list, task list, comments | Page number buttons + previous/next arrows. Shows current page and total. |
| ConfirmDialog | delete actions | Modal asking "Are you sure?" with cancel and confirm (danger) buttons. |
| Skeleton | loading states | Animated placeholder blocks matching the shape of cards, rows, or text. |
