import { CloudBillingClient } from "@google-cloud/billing";
import { logger } from "firebase-functions/v2";

export interface BillingClientAdapter {
  isBillingEnabled(projectId: string): Promise<boolean>;
  disableBilling(projectId: string): Promise<void>;
}

export interface CloudBillingClientLike {
  getProjectBillingInfo(request: {
    name: string;
  }): Promise<
    [
      { billingEnabled?: boolean | null; billingAccountName?: string | null },
      ...unknown[],
    ]
  >;
  updateProjectBillingInfo(request: {
    name: string;
    projectBillingInfo: { billingAccountName: string };
  }): Promise<
    [
      { billingEnabled?: boolean | null; billingAccountName?: string | null },
      ...unknown[],
    ]
  >;
}

/**
 * Formats a GCP project ID into the resource name expected by Cloud Billing API.
 */
export function formatProjectResourceName(projectId: string): string {
  const trimmed = projectId.trim();
  if (!trimmed) {
    throw new Error("Project ID must not be empty");
  }
  return `projects/${trimmed}`;
}

/**
 * Wraps @google-cloud/billing behind BillingClientAdapter so billing state
 * checks and emergency unlinking can be tested cleanly without network calls.
 */
export class CloudBillingService implements BillingClientAdapter {
  constructor(
    private readonly client: CloudBillingClientLike = new CloudBillingClient()
  ) {}

  async isBillingEnabled(projectId: string): Promise<boolean> {
    const [billingInfo] = await this.client.getProjectBillingInfo({
      name: formatProjectResourceName(projectId),
    });
    return (
      billingInfo.billingEnabled === true &&
      Boolean(billingInfo.billingAccountName?.trim())
    );
  }

  async disableBilling(projectId: string): Promise<void> {
    const [updatedInfo] = await this.client.updateProjectBillingInfo({
      name: formatProjectResourceName(projectId),
      projectBillingInfo: { billingAccountName: "" },
    });

    const billingEnabled = updatedInfo.billingEnabled === true;
    logger.info("Updated project billing status via Cloud Billing API", {
      projectId: projectId.trim(),
      billingEnabled,
    });

    if (billingEnabled) {
      throw new Error(
        `Billing remained enabled after unlink request for project ${projectId}`
      );
    }
  }
}
