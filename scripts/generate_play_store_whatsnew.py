#!/usr/bin/env python3
"""Generate localized, filtered Google Play Store 'whatsnew' release notes."""

from __future__ import annotations

import argparse
from dataclasses import dataclass
import json
from pathlib import Path
import re
import sys
from typing import Any

MAX_WHATSNEW_CHARS: int = 500
DEFAULT_WHATSNEW_EN: str = "- Bug fixes and performance improvements."
DEFAULT_WHATSNEW_ES: str = "- Corrección de errores y mejoras de rendimiento."

EXCLUDED_LABELS: frozenset[str] = frozenset({"sync", "config", "release"})
USER_FACING_LABELS: frozenset[str] = frozenset({"feature", "bug", "enhancement", "hotfix"})

SYNC_TITLE_PATTERN: re.Pattern[str] = re.compile(r"^sync\s*:", re.IGNORECASE)
ISSUE_KEY_PREFIX_PATTERN: re.Pattern[str] = re.compile(
    r"^\[(?:SPLTRP|EXSHAPP)-\d+\]:?\s*",
    re.IGNORECASE,
)
LINKED_ISSUE_PATTERN: re.Pattern[str] = re.compile(
    r"(?:closes|fixes|resolves)\s+#(\d+)",
    re.IGNORECASE,
)
TITLE_ISSUE_NUMBER_PATTERN: re.Pattern[str] = re.compile(
    r"\[(?:SPLTRP|EXSHAPP)-0*(\d+)\]",
    re.IGNORECASE,
)

HTML_COMMENT_PATTERN: re.Pattern[str] = re.compile(r"<!--.*?-->", re.DOTALL)
MARKDOWN_HEADING_PATTERN: re.Pattern[str] = re.compile(r"^(#{1,6})\s+(.*?)\s*$")
RELEASE_SECTION_HEADING_PATTERN: re.Pattern[str] = re.compile(
    r"^(#{1,4})\s*(Release Notes\s*/\s*Context|Release Notes|What's New|Novedades)\s*$",
    re.IGNORECASE,
)
EN_HEADING_PATTERN: re.Pattern[str] = re.compile(
    r"^#{2,5}\s*(?:en-US|whatsnew-en-US|English(?:\s*\(en-US\))?)\s*$",
    re.IGNORECASE,
)
ES_HEADING_PATTERN: re.Pattern[str] = re.compile(
    r"^#{2,5}\s*(?:es-ES|whatsnew-es-ES|Español(?:\s*\(es-ES\))?|Spanish(?:\s*\(es-ES\))?)\s*$",
    re.IGNORECASE,
)
EN_INLINE_LABEL_PATTERN: re.Pattern[str] = re.compile(
    r"^(?:\*\*|__)?(?:en-US|whatsnew-en-US|English)(?:\*\*|__)?\s*:\s*(.*)$",
    re.IGNORECASE,
)
ES_INLINE_LABEL_PATTERN: re.Pattern[str] = re.compile(
    r"^(?:\*\*|__)?(?:es-ES|whatsnew-es-ES|Español|Spanish)(?:\*\*|__)?\s*:\s*(.*)$",
    re.IGNORECASE,
)
BOILERPLATE_LINE_PATTERN: re.Pattern[str] = re.compile(
    r"^(?:_No response_|No response|None|N/A|"
    r"Automated PR for (?:release|hotfix)\.?.*|"
    r"(?:Closes|Fixes|Resolves)\s+#\d+\.?)$",
    re.IGNORECASE,
)


@dataclass(frozen=True)
class PullRequestSummary:
    number: int
    title: str
    labels: tuple[str, ...]
    merged_at: str = ""


@dataclass(frozen=True)
class BilingualNotes:
    en_us: str | None = None
    es_es: str | None = None


@dataclass(frozen=True)
class ResolvedWhatsNew:
    en_us: str
    es_es: str
    en_source: str
    es_source: str


def _normalize_labels(raw_labels: Any) -> tuple[str, ...]:
    if not isinstance(raw_labels, list):
        return ()
    names: list[str] = []
    for item in raw_labels:
        if isinstance(item, str) and item.strip():
            names.append(item.strip())
        elif isinstance(item, dict):
            name = item.get("name")
            if isinstance(name, str) and name.strip():
                names.append(name.strip())
    return tuple(names)


