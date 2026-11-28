# ADR-0009: Server-Timestamped Incremental Firestore Delta Sync & Soft-Delete Tombstones

* **Status:** Accepted
* **Deciders:** SplitTrip Engineering Team
* **Date:** 2026-11-27
* **Tags:** data, offline-first, room, firestore, delta-sync, cost-optimization

---

## Context and Problem Statement

Previously, all 6 group subcollection listeners (`expenses`, `contributions`, `cash_withdrawals`, `cash_transfers`, `subunits`, `settlements`) attached unfiltered `addSnapshotListener` queries across the entire subcollection and reconciled the complete snapshot into Room via `replace*ForGroup()`.

Because Cloud Firestore bills per document retrieved—and re-evaluates/re-bills the entire query whenever a mobile `SnapshotListener` reconnects after >30 minutes offline (routine during flights and travel) or on cold starts—groups with 200+ expenses re-read every document repeatedly. Furthermore, client wall-clock `lastUpdatedAt` fields (`LocalDateTime.now()`) cannot prevent clock-skew skips, and hard deletes cannot be observed incrementally by `whereGreaterThan("serverUpdatedAt", cursor)`.

How do we transition all 6 group subcollections to incremental delta synchronization while preserving SplitTrip's offline-first Room-as-SSOT architecture and handling deletions, offline create→delete ordering, and TTL cleanup safely?

## Decision Drivers

* Eliminate O(N) full-collection re-reads on cold starts and post-travel (>30 min) reconnects.
* Zero reliance on client wall-clock timestamps for incremental sync cursors (clock-skew safety).
* Deterministic cross-device deletion propagation without breaking offline `create → delete` FIFO ordering.
* Bounded tombstone retention via Firestore TTL with graceful full-sync recovery for stale clients.
* Preserve Room as the sole UI source of truth (`Flow` from Room only).

## Considered Options

1. **Unfiltered Full-Collection Listeners (Status Quo):** Simple, but re-bills all documents on cold start and >30-min offline reconnects.
2. **Client-Clock Delta Queries (`whereGreaterThan("lastUpdatedAt", ...)`):** Vulnerable to client clock skew causing missed updates and unable to detect hard deletes.
3. **Server-Timestamped Delta Sync + Soft-Delete Tombstones + Firestore TTL (Chosen):** Monotonic server timestamps (`serverUpdatedAt`), lossless `(seconds, nanoseconds)` Room checkpoints, idempotent merge tombstones with 60-day `expireAt` TTL, and 30-day client fallback.

## Decision Outcome

Chosen option: **Option 3 (Server-Timestamped Delta Sync + Soft-Delete Tombstones + Firestore TTL)**.

### Key Protocol Rules

1. **Lossless Server Watermark (`SyncWatermark`):**
   - Domain value object `SyncWatermark(seconds: Long, nanoseconds: Int)` implements `Comparable<SyncWatermark>` and avoids millisecond truncation.
   - Stored per `(groupId, collectionName)` in Room table `group_sync_checkpoints` (Room v47, `Migration46To47`, `ON DELETE CASCADE` on `groups.id`).
   - All creates, updates, cash-tranche withdrawal deductions, pending-user reconciliations, subunit member removals, and soft-deletes populate `serverUpdatedAt = FieldValue.serverTimestamp()` (or `@field:ServerTimestamp`).

2. **Clock-Skew-Safe & Server-Confirmed Watermark Advancement:**
   - Listeners attach with `MetadataChanges.INCLUDE`.
   - Watermarks advance strictly monotonically (`candidate > currentWatermark`) and **only** when `!snapshot.metadata.isFromCache && !snapshot.metadata.hasPendingWrites()`.
   - Empty or legacy collections lacking `serverUpdatedAt` never synthesize a client-clock watermark.

3. **Soft-Delete Tombstones & 60-Day TTL / 30-Day Fallback:**
   - Deletions write an idempotent merge tombstone (`SetOptions.merge()`) with `groupId`, `isDeleted = true`, `deletedAt = FieldValue.serverTimestamp()`, `serverUpdatedAt = FieldValue.serverTimestamp()`, `expireAt = Timestamp(now + 60 days)`, and optional `lastUpdatedBy`.
   - Using `SetOptions.merge()` preserves existing fields needed by Cloud Functions notifications and prevents `NOT_FOUND` errors on offline `create → delete`.
   - `firestore.indexes.json` configures `expireAt` TTL (`"ttl": true, "indexes": []`) on all 6 collection groups.
   - `SyncCheckpointPolicy` treats checkpoints older than 30 days (`CHECKPOINT_MAX_AGE_MILLIS`) as expired (half of the 60-day TTL), clearing the checkpoint and falling back to a full paginated snapshot (`replace*ForGroup`) so stale rows whose tombstones were TTL-purged are pruned. Backwards clock skew (`nowMillis < watermark`) is never treated as expired.

4. **Two-Phase Bootstrap → Bounded Delta Loop (`DeltaSyncDelegates.kt`):**
   - **Active groups with valid checkpoint:** Immediately attach `whereGreaterThan("serverUpdatedAt", cursor).orderBy("serverUpdatedAt", ASC).limit(500)`. If a server-confirmed emission reaches `500` documents and advances the watermark, the listener cleanly closes (`PageCursorAdvanceSignal`) and re-subscribes from the new cursor.
   - **Active groups with null/expired checkpoint:** Paginated full fetch (`orderBy(FieldPath.documentId()).limit(500)` + `startAfter`) → `replace*ForGroup` → persist watermark → attach delta listener.
   - **Archived groups:** One-shot `fetch*Delta` (or `fetch*FullSnapshot` when null/expired) once per session.

5. **Cloud Functions & Firestore Rules Alignment:**
   - Cloud Functions suppress creation notifications when `isDeleted === true`, emit `EXPENSE_DELETED` on `before.isDeleted !== true && after.isDeleted === true` in `onExpenseUpdated`, suppress TTL-purge `onDocumentDeleted` events when `isDeleted === true`, and skip tombstoned expenses in `expenseReminders`.
   - `firestore.rules` splits subcollection `allow read` into `allow get: if isAuthenticated();` and `allow list: if isAuthenticated() && hasValidQueryLimit(500);`, and validates payload bounds and `serverUpdatedAt` anti-forgery without adding per-document `get()` membership checks.

6. **Rolling-Upgrade Compatibility:**
   - Legacy clients continue reading active documents; `firestore.rules` permits optional `serverUpdatedAt` (`!(fieldName in request.resource.data) || ... == request.time`) and retains `allow delete` during rollout.
   - Legacy hard-deletes continue to trigger `onExpenseDeleted` when `isDeleted !== true`.

## Consequences

### Positive
* Eliminates redundant full-collection reads on cold starts and post-travel reconnects (~10x read reduction for large groups).
* Propagates deletions across devices cleanly offline and online.
* Protects Firestore against unbounded list queries and oversized payloads.

### Negative / Trade-offs
* Adds Room schema table `group_sync_checkpoints` (Migration 46 → 47) and soft-delete tombstone metadata fields.
* Deleted documents remain in Firestore for up to 60 days until TTL purge (acceptable for small metadata docs; `expireAt` is unindexed).

## References
* [`docs/architecture/patterns/offline-first.md`](../patterns/offline-first.md)
* [`docs/architecture/patterns/core-services-catalog.md`](../patterns/core-services-catalog.md) § G
* [`docs/architecture/adrs/0001-offline-first-sync.md`](0001-offline-first-sync.md)
