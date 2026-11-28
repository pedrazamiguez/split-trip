#!/usr/bin/env python3
"""Synchronize SplitTrip's docs/ directory to the GitHub Wiki repository.

This script treats ``docs/`` as the single source of truth:
- Discovers Markdown documents under ``docs/`` (excluding ``docs/release/assets/``)
  and fails fast on duplicate Wiki stems or reserved filenames.
- Generates ``Home.md`` and ``_Sidebar.md`` from ``docs/README.md``.
- Rewrites relative links targeting other ``docs/`` files into flat GitHub Wiki
  page slugs (preserving ``#fragment`` anchors and optional link titles).
- Rewrites relative links targeting non-Wiki repository files into GitHub
  ``blob/<branch>/...`` URLs (including tolerance for legacy single-``../``
  links pointing to repository-root paths).
- Preserves fenced code blocks and inline code spans while keeping backticked
  link labels intact.
- Performs a clean mirror in ``--output-dir`` by purging all stale files and
  directories outside ``.git``.
"""

from __future__ import annotations

import argparse
from dataclasses import dataclass
from pathlib import Path
import re
import shutil
import sys

DEFAULT_DOCS_DIR: Path = Path("docs")
DEFAULT_REPO_ROOT: Path = Path(".")
DEFAULT_REPOSITORY: str = "pedrazamiguez/split-trip"
DEFAULT_BRANCH: str = "main"
EXCLUDED_DOCS_PREFIXES: tuple[str, ...] = ("release/assets",)
HOME_FILENAME: str = "Home.md"
HOME_SLUG: str = "Home"
SIDEBAR_FILENAME: str = "_Sidebar.md"

_RESERVED_STEMS: frozenset[str] = frozenset({"home", "_sidebar"})
_EXTERNAL_PREFIXES: tuple[str, ...] = ("http://", "https://", "mailto:", "tel:")
_FENCE_OPEN_RE = re.compile(r"^[ ]{0,3}(`{3,}|~{3,})(.*)$")
_LINK_RE = re.compile(r"(?P<bang>!?)\[(?P<label>[^\]]*)\]\((?P<target>(?:[^()\n]|\([^()\n]*\))+)\)")
_TARGET_TITLE_RE = re.compile(r'^(\S+)(\s+(?:"[^"]*"|\'[^\']*\'|\([^)]*\)))$')
_HR_RE = re.compile(r"^\s*([-*_])(?:\s*\1){2,}\s*$")
_BULLET_LINK_RE = re.compile(r"^\s*[-*]\s+\[")


@dataclass(frozen=True)
class SyncedPage:
    source_path: Path
    wiki_filename: str
    wiki_slug: str
    content: str


@dataclass(frozen=True)
class SyncStats:
    total_pages: int
    article_count: int
    purged_files: tuple[str, ...]
    rewritten_links: int


@dataclass(frozen=True)
class SyncResult:
    pages: tuple[SyncedPage, ...]
    stats: SyncStats


def _is_excluded(rel_posix: str, excluded_prefixes: tuple[str, ...]) -> bool:
    for prefix in excluded_prefixes:
        normalized = prefix.strip("/")
        if not normalized:
            continue
        if rel_posix == normalized or rel_posix.startswith(f"{normalized}/"):
            return True
    return False


