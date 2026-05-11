# TaskFlow — Error Handling & Edge Cases

## 1. API Error Response Contract

Every error response from the backend uses this exact shape:

```json
{
  "error": "ERROR_CODE",
  "message": "Human-readable description suitable for display in the UI",
  "details": { ... }
}
```

Rules:
- `error` is always a SCREAMING_SNAKE_CASE string that the frontend uses for programmatic handling (switch statements, field mapping)
- `message` is always a user-friendly sentence that the frontend can display directly in a toast or alert
- `details` is optional and only present for validation errors or when additional context is needed

---

## 2. Error Code Catalog

Every error code the API can return, grouped by domain. The frontend should handle all of these.

### 2.1 Auth Errors

| Error Code | HTTP Status | Message | Frontend Behavior |
|------------|-------------|---------|-------------------|
| VALIDATION_FAILED | 400 | One or more fields are invalid | Show inline field errors from `details.fieldErrors` |
| INVALID_CREDENTIALS | 401 | Invalid email or password | Show in alert box above login button |
| INVALID_REFRESH_TOKEN | 401 | Refresh token is invalid or expired | Clear auth state, redirect to `/login` |
| DUPLICATE_USERNAME | 409 | Username is already taken | Show inline error below username field |
| DUPLICATE_EMAIL | 409 | Email is already registered | Show inline error below email field |
| ACCOUNT_LOCKED | 423 | Account locked due to too many failed attempts. Try again in 15 minutes. | Show in alert box with remaining time from `lockedUntil` |
| RATE_LIMITED | 429 | Too many requests. Please try again later. | Show toast, disable the submit button for `Retry-After` seconds |

### 2.2 Project Errors

| Error Code | HTTP Status | Message | Frontend Behavior |
|------------|-------------|---------|-------------------|
| PROJECT_NOT_FOUND | 404 | Project not found | Navigate to 404 page |
| NOT_PROJECT_MEMBER | 403 | You are not a member of this project | Navigate to 403 page |
| NOT_PROJECT_OWNER | 403 | Only the project owner can perform this action | Show toast error |
| CONFIRMATION_MISMATCH | 400 | Project name does not match | Show inline error in delete confirmation dialog |

### 2.3 Member Errors

| Error Code | HTTP Status | Message | Frontend Behavior |
|------------|-------------|---------|-------------------|
| USER_NOT_FOUND | 404 | No account found with this email | Show inline error below invite email field |
| ALREADY_MEMBER | 409 | This user is already a member of this project | Show inline error below invite email field |
| LAST_OWNER | 400 | Cannot change role — this is the only owner of the project | Show toast error |
| OWNER_CANNOT_LEAVE | 400 | Project owner cannot leave. Transfer ownership or delete the project. | Show toast error |

### 2.4 Task Errors

| Error Code | HTTP Status | Message | Frontend Behavior |
|------------|-------------|---------|-------------------|
| TASK_NOT_FOUND | 404 | Task not found | Close detail panel, show toast, refresh task list |
| NOT_TASK_OWNER | 403 | Only the task creator or project owner can delete this task | Show toast error |
| ASSIGNEE_NOT_MEMBER | 400 | Assignee is not a member of this project | Show inline error on assignee field in detail panel |
| LABEL_NOT_IN_PROJECT | 400 | One or more labels do not belong to this project | Show toast error (this should not happen in normal UI flow) |
| TOO_MANY_LABELS | 400 | A task can have at most 5 labels | Hide the "+" label button and show toast if triggered via race condition |

### 2.5 Label Errors

| Error Code | HTTP Status | Message | Frontend Behavior |
|------------|-------------|---------|-------------------|
| DUPLICATE_LABEL_NAME | 409 | A label with this name already exists in this project | Show inline error below label name field |
| LABEL_ALREADY_ATTACHED | 409 | This label is already attached to the task | Ignore silently (UI should prevent this, but handle the race condition gracefully) |

### 2.6 Comment Errors

| Error Code | HTTP Status | Message | Frontend Behavior |
|------------|-------------|---------|-------------------|
| NOT_COMMENT_AUTHOR | 403 | Only the comment author can edit this comment | Show toast error. This should never happen from the UI since the edit button is only shown to the author. |
| NOT_AUTHORIZED | 403 | Only the comment author or project owner can delete this comment | Show toast error |

