---
name: sp-replan-issue
description: Revisit and revise a proposed implementation plan or solution for an issue using a different approach, architecture, or set of libraries, presenting it to the user for approval and updating the GitHub issue.
mode: agent
tools:
  - codebase
  - githubRepo
  - terminalLastCommand
arguments:
  - name: issue_url_or_number
    description: The GitHub issue URL or number to replan.
    required: true
  - name: feedback_or_alternatives
    description: The feedback, new requirements, or alternative approaches to incorporate.
    required: true
---
# Replan Issue

Revisit and revise the implementation plan or proposed solution for GitHub issue:
- Issue: $ISSUE_URL_OR_NUMBER

Incorporating the following feedback or alternative approaches:
- Feedback / Alternatives: $FEEDBACK_OR_ALTERNATIVES

---

## Your job

Analyze the issue, existing plan/solution, and feedback to draft a revised proposal/solution. Do NOT implement any changes or modify the codebase. Present the revised proposal/plan to the user. Once approved, update the GitHub issue with the accepted proposal.

---

## Step 1 — Review the issue and feedback

Load and examine the current issue, existing plan/comments, and proposed feedback:
1. Fetch and read the GitHub issue content and discussion.
2. If there is an existing `implementation_plan.md` or proposal draft, locate and read it.
3. Read the review feedback, comments, or alternative proposals.

---

## Step 2 — Compare the alternative approaches

Perform a comparative analysis of the original approach against the proposed alternatives:
1. **Trade-offs**: Compare ease of implementation, code complexity, performance, and risk.
2. **Dependencies**: Evaluate any additional library sizes, licenses, or compilation impacts.
3. **Data Integrity**: Assess how the alternative impacts the offline-first sync architecture (Room transactions, cloud syncing delegates).
4. **Maintenance**: Analyse the long-term impact on readability and refactoring.

---

## Step 3 — Align with Clean Architecture and project guidelines

Ensure that the proposed alternative strictly complies with the rules in [AGENTS.md](../../../AGENTS.md):
1. **Module Visibility**: Confirm that the new approach does not violate feature isolation (features must not see other features or `:data`).
2. **Triad Pattern**: Verify that ViewModels do not inject Repositories and only interact via Use Cases, Mappers, or Domain Services.
3. **Event Handlers & Delegates**: If the complexity of any handler class exceeds 200 lines, extract event handling into dedicated Event Handler classes.
4. **File-Size Guards**: Ensure that none of the modified files will exceed the 600-line hard limit.

---

## Step 4 — Draft the revised proposal

Draft a revised proposed solution or plan:
1. Clearly document the new design decisions, highlight the chosen alternative, and provide a clear technical rationale for the change.
2. Update the checklist of files to be modified, created, or deleted.
3. Adjust the verification plan (both automated unit/instrumentation tests and manual checks).
4. **Do NOT modify any code or execute changes.**
5. Present the draft proposal to the user for feedback and approval.

---

## Step 5 — Update the GitHub Issue

Once (and only after) the user explicitly approves/accepts the proposed plan:
1. Post the revised plan/proposal as a comment or update the issue description on the GitHub issue.
2. Confirm the update to the user.
