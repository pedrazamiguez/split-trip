package es.pedrazamiguez.splittrip.domain.usecase.setting

import es.pedrazamiguez.splittrip.domain.repository.OnboardingPreferenceRepository
import es.pedrazamiguez.splittrip.domain.usecase.setting.impl.SetOnboardingCompleteUseCaseImpl
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class SetOnboardingCompleteUseCaseTest {

    private val repository: OnboardingPreferenceRepository = mockk(relaxed = true)
    private val useCase = SetOnboardingCompleteUseCaseImpl(repository)

    @Test
    fun `delegates to repository`() = runTest {
        useCase()

        coVerify { repository.setOnboardingComplete() }
    }
}
