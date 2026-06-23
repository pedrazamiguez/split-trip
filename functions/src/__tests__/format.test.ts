import { buildDeepLink } from "../utils/format";

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
