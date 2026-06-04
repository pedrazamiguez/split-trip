# Code Quality & Static Analysis

This project uses six complementary code quality tools, each addressing a distinct concern.

---

## Tool Overview

| Tool | Purpose | Scope | GitHub Integration |
|---|---|---|---|
| **CodeQL** | Security vulnerability detection | Data-flow analysis: injection, XSS, credentials | Security tab, inline PR annotations |
| **Detekt** | Kotlin code quality & complexity | Code smells, cognitive complexity, naming, empty blocks | Security tab, inline PR annotations (via SARIF) |
| **Android Lint** | Android framework issues | Hardcoded text, missing translations, API level, accessibility, manifest | Security tab, inline PR annotations (via SARIF, `/tool:android-lint` category) |
| **Ktlint** | Kotlin formatting & style | Whitespace, imports, indentation, trailing commas | CI check (pass/fail) |
| **CPD** | Code duplication detection | Duplicate code blocks (≥100 tokens) across all modules | Security tab as `Note`-level findings (via SARIF, `/tool:cpd` category), inline PR annotations |
| **JaCoCo** | Code coverage measurement | Unit test line/branch coverage per module + merged report | SonarQube dashboard (post-merge on `develop`) |
| **SonarQube** | Centralized quality dashboard | Coverage trends, bugs, code smells, vulnerabilities, technical debt | Self-hosted CE 9.9 — post-merge analysis on `develop` only (CE limitation: no PR analysis) |
| **Konsist** | Architecture rule enforcement | Naming conventions, dependency rules, structural patterns | Check Run with per-rule pass/fail annotations in PR Checks tab |

**CodeQL and Detekt are complementary, not competing.** CodeQL focuses on security patterns; Detekt focuses on code quality. Both upload SARIF with different categories (`/language:java-kotlin` vs `/tool:detekt`), so findings appear separately in the Security tab.

---

## Configuration Files

| File | Purpose |
|---|---|
| `config/detekt/detekt.yml` | Detekt rule configuration (thresholds, enabled/disabled rules) |
| `.editorconfig` | Ktlint formatting rules (line length, disabled rules for Compose conventions) |
| `build.gradle.kts` (root) | CPD, JaCoCo, Detekt, Ktlint — configured for all subprojects |
| `konsist-tests/` | Konsist architecture tests (`:konsist-tests` module) |
| `.github/workflows/static-analysis.yml` | CI workflow: runs ktlint + detekt + CPD in parallel |
| `.github/workflows/coverage-and-architecture.yml` | CI workflow: runs JaCoCo coverage + Konsist architecture tests |
| `.github/workflows/build-and-test.yml` | CI workflow: lint, unit tests, build, APK upload |
| `.github/workflows/codeql.yml` | CI workflow: runs CodeQL security analysis |
| `scripts/pre-commit` | Git pre-commit hook (ktlint only) |

---

## CI Workflows

All workflows are independent and run in parallel on every push to `main`/`develop` and every PR targeting those branches.

### Static Analysis (`static-analysis.yml`)

Four parallel jobs:

1. **Ktlint (Formatting):** Runs `./gradlew ktlintCheck`. Fails the check if any formatting violations are found.
2. **Detekt (Code Quality):** Runs `./gradlew detekt --continue`. Uploads a merged SARIF report to GitHub Code Scanning. The Gradle task itself does not fail (`ignoreFailures = true`); gating is handled by GitHub's **"Code scanning results"** check.
3. **CPD (Duplication Detection):** Runs `./gradlew cpdCheck --continue`. Converts the CPD XML report into SARIF and uploads it to **GitHub Code Scanning** with category `/tool:cpd`. Each duplication block becomes a `Note`-level finding pointing to the primary file location, with the secondary location(s) attached as `relatedLocations` (visible in the finding detail). Findings appear in the Security → Code Scanning tab alongside Detekt and CodeQL. Uses `ignoreFailures = true` — duplications never block PRs by default (Note severity is below the standard gating threshold).
4. **Android Lint:** Runs `./gradlew lintDebug` with `continue-on-error: true` (because `abortOnError = true` in `build.gradle.kts` causes a non-zero exit when findings are present; reports are still generated before the abort). Merges all per-module `lint-results-debug.sarif` files and uploads to GitHub Code Scanning with category `/tool:android-lint`. Findings appear in the Security tab alongside Detekt/CodeQL. **Note:** `build-and-test.yml` already runs `lintDebug` as a hard blocking gate (`abortOnError = true`); this job is purely for Code Scanning visibility and PR annotations.