def discover_doc_files(
    docs_dir: Path,
    excluded_prefixes: tuple[str, ...] = EXCLUDED_DOCS_PREFIXES,
) -> list[Path]:
    """Discover Markdown docs deterministically and validate Wiki filename uniqueness."""
    resolved_docs = docs_dir.resolve()
    readme_path = (resolved_docs / "README.md").resolve()
    if not readme_path.is_file():
        raise FileNotFoundError(f"Required documentation hub not found: {readme_path}")

    discovered: list[Path] = []
    seen_stems: dict[str, Path] = {}

    candidates = sorted(
        resolved_docs.rglob("*.md"),
        key=lambda path: path.relative_to(resolved_docs).as_posix(),
    )

    for candidate in candidates:
        if not candidate.is_file():
            continue
        rel_posix = candidate.relative_to(resolved_docs).as_posix()
        if _is_excluded(rel_posix, excluded_prefixes):
            continue

        resolved_candidate = candidate.resolve()
        if resolved_candidate != readme_path:
            stem_key = resolved_candidate.stem.lower()
            if stem_key in _RESERVED_STEMS:
                raise ValueError(
                    f"Reserved Wiki filename stem '{resolved_candidate.stem}' in {rel_posix}"
                )
            if stem_key in seen_stems:
                other = seen_stems[stem_key].relative_to(resolved_docs).as_posix()
                raise ValueError(
                    f"Duplicate Wiki page stem '{resolved_candidate.stem}': "
                    f"'{other}' collides with '{rel_posix}'"
                )
            seen_stems[stem_key] = resolved_candidate

        discovered.append(resolved_candidate)

    return discovered


def build_slug_map(doc_files: list[Path], docs_dir: Path) -> dict[Path, str]:
    """Map resolved documentation paths to their flat GitHub Wiki slugs."""
    resolved_readme = (docs_dir / "README.md").resolve()
    slug_map: dict[Path, str] = {}
    for doc_file in doc_files:
        resolved = doc_file.resolve()
        slug_map[resolved] = HOME_SLUG if resolved == resolved_readme else resolved.stem
    return slug_map


def resolve_relative_target(
    raw_path: str,
    source_file: Path,
    docs_dir: Path,
    repo_root: Path,
    strict: bool = True,
) -> Path:
    """Resolve a relative Markdown link target against source_file and repo_root."""
    resolved_root = repo_root.resolve()
    resolved_docs = docs_dir.resolve()
    resolved_source = source_file.resolve()

    candidate = (resolved_source.parent / raw_path).resolve()

    # Read-only tolerance for legacy docs that reference repo-root paths with a single '../'.
    if not candidate.exists() and raw_path.startswith("../"):
        fallback = (resolved_docs / raw_path).resolve()
        if fallback.exists():
            candidate = fallback

    if strict:
        try:
            candidate.relative_to(resolved_root)
        except ValueError as exc:
            raise ValueError(
                f"Link target '{raw_path}' in {source_file} escapes repository root {resolved_root}"
            ) from exc

        if not candidate.exists():
            raise ValueError(
                f"Broken relative link '{raw_path}' in {source_file} "
                f"(resolved to missing path: {candidate})"
            )

    return candidate


def _split_target_and_title(raw_target: str) -> tuple[str, str]:
    stripped = raw_target.strip()
    match = _TARGET_TITLE_RE.match(stripped)
    if match:
        url_part, title_suffix = match.group(1), match.group(2)
    else:
        url_part, title_suffix = stripped, ""

    if url_part.startswith("<") and url_part.endswith(">") and len(url_part) >= 2:
        url_part = url_part[1:-1]

    return url_part, title_suffix


def rewrite_link_target(
    raw_target: str,
    source_file: Path,
    docs_dir: Path,
    repo_root: Path,
    slug_map: dict[Path, str],
    repository: str,
    branch: str,
    strict: bool = True,
) -> tuple[str, bool]:
    """Rewrite a single Markdown link target to a Wiki slug or GitHub blob URL."""
    url_part, title_suffix = _split_target_and_title(raw_target)
    if not url_part:
        return raw_target, False

    lower_url = url_part.lower()
    if lower_url.startswith(_EXTERNAL_PREFIXES) or url_part.startswith("#"):
        return raw_target, False

    if "#" in url_part:
        path_part, fragment_body = url_part.split("#", 1)
        fragment = f"#{fragment_body}"
    else:
        path_part, fragment = url_part, ""

    if not path_part:
        return raw_target, False

    resolved_target = resolve_relative_target(
        raw_path=path_part,
        source_file=source_file,
        docs_dir=docs_dir,
        repo_root=repo_root,
        strict=strict,
    )

    resolved_root = repo_root.resolve()
    try:
        repo_rel = resolved_target.relative_to(resolved_root).as_posix()
    except ValueError:
        return raw_target, False

    if not resolved_target.exists():
        return raw_target, False

    if resolved_target in slug_map:
        return f"{slug_map[resolved_target]}{fragment}{title_suffix}", True

    blob_url = f"https://github.com/{repository}/blob/{branch}/{repo_rel}{fragment}{title_suffix}"
    return blob_url, True


