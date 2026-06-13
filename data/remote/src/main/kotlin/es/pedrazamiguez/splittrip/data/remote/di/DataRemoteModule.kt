package es.pedrazamiguez.splittrip.data.remote.di

import es.pedrazamiguez.splittrip.core.logging.LogTag
import es.pedrazamiguez.splittrip.data.remote.BuildConfig
import es.pedrazamiguez.splittrip.data.remote.api.OpenExchangeRatesApi
import es.pedrazamiguez.splittrip.data.remote.datasource.impl.RemoteCurrencyDataSourceImpl
import es.pedrazamiguez.splittrip.domain.datasource.remote.RemoteCurrencyDataSource
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber

val dataRemoteModule = module {

    single {
        val okHttpClient = OkHttpClient.Builder().apply {
            if (BuildConfig.DEBUG) {
                val loggingInterceptor = HttpLoggingInterceptor { message ->
                    val redactedMessage = message.replace(Regex("app_id=[^&\\s]+"), "app_id=REDACTED")
                    Timber.tag(LogTag.NETWORK).d(redactedMessage)
                }.apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                }
                addInterceptor(loggingInterceptor)
            }
        }.build()

        Retrofit
            .Builder()
            .baseUrl(BuildConfig.OER_API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    single<OpenExchangeRatesApi> {
        get<Retrofit>().create(OpenExchangeRatesApi::class.java)
    }

    single<RemoteCurrencyDataSource> {
        RemoteCurrencyDataSourceImpl(
            api = get<OpenExchangeRatesApi>(),
            appId = BuildConfig.OER_APP_ID
        )
    }
}
