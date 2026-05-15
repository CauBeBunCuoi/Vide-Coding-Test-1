# Git Workflow

## Subagent Commit Handoff

Subagents always run with `isolation: "worktree"`. They commit to their
throwaway branch only — never directly to the base branch.

When a subagent finishes, the root agent:

1. Identify the base branch and worktree branch:
```bash
   BASE=$(git rev-parse --abbrev-ref HEAD)
   WORKTREE_BRANCH=worktree-agent-xxx
   WORKTREE_PATH=.claude/worktrees/agent-xxx
```

2. Apply changes as unstaged edits on the base branch:
```bash
   git diff $BASE..$WORKTREE_BRANCH > /tmp/task.patch
   git apply /tmp/task.patch
```

3. Clean up the throwaway:
```bash
   git worktree remove $WORKTREE_PATH
   git branch -D $WORKTREE_BRANCH
```

4. Stop here. Do not stage, commit, or push.

The user reviews unstaged changes in VS Code, then stages, commits, and pushes manually.

## Rules

- Never commit directly to the base branch on behalf of the user
- Never `git merge` a worktree branch — creates unwanted commit history
- Always use `git apply` to land changes as unstaged edits
- Always clean up worktree + branch after applying
- The user owns the commit — Claude Code owns the apply

## Why git apply, not git merge

`git merge` creates a commit automatically — review checkpoint bypassed.
`git apply` deposits raw file edits with no history — you decide if and when anything gets recorded.

## Defensive Checks (known Claude Code bugs)

Before applying, verify worktree isolation actually worked:

```bash
git worktree list                        # confirm agent worktree exists
git branch --show-current               # confirm still on base branch
git log $WORKTREE_BRANCH --oneline -5   # confirm agent committed to throwaway
```

If `worktreeBranch` undefined or agent committed to base branch — stop and report to user immediately.