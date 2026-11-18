---
name: sp-investigate-production-incident
description: Investigate a production or closed-testing incident using Firebase Crashlytics MCP, correlate telemetry and stack traces with the codebase, and draft an approved hotfix GitHub issue.
mode: agent
tools:
  - codebase
  - firebase
  - githubRepo
  - terminalLastCommand
arguments:
  - name: incident_description
    description: User bug report, observed symptom, and affected version/device if known.
    required: true
  - name: additional_context
    description: Optional tester notes, logs, screenshots, device IDs, or reproduction clues.
    required: false
---

# Investigate Production Incident

Investigate a production or closed-testing incident using Firebase Crashlytics telemetry and codebase correlation:
- Incident Description: $INCIDENT_DESCRIPTION
- Additional Context: $ADDITIONAL_CONTEXT

> [!IMPORTANT]
> **CRITICAL GUARDRAILS:**
> - **Read-Only Triage:** Strictly prohibit editing, creating, or deleting any repository source files, test files, build files, or machine-local configs (`~/.gemini/config/mcp_config.json`) during investigation.
> - **No Speculative Root Causes:** Only claim root causes backed by verified Crashlytics payloads, git history, or codebase inspection. Explicitly document remaining unknowns and hypotheses when evidence is inconclusive.
> - **Mandatory Approval Gate:** Do NOT create the GitHub issue via `issue_write` until the user explicitly reviews and approves the verified evidence summary and drafted issue.

---

## Step 1 — Verify Firebase MCP Environment & Read Crashlytics Guides

1. **Inspect Environment:** Call `firebase_get_environment` to check `project_dir`, the authenticated user, and `active_project`.
2. **Ensure Workspace & Project Match:** Confirm `project_dir` points to the SplitTrip repository root and `active_project` is `expshapp` (per `.firebaserc` and `app/google-services.json`).
   - If `project_dir` or `active_project` is unset or mismatched, call `firebase_update_environment` with `project_dir` (workspace root) and `active_project: "expshapp"`.
   - Never modify `~/.gemini/config/mcp_config.json`.
3. **Resolve Android App ID:** Call `firebase_list_apps` (`platform: "android"`) to resolve the Android `appId` (expected `1:1083910180813:android:e09e232599e989250ffe84` for `es.pedrazamiguez.splittrip` per `app/google-services.json`).
4. **Read Crashlytics Guides:** Before issuing any report or event queries, call `firebase_read_resources` for all three bundled Crashlytics guides:
   - `firebase://guides/crashlytics/reports`
   - `firebase://guides/crashlytics/issues`
   - `firebase://guides/crashlytics/investigations`

---

## Step 2 — Query Crashlytics Reports, Issues & Filtered Events

1. **Resolve Release Versions:** Call `crashlytics_get_report` with `report: "topVersions"` (and optionally `"releaseVersions"` or `"appSummary"`) to obtain exact `versionDisplayNames` (`'version (build)'`) for the reported or latest closed-testing/production release.
2. **Query Top Issues Across All Error Types:** Call `crashlytics_get_report` with `report: "topIssues"` across **all three** mobile `issueErrorTypes`:
   - `FATAL`
   - `ANR`
   - `NON_FATAL`
   Pass `intervalStartTime` and `intervalEndTime` (ISO 8601 timestamps within the 90-day limit) and `versionDisplayNames` when known.
   - **Why `NON_FATAL` is mandatory:** `ProductionCrashlyticsTree` (`core/logging/src/main/kotlin/es/pedrazamiguez/splittrip/core/logging/tree/ProductionCrashlyticsTree.kt`) records `Timber.e(...)` (wrapping a null `Throwable` in `SyntheticDiagnosticException`) and `Timber.w(throwable, ...)` as non-fatal Crashlytics exceptions. Startup or sync failures frequently surface only as `NON_FATAL` issues.
