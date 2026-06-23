/**
 * Unit tests for notification.service.ts
 *
 * Tests:
 * - Correct FCM message structure with data + android.notification (localization keys)
 * - Stale token cleanup on send failure
 * - Empty token array handling
 */

jest.mock("firebase-admin", () => {
  const sendEachForMulticastMock = jest.fn();
  const batchMock = {
    delete: jest.fn(),
    commit: jest.fn().mockResolvedValue(undefined),
  };
  const collectionGroupMock = jest.fn();

  const firestoreFn = jest.fn(() => ({
    collectionGroup: collectionGroupMock,
    batch: jest.fn(() => batchMock),
  }));

  return {
    firestore: firestoreFn,
    initializeApp: jest.fn(),
    messaging: jest.fn(() => ({
      sendEachForMulticast: sendEachForMulticastMock,
    })),
  };
});

import * as admin from "firebase-admin";
import { sendDataMessage } from "../services/notification.service";
import {
  NotificationType,
  FcmDataPayload,
  NotificationDisplay,
  NotificationChannelId,
} from "../types";

describe("notification.service", () => {
  let sendEachForMulticastMock: jest.Mock;
  let collectionGroupMock: jest.Mock;
  let batchDeleteMock: jest.Mock;
  let batchCommitMock: jest.Mock;

  const samplePayload: FcmDataPayload = {
    type: NotificationType.EXPENSE_ADDED,
    groupId: "group123",
    groupName: "Trip to Japan",
    memberName: "Alice",
    deepLink: "splittrip://groups/group123/expenses/exp456",
    entityId: "exp456",
    amountCents: "4500",
    currencyCode: "EUR",
    expenseTitle: "Sushi dinner",
  };

  const sampleDisplay: NotificationDisplay = {
    title: "Trip to Japan",
    titleLocKey: "notification_expense_added_title",
    bodyLocKey: "notification_expense_added_body_brief",
    bodyLocArgs: ["Alice"],
    channelId: NotificationChannelId.EXPENSES,
  };

  beforeEach(() => {
    jest.clearAllMocks();
    sendEachForMulticastMock = (admin.messaging() as unknown as { sendEachForMulticast: jest.Mock })
      .sendEachForMulticast;
    const db = admin.firestore() as unknown as {
      collectionGroup: jest.Mock;
      batch: jest.Mock;
    };
    collectionGroupMock = db.collectionGroup;
    const batch = db.batch();
    batchDeleteMock = (batch as unknown as { delete: jest.Mock }).delete;
    batchCommitMock = (batch as unknown as { commit: jest.Mock }).commit;
  });

  it("sends a multicast message with data, top-level notification, and android.notification localization keys", async () => {
    sendEachForMulticastMock.mockResolvedValue({
      successCount: 2,
      failureCount: 0,
      responses: [{ success: true }, { success: true }],
    });

    await sendDataMessage(["token1", "token2"], samplePayload, sampleDisplay);

    expect(sendEachForMulticastMock).toHaveBeenCalledTimes(1);
    const call = sendEachForMulticastMock.mock.calls[0][0];

    // Data payload must be present
    expect(call.data).toBeDefined();
    expect(call.data.type).toBe("EXPENSE_ADDED");
    expect(call.data.groupId).toBe("group123");
    expect(call.data.groupName).toBe("Trip to Japan");
    expect(call.data.memberName).toBe("Alice");
    expect(call.data.amountCents).toBe("4500");
    expect(call.data.currencyCode).toBe("EUR");
    expect(call.data.expenseTitle).toBe("Sushi dinner");
    expect(call.tokens).toEqual(["token1", "token2"]);

    // Top-level notification must be present (signals FCM this is a notification message)
    // MUST NOT include body — FCM docs say body + body_loc_key must not coexist
    expect(call.notification).toBeDefined();
    expect(call.notification.title).toBe("Trip to Japan");
    expect(call.notification.body).toBeUndefined();

    // Android notification block with localization keys must be present
    expect(call.android.priority).toBe("high");
    expect(call.android.notification).toBeDefined();
    expect(call.android.notification.title).toBe("Trip to Japan");
    expect(call.android.notification.bodyLocKey).toBe("notification_expense_added_body_brief");
    expect(call.android.notification.bodyLocArgs).toEqual(["Alice"]);
    expect(call.android.notification.channelId).toBe(NotificationChannelId.EXPENSES);
  });

  it("uses titleLocKey when title is not provided", async () => {
    sendEachForMulticastMock.mockResolvedValue({
      successCount: 1,
      failureCount: 0,
      responses: [{ success: true }],
    });

    const displayWithoutTitle: NotificationDisplay = {
      titleLocKey: "notification_expense_added_title",
      bodyLocKey: "notification_expense_added_body_brief",
      bodyLocArgs: ["Alice"],
      channelId: NotificationChannelId.EXPENSES,
    };

    await sendDataMessage(["token1"], samplePayload, displayWithoutTitle);

    const call = sendEachForMulticastMock.mock.calls[0][0];
    // Top-level notification: title is undefined, body must not be present
    expect(call.notification.title).toBeUndefined();
    expect(call.notification.body).toBeUndefined();
    // Android notification uses titleLocKey instead of direct title
    expect(call.android.notification.title).toBeUndefined();
    expect(call.android.notification.titleLocKey).toBe("notification_expense_added_title");
  });

  it("skips sending when tokens array is empty", async () => {
    await sendDataMessage([], samplePayload, sampleDisplay);
    expect(sendEachForMulticastMock).not.toHaveBeenCalled();
  });

  it("cleans up stale tokens on registration-token-not-registered error", async () => {
    sendEachForMulticastMock.mockResolvedValue({
      successCount: 1,
      failureCount: 1,
      responses: [
        { success: true },
        {
          success: false,
          error: {
            code: "messaging/registration-token-not-registered",
            message: "Token not registered",
          },
        },
      ],
    });

    // Mock collectionGroup query for stale token lookup
    const staleDocRef = { id: "device1" };
    collectionGroupMock.mockReturnValue({
      where: jest.fn().mockReturnValue({
        get: jest.fn().mockResolvedValue({
          forEach: (cb: (doc: { ref: typeof staleDocRef }) => void) => {
            cb({ ref: staleDocRef });
          },
        }),
      }),
    });

    await sendDataMessage(["good_token", "stale_token"], samplePayload, sampleDisplay);

    expect(batchDeleteMock).toHaveBeenCalledWith(staleDocRef);
    expect(batchCommitMock).toHaveBeenCalled();
  });

  it("omits undefined payload fields from data map", async () => {
    const minimalPayload: FcmDataPayload = {
      type: NotificationType.MEMBER_ADDED,
      groupId: "group123",
      groupName: "My Group",
      memberName: "Bob",
      deepLink: "splittrip://groups/group123",
    };

    const minimalDisplay: NotificationDisplay = {
      title: "My Group",
      titleLocKey: "notification_member_added_title",
      bodyLocKey: "notification_member_added_body",
      bodyLocArgs: ["Bob"],
      channelId: NotificationChannelId.MEMBERSHIP,
    };

    sendEachForMulticastMock.mockResolvedValue({
      successCount: 1,
      failureCount: 0,
      responses: [{ success: true }],
    });

    await sendDataMessage(["token1"], minimalPayload, minimalDisplay);

    const call = sendEachForMulticastMock.mock.calls[0][0];
    expect(call.data.amountCents).toBeUndefined();
    expect(call.data.currencyCode).toBeUndefined();
    expect(call.data.entityId).toBeUndefined();
    expect(call.data.expenseTitle).toBeUndefined();
  });

  it("omits bodyLocArgs from android.notification when not provided", async () => {
    sendEachForMulticastMock.mockResolvedValue({
      successCount: 1,
      failureCount: 0,
      responses: [{ success: true }],
    });

    const displayWithoutArgs: NotificationDisplay = {
      title: "My Group",
      bodyLocKey: "notification_default_body",
      channelId: NotificationChannelId.DEFAULT,
    };

    await sendDataMessage(["token1"], samplePayload, displayWithoutArgs);

    const call = sendEachForMulticastMock.mock.calls[0][0];
    expect(call.android.notification.bodyLocArgs).toBeUndefined();
    expect(call.android.notification.bodyLocKey).toBe("notification_default_body");
  });
});
