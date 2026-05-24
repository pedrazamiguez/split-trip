---
name: follow-up
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

## Step 1 — Read Everything First (mandatory, no shortcuts)

Read and analyze all of the following before doing any implementation planning or writing code:
1. **GitHub Issue & Comments**: Fetch, read, and analyze the referenced GitHub issue `$ISSUE_NUMBER` and all its comment threads using the `issue_read` tool (owner: `pedrazamiguez`, repo: `split-trip`, methods: `get` and `get_comments`) to understand the complete historical context and initial implementation.
2. **Project Guidelines**:
   - [.github/copilot-instructions.md](../../../.github/copilot-instructions.md)
   - [AGENTS.md](../../../AGENTS.md)
   - [DESIGN.md](../../../DESIGN.md)
3. **Documentation**: All relevant `wiki/*.md` articles (especially [wiki/core-services-catalog.md](../../../wiki/core-services-catalog.md) and [wiki/offline-first-architecture.md](../../../wiki/offline-first-architecture.md)).

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
- Architecture compliance checklist (from [AGENTS.md](../../../AGENTS.md)) confirmed for each new/modified component

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

Follow all architecture constraints in [AGENTS.md](../../../AGENTS.md) and [.github/copilot-instructions.md](../../../.github/copilot-instructions.md) strictly.

**Core Reminders:**
- **No Pragmatic Patches:** Write clean, modular, production-ready code. Do not use temporary workarounds.
- **BigDecimal Math:** Use `BigDecimal` with an explicit scale and rounding mode for all precision-sensitive calculations (never `Double` or `Float`).
- **Offline-First Protocol:** Generate UUIDs and timestamps locally, write to Room first, and sync to Firestore in the background using the reusable sync delegates.
- **Design System:** Comply with the "Horizon Narrative" guidelines (no raw 1px borders, Outfit/Inter/Jakarta Sans typography, tonal layering, and bottom padding via `LocalBottomPadding` on tab screens).
- **Commenting Policy:** Comment the *why*, never the *what*. Avoid redundant comments.

---

## Step 6 — Local Verification Gate (run BEFORE declaring done)

Do not consider the work complete until ALL of the following pass locally:
```bash
make check   # Konsist architecture rules + all unit tests + debug compilation — must show 0 failures
```

If any check fails, fix it before finishing. Do not leave the user to discover failures in CI.
