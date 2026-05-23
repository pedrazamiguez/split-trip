package es.pedrazamiguez.splittrip.data.di

import com.google.ai.edge.aicore.GenerativeModel
import com.google.ai.edge.aicore.generationConfig
import es.pedrazamiguez.splittrip.data.repository.impl.ExpenseRepositoryImpl
import es.pedrazamiguez.splittrip.data.service.AICoreCapabilityProvider
import es.pedrazamiguez.splittrip.data.service.AICoreReceiptParser
import es.pedrazamiguez.splittrip.data.service.MLKitOcrService
import es.pedrazamiguez.splittrip.data.service.ReceiptExtractionServiceImpl
import es.pedrazamiguez.splittrip.domain.datasource.cloud.CloudExpenseDataSource
import es.pedrazamiguez.splittrip.domain.datasource.cloud.CloudStorageDataSource
import es.pedrazamiguez.splittrip.domain.datasource.local.LocalExpenseDataSource
import es.pedrazamiguez.splittrip.domain.repository.ExpenseRepository
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.ReceiptExtractionService
import es.pedrazamiguez.splittrip.domain.service.ReceiptOcrService
import es.pedrazamiguez.splittrip.domain.service.ReceiptStorageService
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val expensesDataModule = module {

    single<ExpenseRepository> {
        ExpenseRepositoryImpl(
            cloudExpenseDataSource = get<CloudExpenseDataSource>(),
            localExpenseDataSource = get<LocalExpenseDataSource>(),
            authenticationService = get<AuthenticationService>(),
            cloudStorageDataSource = get<CloudStorageDataSource>(),
            receiptStorageService = get<ReceiptStorageService>(),
            ioDispatcher = Dispatchers.IO
        )
    }

    single<ReceiptOcrService> {
        MLKitOcrService(
            context = androidContext(),
            defaultDispatcher = Dispatchers.Default
        )
    }

    single<AICoreCapabilityProvider> {
        AICoreCapabilityProvider(context = androidContext())
    }

    single<GenerativeModel> {
        GenerativeModel(
            generationConfig = generationConfig {
                context = androidContext().applicationContext
                temperature = AICORE_TEMPERATURE
                maxOutputTokens = AICORE_MAX_OUTPUT_TOKENS
            }
        )
    }

    single<AICoreReceiptParser> {
        AICoreReceiptParser(
            generativeModel = get<GenerativeModel>(),
            defaultDispatcher = Dispatchers.Default
        )
    }

    single<ReceiptExtractionService> {
        ReceiptExtractionServiceImpl(
            aiCoreCapabilityProvider = get<AICoreCapabilityProvider>(),
            aiCoreReceiptParser = lazy { get<AICoreReceiptParser>() },
            defaultDispatcher = Dispatchers.Default
        )
    }
}

private const val AICORE_MAX_OUTPUT_TOKENS = 150
private const val AICORE_TEMPERATURE = 0.7f
