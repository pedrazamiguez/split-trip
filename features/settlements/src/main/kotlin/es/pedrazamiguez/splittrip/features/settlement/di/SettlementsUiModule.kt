package es.pedrazamiguez.splittrip.features.settlement.di

import es.pedrazamiguez.splittrip.core.common.network.NetworkMonitor
import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.core.designsystem.navigation.TabGraphContributor
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.FormattingHelper
import es.pedrazamiguez.splittrip.core.designsystem.presentation.mapper.UserUiMapper
import es.pedrazamiguez.splittrip.core.designsystem.presentation.screen.ScreenUiProvider
import es.pedrazamiguez.splittrip.domain.service.AppConfigService
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.PocketDebtDistributionService
import es.pedrazamiguez.splittrip.domain.usecase.balance.ConfirmSettlementUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.DisputeSettlementUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetCashWithdrawalsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetGroupContributionsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetGroupSettlementsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetMemberBalancesFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetSettlementSuggestionsUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.GetGroupExpensesFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.ArchiveGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.GetGroupByIdUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.ObserveGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.settlement.GetNudgeTimestampsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.settlement.NudgeDebtorUseCase
import es.pedrazamiguez.splittrip.domain.usecase.subunit.GetGroupSubunitsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.GetMemberProfilesUseCase
import es.pedrazamiguez.splittrip.features.settlement.navigation.impl.SettlementsTabGraphContributorImpl
import es.pedrazamiguez.splittrip.features.settlement.presentation.mapper.GroupSettlementOverviewUiMapper
import es.pedrazamiguez.splittrip.features.settlement.presentation.mapper.MemberSpendingChartUiMapper
import es.pedrazamiguez.splittrip.features.settlement.presentation.mapper.SettlementConsensusUiMapper
import es.pedrazamiguez.splittrip.features.settlement.presentation.mapper.YourBalanceUiMapper
import es.pedrazamiguez.splittrip.features.settlement.presentation.mapper.impl.GroupSettlementOverviewUiMapperImpl
import es.pedrazamiguez.splittrip.features.settlement.presentation.screen.impl.GroupSettlementOverviewScreenUiProviderImpl
import es.pedrazamiguez.splittrip.features.settlement.presentation.screen.impl.YourBalanceScreenUiProviderImpl
import es.pedrazamiguez.splittrip.features.settlement.presentation.viewmodel.GroupSettlementOverviewViewModel
import es.pedrazamiguez.splittrip.features.settlement.presentation.viewmodel.YourBalanceUseCases
import es.pedrazamiguez.splittrip.features.settlement.presentation.viewmodel.YourBalanceViewModel
import es.pedrazamiguez.splittrip.features.settlement.presentation.viewmodel.delegate.YourBalanceActionDelegate
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module

