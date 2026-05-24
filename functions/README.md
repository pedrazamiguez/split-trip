# SplitTrip — Firebase Cloud Functions

Server-side notification dispatch infrastructure for SplitTrip. These Cloud Functions listen to Firestore document lifecycle events and send FCM push notifications to group members.

## Architecture

```
functions/
├── src/
│   ├── index.ts                    # Entry point — re-exports all triggers
│   ├── types.ts                    # Shared TypeScript interfaces & enums
│   ├── services/
│   │   ├── notification.service.ts # FCM multicast dispatch + stale token cleanup
│   │   ├── token.service.ts        # Device token resolution (group members → FCM tokens)
│   │   └── firestore.service.ts    # Shared Firestore read helpers
│   ├── utils/
│   │   └── format.ts              # Amount formatting + deep link builder
│   ├── triggers/
│   │   ├── onExpenseCreated.ts     # EXPENSE_ADDED
│   │   ├── onExpenseUpdated.ts     # EXPENSE_UPDATED
│   │   ├── onExpenseDeleted.ts     # EXPENSE_DELETED
│   │   ├── onMemberAdded.ts        # MEMBER_ADDED
│   │   ├── onMemberRemoved.ts      # MEMBER_REMOVED
│   │   ├── onCashWithdrawal.ts     # CASH_WITHDRAWAL
│   │   └── onContributionAdded.ts  # CONTRIBUTION_ADDED
│   └── __tests__/                  # Unit tests
├── package.json
├── tsconfig.json
├── jest.config.js
├── .eslintrc.js
└── .prettierrc
```

## Firestore Triggers

| Function | Event | Path | NotificationType |
|---|---|---|---|
| `onExpenseCreated` | `onCreate` | `groups/{groupId}/expenses/{expenseId}` | `EXPENSE_ADDED` |
| `onExpenseUpdated` | `onUpdate` | `groups/{groupId}/expenses/{expenseId}` | `EXPENSE_UPDATED` |
| `onExpenseDeleted` | `onDelete` | `groups/{groupId}/expenses/{expenseId}` | `EXPENSE_DELETED` |
| `onMemberAdded` | `onCreate` | `groups/{groupId}/members/{memberId}` | `MEMBER_ADDED` |
| `onMemberRemoved` | `onDelete` | `groups/{groupId}/members/{memberId}` | `MEMBER_REMOVED` |
| `onCashWithdrawal` | `onCreate` | `groups/{groupId}/cash_withdrawals/{id}` | `CASH_WITHDRAWAL` |
| `onContributionAdded` | `onCreate` | `groups/{groupId}/contributions/{id}` | `CONTRIBUTION_ADDED` |

## FCM Payload Contract

All messages include both a **`notification`** key (for system-tray display when the app is killed/backgrounded) and a **`data`** key (for custom handling when the app is in the foreground). The `android.notification` block provides localization keys so Android resolves locale-specific string resources.

**Behavior by app state:**
| App state | Behavior |
|---|---|
| **Foreground** | `onMessageReceived()` fires — app builds a custom notification |
| **Background** | System tray auto-displays using `android.notification` loc keys |
| **Killed** | System tray auto-displays using `android.notification` loc keys |

### Common fields (all types)

| Key | Type | Description |
|---|---|---|
| `type` | `string` | `NotificationType` enum value |
| `groupId` | `string` | Group where the action occurred |
| `groupName` | `string` | Human-readable group name |
| `memberName` | `string` | Display name of the actor |
| `deepLink` | `string` | Deep link URI for in-app navigation |

### Type-specific fields

| Key | Used by | Description |
|---|---|---|
| `amountCents` | Expense, CashWithdrawal, Contribution | Raw amount in cents (client formats with device locale) |
| `currencyCode` | Expense, CashWithdrawal, Contribution | ISO 4217 currency code (e.g. `"EUR"`, `"USD"`) |
| `entityId` | All types | Entity ID for deep link construction |
| `expenseTitle` | Expense events | Title of the expense |

## Prerequisites