3. **Inspect Candidate Issues:** Call `crashlytics_get_issue` (`appId`, `issueId`) for relevant issues to gather issue metadata, signals (`SIGNAL_EARLY`, `SIGNAL_FRESH`, `SIGNAL_REGRESSED`, `SIGNAL_REPETITIVE`), and affected user/event counts.
4. **Fetch Sample & Recent Events:**
   - Call `crashlytics_batch_get_events` (`appId`, `names` from `sampleEvent` fields in issue/report responses) to inspect concrete crash/exception payloads.
   - Call `crashlytics_list_events` (`appId`, `filter`, `pageSize`) to retrieve additional events for the implicated issue, version, or timeframe.
5. **Filter by SplitTrip Custom Keys (When Targeting Specific Reports):**
   When investigating a specific user, tester, device, or app-state report, query `crashlytics_list_events` using AIP-160 `filterExpression` predicates against SplitTrip's instrumented custom keys:
   - `custom_keys.deviceId` — set in `ProductionCrashlyticsTree` from `LogContext.deviceId` / `UserPreferences.deviceId`
   - `custom_keys.sessionId` — set in `ProductionCrashlyticsTree` from `LogContext.sessionId`
   - `custom_keys.appVersion` — set in `ProductionCrashlyticsTree` from `LogContext.appVersion` / `BuildConfig.VERSION_NAME`
   - `custom_keys.selected_group_id` — set in `AppNavHost` via `TelemetryTracker.setCustomKey("selected_group_id", ...)` (`"none"` when unset)
   - `custom_keys.current_screen` — set in `FirebaseTelemetryTracker.trackScreenView`
   - `custom_keys.network_online` — set in `AppNavHost` via `TelemetryTracker.setCustomKey("network_online", isOnline)`
   Also inspect `userId` on event payloads (hashed via `hashIdentifier()` or `"anonymous"` in `LogContextImpl` / `FirebaseTelemetryTracker`).

---

## Step 3 — Inspect Event Evidence & Correlate with Revision / Codebase

1. **Extract Event Payload Details:** For each relevant event, inspect:
   - Stack traces and ANR thread states / lock contention.
   - `breadcrumbs` and Crashlytics `logs`:
     - `ProductionCrashlyticsTree` logs `I/`, `W/`, `E/`, and `A/` messages formatted as `"${priority}/${tag}: ${sanitizedMessage}"` and drops `VERBOSE` / `DEBUG`.
     - `AppNavHost` emits `SplitTrip:Navigation` (`LogTag.NAVIGATION`) logs such as `"Startup gate: ..."`, `"NavHost gate: ..."`, and `"Navigated to: ..."`.
     - `FirebaseTelemetryTracker.trackScreenView` logs `"Screen: <screenName>"`.
   - `buildStamp` (git commit/revision metadata embedded in the build).
2. **Correlate `buildStamp` Against Git History (Read-Only):**
   Use read-only git commands (`git log`, `git show <rev>:<path>`) to inspect the exact revision from `buildStamp` and compare it against current `main` and `develop`. Do NOT check out branches, reset, or modify the working directory.
3. **Inspect Implicated Code Paths:**
   Trace the implicated startup, navigation, or data paths in the codebase, including:
   - `app/src/main/kotlin/es/pedrazamiguez/splittrip/App.kt`
   - `app/src/main/kotlin/es/pedrazamiguez/splittrip/di/AppModule.kt`
   - `app/src/main/kotlin/es/pedrazamiguez/splittrip/MainActivity.kt`
   - `app/src/main/kotlin/es/pedrazamiguez/splittrip/navigation/AppNavHost.kt`
   - `StartupReconciliationEffect`, `DeferredLoadingContainer`, `AppLockGate`, `SessionExpirationGuard`, `DeepLinkHolder`, and implicated repositories/sync delegates.
