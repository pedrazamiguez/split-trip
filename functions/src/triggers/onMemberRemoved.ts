/**
 * Trigger: onMemberRemoved
 *
 * Fires when a member document is deleted from a group's members subcollection.
 * Sends a MEMBER_REMOVED notification to all remaining group members.
 *
 * Uses the deleted document's data (before snapshot) to identify the removed member.
 *
 * Note: The `removedBy` field is defined in `GroupMemberDoc` (types.ts) and used
 * by this trigger on line 45 for actor resolution. However, the Android client
 * does not yet write this field — it is reserved for future individual member
 * removal. Currently, member deletion only occurs during full group deletion,
 * so `removedBy` will be undefined and the fallback to `removedUserId` applies.
 */

import "../config";
import { onDocumentDeleted } from "firebase-functions/v2/firestore";
import { logger } from "firebase-functions/v2";
import {
  GroupMemberDoc,
  NotificationType,
  FcmDataPayload,
  NotificationDisplay,
  NotificationChannelId,
} from "../types";
import { getRecipientTokens } from "../services/token.service";
import { sendDataMessage } from "../services/notification.service";
import { getGroupData, getActorDisplayName } from "../services/firestore.service";
import { buildDeepLink } from "../utils/format";

export const onMemberRemoved = onDocumentDeleted(
  "groups/{groupId}/members/{memberId}",
  async (event) => {
    const snapshot = event.data;
    if (!snapshot) {
      logger.warn("onMemberRemoved: No data in event");
      return;
    }

    const member = snapshot.data() as GroupMemberDoc;
    const groupId = event.params.groupId;
    const memberId = event.params.memberId;

    const removedUserId = member.userId;
    if (!removedUserId) {
      logger.warn("onMemberRemoved: No userId in member document", { groupId, memberId });
      return;
    }

    // Use removedBy if available (future: individual member removal),
    // otherwise fall back to the removed member's userId (self-leave / legacy)
    const actorId = member.removedBy || removedUserId;
    const isAdminAction = actorId !== removedUserId;

    const namePromises: Promise<string>[] = [getActorDisplayName(actorId)];
    if (isAdminAction) {
      namePromises.push(getActorDisplayName(removedUserId));
    }

    const [groupData, ...displayNames] = await Promise.all([
      getGroupData(groupId),
      ...namePromises,
    ]);

    // Suppress notifications during cascading group deletion (or missing group)
    if (!groupData || groupData.deletionRequested) {
      if (groupData?.deletionRequested) {
        logger.info("onMemberRemoved: Suppressed — group is being deleted", { groupId, memberId });
      }
      return;
    }

    const actorDisplayName = displayNames[0] as string;
    const memberDisplayName = isAdminAction ? (displayNames[1] as string) : actorDisplayName;

    // Exclude the real actor from notifications
    const tokens = await getRecipientTokens(groupId, actorId, groupData.memberIds);
    if (tokens.length === 0) return;

    const payload: FcmDataPayload = {
      type: NotificationType.MEMBER_REMOVED,
      groupId,
      groupName: groupData.name,
      memberName: memberDisplayName,
      deepLink: buildDeepLink(groupId),
      entityId: memberId,
      ...(isAdminAction && { actorName: actorDisplayName }),
    };

    const display: NotificationDisplay = {
      title: groupData.name,
      titleLocKey: "notification_member_removed_title",
      bodyLocKey: isAdminAction
        ? "notification_member_removed_by_admin_body"
        : "notification_member_removed_body",
      bodyLocArgs: isAdminAction ? [actorDisplayName, memberDisplayName] : [memberDisplayName],
      channelId: NotificationChannelId.MEMBERSHIP,
    };

    await sendDataMessage(tokens, payload, display);
  }
);
