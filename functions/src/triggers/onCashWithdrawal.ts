/**
 * Trigger: onCashWithdrawal
 *
 * Fires when a new cash withdrawal document is created in a group's
 * cash_withdrawals subcollection. Sends a CASH_WITHDRAWAL notification
 * to all group members except the creator.
 *
 * Detects impersonation: when `createdBy` differs from `withdrawnBy`, the
 * notification uses an "on behalf of" body variant with both display names.
 */

import "../config";
import { onDocumentCreated } from "firebase-functions/v2/firestore";
import { logger } from "firebase-functions/v2";
import {
  CashWithdrawalDoc,
  NotificationType,
  FcmDataPayload,
  NotificationDisplay,
  NotificationChannelId,
} from "../types";
import { getRecipientTokens } from "../services/token.service";
import { sendDataMessage } from "../services/notification.service";
import { getGroupData, getActorDisplayName } from "../services/firestore.service";
import { buildDeepLink } from "../utils/format";

export const onCashWithdrawal = onDocumentCreated(
  "groups/{groupId}/cash_withdrawals/{withdrawalId}",
  async (event) => {
    const snapshot = event.data;
    if (!snapshot) {
      logger.warn("onCashWithdrawal: No data in event");
      return;
    }

    const withdrawal = snapshot.data() as CashWithdrawalDoc;
    const groupId = event.params.groupId;
    const withdrawalId = event.params.withdrawalId;
    const actorId = withdrawal.createdBy;

    if (!actorId) {
      logger.warn("onCashWithdrawal: No createdBy field", { groupId, withdrawalId });
      return;
    }

    // Detect impersonation: actor (createdBy) differs from target (withdrawnBy)
    const targetId = withdrawal.withdrawnBy;
    const isImpersonation = !!targetId && targetId !== actorId;

    const namePromises: Promise<string>[] = [getActorDisplayName(actorId)];
    if (isImpersonation) {
      namePromises.push(getActorDisplayName(targetId));
    }

    const [groupData, ...displayNames] = await Promise.all([
      getGroupData(groupId),
      ...namePromises,
    ]);

    // Suppress notifications during cascading group deletion (or missing group)
    if (!groupData || groupData.deletionRequested) {
      if (groupData?.deletionRequested) {
        logger.info("onCashWithdrawal: Suppressed — group is being deleted", {
          groupId,
          withdrawalId,
        });
      }
      return;
    }

    const actorName = displayNames[0] as string;
    const targetName = isImpersonation ? (displayNames[1] as string) : actorName;

    // Exclude the actor (createdBy) from notifications — target member receives one
    const tokens = await getRecipientTokens(groupId, actorId, groupData.memberIds);
    if (tokens.length === 0) return;

    const payload: FcmDataPayload = {
      type: NotificationType.CASH_WITHDRAWAL,
      groupId,
      groupName: groupData.name,
      memberName: targetName,
      deepLink: buildDeepLink(groupId, `cash_withdrawals/${withdrawalId}`),
      entityId: withdrawalId,
      amountCents: String(withdrawal.amountWithdrawn),
      currencyCode: withdrawal.currency,
      ...(isImpersonation && { actorName }),
    };

    const display: NotificationDisplay = {
      title: groupData.name,
      titleLocKey: "notification_cash_withdrawal_title",
      bodyLocKey: isImpersonation
        ? "notification_cash_withdrawal_body_on_behalf"
        : "notification_cash_withdrawal_body_brief",
      bodyLocArgs: isImpersonation ? [actorName, targetName] : [actorName],
      channelId: NotificationChannelId.FINANCIAL,
    };

    await sendDataMessage(tokens, payload, display);
  }
);
