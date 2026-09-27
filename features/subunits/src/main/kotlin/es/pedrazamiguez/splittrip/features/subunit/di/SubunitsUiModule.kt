package es.pedrazamiguez.splittrip.features.subunit.di

import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.core.designsystem.navigation.TabGraphContributor
import es.pedrazamiguez.splittrip.core.designsystem.presentation.mapper.UserUiMapper
import es.pedrazamiguez.splittrip.core.designsystem.presentation.screen.ScreenUiProvider
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.SubunitShareDistributionService
import es.pedrazamiguez.splittrip.domain.service.featuregate.FeatureGateService
import es.pedrazamiguez.splittrip.domain.usecase.group.GetGroupByIdUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.ObserveGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.ObserveSelectedGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.subunit.CreateSubunitUseCase
import es.pedrazamiguez.splittrip.domain.usecase.subunit.DeleteSubunitUseCase
import es.pedrazamiguez.splittrip.domain.usecase.subunit.GetGroupSubunitsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.subunit.UpdateSubunitUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.GetMemberProfilesUseCase
import es.pedrazamiguez.splittrip.features.subunit.navigation.impl.SubunitsTabGraphContributorImpl
import es.pedrazamiguez.splittrip.features.subunit.presentation.mapper.SubunitUiMapper
import es.pedrazamiguez.splittrip.features.subunit.presentation.mapper.impl.SubunitUiMapperImpl
import es.pedrazamiguez.splittrip.features.subunit.presentation.screen.impl.CreateEditSubunitScreenUiProviderImpl
import es.pedrazamiguez.splittrip.features.subunit.presentation.screen.impl.SubunitManagementScreenUiProviderImpl
import es.pedrazamiguez.splittrip.features.subunit.presentation.viewmodel.CreateEditSubunitViewModel
import es.pedrazamiguez.splittrip.features.subunit.presentation.viewmodel.SubunitManagementViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module

val subunitsUiModule = module {

    single<SubunitUiMapper> {
        SubunitUiMapperImpl(
            localeProvider = get<LocaleProvider>(),
            resourceProvider = get<ResourceProvider>(),
            userUiMapper = get<UserUiMapper>()
        )
    }

    viewModel {
        SubunitManagementViewModel(
            getGroupSubunitsFlowUseCase = get<GetGroupSubunitsFlowUseCase>(),
            deleteSubunitUseCase = get<DeleteSubunitUseCase>(),
            getGroupByIdUseCase = get<GetGroupByIdUseCase>(),
            getMemberProfilesUseCase = get<GetMemberProfilesUseCase>(),
            subunitUiMapper = get<SubunitUiMapper>(),
            observeGroupUseCase = get<ObserveGroupUseCase>(),
            featureGateService = get<FeatureGateService>(),
            authenticationService = get<AuthenticationService>()
        )
    }

    viewModel {
        CreateEditSubunitViewModel(
            createSubunitUseCase = get<CreateSubunitUseCase>(),
            updateSubunitUseCase = get<UpdateSubunitUseCase>(),
            getGroupByIdUseCase = get<GetGroupByIdUseCase>(),
            getGroupSubunitsFlowUseCase = get<GetGroupSubunitsFlowUseCase>(),
            getMemberProfilesUseCase = get<GetMemberProfilesUseCase>(),
            subunitUiMapper = get<SubunitUiMapper>(),
            shareDistributionService = get<SubunitShareDistributionService>(),
            authenticationService = get<AuthenticationService>(),
            featureGateService = get<FeatureGateService>()
        )
    }

    factory { SubunitsTabGraphContributorImpl() } bind TabGraphContributor::class
    single {
        SubunitManagementScreenUiProviderImpl(
            observeSelectedGroupUseCase = get<ObserveSelectedGroupUseCase>(),
            featureGateService = get<FeatureGateService>()
        )
    } bind ScreenUiProvider::class
    single { CreateEditSubunitScreenUiProviderImpl() } bind ScreenUiProvider::class
}
