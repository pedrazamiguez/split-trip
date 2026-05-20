Start working on this issue

https://github.com/pedrazamiguez/split-trip/issues/xxxx

We're already on the correct branch with the latest changes from develop.

---

## Step 1 — Read everything first (mandatory, no shortcuts)

Read each of the following before writing a single line of code:

- `.github/copilot-instructions.md`
- `AGENTS.md`
- `DESIGN.md`
- All relevant `wiki/*.md` articles (especially `wiki/core-services-catalog.md`)
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
- Architecture compliance checklist (from `AGENTS.md`) confirmed for each new/modified component

Stick to the posted plan. If the plan needs to change, update the comment.

---

## Step 5 — Implement

Follow the architecture constraints in `.github/copilot-instructions.md` and `AGENTS.md` strictly.  
Take into account detekt rules, ktlint formatting, new code coverage, and the 600-line file-size limit at all times.

**Core Reminders:**
- **No Pragmatic Patches:** Write clean, modular, production-ready code. Do not use quick hacks or temporary workarounds.
- **BigDecimal Math:** Use `BigDecimal` with an explicit scale and rounding mode for all precision-sensitive calculations (never `Double` or `Float`).
- **Offline-First Protocol:** Generate UUIDs and timestamps locally, write to Room first, and sync to Firestore in the background using reusable sync delegates.
- **Design System:** Comply with the "Horizon Narrative" guidelines (no raw 1px borders, Outfitt/Inter/Jakarta Sans typography, tonal layering, and bottom padding via `LocalBottomPadding` on tab screens).

**Commenting policy:** Comment the *why*, never the *what*. If a comment only restates what the code already says, delete it. Add a comment only when the reasoning behind a decision would not be obvious to the next reader from the code alone.

---

## Step 6 — Local verification gate (run BEFORE declaring done)

Do not consider the work complete until ALL of the following pass locally:

```bash
make check   # Konsist architecture rules + all unit tests + debug compilation — must show 0 failures
```

If any check fails, fix it before finishing. Do not leave the user to discover failures in CI.
