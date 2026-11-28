# ADR-0010: Firestore Database Region Evaluation (`eur3` Multi-Region vs. Single-Region EU)

* **Status:** Accepted
* **Deciders:** SplitTrip Engineering Team
* **Date:** 2026-11-28
* **Tags:** firestore, cloud-functions, infrastructure, cost-optimization, architecture

---

## Context and Problem Statement

SplitTrip runs on the Firebase Blaze (pay-as-you-go) plan with its `(default)` Cloud Firestore database provisioned in the **`eur3` (Multi-Region EU)** location (Belgium `europe-west1` + Netherlands `europe-west4` + Finland `europe-north1` witness), while all Cloud Functions v2 triggers are pinned to **`europe-west1`** in [`functions/src/config.ts`](../../../functions/src/config.ts).

Multi-region `eur3` carries roughly a **1.67× per-operation and storage pricing multiplier** (~$0.06 vs. ~$0.036 per 100k document reads) compared to single-region European deployments such as `europe-west1`. Because SplitTrip is an offline-first application with Room as the local single source of truth ([ADR-0001](0001-offline-first-sync.md)), we evaluated before `v1.0.0` public launch whether to migrate `(default)` Firestore to a single EU region now while the dataset is small, or retain `eur3` alongside automated Blaze budget safeguards.

## Decision Drivers

* **Pre-Launch Operational Risk:** Avoid database downtime, Eventarc trigger re-binding, and closed-tester cache disruption during Google Play 14-day Closed Testing (`#1768`) days before `v1.0.0` public release.
* **Real-World Cost Impact at Early Scale:** Account for Firestore's daily free tier (50k reads / 20k writes / 20k deletes / 1 GiB storage) and [ADR-0009](0009-incremental-firestore-delta-sync.md) incremental delta sync + `#2116` listener/media optimizations.
* **Cloud Functions Co-Location & Platform Support:** Keep Cloud Functions v2 triggers co-located with Firestore to avoid cross-region trigger egress and latency.
* **High Availability vs. Offline-First Tolerance:** Compare `99.999%` multi-region SLA against `99.99%` multi-zone single-region SLA given Room-first local UX.

## Considered Options

| Dimension | `eur3` (Multi-Region EU) — **Retained** | `europe-west1` (Single-Region Belgium) | `europe-southwest1` (Single-Region Madrid) |
| :--- | :---: | :---: | :---: |
| **Document Reads** | `$0.06` / 100k | `$0.036` / 100k (`-40%`) | `$0.04`–`$0.044` / 100k (higher regional tier) |
| **Document Writes** | `$0.18` / 100k | `$0.108` / 100k (`-40%`) | Higher than `europe-west1` |
| **Document Deletes** | `$0.02` / 100k | `$0.012` / 100k (`-40%`) | Higher than `europe-west1` |
| **Storage** | `$0.18` / GiB-mo | `$0.108` / GiB-mo (`-40%`) | Higher than `europe-west1` |
| **Availability SLA** | `99.999%` (multi-region) | `99.99%` (multi-zone regional) | `99.99%` (multi-zone regional) |
| **Cloud Functions v2 Co-Location** | Partial (replicates across EU, primary in `europe-west1`/`europe-west4`) | Exact match (`functions/src/config.ts` uses `europe-west1`, zero cross-region egress) | **Unsupported** by Cloud Functions v2 (forces cross-region trigger traffic against `europe-west1`) |
| **Pre-Launch Migration Effort** | None | High (immutable `(default)` location requires full export → delete → recreate → redeploy → import) | Rejected |

* **`europe-southwest1` Rejected:** Firebase Cloud Functions v2 does not support `europe-southwest1`, which would force cross-region Eventarc/Firestore trigger traffic against `europe-west1` functions and incur higher regional unit rates than `europe-west1`.

## Unit-Economics & Scale Impact

