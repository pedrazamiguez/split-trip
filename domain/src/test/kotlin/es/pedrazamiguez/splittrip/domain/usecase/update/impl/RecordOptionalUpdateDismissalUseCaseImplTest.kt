package es.pedrazamiguez.splittrip.domain.usecase.update.impl

import es.pedrazamiguez.splittrip.domain.repository.UserPreferenceRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class RecordOptionalUpdateDismissalUseCaseImplTest {

    @Test
    fun `records versionCode with current timestamp in UserPreferenceRepository`() = runTest {
        val userPreferenceRepository = mockk<UserPreferenceRepository>()
        val fixedNow = 1_700_000_000_000L
        coEvery { userPreferenceRepository.recordOptionalUpdateDismissal(1_001_000L, fixedNow) } just runs

        val useCase = RecordOptionalUpdateDismissalUseCaseImpl(
            userPreferenceRepository = userPreferenceRepository,
            currentTimeMillis = { fixedNow }
        )

        useCase(1_001_000L)

        coVerify(exactly = 1) {
            userPreferenceRepository.recordOptionalUpdateDismissal(1_001_000L, fixedNow)
        }
    }

    @Test
    fun `uses system clock by default when recording dismissal`() = runTest {
        val userPreferenceRepository = mockk<UserPreferenceRepository>()
        coEvery { userPreferenceRepository.recordOptionalUpdateDismissal(1_002_000L, any()) } just runs

        val useCase = RecordOptionalUpdateDismissalUseCaseImpl(
            userPreferenceRepository = userPreferenceRepository
        )

        useCase(1_002_000L)

        coVerify(exactly = 1) {
            userPreferenceRepository.recordOptionalUpdateDismissal(1_002_000L, any())
        }
    }
}
