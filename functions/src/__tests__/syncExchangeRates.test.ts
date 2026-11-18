import { processExchangeRatesSync } from "../triggers/scheduled/syncExchangeRates";
import * as admin from "firebase-admin";

jest.mock("firebase-admin", () => {
  const firestoreMock = Object.assign(jest.fn(), {
    Timestamp: {
      fromDate: jest.fn((date: Date) => ({
        toDate: () => date,
        seconds: Math.floor(date.getTime() / 1000),
      })),
    },
  });

  return {
    firestore: firestoreMock,
    initializeApp: jest.fn(),
  };
});

describe("processExchangeRatesSync", () => {
  let latestSetMock: jest.Mock;
  let currenciesGetMock: jest.Mock;
  let currenciesSetMock: jest.Mock;
  let collectionMock: jest.Mock;
  let fetchMock: jest.Mock;

  const fixedNow = new Date("2026-11-18T12:00:00Z");

  beforeEach(() => {
    jest.clearAllMocks();

    latestSetMock = jest.fn().mockResolvedValue(undefined);
    currenciesGetMock = jest.fn();
    currenciesSetMock = jest.fn().mockResolvedValue(undefined);

    const docMock = jest.fn((docId: string) => {
      if (docId === "latest") {
        return {
          set: latestSetMock,
        };
      }
      if (docId === "currencies") {
        return {
          get: currenciesGetMock,
          set: currenciesSetMock,
        };
      }
      throw new Error(`Unexpected docId: ${docId}`);
    });

    collectionMock = jest.fn().mockReturnValue({
      doc: docMock,
    });

    (admin.firestore as unknown as jest.Mock).mockReturnValue({
      collection: collectionMock,
    });

    fetchMock = jest.fn();
    global.fetch = fetchMock as unknown as typeof fetch;
  });

  it("fetches latest.json and writes exchange_rates/latest with stringified rates and updatedAt, and fetches currencies.json when currencies doc does not exist", async () => {
    fetchMock
      .mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => ({
          base: "USD",
          timestamp: 1795003200,
          rates: {
            EUR: 0.923456,
            JPY: 154.12,
          },
        }),
      })
      .mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => ({
          USD: "United States Dollar",
          EUR: "Euro",
        }),
      });

    currenciesGetMock.mockResolvedValue({
      exists: false,
      data: () => undefined,
    });

    await processExchangeRatesSync("test-oer-key", fixedNow);

    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(fetchMock).toHaveBeenNthCalledWith(
      1,
      "https://openexchangerates.org/api/latest.json?app_id=test-oer-key&base=USD"
    );
    expect(fetchMock).toHaveBeenNthCalledWith(
      2,
      "https://openexchangerates.org/api/currencies.json?app_id=test-oer-key"
    );

    expect(collectionMock).toHaveBeenCalledWith("exchange_rates");
    expect(latestSetMock).toHaveBeenCalledTimes(1);
    expect(latestSetMock).toHaveBeenCalledWith({
      base: "USD",
      timestamp: 1795003200,
      rates: {
        EUR: "0.923456",
        JPY: "154.12",
      },
      updatedAt: expect.objectContaining({
        seconds: Math.floor(fixedNow.getTime() / 1000),
      }),
    });

    expect(currenciesSetMock).toHaveBeenCalledTimes(1);
    expect(currenciesSetMock).toHaveBeenCalledWith({
      currencies: {
        USD: "United States Dollar",
        EUR: "Euro",
      },
      updatedAt: expect.objectContaining({
        seconds: Math.floor(fixedNow.getTime() / 1000),
      }),
    });
  });

  it("skips fetching currencies.json when exchange_rates/currencies exists and was updated less than 24 hours ago", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({
        base: "USD",
        timestamp: 1795003200,
        rates: {
          EUR: 0.92,
        },
      }),
    });

    const twelveHoursAgo = new Date(fixedNow.getTime() - 12 * 60 * 60 * 1000);
    currenciesGetMock.mockResolvedValue({
      exists: true,
      data: () => ({
        currencies: {
          USD: "United States Dollar",
          EUR: "Euro",
        },
        updatedAt: {
          toDate: () => twelveHoursAgo,
        },
      }),
    });

    await processExchangeRatesSync("test-oer-key", fixedNow);

    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(latestSetMock).toHaveBeenCalledTimes(1);
    expect(currenciesSetMock).not.toHaveBeenCalled();
  });

  it("refreshes exchange_rates/currencies when existing currencies doc is 24 hours or older", async () => {
    fetchMock
      .mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => ({
          base: "USD",
          timestamp: 1795003200,
          rates: {
            EUR: 0.92,
          },
        }),
      })
      .mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => ({
          USD: "United States Dollar",
          EUR: "Euro",
        }),
      });

    const twentyFiveHoursAgo = new Date(
      fixedNow.getTime() - 25 * 60 * 60 * 1000
    );
    currenciesGetMock.mockResolvedValue({
      exists: true,
      data: () => ({
        currencies: {
          USD: "United States Dollar",
        },
        updatedAt: {
          toDate: () => twentyFiveHoursAgo,
        },
      }),
    });

    await processExchangeRatesSync("test-oer-key", fixedNow);

    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(latestSetMock).toHaveBeenCalledTimes(1);
    expect(currenciesSetMock).toHaveBeenCalledTimes(1);
  });

  it("refreshes exchange_rates/currencies when existing currencies doc has empty currencies map", async () => {
    fetchMock
      .mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => ({
          base: "USD",
          timestamp: 1795003200,
          rates: {
            EUR: 0.92,
          },
        }),
      })
      .mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => ({
          USD: "United States Dollar",
        }),
      });

    const oneHourAgo = new Date(fixedNow.getTime() - 60 * 60 * 1000);
    currenciesGetMock.mockResolvedValue({
      exists: true,
      data: () => ({
        currencies: {},
        updatedAt: {
          toDate: () => oneHourAgo,
        },
      }),
    });

    await processExchangeRatesSync("test-oer-key", fixedNow);

    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(currenciesSetMock).toHaveBeenCalledTimes(1);
  });

  it("filters out invalid currency codes and non-finite or negative rates from latest.json", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({
        base: "USD",
        timestamp: 1795003200,
        rates: {
          EUR: 0.92,
          invalid: 1.5,
          NEG: -0.5,
          NAN: NaN,
        },
      }),
    });

    const oneHourAgo = new Date(fixedNow.getTime() - 60 * 60 * 1000);
    currenciesGetMock.mockResolvedValue({
      exists: true,
      data: () => ({
        currencies: { USD: "United States Dollar" },
        updatedAt: { toDate: () => oneHourAgo },
      }),
    });

    await processExchangeRatesSync("test-oer-key", fixedNow);

    expect(latestSetMock).toHaveBeenCalledWith(
      expect.objectContaining({
        rates: {
          EUR: "0.92",
        },
      })
    );
  });

  it("throws error and does not call fetch when appId is blank", async () => {
    await expect(processExchangeRatesSync("   ", fixedNow)).rejects.toThrow(
      "OER_APP_ID secret is not configured"
    );

    expect(fetchMock).not.toHaveBeenCalled();
    expect(latestSetMock).not.toHaveBeenCalled();
  });

  it("throws error when latest.json responds with non-OK HTTP status", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: false,
      status: 401,
      statusText: "Unauthorized",
    });

    await expect(
      processExchangeRatesSync("test-oer-key", fixedNow)
    ).rejects.toThrow("OER latest.json failed with HTTP 401");

    expect(latestSetMock).not.toHaveBeenCalled();
    expect(currenciesSetMock).not.toHaveBeenCalled();
  });

  it("throws error when currencies.json responds with non-OK HTTP status after writing latest", async () => {
    fetchMock
      .mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => ({
          base: "USD",
          timestamp: 1795003200,
          rates: {
            EUR: 0.92,
          },
        }),
      })
      .mockResolvedValueOnce({
        ok: false,
        status: 500,
        statusText: "Internal Server Error",
      });

    currenciesGetMock.mockResolvedValue({
      exists: false,
      data: () => undefined,
    });

    await expect(
      processExchangeRatesSync("test-oer-key", fixedNow)
    ).rejects.toThrow("OER currencies.json failed with HTTP 500");

    expect(latestSetMock).toHaveBeenCalledTimes(1);
    expect(currenciesSetMock).not.toHaveBeenCalled();
  });
});
