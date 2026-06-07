package es.pedrazamiguez.splittrip.data.remote.di

import es.pedrazamiguez.splittrip.data.remote.BuildConfig
import es.pedrazamiguez.splittrip.data.remote.api.OpenExchangeRatesApi
import es.pedrazamiguez.splittrip.data.remote.datasource.impl.RemoteCurrencyDataSourceImpl
import es.pedrazamiguez.splittrip.domain.datasource.remote.RemoteCurrencyDataSource
import es.pedrazamiguez.splittrip.logging.LogTag
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber

val dataRemoteModule = module {

    single {
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            Timber.tag(LogTag.NETWORK).d(message)
        }.apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.BASIC
            }
        }

        Retrofit
            .Builder()
            .baseUrl(BuildConfig.OER_API_BASE_URL)
            .client(
                OkHttpClient
                    .Builder()
                    .addInterceptor(loggingInterceptor)
                    .build()
            )
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