def _mask_fenced_code_blocks(markdown: str) -> tuple[str, list[str]]:
    lines = markdown.splitlines(keepends=True)
    output_parts: list[str] = []
    fenced_blocks: list[str] = []

    in_fence = False
    fence_char = ""
    fence_len = 0
    current_block: list[str] = []

    for line in lines:
        if not in_fence:
            match = _FENCE_OPEN_RE.match(line.rstrip("\r\n"))
            if match:
                fence_token, info = match.group(1), match.group(2)
                char = fence_token[0]
                if char == "`" and "`" in info:
                    output_parts.append(line)
                    continue
                in_fence = True
                fence_char = char
                fence_len = len(fence_token)
                current_block = [line]
            else:
                output_parts.append(line)
        else:
            current_block.append(line)
            stripped = line.rstrip("\r\n").lstrip(" ")
            if (
                len(line) - len(line.lstrip(" ")) <= 3
                and stripped
                and set(stripped.rstrip()) == {fence_char}
                and len(stripped.rstrip()) >= fence_len
            ):
                placeholder = f"\x00SYNC_WIKI_FENCE_{len(fenced_blocks)}\x00"
                fenced_blocks.append("".join(current_block))
                output_parts.append(placeholder)
                in_fence = False
                current_block = []

    if in_fence and current_block:
        placeholder = f"\x00SYNC_WIKI_FENCE_{len(fenced_blocks)}\x00"
        fenced_blocks.append("".join(current_block))
        output_parts.append(placeholder)

    return "".join(output_parts), fenced_blocks


def _mask_inline_code_spans(text: str) -> tuple[str, list[str]]:
    spans: list[str] = []
    out: list[str] = []
    i = 0
    length = len(text)

    while i < length:
        if text[i] != "`":
            out.append(text[i])
            i += 1
            continue

        tick_end = i
        while tick_end < length and text[tick_end] == "`":
            tick_end += 1
        tick_count = tick_end - i

        close_pos = -1
        scan = tick_end
        while scan < length:
            if text[scan] == "`":
                close_end = scan
                while close_end < length and text[close_end] == "`":
                    close_end += 1
                if close_end - scan == tick_count:
                    close_pos = close_end
                    break
                scan = close_end
            else:
                scan += 1

        if close_pos != -1:
            placeholder = f"\x00SYNC_WIKI_CODE_{len(spans)}\x00"
            spans.append(text[i:close_pos])
            out.append(placeholder)
            i = close_pos
        else:
            out.append(text[i:tick_end])
            i = tick_end

    return "".join(out), spans


def rewrite_markdown_links(
    markdown: str,
    source_file: Path,
    docs_dir: Path,
    repo_root: Path,
    slug_map: dict[Path, str],
    repository: str,
    branch: str,
    strict: bool = True,
) -> tuple[str, int]:
    """Rewrite relative Markdown links while preserving code blocks and inline code."""
    masked_fences, fenced_blocks = _mask_fenced_code_blocks(markdown)
    masked_all, inline_spans = _mask_inline_code_spans(masked_fences)

    rewrite_count = 0

    def _replace_match(match: re.Match[str]) -> str:
        nonlocal rewrite_count
        bang = match.group("bang")
        label = match.group("label")
        target = match.group("target")
        rewritten, changed = rewrite_link_target(
            raw_target=target,
            source_file=source_file,
            docs_dir=docs_dir,
            repo_root=repo_root,
            slug_map=slug_map,
            repository=repository,
            branch=branch,
            strict=strict,
        )
        if changed:
            rewrite_count += 1
        return f"{bang}[{label}]({rewritten})"

    rewritten_text = _LINK_RE.sub(_replace_match, masked_all)

    for idx, span in enumerate(inline_spans):
        rewritten_text = rewritten_text.replace(f"\x00SYNC_WIKI_CODE_{idx}\x00", span)

    for idx, block in enumerate(fenced_blocks):
        rewritten_text = rewritten_text.replace(f"\x00SYNC_WIKI_FENCE_{idx}\x00", block)

    return rewritten_text, rewrite_count


