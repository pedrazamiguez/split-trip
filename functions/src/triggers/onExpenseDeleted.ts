/**
 * Trigger: onExpenseDeleted
 *
 * Fires when an expense document is deleted from a group's expenses subcollection.
 * Sends an EXPENSE_DELETED notification to all group members except the deleter.
 *
 * Note: Uses the "before" snapshot since the document no longer exists.
 */

import "../config";
import { onDocumentDeleted } from "firebase-functions/v2/firestore";
import { logger } from "firebase-functions/v2";
import {
  ExpenseDoc,
  NotificationType,
  FcmDataPayload,
  NotificationDisplay,
  NotificationChannelId,
} from "../types";
import { getRecipientTokens } from "../services/token.service";
import { sendDataMessage } from "../services/notification.service";
import { getGroupData, getActorDisplayName } from "../services/firestore.service";
import { buildDeepLink } from "../utils/format";

export const onExpenseDeleted = onDocumentDeleted(
  "groups/{groupId}/expenses/{expenseId}",
  async (event) => {
    const snapshot = event.data;
    if (!snapshot) {
      logger.warn("onExpenseDeleted: No data in event");
      return;
    }

    const expense = snapshot.data() as ExpenseDoc;
    const groupId = event.params.groupId;
    const expenseId = event.params.expenseId;

    // For deletion, the actor is typically the lastUpdatedBy or createdBy
    const actorId = expense.lastUpdatedBy || expense.createdBy;
    if (!actorId) {
      logger.warn("onExpenseDeleted: No actor ID", { groupId, expenseId });
      return;
    }

    const [groupData, actorName] = await Promise.all([
      getGroupData(groupId),
      getActorDisplayName(actorId),
    ]);

    // Suppress notifications during cascading group deletion (or missing group)
    if (!groupData || groupData.deletionRequested) {
      if (groupData?.deletionRequested) {
        logger.info("onExpenseDeleted: Suppressed — group is being deleted", {
          groupId,
          expenseId,
        });
      }
      return;
    }

    const tokens = await getRecipientTokens(groupId, actorId, groupData.memberIds);
    if (tokens.length === 0) return;

    const currency = expense.currency || groupData.currency;
    const amountCents = expense.groupAmountCents ?? expense.amountCents;

    const payload: FcmDataPayload = {
      type: NotificationType.EXPENSE_DELETED,
      groupId,
      groupName: groupData.name,
      memberName: actorName,
      deepLink: buildDeepLink(groupId),
      entityId: expenseId,
      amountCents: String(amountCents),
      currencyCode: currency,
      expenseTitle: expense.title,
    };

    const display: NotificationDisplay = {
      title: groupData.name,
      titleLocKey: "notification_expense_deleted_title",
      bodyLocKey: "notification_expense_deleted_body_brief",
      bodyLocArgs: [actorName],
      channelId: NotificationChannelId.EXPENSES,
    };

    await sendDataMessage(tokens, payload, display);
  }
);