### Coverage and Architecture (`coverage-and-architecture.yml`)

Three jobs:

1. **Konsist Architecture Tests:** _(parallel)_ Runs `./gradlew :konsist-tests:test`. Enforces naming conventions, dependency rules, and structural patterns. Failures block the PR. After the test run, `dorny/test-reporter` publishes a **Check Run** named "Konsist Architecture Tests" — visible in the PR's **Checks** tab with per-rule pass/fail and inline annotations pinpointing the exact violation.
2. **JaCoCo Coverage Report:** _(parallel)_ Runs `testDebugUnitTest`, `:domain:test`, then `jacocoMergedReport`. Uploads the merged XML/HTML report as an artifact. Coverage data is consumed by SonarQube in the next job.
3. **SonarQube Analysis:** _(sequential, after JaCoCo)_ Downloads the JaCoCo artifact, runs `./gradlew sonar` with `-Dsonar.qualitygate.wait=true`. Sends analysis to the self-hosted SonarQube CE 9.9 instance. If the Quality Gate fails, the CI job fails — this gates both PRs and pushes to `develop`/`main`. **CE limitation:** CE does not support PR decoration (inline comments on diffs) — that requires Developer Edition+. Concurrent PR analyses may temporarily overwrite the single project dashboard baseline; the next push to `develop`/`main` always restores the authoritative state.

### Build and Test (`build-and-test.yml`)

Unchanged. Runs lint, unit tests, and assembles a debug APK. Publishes test results and uploads artifacts. This workflow is untouched.

### How GitHub Code Scanning Gating Works

- On pushes to `main`/`develop`, GitHub indexes ALL detekt findings as the **baseline** for that branch.
- On a PR, GitHub compares the PR's SARIF against the base branch's SARIF.
- **Only NEW alerts** (introduced by the PR) cause the check to fail.
- **Existing alerts** are visible but don't block the PR.
- This is the same mechanism CodeQL already uses.

---

## CPD (Copy-Paste Detector)

CPD uses PMD's Kotlin tokenizer to detect duplicated code blocks. It is configured at the root `build.gradle.kts` level using the `de.aaschmid.cpd` Gradle plugin.

### Configuration

- **Minimum token count:** 100 (blocks with fewer than 100 identical tokens are ignored)
- **Language:** Kotlin
- **Scope:** All `src/main/kotlin` directories across all subprojects
- **Reports:** XML + text (saved to `build/reports/cpd/`)
- **Failure behavior:** `ignoreFailures = true` (informational only)

### Interpreting Reports

Each duplication entry in the CPD report shows:
- The two (or more) file locations with duplicated code
- The number of duplicated tokens and lines
- The duplicated code fragment

Use this to identify extraction candidates — duplicated logic should be centralized into domain services or shared utilities.

---

## JaCoCo (Code Coverage)

JaCoCo is configured for all subprojects via the root `build.gradle.kts`. It supports both Android modules (library/application) and pure JVM modules (`:domain`).

### How It Works

- **Android modules:** Coverage is collected from `testDebugUnitTest` execution data (`build/outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec`). Classes are read from `build/intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes` (AGP 9.x output location).
- **JVM modules:** Coverage is collected from `test` execution data (`build/jacoco/test.exec`). Classes are read from `build/classes/kotlin/main`.
- **Merged report:** The `jacocoMergedReport` task aggregates all subproject execution data into a single HTML + XML report at `build/reports/jacoco/merged/`.
- **Local coverage gate (`scripts/check_coverage.py`):** Run via `make coverage` / `make check`. Enforces **two** thresholds against the merged report:
  1. **Overall** LINE coverage ≥ 80%.
  2. **Per-file** LINE coverage ≥ 80% for every source file with ≥ 3 instructionable lines.
  The per-file gate mirrors SonarQube's "New Code" coverage rule — a brand-new module with 0% coverage no longer slips through when the overall number stays high. Files that are genuinely untestable (Composables, DI modules, Room DAOs, …) must be added to `JacocoExclusions.classExcludes` in `build-logic/` rather than silenced via thresholds. Override locally with `--threshold`, `--file-threshold`, or `--min-lines` flags.

