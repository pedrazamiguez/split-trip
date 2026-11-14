package es.pedrazamiguez.splittrip.di.domain

import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.repository.UserPreferenceRepository
import es.pedrazamiguez.splittrip.domain.repository.UserRepository
import es.pedrazamiguez.splittrip.domain.service.BillingService
import es.pedrazamiguez.splittrip.domain.service.UserValidationService
import es.pedrazamiguez.splittrip.domain.service.impl.UserValidationServiceImpl
import es.pedrazamiguez.splittrip.domain.usecase.user.CheckPendingReconciliationUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.GetCurrentUserProfileUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.GetMemberProfilesUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.ObserveCurrentUserProfileUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.ReconcileSubscriptionEntitlementsUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.ReconcileUnregisteredUserUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.SearchUsersByEmailUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.UpdateUserProfileUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.UpdateUserReminderPreferencesUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.UpdateUserTierUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.impl.CheckPendingReconciliationUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.user.impl.GetCurrentUserProfileUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.user.impl.GetMemberProfilesUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.user.impl.ObserveCurrentUserProfileUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.user.impl.ReconcileSubscriptionEntitlementsUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.user.impl.ReconcileUnregisteredUserUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.user.impl.SearchUsersByEmailUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.user.impl.UpdateUserProfileUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.user.impl.UpdateUserReminderPreferencesUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.user.impl.UpdateUserTierUseCaseImpl
import org.koin.dsl.module

val profileDomainModule = module {
    factory<UserValidationService> {
        UserValidationServiceImpl()
    }
    factory<UpdateUserProfileUseCase> {
        UpdateUserProfileUseCaseImpl(
            userRepository = get<UserRepository>(),
            userValidationService = get<UserValidationService>()
        )
    }
    factory<GetMemberProfilesUseCase> {
        GetMemberProfilesUseCaseImpl(
            userRepository = get<UserRepository>()
        )
    }
    factory<GetCurrentUserProfileUseCase> {
        GetCurrentUserProfileUseCaseImpl(
            userRepository = get<UserRepository>()
        )
    }
    factory<ObserveCurrentUserProfileUseCase> {
        ObserveCurrentUserProfileUseCaseImpl(
            userRepository = get<UserRepository>()
        )
    }
    factory<SearchUsersByEmailUseCase> {
        SearchUsersByEmailUseCaseImpl(
            userRepository = get<UserRepository>()
        )
    }
    factory<ReconcileUnregisteredUserUseCase> {
        ReconcileUnregisteredUserUseCaseImpl(
            groupRepository = get<GroupRepository>(),
            userRepository = get<UserRepository>(),
            userPreferenceRepository = get<UserPreferenceRepository>()
        )
    }
    factory<CheckPendingReconciliationUseCase> {
        CheckPendingReconciliationUseCaseImpl(
            userRepository = get<UserRepository>()
        )
    }
    factory<UpdateUserReminderPreferencesUseCase> {
        UpdateUserReminderPreferencesUseCaseImpl(
            userRepository = get<UserRepository>()
        )
    }
    factory<UpdateUserTierUseCase> {
        UpdateUserTierUseCaseImpl(
            userRepository = get<UserRepository>()
        )
    }
    factory<ReconcileSubscriptionEntitlementsUseCase> {
        val userRepository = get<UserRepository>()
        val billingService = get<BillingService>()
        val updateUserTierUseCase = get<UpdateUserTierUseCase>()
        ReconcileSubscriptionEntitlementsUseCaseImpl(
            userRepository = userRepository,
            billingService = billingService,
            updateUserTierUseCase = updateUserTierUseCase
        )
    }
}
