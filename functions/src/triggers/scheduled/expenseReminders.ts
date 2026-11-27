import * as functions from "firebase-functions/v2";
import * as admin from "firebase-admin";
import { logger } from "firebase-functions/v2";
import { toZonedTime } from "date-fns-tz";
import { addDays, subDays } from "date-fns";
import {
  NotificationType,
  NotificationChannelId,
  FcmDataPayload,
  NotificationDisplay,
  GroupDoc,
  UserDoc,
} from "../../types";
import { getGroupData } from "../../services/firestore.service";
import { getUserDeviceTokens } from "../../services/token.service";
import { sendDataMessage } from "../../services/notification.service";
import { buildDeepLink } from "../../utils/format";

const DUE_DATE_WINDOW_DAYS_BEFORE = 2;
const DUE_DATE_WINDOW_DAYS_AFTER = 3;

export async function processExpenseReminders(now: Date = new Date()): Promise<void> {
  const db = admin.firestore();

  const windowStart = admin.firestore.Timestamp.fromDate(
    subDays(now, DUE_DATE_WINDOW_DAYS_BEFORE)
  );
  const windowEnd = admin.firestore.Timestamp.fromDate(
    addDays(now, DUE_DATE_WINDOW_DAYS_AFTER)
  );

  const expensesQuery = await db
    .collectionGroup("expenses")
    .where("paymentStatus", "in", ["SCHEDULED", "REFUNDABLE"])
    .where("dueDate", ">=", windowStart)
    .where("dueDate", "<=", windowEnd)
    .get();

  if (expensesQuery.empty) {
    return;
  }

  const groupCache = new Map<string, GroupDoc | null>();
  const userCache = new Map<string, UserDoc | null>();
  const tokenCache = new Map<string, string[]>();

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
      let user: UserDoc | null;
      if (userCache.has(memberId)) {
        user = userCache.get(memberId) ?? null;
      } else {
        const userDoc = await db.collection("users").doc(memberId).get();
        user = userDoc.exists ? (userDoc.data() as UserDoc) : null;
        userCache.set(memberId, user);
      }
      if (!user) continue;

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
          let tokens: string[];
          if (tokenCache.has(memberId)) {
            tokens = tokenCache.get(memberId) ?? [];
          } else {
            tokens = await getUserDeviceTokens(memberId);
            tokenCache.set(memberId, tokens);
          }
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
