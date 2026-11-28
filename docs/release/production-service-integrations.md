# Production Service Integrations & Configuration Guide

> [!IMPORTANT]
> **Security & Privacy First:** This guide provides step-by-step setup instructions for external production services. It strictly avoids hardcoded secrets, private tokens, or private credentials. All values use public addresses or generic placeholders (`<PLACEHOLDER>`).

---

## Overview

SplitTrip utilizes the custom domain `splittrip.eu` across multiple services:
1. **GitHub Pages:** Hosts the public landing page (`web/index.html`), Privacy Policy (`web/privacy-policy.html`), and Digital Asset Links (`web/.well-known/assetlinks.json`) via GitHub Actions deployment.
2. **DNS & Email Forwarding:** Routes apex and subdomain traffic to GitHub Pages, and forwards inbound support emails (`support@splittrip.eu`) to the developer's personal inbox.
3. **Firebase Authentication:** Authorizes `splittrip.eu` for OAuth redirect handlers and brands transactional auth emails (password resets and email verification).
4. **Google Play Console:** Maps the official website, support email, privacy policy URL, and App Signing SHA-256 fingerprints for Android App Links.
5. **GCP Cloud Billing Budgets & Automated Kill-Switch:** Enforces staged `€20/mo` soft email alerts and an automated `€50/mo` Pub/Sub emergency kill-switch (`onBillingBudgetAlert`) that unlinks billing on runaway Blaze spend.

---

## 1. Domain & DNS Configuration (Registrar / DNS Provider)

### 1.1 GitHub Domain Verification (Domain Lock) — *Completed*
To prevent domain takeover and prove ownership:
1. In your personal GitHub account, navigate to **Settings → Pages → Verified domains**.
2. Add `splittrip.eu`.
3. Add the generated `TXT` challenge record in your DNS provider:
   - **Type:** `TXT`
   - **Host:** `_github-pages-challenge-<username>.splittrip.eu`
   - **Value:** `<VERIFICATION_CODE>`
4. Click **Verify** in GitHub. The domain is now locked to your account.

### 1.2 Web Traffic DNS Records

Configure the following records in your DNS provider (e.g., Cloudflare, Namecheap, Google Domains / Squarespace, GoDaddy):

| Host / Name | Type | Value / Destination | TTL | Purpose |
|---|---|---|---|---|
| `@` | `A` | `185.199.108.153` | 300 / Auto | GitHub Pages Apex IP 1 |
| `@` | `A` | `185.199.109.153` | 300 / Auto | GitHub Pages Apex IP 2 |
| `@` | `A` | `185.199.110.153` | 300 / Auto | GitHub Pages Apex IP 3 |
| `@` | `A` | `185.199.111.153` | 300 / Auto | GitHub Pages Apex IP 4 |
| `@` *(Optional)* | `AAAA` | `2606:50c0:8000::153` | 300 / Auto | GitHub Pages IPv6 1 |
| `@` *(Optional)* | `AAAA` | `2606:50c0:8001::153` | 300 / Auto | GitHub Pages IPv6 2 |
| `@` *(Optional)* | `AAAA` | `2606:50c0:8002::153` | 300 / Auto | GitHub Pages IPv6 3 |
| `@` *(Optional)* | `AAAA` | `2606:50c0:8003::153` | 300 / Auto | GitHub Pages IPv6 4 |
| `www` | `CNAME` | `pedrazamiguez.github.io.` | 300 / Auto | Subdomain redirect to GitHub Pages |

### 1.3 Inbound Email Routing (`support@splittrip.eu`)

SplitTrip does not require a paid email inbox provider. Free inbound email forwarding routes incoming support inquiries directly to your personal mailbox:

1. **Option A — IONOS Email Forwarding (Direct & Native for splittrip.eu):**
   - In the IONOS dashboard, navigate to **Correo** (Email) in the main navigation.
   - Click **Crear dirección de email** (Create email address).
   - Select **Reenvío** / **Redirección de email** (Email forwarding).
   - Set the email address to `support` @ `splittrip.eu`.
   - Set the forwarding destination to your personal email inbox (e.g., Gmail, Outlook).
   - Click **Guardar** (Save).
   - *Note:* The IONOS MX records (`mx00.ionos.es`, `mx01.ionos.es`) and SPF/DKIM records are already configured in your DNS zone by default, so no DNS edits are needed.

