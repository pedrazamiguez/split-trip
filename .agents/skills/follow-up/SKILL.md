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
---# Follow-Up on Issue

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
2. Read targeted `/docs/` articles ONLY if the follow-up domain requires it:
   > - Decimal/currency math → `docs/domain/multi-currency-and-snapshots.md`
   > - Sync / offline patterns → `docs/architecture/patterns/offline-first.md`
   > - UI components / design tokens → `docs/design-system/horizon-narrative-design.md`
   > - Reusable services or components → `docs/architecture/patterns/core-services-catalog.md` (relevant section only)
   > - Data mapping → `docs/architecture/patterns/data-mapping-strategy.md`
   > - Architecture decisions → `docs/architecture/adrs/`
---

## Step 2 — Triage the Follow-Up Request & Screenshot

1. **Analyze the Issue Context**: Review the notes and context gathered from the GitHub issue comments and description.
2. **Understand the problem**: Read the `$FOLLOW_UP_DESCRIPTION` carefully and reconcile it with the issue context.
3. **Review Screenshot**: If `$SCREENSHOT_PATH` is provided, view the attached screenshot/image to see the visual discrepancy, crash, or unexpected UI state:
   - Use the appropriate file viewing/image tool to inspect the image contents.
4. **Locate the affected code**: Search the codebase for the features, ViewModels, Screens, or Services associated with the issue.

## Step 3 — Provide a Plain English Explanation, RCA, and Request Approval

Before generating any code or technical implementation plans, you MUST provide the user with a clear, plain-English explanation in your chat response. Do not skip this step.
1. Explain the **Root Cause**: What exactly was causing the bug or issue? Why was it happening?
2. Explain the **Proposed Fix**: How do you plan to fix it? Why will this solution work?
3. **Request Approval**: You MUST explicitly ask the user to confirm or review the proposed fix.
4. **STOP AND WAIT**: You MUST stop execution and wait for the user's explicit approval before proceeding to write any code, create implementation plans, or make changes. If the user suggests alternative approaches, adjust your plan and seek approval again.

## Step 4 — Post Implementation Plan as an Issue Comment

After the user has explicitly approved your proposed fix from Step 3, you MUST automatically post your proposed implementation plan/changes as a comment on the GitHub issue using `gh issue comment` via `run_command` (or fall back immediately to `gh` on any MCP error or missing URL) before writing code:
```bash
gh issue comment $ISSUE_NUMBER --repo pedrazamiguez/split-trip --body-file - <<'EOF'
<implementation plan...>
EOF
```
Verify that the command exits with code `0` and stdout contains an actual `https://github.com/pedrazamiguez/split-trip/issues/...` comment URL. Do not wait for the user to ask or perform this step manually; the agent must perform this step programmatically as part of this skill.

The comment must include:
- Summary of proposed changes per file
- Architecture compliance checklist confirmed for each new/modified component

Stick to the plan. If the plan needs to change, seek user approval for the changes first, then post an updated comment on the issue using `gh issue comment` (and verify exit code `0` plus the returned URL).
---

## Step 5 — File-Size Guard (600-line hard limit, enforced by Konsist)

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

## Step 6 — Implement

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

## Step 7 — Local Verification Gate (run BEFORE declaring done)

Use `make fast-check` for fast feedback during iterative development (~15–30s):
```bash
make fast-check > build.log 2>&1 && echo "Fast check passed" || (echo "Fast check failed. Last 100 lines:" && tail -n 100 build.log)
```

Do not consider the work complete until `make check` passes locally (full cold verification):
```bash
make check > build.log 2>&1 && echo "Check passed successfully" || (tail -n 100 build.log && exit 1)
```

If any check fails, fix it before finishing. Do not leave the user to discover failures in CI.
---

## Step 8 — Clean up helper scripts

After `make check` passes, remove any **temporary** helper scripts (`.py`, `.sh`, etc.) that were created during this task for refactoring, bulk edits, or code generation. These must not remain in the working copy. Do NOT delete scripts inside the `scripts/` directory — those are permanent project utilities.

```bash
# Find any temporary helper scripts created during this session
find . -maxdepth 3 -type f \( -name "*.py" -o -name "*.sh" \) \
  ! -path "./scripts/*" ! -path "./.agents/*" ! -path "./gradlew" \
  ! -path "./.git/*" ! -path "*/build/*" ! -path "*/graphify-out/*" 2>/dev/null
```

Review the list. Delete every script that was created as a helper for this task. If in doubt, delete it — production code in this project is Kotlin only. Also remove any `.log` files generated by build commands:

```bash
find . -maxdepth 1 -name "*.log" -delete
```
---

## Step 9 — Synchronize AI Knowledge Indexes

Before posting the walkthrough and declaring done, ensure that all AI knowledge indexes (code syntax graphs, documentation RAG, and module topology) reflect the latest codebase and documentation changes:

```bash
make knowledge-update
```
---

## Step 10 — Post walkthrough as an issue comment

After verifying that all checks pass locally and your work is complete, you MUST automatically post the walkthrough you generate as a comment on the GitHub issue using `gh issue comment` via `run_command` (or fall back immediately to `gh` on any MCP error or missing URL) before finishing the task:
```bash
gh issue comment $ISSUE_NUMBER --repo pedrazamiguez/split-trip --body-file - <<'EOF'
<walkthrough...>
EOF
```
Verify that the command exits with code `0` and stdout contains an actual `https://github.com/pedrazamiguez/split-trip/issues/...` comment URL before finishing. Do not wait for the user to ask or perform this step manually; the agent must perform this step programmatically as part of this skill.

The comment must include:
- A summary of the changes made and what was accomplished
- A summary of the testing and validation results (e.g. `make check` results, unit tests executed)
