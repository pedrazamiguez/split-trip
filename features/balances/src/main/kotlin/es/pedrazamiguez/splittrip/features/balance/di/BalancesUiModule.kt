package es.pedrazamiguez.splittrip.features.balance.di

import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.core.designsystem.navigation.NavigationProvider
import es.pedrazamiguez.splittrip.core.designsystem.navigation.TabGraphContributor
import es.pedrazamiguez.splittrip.core.designsystem.presentation.mapper.UserUiMapper
import es.pedrazamiguez.splittrip.core.designsystem.presentation.screen.ScreenUiProvider
import es.pedrazamiguez.splittrip.domain.service.AppConfigService
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.usecase.balance.DeleteCashWithdrawalUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.DeleteContributionUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetCashWithdrawalsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetGroupContributionsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetGroupPocketBalanceFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetMemberBalancesFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.GetGroupExpensesFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.GetGroupByIdUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetLastSeenBalanceUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.SetLastSeenBalanceUseCase
import es.pedrazamiguez.splittrip.domain.usecase.subunit.GetGroupSubunitsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.GetMemberProfilesUseCase
import es.pedrazamiguez.splittrip.features.balance.navigation.impl.BalancesNavigationProviderImpl
import es.pedrazamiguez.splittrip.features.balance.presentation.mapper.BalancesUiMapper
import es.pedrazamiguez.splittrip.features.balance.presentation.screen.impl.BalancesScreenUiProviderImpl
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.BalancesUseCases
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.BalancesViewModel
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.handler.BalancesActivityEventHandler
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.handler.BalancesActivityEventHandlerImpl
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module

val balancesUiModule = module {

    single {
        BalancesUiMapper(
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
            useCases = BalancesUseCases(
                getGroupPocketBalanceFlowUseCase = get<GetGroupPocketBalanceFlowUseCase>(),
                getGroupContributionsFlowUseCase = get<GetGroupContributionsFlowUseCase>(),
                getCashWithdrawalsFlowUseCase = get<GetCashWithdrawalsFlowUseCase>(),
                getGroupExpensesFlowUseCase = get<GetGroupExpensesFlowUseCase>(),
                getMemberBalancesFlowUseCase = get<GetMemberBalancesFlowUseCase>(),
                getGroupSubunitsFlowUseCase = get<GetGroupSubunitsFlowUseCase>(),
                getGroupByIdUseCase = get<GetGroupByIdUseCase>(),
                getLastSeenBalanceUseCase = get<GetLastSeenBalanceUseCase>(),
                setLastSeenBalanceUseCase = get<SetLastSeenBalanceUseCase>(),
                getMemberProfilesUseCase = get<GetMemberProfilesUseCase>(),
                deleteContributionUseCase = deleteContributionUseCase,
                deleteCashWithdrawalUseCase = deleteCashWithdrawalUseCase
            ),
            authenticationService = get<AuthenticationService>(),
            balancesUiMapper = get<BalancesUiMapper>(),
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
}