val settlementsUiModule = module {
    factory {
        val confirmSettlementUseCase = get<ConfirmSettlementUseCase>()
        val disputeSettlementUseCase = get<DisputeSettlementUseCase>()
        val nudgeDebtorUseCase = get<NudgeDebtorUseCase>()
        YourBalanceActionDelegate(
            confirmSettlementUseCase = confirmSettlementUseCase,
            disputeSettlementUseCase = disputeSettlementUseCase,
            nudgeDebtorUseCase = nudgeDebtorUseCase
        )
    }

    single<GroupSettlementOverviewUiMapper> {
        val formattingHelper = get<FormattingHelper>()
        val resourceProvider = get<ResourceProvider>()
        GroupSettlementOverviewUiMapperImpl(
            formattingHelper = formattingHelper,
            resourceProvider = resourceProvider,
            userUiMapper = get<UserUiMapper>()
        )
    }

    viewModel {
        val getGroupSettlementsFlowUseCase = get<GetGroupSettlementsFlowUseCase>()
        val getMemberProfilesUseCase = get<GetMemberProfilesUseCase>()
        val observeGroupUseCase = get<ObserveGroupUseCase>()
        val groupSettlementOverviewUiMapper = get<GroupSettlementOverviewUiMapper>()
        val authenticationService = get<AuthenticationService>()
        val confirmSettlementUseCase = get<ConfirmSettlementUseCase>()
        val disputeSettlementUseCase = get<DisputeSettlementUseCase>()
        val archiveGroupUseCase = get<ArchiveGroupUseCase>()
        val getSettlementSuggestionsUseCase = get<GetSettlementSuggestionsUseCase>()
        GroupSettlementOverviewViewModel(
            getGroupSettlementsFlowUseCase = getGroupSettlementsFlowUseCase,
            getMemberProfilesUseCase = getMemberProfilesUseCase,
            observeGroupUseCase = observeGroupUseCase,
            groupSettlementOverviewUiMapper = groupSettlementOverviewUiMapper,
            authenticationService = authenticationService,
            confirmSettlementUseCase = confirmSettlementUseCase,
            disputeSettlementUseCase = disputeSettlementUseCase,
            archiveGroupUseCase = archiveGroupUseCase,
            getSettlementSuggestionsUseCase = getSettlementSuggestionsUseCase
        )
    }

    viewModel {
        val getGroupByIdUseCase = get<GetGroupByIdUseCase>()
        val getGroupContributionsFlowUseCase = get<GetGroupContributionsFlowUseCase>()
        val getCashWithdrawalsFlowUseCase = get<GetCashWithdrawalsFlowUseCase>()
        val getGroupExpensesFlowUseCase = get<GetGroupExpensesFlowUseCase>()
        val getGroupSubunitsFlowUseCase = get<GetGroupSubunitsFlowUseCase>()
        val getMemberBalancesFlowUseCase = get<GetMemberBalancesFlowUseCase>()
        val getGroupSettlementsFlowUseCase = get<GetGroupSettlementsFlowUseCase>()
        val getMemberProfilesUseCase = get<GetMemberProfilesUseCase>()
        val getSettlementSuggestionsUseCase = get<GetSettlementSuggestionsUseCase>()
        val getNudgeTimestampsFlowUseCase = get<GetNudgeTimestampsFlowUseCase>()
        val authenticationService = get<AuthenticationService>()
        val appConfigService = get<AppConfigService>()
        val networkMonitor = get<NetworkMonitor>()
        val localeProvider = get<LocaleProvider>()
        val resourceProvider = get<ResourceProvider>()
        val yourBalanceActionDelegate = get<YourBalanceActionDelegate>()

        val yourBalanceUiMapper = YourBalanceUiMapper(
            localeProvider = localeProvider,
            resourceProvider = resourceProvider
        )

        val settlementConsensusUiMapper = SettlementConsensusUiMapper(
            localeProvider = localeProvider,
            resourceProvider = resourceProvider
        )

        val userUiMapper = get<UserUiMapper>()
        val pocketDebtDistributionService = get<PocketDebtDistributionService>()

        val memberSpendingChartUiMapper = MemberSpendingChartUiMapper(
            localeProvider = localeProvider,
            userUiMapper = userUiMapper,
            pocketDebtDistributionService = pocketDebtDistributionService
        )

        val yourBalanceUseCases = YourBalanceUseCases(
            getGroupByIdUseCase = getGroupByIdUseCase,
            getGroupContributionsFlowUseCase = getGroupContributionsFlowUseCase,
            getCashWithdrawalsFlowUseCase = getCashWithdrawalsFlowUseCase,
            getGroupExpensesFlowUseCase = getGroupExpensesFlowUseCase,
            getGroupSubunitsFlowUseCase = getGroupSubunitsFlowUseCase,
            getMemberBalancesFlowUseCase = getMemberBalancesFlowUseCase,
            getGroupSettlementsFlowUseCase = getGroupSettlementsFlowUseCase,
            getMemberProfilesUseCase = getMemberProfilesUseCase,
            getSettlementSuggestionsUseCase = getSettlementSuggestionsUseCase,
            getNudgeTimestampsFlowUseCase = getNudgeTimestampsFlowUseCase
        )

        YourBalanceViewModel(
            useCases = yourBalanceUseCases,
            actionDelegate = yourBalanceActionDelegate,
            authenticationService = authenticationService,
            yourBalanceUiMapper = yourBalanceUiMapper,
            settlementConsensusUiMapper = settlementConsensusUiMapper,
            memberSpendingChartUiMapper = memberSpendingChartUiMapper,
            appConfigService = appConfigService,
            networkMonitor = networkMonitor
        )
    }
    factory { SettlementsTabGraphContributorImpl() } bind TabGraphContributor::class
    single { YourBalanceScreenUiProviderImpl() } bind ScreenUiProvider::class
    single { GroupSettlementOverviewScreenUiProviderImpl() } bind ScreenUiProvider::class
}
