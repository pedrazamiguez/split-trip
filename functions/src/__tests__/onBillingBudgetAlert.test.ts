import { logger } from "firebase-functions/v2";
import { BillingClientAdapter } from "../services/billing.service";
import {
  handleBillingBudgetAlert,
  parseBillingBudgetNotification,
  resolveBillingKillSwitchConfig,
} from "../triggers/onBillingBudgetAlert";
import {
  BillingBudgetNotification,
  BillingKillSwitchConfig,
} from "../types";

jest.mock("firebase-functions/v2", () => ({
  logger: {
    info: jest.fn(),
    warn: jest.fn(),
    error: jest.fn(),
  },
}));

describe("resolveBillingKillSwitchConfig", () => {
  it("uses fail-safe defaults (enabled=false, dryRun=true) when env vars are unset", () => {
    const config = resolveBillingKillSwitchConfig(
      { GCLOUD_PROJECT: "test-proj" },
      "fallback-proj"
    );

    expect(config).toEqual({
      projectId: "test-proj",
      enabled: false,
      dryRun: true,
      emergencyBudgetDisplayName: "",
    });
  });

  it("parses explicit true/false strings case-insensitively with whitespace trimming", () => {
    const config = resolveBillingKillSwitchConfig({
      GCLOUD_PROJECT: " test-proj ",
      BILLING_KILL_SWITCH_ENABLED: " TRUE ",
      BILLING_KILL_SWITCH_DRY_RUN: " False ",
      BILLING_EMERGENCY_BUDGET_NAME: " SplitTrip Emergency Hard Cap ",
    });

    expect(config).toEqual({
      projectId: "test-proj",
      enabled: true,
      dryRun: false,
      emergencyBudgetDisplayName: "SplitTrip Emergency Hard Cap",
    });
  });

  it("falls back to GOOGLE_CLOUD_PROJECT and fallbackProjectId when GCLOUD_PROJECT is missing", () => {
    const fromGoogleCloudProject = resolveBillingKillSwitchConfig(
      { GOOGLE_CLOUD_PROJECT: "cloud-proj" },
      "fallback-proj"
    );
    expect(fromGoogleCloudProject.projectId).toBe("cloud-proj");

    const fromFallback = resolveBillingKillSwitchConfig({}, " fallback-proj ");
    expect(fromFallback.projectId).toBe("fallback-proj");
  });
});

describe("parseBillingBudgetNotification", () => {
  it("extracts valid notification from message.json", () => {
    const notification = parseBillingBudgetNotification({
      json: {
        schemaVersion: "1.0",
        budgetDisplayName: "SplitTrip Emergency Hard Cap",
        costAmount: 52.4,
        costIntervalStart: "2026-11-01T07:00:00Z",
        budgetAmount: 50,
        budgetAmountType: "SPECIFIED_AMOUNT",
        currencyCode: "EUR",
        alertThresholdExceeded: 1.0,
      },
    });

    expect(notification).toEqual({
      schemaVersion: "1.0",
      budgetDisplayName: "SplitTrip Emergency Hard Cap",
      costAmount: 52.4,
      costIntervalStart: "2026-11-01T07:00:00Z",
      budgetAmount: 50,
      budgetAmountType: "SPECIFIED_AMOUNT",
      currencyCode: "EUR",
      alertThresholdExceeded: 1.0,
      forecastThresholdExceeded: undefined,
    });
  });

  it("decodes valid base64 JSON payload from message.data when json is omitted", () => {
    const payload = {
      budgetDisplayName: "SplitTrip Emergency Hard Cap",
      costAmount: 50,
      budgetAmount: 50,
      currencyCode: "EUR",
    };
    const encoded = Buffer.from(JSON.stringify(payload), "utf8").toString(
      "base64"
    );

    const notification = parseBillingBudgetNotification({ data: encoded });

    expect(notification).toEqual({
      schemaVersion: undefined,
      budgetDisplayName: "SplitTrip Emergency Hard Cap",
      costAmount: 50,
      costIntervalStart: undefined,
      budgetAmount: 50,
      budgetAmountType: undefined,
      currencyCode: "EUR",
      alertThresholdExceeded: undefined,
      forecastThresholdExceeded: undefined,
    });
  });

  it("returns null for invalid base64 JSON or throwing message.json getter", () => {
    expect(
      parseBillingBudgetNotification({
        data: Buffer.from("{not-valid-json", "utf8").toString("base64"),
      })
    ).toBeNull();

    const throwingMessage = {
      get json(): unknown {
        throw new Error("Invalid JSON");
      },
      data: "bm90LWpzb24=",
    };
    expect(parseBillingBudgetNotification(throwingMessage)).toBeNull();
    expect(parseBillingBudgetNotification(undefined)).toBeNull();
  });

  it("returns null when costAmount is negative, NaN, or missing", () => {
    expect(
      parseBillingBudgetNotification({
        json: { costAmount: -1, budgetAmount: 50 },
      })
    ).toBeNull();
    expect(
      parseBillingBudgetNotification({
        json: { costAmount: Number.NaN, budgetAmount: 50 },
      })
    ).toBeNull();
    expect(
      parseBillingBudgetNotification({
        json: { budgetAmount: 50 },
      })
    ).toBeNull();
  });

  it("returns null when budgetAmount is zero, negative, Infinity, or missing", () => {
    expect(
      parseBillingBudgetNotification({
        json: { costAmount: 10, budgetAmount: 0 },
      })
    ).toBeNull();
    expect(
      parseBillingBudgetNotification({
        json: { costAmount: 10, budgetAmount: -50 },
      })
    ).toBeNull();
    expect(
      parseBillingBudgetNotification({
        json: { costAmount: 10, budgetAmount: Number.POSITIVE_INFINITY },
      })
    ).toBeNull();
    expect(
      parseBillingBudgetNotification({
        json: { costAmount: 10 },
      })
    ).toBeNull();
  });
});

