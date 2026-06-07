package es.pedrazamiguez.splittrip.domain.usecase.notification

import es.pedrazamiguez.splittrip.domain.enums.NotificationCategory
import es.pedrazamiguez.splittrip.domain.repository.NotificationPreferencesRepository
import es.pedrazamiguez.splittrip.domain.usecase.notification.impl.UpdateNotificationPreferenceUseCaseImpl
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class UpdateNotificationPreferenceUseCaseTest {

    private val repository: NotificationPreferencesRepository = mockk(relaxed = true)
    private val useCase = UpdateNotificationPreferenceUseCaseImpl(repository)

    @Test
    fun `delegates MEMBERSHIP toggle to repository`() = runTest {
        useCase(NotificationCategory.MEMBERSHIP, false)
        coVerify { repository.updatePreference(NotificationCategory.MEMBERSHIP, false) }
    }

    @Test
    fun `delegates EXPENSES toggle to repository`() = runTest {
        useCase(NotificationCategory.EXPENSES, true)
        coVerify { repository.updatePreference(NotificationCategory.EXPENSES, true) }
    }

    @Test
    fun `delegates FINANCIAL toggle to repository`() = runTest {
        useCase(NotificationCategory.FINANCIAL, false)
        coVerify { repository.updatePreference(NotificationCategory.FINANCIAL, false) }
    }
}