def parse_pull_requests(raw_json: str) -> list[PullRequestSummary]:
    if not raw_json or not raw_json.strip():
        return []
    try:
        parsed = json.loads(raw_json)
    except json.JSONDecodeError as exc:
        print(f"Warning: failed to parse PR JSON ({exc}); returning empty list.", file=sys.stderr)
        return []

    if not isinstance(parsed, list):
        print("Warning: expected JSON array of pull requests; returning empty list.", file=sys.stderr)
        return []

    summaries: list[PullRequestSummary] = []
    for item in parsed:
        if not isinstance(item, dict):
            continue
        number = item.get("number")
        title = item.get("title")
        if not isinstance(number, int) or not isinstance(title, str):
            continue
        labels = _normalize_labels(item.get("labels"))
        merged_at = item.get("mergedAt") or item.get("merged_at") or ""
        summaries.append(
            PullRequestSummary(
                number=number,
                title=title,
                labels=labels,
                merged_at=merged_at if isinstance(merged_at, str) else "",
            )
        )
    return summaries


def merge_and_deduplicate_prs(
    develop_prs: list[PullRequestSummary],
    main_prs: list[PullRequestSummary],
) -> list[PullRequestSummary]:
    by_number: dict[int, PullRequestSummary] = {}
    for pr in (*develop_prs, *main_prs):
        if pr.number not in by_number:
            by_number[pr.number] = pr
    return [by_number[num] for num in sorted(by_number)]


def is_user_facing_pr(pr: PullRequestSummary) -> bool:
    lower_labels = {label.strip().lower() for label in pr.labels if label.strip()}
    if lower_labels & EXCLUDED_LABELS:
        return False
    if SYNC_TITLE_PATTERN.match(pr.title.strip()):
        return False
    return bool(lower_labels & USER_FACING_LABELS)


def filter_user_facing_prs(prs: list[PullRequestSummary]) -> list[PullRequestSummary]:
    return [pr for pr in prs if is_user_facing_pr(pr)]


def clean_pr_title(title: str) -> str:
    stripped = ISSUE_KEY_PREFIX_PATTERN.sub("", title.strip())
    return " ".join(stripped.split())


def extract_linked_issue_number(
    pr_body: str | None,
    pr_title: str | None = None,
    commit_message: str | None = None,
) -> int | None:
    for text in (pr_body, commit_message):
        if text:
            match = LINKED_ISSUE_PATTERN.search(text)
            if match:
                return int(match.group(1))

    for text in (pr_title, commit_message, pr_body):
        if text:
            match = TITLE_ISSUE_NUMBER_PATTERN.search(text)
            if match:
                return int(match.group(1))

    return None


def _clean_extracted_block(lines: list[str]) -> str | None:
    cleaned_lines: list[str] = []
    for raw_line in lines:
        stripped = raw_line.strip()
        if stripped and BOILERPLATE_LINE_PATTERN.match(stripped):
            continue
        cleaned_lines.append(raw_line.rstrip())

    text = "\n".join(cleaned_lines).strip()
    if not text or BOILERPLATE_LINE_PATTERN.match(text):
        return None
    return text


def extract_curated_notes_from_markdown(
    markdown: str | None,
    *,
    allow_untagged_release_section: bool = True,
) -> BilingualNotes:
    if not markdown or not markdown.strip():
        return BilingualNotes()

    normalized = HTML_COMMENT_PATTERN.sub("", markdown.replace("\r\n", "\n").replace("\r", "\n"))
    lines = normalized.split("\n")

    en_lines: list[str] = []
    es_lines: list[str] = []
    active_locale: str | None = None
    found_explicit_locale = False

    for line in lines:
        stripped = line.strip()
        if EN_HEADING_PATTERN.match(stripped):
            active_locale = "en"
            found_explicit_locale = True
            continue
        if ES_HEADING_PATTERN.match(stripped):
            active_locale = "es"
            found_explicit_locale = True
            continue

        en_inline = EN_INLINE_LABEL_PATTERN.match(stripped)
        if en_inline:
            active_locale = "en"
            found_explicit_locale = True
            inline_text = en_inline.group(1).strip()
            if inline_text:
                en_lines.append(inline_text)
            continue

        es_inline = ES_INLINE_LABEL_PATTERN.match(stripped)
        if es_inline:
            active_locale = "es"
            found_explicit_locale = True
            inline_text = es_inline.group(1).strip()
            if inline_text:
                es_lines.append(inline_text)
            continue

        if MARKDOWN_HEADING_PATTERN.match(stripped):
            active_locale = None
            continue

        if active_locale == "en":
            en_lines.append(line)
        elif active_locale == "es":
            es_lines.append(line)

    if found_explicit_locale:
        return BilingualNotes(
            en_us=_clean_extracted_block(en_lines),
            es_es=_clean_extracted_block(es_lines),
        )

    if not allow_untagged_release_section:
        return BilingualNotes()

    section_title: str | None = None
    section_level: int = 0
    section_lines: list[str] = []
    in_release_section = False

    for line in lines:
        stripped = line.strip()
        release_heading = RELEASE_SECTION_HEADING_PATTERN.match(stripped)
        if release_heading and not in_release_section:
            in_release_section = True
            section_level = len(release_heading.group(1))
            section_title = release_heading.group(2).strip().lower()
            continue

        if in_release_section:
            any_heading = MARKDOWN_HEADING_PATTERN.match(stripped)
            if any_heading and len(any_heading.group(1)) <= section_level:
                break
            section_lines.append(line)

    if not in_release_section:
        return BilingualNotes()

    cleaned_section = _clean_extracted_block(section_lines)
    if not cleaned_section:
        return BilingualNotes()

    if section_title == "novedades":
        return BilingualNotes(en_us=None, es_es=cleaned_section)
    return BilingualNotes(en_us=cleaned_section, es_es=None)


