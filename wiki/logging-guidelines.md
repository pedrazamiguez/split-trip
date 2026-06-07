# Logging Guidelines

This document outlines the standard logging architecture and best practices for SplitTrip.

## Log Levels

- **VERBOSE (V):** Used for debugging scratchpads locally. **NEVER commit Verbose logs to Git**.
- **DEBUG (D):** Detailed internal state updates (SQL queries, cache state, MVI events/actions). Active in debug builds.
- **INFO (I):** Milestones, high-level pathways, and user journeys. Safe for production.
- **WARN (W):** Recoverable exceptions, offline cache fallbacks, bad user inputs.
- **ERROR (E):** Critical errors, database corruptions, unhandled failures.

## Logging Tags

Always use structured tags from `LogTag`:
- `LogTag.USE_CASE` ("SplitTrip:UseCase") - Domain UseCase invocations.
- `LogTag.SERVICE` ("SplitTrip:Service") - Domain Service invocations.
- `LogTag.NAVIGATION` ("SplitTrip:Navigation") - Route transitions.
- `LogTag.NETWORK` ("SplitTrip:Network") - HTTP requests and network transactions.
- `LogTag.SYNC` ("SplitTrip:Sync") - Cloud replica reconciliation.
- `LogTag.MVI` ("SplitTrip:MVI") - UI event routing.

## PII Sanitization

Never log plaintext PII (e.g. emails, names, billing totals). Use `maskEmail()` helper to sanitize email addresses before logging:
```kotlin
Timber.tag(LogTag.MVI).d("User email logged in: %s", email.maskEmail())
```
