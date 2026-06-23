package es.pedrazamiguez.splittrip.features.withdrawal.di

import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.core.designsystem.navigation.TabGraphContributor
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.FormattingHelper
import es.pedrazamiguez.splittrip.core.designsystem.presentation.mapper.UserUiMapper
import es.pedrazamiguez.splittrip.core.designsystem.presentation.screen.ScreenUiProvider
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.CashWithdrawalValidationService
import es.pedrazamiguez.splittrip.domain.service.ExchangeRateCalculationService
import es.pedrazamiguez.splittrip.domain.usecase.balance.AddCashWithdrawalUseCase
import es.pedrazamiguez.splittrip.domain.usecase.currency.GetExchangeRateUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.GetGroupExpenseConfigUseCase
import es.pedrazamiguez.splittrip.domain.usecase.subunit.GetGroupSubunitsUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.GetMemberProfilesUseCase
import es.pedrazamiguez.splittrip.features.withdrawal.navigation.impl.WithdrawalsTabGraphContributorImpl
import es.pedrazamiguez.splittrip.features.withdrawal.presentation.mapper.AddCashWithdrawalUiMapper
import es.pedrazamiguez.splittrip.features.withdrawal.presentation.screen.impl.AddCashWithdrawalScreenUiProviderImpl
import es.pedrazamiguez.splittrip.features.withdrawal.presentation.viewmodel.AddCashWithdrawalViewModel
import es.pedrazamiguez.splittrip.features.withdrawal.presentation.viewmodel.handler.WithdrawalConfigHandler
import es.pedrazamiguez.splittrip.features.withdrawal.presentation.viewmodel.handler.WithdrawalCurrencyHandler
import es.pedrazamiguez.splittrip.features.withdrawal.presentation.viewmodel.handler.WithdrawalFeeHandler
import es.pedrazamiguez.splittrip.features.withdrawal.presentation.viewmodel.handler.WithdrawalSubmitHandler
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module

val withdrawalsUiModule = module {

    single {
        AddCashWithdrawalUiMapper(
            resourceProvider = get<ResourceProvider>(),
            userUiMapper = get<UserUiMapper>()
        )
    }

    // ── AddCashWithdrawal ViewModel with co-created handlers ─────────
    // Handlers are created inside the viewModel block so the SAME instances
    // are shared between the ViewModel and any cross-handler references.

    viewModel {
        val addCashWithdrawalUiMapper = get<AddCashWithdrawalUiMapper>()
        val formattingHelper = get<FormattingHelper>()

        val configHandler = WithdrawalConfigHandler(
            getGroupExpenseConfigUseCase = get<GetGroupExpenseConfigUseCase>(),
            getGroupSubunitsUseCase = get<GetGroupSubunitsUseCase>(),
            getMemberProfilesUseCase = get<GetMemberProfilesUseCase>(),
            authenticationService = get<AuthenticationService>(),
            addCashWithdrawalUiMapper = addCashWithdrawalUiMapper
        )

        val currencyHandler = WithdrawalCurrencyHandler(
            getExchangeRateUseCase = get<GetExchangeRateUseCase>(),
            exchangeRateCalculationService = get<ExchangeRateCalculationService>(),
            addCashWithdrawalUiMapper = addCashWithdrawalUiMapper,
            formattingHelper = formattingHelper
        )

        val feeHandler = WithdrawalFeeHandler(
            getExchangeRateUseCase = get<GetExchangeRateUseCase>(),
            exchangeRateCalculationService = get<ExchangeRateCalculationService>(),
            addCashWithdrawalUiMapper = addCashWithdrawalUiMapper,
            formattingHelper = formattingHelper
        )

        val submitHandler = WithdrawalSubmitHandler(
            addCashWithdrawalUseCase = get<AddCashWithdrawalUseCase>(),
            cashWithdrawalValidationService = get<CashWithdrawalValidationService>(),
            exchangeRateCalculationService = get<ExchangeRateCalculationService>()
        )

        AddCashWithdrawalViewModel(
            configHandler = configHandler,
            currencyHandler = currencyHandler,
            feeHandler = feeHandler,
            submitHandler = submitHandler,
            addCashWithdrawalUiMapper = addCashWithdrawalUiMapper
        )
    }

    factory { WithdrawalsTabGraphContributorImpl() } bind TabGraphContributor::class
    single { AddCashWithdrawalScreenUiProviderImpl() } bind ScreenUiProvider::class
}
