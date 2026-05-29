/**
 * Trigger: onExpenseUpdated
 *
 * Fires when an expense document is updated in a group's expenses subcollection.
 * Sends an EXPENSE_UPDATED notification to all group members except the updater.
 *
 * Includes a meaningful-change guard: skips notification if none of the
 * substantive fields changed (only metadata like timestamps updated).
 */

import "../config";
import { onDocumentUpdated } from "firebase-functions/v2/firestore";
import { logger } from "firebase-functions/v2";
import { ExpenseDoc, NotificationType, FcmDataPayload, NotificationDisplay, NotificationChannelId } from "../types";
import { getRecipientTokens } from "../services/token.service";
import { sendDataMessage } from "../services/notification.service";
import { getGroupData, getActorDisplayName } from "../services/firestore.service";
import { buildDeepLink } from "../utils/format";

/** Fields that constitute a "meaningful" change worth notifying about. */
const SUBSTANTIVE_FIELDS: (keyof ExpenseDoc)[] = [
  "title",
  "description",
  "vendor",
  "amountCents",
  "groupAmountCents",
  "currency",
  "groupCurrency",
  "expenseCategory",
  "paymentMethod",
  "paymentStatus",
  "payerType",
  "payerId",
  "splitType",
  "notes",
];

export const onExpenseUpdated = onDocumentUpdated(
  "groups/{groupId}/expenses/{expenseId}",
  async (event) => {
    const change = event.data;
    if (!change) {
      logger.warn("onExpenseUpdated: No data in event");
      return;
    }

    const before = change.before.data() as ExpenseDoc;
    const after = change.after.data() as ExpenseDoc;
    const groupId = event.params.groupId;
    const expenseId = event.params.expenseId;

    // Skip if no substantive fields changed
    if (!hasSubstantiveChange(before, after)) {
      logger.info("onExpenseUpdated: Metadata-only change — skipping notification", {
        groupId,
        expenseId,
      });
      return;
    }

    const actorId = after.lastUpdatedBy || after.createdBy;
    if (!actorId) {
      logger.warn("onExpenseUpdated: No actor ID", { groupId, expenseId });
      return;
    }

    const [groupData, actorName] = await Promise.all([
      getGroupData(groupId),
      getActorDisplayName(actorId),
    ]);

    // Suppress notifications during cascading group deletion (or missing group)
    if (!groupData || groupData.deletionRequested) {
      if (groupData?.deletionRequested) {
        logger.info("onExpenseUpdated: Suppressed — group is being deleted", { groupId, expenseId });
      }
      return;
    }

    const tokens = await getRecipientTokens(groupId, actorId, groupData.memberIds);
    if (tokens.length === 0) return;

    const currency = after.currency || groupData.currency;
    const amountCents = after.groupAmountCents ?? after.amountCents;

    const payload: FcmDataPayload = {
      type: NotificationType.EXPENSE_UPDATED,
      groupId,
      groupName: groupData.name,
      memberName: actorName,
      deepLink: buildDeepLink(groupId, `expenses/${expenseId}`),
      entityId: expenseId,
      amountCents: String(amountCents),
      currencyCode: currency,
      expenseTitle: after.title,
    };

    const display: NotificationDisplay = {
      title: groupData.name,
      titleLocKey: "notification_expense_updated_title",
      bodyLocKey: "notification_expense_updated_body_brief",
      bodyLocArgs: [actorName],
      channelId: NotificationChannelId.EXPENSES,
    };

    await sendDataMessage(tokens, payload, display);
  }
);

function hasSubstantiveChange(before: ExpenseDoc, after: ExpenseDoc): boolean {
  for (const field of SUBSTANTIVE_FIELDS) {
    if (before[field] !== after[field]) {
      return true;
    }
  }
  return false;
}
