package es.pedrazamiguez.splittrip.data.remote.datasource.impl

import es.pedrazamiguez.splittrip.data.remote.api.OpenExchangeRatesApi
import es.pedrazamiguez.splittrip.data.remote.dto.ExchangeRateResponse
import es.pedrazamiguez.splittrip.domain.exception.ApiKeyNotConfiguredException
import es.pedrazamiguez.splittrip.domain.exception.RemoteCurrencyApiException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import retrofit2.HttpException
import retrofit2.Response

class RemoteCurrencyDataSourceImplTest {

    private val api: OpenExchangeRatesApi = mockk()

    @Test
    fun `fetchExchangeRates throws ApiKeyNotConfiguredException when appId is blank`() {
        val dataSource = RemoteCurrencyDataSourceImpl(api = api, appId = "   ")

        assertThrows<ApiKeyNotConfiguredException> {
            runBlocking { dataSource.fetchExchangeRates("USD") }
        }
        coVerify(exactly = 0) { api.getExchangeRates(any(), any()) }
    }

    @Test
    fun `fetchExchangeRates throws ApiKeyNotConfiguredException when appId is placeholder`() {
        val dataSource = RemoteCurrencyDataSourceImpl(api = api, appId = "YOUR_DEBUG_OER_APP_ID")

        assertThrows<ApiKeyNotConfiguredException> {
            runBlocking { dataSource.fetchExchangeRates("USD") }
        }
        coVerify(exactly = 0) { api.getExchangeRates(any(), any()) }
    }

    @Test
    fun `fetchExchangeRates wraps HttpException 401 into RemoteCurrencyApiException with httpCode 401`() {
        val dataSource = RemoteCurrencyDataSourceImpl(api = api, appId = "valid-key")
        val httpException = createHttpException<ExchangeRateResponse>(401)
        coEvery { api.getExchangeRates("valid-key", "USD") } throws httpException

        val thrown = assertThrows<RemoteCurrencyApiException> {
            runBlocking { dataSource.fetchExchangeRates("USD") }
        }

        assertEquals(401, thrown.httpCode)
        assertInstanceOf(HttpException::class.java, thrown.cause)
    }

    @Test
    fun `fetchExchangeRates wraps HttpException 500 into RemoteCurrencyApiException with httpCode 500`() {
        val dataSource = RemoteCurrencyDataSourceImpl(api = api, appId = "valid-key")
        val httpException = createHttpException<ExchangeRateResponse>(500)
        coEvery { api.getExchangeRates("valid-key", "USD") } throws httpException

        val thrown = assertThrows<RemoteCurrencyApiException> {
            runBlocking { dataSource.fetchExchangeRates("USD") }
        }

        assertEquals(500, thrown.httpCode)
        assertInstanceOf(HttpException::class.java, thrown.cause)
    }

    @Test
    fun `fetchCurrencies wraps HttpException 401 into RemoteCurrencyApiException with httpCode 401`() {
        val dataSource = RemoteCurrencyDataSourceImpl(api = api, appId = "valid-key")
        val httpException = createHttpException<Map<String, String>>(401)
        coEvery { api.getCurrencies("valid-key") } throws httpException

        val thrown = assertThrows<RemoteCurrencyApiException> {
            runBlocking { dataSource.fetchCurrencies() }
        }

        assertEquals(401, thrown.httpCode)
        assertInstanceOf(HttpException::class.java, thrown.cause)
    }

    @Test
    fun `fetchExchangeRates propagates IOException unchanged`() {
        val dataSource = RemoteCurrencyDataSourceImpl(api = api, appId = "valid-key")
        val ioException = IOException("Connection reset")
        coEvery { api.getExchangeRates("valid-key", "USD") } throws ioException

        val thrown = assertThrows<IOException> {
            runBlocking { dataSource.fetchExchangeRates("USD") }
        }

        assertEquals("Connection reset", thrown.message)
    }

    private fun <T> createHttpException(code: Int): HttpException {
        val errorBody = """{"error":true}""".toResponseBody("application/json".toMediaType())
        return HttpException(Response.error<T>(code, errorBody))
    }
}