def generate_sidebar(
    readme_markdown: str,
    readme_path: Path,
    docs_dir: Path,
    repo_root: Path,
    slug_map: dict[Path, str],
    repository: str,
    branch: str,
    strict: bool = True,
) -> tuple[str, int]:
    """Generate _Sidebar.md from docs/README.md navigation sections."""
    body_lines: list[str] = []
    seen_nav_section = False
    in_hierarchy_section = False
    in_code_block = False
    fence_char = ""
    fence_len = 0

    for raw_line in readme_markdown.splitlines():
        stripped = raw_line.strip()

        if in_code_block:
            if (
                stripped
                and set(stripped) == {fence_char}
                and len(stripped) >= fence_len
            ):
                in_code_block = False
            continue

        fence_match = _FENCE_OPEN_RE.match(raw_line)
        if fence_match:
            token = fence_match.group(1)
            if not (token[0] == "`" and "`" in fence_match.group(2)):
                in_code_block = True
                fence_char = token[0]
                fence_len = len(token)
                continue

        if raw_line.startswith("## "):
            heading_text = raw_line[3:].strip()
            if "documentation hierarchy" in heading_text.lower():
                in_hierarchy_section = True
                continue
            in_hierarchy_section = False
            seen_nav_section = True
            body_lines.append("")
            body_lines.append(f"### {heading_text}")
            continue

        if not seen_nav_section or in_hierarchy_section:
            continue

        if not stripped or _HR_RE.match(raw_line):
            continue

        if raw_line.startswith("### "):
            sub_text = raw_line[4:].strip()
            body_lines.append("")
            body_lines.append(f"**{sub_text}**")
            continue

        if _BULLET_LINK_RE.match(raw_line):
            body_lines.append(raw_line.rstrip())

    raw_body = "\n".join(body_lines)
    rewritten_body, rewrite_count = rewrite_markdown_links(
        markdown=raw_body,
        source_file=readme_path,
        docs_dir=docs_dir,
        repo_root=repo_root,
        slug_map=slug_map,
        repository=repository,
        branch=branch,
        strict=strict,
    )
    header = f"### [🏠 Home]({HOME_SLUG})"
    sidebar = f"{header}\n{rewritten_body}\n" if rewritten_body else f"{header}\n"
    return sidebar, rewrite_count


def _purge_stale_entries(output_dir: Path) -> tuple[str, ...]:
    purged: list[str] = []
    if not output_dir.exists():
        return ()

    for entry in sorted(output_dir.iterdir(), key=lambda p: p.name):
        if entry.name == ".git":
            continue
        if entry.is_dir() and not entry.is_symlink():
            nested_files = sorted(
                item.relative_to(output_dir).as_posix()
                for item in entry.rglob("*")
                if item.is_file() or item.is_symlink()
            )
            purged.extend(nested_files or [entry.relative_to(output_dir).as_posix()])
            shutil.rmtree(entry)
        else:
            purged.append(entry.relative_to(output_dir).as_posix())
            entry.unlink()

    return tuple(sorted(purged))


