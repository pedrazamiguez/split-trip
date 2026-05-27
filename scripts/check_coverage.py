#!/usr/bin/env python3
"""
Local coverage gate for SplitTrip.

Parses the merged JaCoCo XML produced by ./gradlew jacocoMergedReport and
enforces TWO gates:

  1. Overall LINE coverage >= THRESHOLD_OVERALL_PCT (default 80%).
  2. Per-source-file LINE coverage >= THRESHOLD_FILE_PCT (default 80%) for
     every file with at least MIN_FILE_LINES instructionable lines.

The per-file gate mirrors what SonarQube enforces on "New Code" - without
it, a brand-new module with 0% coverage can slip through as long as the
overall number stays high.

Usage:
    python3 scripts/check_coverage.py
    python3 scripts/check_coverage.py --threshold 85       # overall
    python3 scripts/check_coverage.py --file-threshold 70  # per-file
    python3 scripts/check_coverage.py --min-lines 5        # ignore tiny files
"""

import argparse
import math
import sys
import xml.etree.ElementTree as ET
from typing import List, Tuple

REPORT_PATH = "build/reports/jacoco/merged/jacocoMergedReport.xml"
THRESHOLD_OVERALL_PCT = 80.0
THRESHOLD_FILE_PCT = 80.0
MIN_FILE_LINES = 3
MAX_OFFENDERS_PRINTED = 30

# Source file names to skip in the per-file gate regardless of line count.
# Used for Kotlin internals that the compiler generates into our own package
# directories (e.g. coroutine SafeCollector bridge classes). These appear in
# the JaCoCo XML with our package paths but originate from the kotlinx-coroutines
# runtime, so they cannot be excluded via JacocoExclusions class-path patterns.
IGNORED_SOURCEFILES = {
    "SafeCollector.common.kt",
}

RED = "\033[0;31m"
GREEN = "\033[0;32m"
YELLOW = "\033[1;33m"
NC = "\033[0m"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(add_help=True)
    parser.add_argument("--threshold", type=float, default=THRESHOLD_OVERALL_PCT)
    parser.add_argument("--file-threshold", type=float, default=THRESHOLD_FILE_PCT)
    parser.add_argument("--min-lines", type=int, default=MIN_FILE_LINES)
    return parser.parse_args()


def load_report(path: str) -> ET.Element:
    try:
        return ET.parse(path).getroot()
    except FileNotFoundError:
        print(f"{RED}x  Coverage report not found at {path}{NC}")
        print("   Run './gradlew jacocoMergedReport' first.")
        sys.exit(1)


def line_counter(node: ET.Element) -> Tuple[int, int]:
    for counter in node.findall("counter"):
        if counter.get("type") == "LINE":
            missed = int(counter.get("missed", 0))
            covered = int(counter.get("covered", 0))
            return missed, covered
    return 0, 0


def check_overall(root: ET.Element, threshold: float) -> bool:
    missed, covered = line_counter(root)
    total = missed + covered
    pct = covered / total * 100.0 if total > 0 else 0.0
    if pct >= threshold:
        print(
            f"{GREEN}OK  Overall LINE coverage: {pct:.1f}% "
            f"({covered}/{total} lines) -- threshold {threshold:.0f}%{NC}"
        )
        return True
    need = max(0, math.ceil(threshold / 100.0 * total) - covered)
    print(
        f"{RED}x   Overall LINE coverage: {pct:.1f}% "
        f"({covered}/{total} lines) -- below {threshold:.0f}% threshold{NC}"
    )
    print(f"{YELLOW}    Cover at least {need} more line(s) to reach {threshold:.0f}%.{NC}")
    return False


def check_per_file(
    root: ET.Element, threshold: float, min_lines: int
) -> Tuple[bool, List[Tuple[str, float, int, int]]]:
    offenders: List[Tuple[str, float, int, int]] = []
    for package in root.findall("package"):
        pkg_name = package.get("name", "")
        for sf in package.findall("sourcefile"):
            sf_name = sf.get("name", "")
            if sf_name in IGNORED_SOURCEFILES:
                continue
            missed, covered = line_counter(sf)
            total = missed + covered
            if total < min_lines:
                continue
            pct = covered / total * 100.0 if total > 0 else 0.0
            if pct < threshold:
                path = f"{pkg_name}/{sf.get('name', '')}"
                offenders.append((path, pct, covered, total))
    offenders.sort(key=lambda x: (x[1], -x[3]))
    return (len(offenders) == 0), offenders


def print_offenders(
    offenders: List[Tuple[str, float, int, int]], threshold: float
) -> None:
    print(
        f"{RED}x   {len(offenders)} file(s) below per-file threshold "
        f"({threshold:.0f}%):{NC}"
    )
    for path, pct, covered, total in offenders[:MAX_OFFENDERS_PRINTED]:
        print(f"    {RED}{pct:5.1f}%{NC}  {covered:>4}/{total:<4}  {path}")
    if len(offenders) > MAX_OFFENDERS_PRINTED:
        remaining = len(offenders) - MAX_OFFENDERS_PRINTED
        print(f"    {YELLOW}... {remaining} more file(s) omitted.{NC}")
    print(
        f"{YELLOW}    Add unit tests for the files above, or exclude genuinely "
        f"untestable files in build-logic/.../JacocoExclusions.kt.{NC}"
    )


def main() -> None:
    args = parse_args()
    root = load_report(REPORT_PATH)

    overall_ok = check_overall(root, args.threshold)
    per_file_ok, offenders = check_per_file(root, args.file_threshold, args.min_lines)

    if per_file_ok:
        print(
            f"{GREEN}OK  Per-file LINE coverage: every file >= "
            f"{args.file_threshold:.0f}% (min {args.min_lines} lines){NC}"
        )
    else:
        print_offenders(offenders, args.file_threshold)

    sys.exit(0 if overall_ok and per_file_ok else 1)


if __name__ == "__main__":
    main()

