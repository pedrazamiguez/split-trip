# Local AI Model Pack Hosting & Update Runbook (Cloudflare R2)

Operational guide for hosting, validating, and updating SplitTrip's downloadable on-device LiteRT-LM AI model packs (`.litertlm`).

---

## 1. Architecture & Cost Rationale

SplitTrip hosts downloadable `.litertlm` model binaries on **Cloudflare R2** rather than Firebase Cloud Storage:

- **Zero Egress Cost:** Cloudflare R2 provides **$0.00/GB egress** and **10 GB/month free storage** (Standard storage, `WEUR` region), avoiding Firebase Storage's **$0.12/GB** egress fee beyond the 1 GB/day free tier for ~1.5 GB model downloads.
- **Custom Domain Endpoint:** Artifacts in the `splittrip-ai-models` R2 bucket are served over HTTPS at `https://models.splittrip.eu/<filename>`.
- **Client Download & Verification Flow:**
  1. `AppConfigRepository.localAiModelPackConfig` reads `local_ai_model_pack_json` from Firebase Remote Config (parsed by `FirebaseModelPackConfigParser`).
  2. `LocalAiModelPackServiceImpl` validates that `downloadUrl` uses `https://` and `sha256Checksum` is non-blank, and checks device compatibility (`minRamMb`, `minFreeStorageBytes`).
  3. `LocalAiModelDownloadWorker` (`WorkManager`) delegates to `HttpModelPackDownloader` (`GET` with `instanceFollowRedirects = true`, requires `HTTP 200`, streams to `model.litertlm.part`).
  4. `ModelPackStorageManager.computeSha256()` verifies the downloaded `.part` file against `sha256_checksum` before atomically promoting it to `files/ai_models/<pack_id>/<version>/model.litertlm`.
- **Security Posture:**
  - R2's **Public Development URL** (`*.r2.dev`) is kept **disabled** so traffic only flows through `https://models.splittrip.eu`.
  - Firebase Storage [`storage.rules`](../../storage.rules) contains **no** `/public/ai-models/` exception; public unauthenticated reads are restricted exclusively to `/public/avatars/{imageId}`.
  - *Future hardening note:* If automated scraping or non-app egress abuse is ever observed, downloads can be gated behind a lightweight Cloudflare Worker or Firebase Callable Function issuing short-lived signed R2 URLs only to authenticated Pro users.

---

## 2. Active Production Configuration Reference

| Setting | Value |
|---|---|
| **Cloudflare R2 Bucket** | `splittrip-ai-models` |
| **Bucket Location** | `WEUR` (Western Europe, Standard storage) |
| **Custom Domain** | `https://models.splittrip.eu` (`r2.dev` disabled) |
| **Remote Config Key** | `local_ai_model_pack_json` (`parameterGroups.Receipts`) |
| **Active Pack ID** | `qwen2.5-1.5b-litert-v1` |
| **Active Version** | `1.0.0` |
| **Active Artifact** | `Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv4096.litertlm` (`1597922856` bytes) |

### `local_ai_model_pack_json` Schema

| Field | Type | Description |
|---|---|---|
| `pack_id` | `String` | Stable identifier for the model family (used in local storage path). |
| `version` | `String` | Semantic version string; bumping this triggers `isUpdateAvailable` on installed clients. |
| `download_url` | `String` | Full HTTPS URL under `https://models.splittrip.eu/...` pointing to an immutable filename. |
| `sha256_checksum` | `String` | Lowercase 64-char hex SHA-256 digest verified after download before promotion. |
| `size_bytes` | `Long` | Exact artifact size in bytes (used for progress fallback when `Content-Length` is absent). |
| `min_ram_mb` | `Int` | Minimum device RAM in MiB required to offer/run the model pack. |
| `min_free_storage_bytes` | `Long` | Minimum free internal storage in bytes required before starting download (`> size_bytes`). |
| `is_enabled` | `Boolean` | Master Remote Config toggle to enable or disable model pack availability. |

---

## 3. Model Update Workflow (Step-by-Step)

### Step 1 — Inspect Artifact Locally
Always use an **immutable/versioned filename** in R2 (never overwrite an existing filename in place) to avoid CDN edge-cache staleness during rollout.

