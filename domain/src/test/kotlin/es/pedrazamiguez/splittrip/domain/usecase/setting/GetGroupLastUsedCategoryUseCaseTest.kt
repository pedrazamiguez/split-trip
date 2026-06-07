package es.pedrazamiguez.splittrip.domain.usecase.setting

import es.pedrazamiguez.splittrip.domain.repository.GroupPreferenceRepository
import es.pedrazamiguez.splittrip.domain.usecase.setting.impl.GetGroupLastUsedCategoryUseCaseImpl
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GetGroupLastUsedCategoryUseCaseTest {

    private val repository: GroupPreferenceRepository = mockk()
    private val useCase = GetGroupLastUsedCategoryUseCaseImpl(repository)

    @Test
    fun `returns category list flow from repository`() = runTest {
        val groupId = "group-123"
        val expectedCategories = listOf("FOOD", "TRANSPORT")
        every { repository.getGroupLastUsedCategory(groupId) } returns flowOf(expectedCategories)

        val result = useCase(groupId).first()

        assertEquals(expectedCategories, result)
    }

    @Test
    fun `returns empty list when no categories stored`() = runTest {
        val groupId = "group-456"
        every { repository.getGroupLastUsedCategory(groupId) } returns flowOf(emptyList())

        val result = useCase(groupId).first()

        assertTrue(result.isEmpty())
    }
}
