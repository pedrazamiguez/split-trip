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

## Step 1 — Fetch Metadata, Existing Comments & Diff Analysis

Perform the following operations to prepare for the review:
1. Identify the PR branch and target branch (typically `develop` or `main`).
2. Fetch the latest changes and checkout the PR branch.
3. Retrieve existing comments and review threads on the PR using the GitHub MCP server `pull_request_read` with `get_review_comments` (or equivalent). Analyze what concerns have already been raised (especially by automation/Copilot).
4. Generate the diff between the PR branch and its target branch (e.g., develop or main):
   ```bash
   git diff origin/<target-branch>...HEAD
   ```
5. List all modified and newly created files to plan your review.

---

## Step 2 — Deep Architecture & Logic Review

Do not perform a superficial checklist review. Inspect every modified and new file deeply for logic, performance, and correctness:

### 1. Hard Architectural Rules (Gating)
- CHECK: Search for `Double` or `Float` used for currency amounts, shares, exchange rates, or percent splits — all must use `BigDecimal` with explicit `RoundingMode` and `scale`.
- CHECK: Firestore document-layer serialization uses `String` (`toPlainString()` / `toBigDecimalOrNull()`), not `Double`.
- CHECK: Run `wc -l <path/to/file.kt>` for every modified or new production `.kt` file — none may exceed 600 lines (test files exempt).
- CHECK: ViewModels must not inject Repositories, `Context`, `LocaleProvider`, or other ViewModels — only UseCases, Mappers, and Domain Services.
- CHECK: If a ViewModel's `onEvent()` handles >5 event categories or the file exceeds ~200 lines, verify that logic is extracted into plain Event Handler classes.
- CHECK: Formatting is handled in UI Mappers (`*UiMapper` / `*UiMapperImpl`) via `LocaleProvider`. Never in ViewModels or Domain Services.
- CHECK: New functionality has corresponding unit or instrumentation tests. Kotlin's `assert()` is never used — JUnit assertions only.
- CHECK: Repositories launching coroutines inject a `CoroutineDispatcher` (default `Dispatchers.IO`) — never hardcoded.

### 2. Deep Logic, Robustness & Edge Cases
- CHECK: Nullability & Silent Fails — Ensure the code doesn't silently ignore unexpected null values (e.g., early returns from click/save actions without notifying the user or showing an error state).
- CHECK: Flow & Lifecycle Safety — Ensure `collect` / `collectLatest` calls in presentation handle empty/null emissions correctly. Check that hot flows use correct retention constants.
- CHECK: Database Integrity & Sync — Verify that updates to local models don't cause partial data corruption or loss of critical fields (like `email` or `createdAt`) if local cache lookups fail.
- CHECK: Resource & Memory Management — Check that temporary files, camera files, bitmap streams, and memory objects are safely recycled/deleted in `try-finally` blocks.
- CHECK: Environmental/Build Variances — Verify file provider authorities, path mappings, package names, and app ID suffixes work correctly across different build variants (debug vs release).
- CHECK: UI/Compose Best Practices — Ensure Compose-friendly resource resolution is used (`stringResource(...)` or standard `UiText.asString()` Composable extensions) rather than piping `LocalContext` down where avoidable. Ensure padding rules use proper constants.

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
