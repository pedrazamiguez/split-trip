package es.pedrazamiguez.splittrip.features.contribution.di

import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.designsystem.navigation.TabGraphContributor
import es.pedrazamiguez.splittrip.core.designsystem.presentation.mapper.UserUiMapper
import es.pedrazamiguez.splittrip.core.designsystem.presentation.screen.ScreenUiProvider
import es.pedrazamiguez.splittrip.domain.service.AppConfigService
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.ContributionValidationService
import es.pedrazamiguez.splittrip.domain.usecase.balance.AddContributionUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.GetGroupByIdUseCase
import es.pedrazamiguez.splittrip.domain.usecase.subunit.GetGroupSubunitsUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.GetMemberProfilesUseCase
import es.pedrazamiguez.splittrip.features.contribution.navigation.impl.ContributionsTabGraphContributorImpl
import es.pedrazamiguez.splittrip.features.contribution.presentation.mapper.AddContributionUiMapper
import es.pedrazamiguez.splittrip.features.contribution.presentation.screen.impl.AddContributionScreenUiProviderImpl
import es.pedrazamiguez.splittrip.features.contribution.presentation.viewmodel.AddContributionViewModel
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

    // ── AddContribution ViewModel with co-created handlers ──────────
    // Handlers are created inside the viewModel block so the SAME instances
    // are shared between the ViewModel and any cross-handler references.

    viewModel {
        val addContributionUiMapper = get<AddContributionUiMapper>()
        val contributionValidationService = get<ContributionValidationService>()
        val appConfigService = get<AppConfigService>()

        val contributionConfigHandler = ContributionConfigHandler(
            getGroupByIdUseCase = get<GetGroupByIdUseCase>(),
            getGroupSubunitsUseCase = get<GetGroupSubunitsUseCase>(),
            getMemberProfilesUseCase = get<GetMemberProfilesUseCase>(),
            authenticationService = get<AuthenticationService>(),
            addContributionUiMapper = addContributionUiMapper,
            appConfigService = appConfigService
        )

        val contributionSubmitHandler = ContributionSubmitHandler(
            addContributionUseCase = get<AddContributionUseCase>(),
            contributionValidationService = contributionValidationService,
            groupCurrencyProvider = { contributionConfigHandler.groupCurrency }
        )

        AddContributionViewModel(
            configHandler = contributionConfigHandler,
            submitHandler = contributionSubmitHandler,
            contributionValidationService = contributionValidationService,
            addContributionUiMapper = addContributionUiMapper
        )
    }

    factory { ContributionsTabGraphContributorImpl() } bind TabGraphContributor::class
    single { AddContributionScreenUiProviderImpl() } bind ScreenUiProvider::class
}
