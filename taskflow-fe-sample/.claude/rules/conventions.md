# Frontend Conventions — TaskFlow

## Tech Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Language | TypeScript | 5.x (strict mode) |
| Framework | React | 19.x |
| Build | Vite | 6.x |
| Styling | Tailwind CSS | 4.x |
| Components | shadcn/ui (Radix primitives) | — |
| Routing | React Router | 7.x |
| Server state | TanStack Query (React Query) | 5.x |
| Client state | Zustand | 5.x |
| HTTP | Axios | — |
| Forms | React Hook Form + Zod | — |
| Drag-and-drop | @dnd-kit/core + @dnd-kit/sortable | — |
| Markdown | react-markdown | — |
| Dates | date-fns | — |
| Icons | Lucide React | — |
| Testing | Vitest + React Testing Library + Playwright | — |

## Project Structure

```
src/
├── api/            auth.ts, users.ts, projects.ts, tasks.ts, labels.ts, taskLabels.ts, comments.ts, members.ts
│   └── client.ts   Axios instance with auth + refresh interceptors
├── components/     Avatar, Badge, LabelPill, EmptyState, ConfirmDialog, Toast, Pagination, Skeleton
│   └── ui/         shadcn/ui base components (Button, Input, Select, DatePicker, Modal, SlideOver, ...)
├── features/       auth/, projects/, board/, tasks/, comments/, members/, labels/, settings/
├── hooks/          useDebounce.ts, useClickOutside.ts, useKeyboard.ts
├── layouts/        AppLayout.tsx, AuthLayout.tsx, Navbar.tsx
├── pages/          LoginPage, RegisterPage, DashboardPage, ProjectPage, ProjectSettingsPage, ProfilePage, NotFoundPage, ForbiddenPage
├── stores/         authStore.ts (Zustand)
├── types/          api.ts, enums.ts, pagination.ts
└── utils/          cn.ts, formatDate.ts, avatarColor.ts
```

E2E tests: top-level `tests/` directory.

## Naming Conventions

| Element | Convention | Example |
|---------|-----------|---------|
| Components | PascalCase `.tsx` | `TaskCard.tsx`, `KanbanBoard.tsx` |
| Hooks | camelCase, `use` prefix | `useTasks.ts`, `useDebounce.ts` |
| Utilities | camelCase | `formatDate.ts`, `cn.ts` |
| Types/Interfaces | PascalCase | `Task`, `CreateTaskRequest` |
| Enum-like constants | SCREAMING_SNAKE in const object | `TaskStatus.IN_PROGRESS` |
| API functions | camelCase verb+noun | `createTask`, `fetchTasks` |
| Props interfaces | ComponentName + Props | `TaskCardProps`, `FilterBarProps` |
| Query keys | `[resource, ...params]` array | `['tasks', projectId, filters]` |
| Store slices | camelCase noun | `authStore` |
| Event handlers | `handle` + event | `handleSubmit`, `handleDragEnd` |
| Boolean props/state | `is`/`has`/`can` prefix | `isLoading`, `hasMore`, `canDelete` |

## Styling Rules

- **Tailwind utilities only** — no CSS files, no CSS modules, no inline style objects
- Use `cn()` (clsx + tailwind-merge) for conditional class merging
- Color tokens: CSS custom properties (`--color-primary: #2563EB`, `--color-danger: #DC2626`, etc.)
- Priority badge colors: URGENT (#FEE2E2/#991B1B), HIGH (#FEF3C7/#92400E), MEDIUM (#DBEAFE/#1E40AF), LOW (#F3F4F6/#374151)
- Font stack: `Inter, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif`

## TypeScript Rules

- Strict mode (`"strict": true` in tsconfig)
- No `any` — use `unknown` and narrow
- All component props must have explicit interface (`ComponentNameProps`)
- API response types defined in `src/types/api.ts`

## Git Conventions

Branch naming: `feature/auth-login`, `fix/token-refresh-loop`, `chore/update-deps`

Commits (conventional format):
```
feat(board): add drag-and-drop status update
fix(auth): prevent token refresh race condition
test(comments): add delete comment e2e test
```

## Dev Commands

```bash
npm run dev                   # http://localhost:5173
npx tsc --noEmit              # Type check
npm run lint                  # Lint
npm run test                  # Vitest unit/component
npm run test:coverage         # Coverage
npx playwright test           # E2E (requires backend + frontend running)
```
