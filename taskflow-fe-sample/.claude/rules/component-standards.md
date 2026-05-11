# Component Standards — TaskFlow Frontend

## API Layer Pattern

Files in `src/api/` export plain async functions. No TanStack Query — just typed fetch wrappers:

```typescript
// src/api/tasks.ts
export async function fetchTasks(projectId: number, filters: TaskFilters): Promise<PageResponse<Task>> {
  const { data } = await axiosClient.get(`/projects/${projectId}/tasks`, { params: filters });
  return data;
}
```

## Query Hook Pattern

Feature hooks wrap TanStack Query:

```typescript
export function useTasks(projectId: number, filters: TaskFilters) {
  return useQuery({
    queryKey: ['tasks', projectId, filters],
    queryFn: () => fetchTasks(projectId, filters),
    retry: (failureCount, error) => {
      const status = (error as AxiosError).response?.status;
      if (status === 403 || status === 404) return false;  // Never retry permission/not-found
      return failureCount < 3;
    },
  });
}
```

## Optimistic Update Pattern

Used for: drag-and-drop, all inline field edits in the task detail panel, label toggles.

```typescript
return useMutation({
  mutationFn: ({ taskId, data }) => updateTask(taskId, data),
  onMutate: async ({ taskId, data }) => {
    await queryClient.cancelQueries({ queryKey: ['tasks', projectId] });
    const previousTasks = queryClient.getQueryData(['tasks', projectId]);
    queryClient.setQueryData(['tasks', projectId], (old) => /* apply optimistic change */);
    return { previousTasks };
  },
  onError: (_err, _vars, context) => {
    if (context?.previousTasks) {
      queryClient.setQueryData(['tasks', projectId], context.previousTasks);
    }
  },
  onSettled: () => {
    queryClient.invalidateQueries({ queryKey: ['tasks', projectId] });
  },
});
```

## Auth Store (Zustand)

`src/stores/authStore.ts`:
- `accessToken`: string | null — **memory only, never localStorage or sessionStorage**
- `user`: User | null
- `isAuthenticated`: derived (accessToken non-null)
- Actions: `setAccessToken(token)`, `logout()` (clears both token + user)

## Axios Interceptors (`src/api/client.ts`)

**Request interceptor:** attaches `Authorization: Bearer <accessToken>` from auth store.

**Response interceptor — 401 handling (with queue to prevent concurrent refreshes):**

```typescript
let isRefreshing = false;
let failedQueue: Array<{ resolve: Function; reject: Function }> = [];

// On 401 response:
if (!isRefreshing) {
  isRefreshing = true;
  try {
    const newToken = await refreshAccessToken();  // POST /api/v1/auth/refresh
    authStore.setAccessToken(newToken);
    failedQueue.forEach(({ resolve }) => resolve(newToken));
  } catch {
    failedQueue.forEach(({ reject }) => reject(error));
    authStore.logout();
    navigate('/login');
  } finally {
    failedQueue = [];
    isRefreshing = false;
  }
} else {
  return new Promise((resolve, reject) => failedQueue.push({ resolve, reject }))
    .then(token => {
      originalRequest.headers.Authorization = `Bearer ${token}`;
      return axiosClient(originalRequest);
    });
}
```

## Route Guards

- `<ProtectedRoute>`: redirects to `/login` if `isAuthenticated = false`
- `<PublicOnlyRoute>`: redirects to `/` if `isAuthenticated = true` (login + register pages)

Register in `App.tsx` when adding routes.

## Error Handling in Mutations

```typescript
onError: (error) => {
  const apiError = (error as AxiosError<ApiError>).response?.data;
  switch (apiError?.error) {
    case 'ASSIGNEE_NOT_MEMBER':
      setFieldError('assignee', apiError.message);
      break;
    case 'TASK_NOT_FOUND':
      toast.error('This task has been deleted.');
      closePanel();
      queryClient.invalidateQueries({ queryKey: ['tasks'] });
      break;
    case 'NOT_PROJECT_MEMBER':
      toast.error('You are no longer a member of this project.');
      navigate('/');
      break;
    default:
      toast.error(apiError?.message || 'Something went wrong.');
  }
}
```

## Security Rules

- **Never use `dangerouslySetInnerHTML`**
- Use `react-markdown` for all markdown rendering (sanitizes by default)
- User-provided text (titles, labels, usernames) rendered as text nodes only, never as HTML

## Form Validation (Zod + React Hook Form)

Zod schemas must **exactly mirror** backend constraints:

```typescript
const createTaskSchema = z.object({
  title: z.string().trim().min(1, 'Title is required').max(200),
  description: z.string().max(5000).optional(),
  priority: z.enum(['LOW', 'MEDIUM', 'HIGH', 'URGENT']).optional(),
  deadline: z.string().regex(/^\d{4}-\d{2}-\d{2}$/).optional().nullable(),
  assigneeId: z.number().positive().optional().nullable(),
  labelIds: z.array(z.number().positive()).max(5).optional(),
});
```

Wire: `useForm({ resolver: zodResolver(schema) })`. Validate on blur for text, on change for selects.

## Debounce Timings

| Interaction | Debounce |
|------------|---------|
| Text field auto-save (title, description) | 500ms |
| Filter search input | 300ms |
| Label toggle (attach/detach) | 200ms |

## Toast Notifications

| Type | Auto-dismiss | Use for |
|------|-------------|---------|
| Success (green) | 5s | Project created, member invited, task/comment deleted |
| Error (red) | 8s | API errors, network errors, permission errors |
| Info (blue) | 5s | Reserved (not used in v1) |

Auto-save feedback (task field changes): use "Saving…" / "Saved" **indicator in panel** — NOT toasts.
Position: bottom-right (desktop), bottom-center (mobile). Max 3 visible stacked.
