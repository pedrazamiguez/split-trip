# Offline-First Architecture & Synchronization

In our application, **Offline Support** is not an afterthought or a "cache"; it is the primary way the app functions. This document outlines the architectural rules ensuring the app remains responsive and consistent, regardless of network connectivity.

## Core Principle: Local Database as the Single Source of Truth

To provide a seamless user experience, the UI **never** observes the network directly. Instead, we follow a strict unidirectional data flow:

1. **Read:** The UI observes the **Local Database** (Room).
2. **Write:** User actions modify the **Local Database** first.
3. **Sync:** A background process synchronizes these changes with the **Cloud** (Firestore).

This ensures that when a user creates a group or adds an expense, the UI updates instantly, even if the device is in Airplane Mode.

## The Creation Flow (Write)

When creating new data (e.g., a Group or Expense), we cannot rely on the server to generate IDs or timestamps, as this would cause items to be missing or unsortable until a sync occurs.

### 1. Generate IDs Locally

We generate unique identifiers (UUIDs) on the client side before saving. This guarantees that the object exists with a known ID immediately.

**Correct Implementation:**

```kotlin
val expenseId = UUID.randomUUID().toString()
val expense = expense.copy(id = expenseId)

```

**Why:** If we let Firestore generate the ID (`.add()`), we would have a temporary local object without an ID, and a future cloud object with a different ID, leading to duplicates during sync.

### 2. Generate Metadata Locally

Timestamps (creation dates) and attribution (User IDs) must be populated locally.

* **Timestamps:** Use `System.currentTimeMillis()` immediately. Do not rely on Firestore `FieldValue.serverTimestamp()` for the local copy, or the item will appear at the wrong position (or not at all) in sorted lists.
* **User ID:** Inject the `AuthenticationService` into your Repository to fetch the current user's ID synchronously.

### 3. Save Local, Then Sync

The repository method should look like this:

1. **Prepare:** Create the object with a local UUID and timestamp.
2. **Local Commit:** Save to Room. The UI updates instantly via the `Flow`.
3. **Cloud Sync:** Launch a background coroutine to upload the data.

```kotlin
// Example Repository Implementation
override suspend fun addExpense(expense: Expense) {
    // 1. Prepare
    val finalExpense = expense.copy(
        id = UUID.randomUUID().toString(),
        createdAt = System.currentTimeMillis()
    )

    // 2. Local Commit (UI updates now)
    localDataSource.saveExpense(finalExpense)

    // 3. Cloud Sync (Background)
    scope.launch {
        cloudDataSource.upsertExpense(finalExpense)
    }
}

```

## The Synchronization Flow (Read)

When fetching data from the cloud, we must ensure we don't accidentally overwrite newer local changes or create duplicates.

### 1. Upsert Strategy (Set vs Add)

Since we generated the ID locally, our Cloud Data Source must use **Upsert** logic (`set` with the specific ID), not `add`.

* **Cloud:** `.document(id).set(data)`
* **Local:** `@Insert(onConflict = OnConflictStrategy.REPLACE)`

### 2. Handling Race Conditions

A common "flicker" bug occurs when the app downloads an old list from the cloud and overwrites a list containing a brand-new local item.

To prevent this:

* Use `OnConflictStrategy.REPLACE` when saving synced data.
* **Do not** delete the entire local table before inserting cloud data. Only insert/update the items returned from the cloud. This leaves your unsynced local items intact.

## Real-Time Multi-Device Sync

While we are Offline-First, this is a **multi-user, multi-device** app. Changes made by one user (or on one device) must eventually propagate to all other users/devices that share the same data. We achieve this using **Firestore Snapshot Listeners** that feed into Room, keeping the local-first flow intact.

### The Pattern: Incremental Delta Sync → Room Reconciliation

For the 6 group subcollections (`expenses`, `contributions`, `cash_withdrawals`, `cash_transfers`, `subunits`, `settlements`), repositories delegate synchronization to `syncGroupCollectionDelta()` in `DeltaSyncDelegates.kt`:

