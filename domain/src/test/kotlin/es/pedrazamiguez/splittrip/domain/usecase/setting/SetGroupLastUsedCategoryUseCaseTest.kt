package es.pedrazamiguez.splittrip.domain.usecase.setting

import es.pedrazamiguez.splittrip.domain.repository.GroupPreferenceRepository
import es.pedrazamiguez.splittrip.domain.usecase.setting.impl.SetGroupLastUsedCategoryUseCaseImpl
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class SetGroupLastUsedCategoryUseCaseTest {

    private val repository: GroupPreferenceRepository = mockk(relaxed = true)
    private val useCase = SetGroupLastUsedCategoryUseCaseImpl(repository)

    @Test
    fun `delegates to repository with group id and category id`() = runTest {
        val groupId = "group-123"
        val categoryId = "FOOD"

        useCase(groupId, categoryId)

        coVerify { repository.setGroupLastUsedCategory(groupId, categoryId) }
    }
}
