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
---
# Feasibility Study

Study the feasibility and viability of the following proposed feature or architectural change:

> $FEATURE_DESCRIPTION

---

## Step 1 — Research the domain and requirements

Thoroughly research the feature description and explore the codebase to understand the context:
1. Identify which modules and packages in the project would be affected by the proposed changes.
2. Search GitHub or the local codebase for any existing or overlapping implementations.
3. Read targeted wiki articles ONLY if the feature area requires it:
   > - Sync / offline patterns → `wiki/offline-first-architecture.md`
   > - UI components / Horizon design → `wiki/horizon-narrative-design-language.md`
   > - Reusable services or components → `wiki/core-services-catalog.md` (relevant section only)
   > - Data mapping → `wiki/data-mapping-strategy-and-architecture.md`

---

## Step 2 — Analyse data layer and synchronization implications

If the feature touches state or persistent data:
1. Examine how it integrates with the offline-first architecture (Room database schemas, Kotlin serialization, and Firestore sync delegates). If needed, refer to `wiki/offline-first-architecture.md` for patterns.
2. Verify that any decimal or monetary values use `BigDecimal` rather than `Double` or `Float` to prevent precision loss.
3. Plan local UUID generation and database transaction handling.

---

## Step 3 — Analyse UI and Design System impacts

If the feature touches the UI:
1. Review the "Horizon Narrative" design guidelines in `wiki/horizon-narrative-design-language.md` if applicable.
2. Determine if existing components from `:core:design-system` can be reused, or if new components are required.
3. Account for UI state constraints (MVI pattern, stateless Screen composables, LocalBottomPadding application).

---

## Step 4 — Identify technical risks and trade-offs

Assess potential issues:
1. Highlight any performance, scalability, or memory footprint concerns.
2. Identify potential breaking changes or conflicts with existing functionality.
3. List external library dependencies that would be required, evaluating their size, licenses, and compatibility.

---

## Step 5 — Document findings and recommendations

Create a detailed feasibility report containing:
- **Executive Summary**: A clear recommendation on whether the project should proceed with this feature.
- **Proposed Architecture**: Recommended module assignments, Clean Architecture layer mapping, and data schemas.
- **Implementation Trade-offs**: Alternative libraries or design patterns that were considered, with pros and cons for each.
- **Estimated Effort / Risk**: Areas of highest complexity or technical uncertainty.
