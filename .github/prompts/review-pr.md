A variety of comments have been raised on this PR — please take a look.

https://github.com/pedrazamiguez/split-trip/pull/xxxx

---

## Step 1 — Read and triage all comments

For each review comment or thread:

1. **Understand the context** — read the surrounding code, not just the highlighted line.
2. **Decide the outcome:**
   - ✅ **Valid** — address it with a code change.
   - ℹ️ **Partially valid** — address the spirit of the comment, explain any deviation in a reply.
   - ❌ **False positive** — reply with a clear technical reason why no change is needed.
3. **Reply to every comment**, even if no code change is made. Never leave a thread unanswered.

---

## Step 2 — File-size guard before editing

Before touching any file, check its current line count:

```bash
wc -l <path/to/file.kt>
```

If a file is at or near 600 lines (the Konsist hard limit), plan a split or extraction **before** adding code. After editing, re-check and refactor immediately if over 600 lines.

---

## Step 3 — Implement changes

Follow all architecture constraints in `.github/copilot-instructions.md` and `AGENTS.md` strictly.  
Take into account detekt rules, ktlint formatting, new code coverage, and the 600-line file-size limit.

**Core Reminders:**
- **No Pragmatic Patches:** Write clean, modular, production-ready code. Do not use quick hacks or temporary workarounds.
- **BigDecimal Math:** Use `BigDecimal` with an explicit scale and rounding mode for all precision-sensitive calculations (never `Double` or `Float`).
- **Offline-First Protocol:** Generate UUIDs and timestamps locally, write to Room first, and sync to Firestore in the background using reusable sync delegates.
- **Design System:** Comply with the "Horizon Narrative" guidelines (no raw 1px borders, Outfitt/Inter/Jakarta Sans typography, tonal layering, and bottom padding via `LocalBottomPadding` on tab screens).

**Commenting policy:** Comment the *why*, never the *what*. If a comment only restates what the code already says, delete it. Add a comment only when the reasoning behind a decision would not be obvious to the next reader from the code alone.

---

## Step 4 — Local verification gate (run BEFORE declaring done)

Do not consider the review addressed until ALL of the following pass locally:

```bash
make check   # Konsist architecture rules + all unit tests + debug compilation — must show 0 failures
```

If any check fails, fix it before finishing. Do not leave failures for CI to catch.
