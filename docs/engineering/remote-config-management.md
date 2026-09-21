# Firebase Remote Config Management & Deployment

SplitTrip manages Firebase Remote Config declaratively using a version-controlled template located at [`firebase/remoteconfig.template.json`](../../firebase/remoteconfig.template.json).

This operational guide details the architecture, parameter catalog, verification gates, CLI tooling, deployment lifecycle, and rollback procedures.

---

## 🏛️ 1. Architecture & Core Concepts

```
┌────────────────────────────────────────────────────────────────┐
│                   Version-Controlled Template                  │
│                firebase/remoteconfig.template.json             │
└────────────────┬───────────────────────────────┬───────────────┘
                 │ (Automated Sync Gate)         │ (CLI / CI Deployment)
                 ▼                               ▼
┌────────────────────────────────┐    ┌──────────────────────────┐
│      In-App Client Defaults    │    │    Firebase Remote Config│
│ remote_config_defaults.xml     │    │       Cloud Service      │
│  + Kotlin Flow Repositories    │    │   (parameterGroups 📁)   │
└────────────────────────────────┘    └──────────────────────────┘
```

### Declarative Single Source of Truth
Rather than manually editing key-value pairs in the Firebase Web Console, all parameter schemas, types, default values, documentation descriptions, and group hierarchies are declared in [`firebase/remoteconfig.template.json`](../../firebase/remoteconfig.template.json).

### Parameter Groups & Console Folders
In Firebase Remote Config, `parameterGroups` organizes parameters into logical, collapsible folder groups inside the Firebase Console.
- **Console Presentation:** Each group renders as a folder with an icon, title, and description. Inside each folder, all contained parameters display with their individual name, description, value type badge (`BOOLEAN`, `NUMBER`, `STRING`, `JSON`), and default value.
- **Client Backwards-Compatibility:** On the Android client SDK side, keys remain globally queryable flatly by name (e.g. `getString("subscription_gating_enabled")`), ensuring complete backwards compatibility with zero runtime code changes.

### Zero-Drift Verification Gate
Any discrepancy between in-app client defaults ([`data/firebase/src/main/res/xml/remote_config_defaults.xml`](../../data/firebase/src/main/res/xml/remote_config_defaults.xml)) and the declarative template is automatically caught by:
1. Automated Python consistency script (`scripts/validate_remote_config.py` / `make remoteconfig-validate`).
2. JUnit 5 unit test (`RemoteConfigTemplateConsistencyTest`) executed during `./gradlew test`, `make fast-check`, and `make check`.

---

## 📋 2. Parameter Catalog

SplitTrip parameters are organized into 5 functional groups:

### Group 1: Subscriptions
*Operational killswitch and subscription tier quotas for Free and Pro users.*

| Parameter Key | Value Type | Default Value | Description |
|---|---|---|---|
| `subscription_gating_enabled` | `BOOLEAN` | `true` | Master operational killswitch to enforce or bypass feature gating and tier quotas. |
| `max_owned_groups_free` | `NUMBER` | `1` | Maximum number of groups a Free tier user can own concurrently. |
| `max_owned_groups_pro` | `NUMBER` | `100` | Maximum number of groups a Pro tier user can own concurrently. |
| `max_members_per_group_free` | `NUMBER` | `4` | Maximum member cap for groups owned by a Free user. |
| `max_members_per_group_pro` | `NUMBER` | `20` | Maximum member cap for groups owned by a Pro user. |
| `ai_receipt_monthly_limit_free` | `NUMBER` | `0` | Monthly quota of AI receipt scans allocated to Free users. |
| `ai_receipt_monthly_limit_pro` | `NUMBER` | `100` | Monthly quota of AI receipt scans allocated to Pro users. |

### Group 2: Groups
*General group limits, currency fallback defaults, and balance calculation debounce timers.*

| Parameter Key | Value Type | Default Value | Description |
|---|---|---|---|
| `default_currency_code` | `STRING` | `EUR` | Default ISO 4217 currency code applied when creating new groups without an explicit currency. |
| `balance_computation_debounce_ms` | `NUMBER` | `300` | Debounce duration in milliseconds before recalculating member balances following mutation events. |
| `max_members_per_group` | `NUMBER` | `20` | Fallback maximum member cap per group when tier gating is inactive. |

### Group 3: Receipts
*Thresholds and keyword blacklists for OCR receipt parsing and validation.*

| Parameter Key | Value Type | Default Value | Description |
|---|---|---|---|
| `extracted_date_max_future_days` | `NUMBER` | `30` | Maximum allowed days in the future for an OCR-extracted receipt date before being discarded. |
| `ocr_safety_false_positives_blacklist` | `STRING` | `razor,private,toothbrushes` | Comma-separated list of blacklisted terms for OCR false-positive filtering. |

### Group 4: Settlements
*Rate limiting and pacing thresholds for settlement reminders and nudges.*

| Parameter Key | Value Type | Default Value | Description |
|---|---|---|---|
| `settlement_nudge_rate_limit_hours` | `NUMBER` | `24` | Minimum hours that must elapse between sending settlement reminders to the same debtor. |

### Group 5: General
*Public support contact channels and developer profile attribution metadata.*

