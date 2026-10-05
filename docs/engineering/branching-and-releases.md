# Workflow & CI/CD

This project follows a strict **GitFlow** workflow fully automated via GitHub Actions. We prioritize branch protection, ensuring that no direct pushes are ever made to `develop` or `main`.

## 🚀 The Golden Rule

**Never modify `version.properties` manually.** The CI/CD pipelines manage version numbers based on the branch names and PR labels.

---

## ♻️ The Development Lifecycle

### 1. Start with an Issue

* Open a **GitHub Issue** describing the Feature, Bug, or Release.
* **Automation:** The bot creates a branch for you using the format `type/SPLTRP-xxxx`.
* **Auto-PR:** For **Releases**, the bot *automatically* opens the Pull Request to `main`. For other types, you open the PR manually when ready.

| Issue Label | Branch Created | Base Branch | Target PR Branch |
| --- | --- | --- | --- |
| `feature` | `feature/SPLTRP-xxxx` | `develop` | `develop` |
| `bug` | `bugfix/SPLTRP-xxxx` | `develop` | `develop` |
| `enhancement` | `refactor/SPLTRP-xxxx` | `develop` | `develop` |
| `config` | `internal/SPLTRP-xxxx` | `develop` | `develop` |
| `release` | `release/SPLTRP-xxxx` | `develop` | **`main`** (Automatic) |
| `hotfix` | `hotfix/SPLTRP-xxxx` | `main` | **`main`** |

### 2. Standard Development (Features/Bugs/Refactors)

* Checkout the bot-created branch.
* Work and push commits.
* Open a PR to `develop` manually when ready.

---

## 📦 The Release Process (Step-by-Step)

We treat releases as a specific type of issue.

### Phase 1: Preparation

1. Create an Issue titled **"Release v0.12.0"** (or similar) with label **`release`**.
2. **Bot Action:**
   * Creates branch `release/SPLTRP-xxxx` from `develop`.
   * **Automatically opens a PR** from this branch to `main`.
3. **Bot Action (in PR):**
   * The *Prepare Version* workflow runs on the `release/` branch (which is unprotected).
   * It removes `-SNAPSHOT` from `version.properties`.
   * It applies a **Major** version bump if the `major` label is detected on the PR.
   * It commits this change to the PR.

### Phase 2: Publishing

1. Review the PR (ensure all CI quality checks and tests pass).
2. **Merge** the PR into `main`.
3. **Bot Action:** Merging into `main` automatically triggers two parallel release publishing workflows:
   * **GitHub Release (`create-app-release.yml`):**
     * Builds the signed APK.
     * Publishes a GitHub Release with an automated Changelog based on merged PRs.
   * **Google Play Store Publishing (`deploy-play-store.yml`):**
     * Builds and signs the release `.aab` (Android App Bundle).
     * Strictly verifies the release signing certificate (ensures non-debug signature and valid keystore owner).
     * Extracts recent PR summaries and generates localized release notes (`whatsnew-es-ES`, `whatsnew-en-US`).
     * Uploads the `.aab` bundle and ProGuard/R8 deobfuscation mapping (`mapping.txt`) to the target Google Play track (defaults to `internal` / *Prueba interna*).

### Phase 3: Sync Back (Backmerge)

Once `main` is updated, we must update `develop` with the new version tag and prepare for the next cycle.

1. **Bot Action:** The *Sync* workflow triggers automatically.
   * Creates a temporary branch `sync/main-to-develop-xxx`.
   * Merges `main` into it.
   * Bumps the version (e.g., `0.12.0` -> `0.13.0-SNAPSHOT`).
   * **Opens a PR** to `develop`.
2. **Manual Action:** You review and merge this "Sync" PR to update `develop`.

---

## 🚀 Google Play Store Publishing & Promotion Strategy

SplitTrip uses automated publishing to Google Play Store via GitHub Actions and the Google Play Developer API.

### Target Tracks & Operational Flow

| Track | Play Console Name | Typical Usage | Google Review Queue |
|---|---|---|---|
| `internal` | **Prueba interna** | Routine releases & automated CI merges | **Instant** (~5 min rollout, no review gate) |
| `alpha` | **Prueba cerrada** | Closed testing groups (14-day policy verification) | Subject to review |
| `beta` | **Prueba abierta** | Open beta testing (if configured) | Subject to review |
| `production` | **Producción** | Live public store listing for all users | Subject to review |

### The "Promote, Don't Rebuild" Principle

