---
name: sp-follow-up
description: Request follow-up work on a specific issue (e.g. bug fixes, additional requirements, visual regression, or debugging) in a new or existing conversation without prior context.
mode: agent
tools:
  - codebase
  - githubRepo
  - terminalLastCommand
arguments:
  - name: issue_number
    description: The number of the GitHub issue to follow up on. You must read and analyze this issue and its comments first to get the necessary context.
    required: true
  - name: follow_up_description
    description: An explanation of what needs to be checked, fixed, or done.
    required: true
  - name: screenshot_path
    description: Optional path to a screenshot or image demonstrating the issue. This path is automatically populated by the system when a screenshot is attached to the conversation (the user does not write this path manually).
    required: false
---
# Follow-Up on Issue

Follow up on the work for this issue:
- Issue Number: $ISSUE_NUMBER
- Follow-Up Description: $FOLLOW_UP_DESCRIPTION
- Screenshot Path: $SCREENSHOT_PATH

---

## Step 0 — Validate Branch and Context

Because this follow-up may be initiated in a new conversation without prior context, verify your local Git state:
1. Identify the current Git branch:
   ```bash
   git branch --show-current
   ```
2. Verify that you are on the correct branch corresponding to issue `$ISSUE_NUMBER` (e.g., the branch name should contain the issue number, such as `feature/issue-$ISSUE_NUMBER-...` or similar).
3. If not on the correct branch, switch to the correct branch.
4. Pull the latest changes from the remote repository to ensure your branch is fully up-to-date:
   ```bash
   git pull
   ```

---

## Step 1 — Load issue context

1. Fetch issue `$ISSUE_NUMBER` (`get` + `get_comments`) and all its comment threads to understand the complete historical context and initial implementation.
2. Read targeted wiki articles ONLY if the follow-up domain requires it:
   > - Decimal/currency math → `wiki/multi-currency-logic-and-snapshot-model.md`
   > - Sync / offline patterns → `wiki/offline-first-architecture.md`
   > - UI components / design tokens → `wiki/horizon-narrative-design-language.md`
   > - Reusable services or components → `wiki/core-services-catalog.md` (relevant section only)
   > - Data mapping → `wiki/data-mapping-strategy-and-architecture.md`

---

## Step 2 — Triage the Follow-Up Request & Screenshot

1. **Analyze the Issue Context**: Review the notes and context gathered from the GitHub issue comments and description.
2. **Understand the problem**: Read the `$FOLLOW_UP_DESCRIPTION` carefully and reconcile it with the issue context.
3. **Review Screenshot**: If `$SCREENSHOT_PATH` is provided, view the attached screenshot/image to see the visual discrepancy, crash, or unexpected UI state:
   - Use the appropriate file viewing/image tool to inspect the image contents.
4. **Locate the affected code**: Search the codebase for the features, ViewModels, Screens, or Services associated with the issue.

## Step 3 — Post Implementation Plan as an Issue Comment

If the triage in Step 2 reveals that code changes are required, you MUST automatically post your proposed implementation plan/changes as a comment on the GitHub issue using the github-mcp-server tool `add_issue_comment` before writing code. Do not wait for the user to ask or perform this step manually; the agent must perform this step programmatically as part of this skill.

The comment must include:
- Summary of proposed changes per file
   - Architecture compliance checklist confirmed for each new/modified component

Stick to the plan. If the plan needs to change, update the comment on the issue (also automatically using the github-mcp-server).

---

## Step 4 — File-Size Guard (600-line hard limit, enforced by Konsist)

Before editing any file, check its current line count:
```bash
wc -l <path/to/file.kt>
```
If the file is already at or near 600 lines, factor that into your plan (split, extract event handler, extract delegate, etc.) **before** adding new code. Do NOT add code to a file that will push it over 600 lines.

After editing, re-check:
```bash
wc -l <path/to/file.kt>
```
If the result exceeds 600 lines, refactor immediately — do not move on.

---

## Step 5 — Implement

REQUIREMENT: No pragmatic patches. Clean architecture only.
REQUIREMENT: ViewModels inject only UseCases, Mappers, Domain Services.
FORBIDDEN: ViewModels injecting Context, LocaleProvider, Repositories, or other ViewModels.
REQUIREMENT: BigDecimal with explicit RoundingMode and scale for all decimal math.
FORBIDDEN: Double or Float for money, percentage, or exchange-rate values.
REQUIREMENT: Offline-first — Room write first, cloud sync via reusable delegates.
REQUIREMENT: Production source files ≤ 600 lines.
REQUIREMENT: Formatting in UiMappers only. Never in ViewModels or Domain Services.
REQUIREMENT: Comment the *why*, not the *what*. No redundant comments.

---

## Step 6 — Local Verification Gate (run BEFORE declaring done)

Do not consider the work complete until ALL of the following pass locally:
```bash
make check   # Konsist architecture rules + all unit tests + debug compilation — must show 0 failures
```

If any check fails, fix it before finishing. Do not leave the user to discover failures in CI.

---

## Step 7 — Post walkthrough as an issue comment

After verifying that all checks pass locally and your work is complete, you MUST automatically post the walkthrough you generate as a comment on the GitHub issue using the github-mcp-server tool `add_issue_comment` before finishing the task. Do not wait for the user to ask or perform this step manually; the agent must perform this step programmatically as part of this skill.

The comment must include:
- A summary of the changes made and what was accomplished
- A summary of the testing and validation results (e.g. `make check` results, unit tests executed)
