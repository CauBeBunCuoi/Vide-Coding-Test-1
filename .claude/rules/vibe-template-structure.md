# Vibe Template Structure

This document describes the complete directory layout, conventions, and how all pieces connect in the Vibe multi-agent team template.

---

## Overview

The template provides a **root orchestrator + 3 specialist agent teammates** (Frontend, Backend, Tester). The root agent coordinates; teammates work in isolated subproject folders, each with their own CLAUDE.md, rules, and skills.

**Pipelines** automate setup and requirements distribution. **Agent files** define each teammate's role, scope, and working directory.

---

## Directory Tree

```
<project-root>/
│
├── CLAUDE.md                              # Root agent: imports pipelines, agent roles, defines team roster
├── Readme.md                              # Project readme (placeholder until populated)
├── .worktreeinclude                       # Lists gitignored files (e.g. .env) to copy into worktrees
│
├── .claude/
│   ├── settings.json                      # Claude Code settings (teammateMode, CLAUDE_CODE_EXPERIMENTAL_AGENT_TEAMS)
│   │
│   ├── agents/                            # Agent role definition files
│   │   ├── <project>-agent_frontend.example.md   # Permanent template — never modify or delete
│   │   ├── <project>-agent_backend.example.md    # Permanent template — never modify or delete
│   │   ├── <project>-agent_tester.example.md     # Permanent template — never modify or delete
│   │   ├── <project>-agent_frontend.md           # Real file: copy from .example, customize per project
│   │   ├── <project>-agent_backend.md            # Real file
│   │   └── <project>-agent_tester.md             # Real file
│   │
│   ├── pipelines/                         # Step-by-step automation workflows
│   │   ├── init-team/
│   │   │   ├── v1.md                      # (archived) tmux split-pane version
│   │   │   └── v2.md                      # (current) in-process teammate mode
│   │   ├── init-worktrees/
│   │   │   └── v1.md                      # (legacy) manual worktree setup — superseded by init-team v2
│   │   ├── apply-requirements/
│   │   │   ├── v1.md                      # (archived) routed to context/ + skills/ + rules/
│   │   │   ├── v2.md                      # (archived) routed to rules/ + skills/references/
│   │   │   └── v3.md                      # (current) routes to rules/ and skills/<name>/references/
│   │   └── tmux-agents-setup/
│   │       ├── v1.md                      # (archived)
│   │       └── v2.md                      # (current) Windows in-process mode setup guide
│   │
│   ├── requirements/                      # READ-ONLY: drop any project docs here
│   │   ├── 01-product-requirements.md
│   │   ├── 02-api-contract.md
│   │   ├── 03-database-design.md
│   │   ├── 04-ui-design-spec.md
│   │   ├── 05-tech-stack-and-conventions.md
│   │   ├── 06-development-workflows.md
│   │   ├── 07-security-and-validation.md
│   │   └── 08-error-handling-and-edge-cases.md
│   │
│   └── rules/                             # Root-level coordination rules (team-wide, not project-specific)
│       └── vibe-template-structure.md     # This file
│
├── taskflow-fe/                           # Frontend teammate workspace
│   ├── CLAUDE.md                          # Imports rules; has ## Project Context section
│   └── .claude/
│       ├── rules/                         # Frontend behavioral standards (always loaded)
│       │   ├── conventions.md             # Coding conventions
│       │   └── component-standards.md     # Component naming and structure
│       └── skills/                        # Task procedures (loaded on demand)
│           ├── create-component/
│           │   ├── SKILL.md               # Step-by-step: how to create a component
│           │   └── references/
│           │       └── screens.md         # All screens the component may appear in
│           └── implement-feature/
│               ├── SKILL.md               # Step-by-step: how to implement a feature end-to-end
│               └── references/
│                   └── user-flows.md      # User journeys and flows
│
├── taskflow-be/                           # Backend teammate workspace
│   ├── CLAUDE.md                          # Imports rules; has ## Project Context section
│   └── .claude/
│       ├── rules/                         # Backend behavioral standards (always loaded)
│       │   ├── conventions.md             # Backend coding conventions
│       │   ├── database.md                # DB schema and migration conventions
│       │   └── api.md                     # REST API naming and response format standards
│       └── skills/
│           ├── add-endpoint/
│           │   ├── SKILL.md               # Step-by-step: how to add a REST endpoint
│           │   └── references/
│           │       └── api-list.md        # Current list of all endpoints
│           └── manage-background-jobs/
│               ├── SKILL.md               # Step-by-step: how to add/modify a background job
│               └── references/
│                   └── background-jobs.md # Existing background jobs inventory
│
└── taskflow-test/                         # Tester teammate workspace
    ├── CLAUDE.md                          # Imports rules; has ## Project Context section
    └── .claude/
        ├── rules/
        │   └── conventions.md             # Test naming, file structure, coverage conventions
        └── skills/
            └── write-e2e-test/
                ├── SKILL.md               # Step-by-step: how to write an E2E test
                └── references/
                    └── test-scope.md      # What is in/out of scope, coverage targets
```