For maximum reliability and binary parity:
1. **Automated Merges land in `internal`:** When a release or hotfix PR is merged into `main`, `.github/workflows/deploy-play-store.yml` deploys directly to the **`internal`** track (*Prueba interna*).
2. **Device Verification:** Registered internal testers receive the update instantly on their devices without waiting for Google review.
3. **Promote in Google Play Console:** Once validated:
   * Open the [Google Play Console](https://play.google.com/console).
   * Navigate to **Release → Testing → Internal testing** (*Prueba interna*).
   * Under the active release, click **Promote release** (*Promocionar versión*) ➔ **Production** (*Producción*).
   * Review release notes and rollout percentage, then submit for review.
4. **Why:** This guarantees **100% byte-for-byte binary parity** between the exact artifact verified by testers and what is served to public end users, avoiding risks of rebuild discrepancies.

### On-Demand Publishing (`workflow_dispatch`)

You can build and deploy to any track on demand directly from GitHub Actions:
1. Go to **Actions** ➔ **Deploy to Google Play Store**.
2. Click **Run workflow**.
3. Select the branch (usually `main`) and choose the target **Google Play Release Track** from the dropdown:
   * `internal`
   * `alpha`
   * `beta`
   * `production`
4. Click **Run workflow**. The pipeline will compile, sign, verify, and upload the `.aab` and deobfuscation mappings to the chosen track.

---

## 🔑 Required Repository Secrets & Service Account Setup

Automated deployment requires credentials configured in GitHub Secrets (**Settings → Secrets and variables → Actions → Repository secrets**).

### Secrets Catalog

| Secret Name | Description | Source |
|---|---|---|
| `PLAY_STORE_JSON_KEY` | Raw JSON credentials of the Google Play Developer API Service Account | Google Cloud IAM & Play Console |
| `RELEASE_KEYSTORE_BASE64` | Base64-encoded release `.keystore` file | Local release keystore |
| `SIGNING_KEY_ALIAS` | Key alias in the release keystore | Keystore configuration |
| `SIGNING_KEY_PASSWORD` | Key password in the release keystore | Keystore configuration |
| `SIGNING_STORE_PASSWORD` | Store password for the release keystore | Keystore configuration |
| `GOOGLE_SERVICES_JSON_BASE64` | Base64-encoded `google-services.json` | Firebase Console |
| `OER_APP_ID_RELEASE` | Open Exchange Rates API App ID for release builds | Open Exchange Rates account |
| `SPLTRP_ADMOB_APP_ID` | Production Google AdMob application ID | Google AdMob Console |
| `GRADLE_ENCRYPTION_KEY` | Encryption key for Gradle build cache | Gradle Actions config |

### Setting Up the Google Play Service Account (`PLAY_STORE_JSON_KEY`)

1. **Enable API in Google Cloud Console:**
   * Go to the [Google Cloud Console](https://console.cloud.google.com/) and select the project linked to the app (e.g. `expshapp`).
   * Navigate to **APIs & Services → Library**, search for **Google Play Android Developer API**, and click **Enable**.
2. **Create Service Account:**
   * Navigate to **IAM & Admin → Service Accounts**.
   * Click **Create Service Account** with name `play-store-publisher`.
   * Click **Create and Continue**, then **Done** (no Cloud IAM roles required; permissions are assigned in Google Play Console).
   * Open the newly created service account, go to the **Keys** tab, click **Add Key → Create new key → JSON**, and download the key file.
3. **Grant Permissions in Google Play Console:**
   * Go to [Google Play Console](https://play.google.com/console) ➔ **Developer settings → API access**.
   * Link the Google Cloud project if not already linked.
   * Under **Service accounts**, find `play-store-publisher@...` and click **Manage permissions** (or **Invite user**).
   * Under **App permissions**, add `es.pedrazamiguez.splittrip` and grant:
     * ✅ **Release to testing tracks** (*Gestionar versiones y canales de prueba*)
     * ✅ **Release to production** (*Gestionar producción*)
     * ✅ **View app information** (*Ver información de la aplicación*)
   * Click **Apply** and **Save changes**.
4. **Configure GitHub Secret:**
   * In GitHub, navigate to **Settings → Secrets and variables → Actions**.
   * Add a new repository secret named `PLAY_STORE_JSON_KEY`.
   * Paste the entire content of the downloaded `.json` key file.

---

## 🚑 Hotfix Process

1. Create an Issue with label **`hotfix`**.
2. **Bot Action:** Creates `hotfix/` branch from `main` (No Auto-PR is created).
3. Fix the bug and push to that branch.
4. **Manual Action:** Open a PR to `main`.
5. **Bot Action (in PR):** Automatically increments the **Patch** version (e.g., `0.12.0` -> `0.12.1`) and removes the snapshot flag.
6. Merge to `main` -> Triggers GitHub Release and Google Play Store deployment -> Triggers Sync PR to `develop`.

---

## 🛡️ Branch Protection

* **`main`**: Protected. No direct pushes.
* **`develop`**: Protected. No direct pushes.
* **`release/*`, `hotfix/*`, `feature/*`, `refactor/*`, `internal/*`**: Unprotected (Bots can write here).
