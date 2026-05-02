# Tmux Setup Rules — Agent Team Bootstrap

This document defines how the root agent must set up the tmux environment from scratch in any project.

---

## Naming Conventions

- Tmux session name: `<project-kebab>-agent_team_tmux` — e.g. `my-app-agent_team_tmux`
- Agent role file name: `<project-kebab>-agent_<role>.md` — e.g. `my-app-agent_frontend.md`
- Derive `<project-kebab>` from the project root folder name in kebab-case

## Session

- Session name: `<project-kebab>-agent_team_tmux`
- Layout: tiled (evenly split based on agent count)
- Size: minimum 220x50

## Pane Map

| Pane | Agent    | Working Directory | Role Doc                                      |
|------|----------|-------------------|-----------------------------------------------|
| 0    | Agent 1  | project root      | .claude/agents/<project-kebab>-agent_<role>.md |
| 1    | Agent 2  | project root      | .claude/agents/<project-kebab>-agent_<role>.md |
| 2    | Agent 3  | project root      | .claude/agents/<project-kebab>-agent_<role>.md |

---

## Bootstrap Procedure

Run these steps in order when setting up a new project:

### 1. Kill existing session if any
```bash
wsl tmux kill-session -t agent_team_tmux 2>/dev/null
```

### 2. Create session and panes
```bash
wsl tmux new-session -d -s agent_team_tmux -x 220 -y 50
wsl tmux split-window -h -t agent_team_tmux
wsl tmux split-window -v -t agent_team_tmux:0.0
wsl tmux split-window -v -t agent_team_tmux:0.1
wsl tmux select-layout -t agent_team_tmux tiled
```

### 3. Launch claude in each pane from project root
```bash
PROJECT="<absolute-project-path>"

wsl tmux send-keys -t agent_team_tmux:0.0 "cd \"$PROJECT\" && claude" Enter
wsl tmux send-keys -t agent_team_tmux:0.1 "cd \"$PROJECT\" && claude" Enter
wsl tmux send-keys -t agent_team_tmux:0.2 "cd \"$PROJECT\" && claude" Enter
```

### 4. Wait for agents to load (~4 seconds), then inject roles
```bash
sleep 4

wsl tmux send-keys -t agent_team_tmux:0.0 "Read .claude/agents/teammate-a.example.md and follow it strictly as your role for this entire session." Enter
wsl tmux send-keys -t agent_team_tmux:0.1 "Read .claude/agents/teammate-b.example.md and follow it strictly as your role for this entire session." Enter
wsl tmux send-keys -t agent_team_tmux:0.2 "Read .claude/agents/teammate-c.example.md and follow it strictly as your role for this entire session." Enter
```

### 5. Wait for role acknowledgement (~5 seconds), then verify
```bash
sleep 5

wsl tmux send-keys -t agent_team_tmux:0.0 "Who are you? One line only." Enter
wsl tmux send-keys -t agent_team_tmux:0.1 "Who are you? One line only." Enter
wsl tmux send-keys -t agent_team_tmux:0.2 "Who are you? One line only." Enter
```

### 6. Capture and confirm responses
```bash
wsl tmux capture-pane -t agent_team_tmux:0.0 -p
wsl tmux capture-pane -t agent_team_tmux:0.1 -p
wsl tmux capture-pane -t agent_team_tmux:0.2 -p
```

If any agent responds incorrectly, re-send their role injection command and verify again.

---

## Restarting Agents

Use when agent context needs to be refreshed:

```bash
PROJECT="<absolute-project-path>"

wsl tmux send-keys -t agent_team_tmux:0.0 C-c
wsl tmux send-keys -t agent_team_tmux:0.1 C-c
wsl tmux send-keys -t agent_team_tmux:0.2 C-c
sleep 1
wsl tmux send-keys -t agent_team_tmux:0.0 "cd \"$PROJECT\" && claude" Enter
wsl tmux send-keys -t agent_team_tmux:0.1 "cd \"$PROJECT\" && claude" Enter
wsl tmux send-keys -t agent_team_tmux:0.2 "cd \"$PROJECT\" && claude" Enter
sleep 4
wsl tmux send-keys -t agent_team_tmux:0.0 "Read .claude/agents/teammate-a.md and follow it strictly as your role for this entire session." Enter
wsl tmux send-keys -t agent_team_tmux:0.1 "Read .claude/agents/teammate-b.md and follow it strictly as your role for this entire session." Enter
wsl tmux send-keys -t agent_team_tmux:0.2 "Read .claude/agents/teammate-c.md and follow it strictly as your role for this entire session." Enter
```

---

## Delegation Commands

Send task to a pane:
```bash
wsl tmux send-keys -t agent_team_tmux:<pane> "<task>" Enter
```

Read pane output:
```bash
wsl tmux capture-pane -t agent_team_tmux:<pane> -p
```

---

## Rules

- Always use `session:window.pane` notation (e.g. `<project-kebab>-agent_team_tmux:0.0`) — never use bare session name
- All agents launch from project root — they share root CLAUDE.md for project context
- Agent identity is injected via role doc after launch — not via separate CLAUDE.md directories
- tmux is available via `wsl tmux` when running through Claude Code on Windows/WSL
- Replace `<absolute-project-path>` with the actual project path when using in a new project
- Never inject a `.example.md` file into any agent — examples are templates only
- Never create a real agent file that contains `.example` in its name
- Never use `agent_team_tmux` as a bare session name — always prefix with `<project-kebab>`
