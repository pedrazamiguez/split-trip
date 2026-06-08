package es.pedrazamiguez.splittrip.domain.usecase.notification

import es.pedrazamiguez.splittrip.domain.model.NotificationPreferences
import es.pedrazamiguez.splittrip.domain.repository.NotificationPreferencesRepository
import es.pedrazamiguez.splittrip.domain.usecase.notification.impl.GetNotificationPreferencesUseCaseImpl
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GetNotificationPreferencesUseCaseTest {

    private val repository: NotificationPreferencesRepository = mockk()
    private val useCase = GetNotificationPreferencesUseCaseImpl(repository)

    @Test
    fun `returns preferences flow from repository`() = runTest {
        val expected = NotificationPreferences(
            membershipEnabled = false,
            expensesEnabled = true,
            financialEnabled = false
        )
        every { repository.getPreferencesFlow() } returns flowOf(expected)

        val result = useCase().first()
        assertEquals(expected, result)
    }

    @Test
    fun `returns default preferences when repository emits defaults`() = runTest {
        val expected = NotificationPreferences()
        every { repository.getPreferencesFlow() } returns flowOf(expected)

        val result = useCase().first()
        assertEquals(true, result.membershipEnabled)
        assertEquals(true, result.expensesEnabled)
        assertEquals(true, result.financialEnabled)
    }
}
