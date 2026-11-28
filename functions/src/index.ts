/**
 * SplitTrip — Firebase Cloud Functions
 *
 * Entry point for Firestore-triggered notification dispatch functions,
 * scheduled maintenance jobs, and Pub/Sub operational triggers (such as
 * the Cloud Billing emergency kill-switch).
 */

import * as admin from "firebase-admin";
import "./config";

// Initialize Firebase Admin SDK (must happen before any trigger imports)
admin.initializeApp();

// Re-export all trigger functions
export { onExpenseCreated } from "./triggers/onExpenseCreated";
export { onExpenseUpdated } from "./triggers/onExpenseUpdated";
export { onExpenseDeleted } from "./triggers/onExpenseDeleted";
export { onMemberAdded } from "./triggers/onMemberAdded";
export { onMemberRemoved } from "./triggers/onMemberRemoved";
export { onCashWithdrawal } from "./triggers/onCashWithdrawal";
export { onContributionAdded } from "./triggers/onContributionAdded";
export { onNudgeCreated } from "./triggers/onNudgeCreated";
export { onSettlementUpdated } from "./triggers/onSettlementUpdated";
export { onGroupDeletionRequested } from "./triggers/onGroupDeletionRequested";
export { expenseReminders } from "./triggers/scheduled/expenseReminders";
export { syncExchangeRates } from "./triggers/scheduled/syncExchangeRates";
export { onBillingBudgetAlert } from "./triggers/onBillingBudgetAlert";
