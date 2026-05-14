# Superpowers + Vibe Template — Prompt Playbook

---

## 0 — Rejoin an existing session

```
Run the init-team pipeline. Team is already set up — just re-spawn 
teammates and confirm their roles before we continue.
```

---

## 1 — Start a new feature

> Triggers: `superpowers:brainstorming`

```
I want to build [feature name].

[One paragraph describing what it does, who uses it, and any 
constraints you already know.]

Use superpowers:brainstorming before anything else. Ask me questions 
one at a time. Do not touch any code until I approve the design.
```

**Example:**
```
I want to add a task assignment feature. Users can assign any task 
to a teammate, and the assignee gets an in-app notification with 
a link to the task.

Use superpowers:brainstorming before anything else. Ask me questions 
one at a time. Do not touch any code until I approve the design.
```

---

## 2 — Approve spec and create plans

> Triggers: `superpowers:writing-plans`

```
The spec looks good. Approve it.

Use superpowers:writing-plans to create three separate plans:
- One for Teammate A (frontend work only)
- One for Teammate B (backend work only)  
- One for Teammate C (tests and verification only)

Save each plan to docs/superpowers/plans/YYYY-MM-DD-[feature]-[role].md.
Tasks must be 2–5 minutes each with exact file paths and verification steps.
```

---

## 3 — Execute (delegate to teammates)

> Triggers: `superpowers:subagent-driven-development`

```
Plans are approved. Use superpowers:subagent-driven-development to 
delegate each plan to the correct teammate:

- docs/superpowers/plans/[date]-[feature]-frontend.md → Teammate A
- docs/superpowers/plans/[date]-[feature]-backend.md  → Teammate B
- docs/superpowers/plans/[date]-[feature]-test.md     → Teammate C

Each teammate must use superpowers:test-driven-development during 
their tasks. Run all three in parallel. Do not pause between tasks 
unless blocked.
```

---

## 4 — Unblock a stuck teammate

> Triggers: `superpowers:systematic-debugging`

```
Teammate [A/B/C] is stuck on [describe the problem briefly].

Use superpowers:systematic-debugging to find the root cause. 
Do not change any code until the root cause is confirmed.
```

---

## 5 — Request code review

> Triggers: `superpowers:requesting-code-review`

```
All tasks are done. Use superpowers:requesting-code-review against 
the original spec at docs/superpowers/plans/[spec-file].md.

Block on any critical issues. Do not proceed to merge until 
all critical issues are resolved.
```

---

## 6 — Handle review feedback

> Triggers: `superpowers:receiving-code-review`

```
Review feedback has come in. Use superpowers:receiving-code-review 
before making any changes. Do not implement anything until we have 
agreed on the approach.
```

---

## 7 — Verify before closing

> Triggers: `superpowers:verification-before-completion`

```
Use superpowers:verification-before-completion. Confirm everything 
is actually working — do not take the implementer's word for it. 
Run the tests yourself and check the output.
```

---

## 8 — Finish the branch

> Triggers: `superpowers:finishing-a-development-branch`

```
All checks passed. Use superpowers:finishing-a-development-branch 
and present the options: merge to main, open a PR, or discard.
```

---

## Bonus — Add new requirements mid-project

```
New requirements have been added to .claude/requirements/. 
Run the apply-requirements pipeline to distribute them to 
taskflow-fe, taskflow-be, and taskflow-test subprojects.
Do not re-run init-team or init-worktrees.
```

---

## Bonus — Custom skill for this project

```
Use superpowers:writing-skills to create a new skill called 
[skill-name] for [describe what it should enforce].
Save it to [subproject]/.claude/skills/[skill-name]/SKILL.md.
Test it with a subagent before reporting done.
```

---

## Rules to remember

- Always mention the full `superpowers:skill-name` — never abbreviated
- Never skip brainstorming, even for small features
- "Fix this bug" → use `superpowers:systematic-debugging`, not `superpowers:brainstorming`
- Skills auto-trigger but explicit names are more reliable
- Your CLAUDE.md rules always override Superpowers skills