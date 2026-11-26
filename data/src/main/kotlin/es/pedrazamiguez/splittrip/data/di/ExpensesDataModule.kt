package es.pedrazamiguez.splittrip.data.di

import androidx.work.WorkManager
import com.google.ai.edge.aicore.GenerativeModel
import com.google.ai.edge.aicore.generationConfig
import es.pedrazamiguez.splittrip.core.performance.PerformanceMonitor
import es.pedrazamiguez.splittrip.data.local.datastore.LocalAiModelPackPreferences
import es.pedrazamiguez.splittrip.data.local.datastore.UserPreferences
import es.pedrazamiguez.splittrip.data.repository.impl.AICoreInferenceRepositoryImpl
import es.pedrazamiguez.splittrip.data.repository.impl.ExpenseRepositoryImpl
import es.pedrazamiguez.splittrip.data.repository.impl.LiteRtEngineClient
import es.pedrazamiguez.splittrip.data.repository.impl.LiteRtInferenceRepositoryImpl
import es.pedrazamiguez.splittrip.data.repository.impl.LiteRtLmEngineClientImpl
import es.pedrazamiguez.splittrip.data.service.AICoreCapabilityProvider
import es.pedrazamiguez.splittrip.data.service.AiModelResolverImpl
import es.pedrazamiguez.splittrip.data.service.ExpenseCategoryAiClassifierServiceImpl
import es.pedrazamiguez.splittrip.data.service.HttpModelPackDownloader
import es.pedrazamiguez.splittrip.data.service.LocalAiDeviceCompatibilityProvider
import es.pedrazamiguez.splittrip.data.service.LocalAiModelPackServiceImpl
import es.pedrazamiguez.splittrip.data.service.MLKitOcrService
import es.pedrazamiguez.splittrip.data.service.ModelPackDownloader
import es.pedrazamiguez.splittrip.data.service.ModelPackStorageManager
import es.pedrazamiguez.splittrip.data.service.ReceiptExtractionServiceImpl
import es.pedrazamiguez.splittrip.data.sync.SyncTeardownCoordinator
import es.pedrazamiguez.splittrip.data.worker.LocalAiModelPackWorkScheduler
import es.pedrazamiguez.splittrip.data.worker.WorkManagerLocalAiModelPackScheduler
import es.pedrazamiguez.splittrip.domain.datasource.cloud.CloudExpenseDataSource
import es.pedrazamiguez.splittrip.domain.datasource.cloud.CloudStorageDataSource
import es.pedrazamiguez.splittrip.domain.datasource.local.LocalExpenseDataSource
import es.pedrazamiguez.splittrip.domain.datasource.local.LocalGroupDataSource
import es.pedrazamiguez.splittrip.domain.repository.AiInferenceRepository
import es.pedrazamiguez.splittrip.domain.repository.AppConfigRepository
import es.pedrazamiguez.splittrip.domain.repository.ExpenseRepository
import es.pedrazamiguez.splittrip.domain.service.AiModelResolverService
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.ExpenseCategoryAiClassifierService
import es.pedrazamiguez.splittrip.domain.service.LocalAiModelPackService
import es.pedrazamiguez.splittrip.domain.service.ReceiptExtractionService
import es.pedrazamiguez.splittrip.domain.service.ReceiptOcrService
import es.pedrazamiguez.splittrip.domain.service.ReceiptStorageService
import es.pedrazamiguez.splittrip.domain.service.RemainderDistributionService
import es.pedrazamiguez.splittrip.domain.service.featuregate.FeatureGateService
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

