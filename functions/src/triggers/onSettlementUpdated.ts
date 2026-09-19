/**
 * Trigger: onSettlementUpdated
 *
 * Fires when a settlement document status is updated in a group's settlements subcollection.
 * Handles state transitions:
 * - SUGGESTED -> CONFIRMED_BY_PAYER: Payer paid, notify Payee (SETTLEMENT_REQUEST)
 * - CONFIRMED_BY_PAYER -> RESOLVED / SETTLED: Payee confirmed, notify Payer (SETTLEMENT_CONFIRMED)
 * - * -> DISPUTED: Disputed by a member, notify counterparty (SETTLEMENT_DISPUTED)
 */

import "../config";
import { onDocumentUpdated } from "firebase-functions/v2/firestore";
import { logger } from "firebase-functions/v2";
import {
  SettlementDoc,
  NotificationType,
  FcmDataPayload,
  NotificationDisplay,
  NotificationChannelId,
} from "../types";
import { getUserDeviceTokens } from "../services/token.service";
import { sendDataMessage } from "../services/notification.service";
import { getGroupData, getActorDisplayName } from "../services/firestore.service";
import { buildDeepLink, formatAmount } from "../utils/format";

export const onSettlementUpdated = onDocumentUpdated(
  "groups/{groupId}/settlements/{settlementId}",
  async (event) => {
    const change = event.data;
    if (!change) {
      logger.warn("onSettlementUpdated: No data in event");
      return;
    }

    const before = change.before.data() as SettlementDoc;
    const after = change.after.data() as SettlementDoc;
    const groupId = event.params.groupId;
    const settlementId = event.params.settlementId;

    if (!before || !after || before.status === after.status) {
      return;
    }

    const groupData = await getGroupData(groupId);
    if (!groupData || groupData.deletionRequested) {
      if (groupData?.deletionRequested) {
        logger.info("onSettlementUpdated: Suppressed — group is being deleted", {
          groupId,
          settlementId,
        });
      }
      return;
    }

    let actorId: string | undefined;
    let targetUserId: string | undefined;
    let notificationType: NotificationType | undefined;
    let bodyLocKey: string | undefined;
    let titleLocKey: string | undefined;

    const beforeStatus = before.status;
    const afterStatus = after.status;

    if (beforeStatus === "SUGGESTED" && afterStatus === "CONFIRMED_BY_PAYER") {
      actorId = after.fromUserId;
      targetUserId = after.toUserId;
      notificationType = NotificationType.SETTLEMENT_REQUEST;
      bodyLocKey = "notification_settlement_request_body";
      titleLocKey = "notification_settlement_request_title";
    } else if (
      beforeStatus === "CONFIRMED_BY_PAYER" &&
      (afterStatus === "RESOLVED" || afterStatus === "SETTLED")
    ) {
      actorId = after.toUserId;
      targetUserId = after.fromUserId;
      notificationType = NotificationType.SETTLEMENT_CONFIRMED;
      bodyLocKey = "notification_settlement_confirmed_body";
      titleLocKey = "notification_settlement_confirmed_title";
    } else if (afterStatus === "DISPUTED") {
      actorId = after.disputedBy || after.toUserId;
      targetUserId = actorId === after.fromUserId ? after.toUserId : after.fromUserId;
      notificationType = NotificationType.SETTLEMENT_DISPUTED;
      bodyLocKey = "notification_settlement_disputed_body";
      titleLocKey = "notification_settlement_disputed_title";
    }

    if (!actorId || !targetUserId || !notificationType || !bodyLocKey) {
      return;
    }

    const tokens = await getUserDeviceTokens(targetUserId);
    if (tokens.length === 0) {
      logger.info("onSettlementUpdated: Target user has no registered device tokens", {
        groupId,
        targetUserId,
      });
      return;
    }

    const actorName = await getActorDisplayName(actorId);

    const rawAmountCents = after.amountCents;
    const amountCents =
      rawAmountCents !== undefined && rawAmountCents !== null ? String(rawAmountCents) : undefined;
    const currencyCode = after.currency || groupData.currency;
    const formattedAmount = formatAmount(amountCents, currencyCode);

    const payload: FcmDataPayload = {
      type: notificationType,
      groupId,
      groupName: groupData.name,
      memberName: actorName,
      deepLink: buildDeepLink(groupId, `settlements/${settlementId}`),
      entityId: settlementId,
      actorName,
      payerName: actorName,
      formattedAmount,
      ...(amountCents && { amountCents }),
      ...(currencyCode && { currencyCode }),
    };

    const display: NotificationDisplay = {
      title: groupData.name,
      titleLocKey,
      bodyLocKey,
      bodyLocArgs: [actorName, formattedAmount],
      channelId: NotificationChannelId.FINANCIAL,
    };

    await sendDataMessage(tokens, payload, display);
  }
);