def build_english_bullets(prs: list[PullRequestSummary]) -> str | None:
    bullets: list[str] = []
    seen: set[str] = set()
    for pr in prs:
        cleaned = clean_pr_title(pr.title)
        if not cleaned:
            continue
        bullet = f"- {cleaned}"
        if bullet not in seen:
            seen.add(bullet)
            bullets.append(bullet)
    if not bullets:
        return None
    return "\n".join(bullets)


def build_spanish_summary_bullets(prs: list[PullRequestSummary]) -> str | None:
    if not prs:
        return None

    all_labels = {label.strip().lower() for pr in prs for label in pr.labels if label.strip()}
    bullets: list[str] = []

    if "feature" in all_labels:
        bullets.append("- Nuevas funcionalidades y mejoras.")
    if "bug" in all_labels or "hotfix" in all_labels:
        bullets.append("- Corrección de errores y mejoras de estabilidad.")
    if "enhancement" in all_labels:
        bullets.append("- Mejoras de rendimiento y experiencia de usuario.")

    if not bullets:
        return None
    return "\n".join(bullets)


def truncate_whatsnew(text: str, max_chars: int = MAX_WHATSNEW_CHARS) -> str:
    normalized = text.replace("\r\n", "\n").replace("\r", "\n").strip()
    if len(normalized) <= max_chars:
        return normalized

    lines = normalized.split("\n")
    kept_lines: list[str] = []
    current_len = 0

    for line in lines:
        addition = len(line) if not kept_lines else len(line) + 1
        if current_len + addition <= max_chars:
            kept_lines.append(line)
            current_len += addition
        else:
            break

    if kept_lines:
        return "\n".join(kept_lines)

    first_line = lines[0].strip()
    cutoff = max(0, max_chars - 3)
    prefix = first_line[:cutoff]
    last_space = prefix.rfind(" ")
    if last_space > 0:
        prefix = prefix[:last_space].rstrip()
    return f"{prefix}..."


def read_curated_file(path: Path) -> str | None:
    if not path.is_file():
        return None
    try:
        content = path.read_text(encoding="utf-8").strip()
    except OSError:
        return None
    return content if content else None


def resolve_whatsnew(
    whatsnew_dir: Path,
    develop_prs: list[PullRequestSummary],
    main_prs: list[PullRequestSummary],
    pr_body: str | None = None,
    issue_body: str | None = None,
) -> ResolvedWhatsNew:
    combined_prs = merge_and_deduplicate_prs(develop_prs, main_prs)
    filtered_prs = filter_user_facing_prs(combined_prs)

    pr_notes = extract_curated_notes_from_markdown(
        pr_body,
        allow_untagged_release_section=True,
    )
    issue_notes = extract_curated_notes_from_markdown(
        issue_body,
        allow_untagged_release_section=True,
    )

    curated_en = read_curated_file(whatsnew_dir / "whatsnew-en-US")
    curated_es = read_curated_file(whatsnew_dir / "whatsnew-es-ES")

    if curated_en:
        raw_en = curated_en
        en_source = "curated-file"
    elif pr_notes.en_us:
        raw_en = pr_notes.en_us
        en_source = "pr-body"
    elif issue_notes.en_us:
        raw_en = issue_notes.en_us
        en_source = "issue-body"
    else:
        auto_en = build_english_bullets(filtered_prs)
        if auto_en:
            raw_en = auto_en
            en_source = "auto-pr-titles"
        else:
            raw_en = DEFAULT_WHATSNEW_EN
            en_source = "localized-fallback"

    if curated_es:
        raw_es = curated_es
        es_source = "curated-file"
    elif pr_notes.es_es:
        raw_es = pr_notes.es_es
        es_source = "pr-body"
    elif issue_notes.es_es:
        raw_es = issue_notes.es_es
        es_source = "issue-body"
    else:
        summary_es = build_spanish_summary_bullets(filtered_prs)
        if summary_es:
            raw_es = summary_es
            es_source = "localized-summary"
        else:
            raw_es = DEFAULT_WHATSNEW_ES
            es_source = "localized-fallback"

    return ResolvedWhatsNew(
        en_us=truncate_whatsnew(raw_en, MAX_WHATSNEW_CHARS),
        es_es=truncate_whatsnew(raw_es, MAX_WHATSNEW_CHARS),
        en_source=en_source,
        es_source=es_source,
    )


