---
name: start-issue
description: Start working on a GitHub issue end-to-end.
mode: agent
tools:
  - codebase
  - githubRepo
  - terminalLastCommand
arguments:
  - name: issue_url
    description: The URL of the GitHub issue to start working on.
    required: true
  - name: issue_number
    description: The number of the GitHub issue to start working on.
    required: false
---
# Start Issue

Start working on this issue:
- Issue URL: $ISSUE_URL
- Issue Number: $ISSUE_NUMBER

We're already on the correct branch with the latest changes from develop.

---

## Step 1 — Read everything first (mandatory, no shortcuts)

Read each of the following before writing a single line of code:

- [.github/copilot-instructions.md](../../../.github/copilot-instructions.md)
- [AGENTS.md](../../../AGENTS.md)
- [DESIGN.md](../../../DESIGN.md)
- All relevant `wiki/*.md` articles (especially [wiki/core-services-catalog.md](../../../wiki/core-services-catalog.md))
- The issue itself, including ALL parent/linked issues and every comment thread

---

## Step 2 — Establish a baseline (run BEFORE touching any file)

Run the full local check suite to know exactly what the codebase state is before you make changes:

```bash
make check   # Konsist architecture rules + all unit tests + debug compilation
```

**Interpret the results:**

- If a check **passes** → it is your responsibility to keep it passing after your changes.
- If a check **fails on a file you will touch** → fix the pre-existing violation as part of this PR before introducing your changes.
- If a check **fails on a file you will NOT touch** → document it in your implementation plan comment on the issue and ask the user how to proceed. Do NOT push changes that leave these new-to-this-PR failures unresolved.

---

## Step 3 — File-size guard (600-line hard limit, enforced by Konsist)

Before editing any file, check its current line count:

```bash
wc -l <path/to/file.kt>
```

If the file is already at or near 600 lines, factor that into your plan (split, extract delegate, etc.) **before** adding new code. Do NOT add code to a file that will push it over 600 lines.

After editing, re-check:

```bash
wc -l <path/to/file.kt>
```

If the result exceeds 600 lines, refactor immediately — do not move on.

---

## Step 4 — Post implementation plan as an issue comment

Post your implementation plan as a comment on the issue before writing code. The comment must include:

- Summary of changes per file
- Any pre-existing violations found in Step 2 and how they will be handled
- Architecture compliance checklist (from [AGENTS.md](../../../AGENTS.md)) confirmed for each new/modified component

Stick to the posted plan. If the plan needs to change, update the comment.

---

## Step 5 — Implement

Follow all architecture constraints in [AGENTS.md](../../../AGENTS.md) and [.github/copilot-instructions.md](../../../.github/copilot-instructions.md) strictly. Ensure you adhere to the project quality and style standards, including detekt rules, ktlint formatting, test coverage requirements, and the 600-line file-size limit.

**Core Reminders (refer to [AGENTS.md](../../../AGENTS.md) for details):**
- **No Pragmatic Patches:** Write clean, modular, production-ready code. Do not use temporary workarounds.
- **BigDecimal Math:** Use `BigDecimal` with an explicit scale and rounding mode for all precision-sensitive calculations (never `Double` or `Float`).
- **Offline-First Protocol:** Generate UUIDs and timestamps locally, write to Room first, and sync to Firestore in the background using the reusable sync delegates.
- **Design System:** Comply with the "Horizon Narrative" guidelines (no raw 1px borders, Outfit/Inter/Jakarta Sans typography, tonal layering, and bottom padding via `LocalBottomPadding` on tab screens).
- **Commenting Policy:** Comment the *why*, never the *what*. Avoid redundant comments. Do not reference GitHub issues or documentation sections in comments to simplify maintenance.

---

## Step 6 — Local verification gate (run BEFORE declaring done)

Do not consider the work complete until ALL of the following pass locally:

```bash
make check   # Konsist architecture rules + all unit tests + debug compilation — must show 0 failures
```

If any check fails, fix it before finishing. Do not leave the user to discover failures in CI.
