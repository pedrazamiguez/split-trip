---
name: sp-start-issue
description: Implement a GitHub issue by strictly following the implementation plan posted on the issue.
mode: agent
tools:
  - codebase
  - githubRepo
  - terminalLastCommand
arguments:
  - name: issue_number
    description: The number of the GitHub issue to start working on.
    required: true
---# Start Issue

Start working on this issue:
- Issue Number: $ISSUE_NUMBER
---

## Step 0 — Branch validation

Before writing any code or performing checks, verify your local Git state:
1. Ensure you are on the correct branch for this task.
2. If starting work on a new issue, ensure a branch has been created from `develop` following the branch naming convention.
3. If this is a hotfix, ensure the branch has been created from `main`.
4. **DO NOT START MODIFYING ANY CODE UNTIL YOU'VE MADE SURE WE HAVE THE LATEST CHANGES FROM THE BASE BRANCH** to avoid conflicts in the future. If you are working on a hotfix branch, you must run `git pull origin main`. For all other branches, you must run `git pull origin develop` to sync the branch first.
5. **CRITICAL:** If getting the latest changes results in merge conflicts, **HALT IMMEDIATELY**. Do not attempt to resolve the merge conflicts manually using file editing tools. Inform the user that they must resolve the conflicts in their IDE before you can proceed.
---

## Step 1 — Load issue context

1. Fetch issue `$ISSUE_NUMBER` (`get` + `get_comments`), all linked issues, and every comment thread.
2. Read targeted `/docs/` articles ONLY if the implementation plan or issue domain requires it:
   > - Decimal/currency math → `docs/domain/multi-currency-and-snapshots.md`
   > - Sync / offline patterns → `docs/architecture/patterns/offline-first.md`
   > - UI components → `docs/architecture/patterns/core-services-catalog.md` §A, `docs/design-system/horizon-narrative-design.md`
   > - Domain services → `docs/architecture/patterns/core-services-catalog.md` §B–§G (relevant entry only)
   > - Architecture decisions → `docs/architecture/adrs/`
---

## Step 2 — Find the Posted Implementation Plan

Retrieve all comments on the GitHub issue using `issue_read` (`github-mcp-server`) or `gh issue view $ISSUE_NUMBER --repo pedrazamiguez/split-trip --comments`.
1. Locate the implementation plan posted as a comment on the issue.
2. **If no implementation plan is found on the GitHub issue, halt immediately.** Do not investigate, deeply analyze, or suggest any technical solution. Stop and inform the user that they must first run the `sp-plan-issue` skill to generate and approve a plan.
3. If an implementation plan is found, read it thoroughly. This plan is your single source of truth for the implementation.
4. If you discover that the plan does not align well with the current code, **do not attempt to fix or replan it yourself**. Stop immediately and suggest that the user run the `sp-replan-issue` skill to update the plan.
5. **Validate that the retrieved plan is a complete technical specification.** It must explicitly specify exact target file paths (using `[NEW]`, `[MODIFY]`, or `[DELETE]` tags), precise class names/signatures/types, database schema updates if any, validation rules, and specific test cases (test class and test case names). **If the plan lacks these essential specification details, halt immediately.** Do not start implementation; inform the user and suggest running `sp-replan-issue` to flesh out the technical specification before proceeding.
---

## Step 3 — File-size guard (600-line hard limit, enforced by Konsist)

Before editing any file, check its current line count:

```bash
wc -l <path/to/file.kt>
```

If the file is already at or near 600 lines, factor that into your implementation (split, extract delegate, etc.) **before** adding new code. Do NOT add code to a file that will push it over 600 lines.

After editing, re-check:

```bash
wc -l <path/to/file.kt>
```

If the result exceeds 600 lines, refactor immediately — do not move on.
---

## Step 4 — Implement the Plan (Phased SDD Pipeline)

Implement the technical solution by strictly following the posted implementation plan through the 5-phase Specification-Driven Development (SDD) lifecycle:

1. **Phase 1 (Specification Ingest):** Review the plan requirements and grounding rules in `docs/domain/`.
2. **Phase 2 (Architecture Contracts):** Verify module boundaries, public interfaces, and UseCase signatures adhere strictly to Clean Architecture (`docs/architecture/adrs/0003-clean-architecture.md`).
3. **Phase 3 (Domain Implementation & TDD):** Implement pure business logic in `:domain` with MockK/JUnit 5 unit tests first.
   - ZERO Android framework dependencies.
   - Strict `BigDecimal` math (`docs/architecture/adrs/0002-bigdecimal-math.md`).
   - Zero string formatting in domain services.
4. **Phase 4 (Infra & UI Wiring):** Implement Room persistence, Firestore sync delegates, MVI ViewModels, and stateless Compose screens (`docs/architecture/patterns/offline-first.md`, `docs/architecture/patterns/mvi-and-stateless-screens.md`).
   - Use `debouncedClickable` on actionable components (`docs/architecture/adrs/0005-debounced-ui-interactions.md`).
   - Formatting in UiMappers only.
5. **Phase 5 (Auditing & Quality Gate):** Verify Konsist architectural rules, 0 FQN violations, and 600-line file limits (`docs/engineering/quality-and-static-analysis.md`).

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

## Step 5 — Local verification gate (run BEFORE declaring done)

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

## Step 6 — Clean up helper scripts

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

## Step 7 — Synchronize AI Knowledge Indexes

Before posting the walkthrough and declaring done, ensure that all AI knowledge indexes (code syntax graphs, documentation RAG, and module topology) reflect the latest codebase and documentation changes:

```bash
make knowledge-update
```
---

## Step 8 — Post walkthrough as an issue comment

After verifying that all checks pass locally and your work is complete, you MUST automatically post the walkthrough you generate as a comment on the GitHub issue using `gh issue comment` via `run_command` (or fall back immediately to `gh` on any MCP error or missing URL) before finishing the task:
```bash
gh issue comment $ISSUE_NUMBER --repo pedrazamiguez/split-trip --body-file - <<'EOF'
<walkthrough...>
EOF
```
Verify that the command exits with code `0` and stdout contains an actual `https://github.com/pedrazamiguez/split-trip/issues/...` comment URL before finishing. Do not wait for the user to ask or perform this step manually; the agent must perform this step programmatically as part of this skill.

The comment must include:
- A summary of the changes made and what was accomplished (referencing the original implementation plan)
- A summary of the testing and validation results (e.g. `make check` results, unit tests executed)