describe("handleBillingBudgetAlert", () => {
  let mockBillingClient: jest.Mocked<BillingClientAdapter>;

  const baseConfig: BillingKillSwitchConfig = {
    projectId: "test-proj",
    enabled: true,
    dryRun: false,
    emergencyBudgetDisplayName: "SplitTrip Emergency Hard Cap",
  };

  const breachedNotification: BillingBudgetNotification = {
    budgetDisplayName: "SplitTrip Emergency Hard Cap",
    costAmount: 55,
    budgetAmount: 50,
    currencyCode: "EUR",
    alertThresholdExceeded: 1.0,
  };

  beforeEach(() => {
    jest.clearAllMocks();
    mockBillingClient = {
      isBillingEnabled: jest.fn(),
      disableBilling: jest.fn(),
    };
  });

  it("returns SKIPPED_INVALID_PAYLOAD without throwing or calling billing client when notification is null", async () => {
    const result = await handleBillingBudgetAlert(
      null,
      baseConfig,
      mockBillingClient
    );

    expect(result.action).toBe("SKIPPED_INVALID_PAYLOAD");
    expect(mockBillingClient.isBillingEnabled).not.toHaveBeenCalled();
    expect(mockBillingClient.disableBilling).not.toHaveBeenCalled();
    expect(logger.warn).toHaveBeenCalled();
  });

  it("returns SKIPPED_DISABLED without calling billing client when enabled is false", async () => {
    const result = await handleBillingBudgetAlert(
      breachedNotification,
      { ...baseConfig, enabled: false },
      mockBillingClient
    );

    expect(result.action).toBe("SKIPPED_DISABLED");
    expect(mockBillingClient.isBillingEnabled).not.toHaveBeenCalled();
    expect(mockBillingClient.disableBilling).not.toHaveBeenCalled();
  });

  it("returns SKIPPED_NON_EMERGENCY_BUDGET when budgetDisplayName belongs to a soft warning budget", async () => {
    const result = await handleBillingBudgetAlert(
      {
        budgetDisplayName: "SplitTrip Monthly Soft Warnings",
        costAmount: 25,
        budgetAmount: 20,
        currencyCode: "EUR",
        alertThresholdExceeded: 1.0,
      },
      baseConfig,
      mockBillingClient
    );

    expect(result.action).toBe("SKIPPED_NON_EMERGENCY_BUDGET");
    expect(mockBillingClient.isBillingEnabled).not.toHaveBeenCalled();
    expect(mockBillingClient.disableBilling).not.toHaveBeenCalled();
  });

  it("returns SKIPPED_NON_EMERGENCY_BUDGET when emergencyBudgetDisplayName config is empty", async () => {
    const result = await handleBillingBudgetAlert(
      breachedNotification,
      { ...baseConfig, emergencyBudgetDisplayName: "   " },
      mockBillingClient
    );

    expect(result.action).toBe("SKIPPED_NON_EMERGENCY_BUDGET");
    expect(mockBillingClient.isBillingEnabled).not.toHaveBeenCalled();
    expect(mockBillingClient.disableBilling).not.toHaveBeenCalled();
  });

  it("returns SKIPPED_BELOW_THRESHOLD when actual costAmount is below budgetAmount", async () => {
    const result = await handleBillingBudgetAlert(
      {
        ...breachedNotification,
        costAmount: 49.99,
        budgetAmount: 50,
      },
      baseConfig,
      mockBillingClient
    );

    expect(result.action).toBe("SKIPPED_BELOW_THRESHOLD");
    expect(mockBillingClient.isBillingEnabled).not.toHaveBeenCalled();
    expect(mockBillingClient.disableBilling).not.toHaveBeenCalled();
  });

  it("returns SKIPPED_BELOW_THRESHOLD on forecast-only breach where forecastThresholdExceeded >= 1.0 but costAmount < budgetAmount", async () => {
    const result = await handleBillingBudgetAlert(
      {
        budgetDisplayName: "SplitTrip Emergency Hard Cap",
        costAmount: 40,
        budgetAmount: 50,
        currencyCode: "EUR",
        forecastThresholdExceeded: 1.0,
      },
      baseConfig,
      mockBillingClient
    );

    expect(result.action).toBe("SKIPPED_BELOW_THRESHOLD");
    expect(mockBillingClient.isBillingEnabled).not.toHaveBeenCalled();
    expect(mockBillingClient.disableBilling).not.toHaveBeenCalled();
  });

  it("checks isBillingEnabled and returns DRY_RUN without calling disableBilling when costAmount === budgetAmount and dryRun is true", async () => {
    mockBillingClient.isBillingEnabled.mockResolvedValue(true);

    const result = await handleBillingBudgetAlert(
      {
        ...breachedNotification,
        costAmount: 50,
        budgetAmount: 50,
      },
      { ...baseConfig, dryRun: true },
      mockBillingClient
    );

    expect(result.action).toBe("DRY_RUN");
    expect(mockBillingClient.isBillingEnabled).toHaveBeenCalledWith("test-proj");
    expect(mockBillingClient.disableBilling).not.toHaveBeenCalled();
    expect(logger.warn).toHaveBeenCalled();
  });

  it("checks isBillingEnabled and unlinks billing when costAmount >= budgetAmount and dryRun is false", async () => {
    mockBillingClient.isBillingEnabled.mockResolvedValue(true);
    mockBillingClient.disableBilling.mockResolvedValue(undefined);

    const result = await handleBillingBudgetAlert(
      breachedNotification,
      baseConfig,
      mockBillingClient
    );

    expect(result).toEqual({
      action: "UNLINKED",
      projectId: "test-proj",
      budgetDisplayName: "SplitTrip Emergency Hard Cap",
      costAmount: 55,
      budgetAmount: 50,
      currencyCode: "EUR",
      reason: "Emergency budget breached; project billing account unlinked",
    });
    expect(mockBillingClient.isBillingEnabled).toHaveBeenCalledWith("test-proj");
    expect(mockBillingClient.disableBilling).toHaveBeenCalledWith("test-proj");
    expect(logger.error).toHaveBeenCalled();
  });

  it("is idempotent and returns SKIPPED_ALREADY_DISABLED without calling disableBilling when billing is already disabled (live and dry-run modes)", async () => {
    mockBillingClient.isBillingEnabled.mockResolvedValue(false);

    const liveResult = await handleBillingBudgetAlert(
      breachedNotification,
      { ...baseConfig, dryRun: false },
      mockBillingClient
    );
    const dryRunResult = await handleBillingBudgetAlert(
      breachedNotification,
      { ...baseConfig, dryRun: true },
      mockBillingClient
    );

    expect(liveResult.action).toBe("SKIPPED_ALREADY_DISABLED");
    expect(dryRunResult.action).toBe("SKIPPED_ALREADY_DISABLED");
    expect(mockBillingClient.isBillingEnabled).toHaveBeenCalledTimes(2);
    expect(mockBillingClient.disableBilling).not.toHaveBeenCalled();
  });

  it("throws when projectId is blank", async () => {
    await expect(
      handleBillingBudgetAlert(
        breachedNotification,
        { ...baseConfig, projectId: "   " },
        mockBillingClient
      )
    ).rejects.toThrow("Project ID is not configured for billing kill-switch");

    expect(mockBillingClient.isBillingEnabled).not.toHaveBeenCalled();
    expect(mockBillingClient.disableBilling).not.toHaveBeenCalled();
  });

  it("rethrows error when isBillingEnabled fails so Pub/Sub retries delivery", async () => {
    mockBillingClient.isBillingEnabled.mockRejectedValue(
      new Error("PERMISSION_DENIED")
    );

    await expect(
      handleBillingBudgetAlert(
        breachedNotification,
        baseConfig,
        mockBillingClient
      )
    ).rejects.toThrow("PERMISSION_DENIED");

    expect(mockBillingClient.disableBilling).not.toHaveBeenCalled();
    expect(logger.error).toHaveBeenCalled();
  });

  it("rethrows error when disableBilling fails so Pub/Sub retries delivery", async () => {
    mockBillingClient.isBillingEnabled.mockResolvedValue(true);
    mockBillingClient.disableBilling.mockRejectedValue(
      new Error("UNAVAILABLE")
    );

    await expect(
      handleBillingBudgetAlert(
        breachedNotification,
        baseConfig,
        mockBillingClient
      )
    ).rejects.toThrow("UNAVAILABLE");

    expect(logger.error).toHaveBeenCalled();
  });
});
