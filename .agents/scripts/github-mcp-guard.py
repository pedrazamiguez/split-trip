#!/usr/bin/env python3
"""PreToolUse hook guard that blocks broken/disallowed GitHub MCP write tools."""

from __future__ import annotations

import json
import sys
from typing import Any

BLOCKED_ISSUE_WRITE_TOOLS: frozenset[str] = frozenset({"issue_write"})
BLOCKED_GIT_MUTATION_TOOLS: frozenset[str] = frozenset(
    {
        "create_pull_request",
        "merge_pull_request",
        "push_files",
        "create_or_update_file",
        "delete_file",
        "create_branch",
        "create_repository",
        "fork_repository",
        "update_pull_request_branch",
    }
)


def extract_github_tool_name(payload: dict[str, Any]) -> str | None:
    """Extract the underlying GitHub MCP tool name from a PreToolUse payload."""
    tool_call = payload.get("toolCall")
    if not isinstance(tool_call, dict):
        return None

    tool_name = str(tool_call.get("name") or "").strip()
    raw_args = tool_call.get("args")
    args = raw_args if isinstance(raw_args, dict) else {}

    if tool_name == "call_mcp_tool":
        server = str(args.get("ServerName") or args.get("serverName") or "").strip().lower()
        mcp_tool = str(args.get("ToolName") or args.get("toolName") or "").strip()
        if server in {"github", "github-mcp-server"} and mcp_tool:
            return mcp_tool
        return None

    if tool_name.startswith("mcp_github_"):
        return tool_name.removeprefix("mcp_github_")

    return None


def evaluate_tool_call(payload: dict[str, Any]) -> dict[str, str]:
    """Evaluate a PreToolUse payload and return an allow or deny decision."""
    github_tool = extract_github_tool_name(payload)
    if github_tool is None:
        return {"decision": "allow"}

    if github_tool in BLOCKED_ISSUE_WRITE_TOOLS:
        return {
            "decision": "deny",
            "reason": (
                f"GitHub MCP '{github_tool}' is disabled because the host interactive form "
                "fails to render. Use `gh issue create`, `gh issue edit`, or `gh issue close` "
                "via `run_command` (e.g., `gh issue create --repo pedrazamiguez/split-trip "
                "--title \"...\" --body-file - <<'EOF'`) and verify exit code 0 plus the "
                "returned https://github.com/pedrazamiguez/split-trip/ URL."
            ),
        }

    if github_tool in BLOCKED_GIT_MUTATION_TOOLS:
        return {
            "decision": "deny",
            "reason": (
                f"GitHub MCP '{github_tool}' is blocked by the repository's strict "
                "\"No Git Operations\" policy. Never branch, push, merge, mutate files, "
                "or create PRs autonomously."
            ),
        }

    return {"decision": "allow"}


def main() -> int:
    """Read PreToolUse JSON from stdin, evaluate it, and print decision JSON."""
    raw_input = sys.stdin.read()
    if not raw_input.strip():
        print(json.dumps({"decision": "allow"}))
        return 0

    try:
        payload = json.loads(raw_input)
    except json.JSONDecodeError:
        print(json.dumps({"decision": "allow"}))
        return 0

    if not isinstance(payload, dict):
        print(json.dumps({"decision": "allow"}))
        return 0

    result = evaluate_tool_call(payload)
    print(json.dumps(result))
    return 0


if __name__ == "__main__":
    sys.exit(main())