- Node.js 22
- Firebase CLI: `npm install -g firebase-tools`
- Firebase project on the **Blaze** (pay-as-you-go) plan (required for Cloud Functions)
- Authenticated: `firebase login`

## Local Development

```bash
# Install dependencies
cd functions
npm install

# Run linter
npm run lint

# Run unit tests
npm test

# Start local emulator
npm run serve

# View logs
npm run logs
```

## Firebase Emulator

To test functions locally with the Firebase Emulator Suite:

```bash
# From the repo root
firebase emulators:start --only functions,firestore

# In another terminal, use the Firestore emulator UI to create/update/delete
# documents and observe the function logs
```

## Deployment

```bash
# Deploy all functions
npm run deploy

# Or from repo root
firebase deploy --only functions
```

### CI/CD

The `.github/workflows/deploy-firebase.yml` workflow automatically deploys functions when changes are pushed to `main` in the `functions/` directory.

**Required GitHub Secret:** `FIREBASE_SERVICE_ACCOUNT_JSON` — Google Service Account JSON key.

The Service Account is configured with the following IAM roles for deployment. Note that this list reflects the standard roles configured for this project; in production environments adhering strictly to the principle of least privilege, these can be restricted further:
*   **Firebase Admin** (`roles/firebase.admin`) — to read project configuration and manage Firebase services. *Note: This is a broad project-level admin role. For a more restricted setup, replace it with specific Firebase sub-roles tailored to your deployment targets.*
*   **Cloud Functions Admin** (`roles/cloudfunctions.admin`) — to deploy and manage Cloud Functions.
*   **Service Account User** (`roles/iam.serviceAccountUser`) — to run the deployment as the service account. *Note: To avoid over-granting permissions at the project level, it is best practice to grant this role only on the specific runtime service account resource (e.g., the Functions runtime service account) that the deployer needs to impersonate.*
*   **Firebase Rules Admin** (`roles/firebaserules.admin`) — to deploy and update Firestore security rules.
*   **Cloud Datastore Index Admin** (`roles/datastore.indexAdmin`) — to deploy Firestore indexes.
*   **Artifact Registry Writer** (`roles/artifactregistry.writer`) — to upload 2nd-gen Cloud Functions container images.

#### Local Verification of Service Account Credentials
To test your service account configuration locally before committing:
```bash
# 1. Export the path to your service account key JSON file
export GOOGLE_APPLICATION_CREDENTIALS="/absolute/path/to/service-account-key.json"

# 2. Run the deploy command with debug enabled to verify permissions
firebase deploy --only functions,firestore --debug
```
If the command completes successfully, the credentials are valid and have the correct roles. Remember to clear the environment variable afterwards: `unset GOOGLE_APPLICATION_CREDENTIALS`.

#### Troubleshooting: Cloud Billing API Error
If you get an error saying `Cloud Billing API has not been used in project before or it is disabled`, this is because the Firebase CLI needs to query billing info to verify Blaze plan limits, but Service Accounts cannot auto-enable Google APIs on the fly. 

To fix this:
1. Open the [Cloud Billing API overview in GCP Console](https://console.developers.google.com/apis/api/cloudbilling.googleapis.com/overview) for your project.
2. Click **Enable**.
3. Re-run your deployment command.

## Stale Token Cleanup

When an FCM send returns `messaging/registration-token-not-registered`, the function automatically deletes the corresponding device document from `users/{uid}/devices/`. This prevents accumulation of stale tokens over time.

## Key Design Decisions

1. **Notification + Data messages** — Messages include both a top-level `notification` (English fallback) and `android.notification` (with `bodyLocKey`/`bodyLocArgs` for localized display). This ensures system-tray rendering when the app is killed or in the background, while `onMessageReceived()` handles custom display when the app is in the foreground.
2. **Actor exclusion** — The user who triggered the action does NOT receive a notification.
3. **Client-side amount formatting** — Functions send raw `amountCents` + `currencyCode`; clients format display strings locally using device locale.
4. **Metadata-only change guard** — `onExpenseUpdated` skips notification if only `lastUpdatedAt`/`lastUpdatedBy` changed.

