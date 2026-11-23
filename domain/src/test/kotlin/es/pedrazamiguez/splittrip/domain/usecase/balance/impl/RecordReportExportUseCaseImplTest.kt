package es.pedrazamiguez.splittrip.domain.usecase.balance.impl

import es.pedrazamiguez.splittrip.domain.repository.UserPreferenceRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import java.time.YearMonth
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RecordReportExportUseCaseImplTest {

    private lateinit var userPreferenceRepository: UserPreferenceRepository
    private val fixedMonth = YearMonth.of(2026, 11)
    private lateinit var useCase: RecordReportExportUseCaseImpl

    @BeforeEach
    fun setUp() {
        userPreferenceRepository = mockk()
        coEvery { userPreferenceRepository.incrementMonthlyReportExportCount(any()) } just runs
        useCase = RecordReportExportUseCaseImpl(
            userPreferenceRepository = userPreferenceRepository,
            yearMonthProvider = { fixedMonth }
        )
    }

    @Test
    fun `invoke increments monthly report export count for current calendar month`() = runTest {
        useCase()

        coVerify(exactly = 1) {
            userPreferenceRepository.incrementMonthlyReportExportCount(fixedMonth)
        }
    }
}