def write_whatsnew_files(whatsnew_dir: Path, resolved: ResolvedWhatsNew) -> tuple[Path, Path]:
    whatsnew_dir.mkdir(parents=True, exist_ok=True)
    en_path = whatsnew_dir / "whatsnew-en-US"
    es_path = whatsnew_dir / "whatsnew-es-ES"
    en_path.write_text(resolved.en_us, encoding="utf-8")
    es_path.write_text(resolved.es_es, encoding="utf-8")
    return en_path, es_path


def select_release_pr_from_commit_pulls(raw_json: str) -> dict[str, Any] | None:
    if not raw_json or not raw_json.strip():
        return None
    try:
        pulls = json.loads(raw_json)
    except json.JSONDecodeError:
        return None

    if not isinstance(pulls, list) or not pulls:
        return None

    dict_pulls = [p for p in pulls if isinstance(p, dict)]
    if not dict_pulls:
        return None

    main_pulls = [
        p
        for p in dict_pulls
        if isinstance(p.get("base"), dict) and p["base"].get("ref") == "main"
    ]
    candidates = main_pulls if main_pulls else dict_pulls

    for pr in candidates:
        labels = {lbl.lower() for lbl in _normalize_labels(pr.get("labels"))}
        if labels & {"release", "hotfix"}:
            return pr

    return candidates[0]


def _read_optional_file(path_str: str | None) -> str:
    if not path_str:
        return ""
    path = Path(path_str)
    if not path.is_file():
        return ""
    try:
        return path.read_text(encoding="utf-8")
    except OSError:
        return ""


def _build_arg_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Resolve and write localized Google Play whatsnew files.",
    )
    parser.add_argument("--whatsnew-dir", default="distribution/whatsnew")
    parser.add_argument("--develop-prs-file", default=None)
    parser.add_argument("--main-prs-file", default=None)
    parser.add_argument("--pr-json-file", default=None)
    parser.add_argument("--pr-body-file", default=None)
    parser.add_argument("--issue-body-file", default=None)
    parser.add_argument("--commit-message", default=None)
    parser.add_argument("--github-output", default=None)
    parser.add_argument("--print-linked-issue", action="store_true")
    return parser


def main(argv: list[str] | None = None) -> int:
    parser = _build_arg_parser()
    args = parser.parse_args(argv)

    if args.print_linked_issue:
        commit_pulls_raw = _read_optional_file(args.pr_json_file)
        selected_pr = select_release_pr_from_commit_pulls(commit_pulls_raw)
        pr_body = ""
        pr_title = ""
        if selected_pr:
            raw_body = selected_pr.get("body")
            raw_title = selected_pr.get("title")
            pr_body = raw_body if isinstance(raw_body, str) else ""
            pr_title = raw_title if isinstance(raw_title, str) else ""

        if args.pr_body_file:
            Path(args.pr_body_file).write_text(pr_body, encoding="utf-8")

        issue_num = extract_linked_issue_number(
            pr_body=pr_body,
            pr_title=pr_title,
            commit_message=args.commit_message,
        )
        if issue_num is not None:
            print(issue_num)
        return 0

    whatsnew_dir = Path(args.whatsnew_dir)
    develop_prs = parse_pull_requests(_read_optional_file(args.develop_prs_file))
    main_prs = parse_pull_requests(_read_optional_file(args.main_prs_file))
    pr_body = _read_optional_file(args.pr_body_file) or None
    issue_body = _read_optional_file(args.issue_body_file) or None

    resolved = resolve_whatsnew(
        whatsnew_dir=whatsnew_dir,
        develop_prs=develop_prs,
        main_prs=main_prs,
        pr_body=pr_body,
        issue_body=issue_body,
    )
    write_whatsnew_files(whatsnew_dir, resolved)

    if args.github_output:
        output_path = Path(args.github_output)
        with output_path.open("a", encoding="utf-8") as fh:
            fh.write(f"EN_SOURCE={resolved.en_source}\n")
            fh.write(f"ES_SOURCE={resolved.es_source}\n")
            fh.write(f"EN_LENGTH={len(resolved.en_us)}\n")
            fh.write(f"ES_LENGTH={len(resolved.es_es)}\n")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
