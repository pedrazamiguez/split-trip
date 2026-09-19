/**
 * Shared TypeScript types for SplitTrip Cloud Functions.
 *
 * These types mirror the Firestore document shapes defined in the Android
 * codebase (*Document.kt classes) and the FCM data message contract.
 */

// ---------------------------------------------------------------------------
// Notification types — must stay in sync with the Android NotificationType enum
// ---------------------------------------------------------------------------

export enum NotificationType {
  EXPENSE_ADDED = "EXPENSE_ADDED",
  EXPENSE_UPDATED = "EXPENSE_UPDATED",
  EXPENSE_DELETED = "EXPENSE_DELETED",
  EXPENSE_SCHEDULED_EFFECTIVE = "EXPENSE_SCHEDULED_EFFECTIVE",
  MEMBER_ADDED = "MEMBER_ADDED",
  MEMBER_REMOVED = "MEMBER_REMOVED",
  CASH_WITHDRAWAL = "CASH_WITHDRAWAL",
  CONTRIBUTION_ADDED = "CONTRIBUTION_ADDED",
  GROUP_DELETED = "GROUP_DELETED",
  GROUP_INVITE = "GROUP_INVITE",
  SETTLEMENT_REQUEST = "SETTLEMENT_REQUEST",
  SETTLEMENT_CONFIRMED = "SETTLEMENT_CONFIRMED",
  SETTLEMENT_DISPUTED = "SETTLEMENT_DISPUTED",
  DEFAULT = "DEFAULT",
}

// ---------------------------------------------------------------------------
// Firestore document interfaces (subset of fields needed by Cloud Functions)
// ---------------------------------------------------------------------------

export interface GroupDoc {
  groupId: string;
  name: string;
  currency: string;
  memberIds: string[];
  deletionRequested?: boolean;
  deletionNotified?: boolean;
  deletedBy?: string;
}

export interface GroupMemberDoc {
  memberId: string;
  groupId: string;
  userId: string;
  role: string;
  alias?: string;
  addedBy?: string;
  removedBy?: string;
}

export interface ExpenseDoc {
  expenseId: string;
  groupId: string;
  title: string;
  description?: string;
  vendor?: string;
  amountCents: number;
  currency: string;
  groupCurrency: string;
  groupAmountCents?: number;
  expenseCategory: string;
  paymentMethod: string;
  paymentStatus: string;
  payerType: string;
  payerId?: string;
  splitType: string;
  notes?: string;
  createdBy: string;
  lastUpdatedBy?: string;
}

export interface ContributionDoc {
  contributionId: string;
  groupId: string;
  userId: string;
  amountCents: number;
  currency: string;
  createdBy: string;
}

export interface CashWithdrawalDoc {
  withdrawalId: string;
  groupId: string;
  withdrawnBy: string;
  withdrawalScope?: string;
  subunitId?: string;
  amountWithdrawn: number;
  currency: string;
  createdBy: string;
}

export interface UserDoc {
  userId: string;
  username: string;
  email: string;
  displayName?: string;
}

export interface DeviceDoc {
  deviceId: string;
  token: string;
  model: string;
}

export interface SettlementDoc {
  id: string;
  groupId: string;
  fromUserId: string;
  toUserId: string;
  amountCents?: string | number;
  currency?: string;
  status?: string;
  disputedBy?: string;
}

export interface NudgeDoc {
  id: string;
  settlementId: string;
  groupId: string;
  fromUserId: string;
  toUserId: string;
  amountCents?: string | number;
  currencyCode?: string;
  currency?: string;
  createdAt: unknown;
}

// ---------------------------------------------------------------------------
// Android notification channel IDs
// Must stay in sync with NotificationChannelId.kt on the Android side
// ---------------------------------------------------------------------------

export const NotificationChannelId = {
  MEMBERSHIP: "splittrip_membership",
  EXPENSES: "splittrip_expenses",
  FINANCIAL: "splittrip_financial",
  DEFAULT: "splittrip_updates",
} as const;

// ---------------------------------------------------------------------------
// FCM data message payload
// ---------------------------------------------------------------------------

export interface FcmDataPayload {
  type: NotificationType;
  groupId: string;
  groupName: string;
  memberName: string;
  actorName: string;
  deepLink: string;
  entityId?: string;
  formattedAmount?: string;
  amountCents?: string;
  currencyCode?: string;
  expenseTitle?: string;
  payerName?: string;
}

// ---------------------------------------------------------------------------
// Notification display metadata for system-tray rendering
// Used to build the `android.notification` block with localization keys
// so that Android resolves the correct locale-specific string resources
// even when the app process is killed.
// ---------------------------------------------------------------------------

export interface NotificationDisplay {
  /** Direct title text (e.g., group name). Used when a localized title is not needed. */
  title?: string;
  /** Android string resource key for the title (e.g., "notification_expense_added_title"). */
  titleLocKey?: string;
  /** Android string resource key for the body (e.g., "notification_expense_added_body"). */
  bodyLocKey: string;
  /** Substitution arguments for bodyLocKey (e.g., ["Alice", "€45.00"]). */
  bodyLocArgs?: string[];
  /** Android notification channel ID for routing. */
  channelId: string;
}
