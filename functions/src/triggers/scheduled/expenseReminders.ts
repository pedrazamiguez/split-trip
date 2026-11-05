import * as functions from "firebase-functions/v2";
import * as admin from "firebase-admin";
import { logger } from "firebase-functions/v2";
import { toZonedTime } from "date-fns-tz";
import { addDays } from "date-fns";
import {
  NotificationType,
  NotificationChannelId,
  FcmDataPayload,
  NotificationDisplay,
  GroupDoc,
} from "../../types";
import { getGroupData } from "../../services/firestore.service";
import { getUserDeviceTokens } from "../../services/token.service";
import { sendDataMessage } from "../../services/notification.service";
import { buildDeepLink } from "../../utils/format";

export async function processExpenseReminders(now: Date = new Date()): Promise<void> {
  const db = admin.firestore();

  const expensesQuery = await db
    .collectionGroup("expenses")
    .where("paymentStatus", "in", ["SCHEDULED", "REFUNDABLE"])
    .get();

  if (expensesQuery.empty) {
    return;
  }

  const groupCache = new Map<string, GroupDoc | null>();

  for (const doc of expensesQuery.docs) {
    const expense = doc.data();
    if (!expense.dueDate) continue;

    const dueDate: Date = typeof expense.dueDate.toDate === "function"
      ? expense.dueDate.toDate()
      : new Date(expense.dueDate);

    const groupId = expense.groupId || doc.ref.parent.parent?.id;
    if (!groupId) continue;

    const expenseId = doc.id;
    const paymentStatus = expense.paymentStatus;

    let groupData: GroupDoc | null;
    if (groupCache.has(groupId)) {
      groupData = groupCache.get(groupId)!;
    } else {
      groupData = await getGroupData(groupId);
      groupCache.set(groupId, groupData);
    }

    if (!groupData || groupData.deletionRequested) continue;

    const memberIds = groupData.memberIds || [];
    if (memberIds.length === 0) continue;

    for (const memberId of memberIds) {
      const userDoc = await db.collection("users").doc(memberId).get();
      if (!userDoc.exists) continue;

      const user = userDoc.data()!;
      const timezone = user.timezone || "UTC";
      const preferredReminderTime = user.preferredReminderTime || "12:00";

      const [prefHour] = preferredReminderTime.split(":").map(Number);
      const userNow = toZonedTime(now, timezone);

      if (userNow.getHours() === prefHour) {
        const userTomorrow = addDays(userNow, 1);

        // Compare UTC calendar fields of dueDate against user's zoned date fields
        const dueDateYear = dueDate.getUTCFullYear();
        const dueDateMonth = dueDate.getUTCMonth();
        const dueDateDate = dueDate.getUTCDate();

        const isToday =
          dueDateYear === userNow.getFullYear() &&
          dueDateMonth === userNow.getMonth() &&
          dueDateDate === userNow.getDate();

        const isTomorrow =
          dueDateYear === userTomorrow.getFullYear() &&
          dueDateMonth === userTomorrow.getMonth() &&
          dueDateDate === userTomorrow.getDate();

        let type: NotificationType | null = null;
        let titleLocKey: string | null = null;
        let bodyLocKey: string | null = null;

        if (isToday && paymentStatus === "SCHEDULED") {
          type = NotificationType.EXPENSE_SCHEDULED_EFFECTIVE;
          titleLocKey = "notification_scheduled_effective_title";
          bodyLocKey = "notification_scheduled_effective_body";
        } else if (isTomorrow) {
          if (paymentStatus === "SCHEDULED") {
            type = NotificationType.EXPENSE_SCHEDULED_REMINDER;
            titleLocKey = "notification_scheduled_reminder_title";
            bodyLocKey = "notification_scheduled_reminder_body";
          } else if (paymentStatus === "REFUNDABLE") {
            type = NotificationType.EXPENSE_REFUNDABLE_REMINDER;
            titleLocKey = "notification_refundable_reminder_title";
            bodyLocKey = "notification_refundable_reminder_body";
          }
        }

        if (type && titleLocKey && bodyLocKey) {
          const tokens = await getUserDeviceTokens(memberId);
          if (tokens.length > 0) {
            const payload: FcmDataPayload = {
              type,
              groupId,
              groupName: groupData.name,
              memberName: groupData.name,
              actorName: groupData.name,
              deepLink: buildDeepLink(groupId, `expenses/${expenseId}`),
              entityId: expenseId,
              expenseId,
              expenseTitle: expense.title,
            };

            const display: NotificationDisplay = {
              title: groupData.name,
              titleLocKey,
              bodyLocKey,
              channelId: NotificationChannelId.EXPENSES,
            };

            try {
              await sendDataMessage(tokens, payload, display);
            } catch (err) {
              logger.error("Failed to send expense reminder", {
                memberId,
                expenseId,
                groupId,
                err,
              });
            }
          }
        }
      }
    }
  }
}

export const expenseReminders = functions.scheduler.onSchedule(
  {
    schedule: "every 1 hours",
    timeZone: "UTC",
    retryCount: 3,
  },
  async () => {
    await processExpenseReminders(new Date());
  }
);
