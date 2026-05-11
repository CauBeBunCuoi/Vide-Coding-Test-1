# TaskFlow — Product Requirements Document

## 1. Product Vision

TaskFlow is a lightweight project management tool for small teams. Users can create projects, organize tasks on a Kanban board, collaborate through comments, and track progress with labels, priorities, and deadlines.

The product targets teams of 2–10 people who need structure without the overhead of enterprise tools like Jira.

---

## 2. User Personas

**Alex — Team Lead**
Creates projects, invites teammates, assigns work, monitors progress across all tasks. Needs a board-level overview and the ability to filter tasks by assignee or priority.

**Sam — Developer**
Receives task assignments, updates task status as work progresses, leaves comments to ask questions or share updates. Needs a clear view of personal assignments and deadlines.

**Jordan — Freelancer**
Works across multiple projects with different teams. Needs a dashboard that consolidates all projects and highlights upcoming deadlines.

---

## 3. Feature Requirements

### 3.1 Authentication

**US-AUTH-01: Registration**
As a new user, I want to register with a username, email, and password so I can access TaskFlow.

Acceptance criteria:
- User provides username, email, and password
- Username must be unique across the system
- Email must be unique and valid format
- Password must meet strength requirements (defined in security spec)
- After successful registration, user receives a confirmation message and can immediately log in
- If username or email is already taken, show a specific error indicating which field is duplicated
- All three fields are required — missing any field shows inline validation error before submission

**US-AUTH-02: Login**
As a registered user, I want to log in with my email and password to receive an access token.

Acceptance criteria:
- User provides email and password
- On success, system returns an access token (short-lived) and a refresh token (long-lived)
- Access token is stored in memory (never localStorage)
- Refresh token is stored in an httpOnly cookie
- On failure, show generic error "Invalid email or password" — do not reveal whether email exists
- After 5 consecutive failed attempts for the same email, lock the account for 15 minutes

**US-AUTH-03: Token Refresh**
As a logged-in user, I want my session to stay alive without re-entering credentials.

Acceptance criteria:
- When access token expires, frontend automatically calls refresh endpoint using the refresh token cookie
- If refresh succeeds, new access token is returned and used transparently
- If refresh fails (expired/revoked), redirect user to login page
- On refresh, the old refresh token is invalidated and a new one is issued (rotation)

**US-AUTH-04: Logout**
As a logged-in user, I want to log out to invalidate my session.

Acceptance criteria:
- Calling logout invalidates the current refresh token on the server
- Access token is cleared from memory on the frontend
- Refresh token cookie is cleared
- User is redirected to login page

---

### 3.2 Projects

**US-PROJ-01: Create Project**
As a logged-in user, I want to create a new project so I can start organizing tasks.

Acceptance criteria:
- User provides a project name (required) and optional description
- The creating user automatically becomes the project owner
- Owner is also added to the project_members table with role OWNER
- After creation, user is redirected to the empty project board
- Project name must be between 1 and 100 characters

**US-PROJ-02: View Project List**
As a logged-in user, I want to see all projects I belong to (as owner or member).

Acceptance criteria:
- Dashboard shows a grid of project cards
- Each card displays: project name, description (truncated to 120 chars), owner username, member count, total task count, and creation date
- Projects are sorted by most recently created first
- If the user has no projects, show an empty state with a "Create your first project" prompt
- Paginated — 12 projects per page

**US-PROJ-03: View Project Detail**
As a project member, I want to view a project's task board.

Acceptance criteria:
- Default view is Kanban board with 4 columns: TODO, IN_PROGRESS, IN_REVIEW, DONE
- Each column header shows the column name and task count
- Task cards in each column show: title, priority badge, assignee avatar/initials, deadline (if set), label dots, and comment count
- Board supports drag-and-drop to move tasks between columns (updates task status)
- Alternative list view available via toggle, showing tasks in a sortable table
- Only project members (including owner) can access this page
- Non-members who navigate to the URL see a 403 page

**US-PROJ-04: Update Project**
As the project owner, I want to edit my project's name and description.

Acceptance criteria:
- Only the project owner can update project details
- Members see project settings as read-only
- Changes are saved and reflected immediately on the project header

**US-PROJ-05: Delete Project**
As the project owner, I want to delete a project I no longer need.

