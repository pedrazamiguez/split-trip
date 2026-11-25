---
trigger: always_on
description: Enforce gh CLI for GitHub issue writes, require URL/exit-code verification, and keep read-only MCP lookups.
---

## GitHub Operations & Verification Rules

1. **Issue Creation & Updates (`gh` CLI Mandatory):** Never invoke `github` MCP `issue_write`. Always use `gh issue create`, `gh issue edit`, or `gh issue close` via `run_command` against `pedrazamiguez/split-trip`. Pass multi-line bodies via `--body-file - <<'EOF'` to avoid shell-escaping issues and temporary files.
2. **Issue Comments (`gh` CLI Preferred + Immediate Fallback):** Use `gh issue comment <number> --repo pedrazamiguez/split-trip --body-file - <<'EOF'` via `run_command`. If any GitHub MCP write call errors, omits a URL, or surfaces an interactive form message, immediately execute the equivalent `gh` command via `run_command` instead of reporting success or waiting for a UI button.
3. **Strict Exit-Code & URL Verification Gate:** Never report that a GitHub issue or comment was created or updated unless the command exited with code `0` AND stdout contains an actual `https://github.com/pedrazamiguez/split-trip/...` URL.
4. **Workspace Resolution & Read-Only Lookups:** Infer `pedrazamiguez/split-trip` from `.git/config` / `git remote -v` without prompting the user for URLs. Read-only `github` MCP tools (`issue_read`, `list_issues`, `search_issues`, `pull_request_read`) and `gh issue view` / `gh pr view` remain allowed.
5. **No Autonomous Git Operations:** Never stage, commit, push, branch, merge, or create PRs autonomously (enforced by policy and the `PreToolUse` hook).