Compute exact byte size and lowercase SHA-256 digest:
```bash
# Exact byte size (macOS / Linux)
stat -f%z <file>.litertlm   # macOS
stat -c%s <file>.litertlm   # Linux

# Lowercase SHA-256 checksum
shasum -a 256 <file>.litertlm
```

### Step 2 — Upload to Cloudflare R2
Upload the `.litertlm` artifact to the `splittrip-ai-models` bucket via Cloudflare Dashboard (**R2 → `splittrip-ai-models` → Upload**) or Wrangler CLI:
```bash
npx wrangler r2 object put splittrip-ai-models/<filename>.litertlm \
  --file=<local_path> \
  --content-type=application/octet-stream \
  --remote
```

### Step 3 — Verify HTTPS Endpoint & Range Support
Confirm Cloudflare serves the object with `HTTP/2 200`, matching `content-length`, and `accept-ranges: bytes` support:
```bash
curl -sI https://models.splittrip.eu/<filename>.litertlm
curl -sI -r 0-0 https://models.splittrip.eu/<filename>.litertlm
```
Verify the second command returns `HTTP/2 206`.

### Step 4 — Update Version-Controlled Remote Config Defaults
Update `local_ai_model_pack_json` in **both** files (keeping exact parity), bumping `version` so existing clients detect `isUpdateAvailable` (`config.isEnabled && installed.version != config.version`):
1. [`firebase/remoteconfig.template.json`](../../firebase/remoteconfig.template.json) (`parameterGroups.Receipts.parameters.local_ai_model_pack_json`)
2. [`data/firebase/src/main/res/xml/remote_config_defaults.xml`](../../data/firebase/src/main/res/xml/remote_config_defaults.xml)
3. Update expected metadata assertions in [`RemoteConfigTemplateConsistencyTest.kt`](../../data/firebase/src/test/kotlin/es/pedrazamiguez/splittrip/data/firebase/config/RemoteConfigTemplateConsistencyTest.kt).

### Step 5 — Validate Locally
Run the consistency validator and fast checks during iteration, and the full cold check before merging:
```bash
make remoteconfig-validate
make fast-check
make check > build.log 2>&1 && echo "Check passed successfully" || (tail -n 100 build.log && exit 1)
```

### Step 6 — Publish Live Firebase Remote Config

> [!IMPORTANT]
> `make remoteconfig-deploy` runs `scripts/merge_remote_config.py`, which performs an **additive, live-preserving merge**. It preserves existing live `defaultValue`s and will **NOT** overwrite an already-live `local_ai_model_pack_json` value.

1. Inspect live differences first:
   ```bash
   make remoteconfig-diff
   ```
2. Update `local_ai_model_pack_json` in live Firebase Remote Config safely:
   - **Recommended when live Console overrides exist** (e.g. `pro_allowlist_email_hashes`): update `local_ai_model_pack_json` directly in Firebase Console (or via Firebase MCP `remoteconfig_update_template` preserving all unrelated live values).
   - **Only if `make remoteconfig-diff` confirms no unrelated live overrides need preserving:**
     ```bash
     make remoteconfig-deploy OVERWRITE=true
     ```
3. Re-run `make remoteconfig-diff` to confirm `local_ai_model_pack_json` matches git with zero drift.

### Step 7 — End-to-End Verification & Old Artifact Retention
1. On a compatible emulator or physical device (≥ `min_ram_mb` RAM and ≥ `min_free_storage_bytes` free storage) signed in with a Pro-entitled account, navigate to **Settings → Local AI Model Pack**, trigger the download, and verify:
   - Download streams from `https://models.splittrip.eu/<filename>.litertlm`.
   - `ModelPackStatus.Verifying` succeeds against `sha256_checksum`.
   - Final state transitions to `ModelPackStatus.Installed` and `files/ai_models/<pack_id>/<version>/model.litertlm` exists on disk.
2. Retain the previous `.litertlm` object in R2 during the rollout window so Remote Config rollback (`make remoteconfig-rollback VERSION=<v>`) remains instantaneous, then delete superseded objects from `splittrip-ai-models` to stay within R2's 10 GB free storage tier.
