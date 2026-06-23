/**
 * Trigger: onMemberAdded
 *
 * Fires when a new member document is created in a group's members subcollection.
 * Sends a MEMBER_ADDED notification to all existing group members.
 *
 * Uses the `addedBy` field to determine the real actor:
 * - Self-join (addedBy === userId or missing): actor is the new member.
 * - Admin-add (addedBy !== userId): actor is the admin who added the member.
 *   Both actorName and memberName are sent so the client can build
 *   an accurate message (e.g., "Admin added Member to the group").
 */

import "../config";
import { onDocumentCreated } from "firebase-functions/v2/firestore";
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

export const onMemberAdded = onDocumentCreated(
  "groups/{groupId}/members/{memberId}",
  async (event) => {
    const snapshot = event.data;
    if (!snapshot) {
      logger.warn("onMemberAdded: No data in event");
      return;
    }

    const member = snapshot.data() as GroupMemberDoc;
    const groupId = event.params.groupId;
    const memberId = event.params.memberId;

    const newMemberUserId = member.userId;
    if (!newMemberUserId) {
      logger.warn("onMemberAdded: No userId in member document", { groupId, memberId });
      return;
    }

    // Determine real actor: addedBy (admin action) or userId (self-join / legacy)
    const actorId = member.addedBy || newMemberUserId;
    const isAdminAction = actorId !== newMemberUserId;

    const namePromises: Promise<string>[] = [getActorDisplayName(actorId)];
    if (isAdminAction) {
      namePromises.push(getActorDisplayName(newMemberUserId));
    }

    const [groupData, ...displayNames] = await Promise.all([
      getGroupData(groupId),
      ...namePromises,
    ]);

    // Suppress notifications during cascading group deletion (or missing group)
    if (!groupData || groupData.deletionRequested) {
      if (groupData?.deletionRequested) {
        logger.info("onMemberAdded: Suppressed — group is being deleted", { groupId, memberId });
      }
      return;
    }

    const actorDisplayName = displayNames[0] as string;
    const memberDisplayName = isAdminAction ? (displayNames[1] as string) : actorDisplayName;

    // Exclude the real actor (admin or self-joiner) from notifications
    const tokens = await getRecipientTokens(groupId, actorId, groupData.memberIds);
    if (tokens.length === 0) return;

    const payload: FcmDataPayload = {
      type: NotificationType.MEMBER_ADDED,
      groupId,
      groupName: groupData.name,
      memberName: memberDisplayName,
      deepLink: buildDeepLink(groupId),
      entityId: memberId,
      ...(isAdminAction && { actorName: actorDisplayName }),
    };

    const display: NotificationDisplay = {
      title: groupData.name,
      titleLocKey: "notification_member_added_title",
      bodyLocKey: isAdminAction
        ? "notification_member_added_by_admin_body"
        : "notification_member_added_body",
      bodyLocArgs: isAdminAction ? [actorDisplayName, memberDisplayName] : [memberDisplayName],
      channelId: NotificationChannelId.MEMBERSHIP,
    };

    await sendDataMessage(tokens, payload, display);
  }
);