1. **Read:** The UI observes the **Room Flow** (`local*DataSource.get*ByGroupIdFlow(groupId)`).
2. **Checkpoint Lookup:** On start (managed by `KeyedSubscriptionTracker`), `LocalSyncCheckpointDataSource` loads the persisted `SyncWatermark(seconds, nanoseconds)` for `(groupId, collectionName)` from Room (`group_sync_checkpoints`). Checkpoints older than 30 days (`SyncCheckpointPolicy.CHECKPOINT_MAX_AGE_MILLIS`) are cleared and treated as `null` (falling back to a full snapshot so rows whose 60-day tombstones were TTL-purged get pruned).
3. **Two-Phase Bootstrap → Delta Listener:**
   - **Null / expired checkpoint:** Paginated full snapshot (`orderBy(FieldPath.documentId()).limit(500)`) reconciles Room via `replace*ForGroup`, persists the server-confirmed `maxServerUpdatedAt` watermark, and then attaches the bounded delta listener.
   - **Valid checkpoint:** Immediately attaches `whereGreaterThan("serverUpdatedAt", cursor).orderBy("serverUpdatedAt", ASC).limit(500)` with `MetadataChanges.INCLUDE`, applying incremental upserts + tombstone deletions (`isDeleted == true`) via `apply*DeltaForGroup`. If a server-confirmed emission hits the 500-doc limit and advances the watermark, the listener automatically re-subscribes from the new cursor.
   - **Archived groups:** Perform a single one-shot `fetch*Delta` (or `fetch*FullSnapshot` if null/expired) per session.
4. **Server-Confirmed Monotonic Watermarks:** Watermarks advance strictly monotonically (`candidate > currentWatermark`) and **only** when `!snapshot.metadata.isFromCache && !snapshot.metadata.hasPendingWrites()`.

```kotlin
// Repository (Delta Sync Pattern)
override fun getGroupSubunitsFlow(groupId: String): Flow<List<Subunit>> =
    localSubunitDataSource.getSubunitsByGroupIdFlow(groupId)
        .trackSubscription(subscriptionTracker, groupId, syncScope) {
            syncGroupCollectionDelta(
                DeltaSyncParams(
                    groupId = groupId,
                    collectionName = SyncCollectionName.SUBUNITS,
                    checkpointDataSource = localSyncCheckpointDataSource,
                    // ... fetchFullSnapshot, fetchDeltaOnce, observeDeltaFlow,
                    // replaceLocal, applyDeltaLocal, getPendingIds, verifyOnServer, markSynced
                )
            )
        }
```

### Soft-Delete Tombstones & TTL

All 6 synced subcollections perform deletions via idempotent merge tombstones (`SetOptions.merge()`) instead of hard `.delete()` calls:
- Sets `groupId`, `isDeleted = true`, `deletedAt = FieldValue.serverTimestamp()`, `serverUpdatedAt = FieldValue.serverTimestamp()`, `expireAt = Timestamp(now + 60 days)`, and optional `lastUpdatedBy`.
- Preserves offline `create → delete` FIFO ordering and existing document fields needed by Cloud Functions notifications.
- `firestore.indexes.json` defines `expireAt` TTL (`"ttl": true`, `"indexes": []`) on all 6 collection groups.

*(Legacy full-snapshot pattern using `subscribeAndReconcile()` in `CloudSyncDelegates.kt` remains used by `GroupRepositoryImpl`.)*

### ⚠️ Preventing Duplicate Snapshot Listeners

Because `onStart` fires every time the Flow gets a new collector (config changes, `WhileSubscribed` resubscriptions, `flatMapLatest` restarts), launching a new listener in `syncScope` without cancelling the previous one would **leak** Firestore snapshot listeners. Each leaked listener keeps performing `replaceAll` writes to Room, wasting resources and causing duplicate reconciliation loops.

**The Rule:** Track the cloud subscription as a `Job` and cancel it before launching a new one.

```kotlin
// ❌ BAD - Leaks a new listener on every resubscription
syncScope.launch { subscribeToCloudChanges() }

// ✅ GOOD - At most one active listener at any time
cloudSubscriptionJob?.cancel()
cloudSubscriptionJob = syncScope.launch { subscribeToCloudChanges() }
```

For repositories with **keyed** subscriptions (e.g., expenses per group), use a `ConcurrentHashMap<String, Job>` to track one listener per key:

```kotlin
private val cloudSubscriptionJobs = ConcurrentHashMap<String, Job>()

// In onStart:
cloudSubscriptionJobs[groupId]?.cancel()
cloudSubscriptionJobs[groupId] = syncScope.launch {
    subscribeToCloudChanges(groupId)
}
```

