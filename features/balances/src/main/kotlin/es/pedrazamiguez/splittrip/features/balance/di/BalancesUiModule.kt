package es.pedrazamiguez.splittrip.features.balance.di

import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.core.designsystem.navigation.NavigationProvider
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.core.designsystem.navigation.TabGraphContributor
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.FormattingHelper
import es.pedrazamiguez.splittrip.core.designsystem.presentation.mapper.UserUiMapper
import es.pedrazamiguez.splittrip.core.designsystem.presentation.screen.ScreenUiProvider
import es.pedrazamiguez.splittrip.domain.service.AddOnCalculationService
import es.pedrazamiguez.splittrip.domain.service.AppConfigService
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.usecase.balance.DeleteCashWithdrawalUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.DeleteContributionUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetBalancesDashboardFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetGroupSpendingInsightsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.GetGroupByIdUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.ObserveGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetLastSeenBalanceUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.SetLastSeenBalanceUseCase
import es.pedrazamiguez.splittrip.features.balance.navigation.impl.BalancesNavigationProviderImpl
import es.pedrazamiguez.splittrip.features.balance.navigation.impl.SpendingInsightsTabGraphContributorImpl
import es.pedrazamiguez.splittrip.features.balance.presentation.mapper.BalancesUiMapper
import es.pedrazamiguez.splittrip.features.balance.presentation.mapper.SettlementsUiMapper
import es.pedrazamiguez.splittrip.features.balance.presentation.mapper.SpendingInsightsUiMapper
import es.pedrazamiguez.splittrip.features.balance.presentation.screen.impl.BalancesScreenUiProviderImpl
import es.pedrazamiguez.splittrip.features.balance.presentation.screen.impl.SpendingInsightsScreenUiProviderImpl
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.BalancesViewModel
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.SpendingInsightsViewModel
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.handler.BalancesActivityEventHandler
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.handler.BalancesActivityEventHandlerImpl
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

val balancesUiModule = module {

    single {
        BalancesUiMapper(
            localeProvider = get<LocaleProvider>(),
            resourceProvider = get<ResourceProvider>(),
            userUiMapper = get<UserUiMapper>(),
            addOnCalculationService = get<AddOnCalculationService>()
        )
    }

    single {
        SettlementsUiMapper(
            localeProvider = get<LocaleProvider>(),
            resourceProvider = get<ResourceProvider>(),
            userUiMapper = get<UserUiMapper>()
        )
    }

    viewModel {
        val deleteContributionUseCase = get<DeleteContributionUseCase>()
        val deleteCashWithdrawalUseCase = get<DeleteCashWithdrawalUseCase>()
        val appConfigService = get<AppConfigService>()

        val balancesActivityEventHandler: BalancesActivityEventHandler =
            BalancesActivityEventHandlerImpl(
                deleteContributionUseCase = deleteContributionUseCase,
                deleteCashWithdrawalUseCase = deleteCashWithdrawalUseCase
            )

        BalancesViewModel(
            getBalancesDashboardFlowUseCase = get<GetBalancesDashboardFlowUseCase>(),
            getLastSeenBalanceUseCase = get<GetLastSeenBalanceUseCase>(),
            setLastSeenBalanceUseCase = get<SetLastSeenBalanceUseCase>(),
            getGroupByIdUseCase = get<GetGroupByIdUseCase>(),
            observeGroupUseCase = get<ObserveGroupUseCase>(),
            authenticationService = get<AuthenticationService>(),
            balancesUiMapper = get<BalancesUiMapper>(),
            settlementsUiMapper = get<SettlementsUiMapper>(),
            activityEventHandler = balancesActivityEventHandler,
            appConfigService = appConfigService,
            computationDispatcher = Dispatchers.Default
        )
    }

    factory {
        BalancesNavigationProviderImpl(
            graphContributors = getAll<TabGraphContributor>()
        )
    } bind NavigationProvider::class

    single {
        BalancesScreenUiProviderImpl()
    } bind ScreenUiProvider::class

    single {
        SpendingInsightsUiMapper(
            formattingHelper = get<FormattingHelper>(),
            resourceProvider = get<ResourceProvider>(),
            userUiMapper = get<UserUiMapper>()
        )
    }

    viewModel {
        val getGroupSpendingInsightsFlowUseCase = get<GetGroupSpendingInsightsFlowUseCase>()
        val spendingInsightsUiMapper = get<SpendingInsightsUiMapper>()
        SpendingInsightsViewModel(
            getGroupSpendingInsightsFlowUseCase = getGroupSpendingInsightsFlowUseCase,
            spendingInsightsUiMapper = spendingInsightsUiMapper,
            computationDispatcher = Dispatchers.Default
        )
    }

    single {
        SpendingInsightsScreenUiProviderImpl()
    } bind ScreenUiProvider::class

    factory(named(Routes.SPENDING_INSIGHTS_CONTRIBUTOR_QUALIFIER)) {
        SpendingInsightsTabGraphContributorImpl()
    } bind TabGraphContributor::class
}
