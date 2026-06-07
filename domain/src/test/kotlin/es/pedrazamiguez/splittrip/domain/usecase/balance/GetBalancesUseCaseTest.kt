package es.pedrazamiguez.splittrip.domain.usecase.balance

import es.pedrazamiguez.splittrip.domain.usecase.balance.impl.GetBalancesUseCaseImpl
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GetBalancesUseCaseTest {

    private val useCase = GetBalancesUseCaseImpl()

    @Test
    fun `invoke returns success with empty list`() = runTest {
        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals(emptyList<Any>(), result.getOrNull())
    }
}