val expensesDataModule = module {

    single<ExpenseRepository> {
        ExpenseRepositoryImpl(
            cloudExpenseDataSource = get<CloudExpenseDataSource>(),
            localExpenseDataSource = get<LocalExpenseDataSource>(),
            authenticationService = get<AuthenticationService>(),
            cloudStorageDataSource = get<CloudStorageDataSource>(),
            receiptStorageService = get<ReceiptStorageService>(),
            performanceMonitor = get<PerformanceMonitor>(),
            localGroupDataSource = get<LocalGroupDataSource>(),
            syncTeardownCoordinator = get<SyncTeardownCoordinator>(),
            remainderDistributionService = get<RemainderDistributionService>(),
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

    single<AiInferenceRepository>(named("ai_core")) {
        AICoreInferenceRepositoryImpl(
            generativeModel = get<GenerativeModel>(),
            defaultDispatcher = Dispatchers.Default
        )
    }

    single<LocalAiDeviceCompatibilityProvider> {
        LocalAiDeviceCompatibilityProvider(context = androidContext())
    }

    single<ModelPackStorageManager> {
        ModelPackStorageManager(baseDir = androidContext().filesDir)
    }

    single<ModelPackDownloader> { HttpModelPackDownloader() }

    single<LocalAiModelPackWorkScheduler> {
        WorkManagerLocalAiModelPackScheduler(workManager = get<WorkManager>())
    }

    single<LiteRtEngineClient> {
        LiteRtLmEngineClientImpl(context = androidContext())
    }

    single<LocalAiModelPackService> {
        val appConfigRepository = get<AppConfigRepository>()
        val featureGateService = get<FeatureGateService>()
        val localAiDeviceCompatibilityProvider = get<LocalAiDeviceCompatibilityProvider>()
        val modelPackStorageManager = get<ModelPackStorageManager>()
        val localAiModelPackPreferences = get<LocalAiModelPackPreferences>()
        val localAiModelPackWorkScheduler = get<LocalAiModelPackWorkScheduler>()
        LocalAiModelPackServiceImpl(
            appConfigRepository = appConfigRepository,
            featureGateService = featureGateService,
            compatibilityProvider = localAiDeviceCompatibilityProvider,
            storageManager = modelPackStorageManager,
            preferences = localAiModelPackPreferences,
            workScheduler = localAiModelPackWorkScheduler,
            engineClient = lazy { get<LiteRtEngineClient>() },
            ioDispatcher = Dispatchers.IO
        )
    }

    single<AiInferenceRepository>(named("lite_rt")) {
        val localAiModelPackService = get<LocalAiModelPackService>()
        val featureGateService = get<FeatureGateService>()
        val liteRtEngineClient = get<LiteRtEngineClient>()
        LiteRtInferenceRepositoryImpl(
            modelPackService = localAiModelPackService,
            featureGateService = featureGateService,
            engineClient = liteRtEngineClient,
            defaultDispatcher = Dispatchers.Default
        )
    }

    single<AiModelResolverService> {
        val userPreferences = get<UserPreferences>()
        val localAiModelPackService = get<LocalAiModelPackService>()
        val featureGateService = get<FeatureGateService>()
        val aiCoreCapabilityProvider = get<AICoreCapabilityProvider>()
        AiModelResolverImpl(
            userPreferences = userPreferences,
            localAiModelPackService = localAiModelPackService,
            featureGateService = featureGateService,
            aiCoreCapabilityProvider = aiCoreCapabilityProvider
        )
    }

    single<ReceiptExtractionService> {
        val appConfigRepository = get<AppConfigRepository>()
        val aiCoreCapabilityProvider = get<AICoreCapabilityProvider>()
        val aiModelResolverService = get<AiModelResolverService>()
        val localAiModelPackService = get<LocalAiModelPackService>()
        ReceiptExtractionServiceImpl(
            context = androidContext(),
            appConfigRepository = appConfigRepository,
            aiCoreCapabilityProvider = aiCoreCapabilityProvider,
            aiCoreInferenceRepository = lazy { get<AiInferenceRepository>(named("ai_core")) },
            liteRtInferenceRepository = lazy { get<AiInferenceRepository>(named("lite_rt")) },
            aiModelResolver = aiModelResolverService,
            localAiModelPackService = localAiModelPackService,
            defaultDispatcher = Dispatchers.Default
        )
    }

    single<ExpenseCategoryAiClassifierService> {
        val aiCoreCapabilityProvider = get<AICoreCapabilityProvider>()
        val aiModelResolverService = get<AiModelResolverService>()
        val localAiModelPackService = get<LocalAiModelPackService>()
        ExpenseCategoryAiClassifierServiceImpl(
            aiCoreCapabilityProvider = aiCoreCapabilityProvider,
            aiCoreInferenceRepository = lazy { get<AiInferenceRepository>(named("ai_core")) },
            liteRtInferenceRepository = lazy { get<AiInferenceRepository>(named("lite_rt")) },
            aiModelResolver = aiModelResolverService,
            localAiModelPackService = localAiModelPackService,
            defaultDispatcher = Dispatchers.Default
        )
    }
}

private const val AICORE_MAX_OUTPUT_TOKENS = 150
private const val AICORE_TEMPERATURE = 0.7f
