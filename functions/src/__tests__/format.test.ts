import { buildDeepLink, formatAmount } from "../utils/format";

describe("buildDeepLink", () => {
  it("builds a group-level deep link", () => {
    const link = buildDeepLink("group123");
    expect(link).toBe("splittrip://groups/group123");
  });

  it("builds a deep link with sub-path", () => {
    const link = buildDeepLink("group123", "expenses/exp456");
    expect(link).toBe("splittrip://groups/group123/expenses/exp456");
  });

  it("handles empty path", () => {
    const link = buildDeepLink("group123", "");
    // Empty string is falsy → no path appended
    expect(link).toBe("splittrip://groups/group123");
  });
});

describe("formatAmount", () => {
  it("formats standard 2-decimal currency (EUR in es-ES and en-US)", () => {
    const es = formatAmount(5000, "EUR", "es-ES");
    expect(es).toBe("50,00\u00A0€");
    const en = formatAmount(5000, "EUR", "en-US");
    expect(en).toBe("€50.00");
  });

  it("formats disambiguated $ symbols (USD, MXN, CAD)", () => {
    const usdEn = formatAmount(5000, "USD", "en-US");
    expect(usdEn).toBe("US$50.00");
    const mxnEn = formatAmount(5000, "MXN", "en-US");
    expect(mxnEn).toBe("MX$50.00");
    const cadEn = formatAmount(5000, "CAD", "en-US");
    expect(cadEn).toBe("CA$50.00");
    const usdEs = formatAmount(5000, "USD", "es-ES");
    expect(usdEs).toBe("50,00\u00A0US$");
    const mxnEs = formatAmount(5000, "MXN", "es-ES");
    expect(mxnEs).toBe("50,00\u00A0MX$");
  });

  it("formats 0-decimal currency without decimal truncation (JPY)", () => {
    const jpyEs = formatAmount(1000, "JPY", "es-ES");
    expect(jpyEs).toContain("1000");
    expect(jpyEs).toContain("¥");
    expect(jpyEs).not.toContain(",00");
    expect(jpyEs).not.toContain(".00");

    const jpyEn = formatAmount(1000, "JPY", "en-US");
    expect(jpyEn).toBe("¥1,000");
  });

  it("formats 3-decimal currency (KWD)", () => {
    const kwdEs = formatAmount(10500, "KWD", "es-ES");
    expect(kwdEs).toBe("10,500\u00A0KWD");
    const kwdEn = formatAmount(10500, "KWD", "en-US");
    expect(kwdEn).toBe("KWD\u00A010.500");
  });

  it("returns empty string for invalid or missing inputs", () => {
    expect(formatAmount(undefined, "EUR")).toBe("");
    expect(formatAmount(null as unknown as undefined, "EUR")).toBe("");
    expect(formatAmount("", "EUR")).toBe("");
    expect(formatAmount("invalid", "EUR")).toBe("");
    expect(formatAmount(5000, "")).toBe("");
    expect(formatAmount(5000, undefined)).toBe("");
    expect(formatAmount(5000, "INVALID_CURRENCY_CODE")).toBe("");
  });
});
