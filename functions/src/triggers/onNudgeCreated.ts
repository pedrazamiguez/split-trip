/**
 * Trigger: onNudgeCreated
 *
 * Fires when a new nudge document is created in a group's nudges subcollection.
 * Resolves the creditor's display name and debtor's device token(s), then sends
 * a SETTLEMENT_REQUEST notification directly to the target debtor user.
 */

import "../config";
import { onDocumentCreated } from "firebase-functions/v2/firestore";
import { logger } from "firebase-functions/v2";
import {
  NudgeDoc,
  NotificationType,
  FcmDataPayload,
  NotificationDisplay,
  NotificationChannelId,
} from "../types";
import { getUserDeviceTokens } from "../services/token.service";
import { sendDataMessage } from "../services/notification.service";
import {
  getGroupData,
  getActorDisplayName,
  getSettlementData,
} from "../services/firestore.service";
import { buildDeepLink, formatAmount } from "../utils/format";

export const onNudgeCreated = onDocumentCreated(
  "groups/{groupId}/nudges/{nudgeId}",
  async (event) => {
    const snapshot = event.data;
    if (!snapshot) {
      logger.warn("onNudgeCreated: No data in event");
      return;
    }

    const nudge = snapshot.data() as NudgeDoc;
    const groupId = event.params.groupId;
    const nudgeId = event.params.nudgeId;

    if (!nudge.fromUserId || !nudge.toUserId) {
      logger.warn("onNudgeCreated: Missing fromUserId or toUserId", { groupId, nudgeId });
      return;
    }

    const [groupData, creditorName, settlementData] = await Promise.all([
      getGroupData(groupId),
      getActorDisplayName(nudge.fromUserId),
      getSettlementData(groupId, nudge.settlementId),
    ]);

    // Suppress notifications during cascading group deletion (or missing group)
    if (!groupData || groupData.deletionRequested) {
      if (groupData?.deletionRequested) {
        logger.info("onNudgeCreated: Suppressed — group is being deleted", {
          groupId,
          nudgeId,
        });
      }
      return;
    }

    const tokens = await getUserDeviceTokens(nudge.toUserId);
    if (tokens.length === 0) {
      logger.info("onNudgeCreated: Target user has no registered device tokens", {
        groupId,
        toUserId: nudge.toUserId,
      });
      return;
    }

    const rawAmountCents = nudge.amountCents ?? settlementData?.amountCents;
    const amountCents =
      rawAmountCents !== undefined && rawAmountCents !== null ? String(rawAmountCents) : undefined;
    const currencyCode =
      nudge.currencyCode || nudge.currency || settlementData?.currency || groupData.currency;
    const formattedAmount = formatAmount(amountCents, currencyCode);

    const payload: FcmDataPayload = {
      type: NotificationType.SETTLEMENT_REQUEST,
      groupId,
      groupName: groupData.name,
      memberName: creditorName,
      deepLink: buildDeepLink(groupId, `settlements/${nudge.settlementId}`),
      entityId: nudge.settlementId,
      actorName: creditorName,
      payerName: creditorName,
      formattedAmount,
      ...(amountCents && { amountCents }),
      ...(currencyCode && { currencyCode }),
    };

    const display: NotificationDisplay = {
      title: groupData.name,
      titleLocKey: "notification_settlement_request_title",
      bodyLocKey: "notification_settlement_request_body",
      bodyLocArgs: [creditorName, formattedAmount],
      channelId: NotificationChannelId.FINANCIAL,
    };

    await sendDataMessage(tokens, payload, display);
  }
);
