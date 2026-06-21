package es.pedrazamiguez.splittrip.features.activitylog.di

import es.pedrazamiguez.splittrip.core.designsystem.navigation.NavigationProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.screen.ScreenUiProvider
import es.pedrazamiguez.splittrip.features.activitylog.navigation.impl.ActivityLoggingNavigationProviderImpl
import es.pedrazamiguez.splittrip.features.activitylog.presentation.screen.impl.ActivityLoggingScreenUiProviderImpl
import es.pedrazamiguez.splittrip.features.activitylog.presentation.viewmodel.ActivityLoggingViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module

val activityLoggingUiModule = module {
    viewModel {
        ActivityLoggingViewModel()
    }

    factory { ActivityLoggingNavigationProviderImpl() } bind NavigationProvider::class
    single {
        ActivityLoggingScreenUiProviderImpl()
    } bind ScreenUiProvider::class
}
