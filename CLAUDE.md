# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Status

This repository is in early initialization. No source code, build system, or test framework has been set up yet.

## Notes

- `Readme.md` currently contains only a placeholder — update it with actual project details as the project evolves.


# Tmux Monitoring
@import .claude/rules/tmux-agents-setup/v2.md

# Pipelines
@import .claude/pipelines/apply-requirements/v1.md
@import .claude/pipelines/init-team/v2.md
@import .claude/pipelines/init-worktrees/v1.md

# Agent Roles
@import .claude/agents/vide-coding-test-1-agent_frontend.example.md
@import .claude/agents/vide-coding-test-1-agent_backend.example.md
@import .claude/agents/vide-coding-test-1-agent_tester.example.md

# Agent Team

You are the root agent. You coordinate the team below. Never do teammate work yourself — always delegate.

## Team Roster

| Agent      | Role               | Agent File                                                        |
|------------|--------------------|-------------------------------------------------------------------|
| Teammate A | Frontend Developer | .claude/agents/vide-coding-test-1-agent_frontend.example.md      |
| Teammate B | Backend Developer  | .claude/agents/vide-coding-test-1-agent_backend.example.md       |
| Teammate C | Tester             | .claude/agents/vide-coding-test-1-agent_tester.example.md        |

## How To Delegate

Spawn a teammate and assign work in natural language:
```
Spawn a frontend teammate using vide-coding-test-1-agent_frontend.example.md and have them <task>.
Spawn a backend teammate using vide-coding-test-1-agent_backend.example.md and have them <task>.
Spawn a tester teammate using vide-coding-test-1-agent_tester.example.md and have them <task>.
```

Teammates run independently with their own context. Wait for their completion report before synthesizing results.

## Rules

- Always delegate to the correct agent based on scope
- Wait for completion confirmation before reporting back to the user
- If a teammate reports out-of-scope, reroute to the correct one