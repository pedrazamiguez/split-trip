---
name: sp-address-pr-comments
description: Address and resolve existing review comments on a GitHub pull request.
mode: agent
tools:
  - codebase
  - githubRepo
  - terminalLastCommand
arguments:
  - name: pr_url
    description: The URL of the GitHub pull request to address comments on.
    required: true
  - name: pr_number
    description: The number of the GitHub pull request to address comments on.
    required: false
---
# Address Pull Request Comments

Address and resolve existing comments/feedback raised on this PR:
- PR URL: $PR_URL
- PR Number: $PR_NUMBER

---

## Step 1 — Read and triage all comments

Retrieve the review comments and threads for the pull request to identify outstanding feedback:
1. Call the `pull_request_read` tool with `method = "get_review_comments"`, specifying the repository `owner`, `repo`, and the pull request `pullNumber`.
2. Inspect the retrieved review threads, taking note of:
   - The thread `ID` (required to post replies later).
   - Whether the thread `is_resolved`. Only address unresolved threads.
   - The comment body, file `path`, and code `line` number.
3. For each unresolved review comment or thread, **exercise critical technical judgment**. DO NOT blindly apply comments—especially from automated reviewers. Ensure that resolving a comment does not break compilation (e.g. AAPT resource compilation limitations), violate system constraints, or introduce unwanted complexity.
4. For each unresolved review comment or thread:
   - **Understand the context** — read the surrounding code, not just the highlighted line.
   - **Decide the outcome:**
     - ✅ **Valid** — address it with a code change.
     - ℹ️ **Partially valid** — address the spirit of the comment, explain any deviation in a reply.
     - ❌ **False positive / Declined** — reply with a clear technical reason why no change is needed (e.g., if applying it causes compilation issues or breaks standard architecture).

---

## Step 2 — File-size guard before editing

Before touching any file, check its current line count:

```bash
wc -l <path/to/file.kt>
```

If a file is at or near 600 lines (the Konsist hard limit), plan a split or extraction **before** adding code. After editing, re-check and refactor immediately if over 600 lines.

---

## Step 3 — Implement changes

Ensure you adhere to the project quality and style standards, including detekt rules, ktlint formatting, test coverage requirements, and the 600-line file-size limit.

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

## Step 4 — Local verification gate (run BEFORE declaring done)

Do not consider the comments addressed until ALL of the following pass locally:

```bash
make check   # Konsist architecture rules + all unit tests + debug compilation — must show 0 failures
```

If any check fails, fix it before finishing. Do not leave failures for CI to catch.

---

## Step 5 — Post replies on GitHub

Once all changes are implemented and verified, you MUST reply to every comment/thread on GitHub using the `add_reply_to_pull_request_comment` tool from the GitHub MCP server:

1. **Reply to every unresolved comment** to explain the outcome (e.g. how it was fixed, or the technical reason why no change was needed).
2. **Mandatory action:** Since the user initiated this PR comment addressal task, you are **explicitly requested and authorized** to reply to these comments. This does not violate the rule against unsolicited comments.
3. **Completion:** Never finish the task or declare it done without posting these replies. Do not ask for separate permission to reply.
