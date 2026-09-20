---
name: sp-plan-issue
description: Plan a GitHub issue by investigating context and creating a detailed implementation plan. Present it to the user, and post it to the issue as a comment upon approval. No code changes.
mode: agent
tools:
  - codebase
  - githubRepo
  - terminalLastCommand
arguments:
  - name: issue_number
    description: The number of the GitHub issue to plan.
    required: true
---# Plan Issue

Plan a technical solution for this issue:
- Issue Number: $ISSUE_NUMBER
---

> [!IMPORTANT]
> **CRITICAL: DO NOT WRITE ANY CODE OR MAKE CODEBASE CHANGES.**
> This skill's scope ends with posting the approved plan to the GitHub issue. Under no circumstances should you edit or create production source code files, run modifying commands, or begin implementing the plan.
---

## Step 1 — Load issue context

1. Fetch issue `$ISSUE_NUMBER` (`get` + `get_comments`), all parent/linked issues, and every comment thread.
2. Read targeted `/docs/` articles ONLY if the task domain requires it:
   > - Decimal/currency math → `docs/domain/multi-currency-and-snapshots.md`
   > - Sync / offline patterns → `docs/architecture/patterns/offline-first.md`
   > - UI components / design tokens → `docs/design-system/horizon-narrative-design.md`
   > - Reusable services or components → `docs/architecture/patterns/core-services-catalog.md` (relevant section only)
   > - Data mapping → `docs/architecture/patterns/data-mapping-strategy.md`
   > - Architecture decisions → `docs/architecture/adrs/`
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
2. **When running in Antigravity:** You MUST present the plan using an `implementation_plan.md` artifact with `request_feedback=true`. You must STOP calling tools and wait for the user to click "Proceed" or provide feedback.
3. If the user requests changes, refine the plan and present it again.
4. **DO NOT** make any codebase changes, write production code, or execute any modifications.
---

## Step 4 — Post to GitHub

Once (and only after) the user has explicitly approved the plan (e.g. by clicking "Proceed"):
1. Post the final approved implementation plan as a comment on the GitHub issue using `add_issue_comment`.
   - **CRITICAL:** You MUST post the **entire, unabridged content** of the approved `implementation_plan.md` artifact verbatim.
   - **DO NOT summarize, condense, truncate, or omit** any sections, file lists, `[NEW]`/`[MODIFY]`/`[DELETE]` tags, exact file paths, line numbers, signatures, or test case specifications.
   - The comment posted to GitHub is the single source of truth that `sp-start-issue` will parse and validate; if it is summarized or abbreviated, `sp-start-issue` will fail validation and reject the plan.
2. Notify the user that the plan has been posted.
3. **STOP immediately.** The user clicking "Proceed" means "post the plan and stop." It does NOT mean you should begin execution. You have achieved your goal for this skill. Do not start implementing or executing code.
