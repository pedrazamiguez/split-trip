package es.pedrazamiguez.splittrip.domain.usecase.setting

import es.pedrazamiguez.splittrip.domain.repository.UserPreferenceRepository
import es.pedrazamiguez.splittrip.domain.usecase.setting.impl.SetAppThemeUseCaseImpl
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class SetAppThemeUseCaseTest {

    private val repository: UserPreferenceRepository = mockk()
    private val useCase = SetAppThemeUseCaseImpl(repository)

    @Test
    fun `sets app theme in repository`() = runTest {
        val theme = "LIGHT"
        coJustRun { repository.setAppTheme(theme) }

        useCase(theme)

        coVerify(exactly = 1) {
            repository.setAppTheme(theme)
        }
    }
}
