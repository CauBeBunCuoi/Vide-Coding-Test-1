# My Way of Working — Requirements Application

## Context

I work with a Claude Code multi-agent setup where a root agent coordinates three specialist agents: **frontend**, **backend**, and **tester**. Each agent works in its own isolated workspace.

When I want to give the team project requirements, I drop documents into a folder called `.claude/requirements/`. The root agent then reads all those documents and distributes the relevant content into each agent's workspace automatically. This is called the **Apply Requirements** pipeline.

---

## How the Pipeline Works (concept only)

The root agent reads all requirement documents holistically, then routes extracted content into three destination types per agent workspace:

| Destination | Purpose |
|---|---|
| `context/` | **What** the system is — screens, user flows, APIs, data models, business rules, domain entities |
| `skills/` | **How to do** a repeatable procedure — step-by-step workflows the agent should follow |
| `rules/` | **How to write** the code — standards, conventions, patterns, constraints |

Each agent workspace only receives content relevant to its scope:

| Agent workspace | Relevant content |
|---|---|
| `taskflow-fe` (frontend) | UI/UX, screens, user flows, component inventory, design system, frontend tech stack |
| `taskflow-be` (backend) | Data models, API design, auth, architecture decisions, backend tech stack |
| `taskflow-test` (tester) | Test strategy, coverage targets, QA scope, edge cases, test conventions |

Content relevant to more than one agent goes into all of them.

---

## What I Need From You

I want to **test this pipeline end-to-end**. To do that, I need sample requirement documents to drop into `.claude/requirements/`.

Please prepare a set of realistic requirement documents for a sample project. The documents should together provide enough content to exercise all three destination types (`context/`, `skills/`, `rules/`) across all three agent workspaces (`taskflow-fe`, `taskflow-be`, `taskflow-test`).

**Format:** Any realistic document format is fine — PRD, design spec, API contract, ADR, or plain notes. The pipeline accepts any document type. What matters is that the content is rich enough that the agent has real decisions to make about what goes where.

**Do not pre-label or pre-sort** the content by destination type or agent workspace. The whole point is that the root agent figures that out itself.
