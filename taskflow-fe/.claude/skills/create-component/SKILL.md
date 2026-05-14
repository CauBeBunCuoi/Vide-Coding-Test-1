---
name: create-component
description: How to create a new UI component in this project
---

# How to Create a New Component

1. Create a folder under `src/components/<ComponentName>/`
2. Add `index.tsx` — the component file
3. Add `<ComponentName>.module.css` — scoped styles
4. Export from `src/components/index.ts`
5. Write a unit test in `src/components/<ComponentName>/<ComponentName>.test.tsx`
6. Check `.claude/rules/component-standards.md` for naming and prop conventions

## References
- `references/screens.md` — all screens this component may appear in
- `references/user-flows.md` — user journeys that involve this component
- `.claude/rules/component-standards.md` — naming and prop conventions
- `.claude/rules/conventions.md` — general frontend coding conventions

> **Template note:** Rule file names above are examples. Check what exists in `.claude/rules/` and reference the correct files.
