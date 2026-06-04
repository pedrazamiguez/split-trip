package es.pedrazamiguez.splittrip.data.remote.datasource.impl

import es.pedrazamiguez.splittrip.data.remote.api.OpenExchangeRatesApi
import es.pedrazamiguez.splittrip.data.remote.mapper.CurrencyDtoMapper
import es.pedrazamiguez.splittrip.domain.datasource.remote.RemoteCurrencyDataSource
import es.pedrazamiguez.splittrip.domain.exception.ApiKeyNotConfiguredException
import es.pedrazamiguez.splittrip.domain.model.Currency
import es.pedrazamiguez.splittrip.domain.model.ExchangeRates

class RemoteCurrencyDataSourceImpl(private val api: OpenExchangeRatesApi, private val appId: String) :
    RemoteCurrencyDataSource {

    override suspend fun fetchCurrencies(): List<Currency> {
        validateApiKey()
        val response = api.getCurrencies(appId)
        return CurrencyDtoMapper.mapCurrencies(response)
    }

    override suspend fun fetchExchangeRates(baseCurrencyCode: String): ExchangeRates {
        validateApiKey()
        val response = api.getExchangeRates(
            appId,
            baseCurrencyCode
        )
        return CurrencyDtoMapper.mapExchangeRates(response)
    }

    private fun validateApiKey() {
        if (appId.isBlank() || appId == PLACEHOLDER_APP_ID) {
            throw ApiKeyNotConfiguredException("API key is not configured.")
        }
    }

    companion object {
        private const val PLACEHOLDER_APP_ID = "YOUR_DEBUG_OER_APP_ID"
    }
}