### Merge Reconciliation in a `@Transaction`

Because each Firestore snapshot represents the **complete** current state, we need to reconcile the local database with it. However, a naive **delete all + insert** would destroy locally-created items that haven't synced to Firestore yet (e.g., a group created offline whose cloud write hasn't completed).

Instead, we use a **merge strategy** — upsert remote data, then selectively delete only stale items:

```kotlin
// Room DAO
@Transaction
suspend fun replaceExpensesForGroup(groupId: String, expenses: List<ExpenseEntity>) {
    val remoteIds = expenses.map { it.id }.toSet()
    val localIds = getExpenseIdsByGroupId(groupId)
    val staleIds = localIds.filter { it !in remoteIds }

    // 1. Upsert remote (adds new, updates existing)
    insertExpenses(expenses)       // @Upsert

    // 2. Remove only stale items (exist locally but not in remote snapshot)
    if (staleIds.isNotEmpty()) {
        deleteExpensesByIds(staleIds)
    }
}
```

This handles all cases safely:
- **Additions** by other users → upserted into Room.
- **Deletions** by other users → removed as stale.
- **Modifications** by other users → updated via upsert.
- **Unsynced local items** → preserved because Firestore's latency compensation includes pending local writes in snapshot emissions. In the rare race where a snapshot fires before the Firestore SDK caches the local write, the merge strategy provides an extra safety net — the item stays in Room until the next snapshot includes it.

> **Why not destructive `deleteAll + insertAll`?** A destructive replace wipes locally-created items that haven't reached Firestore yet. It also triggers unnecessary `ForeignKey CASCADE` deletions (e.g., all expenses for a group), causing UI flicker even when only the group metadata changed.

### 🛑 Critical: Subcollection Cleanup on Deletion

Firestore does **NOT** automatically delete subcollections when a parent document is deleted. If your real-time listener watches a **subcollection** (e.g., `group_members`) to determine data membership, you **MUST** explicitly delete those subcollection documents before deleting the parent.

**Example: Group Deletion**

The group real-time listener watches `group_members` collectionGroup to know which groups the user belongs to. If we only delete the group document, the orphaned member documents remain, and the listener on other devices **never fires** — other users continue to see the deleted group.

```kotlin
// ❌ BAD - Only deletes parent, orphans subcollection
override suspend fun deleteGroup(groupId: String) {
    firestore.collection("groups").document(groupId).delete().await()
    // group_members subcollection still exists → other devices still "see" this group
}

// ✅ GOOD - Deletes subcollection FIRST, then parent
override suspend fun deleteGroup(groupId: String) {
    // 1. Delete all member documents → triggers listener on other devices
    val membersCollection = firestore.collection("groups/$groupId/members")
    val memberDocs = membersCollection.get().await()
    memberDocs.documents.forEach { doc ->
        doc.reference.delete().await()
    }

    // 2. Delete the group document itself
    firestore.collection("groups").document(groupId).delete().await()
}
```

**Rule:** When deleting a document that has subcollections used by real-time listeners, always delete the subcollection documents **first** so the listener fires and propagates the deletion to all devices.

### Firestore Data Source: Cache-First Loading

The Firestore `CloudDataSource` implementations use a **cache-first** strategy inside the snapshot listener callback:

1. **Cache hit:** Load documents from Firestore's local cache (instant, no network).
2. **Cache miss:** Fall back to server fetch for any documents not yet cached.

This minimizes network round-trips while ensuring data completeness.

## Reusable Sync Delegates

All the patterns described above (snapshot subscription management, reconciliation, create-sync, delete-sync) are encapsulated in reusable utility functions in the `es.pedrazamiguez.splittrip.data.sync` package (`:data` module). **New repositories MUST use these delegates** instead of duplicating the boilerplate.

> **Location:** `data/src/main/kotlin/.../data/sync/`
> **Visibility:** `internal` — scoped to the `:data` module only. Not injectable via Koin.

### `KeyedSubscriptionTracker`

Manages a set of keyed cloud subscription `Job`s, ensuring only one active subscription exists per key. Replaces the manual `ConcurrentHashMap<String, Job>` + cancel + relaunch pattern.