### 2.7 Global Errors

| Error Code | HTTP Status | Message | Frontend Behavior |
|------------|-------------|---------|-------------------|
| UNAUTHORIZED | 401 | Authentication required | Attempt token refresh. If that fails, redirect to `/login`. |
| INTERNAL_ERROR | 500 | An unexpected error occurred. Please try again. | Show toast error with the message. Log the error for debugging. |

---

## 3. Backend Error Handling Implementation

### 3.1 Custom Exception Classes

```
BusinessRuleException        → 400 (LAST_OWNER, ASSIGNEE_NOT_MEMBER, TOO_MANY_LABELS, etc.)
ResourceNotFoundException   → 404 (TASK_NOT_FOUND, PROJECT_NOT_FOUND, USER_NOT_FOUND)
DuplicateResourceException  → 409 (DUPLICATE_USERNAME, DUPLICATE_EMAIL, DUPLICATE_LABEL_NAME, etc.)
AccessDeniedException       → 403 (NOT_PROJECT_MEMBER, NOT_PROJECT_OWNER, NOT_TASK_OWNER, etc.)
AccountLockedException      → 423 (ACCOUNT_LOCKED — includes lockedUntil field)
```

Each exception carries the `error` code and `message` as constructor parameters. The `GlobalExceptionHandler` catches them and returns the `ErrorResponse` DTO.

### 3.2 GlobalExceptionHandler Structure

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Custom business exceptions → mapped to correct HTTP status
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(BusinessRuleException ex) { ... }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) { ... }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateResourceException ex) { ... }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) { ... }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ErrorResponse> handleLocked(AccountLockedException ex) { ... }

    // Jakarta Validation errors → 400 with field details
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> new FieldError(fe.getField(), fe.getDefaultMessage()))
            .toList();
        return ResponseEntity.badRequest().body(
            new ErrorResponse("VALIDATION_FAILED", "One or more fields are invalid",
                Map.of("fieldErrors", fieldErrors))
        );
    }

    // Catch-all for unexpected errors → 500
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(500).body(
            new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred. Please try again.", null)
        );
    }
}
```

The catch-all handler logs the full stack trace server-side but never exposes it in the response.

---

## 4. Frontend Error Handling Strategy

### 4.1 Axios Error Interceptor

The Axios response interceptor categorizes errors before they reach component code:

```typescript
axiosInstance.interceptors.response.use(
  (response) => response,
  async (error) => {
    const status = error.response?.status;
    const data = error.response?.data;

    // 401: attempt token refresh (unless already refreshing)
    if (status === 401 && !error.config._retry) {
      // ... refresh logic (see security doc)
    }

    // Network error (no response at all)
    if (!error.response) {
      toast.error('Unable to connect. Please check your connection and try again.');
    }

    // Re-throw so individual queries/mutations can handle specific errors
    return Promise.reject(error);
  }
);
```

### 4.2 Error Handling in TanStack Query Mutations

Each mutation's `onError` callback handles errors specific to that operation:

```typescript
const updateTask = useMutation({
  mutationFn: (data) => api.updateTask(taskId, data),
  onError: (error) => {
    const apiError = error.response?.data;

    switch (apiError?.error) {
      case 'ASSIGNEE_NOT_MEMBER':
        // Show inline error on assignee field
        setFieldError('assignee', apiError.message);
        break;
      case 'TASK_NOT_FOUND':
        // Task was deleted by someone else
        toast.error('This task has been deleted.');
        closePanel();
        queryClient.invalidateQueries(['tasks']);
        break;
      case 'NOT_PROJECT_MEMBER':
        // User was removed from the project while viewing
        toast.error('You are no longer a member of this project.');
        navigate('/');
        break;
      default:
        toast.error(apiError?.message || 'Something went wrong.');
    }
  },
  // Optimistic update rollback
  onError: (err, variables, context) => {
    if (context?.previousData) {
      queryClient.setQueryData(['tasks', projectId], context.previousData);
    }
  },
});
```

### 4.3 Error Handling in TanStack Query Queries

For data fetching, errors are handled at the component level:

```typescript
const { data, error, isError } = useQuery({
  queryKey: ['project', projectId],
  queryFn: () => fetchProject(projectId),
  retry: (failureCount, error) => {
    // Don't retry 403 or 404
    const status = error.response?.status;
    if (status === 403 || status === 404) return false;
    return failureCount < 3;
  },
});

