import { logger } from "firebase-functions/v2";
import { expr, projectID } from "firebase-functions/params";
import { onMessagePublished } from "firebase-functions/v2/pubsub";
import {
  BillingClientAdapter,
  CloudBillingService,
} from "../services/billing.service";
import {
  BillingBudgetNotification,
  BillingKillSwitchConfig,
  BillingKillSwitchResult,
} from "../types";

export const BILLING_BUDGET_ALERTS_TOPIC = "billing-budget-alerts";
export const BILLING_KILL_SWITCH_SERVICE_ACCOUNT_ID = "billing-kill-switch";

// Built-in Firebase projectID param resolves at deploy time without prompting CI.
const billingKillSwitchServiceAccount = expr`${BILLING_KILL_SWITCH_SERVICE_ACCOUNT_ID}@${projectID}.iam.gserviceaccount.com`;

/**
 * Parses a boolean environment flag, falling back to defaultValue if unset or invalid.
 */
export function parseBooleanFlag(
  rawValue: string | undefined,
  defaultValue: boolean
): boolean {
  if (rawValue === undefined) {
    return defaultValue;
  }
  const normalized = rawValue.trim().toLowerCase();
  if (normalized === "true") {
    return true;
  }
  if (normalized === "false") {
    return false;
  }
  return defaultValue;
}

/**
 * Resolves billing kill-switch configuration with fail-safe defaults
 * (enabled=false, dryRun=true) when environment variables are omitted.
 */
export function resolveBillingKillSwitchConfig(
  env: NodeJS.ProcessEnv = process.env,
  fallbackProjectId?: string
): BillingKillSwitchConfig {
  const projectId = (
    env.GCLOUD_PROJECT ??
    env.GOOGLE_CLOUD_PROJECT ??
    fallbackProjectId ??
    ""
  ).trim();

  return {
    projectId,
    enabled: parseBooleanFlag(env.BILLING_KILL_SWITCH_ENABLED, false),
    dryRun: parseBooleanFlag(env.BILLING_KILL_SWITCH_DRY_RUN, true),
    emergencyBudgetDisplayName: (
      env.BILLING_EMERGENCY_BUDGET_NAME ?? ""
    ).trim(),
  };
}

function tryExtractPayloadObject(message: {
  data?: string;
  json?: unknown;
}): unknown {
  try {
    if (message.json !== undefined && message.json !== null) {
      return message.json;
    }
  } catch {
    // Message.json getter throws if data is not valid JSON; fall through to base64 parse.
  }

  if (typeof message.data === "string" && message.data.trim().length > 0) {
    try {
      const decoded = Buffer.from(message.data, "base64").toString("utf8");
      return JSON.parse(decoded) as unknown;
    } catch {
      return null;
    }
  }

  return null;
}

/**
 * Parses and validates a Cloud Billing Budget Pub/Sub notification message.
 */
export function parseBillingBudgetNotification(
  message: { data?: string; json?: unknown } | undefined
): BillingBudgetNotification | null {
  if (!message) {
    return null;
  }

  const parsed = tryExtractPayloadObject(message);
  if (typeof parsed !== "object" || parsed === null || Array.isArray(parsed)) {
    return null;
  }

  const raw = parsed as Record<string, unknown>;
  const costAmount = raw.costAmount;
  const budgetAmount = raw.budgetAmount;

  if (
    typeof costAmount !== "number" ||
    !Number.isFinite(costAmount) ||
    costAmount < 0
  ) {
    return null;
  }

  if (
    typeof budgetAmount !== "number" ||
    !Number.isFinite(budgetAmount) ||
    budgetAmount <= 0
  ) {
    return null;
  }

  return {
    schemaVersion:
      typeof raw.schemaVersion === "string" ? raw.schemaVersion : undefined,
    budgetDisplayName:
      typeof raw.budgetDisplayName === "string"
        ? raw.budgetDisplayName
        : undefined,
    costAmount,
    costIntervalStart:
      typeof raw.costIntervalStart === "string"
        ? raw.costIntervalStart
        : undefined,
    budgetAmount,
    budgetAmountType:
      typeof raw.budgetAmountType === "string"
        ? raw.budgetAmountType
        : undefined,
    currencyCode:
      typeof raw.currencyCode === "string" ? raw.currencyCode : undefined,
    alertThresholdExceeded:
      typeof raw.alertThresholdExceeded === "number" &&
      Number.isFinite(raw.alertThresholdExceeded)
        ? raw.alertThresholdExceeded
        : undefined,
    forecastThresholdExceeded:
      typeof raw.forecastThresholdExceeded === "number" &&
      Number.isFinite(raw.forecastThresholdExceeded)
        ? raw.forecastThresholdExceeded
        : undefined,
  };
}

