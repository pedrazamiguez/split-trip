/**
 * Trigger: onExpenseDeleted
 *
 * Fires when an expense document is deleted from a group's expenses subcollection.
 * Sends an EXPENSE_DELETED notification to all group members except the deleter
 * for legacy hard-deletes, while suppressing TTL/cleanup purges of already-tombstoned docs.
 */

import "../config";
import { onDocumentDeleted } from "firebase-functions/v2/firestore";
import { logger } from "firebase-functions/v2";
import { ExpenseDoc } from "../types";
import { dispatchExpenseDeletedNotification } from "../utils/expenseNotifications";

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

    if (expense.isDeleted === true) {
      logger.info("onExpenseDeleted: Suppressed — already soft-deleted (TTL cleanup)", {
        groupId,
        expenseId,
      });
      return;
    }

    await dispatchExpenseDeletedNotification(groupId, expenseId, expense, "onExpenseDeleted");
  }
);
