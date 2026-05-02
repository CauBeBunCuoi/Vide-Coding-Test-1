# Pipeline: Init Agent Team

Run this pipeline at the start of any new session or when agents need a full reset.
Follow every step in order. Do not skip or assume — execute each step explicitly.

## Naming Conventions

- Tmux session name: `<project-kebab>-agent_team_tmux` — e.g. `my-app-agent_team_tmux`
- Agent role file name: `<project-kebab>-agent_<role>.md` — e.g. `my-app-agent_frontend.md`
- Derive `<project-kebab>` from the project root folder name in kebab-case

## Hard Rules — Enforce Before Every Run

STOP and do not proceed if any of these violations are found:

1. Any `@import` in the `# Agent Roles` section of `CLAUDE.md` references a `.example.md` file
2. Any real agent file (the one being injected into agents) contains `.example` in its name
3. The tmux session name does not follow `<project-kebab>-agent_team_tmux` format
4. Any agent role file name does not follow `<project-kebab>-agent_<role>.md` format

Fix all violations before continuing.

---

## Step 1 — Read the Team Roster

Open `CLAUDE.md` and find the `# Agent Roles` section.
Count how many `@import` lines exist in that section. That number is the total agent count and determines how many panes to create.
Note the exact file path each import points to — you will need these in later steps.

---

## Step 2 — Enforce No .example In Imports

Scan every `@import` line in the `# Agent Roles` section of `CLAUDE.md`.
If any import path contains `.example`, STOP — fix it before proceeding.

Real imports must look like:
```
@import .claude/agents/<project-kebab>-agent_frontend.md
@import .claude/agents/<project-kebab>-agent_backend.md
@import .claude/agents/<project-kebab>-agent_tester.md
```

Not like:
```
@import .claude/agents/teammate-a.example.md   ← FORBIDDEN
```

---

## Step 3 — Check If Real Role Files Exist

For each agent import found in Step 1, check the file actually exists:

```bash
ls .claude/agents/
```

If a real role file is missing, copy from the matching `.example.md` template and rename it to follow the `<project-kebab>-agent_<role>.md` convention. Customize the content for this project.

Do NOT modify or delete any `.example.md` file — they are permanent templates.

---

## Step 4 — Update CLAUDE.md Imports If Needed

If any import in `CLAUDE.md` still points to a `.example.md` or an old generic name (e.g. `teammate-a.md`), update it to the correct project-specific name:

```
@import .claude/agents/<project-kebab>-agent_frontend.md
@import .claude/agents/<project-kebab>-agent_backend.md
@import .claude/agents/<project-kebab>-agent_tester.md
```

---

## Step 5 — Kill Any Existing Session

Derive the session name from the project folder name in kebab-case:
```
SESSION="<project-kebab>-agent_team_tmux"
```

Kill any leftover session:
```bash
wsl tmux kill-session -t $SESSION 2>/dev/null
```

No error if session does not exist — that is expected.

---

## Step 6 — Create New Tmux Session

```bash
wsl tmux new-session -d -s $SESSION -x 220 -y 50
```

---

## Step 7 — Split Into N Panes

Create one pane per agent. For 3 agents:

```bash
wsl tmux split-window -h -t $SESSION
wsl tmux split-window -v -t $SESSION:0.0
wsl tmux split-window -v -t $SESSION:0.1
wsl tmux select-layout -t $SESSION tiled
```

If agent count is different, adjust the number of `split-window` commands — one split per additional agent beyond the first.

Verify pane count matches agent count:
```bash
wsl tmux list-panes -t $SESSION
```

---

## Step 8 — Launch Claude In Each Pane

Launch claude from the project root in every pane:

```bash
PROJECT="<absolute-project-path>"

wsl tmux send-keys -t $SESSION:0.0 "cd \"$PROJECT\" && claude" Enter
wsl tmux send-keys -t $SESSION:0.1 "cd \"$PROJECT\" && claude" Enter
wsl tmux send-keys -t $SESSION:0.2 "cd \"$PROJECT\" && claude" Enter
```

---

## Step 9 — Wait 4 Seconds

Do not proceed until all claude instances have finished loading:

```bash
sleep 4
```

---

## Step 10 — Inject Roles

Send the force-read command to each pane using the real role file (never `.example`):

```bash
wsl tmux send-keys -t $SESSION:0.0 "Read .claude/agents/<project-kebab>-agent_frontend.md and follow it strictly as your role for this entire session." Enter
wsl tmux send-keys -t $SESSION:0.1 "Read .claude/agents/<project-kebab>-agent_backend.md and follow it strictly as your role for this entire session." Enter
wsl tmux send-keys -t $SESSION:0.2 "Read .claude/agents/<project-kebab>-agent_tester.md and follow it strictly as your role for this entire session." Enter
```

---

## Step 11 — Wait 5 Seconds

Allow each agent time to read and acknowledge their role:

```bash
sleep 5
```

---

## Step 12 — Verify Identities

Ask each pane to identify themselves:

```bash
wsl tmux send-keys -t $SESSION:0.0 "Who are you? One line only." Enter
wsl tmux send-keys -t $SESSION:0.1 "Who are you? One line only." Enter
wsl tmux send-keys -t $SESSION:0.2 "Who are you? One line only." Enter
```

Wait 3 seconds, then capture each pane:

```bash
sleep 3
wsl tmux capture-pane -t $SESSION:0.0 -p
wsl tmux capture-pane -t $SESSION:0.1 -p
wsl tmux capture-pane -t $SESSION:0.2 -p
```

Check that each agent's response matches the identity defined in their role doc.

---

## Step 13 — Handle Identity Failures

If any pane does not respond with the correct identity:

1. Re-send the role injection for that pane:
```bash
wsl tmux send-keys -t $SESSION:0.<pane> "Read .claude/agents/<project-kebab>-agent_<role>.md and follow it strictly as your role for this entire session." Enter
```
2. Wait 5 seconds
3. Re-verify:
```bash
wsl tmux send-keys -t $SESSION:0.<pane> "Who are you? One line only." Enter
sleep 3
wsl tmux capture-pane -t $SESSION:0.<pane> -p
```
4. If still incorrect after retry — report to user: "Pane `<pane>` failed identity check — manual intervention needed."

---

## Step 14 — Report To User

If all agents passed, report a summary table:

```
Team is ready.
Session: <project-kebab>-agent_team_tmux

| Pane | Agent file                          | Role               |
|------|-------------------------------------|--------------------|
| 0    | <project-kebab>-agent_frontend.md   | Frontend Developer |
| 1    | <project-kebab>-agent_backend.md    | Backend Developer  |
| 2    | <project-kebab>-agent_tester.md     | Tester             |
```

If any agent failed, list which panes need attention before proceeding.

---

## Notes

- Always use `session:window.pane` notation (e.g. `$SESSION:0.0`) — never use bare session name
- `<absolute-project-path>` must be the real full path — do not leave it as a placeholder
- `.example.md` files are permanent templates — never inject them into agents, never delete them
