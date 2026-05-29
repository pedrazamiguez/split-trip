/**
 * SplitTrip — Firebase Cloud Functions
 *
 * Entry point for all Firestore-triggered notification dispatch functions.
 * Each function listens to a specific Firestore document lifecycle event
 * and sends FCM notification messages (with data payload) to relevant
 * group members.
 */

import * as admin from "firebase-admin";
import { setGlobalOptions } from "firebase-functions/v2";

// Initialize Firebase Admin SDK (must happen before any trigger imports)
admin.initializeApp();

// Configure default region to match the eur3 database triggers
setGlobalOptions({ region: "europe-west1" });

// Re-export all trigger functions
export { onExpenseCreated } from "./triggers/onExpenseCreated";
export { onExpenseUpdated } from "./triggers/onExpenseUpdated";
export { onExpenseDeleted } from "./triggers/onExpenseDeleted";
export { onMemberAdded } from "./triggers/onMemberAdded";
export { onMemberRemoved } from "./triggers/onMemberRemoved";
export { onCashWithdrawal } from "./triggers/onCashWithdrawal";
export { onContributionAdded } from "./triggers/onContributionAdded";
export { onGroupDeletionRequested } from "./triggers/onGroupDeletionRequested";