---

## Key Concepts

### Agent Files — `.example.md` vs Real Files

| File type | Name pattern | Purpose |
|---|---|---|
| Template | `<project>-agent_<role>.example.md` | Permanent template, never modified, never injected into agents |
| Real | `<project>-agent_<role>.md` | Copied from template, customized per project, injected into teammates |

CLAUDE.md must import real files (not `.example`) in the `# Agent Roles` section.

### Agent Frontmatter

Each agent file defines:
- `name` — agent identifier
- `tools` — allowed tools (Read, Edit, Write, Bash, Glob, Grep)
- `model` — default is `haiku`
- `isolation: worktree` — Claude Code auto-creates an isolated worktree per task

### Subproject Folder Anatomy

Each subproject (`taskflow-fe`, `taskflow-be`, `taskflow-test`) has the same layout:

```
<subproject>/
├── CLAUDE.md               ← Agent loads this on every task (via @import rules)
└── .claude/
    ├── rules/              ← Always-on behavioral instructions ("how to write code")
    └── skills/
        └── <skill-name>/
            ├── SKILL.md            ← Procedural how-to (loaded on demand)
            └── references/         ← Large domain reference data for this skill
                └── <file>.md
```

**Rule:** `rules/` = short, always-true behavioral standards. `skills/references/` = large domain data looked up on demand.

---

## Active Pipelines

### `init-team/v2.md` (current)

Prepares agent files and spawns the in-process team. Key steps:
1. Validate no `.example` in CLAUDE.md imports
2. Copy `.example.md` → real agent files if missing
3. Update CLAUDE.md imports and roster
4. Verify subproject structure and fill in rule stubs
5. Spawn team with `teammateMode: in-process`
6. Report team ready

### `apply-requirements/v3.md` (current)

Reads all docs in `.claude/requirements/` and distributes content into subproject files. Key steps:
1. Detect active worktrees (`git worktree list`)
2. Scan and read all requirement files
3. Route content: `rules/` for behavioral standards, `skills/<name>/references/` for large reference data
4. Update each subproject's CLAUDE.md `## Project Context`
5. Write to both main checkout and active worktrees
6. Report what was updated

### `tmux-agents-setup/v2.md` (current)

Windows-specific: configures `teammateMode: in-process` in `settings.json`. Tmux split-pane is unsupported on Windows Terminal. In-process mode uses keyboard shortcuts (`Shift+Down` to cycle, `Ctrl+T` for task list).

### `init-worktrees/v1.md` (legacy)

Manual worktree creation per agent. Superseded by `init-team/v2.md` which handles worktrees automatically via `isolation: worktree` in agent frontmatter.

---

## Setup Sequence

```
1. Drop project docs into .claude/requirements/
2. Run: init-team pipeline        → creates real agent files, spawns team
3. Run: apply-requirements pipeline → distributes requirements into subproject files
4. Teammates are ready to receive tasks
```

---

## Worktree Conventions

| Agent | Subproject folder | Worktree path (Claude Code managed) |
|---|---|---|
| Teammate A (Frontend) | `taskflow-fe/` | `.claude/worktrees/taskflow-fe/` |
| Teammate B (Backend) | `taskflow-be/` | `.claude/worktrees/taskflow-be/` |
| Teammate C (Tester) | `taskflow-test/` | `.claude/worktrees/taskflow-test/` |

When `apply-requirements` runs with active worktrees, it writes to both the main checkout and each worktree. Teammates read from their worktree.

`.worktreeinclude` at project root lists gitignored files (e.g. `.env.*`) that Claude Code copies into each worktree so agents can run dev servers.

---

## Naming Conventions

- Project kebab: derived from project root folder name — e.g. `vide-coding-test-1`
- Agent file: `<project-kebab>-agent_<role>.md` — e.g. `vide-coding-test-1-agent_frontend.md`
- Rule files: any name that reflects content — defined per project, not fixed
- Skill directories: any name that reflects the task — e.g. `add-endpoint/`, `create-component/`