2. **Option B — Cloudflare Email Routing:**
   - In Cloudflare Dashboard, go to **Email → Email Routing**.
   - Enable Email Routing. Cloudflare will automatically add the required `MX` and `TXT` (SPF) records.
   - Add a custom address rule: `support@splittrip.eu` &rarr; `<your-personal-email@example.com>`.

3. **Option C — ForwardEmail.net:**
   - Add MX records: `mx1.forwardemail.net` (Priority 10), `mx2.forwardemail.net` (Priority 10).
   - Add TXT record: `forward-email=support@splittrip.eu:<your-personal-email@example.com>`.

---

## 2. GitHub Pages Configuration (Repository Settings & Deployment Workflow)

1. Navigate to the GitHub repository: **Settings → Pages**.
2. Under **Build and deployment**:
   - **Source:** Select **GitHub Actions** (switches repository build type from legacy branch deployment to automated workflow deployment).
   - Alternatively, configure this via GitHub CLI:
     ```bash
     gh api --method PUT repos/pedrazamiguez/split-trip/pages -f build_type=workflow
     ```
3. **Automated Deployment Workflow (`.github/workflows/deploy-pages.yml`):**
   - Automatically builds and publishes the `web/` directory when changes to `web/**` or the workflow itself are pushed to `main`.
   - Uses official GitHub actions: `actions/configure-pages@v5`, `actions/upload-pages-artifact@v3` (pointing to `web/`), and `actions/deploy-pages@v4`.
   - Can also be triggered manually on demand via `workflow_dispatch`.
4. Under **Custom domain**:
   - Verify `splittrip.eu` is active (persisted by `web/CNAME`).
   - GitHub validates DNS apex `A` records and Let's Encrypt TLS certificate.
5. Under **Enforce HTTPS**:
   - Ensure **Enforce HTTPS** is checked to redirect all HTTP traffic to HTTPS.

---

## 3. Firebase Authentication Configuration (Firebase Console)

