/**
 * Trigger: onExpenseCreated
 *
 * Fires when a new expense document is created in a group's expenses subcollection.
 * Sends an EXPENSE_ADDED notification to all group members except the creator.
 */

import "../config";
import { onDocumentCreated } from "firebase-functions/v2/firestore";
import { logger } from "firebase-functions/v2";
import { ExpenseDoc, NotificationType, FcmDataPayload, NotificationDisplay, NotificationChannelId } from "../types";
import { getRecipientTokens } from "../services/token.service";
import { sendDataMessage } from "../services/notification.service";
import { getGroupData, getActorDisplayName } from "../services/firestore.service";
import { buildDeepLink } from "../utils/format";

export const onExpenseCreated = onDocumentCreated(
  "groups/{groupId}/expenses/{expenseId}",
  async (event) => {
    const snapshot = event.data;
    if (!snapshot) {
      logger.warn("onExpenseCreated: No data in event");
      return;
    }

    const expense = snapshot.data() as ExpenseDoc;
    const groupId = event.params.groupId;
    const expenseId = event.params.expenseId;
    const actorId = expense.createdBy;

    if (!actorId) {
      logger.warn("onExpenseCreated: No createdBy field", { groupId, expenseId });
      return;
    }

    const [groupData, actorName] = await Promise.all([
      getGroupData(groupId),
      getActorDisplayName(actorId),
    ]);

    // Suppress notifications during cascading group deletion (or missing group)
    if (!groupData || groupData.deletionRequested) {
      if (groupData?.deletionRequested) {
        logger.info("onExpenseCreated: Suppressed — group is being deleted", { groupId, expenseId });
      }
      return;
    }

    const tokens = await getRecipientTokens(groupId, actorId, groupData.memberIds);
    if (tokens.length === 0) return;

    const currency = expense.currency || groupData.currency;
    const amountCents = expense.groupAmountCents ?? expense.amountCents;

    const payload: FcmDataPayload = {
      type: NotificationType.EXPENSE_ADDED,
      groupId,
      groupName: groupData.name,
      memberName: actorName,
      deepLink: buildDeepLink(groupId, `expenses/${expenseId}`),
      entityId: expenseId,
      amountCents: String(amountCents),
      currencyCode: currency,
      expenseTitle: expense.title,
    };

    const display: NotificationDisplay = {
      title: groupData.name,
      titleLocKey: "notification_expense_added_title",
      bodyLocKey: "notification_expense_added_body_brief",
      bodyLocArgs: [actorName],
      channelId: NotificationChannelId.EXPENSES,
    };

    await sendDataMessage(tokens, payload, display);
  }
);

