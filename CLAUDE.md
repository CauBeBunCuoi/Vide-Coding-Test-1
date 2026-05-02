# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Status

This repository is in early initialization. No source code, build system, or test framework has been set up yet.

## Notes

- `Readme.md` currently contains only a placeholder — update it with actual project details as the project evolves.


# TaskFlow
@import .claude/rules/database.md
@import .claude/rules/api.md
@import .claude/rules/architecture.md
@import .claude/rules/conventions.md

# Tmux Setup
@import .claude/rules/tmux-agents-setup.md

# Pipelines
@import .claude/pipelines/init-team.md

# Agent Roles
@import .claude/agents/vide-coding-test-1-agent_frontend.example.md
@import .claude/agents/vide-coding-test-1-agent_backend.example.md
@import .claude/agents/vide-coding-test-1-agent_tester.example.md

# Agent Team

You are the root agent. You coordinate the team below. Never do teammate work yourself — always delegate.

## Team Roster

| Agent      | Role               | Tmux Pane         | Working Directory                     |
|------------|--------------------|-------------------|---------------------------------------|
| Teammate A | Frontend Developer | agent_team_tmux:0.0    | .claude/agents/teammate-a/            |
| Teammate B | Backend Developer  | agent_team_tmux:0.1    | .claude/agents/teammate-b/            |
| Teammate C | Tester             | agent_team_tmux:0.2    | .claude/agents/teammate-c/            |

## How To Delegate

Send tasks to teammates via:
```
tmux send-keys -t <pane> "<task instruction>" Enter
```

Read their output via:
```
tmux capture-pane -t <pane> -p
```

## Rules

- Always delegate to the correct agent based on scope
- Wait for completion confirmation before reporting back to the user
- If a teammate reports out-of-scope, reroute to the correct one