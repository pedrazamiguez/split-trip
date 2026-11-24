"""Unit tests for .agents/scripts/github-mcp-guard.py, hooks, rules, AGENTS.md, and skills."""

from __future__ import annotations

import importlib.util
import json
import re
import subprocess
import sys
import unittest
from pathlib import Path
from typing import Any

REPO_ROOT = Path(__file__).resolve().parent.parent
GUARD_SCRIPT = REPO_ROOT / ".agents" / "scripts" / "github-mcp-guard.py"
HOOKS_JSON = REPO_ROOT / ".agents" / "hooks.json"
GITHUB_RULE = REPO_ROOT / ".agents" / "rules" / "github-operations.md"
AGENTS_MD = REPO_ROOT / "AGENTS.md"
SKILLS_DIR = REPO_ROOT / ".agents" / "skills"


def _load_guard_module() -> Any:
    spec = importlib.util.spec_from_file_location("github_mcp_guard", GUARD_SCRIPT)
    assert spec is not None and spec.loader is not None
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


class TestGithubMcpGuardAndRules(unittest.TestCase):
    """Validate PreToolUse guard decisions, hook/rule configs, AGENTS.md cap, and skills."""

    @classmethod
    def setUpClass(cls) -> None:
        cls.guard = _load_guard_module()

    def test_denies_call_mcp_tool_github_issue_write(self) -> None:
        payload = {
            "toolCall": {
                "name": "call_mcp_tool",
                "args": {
                    "ServerName": "github",
                    "ToolName": "issue_write",
                    "Arguments": {"method": "create"},
                },
            }
        }
        result = self.guard.evaluate_tool_call(payload)
        self.assertEqual(result["decision"], "deny")
        self.assertIn("gh", result["reason"])
        self.assertIn("issue_write", result["reason"])

    def test_denies_call_mcp_tool_camel_case_and_server_alias(self) -> None:
        payload = {
            "toolCall": {
                "name": "call_mcp_tool",
                "args": {
                    "serverName": "github-mcp-server",
                    "toolName": "issue_write",
                },
            }
        }
        result = self.guard.evaluate_tool_call(payload)
        self.assertEqual(result["decision"], "deny")

    def test_denies_eager_mcp_github_issue_write(self) -> None:
        payload = {
            "toolCall": {
                "name": "mcp_github_issue_write",
                "args": {},
            }
        }
        result = self.guard.evaluate_tool_call(payload)
        self.assertEqual(result["decision"], "deny")

    def test_denies_autonomous_git_mutation_mcp_tools(self) -> None:
        git_mutation_tools = (
            "create_pull_request",
            "merge_pull_request",
            "push_files",
            "create_or_update_file",
            "delete_file",
            "create_branch",
        )
        for tool_name in git_mutation_tools:
            with self.subTest(tool_name=tool_name):
                payload = {
                    "toolCall": {
                        "name": "call_mcp_tool",
                        "args": {
                            "ServerName": "github",
                            "ToolName": tool_name,
                        },
                    }
                }
                result = self.guard.evaluate_tool_call(payload)
                self.assertEqual(result["decision"], "deny")
                self.assertIn("No Git Operations", result["reason"])

    def test_allows_readonly_github_mcp_tools(self) -> None:
        readonly_tools = (
            "issue_read",
            "list_issues",
            "search_issues",
            "pull_request_read",
        )
        for tool_name in readonly_tools:
            with self.subTest(tool_name=tool_name):
                payload = {
                    "toolCall": {
                        "name": "call_mcp_tool",
                        "args": {
                            "ServerName": "github",
                            "ToolName": tool_name,
                        },
                    }
                }
                result = self.guard.evaluate_tool_call(payload)
                self.assertEqual(result, {"decision": "allow"})

    def test_allows_pr_review_and_comment_mcp_tools(self) -> None:
        allowed_write_tools = (
            "pull_request_review_write",
            "add_comment_to_pending_review",
            "add_reply_to_pull_request_comment",
            "add_issue_comment",
        )
        for tool_name in allowed_write_tools:
            with self.subTest(tool_name=tool_name):
                payload = {
                    "toolCall": {
                        "name": "call_mcp_tool",
                        "args": {
                            "ServerName": "github",
                            "ToolName": tool_name,
                        },
                    }
                }
                result = self.guard.evaluate_tool_call(payload)
                self.assertEqual(result, {"decision": "allow"})

    def test_allows_non_github_mcp_servers(self) -> None:
        other_calls = (
            ("firebase", "crashlytics_get_report"),
            ("sonarqube", "issues"),
        )
        for server_name, tool_name in other_calls:
            with self.subTest(server=server_name, tool=tool_name):
                payload = {
                    "toolCall": {
                        "name": "call_mcp_tool",
                        "args": {
                            "ServerName": server_name,
                            "ToolName": tool_name,
                        },
                    }
                }
                result = self.guard.evaluate_tool_call(payload)
                self.assertEqual(result, {"decision": "allow"})

    def test_cli_subprocess_blocks_issue_write_and_fails_open_on_bad_json(self) -> None:
        issue_write_payload = json.dumps(
            {
                "toolCall": {
                    "name": "call_mcp_tool",
                    "args": {"ServerName": "github", "ToolName": "issue_write"},
                }
            }
        )
        deny_proc = subprocess.run(
            [sys.executable, str(GUARD_SCRIPT)],
            input=issue_write_payload,
            text=True,
            capture_output=True,
            check=False,
        )
        self.assertEqual(deny_proc.returncode, 0)
        deny_output = json.loads(deny_proc.stdout)
        self.assertEqual(deny_output["decision"], "deny")

        bad_proc = subprocess.run(
            [sys.executable, str(GUARD_SCRIPT)],
            input="not-json",
            text=True,
            capture_output=True,
            check=False,
        )
        self.assertEqual(bad_proc.returncode, 0)
        bad_output = json.loads(bad_proc.stdout)
        self.assertEqual(bad_output, {"decision": "allow"})

    def test_hooks_json_valid_and_script_exists(self) -> None:
        self.assertTrue(GUARD_SCRIPT.is_file(), "github-mcp-guard.py must exist")
        hooks_data = json.loads(HOOKS_JSON.read_text(encoding="utf-8"))
        guard_cfg = hooks_data["github-mcp-guard"]
        self.assertIs(guard_cfg["enabled"], True)
        pre_tool_use = guard_cfg["PreToolUse"]
        self.assertGreaterEqual(len(pre_tool_use), 1)
        matcher = pre_tool_use[0]["matcher"]
        self.assertIsNotNone(re.fullmatch(matcher, "call_mcp_tool"))
        self.assertIsNotNone(re.fullmatch(matcher, "mcp_github_issue_write"))

    def test_agents_md_under_24000_bytes_and_complete(self) -> None:
        content_bytes = AGENTS_MD.read_bytes()
        self.assertLessEqual(len(content_bytes), 24000)
        content_text = content_bytes.decode("utf-8")
        self.assertIn("## AI Agent Behavior Rules (CRITICAL)", content_text)
        self.assertIn("## Make Check Gate", content_text)
        self.assertIn("## String Translations & Localization", content_text)

    def test_github_operations_rule_has_always_on_frontmatter(self) -> None:
        rule_text = GITHUB_RULE.read_text(encoding="utf-8")
        self.assertTrue(rule_text.startswith("---\n"))
        frontmatter_end = rule_text.find("\n---\n", 4)
        self.assertGreater(frontmatter_end, 0)
        frontmatter = rule_text[:frontmatter_end]
        self.assertIn("trigger: always_on", frontmatter)
        self.assertIn("gh", rule_text)
        self.assertIn("https://github.com/", rule_text)

    def test_skills_use_gh_cli_and_forbid_issue_write(self) -> None:
        create_skills = ("create-issue", "investigate-production-incident")
        for skill_name in create_skills:
            with self.subTest(skill=skill_name):
                text = (SKILLS_DIR / skill_name / "SKILL.md").read_text(encoding="utf-8")
                self.assertIn("gh issue create", text)
                self.assertNotIn("call `issue_write`", text)
                self.assertNotIn("via `issue_write`", text)
                self.assertIn("https://github.com/", text)

        comment_skills = (
            "plan-issue",
            "replan-issue",
            "start-issue",
            "follow-up",
            "feasibility-study",
        )
        for skill_name in comment_skills:
            with self.subTest(skill=skill_name):
                text = (SKILLS_DIR / skill_name / "SKILL.md").read_text(encoding="utf-8")
                self.assertIn("gh issue comment", text)
                self.assertIn("https://github.com/", text)


if __name__ == "__main__":
    unittest.main()