def sync_docs_to_wiki(
    docs_dir: Path,
    repo_root: Path,
    output_dir: Path | None,
    repository: str = DEFAULT_REPOSITORY,
    branch: str = DEFAULT_BRANCH,
    dry_run: bool = False,
    strict: bool = True,
) -> SyncResult:
    """Validate docs/, normalize links, build Wiki pages, and optionally write clean mirror."""
    resolved_docs = docs_dir.resolve()
    resolved_root = repo_root.resolve()
    readme_path = (resolved_docs / "README.md").resolve()

    doc_files = discover_doc_files(resolved_docs)
    slug_map = build_slug_map(doc_files, resolved_docs)
    rewrite_ctx = (resolved_docs, resolved_root, slug_map, repository, branch, strict)

    pages: list[SyncedPage] = []
    total_rewrites = 0
    article_count = 0

    readme_raw = readme_path.read_text(encoding="utf-8")
    home_content, home_rewrites = rewrite_markdown_links(readme_raw, readme_path, *rewrite_ctx)
    total_rewrites += home_rewrites
    pages.append(SyncedPage(readme_path, HOME_FILENAME, HOME_SLUG, home_content))

    sidebar_content, sidebar_rewrites = generate_sidebar(readme_raw, readme_path, *rewrite_ctx)
    total_rewrites += sidebar_rewrites
    pages.append(SyncedPage(readme_path, SIDEBAR_FILENAME, "_Sidebar", sidebar_content))

    for doc_file in doc_files:
        if doc_file == readme_path:
            continue
        article_count += 1
        raw_text = doc_file.read_text(encoding="utf-8")
        rewritten, rewrites = rewrite_markdown_links(raw_text, doc_file, *rewrite_ctx)
        total_rewrites += rewrites
        slug = slug_map[doc_file]
        pages.append(SyncedPage(doc_file, f"{slug}.md", slug, rewritten))

    purged_files: tuple[str, ...] = ()
    if not dry_run:
        if output_dir is None:
            raise ValueError("--output-dir is required unless --dry-run is specified.")
        output_dir.mkdir(parents=True, exist_ok=True)
        purged_files = _purge_stale_entries(output_dir)
        for page in pages:
            (output_dir / page.wiki_filename).write_text(page.content, encoding="utf-8")

    stats = SyncStats(len(pages), article_count, purged_files, total_rewrites)
    return SyncResult(pages=tuple(pages), stats=stats)


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Sync docs/ Markdown documentation to a GitHub Wiki checkout."
    )
    parser.add_argument("--docs-dir", type=Path, default=DEFAULT_DOCS_DIR, help="Source docs/ dir.")
    parser.add_argument("--repo-root", type=Path, default=DEFAULT_REPO_ROOT, help="Repository root.")
    parser.add_argument(
        "--output-dir",
        type=Path,
        default=None,
        help="Target Wiki checkout directory (required unless --dry-run is set).",
    )
    parser.add_argument("--repository", default=DEFAULT_REPOSITORY, help="GitHub owner/repo slug.")
    parser.add_argument("--branch", default=DEFAULT_BRANCH, help="Branch for non-Wiki blob URLs.")
    parser.add_argument("--dry-run", action="store_true", help="Validate without writing files.")
    parser.add_argument("--non-strict", action="store_true", help="Allow unresolved relative links.")
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv)
    try:
        result = sync_docs_to_wiki(
            docs_dir=args.docs_dir,
            repo_root=args.repo_root,
            output_dir=args.output_dir,
            repository=args.repository,
            branch=args.branch,
            dry_run=args.dry_run,
            strict=not args.non_strict,
        )
    except (FileNotFoundError, ValueError, OSError) as exc:
        print(f"Error syncing docs to GitHub Wiki: {exc}", file=sys.stderr)
        return 1

    mode_label = "Dry-run" if args.dry_run else "Synced"
    print(
        f"{mode_label} {result.stats.total_pages} Wiki pages "
        f"({result.stats.article_count} articles + Home.md + _Sidebar.md), "
        f"rewrote {result.stats.rewritten_links} links, "
        f"purged {len(result.stats.purged_files)} stale entries."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
