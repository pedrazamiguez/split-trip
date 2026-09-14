/**
 * Formatting utilities for FCM notification payloads.
 */

/**
 * Builds a deep link URI for in-app navigation.
 *
 * @param groupId  - The group ID
 * @param path     - Optional sub-path (e.g. "expenses/exp_123")
 * @returns Deep link string like "splittrip://groups/abc123/expenses/exp_456"
 */
export function buildDeepLink(groupId: string, path?: string): string {
  const base = `splittrip://groups/${groupId}`;
  return path ? `${base}/${path}` : base;
}

const DISAMBIGUATED_SYMBOLS: Record<string, string> = {
  USD: "US$",
  MXN: "MX$",
  CAD: "CA$",
  AUD: "AU$",
  COP: "CO$",
  CLP: "CL$",
  ARS: "AR$",
  EUR: "€",
  GBP: "£",
  JPY: "¥",
};

/**
 * Formats a raw integer amount in atomic units (cents) into a localized currency string.
 *
 * Dynamically resolves ISO 4217 fraction digits (0, 2, or 3) and disambiguates
 * currency symbols (e.g., USD -> US$, MXN -> MX$) to align with Android AmountFormatter.
 *
 * @param amountCents  - The amount in currency's smallest unit (e.g. 5000 for 50.00 EUR)
 * @param currencyCode - The ISO 4217 currency code (e.g. "EUR", "USD", "JPY", "KWD")
 * @param locale       - The target locale for formatting (defaults to "es-ES")
 * @returns Formatted currency string or empty string if input is invalid
 */
export function formatAmount(
  amountCents?: string | number,
  currencyCode?: string,
  locale?: string
): string {
  if (
    amountCents === undefined ||
    amountCents === null ||
    amountCents === "" ||
    !currencyCode ||
    typeof currencyCode !== "string" ||
    currencyCode.trim() === ""
  ) {
    return "";
  }

  const numericAmount = Number(amountCents);
  if (isNaN(numericAmount)) {
    return "";
  }

  const normalizedCurrency = currencyCode.trim().toUpperCase();

  try {
    const fractionDigits =
      new Intl.NumberFormat("en-US", {
        style: "currency",
        currency: normalizedCurrency,
      }).resolvedOptions().maximumFractionDigits ?? 2;

    const divisor = Math.pow(10, fractionDigits);
    const value = numericAmount / divisor;

    const formatter = new Intl.NumberFormat(locale ?? "es-ES", {
      style: "currency",
      currency: normalizedCurrency,
      minimumFractionDigits: fractionDigits,
      maximumFractionDigits: fractionDigits,
    });

    const parts = formatter.formatToParts(value);
    const targetSymbol = DISAMBIGUATED_SYMBOLS[normalizedCurrency];

    if (targetSymbol) {
      return parts
        .map((part) => {
          if (part.type === "currency") {
            if (part.value === normalizedCurrency || part.value === "$") {
              return targetSymbol;
            }
          }
          return part.value;
        })
        .join("");
    }

    return formatter.format(value);
  } catch {
    return "";
  }
}