if (isError) {
  const status = error.response?.status;
  if (status === 404) return <NotFoundPage />;
  if (status === 403) return <ForbiddenPage />;
  return <ErrorState message="Failed to load project." onRetry={refetch} />;
}
```

---

## 5. UI Error States

Every data-fetching view has three states beyond the success state:

### 5.1 Loading State (Skeleton)

Display placeholder shapes matching the content layout:
- Dashboard: 6 project card skeletons (gray rounded rectangles pulsing)
- Board: 4 column headers with 2-3 task card skeletons each
- Task detail panel: field placeholders (label-sized rectangles)
- Comments: 3 comment bubble skeletons

Skeletons use CSS `animate-pulse` with `bg-gray-200` on a `bg-gray-50` background.

### 5.2 Empty State

When the query succeeds but returns zero items:

| Context | Illustration | Message | CTA |
|---------|-------------|---------|-----|
| Dashboard (no projects) | Folder icon | "No projects yet" | "Create your first project" button |
| Board column (no tasks in status) | — | — | No explicit empty state — just an empty column with "Add task" at the bottom of TODO |
| Board (all columns empty, project has 0 tasks) | Clipboard icon | "No tasks yet" | "Create your first task" button in the TODO column |
| Task detail: no comments | Chat bubble icon | "No comments yet. Start the conversation." | Focus the comment input |
| Task detail: no labels | — | — | Show the "+" label button, no separate empty state |
| Settings: no additional members | Person icon | "No other members yet" | Show invite form (owner) or "Only you and the owner are here" (member) |
| Settings: no labels | Tag icon | "No labels yet" | Show add label form |
| Search/filter with no results | Search icon | "No tasks match your filters" | "Clear filters" link |

### 5.3 Error State

When the query fails:

| Context | Message | Action |
|---------|---------|--------|
| Page-level data fails (project, dashboard) | "Something went wrong while loading [resource]." | "Try again" button that calls `refetch()` |
| Panel-level data fails (task detail) | "Failed to load task details." | "Try again" link in the panel |
| Inline data fails (comments, labels) | "Failed to load [resource]." | "Retry" text link |

### 5.4 Toast Notifications

Toast types and their colors:

| Type | Background | Icon | Auto-dismiss |
|------|-----------|------|-------------|
| Success | green-50, green-700 text | Check circle | 5 seconds |
| Error | red-50, red-700 text | X circle | 8 seconds (longer to read) |
| Info | blue-50, blue-700 text | Info circle | 5 seconds |

Toasts stack vertically in the bottom-right corner (desktop) or bottom-center (mobile). Max 3 visible — older ones are pushed out.

When to show each type:
- **Success toasts**: project created, member invited, label created, task deleted, comment deleted
- **Error toasts**: API errors (shown with the `message` from the error response), network errors, permission errors
- **Info toasts**: not used in v1 (reserved for future features like notifications)

Note: Auto-save feedback (task field changes) uses a subtle "Saving..." / "Saved" indicator in the task detail panel, NOT toasts. Toasts are for discrete actions only.

---

## 6. Edge Cases by Feature

### 6.1 Authentication

| Scenario | Expected Behavior |
|----------|------------------|
| User registers with an email that differs only in case (Alex@Example.com vs alex@example.com) | Treated as the same email. Backend stores lowercase. Return 409 DUPLICATE_EMAIL. |
| User registers with a username that differs only in case (Alex_Lead vs alex_lead) | Treated as the same username. Backend compares lowercase. Return 409 DUPLICATE_USERNAME. |
| Access token expires during a form submission | Axios interceptor catches 401, refreshes token, retries the submission. User does not notice. |
| Refresh token expires while user is on a page | Next API call triggers 401 → refresh attempt → refresh fails → redirect to login. No data loss since the last auto-save would have already persisted changes. |
| Two browser tabs open, one logs out | The tab that logged out clears the auth store. The other tab's next API call will fail (refresh token was invalidated), triggering redirect to login. |
| User submits login form with trailing whitespace in email | Backend trims and lowercases the email before lookup. Login succeeds. |
| BCrypt comparison on a password with exactly 72 characters | BCrypt silently truncates at 72 bytes. The 72-char validation limit prevents user confusion. |

### 6.2 Projects

| Scenario | Expected Behavior |
|----------|------------------|
| Owner tries to delete a project, types the name incorrectly | Return 400 CONFIRMATION_MISMATCH. Frontend shows inline error: "Project name does not match." Dialog stays open. |
| User navigates to a project URL they were removed from | Return 403 NOT_PROJECT_MEMBER. Show 403 page. |
| User navigates to a project URL that was deleted | Return 404 PROJECT_NOT_FOUND. Show 404 page. |
| Two users try to update the same project name simultaneously | Last write wins. No conflict detection in v1. |
| Project description is exactly 1000 characters | Accepted. Frontend shows character counter approaching the limit. |
| Project description is 1001 characters | Backend rejects with VALIDATION_FAILED. Frontend prevents submission (character counter turns red at 1000). |

### 6.3 Members

| Scenario | Expected Behavior |
|----------|------------------|
| Owner invites a user who is already a member | Return 409 ALREADY_MEMBER. Show inline error on invite form. |
| Owner invites an email not associated with any account | Return 404 USER_NOT_FOUND. Show inline error: "No account found with this email." |
| Owner tries to remove themselves | Return 400 OWNER_CANNOT_LEAVE. Show toast: "Transfer ownership first or delete the project." |
| Owner changes their own role to MEMBER when they are the only OWNER | Return 400 LAST_OWNER. Show toast. |
| Owner changes another member to OWNER, then changes their own role to MEMBER | Allowed. The other user becomes the sole OWNER. The `owner_id` on the project is updated in the same transaction. |
| A member is removed while they have tasks assigned | Tasks remain but `assignee_id` is set to NULL. Board shows those tasks as unassigned. |
| Member leaves a project, then navigates back to the project URL | Return 403 NOT_PROJECT_MEMBER. Show 403 page. Dashboard no longer shows the project. |

### 6.4 Tasks

| Scenario | Expected Behavior |
|----------|------------------|
| User creates a task with a deadline in the past | Allowed. The task is created with the past deadline. The board card shows the deadline in red (overdue). |
| User sets deadline to today | Allowed. Not shown as overdue. Overdue means `deadline < today`, not `deadline <= today`. |
| User tries to assign a task to someone who was just removed from the project | Return 400 ASSIGNEE_NOT_MEMBER. Frontend shows error on assignee field. |
| User drags a task to a new column, but the API call fails | Optimistic update rolls back. Card animates back to original column. Error toast shown. |
| User drags a task to the same column it's already in | No API call is made. The drag is a no-op. |
| User updates task title to whitespace only | Backend rejects: title must not be blank after trimming. Frontend prevents (button disabled for whitespace-only input). |
| User clears the task description (sets it to empty string) | Backend stores NULL (empty string is treated as clearing the field). Frontend shows "Add a description…" placeholder. |
| Two users edit the same task simultaneously | Last write wins. No conflict detection. The user who saves second will overwrite the first user's changes. Both see the final state on next refetch. |
| Task with 5 labels — user tries to add a 6th | Backend returns 400 TOO_MANY_LABELS. Frontend hides the "+" label button when 5 labels are attached. |
| User deletes a task while another user is viewing it | The viewer's next interaction will trigger a refetch. If the task is gone, show toast "This task has been deleted" and close the panel. |
| Search query with special characters (%_) | Backend escapes these in the LIKE query to prevent SQL pattern injection. `%` and `_` are treated as literal characters. |
| Filtering by multiple statuses (e.g., TODO and IN_PROGRESS) | Backend uses `WHERE status IN ('TODO', 'IN_PROGRESS')`. Both filters are applied as OR within the status dimension, AND across different filter dimensions. |

### 6.5 Labels

| Scenario | Expected Behavior |
|----------|------------------|
| User creates a label with a name that exists in a different case ("Frontend" when "frontend" exists) | Return 409 DUPLICATE_LABEL_NAME. The uniqueness check is case-insensitive. |
| User deletes a label that is attached to 3 tasks | Confirmation dialog shows: "Delete label 'frontend'? It will be removed from 3 tasks." On confirm, all task_labels entries are cascade-deleted. Tasks remain, just without that label. |
| User creates a label in one project and tries to attach it to a task in another project | Return 400 LABEL_NOT_IN_PROJECT. This cannot happen through the normal UI (label picker only shows labels from the task's project). This is a safety check for direct API calls. |
| User tries to attach a label that is already attached to the task | Return 409 LABEL_ALREADY_ATTACHED. Frontend should prevent this (checkbox is already checked in the picker). Handle silently. |
| Label color is submitted as "#fff" (3-char hex) | Backend rejects: color must match `^#[0-9A-Fa-f]{6}$` (full 6-char hex required). Frontend color picker only emits 6-char codes. |
| Label color is submitted as "red" (named color) | Backend rejects: same regex validation. Frontend enforces hex-only selection. |

