---
name: follow-up
description: Request follow-up work on a specific issue (e.g. bug fixes, additional requirements, visual regression, or debugging) in a new or existing conversation without prior context.
mode: agent
tools:
  - codebase
  - githubRepo
  - terminalLastCommand
arguments:
  - name: issue_number
    description: The number of the GitHub issue we are following up on.
    required: true
  - name: follow_up_description
    description: An explanation of what needs to be checked, fixed, or done.
    required: true
  - name: screenshot_path
    description: Optional path to a screenshot or image demonstrating the issue or unexpected behavior.
    required: false
---
# Follow-Up on Issue

Follow up on the work for this issue:
- Issue Number: $ISSUE_NUMBER
- Follow-Up Description: $FOLLOW_UP_DESCRIPTION
- Screenshot Path: $SCREENSHOT_PATH

---

## Step 0 — Validate Branch and Context

Because this follow-up may be initiated in a new conversation without prior context, verify your local Git state:
1. Identify the current Git branch:
   ```bash
   git branch --show-current
   ```
2. Verify that you are on the correct branch corresponding to issue `$ISSUE_NUMBER` (e.g., the branch name should contain the issue number, such as `feature/issue-$ISSUE_NUMBER-...` or similar).
3. If not on the correct branch, switch to the correct branch.
4. Pull the latest changes from the remote repository to ensure your branch is fully up-to-date:
   ```bash
   git pull
   ```

---

## Step 1 — Read Everything First (mandatory, no shortcuts)

Read each of the following before writing a single line of code:
- [.github/copilot-instructions.md](../../../.github/copilot-instructions.md)
- [AGENTS.md](../../../AGENTS.md)
- [DESIGN.md](../../../DESIGN.md)
- All relevant `wiki/*.md` articles (especially [wiki/core-services-catalog.md](../../../wiki/core-services-catalog.md) and [wiki/offline-first-architecture.md](../../../wiki/offline-first-architecture.md))
- The GitHub issue `$ISSUE_NUMBER`, including ALL parent/linked issues and every comment thread, to understand the historical context and initial implementation.

---

## Step 2 — Triage the Follow-Up Request & Screenshot

1. **Understand the problem**: Read the `$FOLLOW_UP_DESCRIPTION` carefully.
2. **Review Screenshot**: If `$SCREENSHOT_PATH` is provided, view the screenshot/image to see the visual discrepancy, crash, or unexpected UI state:
   - Use the appropriate tool to inspect the image contents.
3. **Locate the affected code**: Search the codebase for the features, ViewModels, Screens, or Services associated with the issue.

---

## Step 3 — File-Size Guard (600-line hard limit, enforced by Konsist)

Before editing any file, check its current line count:
```bash
wc -l <path/to/file.kt>
```
If the file is already at or near 600 lines, factor that into your plan (split, extract event handler, extract delegate, etc.) **before** adding new code. Do NOT add code to a file that will push it over 600 lines.

After editing, re-check:
```bash
wc -l <path/to/file.kt>
```
If the result exceeds 600 lines, refactor immediately — do not move on.

---

## Step 4 — Implement

Follow all architecture constraints in [AGENTS.md](../../../AGENTS.md) and [.github/copilot-instructions.md](../../../.github/copilot-instructions.md) strictly.

**Core Reminders:**
- **No Pragmatic Patches:** Write clean, modular, production-ready code. Do not use temporary workarounds.
- **BigDecimal Math:** Use `BigDecimal` with an explicit scale and rounding mode for all precision-sensitive calculations (never `Double` or `Float`).
- **Offline-First Protocol:** Generate UUIDs and timestamps locally, write to Room first, and sync to Firestore in the background using the reusable sync delegates.
- **Design System:** Comply with the "Horizon Narrative" guidelines (no raw 1px borders, Outfit/Inter/Jakarta Sans typography, tonal layering, and bottom padding via `LocalBottomPadding` on tab screens).
- **Commenting Policy:** Comment the *why*, never the *what*. Avoid redundant comments.

---

## Step 5 — Local Verification Gate (run BEFORE declaring done)

Do not consider the work complete until ALL of the following pass locally:
```bash
make check   # Konsist architecture rules + all unit tests + debug compilation — must show 0 failures
```

If any check fails, fix it before finishing. Do not leave the user to discover failures in CI.
