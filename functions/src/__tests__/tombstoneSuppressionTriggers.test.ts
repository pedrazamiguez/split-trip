import { onContributionAdded } from "../triggers/onContributionAdded";
import { onCashWithdrawal } from "../triggers/onCashWithdrawal";
import { getGroupData, getActorDisplayName } from "../services/firestore.service";
import { sendDataMessage } from "../services/notification.service";

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

describe("Tombstone creation suppression triggers", () => {
  const getGroupDataMock = getGroupData as jest.Mock;
  const getActorDisplayNameMock = getActorDisplayName as jest.Mock;
  const sendDataMessageMock = sendDataMessage as jest.Mock;

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("onContributionAdded suppresses notification when isDeleted is true", async () => {
    await (onContributionAdded as unknown as { run: (event: unknown) => Promise<void> }).run({
      data: {
        data: () => ({
          contributionId: "c1",
          groupId: "group1",
          userId: "userA",
          amountCents: 1000,
          currency: "EUR",
          createdBy: "userA",
          isDeleted: true,
        }),
      },
      params: { groupId: "group1", contributionId: "c1" },
    });

    expect(getGroupDataMock).not.toHaveBeenCalled();
    expect(getActorDisplayNameMock).not.toHaveBeenCalled();
    expect(sendDataMessageMock).not.toHaveBeenCalled();
  });

  it("onCashWithdrawal suppresses notification when isDeleted is true", async () => {
    await (onCashWithdrawal as unknown as { run: (event: unknown) => Promise<void> }).run({
      data: {
        data: () => ({
          withdrawalId: "w1",
          groupId: "group1",
          withdrawnBy: "userA",
          amountWithdrawn: 5000,
          currency: "EUR",
          createdBy: "userA",
          isDeleted: true,
        }),
      },
      params: { groupId: "group1", withdrawalId: "w1" },
    });

    expect(getGroupDataMock).not.toHaveBeenCalled();
    expect(getActorDisplayNameMock).not.toHaveBeenCalled();
    expect(sendDataMessageMock).not.toHaveBeenCalled();
  });
});