```kotlin
internal class KeyedSubscriptionTracker {
    fun cancelAndRelaunch(key: String, scope: CoroutineScope, block: suspend () -> Unit)
}
```

**When to use:** Any group-keyed repository (Expense, Subunit, Contribution, CashWithdrawal) where subscriptions are scoped per `groupId`.

**When NOT to use:** `GroupRepositoryImpl` uses a single `Job?` — the pattern is trivial (3 lines) and doesn't benefit from the tracker's key management.

### `DeltaSyncDelegates.kt`

Encapsulates server-timestamped incremental delta synchronization (`syncGroupCollectionDelta`, `DeltaSyncParams`, `SyncCheckpointPolicy`) for all 6 group subcollections (`expenses`, `contributions`, `cash_withdrawals`, `cash_transfers`, `subunits`, `settlements`). See [ADR-0009](../adrs/0009-incremental-firestore-delta-sync.md) for the full protocol.

### `CloudSyncDelegates.kt`

Four top-level `internal` functions that encapsulate the common offline-first coordination patterns (used by `GroupRepositoryImpl` and shared write/pending-confirm helpers):

#### `subscribeAndReconcile<T>()`

Subscribes to a real-time cloud `Flow` and reconciles the local database on each emission. After reconciliation, attempts to confirm any `PENDING_SYNC` items via server verification.

```kotlin
internal suspend fun <T> subscribeAndReconcile(
    cloudFlow: Flow<List<T>>,           // Firestore snapshot listener Flow
    reconcileLocal: suspend (List<T>) -> Unit,  // Merge strategy (upsert + selective delete)
    getPendingIds: suspend () -> List<String>,   // PENDING_SYNC item IDs
    verifyOnServer: suspend (String) -> Boolean, // Source.SERVER round-trip check
    markSynced: suspend (String) -> Unit,        // Transition to SYNCED
    entityLabel: String,                         // For Timber logging ("expense", "subunit")
    logContext: String                           // Extra log context ("for group abc-123")
)
```

**Encapsulates:** `subscribeToCloudChanges()` + `confirmPendingSyncXxx()` — the ~25-line pattern that was duplicated across all 5 entity repositories.

#### `confirmPendingSync()`

Attempts to confirm `PENDING_SYNC` entities by verifying their existence on the server. Called automatically by `subscribeAndReconcile` after each reconciliation cycle, but also available standalone.

```kotlin
internal suspend fun confirmPendingSync(
    getPendingIds: suspend () -> List<String>,
    verifyOnServer: suspend (String) -> Boolean,
    markSynced: suspend (String) -> Unit,
    entityLabel: String
)
```

**Handles:** The `PENDING_SYNC → SYNCED` transition when the device comes back online after an app restart (where the original `syncScope` coroutine was killed before completing).

#### `syncCreateToCloud()`

Launches a background coroutine that pushes a locally-saved entity to the cloud and transitions its sync status.

```kotlin
internal fun syncCreateToCloud(
    scope: CoroutineScope,
    entityId: String,
    params: SyncCreateParams
)
```

**Status transitions:** Success → `SYNCED`, Failure → `SYNC_FAILED`.

**Use for:** Both `create` and `update` operations (the sync orchestration is identical — only the cloud write lambda differs).

#### `syncDeletionToCloud()`

Launches a background coroutine that deletes an entity from the cloud.

```kotlin
internal fun syncDeletionToCloud(
    scope: CoroutineScope,
    entityId: String,
    cloudDelete: suspend () -> Unit,
    entityLabel: String
)
```

**Key behavior:** Always queues the cloud deletion — Firestore SDK guarantees write ordering, so a pending `SET` (from creation) executes before this `DELETE` when connectivity is restored.

### Repository Usage Pattern (Reference)

Here is the canonical pattern for a group-keyed repository using all delegates:

