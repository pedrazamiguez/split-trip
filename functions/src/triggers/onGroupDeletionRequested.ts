/**
 * Trigger: onGroupDeletionRequested
 *
 * Fires when a group document is updated. Checks if `deletionRequested` has
 * transitioned from falsy to `true` — if so, performs a server-side cascading
 * delete of all subcollections, sends a single GROUP_DELETED notification to
 * all former members, and deletes the group document.
 *
 * This replaces the client-side "Capture-then-Kill" strategy which was:
 * - Incomplete (only captured locally-synced IDs)
 * - Non-atomic (sequential deletes could fail midway)
 * - Caused notification spam (each subcollection delete triggered FCM)
 * - Left stale data on other devices (members deletion happened last)
 */

import "../config";
import { onDocumentUpdated } from "firebase-functions/v2/firestore";
import { logger } from "firebase-functions/v2";
import * as admin from "firebase-admin";
import {
  GroupDoc,
  NotificationType,
  FcmDataPayload,
  NotificationDisplay,
  NotificationChannelId,
} from "../types";
import { getRecipientTokens } from "../services/token.service";
import { sendDataMessage } from "../services/notification.service";
import { getActorDisplayName } from "../services/firestore.service";

const db = () => admin.firestore();

/** Firestore batch write limit. */
const BATCH_SIZE = 500;

/** Deep link to the groups list (not a specific group, since it's being deleted). */
const GROUPS_LIST_DEEP_LINK = "splittrip://groups";

/**
 * Deletes all documents in a subcollection using batched writes.
 * Processes up to BATCH_SIZE documents per batch to stay within Firestore limits.
 */
async function deleteSubcollection(collectionPath: string): Promise<number> {
  const collectionRef = db().collection(collectionPath);
  let totalDeleted = 0;

  // Process in batches to handle large subcollections
  // eslint-disable-next-line no-constant-condition
  while (true) {
    const snapshot = await collectionRef.limit(BATCH_SIZE).get();
    if (snapshot.empty) break;

    const batch = db().batch();
    snapshot.docs.forEach((doc) => batch.delete(doc.ref));
    await batch.commit();
    totalDeleted += snapshot.size;

    if (snapshot.size < BATCH_SIZE) break;
  }

  return totalDeleted;
}

export const onGroupDeletionRequested = onDocumentUpdated(
  {
    document: "groups/{groupId}",
    // Large groups may have hundreds of subcollection docs to delete.
    // Increase timeout/memory to avoid mid-cascade timeouts.
    timeoutSeconds: 300,
    memory: "512MiB",
  },
  async (event) => {
    const change = event.data;
    if (!change) {
      logger.warn("onGroupDeletionRequested: No data in event");
      return;
    }

    const before = change.before.data() as GroupDoc;
    const after = change.after.data() as GroupDoc;
    const groupId = event.params.groupId;

    // Guard: only act on the transition to deletionRequested = true
    if (before.deletionRequested || !after.deletionRequested) {
      return;
    }

    const deletedBy = after.deletedBy;
    const groupName = after.name || before.name || "";
    // Capture memberIds BEFORE we delete anything
    const memberIds = after.memberIds || before.memberIds || [];

    logger.info("Group deletion requested — starting cascading delete", {
      groupId,
      deletedBy,
      memberCount: memberIds.length,
    });

    try {
      // 1. Delete members subcollection FIRST.
      // This is critical: other devices have snapshot listeners on collectionGroup("members").
      // When member docs are removed, those listeners fire and the Room reconciliation
      // removes the group from their local DB + UI.
      const membersDeleted = await deleteSubcollection(`groups/${groupId}/members`);
      logger.info("Deleted members subcollection", { groupId, count: membersDeleted });

      // 2. Delete all other subcollections in parallel.
      // These deletions won't cause notification spam because the existing triggers
      // check groupData.deletionRequested and skip notifications when true.
      const [expensesDeleted, contributionsDeleted, withdrawalsDeleted, subunitsDeleted] =
        await Promise.all([
          deleteSubcollection(`groups/${groupId}/expenses`),
          deleteSubcollection(`groups/${groupId}/contributions`),
          deleteSubcollection(`groups/${groupId}/cash_withdrawals`),
          deleteSubcollection(`groups/${groupId}/subunits`),
        ]);

      logger.info("Deleted all subcollections", {
        groupId,
        expenses: expensesDeleted,
        contributions: contributionsDeleted,
        withdrawals: withdrawalsDeleted,
        subunits: subunitsDeleted,
      });

      // 3. Send a single GROUP_DELETED notification to all former members (except deleter).
      // Idempotency: Set deletionNotified flag before sending to prevent duplicate
      // notifications on Cloud Functions retries (if the function fails after sending
      // but before completing the final group doc delete).
      if (deletedBy && memberIds.length > 0) {
        const groupRef = db().collection("groups").doc(groupId);
        const groupSnap = await groupRef.get();

        // Only send notification if we haven't already (idempotency guard)
        if (groupSnap.exists && !groupSnap.data()?.deletionNotified) {
          try {
            // Mark as notified BEFORE sending to handle crash-after-send
            await groupRef.update({ deletionNotified: true });

            const actorName = await getActorDisplayName(deletedBy);
            const tokens = await getRecipientTokens(groupId, deletedBy, memberIds);

            if (tokens.length > 0) {
              const payload: FcmDataPayload = {
                type: NotificationType.GROUP_DELETED,
                groupId,
                groupName,
                memberName: actorName,
                deepLink: GROUPS_LIST_DEEP_LINK,
              };

              const display: NotificationDisplay = {
                title: groupName,
                titleLocKey: "notification_group_deleted_title",
                bodyLocKey: "notification_group_deleted_body",
                bodyLocArgs: [actorName, groupName],
                channelId: NotificationChannelId.MEMBERSHIP,
              };

              await sendDataMessage(tokens, payload, display);
              logger.info("Sent GROUP_DELETED notification", {
                groupId,
                recipientCount: tokens.length,
              });
            }
          } catch (notifErr) {
            // Notification failure should not block group cleanup
            logger.error("Failed to send GROUP_DELETED notification", { groupId, notifErr });
          }
        } else {
          logger.info("Skipping GROUP_DELETED notification — already sent", { groupId });
        }
      }

      // 4. Delete the group document itself.
      await db().collection("groups").doc(groupId).delete();

      logger.info("Cascading group deletion complete", {
        groupId,
        members: membersDeleted,
        expenses: expensesDeleted,
        contributions: contributionsDeleted,
        withdrawals: withdrawalsDeleted,
        subunits: subunitsDeleted,
      });
    } catch (err) {
      logger.error("Cascading group deletion failed", { groupId, err });
      throw err; // Let Cloud Functions retry
    }
  }
);
