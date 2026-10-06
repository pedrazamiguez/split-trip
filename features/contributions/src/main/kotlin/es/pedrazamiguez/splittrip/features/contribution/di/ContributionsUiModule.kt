package es.pedrazamiguez.splittrip.features.contribution.di

import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.core.designsystem.navigation.TabGraphContributor
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.FormattingHelper
import es.pedrazamiguez.splittrip.core.designsystem.presentation.mapper.UserUiMapper
import es.pedrazamiguez.splittrip.core.designsystem.presentation.screen.ScreenUiProvider
import es.pedrazamiguez.splittrip.core.logging.TelemetryTracker
import es.pedrazamiguez.splittrip.domain.service.AppConfigService
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.ContributionValidationService
import es.pedrazamiguez.splittrip.domain.usecase.balance.AddContributionUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.DeleteContributionUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetContributionUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetGroupContributionsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.UpdateContributionUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.GetGroupByIdUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.ObserveGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.subunit.GetGroupSubunitsUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.GetMemberProfilesUseCase
import es.pedrazamiguez.splittrip.features.contribution.navigation.impl.ContributionsTabGraphContributorImpl
import es.pedrazamiguez.splittrip.features.contribution.presentation.mapper.AddContributionUiMapper
import es.pedrazamiguez.splittrip.features.contribution.presentation.mapper.ContributionDetailUiMapper
import es.pedrazamiguez.splittrip.features.contribution.presentation.screen.impl.AddContributionScreenUiProviderImpl
import es.pedrazamiguez.splittrip.features.contribution.presentation.screen.impl.ContributionDetailScreenUiProviderImpl
import es.pedrazamiguez.splittrip.features.contribution.presentation.viewmodel.AddContributionViewModel
import es.pedrazamiguez.splittrip.features.contribution.presentation.viewmodel.ContributionDetailViewModel
import es.pedrazamiguez.splittrip.features.contribution.presentation.viewmodel.handler.ContributionConfigHandler
import es.pedrazamiguez.splittrip.features.contribution.presentation.viewmodel.handler.ContributionSubmitHandler
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module

val contributionsUiModule = module {

    single {
        AddContributionUiMapper(
            localeProvider = get<LocaleProvider>(),
            userUiMapper = get<UserUiMapper>()
        )
    }

    single {
        ContributionDetailUiMapper(
            formattingHelper = get<FormattingHelper>(),
            resourceProvider = get<ResourceProvider>(),
            userUiMapper = get<UserUiMapper>()
        )
    }

    // ── AddContribution ViewModel with co-created handlers ──────────
    // Handlers are created inside the viewModel block so the SAME instances
    // are shared between the ViewModel and any cross-handler references.

    viewModel {
        val addContributionUiMapper = get<AddContributionUiMapper>()
        val contributionValidationService = get<ContributionValidationService>()
        val appConfigService = get<AppConfigService>()
        val telemetryTracker = get<TelemetryTracker>()

        val contributionConfigHandler = ContributionConfigHandler(
            getGroupByIdUseCase = get<GetGroupByIdUseCase>(),
            getGroupSubunitsUseCase = get<GetGroupSubunitsUseCase>(),
            getMemberProfilesUseCase = get<GetMemberProfilesUseCase>(),
            getContributionUseCase = get<GetContributionUseCase>(),
            authenticationService = get<AuthenticationService>(),
            addContributionUiMapper = addContributionUiMapper,
            appConfigService = appConfigService
        )

        val contributionSubmitHandler = ContributionSubmitHandler(
            addContributionUseCase = get<AddContributionUseCase>(),
            updateContributionUseCase = get<UpdateContributionUseCase>(),
            contributionValidationService = contributionValidationService,
            groupCurrencyProvider = { contributionConfigHandler.groupCurrency },
            telemetryTracker = telemetryTracker
        )

        AddContributionViewModel(
            configHandler = contributionConfigHandler,
            submitHandler = contributionSubmitHandler,
            contributionValidationService = contributionValidationService,
            addContributionUiMapper = addContributionUiMapper
        )
    }

    viewModel {
        val contributionDetailUiMapper = get<ContributionDetailUiMapper>()
        ContributionDetailViewModel(
            getGroupContributionsFlowUseCase = get<GetGroupContributionsFlowUseCase>(),
            observeGroupUseCase = get<ObserveGroupUseCase>(),
            getMemberProfilesUseCase = get<GetMemberProfilesUseCase>(),
            getGroupSubunitsUseCase = get<GetGroupSubunitsUseCase>(),
            deleteContributionUseCase = get<DeleteContributionUseCase>(),
            authenticationService = get<AuthenticationService>(),
            contributionDetailUiMapper = contributionDetailUiMapper
        )
    }

    factory { ContributionsTabGraphContributorImpl() } bind TabGraphContributor::class
    single { AddContributionScreenUiProviderImpl() } bind ScreenUiProvider::class
    single { ContributionDetailScreenUiProviderImpl() } bind ScreenUiProvider::class
}
