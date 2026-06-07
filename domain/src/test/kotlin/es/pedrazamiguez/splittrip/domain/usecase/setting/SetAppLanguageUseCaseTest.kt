package es.pedrazamiguez.splittrip.domain.usecase.setting

import es.pedrazamiguez.splittrip.domain.repository.UserPreferenceRepository
import es.pedrazamiguez.splittrip.domain.usecase.setting.impl.SetAppLanguageUseCaseImpl
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class SetAppLanguageUseCaseTest {

    private val repository: UserPreferenceRepository = mockk()
    private val useCase = SetAppLanguageUseCaseImpl(repository)

    @Test
    fun `sets app language and updates show pill to true in repository`() = runTest {
        val language = "es"
        coJustRun { repository.setAppLanguage(language) }
        coJustRun { repository.setShouldShowLanguagePill(true) }

        useCase(language)

        coVerify(exactly = 1) {
            repository.setAppLanguage(language)
            repository.setShouldShowLanguagePill(true)
        }
    }
}
