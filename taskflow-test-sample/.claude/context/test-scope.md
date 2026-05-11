# Test Scope — TaskFlow

## Feature Coverage by Priority

### P0 — Must Test (Auth, Project CRUD, Task CRUD, Kanban)

**Auth:**
- Register: success, duplicate username, duplicate email, validation errors
- Login: success, invalid credentials, account locked (after 5 failures in 15 min)
- Token refresh: success (rotation), expired refresh token → redirect to login
- Logout: success, idempotent (already logged out)

**Projects:**
- Create, read (list + detail), update, delete (with confirmName match + mismatch)
- Non-member accessing project → 403

**Tasks:**
- Create (all fields), read (detail + list), update (each field), delete (creator only, owner only, member denied)
- List filtering: by status, priority, assigneeId (including 0=unassigned), labelId (multi), search text, combined filters
- List sorting: by createdAt, deadline (nulls last), priority order (URGENT>HIGH>MEDIUM>LOW), title

**Kanban:**
- Drag-and-drop status change: success (optimistic update), API failure (rollback)

---

### P1 — Should Test (Members, Comments, Labels)

**Members:**
- Invite: success, user not found, already a member
- Remove member: success, tasks become unassigned
- Leave project: success (MEMBER), owner denied
- Change role: success, LAST_OWNER protection
- Owner cannot remove themselves (OWNER_CANNOT_LEAVE)

**Comments:**
- Add, list (pagination), edit (author only), delete (author + owner, other member denied)
- Empty comment rejected (frontend + backend)

**Labels:**
- Create, list, update, delete (cascade detaches from tasks)
- Duplicate name (case-insensitive) → 409
- Attach/detach from task; max 5 labels enforced
- Label from another project → 400

---

### P2 — Nice to Test

- Task list view (table mode) sorting + pagination
- Comment editing inline (no page reload)
- Member role management UI
- Filter state preserved in URL params

---

## Critical Edge Cases to Cover

| Area | Edge Case |
|------|-----------|
| Auth | Email case-insensitive (Alex@Example.com = alex@example.com) |
| Auth | Access token expires mid-form-submission → auto-refresh → transparent retry |
| Auth | Two browser tabs: logout in one invalidates the other on next API call |
| Projects | Delete confirmation name mismatch → 400 (dialog stays open) |
| Projects | Navigate to deleted project → 404 page |
| Tasks | Deadline in the past → allowed, shown as overdue (red) |
| Tasks | Drag to same column → no API call |
| Tasks | Clear description (empty string) → stored as NULL |
| Tasks | Two users edit same task → last write wins (no conflict detection) |
| Tasks | Task deleted by another user while panel open → toast + close panel |
| Labels | Delete label with tasks → confirmation shows count; cascade removes from all tasks |
| Labels | 3-char hex (#fff) → rejected (backend requires 6-char #RRGGBB) |
| Comments | 2000-char comment → accepted; 2001 → rejected |
| Comments | "Load earlier comments" → scroll position preserved |
| Members | Remove member → their task assignee_id set to NULL, tasks remain |
| Network | API failure during drag-drop → optimistic rollback + error toast |
| Network | Network loss → toast + exponential retry (queries only, not mutations) |

---

## Out of Scope

- Email/push notifications (out of scope for v1)
- File attachments
- WebSocket / real-time updates
- Password reset flow
- OAuth / social login
- Dark mode
- Internationalization

---

## Seed Data (Available for Tests)

All passwords: `Test1234!`

| Email | Username | Role |
|-------|----------|------|
| alex@example.com | alex_lead | Owner of "TaskFlow MVP" + "Marketing Site" |
| sam@example.com | sam_dev | Member of "TaskFlow MVP" |
| jordan@example.com | jordan_free | Member of both projects |
