package es.pedrazamiguez.splittrip.core.designsystem.di

import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.FormattingHelper
import es.pedrazamiguez.splittrip.core.designsystem.presentation.mapper.UserUiMapper
import es.pedrazamiguez.splittrip.core.designsystem.presentation.viewmodel.SharedViewModel
import es.pedrazamiguez.splittrip.domain.service.AppConfigService
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetSelectedGroupCurrencyUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetSelectedGroupIdUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetSelectedGroupNameUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.SetSelectedGroupUseCase
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val coreDesignSystemModule = module {

    single { UserUiMapper() }

    single {
        FormattingHelper(
            localeProvider = get<LocaleProvider>(),
            appConfigService = get<AppConfigService>()
        )
    }

    viewModel {
        SharedViewModel(
            getSelectedGroupIdUseCase = get<GetSelectedGroupIdUseCase>(),
            getSelectedGroupNameUseCase = get<GetSelectedGroupNameUseCase>(),
            getSelectedGroupCurrencyUseCase = get<GetSelectedGroupCurrencyUseCase>(),
            setSelectedGroupUseCase = get<SetSelectedGroupUseCase>()
        )
    }
}