1. Open the [Firebase Console](https://console.firebase.google.com/) and select the SplitTrip project.
2. Under **Authentication → Settings → Authorized domains**:
   - Click **Add domain**.
   - Add `splittrip.eu`.
   - Add `www.splittrip.eu`.
   - *(Ensures OAuth handlers and email action links are permitted from the custom domain).*
3. Under **Authentication → Templates**:
   - Select **Password reset**, click the edit (pencil) icon:
     - **Sender name:** `SplitTrip`
     - **Reply-to:** `support@splittrip.eu`
     - Click **Save**.
   - Select **Email address verification**, click the edit icon:
     - **Sender name:** `SplitTrip`
     - **Reply-to:** `support@splittrip.eu`
     - Click **Save**.
   - Select **Email change notice**, click the edit icon:
     - **Sender name:** `SplitTrip`
     - **Reply-to:** `support@splittrip.eu`
     - Click **Save**.

---

## 4. Google Play Console Listing Configuration

1. Open the [Google Play Console](https://play.google.com/console) and select SplitTrip (`es.pedrazamiguez.splittrip`).
2. Under **Store presence → Store settings**:
   - **Website:** `https://splittrip.eu`
   - **Email address:** `support@splittrip.eu`
   - Click **Save**.
3. Under **Policy and programs → App content → Privacy policy**:
   - **Privacy policy URL:** `https://splittrip.eu/privacy-policy.html`
   - Click **Save**.
4. Under **Release → Setup → App Signing** (or **App integrity**):
   - Locate the **App signing key certificate** card.
   - Copy both the **SHA-1** and **SHA-256** certificate fingerprints (`0D:E0:AE:D1:74:2A:59:FF:A5:2B:20:0F:EF:65:E9:C5:57:6B:79:2F:6D:35:CB:36:6C:20:14:08:5A:DA:EA:DB`).
   - In `web/.well-known/assetlinks.json`, the Play App Signing SHA-256 is configured for Android App Links verification.
   - Add both the **Play App Signing SHA-1** and **SHA-256** fingerprints to **Firebase Console → Project settings → Your apps → SplitTrip (Android)** so that Google Sign-In and App Check work in production Play Store builds.
5. Under **Release → Testing → Internal testing**:
   - To build the production-signed `.aab` (Android App Bundle), trigger the on-demand [`.github/workflows/build-release-bundle.yml`](../../.github/workflows/build-release-bundle.yml) workflow in GitHub Actions (`workflow_dispatch`).
   - The workflow compiles `bundleRelease`, verifies the production upload certificate with `keytool`, and exports the bundle and ProGuard `mapping.txt` as a downloadable artifact.
   - Download the artifact and upload `app-release.aab` to create the initial internal testing release.

---

## 5. GCP Cloud Billing Budgets & Automated Kill-Switch

SplitTrip runs on the Firebase **Blaze** (pay-as-you-go) plan. Because GCP Cloud Billing budget alerts are purely informational by default, SplitTrip pairs staged email warnings with an automated Pub/Sub kill-switch Cloud Function (`onBillingBudgetAlert` in `functions/src/triggers/onBillingBudgetAlert.ts`).

> [!IMPORTANT]
> **Placeholder Policy:** Never commit real project IDs, billing account IDs, or service account keys to git. Always substitute `<PROJECT_ID>`, `<BILLING_ACCOUNT_ID>`, and `<DEPLOYER_SA_EMAIL>` locally when running commands below.

### 5.1 Required GCP APIs & Pub/Sub Topic

Enable the required GCP APIs and create the dedicated Pub/Sub topic **before** deploying `onBillingBudgetAlert`:

```bash
gcloud auth login
gcloud config set project <PROJECT_ID>

gcloud services enable \
  cloudbilling.googleapis.com \
  billingbudgets.googleapis.com \
  pubsub.googleapis.com \
  eventarc.googleapis.com \
  --project=<PROJECT_ID>

gcloud pubsub topics create billing-budget-alerts --project=<PROJECT_ID>
```

### 5.2 Least-Privilege IAM Binding

`onBillingBudgetAlert` runs as a dedicated service account (`billing-kill-switch@<PROJECT_ID>.iam.gserviceaccount.com`) so that `roles/billing.projectManager` is isolated away from all other Cloud Functions:

```bash
# 1. Create dedicated runtime service account
gcloud iam service-accounts create billing-kill-switch \
  --display-name="SplitTrip Billing Kill-Switch" \
  --project=<PROJECT_ID>

# 2. Grant project-level billing management + metadata read (resourcemanager.projects.get
#    is required by projects.getBillingInfo; roles/browser grants metadata without data access)
gcloud projects add-iam-policy-binding <PROJECT_ID> \
  --member="serviceAccount:billing-kill-switch@<PROJECT_ID>.iam.gserviceaccount.com" \
  --role="roles/billing.projectManager"

gcloud projects add-iam-policy-binding <PROJECT_ID> \
  --member="serviceAccount:billing-kill-switch@<PROJECT_ID>.iam.gserviceaccount.com" \
  --role="roles/browser"

# 3. Allow the GitHub Actions CI deployer SA to deploy functions running as billing-kill-switch
#    and read Pub/Sub topic metadata
gcloud iam service-accounts add-iam-policy-binding \
  billing-kill-switch@<PROJECT_ID>.iam.gserviceaccount.com \
  --member="serviceAccount:<DEPLOYER_SA_EMAIL>" \
  --role="roles/iam.serviceAccountUser" \
  --project=<PROJECT_ID>

gcloud pubsub topics add-iam-policy-binding billing-budget-alerts \
  --member="serviceAccount:<DEPLOYER_SA_EMAIL>" \
  --role="roles/pubsub.viewer" \
  --project=<PROJECT_ID>

# 4. Allow Eventarc / Pub/Sub push delivery (authenticating as billing-kill-switch) to invoke
#    the Gen-2 Cloud Run service. Run scoped to the service after first deploy (or grant
#    roles/run.invoker at project scope before first deploy):
gcloud run services add-iam-policy-binding onbillingbudgetalert \
  --region=europe-west1 \
  --project=<PROJECT_ID> \
  --member="serviceAccount:billing-kill-switch@<PROJECT_ID>.iam.gserviceaccount.com" \
  --role="roles/run.invoker"
```

> [!NOTE]
> **Billing-Account-Level Permission Required for Unlinking:** `projects.updateProjectBillingInfo` requires permission on the target Cloud Billing account as well as the project. Under **GCP Console → Billing → Account Management → `+ Add principal`** (right-hand permissions panel), add `billing-kill-switch@<PROJECT_ID>.iam.gserviceaccount.com` with **Billing Account Administrator** (`roles/billing.admin`)—or a custom org role containing `billing.resourceAssociations.delete` if your organization supports custom billing roles.

### 5.3 Two-Budget Configuration (GCP Console)

Configure two **separate** budgets under **Google Cloud Console → Billing → Budgets & alerts → Create budget**:

1. **Budget 1 — Soft Warning Budget (Email Notifications Only):**
   - **Name:** `SplitTrip Monthly Soft Warnings`
   - **Time range:** Monthly (calendar)
   - **Scope:** Select `<PROJECT_ID>` only (all services)
   - **Target amount:** Specified amount: `€20`
   - **Threshold rules:** `50%` (Actual), `80%` (Actual), `100%` (Actual), `100%` (Forecasted)
   - **Actions:** Check **Email alerts to billing admins and users**. Do **not** connect to a Pub/Sub topic.
2. **Budget 2 — Emergency Kill-Switch Hard Cap (Connected to Pub/Sub):**
   - **Name:** `SplitTrip Emergency Hard Cap` *(must match `BILLING_EMERGENCY_BUDGET_NAME` in `functions/.env` character-for-character)*
   - **Time range:** Monthly (calendar)
   - **Scope:** Select `<PROJECT_ID>` only (all services)
   - **Target amount:** Specified amount: `€50`
   - **Threshold rules:** `100%` (**Actual only** — never attach kill-switch unlinking to Forecasted thresholds)
   - **Actions:**
     - Check **Email alerts to billing admins and users**
     - Check **Connect a Pub/Sub topic to this budget** → Project: `<PROJECT_ID>` → Topic: `billing-budget-alerts`

### 5.4 Dry-Run Verification & Arming Live Mode

Before enabling live unlinking, set `BILLING_KILL_SWITCH_ENABLED=true` and `BILLING_KILL_SWITCH_DRY_RUN=true` in `functions/.env` to validate Pub/Sub delivery and Cloud Billing API read permissions end-to-end without disrupting production:

1. **Test soft-warning isolation** (expects `SKIPPED_NON_EMERGENCY_BUDGET`):
   ```bash
   gcloud pubsub topics publish billing-budget-alerts \
     --project=<PROJECT_ID> \
     --message='{"budgetDisplayName":"SplitTrip Monthly Soft Warnings","costAmount":25,"budgetAmount":20,"currencyCode":"EUR","alertThresholdExceeded":1.0}'
   ```
2. **Test below-threshold / forecast-only skip** (expects `SKIPPED_BELOW_THRESHOLD`):
   ```bash
   gcloud pubsub topics publish billing-budget-alerts \
     --project=<PROJECT_ID> \
     --message='{"budgetDisplayName":"SplitTrip Emergency Hard Cap","costAmount":40,"budgetAmount":50,"currencyCode":"EUR","forecastThresholdExceeded":1.0}'
   ```
3. **Test breached-cap dry-run & IAM read check** (calls `isBillingEnabled` and logs `[DRY RUN]` with `DRY_RUN`, without unlinking):
   ```bash
   gcloud pubsub topics publish billing-budget-alerts \
     --project=<PROJECT_ID> \
     --message='{"budgetDisplayName":"SplitTrip Emergency Hard Cap","costAmount":55,"budgetAmount":50,"currencyCode":"EUR","alertThresholdExceeded":1.0}'
   ```
4. **Inspect logs** and confirm all three actions completed with zero `PERMISSION_DENIED` errors:
   ```bash
   gcloud functions logs read onBillingBudgetAlert --region=europe-west1 --gen2 --limit=25 --project=<PROJECT_ID>
   ```
5. **Arm live mode:** Set `BILLING_KILL_SWITCH_DRY_RUN=false` in `functions/.env`, redeploy, and verify the live runtime configuration:
   ```bash
   gcloud functions describe onBillingBudgetAlert --region=europe-west1 --gen2 --project=<PROJECT_ID> \
     --format="yaml(serviceConfig.environmentVariables,serviceConfig.serviceAccountEmail)"
   ```

### 5.5 Live Budget Adjustments, User Impact & Post-Incident Recovery

> [!WARNING]
> **Reporting Latency Caveat:** Cloud Billing budget evaluations typically lag real-time usage by **~15–60+ minutes** (occasionally longer for certain services). This kill-switch caps catastrophic multi-hour runaway loops; it does not provide sub-second circuit breaking.

#### Adjusting Budget Amounts Live
* **Amount changes require no code change or redeploy:** `onBillingBudgetAlert` evaluates `costAmount < budgetAmount` dynamically from each Pub/Sub payload. Raising `SplitTrip Emergency Hard Cap` (e.g., `€50` → `€100` as MAU and Net MRR grow) in GCP Console takes effect on subsequent budget evaluations without redeploying Cloud Functions.
* **Do NOT rename `SplitTrip Emergency Hard Cap`** in GCP Console unless you also update `BILLING_EMERGENCY_BUDGET_NAME` in `functions/.env`—mismatched names are ignored (`SKIPPED_NON_EMERGENCY_BUDGET`).

#### What Happens When the Kill-Switch Fires
* **Billing does NOT auto-relink next month:** Re-linking is strictly manual so a runaway bug cannot automatically resume on the 1st of the next calendar month.
* **User experience while unlinked:**
  * *Continues working (offline-first Room SSOT):* App launch, cached groups/balances/expenses, local offline expense recording, on-device receipt OCR, Google Play Billing, and AdMob.
  * *Stops working until re-linked:* Cloud Firestore multi-device sync, online group creation/invites, Cloud Functions (all push notification triggers, `syncExchangeRates`, `expenseReminders`), Cloud Storage receipt/cover uploads & downloads, and potentially Firebase Auth token refreshes/new sign-ins.

#### Post-Incident Recovery Runbook (Step-by-Step)

1. **Mitigate root cause first:** Inspect Cloud Logging, Firestore Usage, Cloud Storage Egress, and Cloud Functions invocation metrics. Fix the runaway loop or tighten security rules **before** re-enabling billing.
2. **Prevent an immediate same-month re-unlink:** Monthly spend on the billing account accumulates across the calendar month. If you are recovering **in the same calendar month** that triggered the cap, edit **`SplitTrip Emergency Hard Cap`** in **GCP Console → Billing → Budgets & alerts** and raise the target amount above your current month-to-date spend (e.g., `€50` → `€100`) **before** re-linking—otherwise GCP's next budget evaluation will still see `costAmount >= budgetAmount` and unlink billing again.
3. **Re-link the billing account manually:**
   * **Via `gcloud` CLI:**
     ```bash
     # List your billing account IDs if you don't have <BILLING_ACCOUNT_ID> handy
     gcloud billing accounts list

     # Re-link <PROJECT_ID> to your Blaze billing account
     gcloud billing projects link <PROJECT_ID> --billing-account=<BILLING_ACCOUNT_ID>

     # Confirm billingEnabled: true
     gcloud billing projects describe <PROJECT_ID>
     ```
   * **Via GCP Console:** Open **Billing → Account Management → My Projects** (or **Billing → Account Management → Linked projects**), locate `<PROJECT_ID>`, and select **Change billing** → choose your Blaze billing account.
4. **Verify & restore Firebase services:**
   * Confirm the **Blaze** plan is active in Firebase Console.
   * Test Firestore read/write, Firebase Auth, Cloud Storage, and Remote Config.
   * Verify Cloud Functions (`onBillingBudgetAlert`, `syncExchangeRates`, `expenseReminders`, and Firestore notification triggers) are active; if any Gen-2 Cloud Run service or Eventarc trigger stopped cleanly during unlink, redeploy with `make firebase` (or trigger `.github/workflows/deploy-firebase.yml` via `workflow_dispatch`).

---

## 6. Verification Checklist

- [ ] `dig splittrip.eu +short` returns the four GitHub Pages IPs (`185.199.108.153`, etc.).
- [ ] `curl -I https://splittrip.eu` returns `HTTP/2 200` with valid Let's Encrypt certificate.
- [ ] `curl -I https://splittrip.eu/privacy-policy.html` resolves successfully.
- [ ] `curl https://splittrip.eu/.well-known/assetlinks.json` returns valid JSON with `application/json` content-type.
- [ ] Send a test email from an external account to `support@splittrip.eu` and confirm receipt in the personal inbox.
- [ ] Send a test password reset email from the app or Firebase Console and verify the sender name displays `SplitTrip` and reply-to displays `support@splittrip.eu`.
- [ ] Confirm `billing-budget-alerts` Pub/Sub topic and `billing-kill-switch@<PROJECT_ID>.iam.gserviceaccount.com` SA exist with `roles/billing.projectManager` + `roles/browser` (project), `roles/run.invoker` (`onbillingbudgetalert` Cloud Run service), and `roles/billing.admin` (billing account).
- [ ] Confirm dry-run Pub/Sub test messages log `SKIPPED_NON_EMERGENCY_BUDGET`, `SKIPPED_BELOW_THRESHOLD`, and `DRY_RUN` cleanly in `onBillingBudgetAlert` before setting `BILLING_KILL_SWITCH_DRY_RUN=false`.
