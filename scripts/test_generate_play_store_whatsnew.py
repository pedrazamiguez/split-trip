#!/usr/bin/env python3
"""Unit tests for scripts/generate_play_store_whatsnew.py."""

from __future__ import annotations

import io
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

from scripts.generate_play_store_whatsnew import (
    DEFAULT_WHATSNEW_EN,
    DEFAULT_WHATSNEW_ES,
    MAX_WHATSNEW_CHARS,
    PullRequestSummary,
    clean_pr_title,
    extract_curated_notes_from_markdown,
    extract_linked_issue_number,
    filter_user_facing_prs,
    main,
    merge_and_deduplicate_prs,
    parse_pull_requests,
    resolve_whatsnew,
    truncate_whatsnew,
)


class TestGeneratePlayStoreWhatsnew(unittest.TestCase):
    def test_excludes_sync_config_release_and_sync_title_prs(self) -> None:
        raw_json = json.dumps(
            [
                {"number": 10, "title": "Sync: Backmerge main to develop", "labels": [{"name": "sync"}]},
                {"number": 11, "title": "[SPLTRP-1795]: Fix workflow tracks", "labels": [{"name": "config"}]},
                {"number": 12, "title": "[SPLTRP-1792]: Release v0.115.1", "labels": [{"name": "release"}]},
                {"number": 13, "title": "Bump actions/checkout from 5 to 6", "labels": [{"name": "dependencies"}]},
                {"number": 14, "title": "Sync: Backmerge main to develop", "labels": [{"name": "bug"}]},
                {"number": 15, "title": "[SPLTRP-1800]: Add expense export", "labels": [{"name": "feature"}]},
            ]
        )
        prs = parse_pull_requests(raw_json)
        filtered = filter_user_facing_prs(prs)
        self.assertEqual([pr.number for pr in filtered], [15])

    def test_excludes_mixed_label_pr_with_excluded_label(self) -> None:
        prs = [
            PullRequestSummary(number=1, title="Config with feature label", labels=("feature", "config")),
            PullRequestSummary(number=2, title="Sync with bug label", labels=("bug", "sync")),
            PullRequestSummary(number=3, title="Release with hotfix label", labels=("hotfix", "release")),
        ]
        self.assertEqual(filter_user_facing_prs(prs), [])

    def test_combines_and_deduplicates_develop_and_main_prs_including_hotfix(self) -> None:
        develop_prs = [
            PullRequestSummary(number=20, title="[SPLTRP-1802]: Split wizard", labels=("feature",)),
            PullRequestSummary(number=18, title="[SPLTRP-1801]: Faster sync", labels=("enhancement",)),
        ]
        main_prs = [
            PullRequestSummary(number=22, title="[SPLTRP-1805]: Critical crash fix", labels=("hotfix",)),
            PullRequestSummary(number=21, title="[SPLTRP-1804]: Release v0.116.0", labels=("release",)),
            PullRequestSummary(number=20, title="[SPLTRP-1802]: Split wizard", labels=("feature",)),
        ]
        merged = merge_and_deduplicate_prs(develop_prs, main_prs)
        filtered = filter_user_facing_prs(merged)
        self.assertEqual([pr.number for pr in filtered], [18, 20, 22])

    def test_clean_pr_title_strips_issue_prefixes(self) -> None:
        self.assertEqual(clean_pr_title("[SPLTRP-1923]: Redirect to Login"), "Redirect to Login")
        self.assertEqual(clean_pr_title("[EXSHAPP-0042] Fix currency"), "Fix currency")
        self.assertEqual(clean_pr_title("[SPLTRP-12345]: Five digit ticket"), "Five digit ticket")
        self.assertEqual(clean_pr_title("Plain title without prefix"), "Plain title without prefix")

    def test_fallback_to_bilingual_defaults_when_only_internal_prs_exist(self) -> None:
        develop_prs = [
            PullRequestSummary(number=1, title="Sync: Backmerge main to develop", labels=("sync",)),
            PullRequestSummary(number=2, title="[SPLTRP-1798]: CI update", labels=("config",)),
        ]
        main_prs = [
            PullRequestSummary(number=3, title="[SPLTRP-1799]: Release", labels=("release",)),
        ]
        with tempfile.TemporaryDirectory() as tmp:
            resolved = resolve_whatsnew(Path(tmp), develop_prs, main_prs)
            self.assertEqual(resolved.en_us, DEFAULT_WHATSNEW_EN)
            self.assertEqual(resolved.es_es, DEFAULT_WHATSNEW_ES)
            self.assertEqual(resolved.en_source, "localized-fallback")
            self.assertEqual(resolved.es_source, "localized-fallback")

    def test_spanish_never_receives_raw_english_pr_titles(self) -> None:
        develop_prs = [
            PullRequestSummary(
                number=101,
                title="[SPLTRP-1901]: Add multi-currency receipt scanner",
                labels=("feature",),
            ),
            PullRequestSummary(
                number=102,
                title="[SPLTRP-1902]: Fix offline room transaction deadlock",
                labels=("bug",),
            ),
            PullRequestSummary(
                number=103,
                title="[SPLTRP-1903]: Optimize compose balance list",
                labels=("enhancement",),
            ),
        ]
        with tempfile.TemporaryDirectory() as tmp:
            resolved = resolve_whatsnew(Path(tmp), develop_prs, [])
            self.assertIn("Add multi-currency receipt scanner", resolved.en_us)
            self.assertIn("Fix offline room transaction deadlock", resolved.en_us)
            self.assertEqual(resolved.en_source, "auto-pr-titles")

            self.assertNotIn("Add multi-currency", resolved.es_es)
            self.assertNotIn("Fix offline room", resolved.es_es)
            self.assertNotIn("Optimize compose", resolved.es_es)
            self.assertEqual(
                resolved.es_es,
                "\n".join(
                    [
                        "- Nuevas funcionalidades y mejoras.",
                        "- Corrección de errores y mejoras de estabilidad.",
                        "- Mejoras de rendimiento y experiencia de usuario.",
                    ]
                ),
            )
            self.assertEqual(resolved.es_source, "localized-summary")

    def test_curated_files_take_highest_precedence(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            whatsnew_dir = Path(tmp)
            (whatsnew_dir / "whatsnew-en-US").write_text("- Curated file EN note.", encoding="utf-8")
            (whatsnew_dir / "whatsnew-es-ES").write_text("- Nota curada en archivo ES.", encoding="utf-8")

            pr_body = "#### en-US\n- From PR EN\n#### es-ES\n- Desde PR ES"
            issue_body = "### Release Notes / Context\n#### en-US\n- From Issue EN\n#### es-ES\n- Desde Issue ES"
            prs = [PullRequestSummary(number=1, title="[SPLTRP-1]: Feature X", labels=("feature",))]

            resolved = resolve_whatsnew(whatsnew_dir, prs, [], pr_body=pr_body, issue_body=issue_body)
            self.assertEqual(resolved.en_us, "- Curated file EN note.")
            self.assertEqual(resolved.es_es, "- Nota curada en archivo ES.")
            self.assertEqual(resolved.en_source, "curated-file")
            self.assertEqual(resolved.es_source, "curated-file")

    def test_partial_curated_file_override_does_not_leak_english_to_spanish(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            whatsnew_dir = Path(tmp)
            (whatsnew_dir / "whatsnew-en-US").write_text("- Custom English only file.", encoding="utf-8")
            prs = [PullRequestSummary(number=1, title="[SPLTRP-1]: Feature X", labels=("feature",))]

            resolved = resolve_whatsnew(whatsnew_dir, prs, [])
            self.assertEqual(resolved.en_us, "- Custom English only file.")
            self.assertEqual(resolved.en_source, "curated-file")
            self.assertEqual(resolved.es_es, "- Nuevas funcionalidades y mejoras.")
            self.assertEqual(resolved.es_source, "localized-summary")

    def test_pr_body_curated_bilingual_notes_override_issue_and_auto(self) -> None:
        pr_body = (
            "## 📝 Summary\nRelease PR\n\n"
            "### Release Notes\n"
            "#### en-US\n- PR English bullet\n"
            "#### es-ES\n- Viñeta en español desde PR\n"
        )
        issue_body = (
            "### Release Notes / Context\n"
            "#### en-US\n- Issue English bullet\n"
            "#### es-ES\n- Viñeta desde Issue\n"
        )
        prs = [PullRequestSummary(number=1, title="[SPLTRP-1]: Auto feature", labels=("feature",))]
        with tempfile.TemporaryDirectory() as tmp:
            resolved = resolve_whatsnew(Path(tmp), prs, [], pr_body=pr_body, issue_body=issue_body)
            self.assertEqual(resolved.en_us, "- PR English bullet")
            self.assertEqual(resolved.es_es, "- Viñeta en español desde PR")
            self.assertEqual(resolved.en_source, "pr-body")
            self.assertEqual(resolved.es_source, "pr-body")

        inline_pr_body = "en-US: - Inline EN note\nes-ES: - Nota inline ES"
        notes = extract_curated_notes_from_markdown(inline_pr_body)
        self.assertEqual(notes.en_us, "- Inline EN note")
        self.assertEqual(notes.es_es, "- Nota inline ES")

    def test_ignores_standard_pr_and_issue_template_boilerplate(self) -> None:
        auto_pr_body = "Automated PR for release. Closes #1926"
        template_pr_body = (
            "## 📝 Summary\n"
            "<!-- Brief description -->\n"
            "Bumped version.\n\n"
            "## 🔗 Related Issues\n"
            "Closes #1926\n\n"
            "## 🧪 Testing Instructions\n"
            "- Verified locally\n"
        )
        empty_issue_body = (
            "### Release Type\n"
            "- [ ] Major Release (Breaking Changes)\n\n"
            "### Release Notes / Context\n"
            "_No response_\n"
        )
        self.assertEqual(extract_curated_notes_from_markdown(auto_pr_body).en_us, None)
        self.assertEqual(extract_curated_notes_from_markdown(auto_pr_body).es_es, None)
        self.assertEqual(extract_curated_notes_from_markdown(template_pr_body).en_us, None)
        self.assertEqual(extract_curated_notes_from_markdown(template_pr_body).es_es, None)
        self.assertEqual(extract_curated_notes_from_markdown(empty_issue_body).en_us, None)
        self.assertEqual(extract_curated_notes_from_markdown(empty_issue_body).es_es, None)

    def test_issue_body_curated_notes_override_auto_extraction(self) -> None:
        issue_body = (
            "### Release Type\n"
            "- [ ] Major Release (Breaking Changes)\n\n"
            "### Release Notes / Context\n"
            "#### en-US\n"
            "- Faster group expense synchronization.\n"
            "#### es-ES\n"
            "- Sincronización más rápida de gastos de grupo.\n"
        )
        prs = [PullRequestSummary(number=1, title="[SPLTRP-1]: Raw title", labels=("feature",))]
        with tempfile.TemporaryDirectory() as tmp:
            resolved = resolve_whatsnew(
                Path(tmp),
                prs,
                [],
                pr_body="Automated PR for release. Closes #1792",
                issue_body=issue_body,
            )
            self.assertEqual(resolved.en_us, "- Faster group expense synchronization.")
            self.assertEqual(resolved.es_es, "- Sincronización más rápida de gastos de grupo.")
            self.assertEqual(resolved.en_source, "issue-body")
            self.assertEqual(resolved.es_source, "issue-body")

    def test_issue_body_untagged_release_notes_populate_english_only(self) -> None:
        issue_body = (
            "### Release Notes / Context\n"
            "- Improved offline expense creation.\n"
        )
        prs = [PullRequestSummary(number=1, title="[SPLTRP-1]: Raw bug", labels=("bug",))]
        with tempfile.TemporaryDirectory() as tmp:
            resolved = resolve_whatsnew(Path(tmp), prs, [], issue_body=issue_body)
            self.assertEqual(resolved.en_us, "- Improved offline expense creation.")
            self.assertEqual(resolved.en_source, "issue-body")
            self.assertEqual(resolved.es_es, "- Corrección de errores y mejoras de estabilidad.")
            self.assertEqual(resolved.es_source, "localized-summary")

    def test_extract_linked_issue_number_variants(self) -> None:
        self.assertEqual(
            extract_linked_issue_number("Automated PR for release. Closes #1926"),
            1926,
        )
        self.assertEqual(extract_linked_issue_number("fixes #42"), 42)
        self.assertEqual(
            extract_linked_issue_number("", pr_title="[SPLTRP-1792]: Release - v0.115.1"),
            1792,
        )
        self.assertIsNone(extract_linked_issue_number("", pr_title="Manual commit"))

    def test_truncate_whatsnew_respects_500_char_limit_at_line_boundaries(self) -> None:
        lines = [
            f"- Mejora número {idx}: optimización de sincronización y cálculos con precisión decimal áéíóúñ."
            for idx in range(1, 10)
        ]
        long_multiline = "\n".join(lines)
        self.assertGreater(len(long_multiline), MAX_WHATSNEW_CHARS)

        truncated = truncate_whatsnew(long_multiline, MAX_WHATSNEW_CHARS)
        self.assertLessEqual(len(truncated), MAX_WHATSNEW_CHARS)
        self.assertTrue(truncated.endswith("áéíóúñ."))

        single_overlong_line = "- " + ("palabra " * 80).strip()
        self.assertGreater(len(single_overlong_line), MAX_WHATSNEW_CHARS)
        truncated_single = truncate_whatsnew(single_overlong_line, MAX_WHATSNEW_CHARS)
        self.assertLessEqual(len(truncated_single), MAX_WHATSNEW_CHARS)
        self.assertTrue(truncated_single.endswith("..."))

    def test_cli_main_writes_files_and_prints_linked_issue(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            tmp_dir = Path(tmp)
            pr_json_file = tmp_dir / "commit_prs.json"
            pr_body_file = tmp_dir / "pr_body.md"
            issue_body_file = tmp_dir / "issue_body.md"
            develop_prs_file = tmp_dir / "develop_prs.json"
            main_prs_file = tmp_dir / "main_prs.json"
            github_output = tmp_dir / "github_output.txt"
            whatsnew_dir = tmp_dir / "whatsnew"

            pr_json_file.write_text(
                json.dumps(
                    [
                        {
                            "number": 1927,
                            "title": "[SPLTRP-1926]: Release - v0.123.0",
                            "body": "Automated PR for release. Closes #1926",
                            "labels": [{"name": "release"}],
                            "base": {"ref": "main"},
                        }
                    ]
                ),
                encoding="utf-8",
            )

            stdout_buf = io.StringIO()
            with patch("sys.stdout", stdout_buf):
                rc = main(
                    [
                        "--print-linked-issue",
                        "--pr-json-file",
                        str(pr_json_file),
                        "--pr-body-file",
                        str(pr_body_file),
                    ]
                )
            self.assertEqual(rc, 0)
            self.assertEqual(stdout_buf.getvalue().strip(), "1926")

            develop_prs_file.write_text(
                json.dumps(
                    [
                        {"number": 1923, "title": "[SPLTRP-1923]: Redirect to Login", "labels": [{"name": "bug"}]},
                        {"number": 1925, "title": "Sync: Backmerge main to develop", "labels": [{"name": "sync"}]},
                    ]
                ),
                encoding="utf-8",
            )
            main_prs_file.write_text("[]", encoding="utf-8")
            issue_body_file.write_text("### Release Notes / Context\n_No response_\n", encoding="utf-8")

            rc2 = main(
                [
                    "--whatsnew-dir",
                    str(whatsnew_dir),
                    "--develop-prs-file",
                    str(develop_prs_file),
                    "--main-prs-file",
                    str(main_prs_file),
                    "--pr-body-file",
                    str(pr_body_file),
                    "--issue-body-file",
                    str(issue_body_file),
                    "--github-output",
                    str(github_output),
                ]
            )
            self.assertEqual(rc2, 0)
            en_text = (whatsnew_dir / "whatsnew-en-US").read_text(encoding="utf-8")
            es_text = (whatsnew_dir / "whatsnew-es-ES").read_text(encoding="utf-8")
            self.assertEqual(en_text, "- Redirect to Login")
            self.assertEqual(es_text, "- Corrección de errores y mejoras de estabilidad.")

            output_lines = github_output.read_text(encoding="utf-8")
            self.assertIn("EN_SOURCE=auto-pr-titles", output_lines)
            self.assertIn("ES_SOURCE=localized-summary", output_lines)


if __name__ == "__main__":
    unittest.main()
