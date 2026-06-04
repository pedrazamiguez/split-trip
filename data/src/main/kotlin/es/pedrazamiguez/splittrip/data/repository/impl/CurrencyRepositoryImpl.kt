package es.pedrazamiguez.splittrip.data.repository.impl

import es.pedrazamiguez.splittrip.domain.datasource.local.LocalCurrencyDataSource
import es.pedrazamiguez.splittrip.domain.datasource.remote.RemoteCurrencyDataSource
import es.pedrazamiguez.splittrip.domain.exception.ApiKeyNotConfiguredException
import es.pedrazamiguez.splittrip.domain.model.Currency
import es.pedrazamiguez.splittrip.domain.repository.CurrencyRepository
import es.pedrazamiguez.splittrip.domain.result.ExchangeRateResult
import java.time.Duration
import java.time.Instant
import timber.log.Timber

class CurrencyRepositoryImpl(
    private val localDataSource: LocalCurrencyDataSource,
    private val remoteDataSource: RemoteCurrencyDataSource,
    private val cacheDuration: Duration
) : CurrencyRepository {

    override suspend fun getCurrencies(forceRefresh: Boolean): List<Currency> {
        if (!forceRefresh) {
            val local = localDataSource.getCurrencies()
            if (local.isNotEmpty()) {
                return local
            }
        }
        return try {
            val remote = remoteDataSource.fetchCurrencies()
            localDataSource.saveCurrencies(remote)
            remote
        } catch (ignored: ApiKeyNotConfiguredException) {
            Timber.w("Failed to fetch currencies: API key is not configured (placeholder is being used).")
            localDataSource.getCurrencies()
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch currencies")
            localDataSource.getCurrencies()
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
                } catch (ignored: ApiKeyNotConfiguredException) {
                    Timber.w("Failed to fetch exchange rates: API key is not configured (placeholder is being used).")
                    ExchangeRateResult.Empty
                } catch (e: Exception) {
                    Timber.e(
                        e,
                        "Failed to fetch exchange rates for baseCurrencyCode=%s (no local cache)",
                        baseCurrencyCode
                    )
                    ExchangeRateResult.Empty
                }
            }

            isStale -> {
                try {
                    val remoteRates = remoteDataSource.fetchExchangeRates(baseCurrencyCode)
                    localDataSource.saveExchangeRates(remoteRates)
                    ExchangeRateResult.Fresh(remoteRates)
                } catch (ignored: ApiKeyNotConfiguredException) {
                    Timber.w("Failed to fetch exchange rates: API key is not configured (placeholder is being used).")
                    ExchangeRateResult.Stale(localRates)
                } catch (e: Exception) {
                    Timber.w(
                        e,
                        "Failed to refresh exchange rates for baseCurrencyCode=%s" +
                            " (lastUpdated=%s, cacheDuration=%s); using stale cache",
                        baseCurrencyCode,
                        lastUpdated?.let { timestamp -> Instant.ofEpochSecond(timestamp).toString() },
                        cacheDuration
                    )
                    ExchangeRateResult.Stale(localRates)
                }
            }

            else -> ExchangeRateResult.Fresh(localRates)
        }
    }
}
