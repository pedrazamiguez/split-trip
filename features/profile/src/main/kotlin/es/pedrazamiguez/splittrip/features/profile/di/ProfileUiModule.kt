package es.pedrazamiguez.splittrip.features.profile.di

import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.designsystem.navigation.NavigationProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.screen.ScreenUiProvider
import es.pedrazamiguez.splittrip.domain.usecase.auth.GetLinkedProvidersUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.LinkEmailPasswordUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.LinkGoogleAccountUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.UnlinkProviderUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.GetCurrentUserProfileUseCase
import es.pedrazamiguez.splittrip.features.profile.navigation.impl.ProfileNavigationProviderImpl
import es.pedrazamiguez.splittrip.features.profile.presentation.mapper.ProfileUiMapper
import es.pedrazamiguez.splittrip.features.profile.presentation.mapper.impl.ProfileUiMapperImpl
import es.pedrazamiguez.splittrip.features.profile.presentation.screen.impl.ProfileScreenUiProviderImpl
import es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel.ProfileViewModel
import es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel.handler.ProfileAccountLinkHandler
import es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel.handler.ProfileAccountLinkHandlerImpl
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module

val profileUiModule = module {

    single<ProfileUiMapper> {
        ProfileUiMapperImpl(
            localeProvider = get<LocaleProvider>()
        )
    }

    factory<ProfileAccountLinkHandler> {
        val linkGoogleAccountUseCase = get<LinkGoogleAccountUseCase>()
        val linkEmailPasswordUseCase = get<LinkEmailPasswordUseCase>()
        val unlinkProviderUseCase = get<UnlinkProviderUseCase>()
        ProfileAccountLinkHandlerImpl(
            linkGoogleAccountUseCase = linkGoogleAccountUseCase,
            linkEmailPasswordUseCase = linkEmailPasswordUseCase,
            unlinkProviderUseCase = unlinkProviderUseCase
        )
    }

    viewModel {
        val getCurrentUserProfileUseCase = get<GetCurrentUserProfileUseCase>()
        val profileUiMapper = get<ProfileUiMapper>()
        val getLinkedProvidersUseCase = get<GetLinkedProvidersUseCase>()
        val profileAccountLinkHandler = get<ProfileAccountLinkHandler>()
        ProfileViewModel(
            getCurrentUserProfileUseCase = getCurrentUserProfileUseCase,
            profileUiMapper = profileUiMapper,
            getLinkedProvidersUseCase = getLinkedProvidersUseCase,
            profileAccountLinkHandler = profileAccountLinkHandler
        )
    }

    factory { ProfileNavigationProviderImpl() } bind NavigationProvider::class
    single { ProfileScreenUiProviderImpl() } bind ScreenUiProvider::class
}