Acceptance criteria:
- Only the project owner can delete the project
- Show a confirmation dialog: "Delete [project name]? This will permanently delete all tasks, comments, and labels. This action cannot be undone."
- User must type the project name to confirm deletion
- On confirmation, project and all associated data (members, tasks, labels, task_labels, comments) are deleted
- User is redirected to the dashboard

---

### 3.3 Project Members

**US-MEM-01: Invite Member**
As the project owner, I want to invite a user to my project by their email address.

Acceptance criteria:
- Owner enters an email address in the invite form
- If the email belongs to a registered user, they are added as a MEMBER
- If the email does not belong to any registered user, show error: "No account found with this email"
- If the user is already a member of the project, show error: "This user is already a member"
- New member appears in the member list immediately
- Default role for invited members is MEMBER

**US-MEM-02: View Members**
As a project member, I want to see who else is in the project.

Acceptance criteria:
- Member list shows: avatar/initials, username, email, role (OWNER or MEMBER), and join date
- Owner is always listed first
- Any project member can view the list
- Only the owner sees invite and remove controls

**US-MEM-03: Change Member Role**
As the project owner, I want to change a member's role.

Acceptance criteria:
- Owner can change a MEMBER to OWNER or vice versa
- There must always be at least one OWNER — if the current user is the only OWNER, they cannot change their own role to MEMBER
- Role change takes effect immediately

**US-MEM-04: Remove Member**
As the project owner, I want to remove a member from the project.

Acceptance criteria:
- Owner can remove any MEMBER from the project
- Owner cannot remove themselves (they must transfer ownership first or delete the project)
- When a member is removed, their task assignments within this project remain but the assignee displays as "Unassigned" — tasks are NOT deleted
- Confirmation dialog: "Remove [username] from [project name]?"

**US-MEM-05: Leave Project**
As a project member (non-owner), I want to leave a project I no longer participate in.

Acceptance criteria:
- Any member with role MEMBER can leave a project
- Owner cannot leave — they must transfer ownership or delete the project
- When a member leaves, their task assignments remain but assignee displays as "Unassigned"
- Confirmation dialog: "Leave [project name]? Your task assignments will be cleared."

---

### 3.4 Tasks

**US-TASK-01: Create Task**
As a project member, I want to create a new task in a project.

