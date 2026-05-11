# Teammate A — Frontend Workspace

> **Template note:** File names in `@import` paths below are examples only. The `context/`, `skills/`, and `rules/` folder structure is fixed, but file names inside are yours to define per project.

# Context
@import .claude/context/screens.md
@import .claude/context/user-flows.md

# Skills
@import .claude/skills/create-component.md

# Rules
@import .claude/rules/conventions.md
@import .claude/rules/component-standards.md

## Project Context

**Stack:** TypeScript 5 (strict) + React 19 + Vite 6 + Tailwind CSS 4 + shadcn/ui + React Router 7 + TanStack Query 5 + Zustand 5 + Axios + React Hook Form + Zod + @dnd-kit + react-markdown + date-fns + Lucide React

**Folder layout:** `src/api/` (typed fetch functions per resource) → `src/features/<area>/` (components + hooks per feature) → `src/pages/` (one file per route) → `src/stores/` (Zustand: authStore) → `src/types/` (API types, enums, pagination) → `src/components/` (shared UI: Avatar, Badge, Toast, etc.) + `src/components/ui/` (shadcn primitives)

**Auth:** Access token in Zustand memory (never localStorage). Refresh token is httpOnly cookie. Axios interceptors handle token attachment + 401 → refresh → retry with a queue to prevent concurrent refresh calls.

**Styling:** Tailwind utilities only. No CSS files. Use `cn()` for conditional classes.

**Run frontend:** `npm run dev` → http://localhost:5173 (proxies `/api` to backend at :8080)

**Run tests:** `npm run test` (Vitest unit/component) | `npx playwright test` (E2E, requires full stack)
