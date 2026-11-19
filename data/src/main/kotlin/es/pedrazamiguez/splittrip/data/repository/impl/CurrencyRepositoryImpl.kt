package es.pedrazamiguez.splittrip.data.repository.impl

import es.pedrazamiguez.splittrip.domain.datasource.local.LocalCurrencyDataSource
import es.pedrazamiguez.splittrip.domain.datasource.remote.RemoteCurrencyDataSource
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

        val hasLocalFallback = localRates.exchangeRates.isNotEmpty()
        if (hasLocalFallback && !isStale) {
            return ExchangeRateResult.Fresh(localRates)
        }

        return try {
            val remoteRates = remoteDataSource.fetchExchangeRates(baseCurrencyCode)
            localDataSource.saveExchangeRates(remoteRates)
            ExchangeRateResult.Fresh(remoteRates)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logExchangeRateFailure(
                exception = e,
                baseCurrencyCode = baseCurrencyCode,
                hasStaleFallback = hasLocalFallback,
                lastUpdated = lastUpdated
            )
            if (hasLocalFallback) {
                ExchangeRateResult.Stale(localRates)
            } else {
                ExchangeRateResult.Empty
            }
        }
    }

    private fun logExchangeRateFailure(
        exception: Throwable,
        baseCurrencyCode: String,
        hasStaleFallback: Boolean,
        lastUpdated: Long?
    ) {
        if (hasStaleFallback) {
            val formattedLastUpdated = lastUpdated?.let { timestamp -> Instant.ofEpochSecond(timestamp).toString() }
            Timber.w(
                exception,
                "Remote network or unexpected error refreshing exchange rates for baseCurrencyCode=%s " +
                    "(lastUpdated=%s, cacheDuration=%s); using stale cache",
                baseCurrencyCode,
                formattedLastUpdated,
                cacheDuration
            )
        } else {
            Timber.e(
                exception,
                "Remote network or unexpected error fetching exchange rates for baseCurrencyCode=%s; " +
                    "no local cache fallback available",
                baseCurrencyCode
            )
        }
    }
}
