---
name: revise-plan
description: Revisit and revise a proposed implementation plan using a different approach, architecture, or set of libraries.
mode: agent
tools:
  - codebase
  - githubRepo
  - terminalLastCommand
arguments:
  - name: plan_url_or_path
    description: The URL or file path of the existing implementation plan to revise.
    required: true
  - name: feedback_or_alternatives
    description: The feedback or alternative approaches to explore and incorporate.
    required: true
---
# Revise Plan

Revisit and revise the implementation plan located at:
- Plan URL / Path: $PLAN_URL_OR_PATH

Explore and incorporate the following feedback or alternative approaches:
- Feedback / Alternatives: $FEEDBACK_OR_ALTERNATIVES

---

## Step 1 — Review the existing plan and feedback

Load and examine the current plan and the proposed feedback:
1. Locate and read the existing implementation plan file (e.g. `implementation_plan.md`).
2. Read the review feedback, comments, or alternative proposals.

---

## Step 2 — Compare the alternative approaches

Perform a comparative analysis of the original approach against the proposed alternatives:
1. **Trade-offs**: Compare ease of implementation, code complexity, performance, and risk.
2. **Dependencies**: Evaluate any additional library sizes, licenses, or compilation impacts.
3. **Data Integrity**: Assess how the alternative impacts the offline-first sync architecture (Room transactions, cloud syncing delegates).
4. **Maintenance**: Analyse the long-term impact on readability and refactoring.

---

## Step 3 — Align with Clean Architecture and project guidelines

Ensure that the chosen alternative strictly complies with the rules in [AGENTS.md](../../../AGENTS.md):
1. **Module Visibility**: Confirm that the new approach does not violate feature isolation (features must not see other features or `:data`).
2. **Triad Pattern**: Verify that ViewModels do not inject Repositories and only interact via Use Cases, Mappers, or Domain Services.
3. **Event Handlers & Delegates**: If the complexity of any handler class exceeds 200 lines, extract event handling into dedicated Event Handler classes.
4. **File-Size Guards**: Ensure that none of the modified files will exceed the 600-line hard limit.

---

## Step 4 — Draft the revised implementation plan

Update the existing `implementation_plan.md` file (or draft a new version if requested):
1. Clearly document the new design decisions, highlight the chosen alternative, and provide a clear technical rationale for the change.
2. Update the checklist of files to be modified, created, or deleted.
3. Adjust the verification plan (both automated unit/instrumentation tests and manual checks).
4. Present the revised plan to the user for feedback and approval before proceeding with execution.