```kotlin
class SubunitRepositoryImpl(
    private val cloudSubunitDataSource: CloudSubunitDataSource,
    private val localSubunitDataSource: LocalSubunitDataSource,
    private val authenticationService: AuthenticationService,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : SubunitRepository {

    private val syncScope = CoroutineScope(ioDispatcher)
    private val subscriptionTracker = KeyedSubscriptionTracker()

    // CREATE: Enrich metadata (entity-specific) → save local → delegate sync
    override suspend fun createSubunit(groupId: String, subunit: Subunit): String {
        val subunitWithMetadata = subunit.copy(/* entity-specific fields */)
        localSubunitDataSource.saveSubunit(subunitWithMetadata)

        syncCreateToCloud(
            scope = syncScope,
            entityId = subunitWithMetadata.id,
            params = SyncCreateParams(
                cloudWrite = { cloudSubunitDataSource.addSubunit(groupId, subunitWithMetadata) },
                updateSyncStatus = localSubunitDataSource::updateSyncStatus,
                entityLabel = "subunit",
                performanceMonitor = performanceMonitor
            )
        )
        return subunitWithMetadata.id
    }

    // DELETE: Remove local → delegate cloud deletion
    override suspend fun deleteSubunit(groupId: String, subunitId: String) {
        localSubunitDataSource.deleteSubunit(subunitId)

        syncDeletionToCloud(
            scope = syncScope,
            entityId = subunitId,
            cloudDelete = { cloudSubunitDataSource.deleteSubunit(groupId, subunitId) },
            entityLabel = "subunit"
        )
    }

    // READ: Local Flow + managed delta sync subscription
    override fun getGroupSubunitsFlow(groupId: String): Flow<List<Subunit>> =
        localSubunitDataSource.getSubunitsByGroupIdFlow(groupId)
            .trackSubscription(subscriptionTracker, groupId, syncScope) {
                syncGroupCollectionDelta(
                    DeltaSyncParams(
                        groupId = groupId,
                        collectionName = SyncCollectionName.SUBUNITS,
                        checkpointDataSource = localSyncCheckpointDataSource,
                        isGroupArchived = {
                            localGroupDataSource.getGroupById(groupId)?.status == GroupStatus.ARCHIVED
                        },
                        syncedArchivedGroupIds = syncedArchivedGroupIds,
                        fetchFullSnapshot = { cloudSubunitDataSource.fetchSubunitsFullSnapshot(groupId) },
                        fetchDeltaOnce = { watermark ->
                            cloudSubunitDataSource.fetchSubunitsDelta(groupId, watermark)
                        },
                        observeDeltaFlow = { watermark ->
                            cloudSubunitDataSource.observeSubunitsDeltaFlow(groupId, watermark)
                        },
                        replaceLocal = { remoteSubunits ->
                            localSubunitDataSource.replaceSubunitsForGroup(groupId, remoteSubunits)
                        },
                        applyDeltaLocal = { upserts, deletedIds ->
                            localSubunitDataSource.applySubunitsDeltaForGroup(groupId, upserts, deletedIds)
                        },
                        getPendingIds = {
                            localSubunitDataSource.getPendingSyncSubunitIds(groupId)
                        },
                        verifyOnServer = { id ->
                            cloudSubunitDataSource.verifySubunitOnServer(groupId, id)
                        },
                        markSynced = { id ->
                            localSubunitDataSource.updateSyncStatus(id, SyncStatus.SYNCED)
                        },
                        entityLabel = "subunit",
                        logContext = "for group $groupId",
                        performanceMonitor = performanceMonitor
                    )
                )
            }
}
```

### What Remains Entity-Specific (NOT Extractable)

The delegates eliminate scaffolding boilerplate. These concerns stay in each repository:

| Concern | Example |
|---|---|
| **Metadata enrichment** (`.copy()` fields) | `createdBy`, `withdrawnBy`, `remainingAmount`, `members` |
| **`GroupRepositoryImpl` two-phase verification** | `createGroup` → `verifyGroupOnServer` |
| **`GroupRepositoryImpl` `GroupDeletionRetryScheduler`** | WorkManager retry for failed deletions |
| **`GroupRepositoryImpl` cloud fallback** | `getGroupById` tries local, then cloud |
| **`CashWithdrawalRepositoryImpl` batch updates** | `updateRemainingAmounts()`, `refundTranche()` |
| **`ContributionRepositoryImpl` linked deletion** | `deleteByLinkedExpenseId()` |

### Testing Delegates

The delegates have their own unit tests in `data/src/test/.../data/sync/`:

| Test File | Coverage |
|---|---|
| `KeyedSubscriptionTrackerTest.kt` | Cancel-and-relaunch, concurrent keys, relaunch after completion |
| `CloudSyncDelegatesTest.kt` | Reconciliation (happy path, error), pending-sync confirm/unreachable/empty, create-sync SYNCED/SYNC_FAILED, deletion-sync success/failure, non-blocking verification |

