---
name: create-component
description: How to create a new UI component in this project
---

# How to Create a New Component

1. Determine whether the component is shared (`src/components/`) or feature-specific (`src/features/<area>/`)
2. Create the component file (e.g. `src/features/board/TaskCard.tsx`)
3. Define a typed `Props` interface (`TaskCardProps`)
4. Use Tailwind utilities only — no custom CSS files
5. Use `cn()` from `src/utils/cn.ts` for conditional class names
6. If the component fetches data, create a TanStack Query hook in the feature folder (e.g. `useTasks.ts`)
7. If the component mutates data, implement optimistic updates in the mutation hook (onMutate/onError/onSettled)
8. Write a component test (`TaskCard.test.tsx`) using React Testing Library

## References
- `references/screens.md` — all screen specs with layout, component inventory, field behaviors, and states
- `references/user-flows.md` — navigation map and user journeys that involve this component
- `.claude/rules/component-standards.md` — shared components, design tokens, priority/status colors, optimistic update pattern
- `.claude/rules/conventions.md` — naming, auth flow, query hooks pattern, error handling, validation
