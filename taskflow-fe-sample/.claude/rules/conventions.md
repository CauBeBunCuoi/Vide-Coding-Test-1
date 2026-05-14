# Frontend Conventions

## Naming
| Element | Convention | Example |
|---------|-----------|---------|
| Components | PascalCase | `TaskCard.tsx`, `KanbanBoard.tsx` |
| Hooks | camelCase with `use` prefix | `useTasks.ts`, `useDebounce.ts` |
| Utilities | camelCase | `formatDate.ts`, `cn.ts` |
| Types/Interfaces | PascalCase | `Task`, `CreateTaskRequest` |
| Enum-like constants | const object, SCREAMING_SNAKE values | `TaskStatus.IN_PROGRESS` |
| API functions | camelCase verb+noun | `createTask`, `fetchTasks`, `updateTask` |
| CSS classes | Tailwind utilities only (no custom CSS files) | `className="flex items-center gap-2"` |
| Props interfaces | Component name + `Props` | `TaskCardProps`, `FilterBarProps` |
| Query keys | `[resource, ...params]` array | `['tasks', projectId, filters]` |
| Event handlers | `handle` + event | `handleSubmit`, `handleDragEnd` |
| Boolean props/state | `is`/`has`/`can` prefix | `isLoading`, `hasMore`, `canDelete` |

## Auth Flow
- Access token in Zustand `authStore` (memory only — never localStorage or sessionStorage).
- Refresh token is httpOnly cookie — JavaScript cannot access it.
- Axios request interceptor attaches `Authorization: Bearer <token>`.
- Axios response interceptor on 401: attempt token refresh → retry original request. If refresh fails, clear auth state and redirect to `/login`.
- Concurrent 401s: queue all failed requests, execute one refresh, drain queue on success or reject all on failure.
- `ProtectedRoute` component: redirects to `/login` when not authenticated.
- `PublicOnlyRoute` component: redirects to `/` when already authenticated.

## API Layer
Each resource file in `src/api/` exports plain async functions returning typed responses.
These are consumed by TanStack Query hooks in feature folders, never called directly from components.

## Query Hooks Pattern
```typescript
// useQuery
export function useTasks(projectId: number, filters: TaskFilters) {
  return useQuery({
    queryKey: ['tasks', projectId, filters],
    queryFn: () => fetchTasks(projectId, filters),
    retry: (failureCount, error) => {
      const status = error.response?.status;
      if (status === 403 || status === 404) return false;
      return failureCount < 3;
    },
  });
}

// useMutation with optimistic update
export function useUpdateTask(projectId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ taskId, data }) => updateTask(taskId, data),
    onMutate: async ({ taskId, data }) => {
      await queryClient.cancelQueries({ queryKey: ['tasks', projectId] });
      const previousData = queryClient.getQueryData(['tasks', projectId]);
      queryClient.setQueryData(['tasks', projectId], /* optimistic update */);
      return { previousData };
    },
    onError: (err, variables, context) => {
      queryClient.setQueryData(['tasks', projectId], context?.previousData);
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: ['tasks', projectId] });
    },
  });
}
```

## Validation
Use Zod schemas mirroring backend constraints. React Hook Form integrates via `@hookform/resolvers/zod`.
Validate on blur for text fields, on change for selects.
Frontend validation is advisory — backend is authoritative.

## Error Handling
- Axios interceptor shows `toast.error()` for network errors (no `error.response`).
- Mutation `onError`: switch on `apiError?.error` code to show inline field errors or toasts.
- Query `isError`: 403 → show `ForbiddenPage`, 404 → show `NotFoundPage`, others → inline error + "Try again" button.
- Never use `dangerouslySetInnerHTML`.
- Markdown (task descriptions, comments): render via `react-markdown` only.
- User text in non-markdown contexts: rendered as text nodes, never HTML.

## Auto-Save (Task Detail Panel)
- Text fields (title, description): save on blur, debounced 500ms.
- Select/date fields: save immediately on change.
- Show "Saving…" indicator near top of panel → "Saved" for 2 seconds.
- Auto-save uses toasts only for discrete actions (create, delete), NOT for inline field saves.

## Filter State in URL
Active filters are preserved in URL query parameters so they can be shared and bookmarked.
Sort state is also synced to URL params (e.g. `?sort=deadline,asc`).

## Git
Branch naming: `feature/kanban-board`, `fix/token-refresh-loop`, `chore/update-deps`.
Commit messages: conventional commits — `feat(board): add drag-and-drop status update`.
