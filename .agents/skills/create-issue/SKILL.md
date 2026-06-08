---
name: sp-create-issue
description: Turn an idea, problem description, or bug report into a well-formed GitHub issue and create it on GitHub once approved.
mode: agent
tools:
  - codebase
  - githubRepo
  - terminalLastCommand
arguments:
  - name: description
    description: The plain English description of the feature, bug, or improvement.
    required: true
---
# Create Issue

Create a GitHub issue for the following idea or problem:

> $DESCRIPTION

---

## Your job

Turn the description above into a well-formed GitHub issue ready to be picked up by a developer. Present the draft to the user, and once approved, create the issue on GitHub. Do NOT implement anything — output the issue draft only first.

---

## Step 1 — Understand the context

1. Search GitHub for existing issues or PRs that overlap with or inform this idea.
2. Read targeted wiki articles ONLY if the feature area requires architectural clarification:
   > - Sync / offline patterns → `wiki/offline-first-architecture.md`
   > - UI components / Horizon design → `wiki/horizon-narrative-design-language.md`
   > - Domain services → `wiki/core-services-catalog.md` (relevant section only)

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
  - Any architecture patterns that apply (e.g. Delegate sub-pattern, offline-first, MVI triad).
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

## Step 3 — Review and Create

Before presenting the draft, verify:
- The proposed solution aligns with the project's architecture rules.
- The acceptance criteria are concrete and testable, not vague.
- The scope is narrow enough to be completed in a single PR.
- No implementation details that belong in a PR description have leaked into the issue.

1. Present the full draft to the user for review.
2. Do NOT create the GitHub issue until the user explicitly approves it.
3. Once approved, create the GitHub issue and provide the link to the user.
