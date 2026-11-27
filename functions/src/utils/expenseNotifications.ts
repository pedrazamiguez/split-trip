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
import { buildDeepLink, formatAmount } from "./format";

export async function dispatchExpenseDeletedNotification(
  groupId: string,
  expenseId: string,
  expense: ExpenseDoc,
  triggerName: string
): Promise<void> {
  const actorId = expense.lastUpdatedBy || expense.createdBy;
  if (!actorId) {
    logger.warn(`${triggerName}: No actor ID`, { groupId, expenseId });
    return;
  }

  const [groupData, actorName] = await Promise.all([
    getGroupData(groupId),
    getActorDisplayName(actorId),
  ]);

  if (!groupData || groupData.deletionRequested) {
    if (groupData?.deletionRequested) {
      logger.info(`${triggerName}: Suppressed — group is being deleted`, {
        groupId,
        expenseId,
      });
    }
    return;
  }

  const tokens = await getRecipientTokens(groupId, actorId, groupData.memberIds);
  if (tokens.length === 0) return;

  const hasGroupAmount =
    expense.groupAmountCents !== undefined && expense.groupAmountCents !== null;
  const amountCents = hasGroupAmount ? expense.groupAmountCents! : expense.amountCents;
  const currency = hasGroupAmount
    ? expense.groupCurrency || groupData.currency
    : expense.currency || groupData.currency;
  const formattedAmount = formatAmount(amountCents, currency);

  const payload: FcmDataPayload = {
    type: NotificationType.EXPENSE_DELETED,
    groupId,
    groupName: groupData.name,
    memberName: actorName,
    actorName,
    deepLink: buildDeepLink(groupId, "expenses"),
    entityId: expenseId,
    formattedAmount,
    amountCents: String(amountCents),
    currencyCode: currency,
    expenseTitle: expense.title,
  };

  const display: NotificationDisplay = {
    title: groupData.name,
    titleLocKey: "notification_expense_deleted_title",
    bodyLocKey: formattedAmount
      ? "notification_expense_deleted_body"
      : "notification_expense_deleted_body_brief",
    bodyLocArgs: formattedAmount ? [actorName, formattedAmount] : [actorName],
    channelId: NotificationChannelId.EXPENSES,
  };

  await sendDataMessage(tokens, payload, display);
}
