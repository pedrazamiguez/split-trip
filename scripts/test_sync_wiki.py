#!/usr/bin/env python3
"""Unit tests for scripts/sync_wiki.py."""

from __future__ import annotations

import contextlib
import io
from pathlib import Path
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parent))

from sync_wiki import (
    DEFAULT_BRANCH,
    DEFAULT_REPOSITORY,
    build_slug_map,
    discover_doc_files,
    generate_sidebar,
    main,
    resolve_relative_target,
    rewrite_markdown_links,
    sync_docs_to_wiki,
)


def _write_file(path: Path, content: str = "# Placeholder\n") -> Path:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")
    return path


class TestSyncWiki(unittest.TestCase):
    def test_discovers_docs_excluding_release_assets(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            docs_dir = Path(tmp) / "docs"
            readme = _write_file(docs_dir / "README.md", "# Docs\n")
            foo = _write_file(docs_dir / "domain" / "foo.md", "# Foo\n")
            adr = _write_file(docs_dir / "architecture" / "adrs" / "0001-bar.md", "# ADR\n")
            _write_file(docs_dir / "release" / "assets" / "store.png", "binary")
            _write_file(docs_dir / "release" / "assets" / "notes.md", "# Ignored\n")

            discovered = discover_doc_files(docs_dir)

            self.assertEqual(
                discovered,
                [
                    readme.resolve(),
                    adr.resolve(),
                    foo.resolve(),
                ],
            )

    def test_duplicate_basename_raises_value_error(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            docs_dir = Path(tmp) / "docs"
            _write_file(docs_dir / "README.md", "# Docs\n")
            _write_file(docs_dir / "domain" / "guide.md", "# Domain Guide\n")
            _write_file(docs_dir / "engineering" / "guide.md", "# Engineering Guide\n")

            with self.assertRaises(ValueError) as ctx:
                discover_doc_files(docs_dir)

            self.assertIn("guide", str(ctx.exception))

    def test_reserved_wiki_filename_raises_value_error(self) -> None:
        for reserved_name in ("Home.md", "_Sidebar.md"):
            with self.subTest(reserved_name=reserved_name):
                with tempfile.TemporaryDirectory() as tmp:
                    docs_dir = Path(tmp) / "docs"
                    _write_file(docs_dir / "README.md", "# Docs\n")
                    _write_file(docs_dir / "domain" / reserved_name, "# Reserved\n")

                    with self.assertRaises(ValueError):
                        discover_doc_files(docs_dir)

    def test_rewrites_internal_docs_links_to_wiki_slugs_and_home(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            repo_root = Path(tmp)
            docs_dir = repo_root / "docs"
            readme = _write_file(docs_dir / "README.md", "# Hub\n")
            adr_0003 = _write_file(
                docs_dir / "architecture" / "adrs" / "0003-clean-architecture.md",
                "# ADR 0003\n",
            )
            adr_0004 = _write_file(
                docs_dir / "architecture" / "adrs" / "0004-mvi.md",
                (
                    "- [ADR-0003](0003-clean-architecture.md)\n"
                    "- [Offline](../patterns/offline-first.md)\n"
                    "- [Multi-Currency](../../domain/multi-currency-and-snapshots.md)\n"
                    "- [Hub](../../README.md)\n"
                    "- [Horizon](../../design-system/horizon-narrative-design.md#12-settings--child-screens-layout-standards)\n"
                ),
            )
            offline = _write_file(docs_dir / "architecture" / "patterns" / "offline-first.md")
            multi = _write_file(docs_dir / "domain" / "multi-currency-and-snapshots.md")
            horizon = _write_file(docs_dir / "design-system" / "horizon-narrative-design.md")
            _ = (readme, adr_0003, offline, multi, horizon)

            doc_files = discover_doc_files(docs_dir)
            slug_map = build_slug_map(doc_files, docs_dir)

            rewritten, count = rewrite_markdown_links(
                markdown=adr_0004.read_text(encoding="utf-8"),
                source_file=adr_0004,
                docs_dir=docs_dir,
                repo_root=repo_root,
                slug_map=slug_map,
                repository=DEFAULT_REPOSITORY,
                branch=DEFAULT_BRANCH,
            )

            self.assertEqual(count, 5)
            self.assertIn("[ADR-0003](0003-clean-architecture)", rewritten)
            self.assertIn("[Offline](offline-first)", rewritten)
            self.assertIn("[Multi-Currency](multi-currency-and-snapshots)", rewritten)
            self.assertIn("[Hub](Home)", rewritten)
            self.assertIn(
                "[Horizon](horizon-narrative-design#12-settings--child-screens-layout-standards)",
                rewritten,
            )

    def test_rewrites_non_wiki_repo_links_to_github_blob_urls(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            repo_root = Path(tmp)
            docs_dir = repo_root / "docs"
            _write_file(docs_dir / "README.md", "# Hub\n")
            _write_file(repo_root / "web" / "index.html", "<html></html>\n")
            _write_file(repo_root / "firebase" / "remoteconfig.template.json", "{}\n")
            _write_file(repo_root / "functions" / "src" / "config.ts", "export {};\n")
            _write_file(repo_root / ".github" / "workflows" / "codeql.yml", "name: CodeQL\n")

            source = _write_file(
                docs_dir / "architecture" / "adrs" / "0010-region.md",
                (
                    "- [Web](../../web/index.html)\n"
                    "- [RC](../../../firebase/remoteconfig.template.json)\n"
                    "- [Config](../../../functions/src/config.ts)\n"
                    "- [CodeQL](../../../.github/workflows/codeql.yml)\n"
                ),
            )
            # Also test 1-level doc link to ../web/index.html from docs/README.md
            readme_source = _write_file(
                docs_dir / "README.md",
                "- [Web](../web/index.html)\n",
            )
            _write_file(
                docs_dir / "engineering" / "guide.md",
                (
                    "- [RC](../../firebase/remoteconfig.template.json)\n"
                    "- [CodeQL](../../.github/workflows/codeql.yml)\n"
                ),
            )
            source.write_text("- [Config](../../../functions/src/config.ts)\n", encoding="utf-8")

            doc_files = discover_doc_files(docs_dir)
            slug_map = build_slug_map(doc_files, docs_dir)

            readme_out, _ = rewrite_markdown_links(
                readme_source.read_text(encoding="utf-8"),
                readme_source,
                docs_dir,
                repo_root,
                slug_map,
                DEFAULT_REPOSITORY,
                DEFAULT_BRANCH,
            )
            eng_file = docs_dir / "engineering" / "guide.md"
            eng_out, _ = rewrite_markdown_links(
                eng_file.read_text(encoding="utf-8"),
                eng_file,
                docs_dir,
                repo_root,
                slug_map,
                DEFAULT_REPOSITORY,
                DEFAULT_BRANCH,
            )
            adr_out, _ = rewrite_markdown_links(
                source.read_text(encoding="utf-8"),
                source,
                docs_dir,
                repo_root,
                slug_map,
                DEFAULT_REPOSITORY,
                DEFAULT_BRANCH,
            )

            self.assertIn(
                "https://github.com/pedrazamiguez/split-trip/blob/main/web/index.html",
                readme_out,
            )
            self.assertIn(
                "https://github.com/pedrazamiguez/split-trip/blob/main/firebase/remoteconfig.template.json",
                eng_out,
            )
            self.assertIn(
                "https://github.com/pedrazamiguez/split-trip/blob/main/.github/workflows/codeql.yml",
                eng_out,
            )
            self.assertIn(
                "https://github.com/pedrazamiguez/split-trip/blob/main/functions/src/config.ts",
                adr_out,
            )

    def test_rewrites_single_dotdot_repo_root_links_via_fallback(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            repo_root = Path(tmp)
            docs_dir = repo_root / "docs"
            _write_file(docs_dir / "README.md", "# Hub\n")
            user_kt = _write_file(repo_root / "domain" / "src" / "User.kt", "data class User(val id: String)\n")
            doc_file = _write_file(
                docs_dir / "domain" / "user-reconciliation.md",
                "See [User](../domain/src/User.kt).\n",
            )

            resolved = resolve_relative_target("../domain/src/User.kt", doc_file, docs_dir, repo_root)
            self.assertEqual(resolved, user_kt.resolve())

            doc_files = discover_doc_files(docs_dir)
            slug_map = build_slug_map(doc_files, docs_dir)
            rewritten, count = rewrite_markdown_links(
                doc_file.read_text(encoding="utf-8"),
                doc_file,
                docs_dir,
                repo_root,
                slug_map,
                DEFAULT_REPOSITORY,
                DEFAULT_BRANCH,
            )
            self.assertEqual(count, 1)
            self.assertEqual(
                rewritten,
                "See [User](https://github.com/pedrazamiguez/split-trip/blob/main/domain/src/User.kt).\n",
            )

    def test_preserves_code_blocks_inline_code_and_code_labels(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            repo_root = Path(tmp)
            docs_dir = repo_root / "docs"
            _write_file(docs_dir / "README.md", "# Hub\n")
            _write_file(repo_root / "domain" / "Symbol.kt", "class Symbol\n")
            doc_file = _write_file(
                docs_dir / "engineering" / "example.md",
                (
                    "```markdown\n"
                    "[a](b.md)\n"
                    "```\n\n"
                    "~~~\n"
                    "[a](b.md)\n"
                    "~~~\n\n"
                    "Inline `[a](b.md)` and [ext](https://example.com) and [sec](#local-anchor).\n"
                    "Code label: [`Symbol`](../../domain/Symbol.kt)\n"
                ),
            )

            doc_files = discover_doc_files(docs_dir)
            slug_map = build_slug_map(doc_files, docs_dir)
            rewritten, count = rewrite_markdown_links(
                doc_file.read_text(encoding="utf-8"),
                doc_file,
                docs_dir,
                repo_root,
                slug_map,
                DEFAULT_REPOSITORY,
                DEFAULT_BRANCH,
            )

            self.assertEqual(count, 1)
            self.assertIn("```markdown\n[a](b.md)\n```", rewritten)
            self.assertIn("~~~\n[a](b.md)\n~~~", rewritten)
            self.assertIn("Inline `[a](b.md)`", rewritten)
            self.assertIn("[ext](https://example.com)", rewritten)
            self.assertIn("[sec](#local-anchor)", rewritten)
            self.assertIn(
                "[`Symbol`](https://github.com/pedrazamiguez/split-trip/blob/main/domain/Symbol.kt)",
                rewritten,
            )

    def test_generates_sidebar_from_readme_hierarchy(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            repo_root = Path(tmp)
            docs_dir = repo_root / "docs"
            _write_file(docs_dir / "architecture" / "adrs" / "0001-offline-first-sync.md", "# ADR\n")
            _write_file(docs_dir / "domain" / "ubiquitous-language.md", "# Glossary\n")
            _write_file(repo_root / "web" / "index.html", "<html></html>\n")
            readme = _write_file(
                docs_dir / "README.md",
                (
                    "# SplitTrip Documentation Hub\n\n"
                    "Welcome to the central technical documentation.\n\n"
                    "---\n\n"
                    "## 📚 Documentation Hierarchy\n\n"
                    "```\n"
                    "docs/\n"
                    "├── domain/\n"
                    "└── architecture/\n"
                    "```\n\n"
                    "---\n\n"
                    "## 🏛️ 1. Architecture\n\n"
                    "### MADRs\n"
                    "* [ADR-0001](architecture/adrs/0001-offline-first-sync.md)\n\n"
                    "---\n\n"
                    "## 💼 2. Domain & Web\n\n"
                    "* [Glossary](domain/ubiquitous-language.md)\n"
                    "* [Landing]( ../web/index.html )\n"
                ),
            )

            doc_files = discover_doc_files(docs_dir)
            slug_map = build_slug_map(doc_files, docs_dir)
            sidebar, count = generate_sidebar(
                readme_markdown=readme.read_text(encoding="utf-8"),
                readme_path=readme,
                docs_dir=docs_dir,
                repo_root=repo_root,
                slug_map=slug_map,
                repository=DEFAULT_REPOSITORY,
                branch=DEFAULT_BRANCH,
            )

            self.assertEqual(count, 3)
            self.assertTrue(sidebar.startswith("### [🏠 Home](Home)\n"))
            self.assertNotIn("Documentation Hierarchy", sidebar)
            self.assertNotIn("├──", sidebar)
            self.assertNotIn("Welcome to the central", sidebar)
            self.assertIn("### 🏛️ 1. Architecture", sidebar)
            self.assertIn("**MADRs**", sidebar)
            self.assertIn("* [ADR-0001](0001-offline-first-sync)", sidebar)
            self.assertIn("### 💼 2. Domain & Web", sidebar)
            self.assertIn("* [Glossary](ubiquitous-language)", sidebar)
            self.assertIn(
                "* [Landing](https://github.com/pedrazamiguez/split-trip/blob/main/web/index.html)",
                sidebar,
            )

    def test_purges_stale_pages_while_preserving_git_dir(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            repo_root = root / "repo"
            docs_dir = repo_root / "docs"
            output_dir = root / "wiki"

            _write_file(docs_dir / "README.md", "# Hub\n\n## Section\n* [Guide](domain/guide.md)\n")
            _write_file(docs_dir / "domain" / "guide.md", "# Guide\n")

            git_head = _write_file(output_dir / ".git" / "HEAD", "ref: refs/heads/master\n")
            _write_file(output_dir / "Architecture-Diagrams.md", "# Legacy\n")
            _write_file(output_dir / "Offline‐First-Architecture-&-Synchronization.md", "# Legacy\n")
            _write_file(output_dir / "old" / "legacy.md", "# Nested Legacy\n")

            result = sync_docs_to_wiki(docs_dir=docs_dir, repo_root=repo_root, output_dir=output_dir)

            self.assertTrue(git_head.exists())
            self.assertEqual(git_head.read_text(encoding="utf-8"), "ref: refs/heads/master\n")
            self.assertFalse((output_dir / "Architecture-Diagrams.md").exists())
            self.assertFalse((output_dir / "Offline‐First-Architecture-&-Synchronization.md").exists())
            self.assertFalse((output_dir / "old").exists())
            self.assertEqual(
                set(result.stats.purged_files),
                {
                    "Architecture-Diagrams.md",
                    "Offline‐First-Architecture-&-Synchronization.md",
                    "old/legacy.md",
                },
            )
            self.assertTrue((output_dir / "Home.md").is_file())
            self.assertTrue((output_dir / "_Sidebar.md").is_file())
            self.assertTrue((output_dir / "guide.md").is_file())

    def test_dry_run_does_not_modify_output_dir(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            repo_root = root / "repo"
            docs_dir = repo_root / "docs"
            output_dir = root / "wiki"

            _write_file(docs_dir / "README.md", "# Hub\n")
            _write_file(docs_dir / "domain" / "guide.md", "# Guide\n")
            sentinel = _write_file(output_dir / "sentinel.md", "keep me\n")

            stdout = io.StringIO()
            with contextlib.redirect_stdout(stdout):
                exit_code = main(
                    [
                        "--docs-dir",
                        str(docs_dir),
                        "--repo-root",
                        str(repo_root),
                        "--output-dir",
                        str(output_dir),
                        "--dry-run",
                    ]
                )

            self.assertEqual(exit_code, 0)
            self.assertIn("Dry-run", stdout.getvalue())
            self.assertTrue(sentinel.is_file())
            self.assertEqual(sentinel.read_text(encoding="utf-8"), "keep me\n")
            self.assertFalse((output_dir / "Home.md").exists())

    def test_strict_links_fails_on_missing_relative_target(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            repo_root = root / "repo"
            docs_dir = repo_root / "docs"
            output_dir = root / "wiki"

            _write_file(docs_dir / "README.md", "# Hub\n")
            _write_file(
                docs_dir / "domain" / "broken.md",
                "Broken [link](non-existent-file.md).\n",
            )

            with self.assertRaises(ValueError):
                sync_docs_to_wiki(
                    docs_dir=docs_dir,
                    repo_root=repo_root,
                    output_dir=output_dir,
                    dry_run=True,
                    strict=True,
                )

            stderr = io.StringIO()
            with contextlib.redirect_stderr(stderr):
                exit_code = main(
                    [
                        "--docs-dir",
                        str(docs_dir),
                        "--repo-root",
                        str(repo_root),
                        "--dry-run",
                    ]
                )

            self.assertEqual(exit_code, 1)
            self.assertIn("non-existent-file.md", stderr.getvalue())

    def test_real_repository_docs_sync_smoke(self) -> None:
        repo_root = Path(__file__).resolve().parent.parent
        docs_dir = repo_root / "docs"
        expected_docs = discover_doc_files(docs_dir)

        with tempfile.TemporaryDirectory() as tmp:
            output_dir = Path(tmp) / "wiki"
            result = sync_docs_to_wiki(
                docs_dir=docs_dir,
                repo_root=repo_root,
                output_dir=output_dir,
                strict=True,
            )

            self.assertTrue((output_dir / "Home.md").is_file())
            self.assertTrue((output_dir / "_Sidebar.md").is_file())
            self.assertEqual(result.stats.article_count, len(expected_docs) - 1)
            self.assertEqual(result.stats.total_pages, len(expected_docs) + 1)

            output_files = sorted(p.name for p in output_dir.iterdir())
            self.assertEqual(len(output_files), result.stats.total_pages)
            self.assertFalse((output_dir / "release").exists())

            for md_file in output_dir.glob("*.md"):
                content = md_file.read_text(encoding="utf-8")
                self.assertNotIn("expense-share-app", content)


if __name__ == "__main__":
    unittest.main()