Acceptance criteria:
- User provides: title (required), description (optional), priority (default MEDIUM), deadline (optional), assignee (optional, must be a project member), labels (optional, from project's label pool)
- Task is created with status TODO
- created_by is set to the authenticated user
- Task appears in the TODO column of the Kanban board
- After creation, the task detail panel opens showing the new task

**US-TASK-02: View Task List**
As a project member, I want to filter and search tasks within a project.

Acceptance criteria:
- Filter by: status (multi-select), priority (multi-select), assignee (single select from member list + "Unassigned" option), label (multi-select from project labels)
- Text search: searches in task title and description
- Sort by: creation date (default, newest first), deadline (earliest first, null deadlines last), priority (urgent first), title (alphabetical)
- All filters can be combined
- Filter state is preserved in URL query parameters so it can be shared/bookmarked
- Paginated — 20 tasks per page in list view, all loaded in board view

**US-TASK-03: View Task Detail**
As a project member, I want to view full task details.

Acceptance criteria:
- Clicking a task card opens a slide-over panel from the right side (board stays visible underneath, dimmed)
- Panel shows all task fields: title, description (rendered as markdown), status, priority, deadline, assignee, created by, labels, creation date, last updated date
- Below the fields, show the comments section (see US-CMT requirements)
- Panel has a close button (X) and also closes on backdrop click or Escape key

**US-TASK-04: Update Task**
As a project member, I want to update task details.

Acceptance criteria:
- Any project member can update any task within the project
- Editable fields: title, description, status, priority, deadline, assignee
- Status change via drag-and-drop on the board or dropdown in the detail panel
- Assigning a user who is not a project member returns an error
- Setting a deadline in the past is allowed (for tracking overdue items) but shows a visual warning
- Changes save automatically (debounced, 500ms after last keystroke for text fields; immediate for select/date fields)

**US-TASK-05: Delete Task**
As the task creator or project owner, I want to delete a task.

Acceptance criteria:
- Only the task creator or project owner can delete a task
- Other members do not see the delete option
- Confirmation dialog: "Delete task [title]? This will also delete all comments on this task."
- On deletion, task is removed from the board and all associated comments and label associations are deleted

**US-TASK-06: Drag-and-Drop Status Change**
As a project member, I want to drag tasks between Kanban columns to update their status.

Acceptance criteria:
- Drag a task card from one column to another
- Status updates immediately on drop (optimistic update on frontend)
- If the API call fails, revert the card to its original column and show an error toast
- Drag handle is the entire card surface
- Tasks within a column maintain their order (sorted by creation date, newest at top)

---

### 3.5 Labels

**US-LBL-01: Create Label**
As a project member, I want to create labels to categorize tasks.

Acceptance criteria:
- User provides a label name and selects a color
- Color is chosen from a predefined palette of 12 colors (see UI spec for palette)
- Label names must be unique within a project — duplicate names show error "A label with this name already exists"
- Label appears in the project's label pool immediately

**US-LBL-02: View Labels**
As a project member, I want to see all labels available in a project.

Acceptance criteria:
- Labels are displayed in the project settings page under a "Labels" section
- Each label shows: colored dot, name, and count of tasks using it
- Labels with zero tasks show a delete option

**US-LBL-03: Update Label**
As a project member, I want to rename or recolor a label.

Acceptance criteria:
- Any project member can edit any label within the project
- Changes propagate to all tasks that use the label

**US-LBL-04: Delete Label**
As a project member, I want to delete a label that is no longer needed.

Acceptance criteria:
- Deleting a label removes it from all tasks that have it attached
- Confirmation dialog: "Delete label [name]? It will be removed from [N] tasks."
- No confirmation needed if the label has 0 tasks

**US-LBL-05: Attach/Detach Labels on Tasks**
As a project member, I want to tag tasks with labels.

Acceptance criteria:
- In the task detail panel, show a label picker dropdown
- Dropdown shows all project labels, with already-attached labels checked
- Clicking a label toggles it on/off
- A task can have up to 5 labels
- Labels appear as colored pills on the task card (on the board) and in the detail panel

---

### 3.6 Comments

**US-CMT-01: Add Comment**
As a project member, I want to comment on a task to discuss it with my team.

Acceptance criteria:
- Comment input is a text area at the bottom of the task detail panel
- User types content and presses a "Send" button or Ctrl+Enter
- Comment appears at the bottom of the comment list immediately (optimistic update)
- Comment shows: author username, avatar/initials, content, and relative timestamp ("2 minutes ago")
- Empty comments are not allowed — disable the send button when input is empty

**US-CMT-02: View Comments**
As a project member, I want to see all comments on a task.

Acceptance criteria:
- Comments are listed in chronological order (oldest first) inside the task detail panel
- Paginated: load 20 comments initially, "Load earlier comments" button at the top if more exist
- Each comment shows: author avatar/initials, author username, content, relative timestamp
- If a comment has been edited, show "(edited)" next to the timestamp

**US-CMT-03: Edit Comment**
As the comment author, I want to edit my own comment.

Acceptance criteria:
- Only the author of a comment can edit it
- Three-dot menu on the comment shows "Edit" and "Delete" options for the author
- Editing replaces the comment text area inline — not a modal
- Show "Save" and "Cancel" buttons during editing
- After saving, the comment shows "(edited)" indicator

**US-CMT-04: Delete Comment**
As the comment author or project owner, I want to delete a comment.

Acceptance criteria:
- Comment author can delete their own comments
- Project owner can delete any comment in their project
- Confirmation dialog: "Delete this comment?"
- Comment is removed from the list immediately

---

## 4. Feature Priority

| Priority | Features |
|----------|----------|
| P0 — Must have | Auth (register, login, refresh, logout), Project CRUD, Task CRUD with status/priority, Kanban board view |
| P1 — Should have | Project members (invite, remove), Task assignment, Comments (add, list), Labels (CRUD, attach/detach) |
| P2 — Nice to have | Drag-and-drop on board, Task list view with filters/sort, Comment editing, Member role management, Leave project |
| P3 — Future | Notifications, Activity log, File attachments, Due date reminders, Task dependencies |

---

## 5. Out of Scope (for this release)

- Email notifications or any push notification system
- Real-time updates via WebSocket (polling or manual refresh is acceptable)
- File attachments on tasks or comments
- Task dependencies or subtasks
- Time tracking
- Multiple workspaces or organizations
- User avatar upload (use initial-based avatars)
- Password reset via email
- OAuth / social login
- Mobile-specific native features (responsive web is sufficient)
- Internationalization (English only)
- Dark mode (light mode only for v1)
