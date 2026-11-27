# Local AI Model Pack Catalog & Device Capability Matrix

Engineering reference for SplitTrip's multi-tier downloadable on-device AI model catalog (`local_ai_model_pack_json`), device capability matching, Cloudflare R2 hosting, and Remote Config release procedure.

---

## 1. Engine Selection Priority (AICore vs. Local AI Model Packs)

SplitTrip resolves its on-device inference engine dynamically via `AiModelResolverImpl`:

1. **AICore (`AI_CORE_GEMMA_4`)** — Selected automatically when `AICoreCapabilityProvider.isSupported()` returns `true` (devices with system-level Gemini Nano / AICore support). No downloadable model pack is required.
2. **Downloaded Model Pack (`LITE_RT_LM`)** — Selected automatically on Pro-entitled devices where AICore is unsupported and a valid `.litertlm` pack is installed (`ModelPackStatus.Installed`).
3. **Fallback (`AI_CORE_GEMMA_4` unavailable)** — When AICore is unsupported and no local pack is installed, the Settings screen displays the catalog of compatible tiers and the expense flow surfaces a non-intrusive opt-in banner (`LocalAiModelPackState.isOptInBannerEligible`) whenever at least one enabled catalog tier fits the device.

Only **one** active `.litertlm` pack is stored on disk at a time under `files/ai_models/<pack_id>/<version>/model.litertlm`. Switching tiers or updating a version downloads and SHA-256-verifies the target pack to a `.part` temporary file first, atomically updates `LocalAiModelPackPreferences`, invalidates any cached `LiteRtEngineClient` instance for the previous path, and deletes superseded pack directories via `ModelPackStorageManager.deleteOtherModelFiles()`.

---

## 2. Device Capability → Recommended Model Matrix

`LocalAiDeviceCompatibilityProvider.evaluateCatalog()` evaluates every enabled pack (`is_enabled = true`) individually against device ABI, total RAM (`ActivityManager.MemoryInfo.totalMem`), and free internal storage (`StatFs(filesDir).availableBytes`).

### 2.1 Compatibility & Recommendation Decision Table

| CPU ABI | Total RAM | Free Internal Storage | Recommended Pack | Other Tiers Status |
|---|---|---|---|---|
| Missing `arm64-v8a` (e.g., `x86_64`, `armeabi-v7a`) | Any | Any | None (`Incompatible`) | All tiers marked `UNSUPPORTED_ABI` |
| `arm64-v8a` | `< 2048 MB` | Any | None (`Incompatible`) | All tiers marked `INSUFFICIENT_RAM` (aggregate threshold reports `2048 MB`) |
| `arm64-v8a` | `≥ 2048 MB` | `< 1 GiB` (`1,073,741,824 B`) | None (`Incompatible`) | All tiers marked `INSUFFICIENT_STORAGE` (aggregate threshold reports lowest-tier storage) |
| `arm64-v8a` | `2048–3071 MB` | `≥ 1 GiB` | **Compact** (`qwen3-0.6b-litert-v1`) | Balanced & High Quality marked `INSUFFICIENT_RAM` |
| `arm64-v8a` | `3072–6143 MB` | `1–3 GiB` | **Compact** (`qwen3-0.6b-litert-v1`) | Balanced & High Quality marked `INSUFFICIENT_STORAGE` / `INSUFFICIENT_RAM` |
| `arm64-v8a` | `3072–6143 MB` | `≥ 3 GiB` (`3,221,225,472 B`) | **Balanced** (`qwen2.5-1.5b-litert-v1`) | Compact switchable; High Quality marked `INSUFFICIENT_RAM` |
| `arm64-v8a` | `≥ 6144 MB` | `3–5 GiB` | **Balanced** (`qwen2.5-1.5b-litert-v1`) | Compact switchable; High Quality marked `INSUFFICIENT_STORAGE` |
| `arm64-v8a` | `≥ 6144 MB` | `≥ 5 GiB` (`5,368,709,120 B`) | **High Quality** (`gemma4-e2b-litert-v1`) | Balanced & Compact switchable |

### 2.2 Ranking, Tie-Breaking & Storage Headroom Rules

- **Display Order:** `priority ASC` → `tier.rank ASC` → `pack_id ASC` (Compact → Balanced → High Quality).
- **Recommended Pack Selection:** Among compatible enabled packs, `LocalAiDeviceCompatibilityProvider` picks the maximum by:
  1. `priority DESC`
  2. `tier.rank DESC` (`HIGH_QUALITY=3`, `BALANCED=2`, `COMPACT=1`)
  3. `min_ram_mb DESC`
  4. `pack_id ASC` (lexicographically smallest tiebreaker)
- **Storage Headroom Invariant:** `requiredStorageBytes = maxOf(minFreeStorageBytes, sizeBytes * 2)` so devices always have room to stream `model.litertlm.part` alongside any existing model before atomic promotion and cleanup.
- **Installed Pack Re-Evaluation:** When evaluating catalog display/compatibility, `requireStorageCheck = false` for the currently installed `pack_id` (since its bytes are already on disk), while `requestDownload()` always enforces `requireStorageCheck = true` before streaming a new download or tier switch.

---

## 3. Production Catalog Roster & Source Traceability

All three production tiers are Apache 2.0, ungated official `litert-community` `.litertlm` artifacts served from Cloudflare R2 (`splittrip-ai-models`) at `https://models.splittrip.eu/`:

| Tier (`tier`) | `pack_id` | Display Name | Hugging Face Repo & Artifact | License | `size_bytes` | `sha256_checksum` | `min_ram_mb` | `min_free_storage_bytes` | `priority` |
|---|---|---|---|---|---:|---|---:|---:|---:|
| `compact` | `qwen3-0.6b-litert-v1` | `Compact (Qwen 3 0.6B)` | [`litert-community/Qwen3-0.6B`](https://huggingface.co/litert-community/Qwen3-0.6B) → `Qwen3-0.6B_dynamic_wi4b32_afp32.litertlm` | Apache 2.0 | `343490560` (~328 MiB) | `bf1aba9ce8d963124407353c6aa4cbf72eba78db21ae133de983eac672ce83e0` | `2048` | `1073741824` (1 GiB) | `10` |
| `balanced` | `qwen2.5-1.5b-litert-v1` | `Balanced (Qwen 2.5 1.5B)` | [`litert-community/Qwen2.5-1.5B-Instruct`](https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct) → `Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv4096.litertlm` | Apache 2.0 | `1597922856` (~1.49 GiB) | `c4a9b7289365ab80fe599b844b77ad253eb3c53fb0e5d963d699146a6f01255e` | `3072` | `3221225472` (3 GiB) | `20` |
| `high_quality` | `gemma4-e2b-litert-v1` | `High Quality (Gemma 4 E2B)` | [`litert-community/gemma-4-E2B-it-litert-lm`](https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm) → `gemma-4-E2B-it.litertlm` | Apache 2.0 | `2588147712` (~2.41 GiB) | `181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c` | `6144` | `5368709120` (5 GiB) | `30` |

**R2 Free-Tier Budget:** `0.34 GB + 1.60 GB + 2.59 GB = 4.53 GB` out of R2's **10 GB/month** free storage tier (~5.47 GB headroom for rolling updates).

---

## 4. How to Update or Add a Model on Cloudflare R2 & Firebase Remote Config

### 4.1 Download & Verify Source Artifacts Locally

```bash
mkdir -p /tmp/splittrip-models
curl -L --fail --progress-bar \
  -o /tmp/splittrip-models/<artifact>.litertlm \
  "<source_download_url>"

stat -f%z /tmp/splittrip-models/<artifact>.litertlm   # macOS exact byte count
shasum -a 256 /tmp/splittrip-models/<artifact>.litertlm
```

### 4.2 Upload Immutable Versioned Filenames to Cloudflare R2

Never overwrite an existing filename in place (to prevent CDN edge-cache / SHA-256 mismatches):

```bash
npx wrangler r2 object put splittrip-ai-models/<new-immutable-filename>.litertlm \
  --file=/tmp/splittrip-models/<artifact>.litertlm \
  --content-type=application/octet-stream \
  --remote
```

Verify `HTTP/2 200`, matching `content-length`, and `HTTP/2 206` byte-range support:

```bash
curl -sI "https://models.splittrip.eu/<new-immutable-filename>.litertlm" | grep -iE "^HTTP/|content-length|accept-ranges"
curl -sI -r 0-0 "https://models.splittrip.eu/<new-immutable-filename>.litertlm" | grep -iE "^HTTP/|content-range"
```

### 4.3 Update Firebase Remote Config (`local_ai_model_pack_json`)

Update the `local_ai_model_pack_json` parameter (under `parameterGroups.Receipts`) in:

1. [`firebase/remoteconfig.template.json`](../../firebase/remoteconfig.template.json)
2. [`data/firebase/src/main/res/xml/remote_config_defaults.xml`](../../data/firebase/src/main/res/xml/remote_config_defaults.xml)
3. Live Firebase Remote Config (Firebase Console or `make remoteconfig-diff` + `make remoteconfig-deploy`)

Exact per-pack fields inside `packs[]`:

| Field | Description |
|---|---|
| `pack_id` | Stable pack identifier (used in `ai_models/<pack_id>/<version>/` storage layout). |
| `display_name` | Human-readable label shown in Settings (e.g., `"Balanced (Qwen 2.5 1.5B)"`). |
| `tier` | `"compact"`, `"balanced"`, or `"high_quality"` (`ModelPackTier`). |
| `priority` | Integer display/ranking priority (e.g., `10`, `20`, `30`). |
| `version` | SemVer string; bumping triggers `isUpdateAvailable` on clients running the old version of that `pack_id`. |
| `download_url` | `https://models.splittrip.eu/<new-immutable-filename>.litertlm` |
| `sha256_checksum` | Lowercase 64-char hex SHA-256 digest. |
| `size_bytes` | Exact byte count (`Long`). |
| `min_ram_mb` | Minimum device RAM in MiB (`Int`). |
| `min_free_storage_bytes` | Minimum free internal storage (`Long`); must satisfy `min_free_storage_bytes > size_bytes * 2`. |
| `is_enabled` | Set to `false` to disable a tier without deleting its entry. |

### 4.4 Validate & Clean Up Superseded Artifacts

```bash
make remoteconfig-validate
make remoteconfig-diff
```

After rollout stabilizes, delete superseded `.litertlm` objects from `splittrip-ai-models` in Cloudflare R2 to preserve free-tier storage headroom.

See also: [Local AI Model Pack Hosting & Update Runbook (Cloudflare R2)](local-ai-model-pack-hosting.md).
