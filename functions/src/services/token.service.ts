/**
 * Device token resolution service.
 *
 * Reads Firestore to determine which users should receive a notification and
 * collects their FCM device tokens.
 */

import * as admin from "firebase-admin";
import { logger } from "firebase-functions/v2";
import { GroupMemberDoc, DeviceDoc } from "../types";

const db = () => admin.firestore();

/**
 * Returns an array of FCM device tokens for all group members except the actor.
 *
 * When `memberIds` is provided (from the group document's denormalised array),
 * skips the extra Firestore read of the members subcollection.
 *
 * @param groupId       - The group whose members should be notified
 * @param excludeUserId - The user who performed the action (should NOT receive the notification)
 * @param memberIds     - Optional pre-fetched member user IDs from group document
 * @returns Array of FCM token strings (may be empty)
 */
export async function getRecipientTokens(
  groupId: string,
  excludeUserId: string,
  memberIds?: string[]
): Promise<string[]> {
  const allMemberIds = memberIds ?? (await getGroupMemberUserIds(groupId));

  // Exclude the actor
  const recipientUserIds = allMemberIds.filter((uid) => uid !== excludeUserId);

  if (recipientUserIds.length === 0) {
    logger.info("No recipients after excluding actor", { groupId, excludeUserId });
    return [];
  }

  // Fetch device tokens for all recipients in parallel
  const tokenArrays = await Promise.all(recipientUserIds.map((uid) => getUserDeviceTokens(uid)));

  return tokenArrays.flat();
}

/**
 * Reads the members subcollection of a group and returns their user IDs.
 */
export async function getGroupMemberUserIds(groupId: string): Promise<string[]> {
  const membersSnap = await db().collection("groups").doc(groupId).collection("members").get();

  return membersSnap.docs
    .map((doc) => (doc.data() as GroupMemberDoc).userId)
    .filter((uid) => !!uid);
}

/**
 * Reads all device documents for a user and returns their FCM tokens.
 */
export async function getUserDeviceTokens(userId: string): Promise<string[]> {
  const devicesSnap = await db().collection("users").doc(userId).collection("devices").get();

  return devicesSnap.docs.map((doc) => (doc.data() as DeviceDoc).token).filter((token) => !!token);
}
