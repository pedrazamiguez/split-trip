# Production Service Integrations & Configuration Guide

> [!IMPORTANT]
> **Security & Privacy First:** This guide provides step-by-step setup instructions for external production services. It strictly avoids hardcoded secrets, private tokens, or private credentials. All values use public addresses or generic placeholders (`<PLACEHOLDER>`).

---

## Overview

SplitTrip utilizes the custom domain `splittrip.eu` across multiple services:
1. **GitHub Pages:** Hosts the public landing page (`docs/index.html`), Privacy Policy (`docs/privacy-policy.html`), and Digital Asset Links (`docs/.well-known/assetlinks.json`).
2. **DNS & Email Forwarding:** Routes apex and subdomain traffic to GitHub Pages, and forwards inbound support emails (`support@splittrip.eu`) to the developer's personal inbox.
3. **Firebase Authentication:** Authorizes `splittrip.eu` for OAuth redirect handlers and brands transactional auth emails (password resets and email verification).
4. **Google Play Console:** Maps the official website, support email, privacy policy URL, and App Signing SHA-256 fingerprints for Android App Links.

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

## 2. GitHub Pages Configuration (Repository Settings)

1. Navigate to the GitHub repository: **Settings → Pages**.
2. Under **Build and deployment**:
   - **Source:** Select **Deploy from a branch**.
   - **Branch:** Select `develop` (or `main`), Folder: `/docs`. Click **Save**.
3. Under **Custom domain**:
   - Enter `splittrip.eu`.
   - Click **Save**.
   - GitHub will create or verify `docs/CNAME` and run a DNS check against the apex `A` records.
4. Once the DNS check passes:
   - Check **Enforce HTTPS**.
   - GitHub will automatically provision and renew a free Let's Encrypt TLS certificate for `splittrip.eu` and `www.splittrip.eu`.

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
   - In `docs/.well-known/assetlinks.json`, both the Play App Signing SHA-256 and the local upload key SHA-256 (`01:2A:B7:5D:86:66:1D:95:C0:A2:E5:09:23:66:25:F7:C8:14:2A:EB:6F:CB:DD:CE:B0:46:6E:DC:26:20:86:0E`) are configured.
   - Add both the **Play App Signing SHA-1** and **SHA-256** fingerprints to **Firebase Console → Project settings → Your apps → SplitTrip (Android)** so that Google Sign-In and App Check work in production Play Store builds.

---

## 5. Verification Checklist

- [ ] `dig splittrip.eu +short` returns the four GitHub Pages IPs (`185.199.108.153`, etc.).
- [ ] `curl -I https://splittrip.eu` returns `HTTP/2 200` with valid Let's Encrypt certificate.
- [ ] `curl -I https://splittrip.eu/privacy-policy.html` resolves successfully.
- [ ] `curl https://splittrip.eu/.well-known/assetlinks.json` returns valid JSON with `application/json` content-type.
- [ ] Send a test email from an external account to `support@splittrip.eu` and confirm receipt in the personal inbox.
- [ ] Send a test password reset email from the app or Firebase Console and verify the sender name displays `SplitTrip` and reply-to displays `support@splittrip.eu`.
