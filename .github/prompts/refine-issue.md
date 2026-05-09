I want to create a GitHub issue for the following idea or problem:

> [describe the feature, bug, or improvement in plain English here]

---

## Your job

Turn the description above into a well-formed GitHub issue ready to be picked up by a developer. Do NOT implement anything — output the issue content only.

---

## Step 1 — Understand the codebase context

Before drafting the issue, read the following to ensure the issue is grounded in the actual architecture and constraints of the project:

- `.github/copilot-instructions.md`
- `AGENTS.md`
- `DESIGN.md`
- Any relevant `wiki/*.md` articles related to the topic
- Any existing issues or PRs that overlap with or inform this idea (search GitHub)

---

## Step 2 — Draft the GitHub issue

Produce the issue in the following structure:

### Title
A short, imperative sentence (e.g. "Extract `CashRateResultDelegate` from `CurrencyEventHandler`").

### Problem / Motivation
- What is wrong or missing today?
- What user-facing or developer-facing impact does it have?
- If it is a refactor/tech debt item, why does it matter (e.g. CI enforcement, maintainability)?

### Proposed Solution
- High-level description of the change.
- Which modules, layers, or files are involved.
- Any architecture patterns from `AGENTS.md` or `.github/copilot-instructions.md` that apply (e.g. Delegate sub-pattern, offline-first, MVI triad).
- Reference similar, existing implementations in the codebase where relevant.

### Acceptance Criteria
A checklist of verifiable conditions that define "done":
- [ ] Criterion 1
- [ ] Criterion 2
- [ ] All existing tests pass; new tests added where behaviour changes.
- [ ] `make check` passes with 0 failures (Konsist + unit tests + compilation).
- [ ] No new detekt findings introduced.

### Out of Scope
Explicitly list anything that this issue should NOT include, to prevent scope creep.

### Related Issues / PRs
Link any parent issues, child issues, or prior PRs that provide context.

---

## Step 3 — Review before posting

Before presenting the draft, verify:
- The proposed solution aligns with the architecture rules in `AGENTS.md`.
- The acceptance criteria are concrete and testable, not vague.
- The scope is narrow enough to be completed in a single PR.
- No implementation details that belong in a PR description have leaked into the issue.

Present the full draft to the user for review. Do NOT create the GitHub issue until the user explicitly approves it.
