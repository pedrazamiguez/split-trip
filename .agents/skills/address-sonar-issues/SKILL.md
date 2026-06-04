---
name: sp-address-sonar-issues
description: Fetch, analyze, and resolve SonarQube quality gate failures, code smells, bugs, and coverage gaps.
mode: agent
tools:
  - codebase
  - sonarqube
  - terminalLastCommand
arguments:
  - name: project_key
    description: The SonarQube project key to inspect (defaults to 'split-trip').
    required: false
  - name: component_path
    description: Optional relative file or directory path to scope the query.
    required: false
  - name: issue_key
    description: Optional specific SonarQube issue key to retrieve and resolve.
    required: false
---
# Address Sonar Issues

Identify, analyze, and resolve SonarQube code quality issues (bugs, code smells, vulnerabilities) or coverage gaps:
- Project Key: $PROJECT_KEY
- Component Path: $COMPONENT_PATH
- Issue Key: $ISSUE_KEY

---

## Step 1 — Query SonarQube via MCP

Query the target SonarQube instance to check project health and identify current issues or coverage gaps:

1. **Verify Project Key:** Default to `split-trip` if `$PROJECT_KEY` is not provided.
2. **Check Quality Gate Status:** Run the `sonarqube` MCP server tool `get_project_quality_gate_status` using `$PROJECT_KEY` to evaluate the overall health and determine which specific quality gate conditions are failing.
3. **Find Code Quality Issues:** Use the `sonarqube` MCP server tool `search_sonar_issues_in_projects` to locate outstanding bugs, vulnerabilities, or code smells. Filter by project key, and optionally `issueStatuses=["OPEN"]` and/or component paths.
4. **Find Coverage Gaps:** Use the `sonarqube` MCP server tool `search_files_by_coverage` to find files with low test coverage. Sort ascending to find the worst-covered files first.

---

## Step 2 — Retrieve Detailed Context

Before starting implementation, retrieve precise failure details:

1. **Retrieve Rule Details:** For code quality issues, run the `sonarqube` MCP server tool `show_rule` using the rule key of the identified issue to understand the pattern violated and how it should be resolved.
2. **Locate Code Smells:** Identify the exact file path and line numbers from the issue details.
3. **Retrieve Coverage Details:** For coverage gaps, use `get_file_coverage_details` with the file key to retrieve line-by-line coverage metrics, showing precisely which lines are uncovered or partially covered.

---

## Step 3 — File-Size Guard (600-line hard limit)

Before and after making edits to any code files, run `wc -l` to check their line count:

```bash
wc -l <path/to/file.kt>
```

- **Before editing:** If a file is at or near the 600-line hard limit, plan to refactor, extract event handlers, or create delegate classes **before** adding new logic.
- **After editing:** Re-check the file's line count. If your edits pushed the file over the 600-line hard limit, immediately refactor it (e.g., by extracting event handlers, delegates, or smaller components) to bring it back under the 600-line limit.

---

## Step 4 — Plan and Implement Fixes

Implement code modifications and unit tests, adhering strictly to all project architecture constraints:

1. **Architecture Constraints (from [AGENTS.md](../../../AGENTS.md) and [.github/copilot-instructions.md](../../../.github/copilot-instructions.md)):**
   - ViewModels MUST NOT inject Repositories (only inject UseCases, Mappers, and Domain Services).
   - ViewModels MUST NOT inject `Context`, `LocaleProvider`, or other ViewModels.
   - All precision-sensitive decimal calculations MUST use `BigDecimal` with explicit scale and rounding mode (never `Double` or `Float`).
   - Mappers handle all formatting and locale-aware translations. Formatters and formatting logic belong in presentation mappers, never in ViewModels or Domain Services.
   - Tab screens must apply `LocalBottomPadding` to avoid layout obstruction by the bottom nav bar.
2. **True Offline-First Protocol:**
   - Write to Room local database first (immediate UI update).
   - Generate UUIDs and timestamps (`createdAt = System.currentTimeMillis()`) locally in the Repository or UseCase.
   - Synchronize with the cloud database (Firestore) in the background using reusable data layer sync delegates.
3. **Unit Tests (Coverage Gaps):**
   - Locate or create the corresponding test file in the matching module's `test/` directory.
   - Implement unit tests using JUnit 5 and MockK targeting the uncovered lines.
   - **Crucial assertion rule:** NEVER use Kotlin's `assert()`. ALWAYS use JUnit assertions (e.g., `Assert.assertTrue(...)`, `Assert.assertEquals(...)`).
   - **Coroutine testing:** Inject `CoroutineDispatcher` (using `StandardTestDispatcher()`) into classes that launch background coroutines to ensure deterministic test execution.

---

## Step 5 — Local Verification Gate

Ensure all local verification checks pass successfully:

1. **Run Check Suite:**
   ```bash
   make check
   ```
   This verifies compilation, runs Konsist architecture tests, detekt, ktlint formatting checks, and all unit tests.
2. **Verify Coverage:** Run the coverage suite to verify that overall and per-file coverage stays above the 80% threshold:
   ```bash
   make coverage
   ```

---

## Step 6 — Update SonarQube Status

Once the fixes are verified and the pull request is created:
1. Use the `change_sonar_issue_status` tool to update the status of the resolved issues in SonarQube to `accept` or `falsepositive` if applicable.
