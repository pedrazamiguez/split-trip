package es.pedrazamiguez.splittrip.domain.usecase.setting

import es.pedrazamiguez.splittrip.domain.repository.GroupPreferenceRepository
import es.pedrazamiguez.splittrip.domain.usecase.setting.impl.SetSelectedGroupUseCaseImpl
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class SetSelectedGroupUseCaseTest {

    private val repository: GroupPreferenceRepository = mockk(relaxed = true)
    private val useCase = SetSelectedGroupUseCaseImpl(repository)

    @Test
    fun `delegates to repository with groupId, groupName and currency`() = runTest {
        val groupId = "group-123"
        val groupName = "Summer Trip"
        val currency = "EUR"

        useCase(groupId, groupName, currency)

        coVerify { repository.setSelectedGroup(groupId, groupName, currency) }
    }

    @Test
    fun `delegates to repository with null values to deselect group`() = runTest {
        useCase(null, null)

        coVerify { repository.setSelectedGroup(null, null, null) }
    }

    @Test
    fun `defaults currency to null when not provided`() = runTest {
        val groupId = "group-456"
        val groupName = "Road Trip"

        useCase(groupId, groupName)

        coVerify { repository.setSelectedGroup(groupId, groupName, null) }
    }
}
