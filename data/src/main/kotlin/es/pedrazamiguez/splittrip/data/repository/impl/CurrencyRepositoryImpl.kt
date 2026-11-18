package es.pedrazamiguez.splittrip.data.repository.impl

import es.pedrazamiguez.splittrip.domain.datasource.local.LocalCurrencyDataSource
import es.pedrazamiguez.splittrip.domain.datasource.remote.RemoteCurrencyDataSource
import es.pedrazamiguez.splittrip.domain.exception.ApiKeyNotConfiguredException
import es.pedrazamiguez.splittrip.domain.exception.RemoteCurrencyApiException
import es.pedrazamiguez.splittrip.domain.model.Currency
import es.pedrazamiguez.splittrip.domain.repository.CurrencyRepository
import es.pedrazamiguez.splittrip.domain.result.ExchangeRateResult
import java.time.Duration
import java.time.Instant
import kotlin.coroutines.cancellation.CancellationException
import timber.log.Timber

class CurrencyRepositoryImpl(
    private val localDataSource: LocalCurrencyDataSource,
    private val remoteDataSource: RemoteCurrencyDataSource,
    private val cacheDuration: Duration
) : CurrencyRepository {

    override suspend fun getCurrencies(forceRefresh: Boolean): List<Currency> {
        val local = if (!forceRefresh) localDataSource.getCurrencies() else null
        if (local != null && local.isNotEmpty()) {
            return local
        }
        return try {
            val remote = remoteDataSource.fetchCurrencies()
            localDataSource.saveCurrencies(remote)
            remote
        } catch (e: CancellationException) {
            throw e
        } catch (ignored: ApiKeyNotConfiguredException) {
            Timber.w("Failed to fetch currencies: API key is not configured or placeholder is being used.")
            local ?: localDataSource.getCurrencies()
        } catch (e: RemoteCurrencyApiException) {
            Timber.e(e, formatHttpFailureDetail(e.httpCode, "currencies"))
            local ?: localDataSource.getCurrencies()
        } catch (e: Exception) {
            Timber.e(e, "Remote network or unexpected error fetching currencies")
            local ?: localDataSource.getCurrencies()
        }
    }

    override suspend fun getExchangeRates(baseCurrencyCode: String): ExchangeRateResult {
        val localRates = localDataSource.getExchangeRates(baseCurrencyCode)
        val lastUpdated = localDataSource.getLastUpdated(baseCurrencyCode)

        val isStale = lastUpdated == null ||
            Instant
                .ofEpochSecond(lastUpdated)
                .isBefore(
                    Instant
                        .now()
                        .minus(cacheDuration)
                )

        return when {
            localRates.exchangeRates.isEmpty() -> {
                try {
                    val remoteRates = remoteDataSource.fetchExchangeRates(baseCurrencyCode)
                    localDataSource.saveExchangeRates(remoteRates)
                    ExchangeRateResult.Fresh(remoteRates)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logExchangeRateFailure(
                        exception = e,
                        baseCurrencyCode = baseCurrencyCode,
                        hasStaleFallback = false,
                        lastUpdated = lastUpdated
                    )
                    ExchangeRateResult.Empty
                }
            }

            isStale -> {
                try {
                    val remoteRates = remoteDataSource.fetchExchangeRates(baseCurrencyCode)
                    localDataSource.saveExchangeRates(remoteRates)
                    ExchangeRateResult.Fresh(remoteRates)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logExchangeRateFailure(
                        exception = e,
                        baseCurrencyCode = baseCurrencyCode,
                        hasStaleFallback = true,
                        lastUpdated = lastUpdated
                    )
                    ExchangeRateResult.Stale(localRates)
                }
            }

            else -> ExchangeRateResult.Fresh(localRates)
        }
    }

    private fun logExchangeRateFailure(
        exception: Throwable,
        baseCurrencyCode: String,
        hasStaleFallback: Boolean,
        lastUpdated: Long?
    ) {
        if (exception is ApiKeyNotConfiguredException) {
            val message = if (hasStaleFallback) {
                "Failed to refresh exchange rates for baseCurrencyCode=%s: " +
                    "API key is not configured or placeholder is being used; using stale cache."
            } else {
                "Failed to fetch exchange rates for baseCurrencyCode=%s: " +
                    "API key is not configured or placeholder is being used (no local cache fallback available)."
            }
            Timber.w(message, baseCurrencyCode)
            return
        }

        val detail = when (exception) {
            is RemoteCurrencyApiException -> formatHttpFailureDetail(exception.httpCode, "exchange rates")
            else -> if (hasStaleFallback) {
                "Remote network or unexpected error refreshing exchange rates"
            } else {
                "Remote network or unexpected error fetching exchange rates"
            }
        }
        if (hasStaleFallback) {
            val formattedLastUpdated = lastUpdated?.let { timestamp -> Instant.ofEpochSecond(timestamp).toString() }
            Timber.w(
                exception,
                "%s for baseCurrencyCode=%s (lastUpdated=%s, cacheDuration=%s); using stale cache",
                detail,
                baseCurrencyCode,
                formattedLastUpdated,
                cacheDuration
            )
        } else {
            Timber.e(
                exception,
                "%s for baseCurrencyCode=%s; no local cache fallback available",
                detail,
                baseCurrencyCode
            )
        }
    }

    private fun formatHttpFailureDetail(httpCode: Int, target: String): String {
        return if (httpCode in AUTH_HTTP_CODES) {
            "Remote API rejected $target request (HTTP $httpCode; check OER_APP_ID)"
        } else {
            "Remote API request failed for $target (HTTP $httpCode)"
        }
    }

    companion object {
        private val AUTH_HTTP_CODES = setOf(401, 403)
    }
}
