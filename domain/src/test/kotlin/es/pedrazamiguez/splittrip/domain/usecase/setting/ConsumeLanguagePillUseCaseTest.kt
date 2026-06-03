package es.pedrazamiguez.splittrip.domain.usecase.setting

import es.pedrazamiguez.splittrip.domain.repository.UserPreferenceRepository
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class ConsumeLanguagePillUseCaseTest {

    private val repository: UserPreferenceRepository = mockk()
    private val useCase = ConsumeLanguagePillUseCase(repository)

    @Test
    fun `sets should show pill to false in repository`() = runTest {
        coJustRun { repository.setShouldShowLanguagePill(false) }

        useCase()

        coVerify(exactly = 1) {
            repository.setShouldShowLanguagePill(false)
        }
    }
}
