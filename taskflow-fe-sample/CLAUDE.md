# Teammate A — Frontend Workspace

# Rules
@import .claude/rules/conventions.md
@import .claude/rules/component-standards.md

## Project Context

**Stack:** TypeScript 5 (strict) + React 19 + Vite 6 + Tailwind CSS 4 + shadcn/ui + React Router 7 + TanStack Query 5 + Zustand 5 + Axios + React Hook Form + Zod + @dnd-kit + react-markdown + date-fns + Lucide React

**Folder layout:** `src/api/` (typed fetch per resource) → `src/features/<area>/` (components + hooks per feature) → `src/pages/` (one file per route) → `src/stores/` (Zustand: authStore) → `src/types/` (API types, enums, pagination) → `src/components/` (shared UI: Avatar, Badge, Toast, etc.) + `src/components/ui/` (shadcn primitives) → `src/hooks/` (useDebounce, useClickOutside, useKeyboard)

**Auth:** Access token in Zustand memory (never localStorage). Refresh token is httpOnly cookie. Axios interceptors handle token attachment + 401 → refresh → retry with a queue to prevent concurrent refresh calls.

**Styling:** Tailwind utilities only. No CSS files. Use `cn()` for conditional classes.

**Routes:** `/login`, `/register`, `/` (dashboard), `/projects/:id` (board + `?view=list`), `/projects/:id/settings`, `/profile`. All routes under `/` require auth — unauthenticated users redirect to `/login`.

**Run frontend:** `npm run dev` → http://localhost:5173 (proxies `/api` to backend at :8080)

**Run tests:** `npm run test` (Vitest unit/component) | `npx playwright test` (E2E, requires full stack)