With [ADR-0009](0009-incremental-firestore-delta-sync.md) server-timestamped delta sync (~10× read reduction on large groups), `#2116` listener/media optimizations, and Firestore's daily free tier (1.5M reads / 600k writes / 600k deletes per 30-day month), estimated monthly Firestore COGS compared against Baseline Net MRR from [`docs/engineering/monetization-and-unit-economics.md`](../../engineering/monetization-and-unit-economics.md) Table 4.3 is:

| MAU Scale | Est. Billable Firestore Spend (`eur3`) | Est. Spend (`europe-west1`) | Monthly Savings | Baseline Net MRR | `eur3` COGS % of Net MRR |
| :--- | :---: | :---: | :---: | :---: | :---: |
| **1,000 MAU** | `~$0.00–$0.50/mo` (mostly within free tier) | `~$0.00–$0.30/mo` | `< $0.25/mo` | `€111.25/mo` | `< 0.5%` |
| **10,000 MAU** | `~$1.50–$5.00/mo` | `~$0.90–$3.00/mo` | `~$0.60–$2.00/mo` | `€1,112.56/mo` | `< 0.5%` |
| **50,000 MAU** | `~$15–$35/mo` | `~$9–$21/mo` | `~$6–$14/mo` | `€5,562.81/mo` | `< 0.7%` |

At early scale (`< 10k MAU`), retaining `eur3` saves or costs less than a couple of dollars per month—far outweighed by pre-launch cutover risk.

## Migration Effort, Downtime & Immutability Trade-Off

Cloud Firestore does **not** support changing the location of an existing `(default)` database in place. Migrating `(default)` from `eur3` to `europe-west1` requires:

1. Putting clients into read-only / maintenance mode to prevent split-brain writes during cutover.
2. Exporting all collections to Cloud Storage (`gcloud firestore export`).
3. Deleting the `(default)` Firestore database (cannot be undone without re-import).
4. Recreating `(default)` in `europe-west1` and redeploying `firestore.rules`, `firestore.indexes.json` (including all 6 ADR-0009 `expireAt` TTL policies), and all Eventarc/Cloud Functions v2 triggers.
5. Importing the backup (`gcloud firestore import`) and verifying active closed-tester caches.

This introduces a **~15–30 minute maintenance window** plus closed-tester client cache disruption (`ADR-0009` checkpoints / offline write queues) days before `v1.0.0` launch, for `< $2/month` in expected savings below 10k MAU.

## HA Trade-Off Analysis

* `eur3` delivers a `99.999%` multi-region availability SLA across multiple European countries.
* Because SplitTrip uses Room as the local single source of truth ([ADR-0001](0001-offline-first-sync.md)), offline reads and queued writes continue working seamlessly during transient cloud unavailability; a future migration to `europe-west1` (`99.99%` multi-zone SLA) would therefore remain acceptable if scale economics later justify it.

## Decision Outcome

**Accepted — Retain `eur3` for `v1.0.0` public launch.**

1. **No pre-launch database migration:** Keep `(default)` Firestore in `eur3` and Cloud Functions v2 in `europe-west1`.
2. **Runaway-Spend Protection:** Pair `eur3` with the automated Cloud Billing Pub/Sub kill-switch (`onBillingBudgetAlert` at `€50/mo` hard cap, plus `€20/mo` soft email warnings) documented in [`docs/release/production-service-integrations.md`](../../release/production-service-integrations.md).
3. **Objective Re-Evaluation Gate:** Revisit migrating to `europe-west1` (via a scheduled maintenance window or named database cutover) **only if monthly Firestore COGS exceeds `5%` of Net MRR**.

## References
* [`docs/architecture/adrs/0001-offline-first-sync.md`](0001-offline-first-sync.md)
* [`docs/architecture/adrs/0009-incremental-firestore-delta-sync.md`](0009-incremental-firestore-delta-sync.md)
* [`docs/engineering/monetization-and-unit-economics.md`](../../engineering/monetization-and-unit-economics.md)
* [`docs/release/production-service-integrations.md`](../../release/production-service-integrations.md)
