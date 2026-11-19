package es.pedrazamiguez.splittrip.di.domain

import es.pedrazamiguez.splittrip.domain.repository.UserPreferenceRepository
import es.pedrazamiguez.splittrip.domain.service.AppConfigService
import es.pedrazamiguez.splittrip.domain.service.AppUpdatePolicyService
import es.pedrazamiguez.splittrip.domain.service.impl.AppUpdatePolicyServiceImpl
import es.pedrazamiguez.splittrip.domain.usecase.update.RecordOptionalUpdateDismissalUseCase
import es.pedrazamiguez.splittrip.domain.usecase.update.ResolveAppUpdateDecisionUseCase
import es.pedrazamiguez.splittrip.domain.usecase.update.impl.RecordOptionalUpdateDismissalUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.update.impl.ResolveAppUpdateDecisionUseCaseImpl
import org.koin.dsl.module

val appUpdateDomainModule = module {
    single<AppUpdatePolicyService> { AppUpdatePolicyServiceImpl() }

    factory<ResolveAppUpdateDecisionUseCase> {
        val appConfigService = get<AppConfigService>()
        val userPreferenceRepository = get<UserPreferenceRepository>()
        val appUpdatePolicyService = get<AppUpdatePolicyService>()
        ResolveAppUpdateDecisionUseCaseImpl(
            appConfigService = appConfigService,
            userPreferenceRepository = userPreferenceRepository,
            appUpdatePolicyService = appUpdatePolicyService
        )
    }

    factory<RecordOptionalUpdateDismissalUseCase> {
        val userPreferenceRepository = get<UserPreferenceRepository>()
        RecordOptionalUpdateDismissalUseCaseImpl(
            userPreferenceRepository = userPreferenceRepository
        )
    }
}
