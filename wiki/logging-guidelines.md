# Logging & Telemetry Guidelines

This document outlines the standard logging architecture, telemetry tracking APIs, and best practices for debugging, troubleshooting, and observing user journeys in SplitTrip.

---

## 📖 Logging Philosophy

SplitTrip is an offline-first application with complex synchronization and multi-currency business logic. To support robust debugging without cluttering developer consoles:
1. **Differentiate Telemetry from Diagnostics**: 
   - **Telemetry (`SplitTrip:Telemetry`)** tracks high-level user actions and screen transitions (what the user did).
   - **Diagnostics (other `SplitTrip` tags)** track internal system behaviors (how the system responded).
2. **Control Noise**: Repetitive/high-frequency logs (like sync loops) must be kept at a low severity (`VERBOSE`) or muted during general development.
3. **No Raw Tags**: Never use arbitrary string tags. Use the predefined tags in `LogTag`.
4. **Sanitize Data**: Never log plaintext PII (email, names, phone numbers) or raw currency figures.

---

## 🛠️ Logging Architecture

Logging in SplitTrip is powered by **Timber**. The root application initializes the logger trees based on the build variant:

- **Debug Builds**: Registers `DevelopmentLogcatTree` which automatically appends:
  - The current **Thread Name** (critical for multi-threaded coroutine tracking).
  - The unique **Session ID** (generated per app launch to isolate sessions).
  - Output format: `[$threadName] [Session:$sessionId] $message`
- **Release/Production Builds**: Registers `ProductionCrashlyticsTree` which routes error and warning logs to Firebase Crashlytics and strips low-level logs.

---

## 📊 Log Levels

Use the appropriate log level based on the type of information:

| Level | Tag / Destination | Usage / Description | Example |
| :--- | :--- | :--- | :--- |
| **VERBOSE (V)** | Logcat (Debug only) | High-frequency events, scratchpad logs, database queries, and polling states. *Never commit verbose logs to Git unless required for sync engine diagnostics.* | `Real-time sync: reconciled 3 groups` |
| **DEBUG (D)** | Logcat (Debug only) | Domain UseCase invocations, local cache operations, MVI event routing, and UI action state updates. | `Event: CreateGroupUiEvent.OnNameChanged` |
| **INFO (I)** | Logcat + Telemetry | High-level milestones, screen views, and user-driven events. Safe for release builds. | `Navigated to: group_detail` |
| **WARN (W)** | Logcat + Crashlytics | Recoverable issues, network timeouts, offline fallback matches, validation rejections. | `Failed to sync group to cloud. Retrying...` |
| **ERROR (E)** | Logcat + Crashlytics | Non-recoverable failures, DB corruptions, unhandled exceptions. | `Critical failure during currency exchange resolution` |

---

## 🏷️ Structured Log Tags

Always use structured tags defined in `LogTag`:

- `LogTag.TELEMETRY` (`"SplitTrip:Telemetry"`): High-level user actions (onboarding complete, group created, expense added, tab changes).
- `LogTag.NAVIGATION` (`"SplitTrip:Navigation"`): Route transitions and root navigation events.
- `LogTag.MVI` (`"SplitTrip:MVI"`): ViewModel UI events and action routing.
- `LogTag.USE_CASE` (`"SplitTrip:UseCase"`): Domain UseCase entries and exits.
- `LogTag.SERVICE` (`"SplitTrip:Service"`): Domain Services (e.g. calculation engines, validations).
- `LogTag.SYNC` (`"SplitTrip:Sync"`): Offline-first Firestore database reconciliation.
- `LogTag.NETWORK` (`"SplitTrip:Network"`): OkHttp HTTP transactions (limited to `BASIC` level in debug to prevent large JSON bodies).

---

## 🔍 Logcat Filtering Guide (How to follow user journey)

To navigate through logs efficiently in Android Studio (Dolphin and newer) or via the Command Line Interface (CLI), use these standard filters.

### A. Android Studio Logcat Window (Dolphin+)

Paste these expressions directly into the Android Studio Logcat search bar:

1. **Follow the User Journey (Telemetry & Screen Views)**
   Show only what screens the user opened and what actions they completed:
   ```text
   tag:SplitTrip:Telemetry | tag:SplitTrip:Navigation
   ```

2. **Isolate Domain Logic (UseCases & Services)**
   Trace data flow and calculation execution without UI or network logs:
   ```text
   tag:SplitTrip:UseCase | tag:SplitTrip:Service
   ```

3. **Mute High-Frequency Noise (Clean Diagnostics)**
   Show all application logs *except* background synchronization loops and OkHttp network traffic:
   ```text
   package:mine -tag:SplitTrip:Sync -tag:SplitTrip:Network
   ```

4. **Filter for Exceptions & Errors**
   Show only system failures or warnings:
   ```text
   package:mine level:WARN | level:ERROR
   ```

---

### B. ADB CLI Commands

Run these command-line tools in your terminal:

1. **Follow the User Journey (Telemetry & Navigation)**
   ```bash
   adb logcat -s SplitTrip:Telemetry SplitTrip:Navigation
   ```

2. **Mute Sync and Network Logs (View everything else at DEBUG level)**
   ```bash
   adb logcat SplitTrip:Sync:S SplitTrip:Network:S *:D
   ```

3. **Strict App-Only Diagnostics (Exclude all system and framework noise)**
   ```bash
   adb logcat *:S SplitTrip:UseCase:D SplitTrip:Service:D SplitTrip:Navigation:D SplitTrip:MVI:D SplitTrip:Telemetry:D
   ```

---

## 🛡️ PII Sanitization

To comply with data privacy regulations, **never** print raw user credentials, emails, or names to logs. Use the `maskEmail()` extension function to sanitize emails before logging:

```kotlin
import es.pedrazamiguez.splittrip.core.logging.sanitizer.maskEmail

// YES: Sanitized and safe
Timber.tag(LogTag.MVI).d("User logging in with email: %s", email.maskEmail())

// NO: Exposes personal email in logs
Timber.tag(LogTag.MVI).d("User logging in with email: %s", email)
```

---

## 📈 Telemetry Observability

The `TelemetryTracker` API defines a clean interface for tracking user behaviors.

### The API Contract
```kotlin
package es.pedrazamiguez.splittrip.core.logging

interface TelemetryTracker {
    fun trackScreenView(screenName: String, className: String?)
    fun trackEvent(eventName: String, params: Map<String, Any> = emptyMap())
    fun setUserId(userId: String?)
    fun setUserProperty(name: String, value: String?)
}
```

### Implementations
1. **`FirebaseTelemetryTracker`** (Production): Routes events to `FirebaseAnalytics` to capture aggregate user engagement metrics.
2. **`DebugTelemetryTracker`** (Development/Debug): Formats events and logs them directly to Logcat under the `SplitTrip:Telemetry` tag, enabling local journey testing.