### Excluded Classes

The following generated classes are excluded from coverage measurements:
- `R.class`, `R$*.class`, `BuildConfig.*`, `Manifest*.*` (Android generated)
- `*_Factory.*`, `*_MembersInjector.*` (DI generated)
- `*Module.*`, `*Module$*.*` (Koin DI modules)
- `*ComposableSingletons*.*` (Compose generated)
- `*_Impl.*`, `*Dao_Impl.*` (Room generated)
- `*PreviewHelper*.*` (debug preview helpers)
- `*$Companion.*` (companion objects)

### Per-Module Reports

Each subproject generates its own `jacocoTestReport` after unit tests run. Reports are saved to `<module>/build/reports/jacoco/`.

---

## Konsist (Architecture Rule Enforcement)

[Konsist](https://docs.konsist.lemonappdev.com/) is a Kotlin architecture testing library that enforces structural rules at test time. Rules are written as JUnit 5 tests in the `:konsist-tests` module.

### Architecture Rules Enforced

#### Naming Conventions
- Domain services must end with `Service`
- Use cases must end with `UseCase`
- Repository interfaces must end with `Repository`
- ViewModels must end with `ViewModel`
- Event handlers must end with `EventHandler`
- Data sources must end with `DataSource`

#### ViewModel Dependency Rules
- ViewModels must NOT import Repository classes
- ViewModels must NOT import data layer packages
- ViewModels must NOT import `android.content.Context`
- ViewModels must NOT import `LocaleProvider`

#### Handler Isolation
- Event handlers must NOT depend on other event handlers (via constructor)

#### Feature Module Isolation
- Feature modules must NOT import from the data layer
- Feature modules must NOT import from other feature modules

#### Domain Layer Purity
- Domain module must NOT import Android framework classes (`android.*`, `androidx.*`)
- Domain module must NOT import data layer
- Domain services must NOT contain formatting/display methods

#### Screen Statelessness
- Screen composables must NOT import ViewModel classes

#### File Size Limits
- Production source files must NOT exceed 600 lines (test files are exempt)

### Adding New Rules

To add a new architecture rule:
1. Open `konsist-tests/src/test/kotlin/.../konsist/ArchitectureTest.kt`
2. Add a new `@Test` method in the appropriate `@Nested` class
3. Use Konsist's fluent API to scope → filter → assert
4. Run `./gradlew :konsist-tests:test` to verify

## Reading Findings on GitHub

### Security Tab (`Security → Code Scanning`)

Go to **Security → Code Scanning** to see all findings:
- Filter by **Tool** to see findings from each tool separately: `CodeQL`, `detekt`, `CPD`, `Android Lint`.
- Filter by **Branch** to compare across branches.
- Each finding shows: file, line, rule name, severity, and description.
- You can **dismiss** false positives with a reason.

**CPD findings** appear as `Note`-level entries under tool `CPD`. Existing duplications form the baseline and won't re-flag on PRs unless new ones are introduced.

**Android Lint findings** appear under tool `Android Lint`. The same baseline mechanism applies — only new findings introduced by a PR will create PR annotations.

### PR Inline Annotations (Security tab / Checks Tab)

- **Detekt** findings appear as inline annotations on the PR diff. New findings are highlighted; existing ones are marked as pre-existing.
- **Android Lint** findings appear as inline annotations on the PR diff for newly introduced issues.
- **CPD** findings appear as inline `Note` annotations if the duplicated block touches a changed line.

### Konsist Check Run (PR Checks Tab)

After every CI run, a **"Konsist Architecture Tests"** Check Run appears in the PR's **Checks** tab. Each failed architecture rule is listed as a named test with its error message. If all rules pass, the check is green.

### SonarQube Dashboard

The `sonar` CI job sends analysis to the self-hosted SonarQube CE 9.9 instance at `https://cantalobos.mooo.com` on every push to `develop`/`main` and on every pull request. The dashboard provides:
- **Overall coverage** with historical trends
- **Code smells, bugs, and vulnerabilities** detected by the SonarScanner
- **Technical debt** estimation
- **Quality Gate status** — if the gate fails, the `sonar` CI job fails (via `-Dsonar.qualitygate.wait=true`), blocking the PR merge or signaling a broken push

**CE 9.9 limitation:** SonarQube Community Edition does not support PR decoration (inline comments on PR diffs) — that requires Developer Edition+. The scanner CAN still run on PR code and the Quality Gate pass/fail result gates the merge via CI status checks. Trade-off: CE has a single project baseline, so concurrent PR analyses may temporarily overwrite each other's dashboard state. The next push to `develop`/`main` always restores the authoritative baseline.

### SonarQube Exclusion System

SonarQube has **four independent exclusion layers**. Confusing them is a common pitfall — each serves a different purpose and has no effect on the others.

| Layer | Gradle Property | Scope | Effect |
|---|---|---|---|
| **JaCoCo class exclusions** | `jacocoExcludes` (in `classDirectories.setFrom(...)`) | JaCoCo Gradle plugin only | Controls which `.class` files count toward JaCoCo coverage %. Has **zero effect** on SonarQube. |
| **Coverage exclusions** | `sonar.coverage.exclusions` | Sonar coverage analyzer | Files Sonar skips for coverage metrics (shows "—" instead of 0%). Does **not** suppress code smells, bugs, or duplications. |
| **Issue exclusions** | `sonar.issue.ignore.multicriteria` | Sonar issue engine | Suppresses specific rules on specific file paths. The primary tool for suppressing false positives and framework-specific patterns. |
| **Full exclusions** | `sonar.exclusions` | ALL Sonar analyzers | Files invisible to every analyzer (issues, coverage, duplications). Use sparingly — hides everything. |

#### `resourceKey` Pattern Rules (Critical)

The `sonar.issue.ignore.multicriteria.<id>.resourceKey` property accepts a **single** Ant-style path pattern per entry.

```kotlin
// ❌ BROKEN — Sonar treats the entire comma-separated string as ONE pattern
property("sonar.issue.ignore.multicriteria.e1.resourceKey",
    "**/component/**/*.kt,**/designsystem/**/*.kt")

// ✅ CORRECT — one pattern per entry, use separate IDs for multiple paths
property("sonar.issue.ignore.multicriteria.e1.resourceKey", "**/component/**/*.kt")
property("sonar.issue.ignore.multicriteria.e2.resourceKey", "**/designsystem/**/*.kt")
```

> **Note:** `sonar.exclusions` and `sonar.coverage.exclusions` DO support comma-separated patterns (or Gradle `listOf(...).joinToString(",")`). Only `resourceKey` in multicriteria requires one-pattern-per-entry.

#### Current Multicriteria Entries

All entries are configured in `build.gradle.kts` inside the `sonarqube { properties { } }` block.

| ID | Rule | Pattern | Rationale |
|---|---|---|---|
| `e1` | `kotlin:S107` (params) | `**/presentation/component/**/*.kt` | Compose components: many optional params with defaults (Detekt-suppressed) |
| `e2` | `kotlin:S107` (params) | `**/designsystem/presentation/**/*.kt` | Design-system Compose components |
| `e3` | `kotlin:S107` (params) | `**/presentation/viewmodel/**/*.kt` | MVI ViewModels: injected UseCases, Handlers, Mappers |
| `e4` | `kotlin:S3776` (complexity) | `**/presentation/component/**/*.kt` | Compose DSL — structurally complex, not logically |
| `e5` | `kotlin:S3776` (complexity) | `**/designsystem/presentation/**/*.kt` | Same as e4 |
| `e6` | `kotlin:S3776` (complexity) | `**/navigation/**/*.kt` | NavHost auth/onboarding branching (Detekt-suppressed) |
| `e7` | `kotlin:S1479` (when clauses) | `**/presentation/viewmodel/**/*.kt` | MVI `when` routing — sealed interface exhaustiveness by design |
| `e8` | `kotlin:S1481` (unused var) | `**/*.kt` | Sonar false positives on destructuring, `remember{}`, lambdas (see #786) |
| `e9` | `kotlin:S1135` (TODO/FIXME) | `**/navigation/**/*.kt` | Tracked TODOs with issue references (e.g., #787) |
| `e10` | `kotlin:S1479` (when clauses) | `**/designsystem/extension/**/*.kt` | CurrencyExtensions mapping Currency enum to Android string resource IDs |
| `e11` | `kotlin:S107` (params) | `**/presentation/screen/**/*.kt` | Feature-layer Compose Screens: UI layouts with optional params, callbacks, state |
| `e12` | `kotlin:S107` (params) | `**/presentation/feature/**/*.kt` | Feature-layer Compose Features: VM controllers + navigation handlers |
| `e13` | `kotlin:S3776` (complexity) | `**/presentation/screen/**/*.kt` | Screen Compose layouts — nested DSL builder layouts (Rows/Columns/Scaffolds) |
| `e14` | `kotlin:S3776` (complexity) | `**/presentation/feature/**/*.kt` | Feature orchestrators — Compose branching logic for state (loading, error, content) |

#### Adding New Entries

When adding a new multicriteria entry:

1. Append the new ID to the comma list: `property("sonar.issue.ignore.multicriteria", "e1,e2,...,eN")`
2. Add two lines: `.ruleKey` and `.resourceKey` — one pattern per `resourceKey`.
3. Document the rationale in the table above.
4. Re-run Sonar and verify the issue disappears.

### Artifacts

HTML/XML/SARIF reports are uploaded as build artifacts for deep-dive offline review when needed:
- **detekt-reports:** HTML + SARIF reports from Detekt
- **cpd-reports:** XML + SARIF reports from CPD
- **android-lint-reports:** HTML + SARIF reports from Android Lint
- **jacoco-coverage-reports:** Merged HTML + XML coverage reports from JaCoCo
- **konsist-test-results:** HTML test result report from Konsist
- **unit-test-reports:** HTML test result reports

---

## Local Development Commands

| Command | Purpose | Speed |
|---|---|---|
| `./gradlew ktlintCheck` | Check formatting (all modules) | ~15s |
| `./gradlew ktlintFormat` | Auto-fix formatting (all modules) | ~15s |
| `./gradlew detekt` | Run full static analysis | ~30-45s |
| `./gradlew cpdCheck` | Run copy-paste detection | ~15s |
| `./gradlew jacocoMergedReport` | Generate merged coverage report | ~2-3min (runs all tests) |
| `./gradlew :domain:jacocoTestReport` | Coverage report for a single module | ~30s |
| `./gradlew :konsist-tests:test` | Run architecture rule tests | ~30s |
| `./gradlew :app:ktlintCheck` | Check formatting (single module) | ~5s |
| `./gradlew :app:detekt` | Run analysis (single module) | ~10s |

---

## Pre-Commit Hook

A Git pre-commit hook runs **ktlint only** (not detekt, CPD, or Konsist) on every commit that includes Kotlin files. It is:

- **Fast:** ~5-10 seconds with the Gradle daemon.
- **Verbose:** Full error output with file paths, line numbers, and rule names. Never piped to `/dev/null`.
- **Helpful:** On failure, it shows how to auto-fix (`./gradlew ktlintFormat`) and how to bypass (`git commit --no-verify`).
- **Smart:** Skips entirely if no `.kt` or `.kts` files are staged.

### Installation

The hook is auto-installed when Gradle syncs the project (via the `installGitHooks` task). You can also install it manually:

```bash
./gradlew installGitHooks
```

### Escape Hatch

If you need to commit without running the hook (e.g., WIP commit, documentation-only changes):

```bash
git commit --no-verify -m "your message"
```

---

## IDE Integration

### IntelliJ / Android Studio — Detekt Plugin

Install the [Detekt IntelliJ Plugin](https://plugins.jetbrains.com/plugin/10761-detekt) for real-time in-editor feedback:

1. **Settings → Plugins → Marketplace** → Search "Detekt" → Install
2. **Settings → Tools → Detekt** → Enable, point to `config/detekt/detekt.yml`
3. Findings appear as warnings/errors directly in the editor

### EditorConfig

Android Studio/IntelliJ natively respects `.editorconfig` for formatting (indent, charset, line length). No plugin needed — it works out of the box.

---

## Triaging Existing Findings

After the initial SARIF upload, the Security tab will show ALL existing detekt findings. To manage them:

1. **Review** findings by severity (Error / Warning / Note).
2. **Dismiss** false positives with a documented reason ("Won't fix", "Used in tests", etc.).
3. **Create follow-up issues** for legitimate findings, grouped by category (e.g., "Reduce complexity in `:features:expenses`").
4. Track progress — as you fix findings, they automatically disappear from the Security tab on the next push.
