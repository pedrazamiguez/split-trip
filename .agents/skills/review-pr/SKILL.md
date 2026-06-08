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

For each modified or new file in the diff, perform the following validation checks:

1. **Precision-Sensitive Math (`BigDecimal` only)**:
   - Search the diff/files for any usage of `Double` or `Float` representing currency amounts, shares, exchange rates, or percent splits.
   - All such values must strictly use `BigDecimal` with an explicit `RoundingMode` and `scale`.
   - Ensure Firestore serialization at the document layer uses `String` (`toPlainString()` / `toBigDecimalOrNull()`) to avoid precision loss.

2. **File-Size Hard Limit (600 lines)**:
   - For every modified or new production source file (e.g. `.kt` files in `main/`), check its line count:
     ```bash
     wc -l <path/to/file.kt>
     ```
   - No production file must exceed **600 lines** (enforced by Konsist). Note that test files are exempt.

3. **ViewModel Restrictions**:
   - Check if any ViewModels depend directly on repositories. ViewModels must only depend on UseCases, Mappers, and Domain Services.
   - ViewModels must not inject Android `Context`, `LocaleProvider`, or other ViewModels.
   - If a ViewModel's `onEvent()` handles >5 event categories or the ViewModel exceeds ~200 lines, verify if logic is extracted into plain Event Handler classes.

4. **Horizon Narrative Design System**:
   - Ensure UI components conform to the "Horizon Narrative" language:
     - No raw 1px solid borders. Use tonal shifts or container level changes.
     - Plus Jakarta Sans + Manrope typography.
     - Usage of `LocalBottomPadding.current` applied as bottom content padding for all scrollable lists, FABs, or bottom-anchored buttons on tab screens.

5. **Completeness of Tests**:
   - Verify if new functionality/components have corresponding unit or instrumentation tests.
   - Ensure Kotlin's `assert()` is NEVER used (use JUnit assertions instead).
   - Ensure repositories that launch coroutines inject a `CoroutineDispatcher` (default `Dispatchers.IO`) for testability and do not hardcode it.

6. **Mappers & Services**:
   - Check that formatting is handled in UI Mappers (`*UiMapper` or `*UiMapperImpl`) using `LocaleProvider` and NOT inside ViewModels or Domain Services.
   - Check that domain services do not contain presentation or formatting logic.

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
