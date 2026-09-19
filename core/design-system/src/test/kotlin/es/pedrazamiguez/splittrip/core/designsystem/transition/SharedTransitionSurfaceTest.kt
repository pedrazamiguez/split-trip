package es.pedrazamiguez.splittrip.core.designsystem.transition

import es.pedrazamiguez.splittrip.core.designsystem.navigation.SharedElementKeys
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SharedTransitionSurfaceTest {

    @Test
    fun `groupCard key formats correctly`() {
        val key = SharedElementKeys.groupCard("group-123")
        assertEquals("group-group-123", key)
    }

    @Test
    fun `expenseCard key formats correctly`() {
        val key = SharedElementKeys.expenseCard("expense-456")
        assertEquals("expense-expense-456", key)
    }

    @Test
    fun `containerSharedTransitionModifier constants match specification`() {
        assertEquals(300, TRANSITION_DURATION_MS)
        assertEquals(0.8f, SPRING_DAMPING_RATIO)
        assertEquals(300f, SPRING_STIFFNESS)
    }
}
