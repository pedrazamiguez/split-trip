---
name: sp-review-pr
description: Review code changes on a pull request and evaluate against project constraints.
mode: agent
tools:
  - codebase
  - githubRepo
  - terminalLastCommand
arguments:
  - name: pr_url
    description: The URL of the GitHub pull request to review.
    required: true
  - name: pr_number
    description: The number of the GitHub pull request to review.
    required: false
---
# Review Pull Request

Review the code changes on the following pull request:
- PR URL: $PR_URL
- PR Number: $PR_NUMBER

---

## Step 1 — Checkout & Diff Analysis

Perform the following operations to fetch the PR changes and inspect the files modified:
1. Identify the PR branch and target branch (typically `develop` or `main`).
2. Fetch the latest changes and checkout the PR branch.
3. Generate the diff between the PR branch and its target branch (e.g., develop or main):
   ```bash
   git diff origin/<target-branch>...HEAD
   ```
4. List all modified and newly created files to plan your review.

---

## Step 2 — Architecture Check (Strict Gating)

For each modified or new file in the diff:

- CHECK: Search for `Double` or `Float` used for currency amounts, shares, exchange rates, or percent splits — all must use `BigDecimal` with explicit `RoundingMode` and `scale`.
- CHECK: Firestore document-layer serialization uses `String` (`toPlainString()` / `toBigDecimalOrNull()`), not `Double`.
- CHECK: Run `wc -l <path/to/file.kt>` for every modified or new production `.kt` file — none may exceed 600 lines (test files exempt).
- CHECK: ViewModels must not inject Repositories, `Context`, `LocaleProvider`, or other ViewModels — only UseCases, Mappers, and Domain Services.
- CHECK: If a ViewModel's `onEvent()` handles >5 event categories or the file exceeds ~200 lines, verify that logic is extracted into plain Event Handler classes.
- CHECK: UI components use no raw 1px solid borders, use Plus Jakarta Sans + Manrope typography, and apply `LocalBottomPadding.current` on tab screens.
- CHECK: New functionality has corresponding unit or instrumentation tests. Kotlin's `assert()` is never used — JUnit assertions only.
- CHECK: Repositories launching coroutines inject a `CoroutineDispatcher` (default `Dispatchers.IO`) — never hardcoded.
- CHECK: Formatting is handled in UI Mappers (`*UiMapper` / `*UiMapperImpl`) via `LocaleProvider`. Never in ViewModels or Domain Services.

---

## Step 3 — Issue Categorization

Triage all architectural violations, bugs, and design inconsistencies into three distinct tiers:

### 🚨 Blockers / High
Violations in this tier are critical and MUST block merging. They will trigger a `REQUEST_CHANGES` review status.
- Severe architectural violations (e.g., VM direct repository dependency, direct cross-ViewModel injection).
- Usage of `Double`/`Float` for money math or currency values instead of `BigDecimal`.
- Source files exceeding the 600-line hard limit.
- Missing required unit or integration tests for new business logic.
- Broken compile/build or syntax errors.

### ⚠️ Medium
Violations in this tier affect maintainability or formatting, triggering a `COMMENT` review status if no Blockers are present.
- Non-conforming naming styles (e.g., presentation mappers not ending with `UiMapper` / `UiMapperImpl`).
- Unused/dead code left in ViewModels or components.
- Suboptimal database queries or missing `@Transaction` where needed.
- Missing inline comments explaining "why" for complex non-obvious logic.

### 💡 Low / Recommendations
Style adjustments, recommendations, or minor refactorings. These trigger a `COMMENT` or `APPROVE` review status.
- Style improvements, typos in documentation/comments.
- Minor performance refactoring suggestions.
- Redundant comments (describing *what* instead of *why*).

---

## Step 4 — Submit Review

Submit a structured PR review using the GitHub MCP server `pull_request_review_write` or equivalent review submission tool.

1. **Review Body**: Construct a markdown summary that lists:
   - A high-level overview of the pull request changes.
   - Categorized findings grouped under **Blockers / High**, **Medium**, and **Low / Recommendations**. For each finding, reference the file and line ranges if applicable.
   - A final verdict.
2. **Review Status**:
   - If there is at least one **Blocker / High** finding: Set state to `REQUEST_CHANGES`.
   - If there are only **Medium** or **Low** findings: Set state to `COMMENT`.
   - If no issues are found and the changes comply fully with all architectural guidelines: Set state to `APPROVE`.
