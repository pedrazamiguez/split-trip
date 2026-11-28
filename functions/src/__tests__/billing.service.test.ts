import {
  CloudBillingClientLike,
  CloudBillingService,
  formatProjectResourceName,
} from "../services/billing.service";

jest.mock("firebase-functions/v2", () => ({
  logger: {
    info: jest.fn(),
    warn: jest.fn(),
    error: jest.fn(),
  },
}));

describe("formatProjectResourceName", () => {
  it("returns projects/<id> for valid project ID and trims whitespace", () => {
    expect(formatProjectResourceName(" expshapp ")).toBe("projects/expshapp");
  });

  it("throws for blank project ID", () => {
    expect(() => formatProjectResourceName("   ")).toThrow(
      "Project ID must not be empty"
    );
  });
});

describe("CloudBillingService", () => {
  let mockClient: jest.Mocked<CloudBillingClientLike>;
  let service: CloudBillingService;

  beforeEach(() => {
    jest.clearAllMocks();
    mockClient = {
      getProjectBillingInfo: jest.fn(),
      updateProjectBillingInfo: jest.fn(),
    };
    service = new CloudBillingService(mockClient);
  });

  it("isBillingEnabled returns true when billingEnabled is true and billingAccountName is non-empty", async () => {
    mockClient.getProjectBillingInfo.mockResolvedValue([
      {
        billingEnabled: true,
        billingAccountName: "billingAccounts/TEST-ACCT",
      },
    ]);

    const result = await service.isBillingEnabled("test-proj");

    expect(result).toBe(true);
    expect(mockClient.getProjectBillingInfo).toHaveBeenCalledWith({
      name: "projects/test-proj",
    });
  });

  it("isBillingEnabled returns false when billingEnabled is false", async () => {
    mockClient.getProjectBillingInfo.mockResolvedValue([
      {
        billingEnabled: false,
        billingAccountName: "billingAccounts/TEST-ACCT",
      },
    ]);

    const result = await service.isBillingEnabled("test-proj");

    expect(result).toBe(false);
  });

  it("isBillingEnabled returns false when billingAccountName is empty even if billingEnabled is true", async () => {
    mockClient.getProjectBillingInfo.mockResolvedValue([
      {
        billingEnabled: true,
        billingAccountName: "   ",
      },
    ]);

    const result = await service.isBillingEnabled("test-proj");

    expect(result).toBe(false);
  });

  it("disableBilling calls updateProjectBillingInfo with empty billingAccountName", async () => {
    mockClient.updateProjectBillingInfo.mockResolvedValue([
      {
        billingEnabled: false,
        billingAccountName: "",
      },
    ]);

    await service.disableBilling("test-proj");

    expect(mockClient.updateProjectBillingInfo).toHaveBeenCalledWith({
      name: "projects/test-proj",
      projectBillingInfo: { billingAccountName: "" },
    });
  });

  it("disableBilling throws when API reports billingEnabled still true after unlink", async () => {
    mockClient.updateProjectBillingInfo.mockResolvedValue([
      {
        billingEnabled: true,
        billingAccountName: "billingAccounts/TEST-ACCT",
      },
    ]);

    await expect(service.disableBilling("test-proj")).rejects.toThrow(
      "Billing remained enabled"
    );
  });

  it("propagates Cloud Billing API errors from getProjectBillingInfo and updateProjectBillingInfo", async () => {
    const apiError = new Error("PERMISSION_DENIED");
    mockClient.getProjectBillingInfo.mockRejectedValue(apiError);
    mockClient.updateProjectBillingInfo.mockRejectedValue(apiError);

    await expect(service.isBillingEnabled("test-proj")).rejects.toThrow(
      "PERMISSION_DENIED"
    );
    await expect(service.disableBilling("test-proj")).rejects.toThrow(
      "PERMISSION_DENIED"
    );
  });
});
