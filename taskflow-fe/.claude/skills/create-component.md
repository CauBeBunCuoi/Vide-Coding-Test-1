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
6. Check `taskflow-fe/.claude/rules/component-standards.md` for naming and prop conventions

> **Template note:** `component-standards.md` above is an example name. Your project's rules files may be named differently — check what exists in `.claude/rules/` and reference the correct file.
