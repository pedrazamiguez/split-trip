import * as functions from "firebase-functions/v2";
import { logger } from "firebase-functions/v2";
import { defineSecret } from "firebase-functions/params";
import * as admin from "firebase-admin";
import {
  CurrenciesDoc,
  ExchangeRatesDoc,
  OerCurrenciesResponse,
  OerLatestResponse,
} from "../../types";

const oerAppId = defineSecret("OER_APP_ID");

const EXCHANGE_RATES_COLLECTION = "exchange_rates";
const LATEST_DOC_ID = "latest";
const CURRENCIES_DOC_ID = "currencies";
const OER_BASE_URL = "https://openexchangerates.org/api";
const CURRENCIES_REFRESH_INTERVAL_MS = 24 * 60 * 60 * 1000;
const ISO_CURRENCY_CODE_REGEX = /^[A-Z]{3}$/;

function shouldRefreshCurrenciesDoc(
  snapshot: FirebaseFirestore.DocumentSnapshot,
  now: Date
): boolean {
  if (!snapshot.exists) {
    return true;
  }

  const existingData = snapshot.data() as Partial<CurrenciesDoc> | undefined;
  const existingCurrencies = existingData?.currencies;
  if (
    !existingCurrencies ||
    typeof existingCurrencies !== "object" ||
    Object.keys(existingCurrencies).length === 0
  ) {
    return true;
  }

  const existingUpdatedAt = existingData?.updatedAt;
  if (
    !existingUpdatedAt ||
    typeof existingUpdatedAt.toDate !== "function"
  ) {
    return true;
  }

  const elapsedMs = now.getTime() - existingUpdatedAt.toDate().getTime();
  return elapsedMs >= CURRENCIES_REFRESH_INTERVAL_MS;
}

export async function processExchangeRatesSync(
  appId: string,
  now: Date = new Date()
): Promise<void> {
  const trimmedAppId = appId.trim();
  if (!trimmedAppId) {
    logger.error("OER_APP_ID secret is empty or not configured");
    throw new Error("OER_APP_ID secret is not configured");
  }

  const db = admin.firestore();
  const encodedAppId = encodeURIComponent(trimmedAppId);

  const latestUrl = `${OER_BASE_URL}/latest.json?app_id=${encodedAppId}&base=USD`;
  const latestResponse = await fetch(latestUrl);
  if (!latestResponse.ok) {
    logger.error("Failed to fetch latest exchange rates from OER", {
      status: latestResponse.status,
      statusText: latestResponse.statusText,
    });
    throw new Error(`OER latest.json failed with HTTP ${latestResponse.status}`);
  }

  const latestData = (await latestResponse.json()) as OerLatestResponse;
  const rawRates =
    latestData && typeof latestData.rates === "object" && latestData.rates !== null
      ? latestData.rates
      : {};

  const stringRates: Record<string, string> = {};
  for (const [code, numericRate] of Object.entries(rawRates)) {
    if (
      ISO_CURRENCY_CODE_REGEX.test(code) &&
      typeof numericRate === "number" &&
      Number.isFinite(numericRate) &&
      numericRate >= 0
    ) {
      stringRates[code] = numericRate.toString();
    }
  }

  if (Object.keys(stringRates).length === 0) {
    throw new Error("OER latest.json returned no valid exchange rates");
  }

  const exchangeRatesDoc: ExchangeRatesDoc = {
    base:
      typeof latestData.base === "string" &&
      ISO_CURRENCY_CODE_REGEX.test(latestData.base)
        ? latestData.base
        : "USD",
    timestamp:
      typeof latestData.timestamp === "number" &&
      Number.isFinite(latestData.timestamp)
        ? latestData.timestamp
        : Math.floor(now.getTime() / 1000),
    rates: stringRates,
    updatedAt: admin.firestore.Timestamp.fromDate(now),
  };

  await db
    .collection(EXCHANGE_RATES_COLLECTION)
    .doc(LATEST_DOC_ID)
    .set(exchangeRatesDoc);

  const currenciesRef = db
    .collection(EXCHANGE_RATES_COLLECTION)
    .doc(CURRENCIES_DOC_ID);
  const currenciesSnapshot = await currenciesRef.get();

  if (shouldRefreshCurrenciesDoc(currenciesSnapshot, now)) {
    const currenciesUrl = `${OER_BASE_URL}/currencies.json?app_id=${encodedAppId}`;
    const currenciesResponse = await fetch(currenciesUrl);
    if (!currenciesResponse.ok) {
      logger.error("Failed to fetch currencies list from OER", {
        status: currenciesResponse.status,
        statusText: currenciesResponse.statusText,
      });
      throw new Error(
        `OER currencies.json failed with HTTP ${currenciesResponse.status}`
      );
    }

    const currenciesData =
      (await currenciesResponse.json()) as OerCurrenciesResponse;
    const rawCurrencies =
      currenciesData &&
      typeof currenciesData === "object" &&
      currenciesData !== null
        ? currenciesData
        : {};

    const validCurrencies: Record<string, string> = {};
    for (const [code, name] of Object.entries(rawCurrencies)) {
      if (
        ISO_CURRENCY_CODE_REGEX.test(code) &&
        typeof name === "string" &&
        name.trim().length > 0
      ) {
        validCurrencies[code] = name;
      }
    }

    if (Object.keys(validCurrencies).length === 0) {
      throw new Error("OER currencies.json returned no valid currencies");
    }

    const currenciesDoc: CurrenciesDoc = {
      currencies: validCurrencies,
      updatedAt: admin.firestore.Timestamp.fromDate(now),
    };

    await currenciesRef.set(currenciesDoc);
  }
}

export const syncExchangeRates = functions.scheduler.onSchedule(
  {
    schedule: "every 2 hours",
    timeZone: "UTC",
    retryCount: 3,
    secrets: [oerAppId],
  },
  async () => {
    await processExchangeRatesSync(oerAppId.value(), new Date());
  }
);