### 6.6 Comments

| Scenario | Expected Behavior |
|----------|------------------|
| User submits a comment with only whitespace | Backend rejects: content must not be blank after trimming. Frontend disables Send button for whitespace-only input. |
| Comment is exactly 2000 characters | Accepted. Frontend shows character counter approaching the limit. |
| Comment is 2001 characters | Backend rejects VALIDATION_FAILED. Frontend prevents by showing red counter at 2000 and disabling Send. |
| User edits a comment, then cancels | Content reverts to the original text. No API call is made. |
| User edits a comment and saves | Backend updates the content and sets `updated_at` to now. Response includes `edited: true`. Frontend shows "(edited)" next to the timestamp. |
| User deletes a comment on a task that was just deleted by another user | Backend returns 404 (comment or task not found). Frontend shows toast and refreshes. |
| Loading more comments when earlier comments exist | "Load earlier comments" button fetches page N-1 (earlier page). New comments are prepended to the list. Scroll position is preserved so the user doesn't lose their place. |

---

## 7. Network and Timing Edge Cases

| Scenario | Expected Behavior |
|----------|------------------|
| Complete network loss during any API call | Axios throws a network error (no response). Toast: "Unable to connect. Please check your connection." Mutations are not retried automatically. Queries retry 3 times with exponential backoff. |
| Server returns 500 | Log the error. Show toast: "An unexpected error occurred. Please try again." For queries, retry up to 3 times. For mutations, do not retry (prevent duplicate writes). |
| Slow response (> 5 seconds) | Frontend shows loading state/skeleton. No timeout on the client side — let the request complete. The server has its own timeout. |
| User rapidly clicks a submit button | Button is disabled immediately on click (shows spinner). The mutation hook's `isPending` state drives the disabled prop. This prevents duplicate requests. |
| User rapidly toggles a label on/off on a task | Each toggle sends an API call. If a user toggles off before the "attach" call completes, the calls may arrive out of order. Use a debounce (200ms) on label toggle, and invalidate the task query on settle to ensure the final state is correct. |
| User edits task title, switches tabs, comes back | The debounced auto-save fires on blur (when the user leaves the field). If the tab is backgrounded, the save still goes through. When the user returns, the saved state is already persisted. |

