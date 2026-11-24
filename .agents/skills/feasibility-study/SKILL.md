---
name: sp-feasibility-study
description: Study the feasibility and viability of a proposed feature or architectural change.
mode: agent
tools:
  - codebase
  - githubRepo
  - terminalLastCommand
arguments:
  - name: feature_description
    description: The plain English description of the feature or architectural change to study.
    required: true
  - name: issue_number
    description: The number of the GitHub issue to link the feasibility study to (optional).
    required: true
---# Feasibility Study

Study the feasibility and viability of the following proposed feature or architectural change:
- Issue URL: $ISSUE_URL (optional)
- Issue Number: $ISSUE_NUMBER (optional)

> $FEATURE_DESCRIPTION
---

## Step 1 — Research the domain and requirements

Thoroughly research the feature description and explore the codebase to understand the context:
1. Identify which modules and packages in the project would be affected by the proposed changes.
2. Search GitHub or the local codebase for any existing or overlapping implementations.
3. Read targeted `/docs/` articles ONLY if the feature area requires it:
   > - Sync / offline patterns → `docs/architecture/patterns/offline-first.md`
   > - UI components / Horizon design → `docs/design-system/horizon-narrative-design.md`
   > - Reusable services or components → `docs/architecture/patterns/core-services-catalog.md` (relevant section only)
   > - Data mapping → `docs/architecture/patterns/data-mapping-strategy.md`
   > - Architecture decisions → `docs/architecture/adrs/`
---

## Step 2 — Analyse data layer and synchronization implications

If the feature touches state or persistent data:
1. Examine how it integrates with the offline-first architecture (Room database schemas, Kotlin serialization, and Firestore sync delegates). If needed, refer to `docs/architecture/patterns/offline-first.md` for patterns.
2. Verify that any decimal or monetary values use `BigDecimal` rather than `Double` or `Float` to prevent precision loss.
3. Plan local UUID generation and database transaction handling.
---

## Step 3 — Analyse UI and Design System impacts

If the feature touches the UI:
1. Review the "Horizon Narrative" design guidelines in `docs/design-system/horizon-narrative-design.md` if applicable.
2. Determine if existing components from `:core:design-system` can be reused, or if new components are required.
3. Account for UI state constraints (MVI pattern, stateless Screen composables, LocalBottomPadding application).
---

## Step 4 — Identify technical risks and trade-offs

Assess potential issues:
1. Highlight any performance, scalability, or memory footprint concerns.
2. Identify potential breaking changes or conflicts with existing functionality.
3. List external library dependencies that would be required, evaluating their size, licenses, and compatibility.
---

## Step 5 — Document findings, present to user, and optionally post to GitHub

1. Create a detailed feasibility report containing:
   - **Executive Summary**: A clear recommendation on whether the project should proceed with this feature.
   - **Proposed Architecture**: Recommended module assignments, Clean Architecture layer mapping, and data schemas.
   - **Spec-Driven Development (SDD) Alignment**: High-level specification of key interface contracts, function signatures, database schema changes, and MVI state changes to serve as an unambiguous foundation for the subsequent implementation plan.
   - **Implementation Trade-offs**: Alternative libraries or design patterns that were considered, with pros and cons for each.
   - **Estimated Effort / Risk**: Areas of highest complexity or technical uncertainty.
2. Present the feasibility report directly to the user in the chat and ask for their feedback.
3. Prompt the user to ask if they would like the report to be posted as a comment on the associated GitHub issue.
4. If the user agrees and the issue number/URL is known (or provided by the user in their response), post the report as a comment on the associated GitHub issue using `gh issue comment` via `run_command` (or fall back immediately to `gh` on any MCP error or missing URL):
   ```bash
   gh issue comment $ISSUE_NUMBER --repo pedrazamiguez/split-trip --body-file - <<'EOF'
   <feasibility report...>
   EOF
   ```
   Verify that the command exits with code `0` and stdout contains an actual `https://github.com/pedrazamiguez/split-trip/issues/...` comment URL before notifying the user.
5. Notify the user once the comment has been successfully posted.
