import { processExpenseReminders } from "../triggers/scheduled/expenseReminders";
import { getGroupData } from "../services/firestore.service";
import { getUserDeviceTokens } from "../services/token.service";
import { sendDataMessage } from "../services/notification.service";
import { NotificationType, NotificationChannelId } from "../types";
import * as admin from "firebase-admin";

jest.mock("firebase-admin", () => {
  const firestoreMock = {
    collection: jest.fn(),
    collectionGroup: jest.fn(),
  };

  return {
    firestore: jest.fn(() => firestoreMock),
    initializeApp: jest.fn(),
  };
});

jest.mock("../services/firestore.service", () => ({
  getGroupData: jest.fn(),
}));

jest.mock("../services/token.service", () => ({
  getUserDeviceTokens: jest.fn(),
}));

jest.mock("../services/notification.service", () => ({
  sendDataMessage: jest.fn(),
}));

describe("processExpenseReminders", () => {
  let firestoreMock: {
    collection: jest.Mock;
    collectionGroup: jest.Mock;
  };

  const getGroupDataMock = getGroupData as jest.Mock;
  const getUserDeviceTokensMock = getUserDeviceTokens as jest.Mock;
  const sendDataMessageMock = sendDataMessage as jest.Mock;

  const sampleGroup = {
    groupId: "group1",
    name: "Trip to NYC",
    currency: "USD",
    memberIds: ["user1"],
  };

  beforeEach(() => {
    jest.clearAllMocks();
    firestoreMock = admin.firestore() as unknown as {
      collection: jest.Mock;
      collectionGroup: jest.Mock;
    };
  });

  it("queries collectionGroup expenses with status in SCHEDULED, REFUNDABLE", async () => {
    const whereMock = jest.fn().mockReturnValue({
      get: jest.fn().mockResolvedValue({ empty: true, docs: [] }),
    });
    firestoreMock.collectionGroup.mockReturnValue({ where: whereMock });

    await processExpenseReminders(new Date("2026-05-10T13:00:00Z"));

    expect(firestoreMock.collectionGroup).toHaveBeenCalledWith("expenses");
    expect(whereMock).toHaveBeenCalledWith("paymentStatus", "in", ["SCHEDULED", "REFUNDABLE"]);
    expect(getGroupDataMock).not.toHaveBeenCalled();
  });

  it("skips documents with no dueDate", async () => {
    const docs = [
      {
        id: "exp1",
        data: () => ({
          groupId: "group1",
          paymentStatus: "SCHEDULED",
          title: "No Due Date",
        }),
        ref: { parent: { parent: { id: "group1" } } },
      },
    ];

    firestoreMock.collectionGroup.mockReturnValue({
      where: jest.fn().mockReturnValue({
        get: jest.fn().mockResolvedValue({ empty: false, docs }),
      }),
    });

    await processExpenseReminders(new Date("2026-05-10T13:00:00Z"));

    expect(getGroupDataMock).not.toHaveBeenCalled();
    expect(sendDataMessageMock).not.toHaveBeenCalled();
  });

  it("skips when group is not found or deletionRequested is true", async () => {
    const docs = [
      {
        id: "exp1",
        data: () => ({
          groupId: "group1",
          paymentStatus: "SCHEDULED",
          title: "Dinner",
          dueDate: { toDate: () => new Date("2026-05-10T00:00:00Z") },
        }),
        ref: { parent: { parent: { id: "group1" } } },
      },
    ];

    firestoreMock.collectionGroup.mockReturnValue({
      where: jest.fn().mockReturnValue({
        get: jest.fn().mockResolvedValue({ empty: false, docs }),
      }),
    });

    // Case 1: groupData is null
    getGroupDataMock.mockResolvedValueOnce(null);
    await processExpenseReminders(new Date("2026-05-10T13:00:00Z"));
    expect(sendDataMessageMock).not.toHaveBeenCalled();

    // Case 2: deletionRequested is true
    getGroupDataMock.mockResolvedValueOnce({ ...sampleGroup, deletionRequested: true });
    await processExpenseReminders(new Date("2026-05-10T13:00:00Z"));
    expect(sendDataMessageMock).not.toHaveBeenCalled();
  });

  it("dispatches EXPENSE_SCHEDULED_EFFECTIVE notification when dueDate is today and hour matches", async () => {
    const docs = [
      {
        id: "exp1",
        data: () => ({
          groupId: "group1",
          paymentStatus: "SCHEDULED",
          title: "Hotel Booking",
          dueDate: { toDate: () => new Date("2026-05-10T00:00:00Z") },
        }),
        ref: { parent: { parent: { id: "group1" } } },
      },
    ];

    firestoreMock.collectionGroup.mockReturnValue({
      where: jest.fn().mockReturnValue({
        get: jest.fn().mockResolvedValue({ empty: false, docs }),
      }),
    });

    getGroupDataMock.mockResolvedValue(sampleGroup);

    firestoreMock.collection.mockReturnValue({
      doc: jest.fn().mockReturnValue({
        get: jest.fn().mockResolvedValue({
          exists: true,
          data: () => ({
            timezone: "America/New_York",
            preferredReminderTime: "09:00",
          }),
        }),
      }),
    });

    getUserDeviceTokensMock.mockResolvedValue(["token1"]);

    // 2026-05-10T13:00:00Z is 09:00 EDT (UTC-4)
    await processExpenseReminders(new Date("2026-05-10T13:00:00Z"));

    expect(sendDataMessageMock).toHaveBeenCalledTimes(1);
    expect(sendDataMessageMock).toHaveBeenCalledWith(
      ["token1"],
      {
        type: NotificationType.EXPENSE_SCHEDULED_EFFECTIVE,
        groupId: "group1",
        groupName: "Trip to NYC",
        memberName: "Trip to NYC",
        actorName: "Trip to NYC",
        deepLink: "splittrip://groups/group1/expenses/exp1",
        entityId: "exp1",
        expenseId: "exp1",
        expenseTitle: "Hotel Booking",
      },
      {
        title: "Trip to NYC",
        titleLocKey: "notification_scheduled_effective_title",
        bodyLocKey: "notification_scheduled_effective_body",
        channelId: NotificationChannelId.EXPENSES,
      }
    );
  });

  it("dispatches EXPENSE_SCHEDULED_REMINDER notification when dueDate is tomorrow and hour matches", async () => {
    const docs = [
      {
        id: "exp1",
        data: () => ({
          groupId: "group1",
          paymentStatus: "SCHEDULED",
          title: "Car Rental",
          dueDate: { toDate: () => new Date("2026-05-10T00:00:00Z") },
        }),
        ref: { parent: { parent: { id: "group1" } } },
      },
    ];

    firestoreMock.collectionGroup.mockReturnValue({
      where: jest.fn().mockReturnValue({
        get: jest.fn().mockResolvedValue({ empty: false, docs }),
      }),
    });

    getGroupDataMock.mockResolvedValue(sampleGroup);

    firestoreMock.collection.mockReturnValue({
      doc: jest.fn().mockReturnValue({
        get: jest.fn().mockResolvedValue({
          exists: true,
          data: () => ({
            timezone: "America/New_York",
            preferredReminderTime: "09:00",
          }),
        }),
      }),
    });

    getUserDeviceTokensMock.mockResolvedValue(["token1"]);

    // At 09:00 on May 9 EDT (13:00 UTC), May 10 is tomorrow
    await processExpenseReminders(new Date("2026-05-09T13:00:00Z"));

    expect(sendDataMessageMock).toHaveBeenCalledTimes(1);
    expect(sendDataMessageMock).toHaveBeenCalledWith(
      ["token1"],
      expect.objectContaining({
        type: NotificationType.EXPENSE_SCHEDULED_REMINDER,
        expenseTitle: "Car Rental",
      }),
      expect.objectContaining({
        titleLocKey: "notification_scheduled_reminder_title",
        bodyLocKey: "notification_scheduled_reminder_body",
      })
    );
  });

  it("dispatches EXPENSE_REFUNDABLE_REMINDER notification when dueDate is tomorrow and hour matches", async () => {
    const docs = [
      {
        id: "exp2",
        data: () => ({
          groupId: "group1",
          paymentStatus: "REFUNDABLE",
          title: "Flight Ticket Deposit",
          dueDate: { toDate: () => new Date("2026-05-10T00:00:00Z") },
        }),
        ref: { parent: { parent: { id: "group1" } } },
      },
    ];

    firestoreMock.collectionGroup.mockReturnValue({
      where: jest.fn().mockReturnValue({
        get: jest.fn().mockResolvedValue({ empty: false, docs }),
      }),
    });

    getGroupDataMock.mockResolvedValue(sampleGroup);

    firestoreMock.collection.mockReturnValue({
      doc: jest.fn().mockReturnValue({
        get: jest.fn().mockResolvedValue({
          exists: true,
          data: () => ({
            timezone: "America/New_York",
            preferredReminderTime: "09:00",
          }),
        }),
      }),
    });

    getUserDeviceTokensMock.mockResolvedValue(["token1"]);

    // At 09:00 on May 9 EDT (13:00 UTC), May 10 is tomorrow
    await processExpenseReminders(new Date("2026-05-09T13:00:00Z"));

    expect(sendDataMessageMock).toHaveBeenCalledTimes(1);
    expect(sendDataMessageMock).toHaveBeenCalledWith(
      ["token1"],
      expect.objectContaining({
        type: NotificationType.EXPENSE_REFUNDABLE_REMINDER,
        expenseTitle: "Flight Ticket Deposit",
      }),
      expect.objectContaining({
        titleLocKey: "notification_refundable_reminder_title",
        bodyLocKey: "notification_refundable_reminder_body",
      })
    );
  });

  it("does not shift calendar date when member timezone has negative UTC offset", async () => {
    // Due date is May 10 at 00:00:00 UTC
    const docs = [
      {
        id: "exp3",
        data: () => ({
          groupId: "group1",
          paymentStatus: "SCHEDULED",
          title: "Tour Guide",
          dueDate: new Date("2026-05-10T00:00:00Z"),
        }),
        ref: { parent: { parent: { id: "group1" } } },
      },
    ];

    firestoreMock.collectionGroup.mockReturnValue({
      where: jest.fn().mockReturnValue({
        get: jest.fn().mockResolvedValue({ empty: false, docs }),
      }),
    });

    getGroupDataMock.mockResolvedValue(sampleGroup);

    firestoreMock.collection.mockReturnValue({
      doc: jest.fn().mockReturnValue({
        get: jest.fn().mockResolvedValue({
          exists: true,
          data: () => ({
            timezone: "America/New_York", // UTC-4
            preferredReminderTime: "09:00",
          }),
        }),
      }),
    });

    getUserDeviceTokensMock.mockResolvedValue(["token1"]);

    // On May 9 at 09:00 EDT (13:00 UTC), the expense due May 10 must be recognized as TOMORROW, not today
    await processExpenseReminders(new Date("2026-05-09T13:00:00Z"));

    expect(sendDataMessageMock).toHaveBeenCalledTimes(1);
    expect(sendDataMessageMock).toHaveBeenCalledWith(
      ["token1"],
      expect.objectContaining({
        type: NotificationType.EXPENSE_SCHEDULED_REMINDER,
      }),
      expect.objectContaining({
        titleLocKey: "notification_scheduled_reminder_title",
      })
    );
  });

  it("skips when user timezone hour does not match preferredReminderTime", async () => {
    const docs = [
      {
        id: "exp1",
        data: () => ({
          groupId: "group1",
          paymentStatus: "SCHEDULED",
          title: "Dinner",
          dueDate: { toDate: () => new Date("2026-05-10T00:00:00Z") },
        }),
        ref: { parent: { parent: { id: "group1" } } },
      },
    ];

    firestoreMock.collectionGroup.mockReturnValue({
      where: jest.fn().mockReturnValue({
        get: jest.fn().mockResolvedValue({ empty: false, docs }),
      }),
    });

    getGroupDataMock.mockResolvedValue(sampleGroup);

    firestoreMock.collection.mockReturnValue({
      doc: jest.fn().mockReturnValue({
        get: jest.fn().mockResolvedValue({
          exists: true,
          data: () => ({
            timezone: "America/New_York",
            preferredReminderTime: "09:00",
          }),
        }),
      }),
    });

    getUserDeviceTokensMock.mockResolvedValue(["token1"]);

    // 2026-05-10T14:00:00Z is 10:00 EDT, while user's preferred time is 09:00
    await processExpenseReminders(new Date("2026-05-10T14:00:00Z"));

    expect(sendDataMessageMock).not.toHaveBeenCalled();
  });

  it("skips sending when user has no device tokens", async () => {
    const docs = [
      {
        id: "exp1",
        data: () => ({
          groupId: "group1",
          paymentStatus: "SCHEDULED",
          title: "Dinner",
          dueDate: { toDate: () => new Date("2026-05-10T00:00:00Z") },
        }),
        ref: { parent: { parent: { id: "group1" } } },
      },
    ];

    firestoreMock.collectionGroup.mockReturnValue({
      where: jest.fn().mockReturnValue({
        get: jest.fn().mockResolvedValue({ empty: false, docs }),
      }),
    });

    getGroupDataMock.mockResolvedValue(sampleGroup);

    firestoreMock.collection.mockReturnValue({
      doc: jest.fn().mockReturnValue({
        get: jest.fn().mockResolvedValue({
          exists: true,
          data: () => ({
            timezone: "America/New_York",
            preferredReminderTime: "09:00",
          }),
        }),
      }),
    });

    getUserDeviceTokensMock.mockResolvedValue([]);

    await processExpenseReminders(new Date("2026-05-10T13:00:00Z"));

    expect(sendDataMessageMock).not.toHaveBeenCalled();
  });
});
