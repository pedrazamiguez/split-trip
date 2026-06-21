package es.pedrazamiguez.splittrip.features.group.di

import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.core.designsystem.navigation.NavigationProvider
import es.pedrazamiguez.splittrip.core.designsystem.navigation.TabGraphContributor
import es.pedrazamiguez.splittrip.core.designsystem.presentation.screen.ScreenUiProvider
import es.pedrazamiguez.splittrip.core.logging.TelemetryTracker
import es.pedrazamiguez.splittrip.domain.service.EmailValidationService
import es.pedrazamiguez.splittrip.domain.service.GroupImageStorageService
import es.pedrazamiguez.splittrip.domain.usecase.currency.GetSupportedCurrenciesUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.CreateGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.DeleteGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.GetGroupByIdUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.GetUserGroupsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetUserDefaultCurrencyUseCase
import es.pedrazamiguez.splittrip.domain.usecase.subunit.GetGroupSubunitsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.GetMemberProfilesUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.SearchUsersByEmailUseCase
import es.pedrazamiguez.splittrip.features.group.navigation.impl.GroupsNavigationProviderImpl
import es.pedrazamiguez.splittrip.features.group.presentation.mapper.GroupUiMapper
import es.pedrazamiguez.splittrip.features.group.presentation.mapper.impl.GroupUiMapperImpl
import es.pedrazamiguez.splittrip.features.group.presentation.screen.impl.CreateGroupScreenUiProviderImpl
import es.pedrazamiguez.splittrip.features.group.presentation.screen.impl.GroupDetailScreenUiProviderImpl
import es.pedrazamiguez.splittrip.features.group.presentation.screen.impl.GroupsScreenUiProviderImpl
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.CreateGroupViewModel
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.GroupDetailViewModel
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.GroupsViewModel
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.handler.CreateGroupImageEventHandler
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.handler.CreateGroupImageEventHandlerImpl
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.handler.CreateGroupNavigationEventHandler
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.handler.CreateGroupNavigationEventHandlerImpl
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.handler.CreateGroupSubmitEventHandler
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.handler.CreateGroupSubmitEventHandlerImpl
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module

val groupsUiModule = module {

    single<GroupUiMapper> {
        GroupUiMapperImpl(
            localeProvider = get<LocaleProvider>(),
            resourceProvider = get<ResourceProvider>()
        )
    }

    factory<CreateGroupNavigationEventHandler> {
        CreateGroupNavigationEventHandlerImpl()
    }

    factory<CreateGroupImageEventHandler> {
        val groupImageStorageService = get<GroupImageStorageService>()
        CreateGroupImageEventHandlerImpl(
            groupImageStorageService = groupImageStorageService
        )
    }

    factory<CreateGroupSubmitEventHandler> {
        val createGroupUseCase = get<CreateGroupUseCase>()
        val telemetryTracker = get<TelemetryTracker>()
        CreateGroupSubmitEventHandlerImpl(
            createGroupUseCase = createGroupUseCase,
            telemetryTracker = telemetryTracker
        )
    }

    viewModel {
        val createGroupNavigationEventHandler = get<CreateGroupNavigationEventHandler>()
        val createGroupImageEventHandler = get<CreateGroupImageEventHandler>()
        val createGroupSubmitEventHandler = get<CreateGroupSubmitEventHandler>()
        val getSupportedCurrenciesUseCase = get<GetSupportedCurrenciesUseCase>()
        val getUserDefaultCurrencyUseCase = get<GetUserDefaultCurrencyUseCase>()
        val searchUsersByEmailUseCase = get<SearchUsersByEmailUseCase>()
        val emailValidationService = get<EmailValidationService>()
        val getMemberProfilesUseCase = get<GetMemberProfilesUseCase>()
        val groupUiMapper = get<GroupUiMapper>()

        CreateGroupViewModel(
            createGroupNavigationEventHandler = createGroupNavigationEventHandler,
            createGroupImageEventHandler = createGroupImageEventHandler,
            createGroupSubmitEventHandler = createGroupSubmitEventHandler,
            getSupportedCurrenciesUseCase = getSupportedCurrenciesUseCase,
            getUserDefaultCurrencyUseCase = getUserDefaultCurrencyUseCase,
            searchUsersByEmailUseCase = searchUsersByEmailUseCase,
            emailValidationService = emailValidationService,
            getMemberProfilesUseCase = getMemberProfilesUseCase,
            groupUiMapper = groupUiMapper
        )
    }

    viewModel {
        GroupsViewModel(
            getUserGroupsFlowUseCase = get<GetUserGroupsFlowUseCase>(),
            deleteGroupUseCase = get<DeleteGroupUseCase>(),
            getMemberProfilesUseCase = get<GetMemberProfilesUseCase>(),
            groupUiMapper = get<GroupUiMapper>()
        )
    }

    viewModel {
        GroupDetailViewModel(
            getGroupByIdUseCase = get<GetGroupByIdUseCase>(),
            getGroupSubunitsFlowUseCase = get<GetGroupSubunitsFlowUseCase>(),
            getMemberProfilesUseCase = get<GetMemberProfilesUseCase>(),
            groupUiMapper = get<GroupUiMapper>()
        )
    }

    factory {
        GroupsNavigationProviderImpl(
            graphContributors = getAll<TabGraphContributor>()
        )
    } bind NavigationProvider::class

    single {
        GroupsScreenUiProviderImpl()
    } bind ScreenUiProvider::class
    single { CreateGroupScreenUiProviderImpl() } bind ScreenUiProvider::class
    single { GroupDetailScreenUiProviderImpl() } bind ScreenUiProvider::class
}
