import { onExpenseCreated } from "../triggers/onExpenseCreated";
import { onExpenseUpdated } from "../triggers/onExpenseUpdated";
import { onExpenseDeleted } from "../triggers/onExpenseDeleted";
import { getGroupData, getActorDisplayName } from "../services/firestore.service";
import { getRecipientTokens } from "../services/token.service";
import { sendDataMessage } from "../services/notification.service";
import { NotificationType } from "../types";

jest.mock("firebase-admin", () => ({
  initializeApp: jest.fn(),
}));

jest.mock("../services/firestore.service", () => ({
  getGroupData: jest.fn(),
  getActorDisplayName: jest.fn(),
}));

jest.mock("../services/token.service", () => ({
  getRecipientTokens: jest.fn(),
}));

jest.mock("../services/notification.service", () => ({
  sendDataMessage: jest.fn(),
}));

describe("Expense lifecycle tombstone triggers", () => {
  const getGroupDataMock = getGroupData as jest.Mock;
  const getActorDisplayNameMock = getActorDisplayName as jest.Mock;
  const getRecipientTokensMock = getRecipientTokens as jest.Mock;
  const sendDataMessageMock = sendDataMessage as jest.Mock;

  const sampleGroup = {
    groupId: "group1",
    name: "Trip to Lisbon",
    currency: "EUR",
    memberIds: ["userA", "userB"],
  };

  const baseExpense = {
    expenseId: "exp1",
    groupId: "group1",
    title: "Dinner",
    amountCents: 2500,
    currency: "EUR",
    groupCurrency: "EUR",
    groupAmountCents: 2500,
    expenseCategory: "FOOD",
    paymentMethod: "CREDIT_CARD",
    paymentStatus: "FINISHED",
    payerType: "GROUP",
    splitType: "EQUAL",
    createdBy: "userA",
    lastUpdatedBy: "userA",
  };

  beforeEach(() => {
    jest.clearAllMocks();
    getGroupDataMock.mockResolvedValue(sampleGroup);
    getActorDisplayNameMock.mockResolvedValue("Alice");
    getRecipientTokensMock.mockResolvedValue(["tokenB"]);
  });

  const runCreated = (data: Record<string, unknown>) =>
    (onExpenseCreated as unknown as { run: (event: unknown) => Promise<void> }).run({
      data: { data: () => data },
      params: { groupId: "group1", expenseId: "exp1" },
    });

  const runUpdated = (
    beforeData: Record<string, unknown>,
    afterData: Record<string, unknown>
  ) =>
    (onExpenseUpdated as unknown as { run: (event: unknown) => Promise<void> }).run({
      data: {
        before: { data: () => beforeData },
        after: { data: () => afterData },
      },
      params: { groupId: "group1", expenseId: "exp1" },
    });

  const runDeleted = (data: Record<string, unknown>) =>
    (onExpenseDeleted as unknown as { run: (event: unknown) => Promise<void> }).run({
      data: { data: () => data },
      params: { groupId: "group1", expenseId: "exp1" },
    });

  it("onExpenseCreated suppresses notification when isDeleted is true", async () => {
    await runCreated({ ...baseExpense, isDeleted: true });

    expect(getGroupDataMock).not.toHaveBeenCalled();
    expect(sendDataMessageMock).not.toHaveBeenCalled();
  });

  it("onExpenseCreated sends EXPENSE_ADDED for normal active expense", async () => {
    await runCreated({ ...baseExpense, isDeleted: false });

    expect(sendDataMessageMock).toHaveBeenCalledTimes(1);
    expect(sendDataMessageMock).toHaveBeenCalledWith(
      ["tokenB"],
      expect.objectContaining({
        type: NotificationType.EXPENSE_ADDED,
        entityId: "exp1",
      }),
      expect.any(Object)
    );
  });

  it("onExpenseUpdated emits EXPENSE_DELETED (not EXPENSE_UPDATED) when isDeleted transitions from false/undefined to true", async () => {
    await runUpdated(
      { ...baseExpense },
      { groupId: "group1", isDeleted: true, lastUpdatedBy: "userA" }
    );

    expect(sendDataMessageMock).toHaveBeenCalledTimes(1);
    expect(sendDataMessageMock).toHaveBeenCalledWith(
      ["tokenB"],
      expect.objectContaining({
        type: NotificationType.EXPENSE_DELETED,
        entityId: "exp1",
        expenseTitle: "Dinner",
      }),
      expect.objectContaining({
        titleLocKey: "notification_expense_deleted_title",
      })
    );
  });

  it("onExpenseUpdated skips notification when both before and after have isDeleted === true", async () => {
    await runUpdated(
      { ...baseExpense, isDeleted: true, title: "Old" },
      { ...baseExpense, isDeleted: true, title: "New" }
    );

    expect(getGroupDataMock).not.toHaveBeenCalled();
    expect(sendDataMessageMock).not.toHaveBeenCalled();
  });

  it("onExpenseUpdated skips notification on metadata-only serverUpdatedAt change", async () => {
    await runUpdated(
      { ...baseExpense, serverUpdatedAt: 100 },
      { ...baseExpense, serverUpdatedAt: 200 }
    );

    expect(getGroupDataMock).not.toHaveBeenCalled();
    expect(sendDataMessageMock).not.toHaveBeenCalled();
  });

  it("onExpenseUpdated emits EXPENSE_UPDATED when substantive field changes on active expense", async () => {
    await runUpdated(
      { ...baseExpense, title: "Dinner" },
      { ...baseExpense, title: "Late Dinner" }
    );

    expect(sendDataMessageMock).toHaveBeenCalledTimes(1);
    expect(sendDataMessageMock).toHaveBeenCalledWith(
      ["tokenB"],
      expect.objectContaining({
        type: NotificationType.EXPENSE_UPDATED,
        expenseTitle: "Late Dinner",
      }),
      expect.any(Object)
    );
  });

  it("onExpenseDeleted suppresses notification when deleted document already had isDeleted === true (TTL purge)", async () => {
    await runDeleted({ ...baseExpense, isDeleted: true });

    expect(getGroupDataMock).not.toHaveBeenCalled();
    expect(sendDataMessageMock).not.toHaveBeenCalled();
  });

  it("onExpenseDeleted still emits EXPENSE_DELETED for legacy hard-delete where isDeleted is not true", async () => {
    await runDeleted({ ...baseExpense });

    expect(sendDataMessageMock).toHaveBeenCalledTimes(1);
    expect(sendDataMessageMock).toHaveBeenCalledWith(
      ["tokenB"],
      expect.objectContaining({
        type: NotificationType.EXPENSE_DELETED,
        entityId: "exp1",
      }),
      expect.any(Object)
    );
  });
});
