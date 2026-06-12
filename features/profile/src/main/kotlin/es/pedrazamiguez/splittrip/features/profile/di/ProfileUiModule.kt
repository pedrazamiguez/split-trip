package es.pedrazamiguez.splittrip.features.profile.di

import es.pedrazamiguez.splittrip.core.designsystem.navigation.NavigationProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.screen.ScreenUiProvider
import es.pedrazamiguez.splittrip.domain.service.ProfileImageStorageService
import es.pedrazamiguez.splittrip.domain.service.UserValidationService
import es.pedrazamiguez.splittrip.domain.usecase.user.GetCurrentUserProfileUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.ObserveCurrentUserProfileUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.UpdateUserProfileUseCase
import es.pedrazamiguez.splittrip.features.profile.navigation.impl.ProfileNavigationProviderImpl
import es.pedrazamiguez.splittrip.features.profile.presentation.mapper.ProfileUiMapper
import es.pedrazamiguez.splittrip.features.profile.presentation.mapper.impl.ProfileUiMapperImpl
import es.pedrazamiguez.splittrip.features.profile.presentation.screen.impl.EditProfileScreenUiProviderImpl
import es.pedrazamiguez.splittrip.features.profile.presentation.screen.impl.ProfileScreenUiProviderImpl
import es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel.EditProfileViewModel
import es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel.ProfileViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module

val profileUiModule = module {

    single<ProfileUiMapper> {
        ProfileUiMapperImpl()
    }

    viewModel {
        val getCurrentUserProfileUseCase = get<GetCurrentUserProfileUseCase>()
        val observeCurrentUserProfileUseCase = get<ObserveCurrentUserProfileUseCase>()
        val profileUiMapper = get<ProfileUiMapper>()
        ProfileViewModel(
            getCurrentUserProfileUseCase = getCurrentUserProfileUseCase,
            observeCurrentUserProfileUseCase = observeCurrentUserProfileUseCase,
            profileUiMapper = profileUiMapper
        )
    }

    viewModel {
        val getCurrentUserProfileUseCase = get<GetCurrentUserProfileUseCase>()
        val updateUserProfileUseCase = get<UpdateUserProfileUseCase>()
        val userValidationService = get<UserValidationService>()
        val profileImageStorageService = get<ProfileImageStorageService>()
        EditProfileViewModel(
            getCurrentUserProfileUseCase = getCurrentUserProfileUseCase,
            updateUserProfileUseCase = updateUserProfileUseCase,
            userValidationService = userValidationService,
            profileImageStorageService = profileImageStorageService
        )
    }

    factory { ProfileNavigationProviderImpl() } bind NavigationProvider::class
    single { ProfileScreenUiProviderImpl() } bind ScreenUiProvider::class
    single { EditProfileScreenUiProviderImpl() } bind ScreenUiProvider::class
}
