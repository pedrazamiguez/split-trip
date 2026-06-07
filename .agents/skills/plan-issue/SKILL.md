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

## Your Job

Investigate the issue, linked issues, comments, codebase context, and architectural constraints to formulate a detailed technical solution (step-by-step implementation plan). Present this plan to the user. Once the user approves, post the implementation plan as a comment on the GitHub issue using the `add_issue_comment` tool from the `github-mcp-server`.

> [!IMPORTANT]
> **CRITICAL: DO NOT WRITE ANY CODE OR MAKE CODEBASE CHANGES.**
> This skill's scope ends with posting the approved plan to the GitHub issue. Under no circumstances should you edit or create production source code files, run modifying commands, or begin implementing the plan.

---

## Step 1 — Read everything first (mandatory, no shortcuts)

Read each of the following before formulating your plan:

- [.github/copilot-instructions.md](../../../.github/copilot-instructions.md)
- [AGENTS.md](../../../AGENTS.md)
- [DESIGN.md](../../../DESIGN.md)
- All relevant `wiki/*.md` articles (especially [wiki/core-services-catalog.md](../../../wiki/core-services-catalog.md))
- The GitHub issue itself, including ALL comments and any linked/parent issues

---

## Step 2 — Formulate the Technical Solution

Act as a professional Android Tech Lead. Design a clean, robust, and detailed implementation plan.

Your plan must adhere strictly to the guidelines in [AGENTS.md](../../../AGENTS.md):
1. **Clean Architecture & Modules**: Ensure feature isolation. ViewModels must never depend on Repositories or context. Features only see `:domain` and `:core`.
2. **MVI Triad**: Define `UiState` (immutable list/data class), `UiEvent` (sealed interface), and `UiAction` (side effects via Flow).
3. **Hot Flows**: Use stateIn with `AppConstants.FLOW_RETENTION_TIME` and `AppConstants.FLOW_REPLAY_EXPIRATION`.
4. **BigDecimal & Decimal Precision**: Use `BigDecimal` with explicit `RoundingMode` and `scale` for all domain math. Do not use Float or Double.
5. **Offline-First Data Flow**: Reconcile local database changes using Room first, then sync to Cloud. Utilize the reusable sync delegates (e.g. `subscribeAndReconcile`, `syncCreateToCloud`, `KeyedSubscriptionTracker`) in `:data`.
6. **File-Size Guards**: Ensure no production source file will exceed the 600-line limit. Plan class extractions (Event Handlers, Delegates) in advance.
7. **Design System (Horizon)**: Utilize the Horizon theme wrappers, design system components, and proper bottom padding/decorative icon accessibility standards.

Draft a detailed, step-by-step implementation plan including:
- A clear summary of the problem and approach.
- File-by-file changes categorized by component (showing file paths, new/modified/deleted files, classes, mappers, DI wiring, and unit tests).
- Concrete code snippets / signatures for key logic.

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
