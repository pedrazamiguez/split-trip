/**
 * Unit tests for onSettlementUpdated trigger
 */

import { onSettlementUpdated } from "../triggers/onSettlementUpdated";
import { getGroupData, getActorDisplayName } from "../services/firestore.service";
import { getUserDeviceTokens } from "../services/token.service";
import { sendDataMessage } from "../services/notification.service";
import { NotificationType, NotificationChannelId } from "../types";

jest.mock("firebase-admin", () => ({
  initializeApp: jest.fn(),
}));

jest.mock("../services/firestore.service", () => ({
  getGroupData: jest.fn(),
  getActorDisplayName: jest.fn(),
}));

jest.mock("../services/token.service", () => ({
  getUserDeviceTokens: jest.fn(),
}));

jest.mock("../services/notification.service", () => ({
  sendDataMessage: jest.fn(),
}));

describe("onSettlementUpdated trigger", () => {
  const getGroupDataMock = getGroupData as jest.Mock;
  const getActorDisplayNameMock = getActorDisplayName as jest.Mock;
  const getUserDeviceTokensMock = getUserDeviceTokens as jest.Mock;
  const sendDataMessageMock = sendDataMessage as jest.Mock;

  const sampleGroup = {
    groupId: "group1",
    name: "Trip to Paris",
    currency: "EUR",
    memberIds: ["userA", "userB"],
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  const invokeTrigger = (
    beforeStatus: string,
    afterStatus: string,
    extraFields: { before?: Record<string, unknown>; after?: Record<string, unknown> } = {}
  ) => {
    const beforeData = {
      id: "settle1",
      groupId: "group1",
      fromUserId: "userA",
      toUserId: "userB",
      amountCents: 2000,
      currency: "EUR",
      status: beforeStatus,
      ...extraFields.before,
    };
    const afterData = {
      id: "settle1",
      groupId: "group1",
      fromUserId: "userA",
      toUserId: "userB",
      amountCents: 2000,
      currency: "EUR",
      status: afterStatus,
      ...extraFields.after,
    };

    return (onSettlementUpdated as unknown as { run: (event: unknown) => Promise<void> }).run({
      data: {
        before: { data: () => beforeData },
        after: { data: () => afterData },
      },
      params: {
        groupId: "group1",
        settlementId: "settle1",
      },
    });
  };

  it("skips notification if status has not changed", async () => {
    await invokeTrigger("SUGGESTED", "SUGGESTED");
    expect(getGroupDataMock).not.toHaveBeenCalled();
  });

  it("skips notification if group is missing or being deleted", async () => {
    getGroupDataMock.mockResolvedValue({ ...sampleGroup, deletionRequested: true });

    await invokeTrigger("SUGGESTED", "CONFIRMED_BY_PAYER");

    expect(getUserDeviceTokensMock).not.toHaveBeenCalled();
  });

  it("handles SUGGESTED -> CONFIRMED_BY_PAYER (Payer paid, notifies Payee)", async () => {
    getGroupDataMock.mockResolvedValue(sampleGroup);
    getActorDisplayNameMock.mockResolvedValue("Alice");
    getUserDeviceTokensMock.mockResolvedValue(["tokenB"]);

    await invokeTrigger("SUGGESTED", "CONFIRMED_BY_PAYER");

    expect(getUserDeviceTokensMock).toHaveBeenCalledWith("userB");
    expect(getActorDisplayNameMock).toHaveBeenCalledWith("userA");
    expect(sendDataMessageMock).toHaveBeenCalledWith(
      ["tokenB"],
      expect.objectContaining({
        type: NotificationType.SETTLEMENT_REQUEST,
        groupId: "group1",
        groupName: "Trip to Paris",
        memberName: "Alice",
        actorName: "Alice",
        entityId: "settle1",
        formattedAmount: "20,00\u00A0€",
      }),
      expect.objectContaining({
        title: "Trip to Paris",
        titleLocKey: "notification_settlement_request_title",
        bodyLocKey: "notification_settlement_request_body",
        bodyLocArgs: ["Alice", "20,00\u00A0€"],
        channelId: NotificationChannelId.FINANCIAL,
      })
    );
  });

  it("handles CONFIRMED_BY_PAYER -> RESOLVED (Payee confirmed receipt, notifies Payer)", async () => {
    getGroupDataMock.mockResolvedValue(sampleGroup);
    getActorDisplayNameMock.mockResolvedValue("Bob");
    getUserDeviceTokensMock.mockResolvedValue(["tokenA"]);

    await invokeTrigger("CONFIRMED_BY_PAYER", "RESOLVED");

    expect(getUserDeviceTokensMock).toHaveBeenCalledWith("userA");
    expect(getActorDisplayNameMock).toHaveBeenCalledWith("userB");
    expect(sendDataMessageMock).toHaveBeenCalledWith(
      ["tokenA"],
      expect.objectContaining({
        type: NotificationType.SETTLEMENT_CONFIRMED,
        groupId: "group1",
        groupName: "Trip to Paris",
        memberName: "Bob",
        actorName: "Bob",
        entityId: "settle1",
        formattedAmount: "20,00\u00A0€",
      }),
      expect.objectContaining({
        title: "Trip to Paris",
        titleLocKey: "notification_settlement_confirmed_title",
        bodyLocKey: "notification_settlement_confirmed_body",
        bodyLocArgs: ["Bob", "20,00\u00A0€"],
        channelId: NotificationChannelId.FINANCIAL,
      })
    );
  });

  it("handles transition to DISPUTED (notifies counterparty)", async () => {
    getGroupDataMock.mockResolvedValue(sampleGroup);
    getActorDisplayNameMock.mockResolvedValue("Bob");
    getUserDeviceTokensMock.mockResolvedValue(["tokenA"]);

    await invokeTrigger("SUGGESTED", "DISPUTED", { after: { disputedBy: "userB" } });

    expect(getUserDeviceTokensMock).toHaveBeenCalledWith("userA");
    expect(getActorDisplayNameMock).toHaveBeenCalledWith("userB");
    expect(sendDataMessageMock).toHaveBeenCalledWith(
      ["tokenA"],
      expect.objectContaining({
        type: NotificationType.SETTLEMENT_DISPUTED,
        groupId: "group1",
        groupName: "Trip to Paris",
        memberName: "Bob",
        actorName: "Bob",
        entityId: "settle1",
        formattedAmount: "20,00\u00A0€",
      }),
      expect.objectContaining({
        title: "Trip to Paris",
        titleLocKey: "notification_settlement_disputed_title",
        bodyLocKey: "notification_settlement_disputed_body",
        bodyLocArgs: ["Bob", "20,00\u00A0€"],
        channelId: NotificationChannelId.FINANCIAL,
      })
    );
  });

  it("skips sending if target user has no registered device tokens", async () => {
    getGroupDataMock.mockResolvedValue(sampleGroup);
    getUserDeviceTokensMock.mockResolvedValue([]);

    await invokeTrigger("SUGGESTED", "CONFIRMED_BY_PAYER");

    expect(sendDataMessageMock).not.toHaveBeenCalled();
  });
});
