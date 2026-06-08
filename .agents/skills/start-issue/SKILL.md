---
name: sp-start-issue
description: Implement a GitHub issue by strictly following the implementation plan posted on the issue.
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

---

## Step 0 — Branch validation

Before writing any code or performing checks, verify your local Git state:
1. Ensure you are on the correct branch for this task.
2. If starting work on a new issue, ensure a branch has been created from `develop` following the branch naming convention.
3. If this is a hotfix, ensure the branch has been created from `main`.
4. Pull the latest changes from the remote repository to guarantee your branch is fully up-to-date with `develop` or `main`.

---

## Step 1 — Load issue context

1. Fetch issue `$ISSUE_NUMBER` (`get` + `get_comments`), all linked issues, and every comment thread.
2. Read targeted wiki articles ONLY if the implementation plan or issue domain requires it:
   > - Decimal/currency math → `wiki/multi-currency-logic-and-snapshot-model.md`
   > - Sync / offline patterns → `wiki/offline-first-architecture.md`
   > - UI components → `wiki/core-services-catalog.md` §A, `wiki/horizon-narrative-design-language.md`
   > - Domain services → `wiki/core-services-catalog.md` §B–§G (relevant entry only)

---

## Step 2 — Find the Posted Implementation Plan

Retrieve all comments on the GitHub issue using the `github-mcp-server`.
1. Locate the implementation plan posted as a comment on the issue.
2. **If no implementation plan is found on the GitHub issue, halt immediately.** Do not investigate, deeply analyze, or suggest any technical solution. Stop and inform the user that they must first run the `sp-plan-issue` skill to generate and approve a plan.
3. If an implementation plan is found, read it thoroughly. This plan is your single source of truth for the implementation.
4. If you discover that the plan does not align well with the current code, **do not attempt to fix or replan it yourself**. Stop immediately and suggest that the user run the `sp-replan-issue` skill to update the plan.

---

## Step 3 — File-size guard (600-line hard limit, enforced by Konsist)

Before editing any file, check its current line count:

```bash
wc -l <path/to/file.kt>
```

If the file is already at or near 600 lines, factor that into your implementation (split, extract delegate, etc.) **before** adding new code. Do NOT add code to a file that will push it over 600 lines.

After editing, re-check:

```bash
wc -l <path/to/file.kt>
```

If the result exceeds 600 lines, refactor immediately — do not move on.

---

## Step 4 — Implement the Plan

Implement the technical solution by sticking strictly to the posted implementation plan. Do not perform any deep design analysis or suggest new technical directions.

- REQUIREMENT: No pragmatic patches. Clean architecture only.
- REQUIREMENT: ViewModels inject only UseCases, Mappers, Domain Services.
- FORBIDDEN: ViewModels injecting Context, LocaleProvider, Repositories, or other ViewModels.
- REQUIREMENT: BigDecimal with explicit RoundingMode and scale for all decimal math.
- FORBIDDEN: Double or Float for money, percentage, or exchange-rate values.
- REQUIREMENT: Offline-first — Room write first, cloud sync via reusable delegates.
- REQUIREMENT: Production source files ≤ 600 lines.
- REQUIREMENT: Formatting in UiMappers only. Never in ViewModels or Domain Services.
- REQUIREMENT: Comment the *why*, not the *what*. No redundant comments.

---

## Step 5 — Local verification gate (run BEFORE declaring done)

Do not consider the work complete until ALL of the following pass locally:

```bash
make check   # Konsist architecture rules + all unit tests + debug compilation — must show 0 failures
```

If any check fails, fix it before finishing. Do not leave the user to discover failures in CI.

---

## Step 6 — Post walkthrough as an issue comment

After verifying that all checks pass locally and your work is complete, you MUST automatically post the walkthrough you generate as a comment on the GitHub issue using the github-mcp-server tool `add_issue_comment` before finishing the task. Do not wait for the user to ask or perform this step manually; the agent must perform this step programmatically as part of this skill.

The comment must include:
- A summary of the changes made and what was accomplished (referencing the original implementation plan)
- A summary of the testing and validation results (e.g. `make check` results, unit tests executed)
