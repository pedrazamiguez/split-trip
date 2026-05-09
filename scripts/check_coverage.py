#!/usr/bin/env python3
"""
Local coverage gate for SplitTrip.

Parses the merged JaCoCo XML produced by ./gradlew jacocoMergedReport and
fails (exit 1) if overall LINE coverage is below THRESHOLD_LINE_PCT.

Usage:
    python3 scripts/check_coverage.py
    python3 scripts/check_coverage.py --threshold 85   # override threshold
"""

import sys
import xml.etree.ElementTree as ET

REPORT_PATH = "build/reports/jacoco/merged/jacocoMergedReport.xml"
THRESHOLD_LINE_PCT = 80.0

# ANSI colours (same palette as the Makefile)
RED = "\033[0;31m"
GREEN = "\033[0;32m"
YELLOW = "\033[1;33m"
NC = "\033[0m"


def main() -> None:
    threshold = THRESHOLD_LINE_PCT
    for arg in sys.argv[1:]:
        if arg.startswith("--threshold"):
            try:
                threshold = float(arg.split("=")[1] if "=" in arg else sys.argv[sys.argv.index(arg) + 1])
            except (IndexError, ValueError):
                print(f"{RED}✗  Invalid --threshold value{NC}")
                sys.exit(1)

    try:
        tree = ET.parse(REPORT_PATH)
    except FileNotFoundError:
        print(f"{RED}✗  Coverage report not found at {REPORT_PATH}{NC}")
        print(f"   Run './gradlew jacocoMergedReport' first.")
        sys.exit(1)

    root = tree.getroot()

    for counter in root.findall("counter"):
        if counter.get("type") == "LINE":
            missed = int(counter.get("missed", 0))
            covered = int(counter.get("covered", 0))
            total = missed + covered
            pct = covered / total * 100.0 if total > 0 else 0.0

            if pct >= threshold:
                print(
                    f"{GREEN}✅  LINE coverage: {pct:.1f}%"
                    f" ({covered}/{total} lines) — threshold {threshold:.0f}% ✓{NC}"
                )
                sys.exit(0)
            else:
                need = max(0, int(threshold / 100.0 * total) - covered + 1)
                print(
                    f"{RED}✗   LINE coverage: {pct:.1f}%"
                    f" ({covered}/{total} lines)"
                    f" — below {threshold:.0f}% threshold{NC}"
                )
                print(
                    f"{YELLOW}    Cover at least {need} more line(s) to reach {threshold:.0f}%.{NC}"
                )
                print(
                    f"    Run: python3 scripts/coverage_analysis.py"
                    f" — for a breakdown of uncovered classes."
                )
                sys.exit(1)

    print(f"{RED}✗  No LINE counter found in merged coverage report.{NC}")
    sys.exit(1)


if __name__ == "__main__":
    main()

