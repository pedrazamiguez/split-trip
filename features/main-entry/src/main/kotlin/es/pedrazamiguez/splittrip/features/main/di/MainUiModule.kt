package es.pedrazamiguez.splittrip.features.main.di

import es.pedrazamiguez.splittrip.domain.usecase.currency.WarmCurrencyCacheUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.GetGroupByIdUseCase
import es.pedrazamiguez.splittrip.domain.usecase.notification.RegisterDeviceTokenUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.ObserveCurrentUserProfileUseCase
import es.pedrazamiguez.splittrip.features.main.navigation.DeepLinkHolder
import es.pedrazamiguez.splittrip.features.main.presentation.viewmodel.MainViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val mainUiModule = module {
    single { DeepLinkHolder() }

    viewModel {
        val registerDeviceTokenUseCase = get<RegisterDeviceTokenUseCase>()
        val getGroupByIdUseCase = get<GetGroupByIdUseCase>()
        val warmCurrencyCacheUseCase = get<WarmCurrencyCacheUseCase>()
        val observeCurrentUserProfileUseCase = get<ObserveCurrentUserProfileUseCase>()
        MainViewModel(
            registerDeviceTokenUseCase = registerDeviceTokenUseCase,
            getGroupByIdUseCase = getGroupByIdUseCase,
            warmCurrencyCacheUseCase = warmCurrencyCacheUseCase,
            observeCurrentUserProfileUseCase = observeCurrentUserProfileUseCase
        )
    }
}
