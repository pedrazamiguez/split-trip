package es.pedrazamiguez.splittrip.di.domain

import es.pedrazamiguez.splittrip.domain.repository.BalancePreferenceRepository
import es.pedrazamiguez.splittrip.domain.repository.GroupPreferenceRepository
import es.pedrazamiguez.splittrip.domain.repository.OnboardingPreferenceRepository
import es.pedrazamiguez.splittrip.domain.repository.UserPreferenceRepository
import es.pedrazamiguez.splittrip.domain.usecase.setting.ConsumeLanguagePillUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetActiveAiEngineUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetAppLanguageUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetAppThemeUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetGroupLastUsedCategoryUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetGroupLastUsedCurrencyUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetGroupLastUsedPaymentMethodUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetLastSeenBalanceUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetSelectedGroupCurrencyUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetSelectedGroupIdUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetSelectedGroupNameUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetShouldShowLanguagePillUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetUserDefaultCurrencyUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.IsOnboardingCompleteUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.SetActiveAiEngineUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.SetAppLanguageUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.SetAppThemeUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.SetGroupLastUsedCategoryUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.SetGroupLastUsedCurrencyUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.SetGroupLastUsedPaymentMethodUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.SetLastSeenBalanceUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.SetOnboardingCompleteUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.SetSelectedGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.SetUserDefaultCurrencyUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.impl.ConsumeLanguagePillUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.setting.impl.GetActiveAiEngineUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.setting.impl.GetAppLanguageUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.setting.impl.GetAppThemeUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.setting.impl.GetGroupLastUsedCategoryUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.setting.impl.GetGroupLastUsedCurrencyUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.setting.impl.GetGroupLastUsedPaymentMethodUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.setting.impl.GetLastSeenBalanceUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.setting.impl.GetSelectedGroupCurrencyUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.setting.impl.GetSelectedGroupIdUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.setting.impl.GetSelectedGroupNameUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.setting.impl.GetShouldShowLanguagePillUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.setting.impl.GetUserDefaultCurrencyUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.setting.impl.IsOnboardingCompleteUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.setting.impl.SetActiveAiEngineUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.setting.impl.SetAppLanguageUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.setting.impl.SetAppThemeUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.setting.impl.SetGroupLastUsedCategoryUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.setting.impl.SetGroupLastUsedCurrencyUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.setting.impl.SetGroupLastUsedPaymentMethodUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.setting.impl.SetLastSeenBalanceUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.setting.impl.SetOnboardingCompleteUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.setting.impl.SetSelectedGroupUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.setting.impl.SetUserDefaultCurrencyUseCaseImpl
import org.koin.dsl.module

val settingsDomainModule = module {

    factory<IsOnboardingCompleteUseCase> {
        IsOnboardingCompleteUseCaseImpl(
            preferenceRepository = get<OnboardingPreferenceRepository>()
        )
    }

    factory<SetOnboardingCompleteUseCase> {
        SetOnboardingCompleteUseCaseImpl(
            preferenceRepository = get<OnboardingPreferenceRepository>()
        )
    }

    factory<GetSelectedGroupIdUseCase> {
        GetSelectedGroupIdUseCaseImpl(
            preferenceRepository = get<GroupPreferenceRepository>()
        )
    }

    factory<GetSelectedGroupNameUseCase> {
        GetSelectedGroupNameUseCaseImpl(
            preferenceRepository = get<GroupPreferenceRepository>()
        )
    }

    factory<GetSelectedGroupCurrencyUseCase> {
        GetSelectedGroupCurrencyUseCaseImpl(
            preferenceRepository = get<GroupPreferenceRepository>()
        )
    }

    factory<SetSelectedGroupUseCase> {
        SetSelectedGroupUseCaseImpl(
            preferenceRepository = get<GroupPreferenceRepository>()
        )
    }

    factory<GetUserDefaultCurrencyUseCase> {
        GetUserDefaultCurrencyUseCaseImpl(
            preferenceRepository = get<UserPreferenceRepository>()
        )
    }

    factory<SetUserDefaultCurrencyUseCase> {
        SetUserDefaultCurrencyUseCaseImpl(
            preferenceRepository = get<UserPreferenceRepository>()
        )
    }

    factory<GetActiveAiEngineUseCase> {
        GetActiveAiEngineUseCaseImpl(
            preferenceRepository = get<UserPreferenceRepository>()
        )
    }

    factory<SetActiveAiEngineUseCase> {
        SetActiveAiEngineUseCaseImpl(
            preferenceRepository = get<UserPreferenceRepository>()
        )
    }

    factory<GetGroupLastUsedCurrencyUseCase> {
        GetGroupLastUsedCurrencyUseCaseImpl(
            preferenceRepository = get<GroupPreferenceRepository>()
        )
    }

    factory<SetGroupLastUsedCurrencyUseCase> {
        SetGroupLastUsedCurrencyUseCaseImpl(
            preferenceRepository = get<GroupPreferenceRepository>()
        )
    }

    factory<GetGroupLastUsedPaymentMethodUseCase> {
        GetGroupLastUsedPaymentMethodUseCaseImpl(
            preferenceRepository = get<GroupPreferenceRepository>()
        )
    }

    factory<SetGroupLastUsedPaymentMethodUseCase> {
        SetGroupLastUsedPaymentMethodUseCaseImpl(
            preferenceRepository = get<GroupPreferenceRepository>()
        )
    }

    factory<GetGroupLastUsedCategoryUseCase> {
        GetGroupLastUsedCategoryUseCaseImpl(
            preferenceRepository = get<GroupPreferenceRepository>()
        )
    }

    factory<SetGroupLastUsedCategoryUseCase> {
        SetGroupLastUsedCategoryUseCaseImpl(
            preferenceRepository = get<GroupPreferenceRepository>()
        )
    }

    factory<GetLastSeenBalanceUseCase> {
        GetLastSeenBalanceUseCaseImpl(
            balancePreferenceRepository = get<BalancePreferenceRepository>()
        )
    }

    factory<SetLastSeenBalanceUseCase> {
        SetLastSeenBalanceUseCaseImpl(
            balancePreferenceRepository = get<BalancePreferenceRepository>()
        )
    }

    factory<GetAppLanguageUseCase> {
        GetAppLanguageUseCaseImpl(
            preferenceRepository = get<UserPreferenceRepository>()
        )
    }

    factory<SetAppLanguageUseCase> {
        SetAppLanguageUseCaseImpl(
            preferenceRepository = get<UserPreferenceRepository>()
        )
    }

    factory<GetShouldShowLanguagePillUseCase> {
        GetShouldShowLanguagePillUseCaseImpl(
            preferenceRepository = get<UserPreferenceRepository>()
        )
    }

    factory<ConsumeLanguagePillUseCase> {
        ConsumeLanguagePillUseCaseImpl(
            preferenceRepository = get<UserPreferenceRepository>()
        )
    }

    factory<GetAppThemeUseCase> {
        GetAppThemeUseCaseImpl(
            preferenceRepository = get<UserPreferenceRepository>()
        )
    }

    factory<SetAppThemeUseCase> {
        SetAppThemeUseCaseImpl(
            preferenceRepository = get<UserPreferenceRepository>()
        )
    }
}
