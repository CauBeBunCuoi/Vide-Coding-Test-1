# FE — Add Task Entry Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Append one new sentence to `taskflow-fe-sample/my-tasks.txt`.

**Architecture:** Single file edit. Agent commits to its own worktree branch only — never to the base branch. Root agent applies the diff as unstaged edits via `git apply` after this task completes.

**Tech Stack:** Plain text file, Git.

> **CRITICAL WORKFLOW RULE:**
> You are running with `isolation: "worktree"`. Your commits MUST stay on your throwaway worktree branch.
> Do NOT checkout, reset, or push to `dev` or any base branch.
> The root agent handles applying your changes to the base branch after you finish.

---

### Task 1: Append a new sentence to my-tasks.txt

**Files:**
- Modify: `taskflow-fe-sample/my-tasks.txt`

- [ ] **Step 1: Read the current file to find the last line**

```bash
tail -5 taskflow-fe-sample/my-tasks.txt
```

Expected: last few lines of the existing content visible, no blank lines issue.

- [ ] **Step 2: Append the new sentence**

```bash
echo "" >> taskflow-fe-sample/my-tasks.txt
echo "The frontend build pipeline compiles TypeScript and bundles assets in under three seconds." >> taskflow-fe-sample/my-tasks.txt
```

- [ ] **Step 3: Verify the line was added**

```bash
tail -3 taskflow-fe-sample/my-tasks.txt
```

Expected output (last line):
```
The frontend build pipeline compiles TypeScript and bundles assets in under three seconds.
```

- [ ] **Step 4: Confirm you are on the worktree branch, NOT the base branch**

```bash
git branch --show-current
```

Expected: `worktree-agent-xxx` (your throwaway branch name, not `dev` or `main`).

If the output is `dev` or `main` — **stop immediately and report BLOCKED**.

- [ ] **Step 5: Commit to worktree branch**

```bash
git add taskflow-fe-sample/my-tasks.txt
git commit -m "feat(fe): add task entry to my-tasks.txt"
```

Expected: commit succeeds on the worktree branch. Do not push. Do not merge. Stop here.