/**
 * Evaluates a Cloud Billing Budget alert and unlinks billing only when the
 * configured emergency hard-cap budget is breached in live mode.
 */
export async function handleBillingBudgetAlert(
  notification: BillingBudgetNotification | null,
  config: BillingKillSwitchConfig,
  billingClient: BillingClientAdapter
): Promise<BillingKillSwitchResult> {
  const projectId = config.projectId.trim();
  if (!projectId) {
    logger.error("Project ID is not configured for billing kill-switch");
    throw new Error("Project ID is not configured for billing kill-switch");
  }

  if (notification === null) {
    logger.warn("Malformed or incomplete billing budget notification payload", {
      projectId,
    });
    return {
      action: "SKIPPED_INVALID_PAYLOAD",
      projectId,
      reason: "Malformed or incomplete billing budget notification payload",
    };
  }

  const baseMetadata = {
    projectId,
    budgetDisplayName: notification.budgetDisplayName,
    costAmount: notification.costAmount,
    budgetAmount: notification.budgetAmount,
    currencyCode: notification.currencyCode,
  };

  if (!config.enabled) {
    logger.info("Billing kill-switch is disabled; skipping alert", baseMetadata);
    return {
      action: "SKIPPED_DISABLED",
      ...baseMetadata,
      reason: "Billing kill-switch is disabled",
    };
  }

  const configuredBudgetName = config.emergencyBudgetDisplayName.trim();
  const incomingBudgetName = notification.budgetDisplayName?.trim() ?? "";
  if (!configuredBudgetName || incomingBudgetName !== configuredBudgetName) {
    logger.info(
      "Ignoring billing alert from non-emergency budget",
      baseMetadata
    );
    return {
      action: "SKIPPED_NON_EMERGENCY_BUDGET",
      ...baseMetadata,
      reason:
        "Alert budgetDisplayName does not match configured emergency budget",
    };
  }

  if (notification.costAmount < notification.budgetAmount) {
    logger.info(
      "Actual cost is below emergency budget cap; skipping unlink",
      baseMetadata
    );
    return {
      action: "SKIPPED_BELOW_THRESHOLD",
      ...baseMetadata,
      reason: "Actual costAmount is strictly below budgetAmount",
    };
  }

  let billingEnabled: boolean;
  try {
    billingEnabled = await billingClient.isBillingEnabled(projectId);
  } catch (error) {
    logger.error("Failed to inspect project billing status", {
      ...baseMetadata,
      errorMessage: error instanceof Error ? error.message : String(error),
    });
    throw error;
  }

  if (!billingEnabled) {
    logger.info(
      "Project billing is already disabled; no unlink required",
      baseMetadata
    );
    return {
      action: "SKIPPED_ALREADY_DISABLED",
      ...baseMetadata,
      reason: "Project billing is already disabled",
    };
  }

  if (config.dryRun) {
    logger.warn(
      "[DRY RUN] Emergency billing threshold breached; skipping billing unlink",
      baseMetadata
    );
    return {
      action: "DRY_RUN",
      ...baseMetadata,
      reason: "Dry-run mode enabled; billing unlink skipped",
    };
  }

  logger.error(
    "CRITICAL: Emergency billing budget breached; disabling project billing",
    baseMetadata
  );

  try {
    await billingClient.disableBilling(projectId);
  } catch (error) {
    logger.error("Failed to disable project billing", {
      ...baseMetadata,
      errorMessage: error instanceof Error ? error.message : String(error),
    });
    throw error;
  }

  logger.warn("Project billing account unlinked by emergency kill-switch", {
    ...baseMetadata,
  });

  return {
    action: "UNLINKED",
    ...baseMetadata,
    reason: "Emergency budget breached; project billing account unlinked",
  };
}

export const onBillingBudgetAlert =
  onMessagePublished<BillingBudgetNotification>(
    {
      topic: BILLING_BUDGET_ALERTS_TOPIC,
      serviceAccount: billingKillSwitchServiceAccount,
      maxInstances: 1,
      retry: true,
    },
    async (event) => {
      const config = resolveBillingKillSwitchConfig(
        process.env,
        projectID.value()
      );
      const notification = parseBillingBudgetNotification(event.data?.message);
      const billingService = new CloudBillingService();
      await handleBillingBudgetAlert(notification, config, billingService);
    }
  );
