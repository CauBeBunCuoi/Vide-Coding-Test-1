---
name: create-component
description: How to add a new page or feature component to the TaskFlow React frontend
---

# How to Add a New Page

1. **Create the page component** in `src/pages/<FeatureName>Page.tsx`

2. **Register the route** in `src/App.tsx`:
   - Wrap with `<ProtectedRoute>` if auth required (most pages)
   - Wrap with `<PublicOnlyRoute>` for login/register (redirects to `/` if already authenticated)

3. **Create feature components** in `src/features/<area>/`:
   - One file per logical component (e.g., `KanbanBoard.tsx`, `FilterBar.tsx`)
   - Co-locate tests: `KanbanBoard.test.tsx` next to `KanbanBoard.tsx`

4. **Add API functions** in `src/api/<resource>.ts`:
   - Export plain async functions that call the Axios client and return typed responses
   - No TanStack Query here — just raw fetch functions

5. **Create query/mutation hooks** in `src/features/<area>/use<Resource>.ts`:
   - Wrap `useQuery` for fetches, `useMutation` for writes
   - All mutations that modify server data should use the optimistic update pattern
   - See `rules/component-standards.md` for the full optimistic update template

6. **Define TypeScript types** in `src/types/`:
   - `api.ts` — response shapes (User, Project, Task, etc.)
   - `enums.ts` — TaskStatus, TaskPriority, MemberRole
   - `pagination.ts` — `PageResponse<T>` generic

7. **Add Zod schema** (for forms) in the feature file or a `schemas.ts` alongside it:
   - Mirror backend validation constraints exactly (same min/max/pattern)
   - Wire to React Hook Form via `@hookform/resolvers/zod`

8. **Write component tests** (Vitest + React Testing Library):
   - Render component, simulate user interactions, assert DOM output
   - Do not test implementation details — test user-visible behavior

9. **Add E2E test** for the critical path in `tests/<feature>.spec.ts` (Playwright):
   - Cover the happy path and at least one error path
   - Backend + frontend must both be running

---

# How to Add a Reusable UI Component

1. Add to `src/components/ui/` if it's a base/primitive (shadcn pattern)
2. Add to `src/components/` for project-specific shared components (Avatar, Badge, LabelPill, etc.)
3. Props interface named `<ComponentName>Props`
4. Export from the component file (no barrel re-exports required)
5. Use Tailwind utilities only — no CSS files
6. Write a co-located test file

> Check `.claude/rules/conventions.md` for naming rules.
> Check `.claude/rules/component-standards.md` for API layer, hooks, and optimistic update patterns.
