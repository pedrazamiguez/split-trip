package es.pedrazamiguez.splittrip.domain.di

import es.pedrazamiguez.splittrip.domain.repository.BalancePreferenceRepository
import es.pedrazamiguez.splittrip.domain.repository.GroupPreferenceRepository
import es.pedrazamiguez.splittrip.domain.repository.OnboardingPreferenceRepository
import es.pedrazamiguez.splittrip.domain.repository.UserPreferenceRepository
import es.pedrazamiguez.splittrip.domain.usecase.setting.ConsumeLanguagePillUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetActiveAiEngineUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetAppLanguageUseCase
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
import es.pedrazamiguez.splittrip.domain.usecase.setting.SetGroupLastUsedCategoryUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.SetGroupLastUsedCurrencyUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.SetGroupLastUsedPaymentMethodUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.SetLastSeenBalanceUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.SetOnboardingCompleteUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.SetSelectedGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.SetUserDefaultCurrencyUseCase
import org.koin.dsl.module

val settingsDomainModule = module {

    factory<IsOnboardingCompleteUseCase> {
        IsOnboardingCompleteUseCase(
            preferenceRepository = get<OnboardingPreferenceRepository>()
        )
    }

    factory<SetOnboardingCompleteUseCase> {
        SetOnboardingCompleteUseCase(
            preferenceRepository = get<OnboardingPreferenceRepository>()
        )
    }

    factory<GetSelectedGroupIdUseCase> {
        GetSelectedGroupIdUseCase(
            preferenceRepository = get<GroupPreferenceRepository>()
        )
    }

    factory<GetSelectedGroupNameUseCase> {
        GetSelectedGroupNameUseCase(
            preferenceRepository = get<GroupPreferenceRepository>()
        )
    }

    factory<GetSelectedGroupCurrencyUseCase> {
        GetSelectedGroupCurrencyUseCase(
            preferenceRepository = get<GroupPreferenceRepository>()
        )
    }

    factory<SetSelectedGroupUseCase> {
        SetSelectedGroupUseCase(
            preferenceRepository = get<GroupPreferenceRepository>()
        )
    }

    factory<GetUserDefaultCurrencyUseCase> {
        GetUserDefaultCurrencyUseCase(
            preferenceRepository = get<UserPreferenceRepository>()
        )
    }

    factory<SetUserDefaultCurrencyUseCase> {
        SetUserDefaultCurrencyUseCase(
            preferenceRepository = get<UserPreferenceRepository>()
        )
    }

    factory<GetActiveAiEngineUseCase> {
        GetActiveAiEngineUseCase(
            preferenceRepository = get<UserPreferenceRepository>()
        )
    }

    factory<SetActiveAiEngineUseCase> {
        SetActiveAiEngineUseCase(
            preferenceRepository = get<UserPreferenceRepository>()
        )
    }

    factory<GetGroupLastUsedCurrencyUseCase> {
        GetGroupLastUsedCurrencyUseCase(
            preferenceRepository = get<GroupPreferenceRepository>()
        )
    }

    factory<SetGroupLastUsedCurrencyUseCase> {
        SetGroupLastUsedCurrencyUseCase(
            preferenceRepository = get<GroupPreferenceRepository>()
        )
    }

    factory<GetGroupLastUsedPaymentMethodUseCase> {
        GetGroupLastUsedPaymentMethodUseCase(
            preferenceRepository = get<GroupPreferenceRepository>()
        )
    }

    factory<SetGroupLastUsedPaymentMethodUseCase> {
        SetGroupLastUsedPaymentMethodUseCase(
            preferenceRepository = get<GroupPreferenceRepository>()
        )
    }

    factory<GetGroupLastUsedCategoryUseCase> {
        GetGroupLastUsedCategoryUseCase(
            preferenceRepository = get<GroupPreferenceRepository>()
        )
    }

    factory<SetGroupLastUsedCategoryUseCase> {
        SetGroupLastUsedCategoryUseCase(
            preferenceRepository = get<GroupPreferenceRepository>()
        )
    }

    factory<GetLastSeenBalanceUseCase> {
        GetLastSeenBalanceUseCase(
            balancePreferenceRepository = get<BalancePreferenceRepository>()
        )
    }

    factory<SetLastSeenBalanceUseCase> {
        SetLastSeenBalanceUseCase(
            balancePreferenceRepository = get<BalancePreferenceRepository>()
        )
    }

    factory<GetAppLanguageUseCase> {
        GetAppLanguageUseCase(
            preferenceRepository = get<UserPreferenceRepository>()
        )
    }

    factory<SetAppLanguageUseCase> {
        SetAppLanguageUseCase(
            preferenceRepository = get<UserPreferenceRepository>()
        )
    }

    factory<GetShouldShowLanguagePillUseCase> {
        GetShouldShowLanguagePillUseCase(
            preferenceRepository = get<UserPreferenceRepository>()
        )
    }

    factory<ConsumeLanguagePillUseCase> {
        ConsumeLanguagePillUseCase(
            preferenceRepository = get<UserPreferenceRepository>()
        )
    }
}