| Parameter Key | Value Type | Default Value | Description |
|---|---|---|---|
| `support_email_address` | `STRING` | `support@splittrip.eu` | Support email address displayed in-app for user inquiries. |
| `developer_info_json` | `JSON` | `{"name":"Andrés Pedraza Míguez",...}` | Serialized JSON containing developer profile, localized roles, bios, credits, and links. |

### Group 6: Advertising
*AdMob configuration, ad unit IDs, and interstitial display pacing for Free tier users.*

| Parameter Key | Value Type | Default Value | Description |
|---|---|---|---|
| `ads_enabled` | `BOOLEAN` | `true` | Master operational killswitch to enable or disable ads across the app. |
| `admob_banner_ad_unit_id` | `STRING` | `ca-app-pub-9638507020441461/2775424807` | Production AdMob banner ad unit ID for anchored adaptive banners. |
| `admob_interstitial_ad_unit_id` | `STRING` | `ca-app-pub-9638507020441461/6643262487` | Production AdMob interstitial ad unit ID for full-screen interstitial ads. |
| `ad_interstitial_action_frequency` | `NUMBER` | `3` | Number of completed transactional actions before displaying an interstitial ad. |
| `ad_interstitial_min_interval_seconds` | `NUMBER` | `180` | Minimum cooldown duration in seconds between interstitial ad impressions. |

---

## 🛠️ 3. Operational CLI & Makefile Tooling

SplitTrip provides a dedicated CLI tool at [`scripts/deploy-remote-config.sh`](../../scripts/deploy-remote-config.sh) and convenient Makefile targets.

### Available Targets & Commands

| Target / Command | Description |
|---|---|
| `make remoteconfig-validate` | Runs `scripts/validate_remote_config.py` to check parity between XML and template. |
| `make remoteconfig-diff` | Fetches live cloud configuration, strips metadata, and outputs a colorized diff. |
| `make remoteconfig-deploy` | Validates consistency and deploys the template via `npx firebase-tools`. |
| `make remoteconfig-rollback VERSION=<v>` | Rolls back the cloud configuration to the specified version number. |
| `./scripts/deploy-remote-config.sh versions` | Lists recent published template versions, authors, and timestamps. |
| `./scripts/deploy-remote-config.sh get [file]` | Fetches the live template JSON to stdout or to a specified file. |

### Example: Inspecting Live Changes (Diff)
Before deploying or after editing local parameters, check pending differences against Firebase:
```bash
make remoteconfig-diff
```

### Example: Deploying Remote Config
To validate and deploy directly from your local terminal:
```bash
make remoteconfig-deploy
```

---

## 🔄 4. Adding or Modifying Parameters (Workflow)

When adding a new parameter or changing an existing default, follow these steps to maintain consistency:

### Step 1: Update In-App XML Defaults
Add or update the `<entry>` in [`data/firebase/src/main/res/xml/remote_config_defaults.xml`](../../data/firebase/src/main/res/xml/remote_config_defaults.xml):
```xml
<entry>
    <key>my_new_feature_enabled</key>
    <value>false</value>
</entry>
```

### Step 2: Update Client Repository & Constants
In `FirebaseAppConfigRepository.kt`, declare the default constant, StateFlow property, and update flow parsing logic.

### Step 3: Update Declarative Template
In [`firebase/remoteconfig.template.json`](../../firebase/remoteconfig.template.json), add the parameter under the appropriate group:
```json
"my_new_feature_enabled": {
  "defaultValue": {
    "value": "false"
  },
  "description": "Enables or disables the new feature.",
  "valueType": "BOOLEAN"
}
```

### Step 4: Run Consistency Check
```bash
make remoteconfig-validate
```

### Step 5: Verify Quality Gates
Run `make fast-check` locally to execute the unit tests and ensure zero static analysis findings.

---

## 🚀 5. CI/CD Deployment Automation

Remote Config deployment is automated via GitHub Actions in [`.github/workflows/deploy-firebase.yml`](../../.github/workflows/deploy-firebase.yml).

- **Trigger:** Any push or merge to the `main` branch touching:
  - `firebase/remoteconfig.template.json`
  - `firebase.json`
  - `firestore.rules`
  - `firestore.indexes.json`
  - `functions/**`
- **Execution:** The workflow authenticates using Google Cloud Service Account credentials (`FIREBASE_SERVICE_ACCOUNT_JSON`) and executes:
  ```bash
  npx firebase-tools deploy --only functions,firestore,remoteconfig --force
  ```

---

## ⏪ 6. Rollback Procedures

If an erroneous or broken configuration is deployed to production:

1. **List Recent Versions:**
   ```bash
   ./scripts/deploy-remote-config.sh versions
   ```
   Note the `Version Number` you want to restore (e.g. `17`).

2. **Execute Rollback:**
   ```bash
   make remoteconfig-rollback VERSION=17
   ```
   *Or using the script directly:*
   ```bash
   ./scripts/deploy-remote-config.sh rollback 17
   ```

3. **Synchronize Local Template:**
   After rolling back the cloud instance, fetch the restored template to keep local Git in sync:
   ```bash
   ./scripts/deploy-remote-config.sh get firebase/remoteconfig.template.json
   ```
   Then commit the rollback in Git following the standard PR workflow.