4. **Non-Throwing Hang / Stuck-Loading Analysis:**
   When user symptoms describe startup hangs, freezes, or infinite loading and **no `FATAL` or `ANR`** is recorded in Crashlytics, explicitly investigate:
   - Startup gate state conditions in `AppNavHost.kt`: `stableStartDestination.value == null`, `!isBiometricPreferenceLoaded`, `isUserLoggedIn == null`, `onboardingCompleted == null`, `isReconciled == null`, and `StartupReconciliationEffect`.
   - Potentially blocking startup calls or indefinitely suspended flows (e.g., `runBlocking { deviceIdDeferred.await() }` in `AppModule.kt`, DataStore `.first()` waits, Firestore or reconciliation timeouts).
   - Preceding `NON_FATAL` exceptions (`Timber.e` / `Timber.w`) or missing `SplitTrip:Navigation` breadcrumbs (`"Startup gate: ..."`, `"NavHost gate: ..."`, `"Navigated to: ..."`) that pinpoint exactly where startup stalled.

---

## Step 4 — Check GitHub History & Targeted Docs

1. **Search Existing Issues & PRs:** Query `pedrazamiguez/split-trip` using GitHub MCP tools (`search_issues`, `search_pull_requests`, `list_pull_requests`) to identify duplicate incident reports, recent release PRs, or regressions touching the implicated files.
2. **Read Targeted Docs (Only When Needed):** Consult `/docs/` articles ONLY if architectural clarification is needed for the implicated subsystem:
   - Sync / offline patterns → `docs/architecture/patterns/offline-first.md`
   - Architecture decisions → `docs/architecture/adrs/`
   - Domain rules → `docs/domain/`

---

## Step 5 — Present Evidence, Draft `hotfix` Issue & Request Approval

1. **Present the Verified Evidence Summary:**
   - Firebase project/app queried, time interval, and filters / `filterExpression` used.
   - Crashlytics issue ID(s), error type (`FATAL` / `NON_FATAL` / `ANR`), signals, affected app versions & `buildStamp` git revision, and impacted users/events/devices.
   - Key custom keys (`appVersion`, `current_screen`, `selected_group_id`, `network_online`, `deviceId`, `sessionId`), relevant stack trace frames, and chronological breadcrumb/log timeline.
   - Confirmed root cause (exact file paths, symbols, and line ranges) vs. remaining unknowns or open hypotheses.
   - Related or duplicate GitHub issues and PRs found.
2. **Present the GitHub Issue Draft (`sp-create-issue` Format):**
   Follow `.agents/skills/create-issue/SKILL.md` strictly:
   - **Title:** Short, imperative sentence.
   - **Label:** Strictly `hotfix` only (no extra or arbitrary labels).
   - **Body sections:**
     - `### Problem / Motivation` — observed user symptom + verified Crashlytics evidence and affected versions/revisions.
     - `### Proposed Solution` — implicated modules, files, and applicable Clean Architecture / offline-first / MVI constraints (without leaking PR-level diffs into the issue).
     - `### Acceptance Criteria` — concrete, testable fix behavior plus:
       - [ ] All existing tests pass; regression test added covering the incident root cause.
       - [ ] `make fast-check` passes during iterative development (~15–30s).
       - [ ] `make check > build.log 2>&1 && echo "Check passed successfully" || (tail -n 100 build.log && exit 1)` passes with 0 failures before merging.
       - [ ] No new detekt findings introduced.
     - `### Out of Scope` — explicit boundaries to prevent scope creep.
     - `### Related Issues / PRs` — linked issues, release PRs, or prior regressions.
3. **STOP and Request Explicit User Approval:**
   - Ask the user to review the evidence summary and issue draft.
   - Do NOT edit repository code or create the GitHub issue until the user explicitly approves.
4. **Create Issue Upon Approval:**
   - Once explicitly approved, call `issue_write` on `pedrazamiguez/split-trip` with ONLY the `hotfix` label applied, and share the resulting issue URL with the user.
