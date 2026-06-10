---
name: sp-plan-issue
description: Plan a GitHub issue by investigating context and creating a detailed implementation plan. Present it to the user, and post it to the issue as a comment upon approval. No code changes.
mode: agent
tools:
  - codebase
  - githubRepo
  - terminalLastCommand
arguments:
  - name: issue_url
    description: The URL of the GitHub issue to plan.
    required: true
  - name: issue_number
    description: The number of the GitHub issue to plan.
    required: false
---
# Plan Issue

Plan a technical solution for this issue:
- Issue URL: $ISSUE_URL
- Issue Number: $ISSUE_NUMBER

---

> [!IMPORTANT]
> **CRITICAL: DO NOT WRITE ANY CODE OR MAKE CODEBASE CHANGES.**
> This skill's scope ends with posting the approved plan to the GitHub issue. Under no circumstances should you edit or create production source code files, run modifying commands, or begin implementing the plan.

---

## Step 1 — Load issue context

1. Fetch issue `$ISSUE_NUMBER` (`get` + `get_comments`), all parent/linked issues, and every comment thread.
2. Read targeted wiki articles ONLY if the task domain requires it:
   > - Decimal/currency math → `wiki/multi-currency-logic-and-snapshot-model.md`
   > - Sync / offline patterns → `wiki/offline-first-architecture.md`
   > - UI components / design tokens → `wiki/horizon-narrative-design-language.md`
   > - Reusable services or components → `wiki/core-services-catalog.md` (relevant section only)
   > - Data mapping → `wiki/data-mapping-strategy-and-architecture.md`

---

## Step 2 — Formulate the Technical Solution

- REQUIREMENT: No pragmatic patches. Clean architecture only.
- REQUIREMENT: ViewModels inject only UseCases, Mappers, Domain Services.
- FORBIDDEN: ViewModels injecting Context, LocaleProvider, Repositories, or other ViewModels.
- REQUIREMENT: All decimal math → BigDecimal with explicit RoundingMode and scale.
- FORBIDDEN: Double or Float for money, percentage, or exchange-rate values.
- REQUIREMENT: Offline-first — Room write first; cloud sync in background via reusable delegates.
- REQUIREMENT: IDs and timestamps generated locally (UUID + System.currentTimeMillis()).
- REQUIREMENT: Production source files ≤ 600 lines. Extract handlers/delegates before going over.
- REQUIREMENT: Formatting in UiMappers only (via LocaleProvider). Never in ViewModels or Domain Services.
- REQUIREMENT: Tab screens use LocalBottomPadding.current for all scrollable lists, FABs, bottom buttons.

Draft a plan that functions as a **complete, unambiguous, and actionable technical specification**. The plan must explicitly define:
- Exact target file paths (categorized by component, using `[NEW]`, `[MODIFY]`, or `[DELETE]` tags for each file).
- Precise class names, interface/function signatures, constructor parameter names, parameter types, and return types.
- Database schema updates (Room entity fields, Firestore document mappings) with exact types.
- Control flow logic, business validation rules, and error handling expectations.
- Specific test specifications (test class name, list of test case names with their input/output expectations and what should be mocked).

---

## Step 3 — Present the Plan and Obtain Approval

1. Present the draft implementation plan directly to the user in the chat.
2. Ask for feedback or approval.
3. If the user requests changes, refine the plan and present it again.
4. **DO NOT** make any codebase changes, write production code, or execute any modifications.

---

## Step 4 — Post to GitHub

Once (and only after) the user has explicitly approved the plan:
1. Post the final approved implementation plan as a comment on the GitHub issue using `add_issue_comment`.
2. Notify the user that the plan has been posted.
3. **STOP immediately.** Do not start implementing or executing code.