**Repository tests remain unchanged** — the delegates are an internal implementation detail. Existing repository test files continue to test through the public repository interface without knowledge of the delegates.

## Summary Checklist

When implementing a new repository, verify these points:

* [ ] **Use Sync Delegates:** Is the repository using `subscribeAndReconcile`, `syncCreateToCloud`, `syncDeletionToCloud` from `data.sync`?
* [ ] **Subscription Tracking:** Is `KeyedSubscriptionTracker` used for group-keyed repos? Is a single `Job?` used for non-keyed repos?
* [ ] **No Network Block:** Does the repository save to Room *before* making any network call?
* [ ] **Local IDs:** Is the ID generated using `UUID` in the Repository (or UseCase)?
* [ ] **Local Dates:** Is `LocalDateTime.now()` used for the creation date?
* [ ] **Conflict Resolution:** Does the DAO use `OnConflictStrategy.REPLACE`?
* [ ] **No Blind Deletes:** Ensure the sync process doesn't wipe unsynced local items.
* [ ] **Real-Time Listener:** Does the Repository subscribe to a Firestore snapshot listener via `onStart` for multi-user/multi-device sync?
* [ ] **Single Subscription:** Is the cloud listener tracked and cancelled before re-launching to prevent duplicate listeners?
* [ ] **Merge Reconciliation:** Does the DAO use `@Transaction` with a merge strategy (upsert + selective delete) instead of destructive `deleteAll + insertAll`?
* [ ] **Subcollection Cleanup:** When deleting a parent document, are subcollection documents deleted first to trigger listeners on other devices?
* [ ] **`CoroutineDispatcher` Injection:** Is `ioDispatcher` injectable with a default of `Dispatchers.IO` for testability?

---

## Room DB Migrations & Performance Notes

### Migration 46 → 47 (Incremental Delta Sync Checkpoints)

Added `group_sync_checkpoints` table (`PK(groupId, collectionName)`, `FOREIGN KEY(groupId) REFERENCES groups(id) ON DELETE CASCADE`, index on `groupId`) storing lossless `(lastServerUpdatedSeconds, lastServerUpdatedNanos)` watermarks and `lastSyncedAtClientMillis` per group subcollection.

### Migration 25 → 26 (Scope-Aware FIFO Indexes)

Added as part of the scope-aware cash tranche work (#1008, #1012). New composite indexes:

| Table | Index | Purpose |
|---|---|---|
| `cash_withdrawals` | `(groupId, currency, withdrawalScope, remainingAmount)` | Covers all 4 scoped FIFO queries; enables `remainingAmount > 0` filter without full-table scan |
| `cash_withdrawals` | `(groupId, syncStatus)` | Pending sync checks |
| `expenses` | `(groupId, syncStatus)` | Pending sync checks |
| `contributions` | `(groupId, syncStatus)` | Pending sync checks |

### Balance Computation: Dispatcher & Debounce

`GetMemberBalancesFlowUseCase` is CPU-bound (no I/O). The combined balance flow in `BalancesViewModel` applies:

```kotlin
.debounce(AppConstants.BALANCE_COMPUTATION_DEBOUNCE_MS)  // 300ms — absorbs rapid Firestore reconciliation bursts
.flowOn(Dispatchers.Default)                              // CPU-bound computation off the main thread
```

`BalancesViewModel` accepts `computationDispatcher: CoroutineDispatcher = Dispatchers.Default` as an injectable constructor parameter for testability — pass `StandardTestDispatcher()` in tests so `advanceUntilIdle()` controls both the debounce timer and the `flowOn` context.

### `@DatabaseView` (Deferred)

A `@DatabaseView` partial aggregate for `expense_splits` was evaluated as a potential optimization. Decision: **deferred**. `computeMemberBalances()` requires full per-split rows for FIFO cash attribution and add-on handling (`BigDecimal` operations). A view would reduce the Kotlin loop cost but would not eliminate the computation. A `// TODO(#1012): revisit @DatabaseView` comment marks the decision point in `BalancesViewModel`.

If the balance screen becomes a bottleneck at >500 expenses, the alternative is a **pre-computed `member_balance_snapshots` table** (updated transactionally on each write). This is tracked in #1012 as the "High Effort" option.

