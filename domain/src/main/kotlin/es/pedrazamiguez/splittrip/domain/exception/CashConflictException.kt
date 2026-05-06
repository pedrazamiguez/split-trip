package es.pedrazamiguez.splittrip.domain.exception

/**
 * Thrown when a Firestore transaction for a cash-funded expense detects that one or more
 * consumed withdrawals have been modified by a concurrent write from another group member.
 *
 * The transaction reads the current `remainingAmount` for each consumed withdrawal,
 * compares against the client-observed expected values, and aborts if any mismatch is found.
 *
 * At the presentation layer, this causes the tranche preview to auto-refresh and a
 * user-friendly conflict message to be shown (same UX as Phase 1 race detection via
 * [InsufficientCashException] when the preview showed available cash).
 */
class CashConflictException : Exception("Cash withdrawal was modified by a concurrent write")