---

## 8. Data Consistency Rules

These are invariants the system must maintain at all times. If any of these are violated, it's a bug.

| Rule | Enforcement |
|------|-------------|
| Every project has at least one OWNER in project_members | Service layer checks before role change or member removal |
| `projects.owner_id` matches the OWNER role in `project_members` | Service layer updates both in the same transaction when ownership changes |
| A task's `assignee_id` references a member of the task's project | Service layer validates on create and update. Set to NULL when member is removed. |
| A task's labels all belong to the same project as the task | Service layer validates on attach. Labels cannot be moved between projects. |
| A task has at most 5 labels | Service layer checks `COUNT(task_labels WHERE task_id = ?)` before insert |
| No two labels in the same project share a name (case-insensitive) | Unique index on `(project_id, LOWER(name))` + service layer check |
| No two users share a username (case-insensitive) or email (case-insensitive) | Unique indexes on `LOWER(username)` and `LOWER(email)` |
| Refresh tokens are hashed (SHA-256) in the database | Token provider generates raw token, hashes before storing. On validation, hash the incoming token and look up the hash. |
| Cascade deletes follow the correct order | ON DELETE CASCADE on FKs where safe. Application code in a transaction for complex cascades (project deletion). |
| `updated_at` is refreshed on every mutation | Service layer calls `entity.setUpdatedAt(Instant.now())` in every update method. JPA `@PreUpdate` can also handle this via an `Auditable` base entity. |
