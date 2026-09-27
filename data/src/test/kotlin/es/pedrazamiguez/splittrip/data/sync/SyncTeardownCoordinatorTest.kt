package es.pedrazamiguez.splittrip.data.sync

import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class SyncTeardownCoordinatorTest {

    @Test
    fun `teardownAll executes registered actions`() = runTest {
        val coordinator = SyncTeardownCoordinator()
        val callback1 = mockk<() -> Unit>(relaxed = true)
        val callback2 = mockk<() -> Unit>(relaxed = true)

        coordinator.registerAction(callback1)
        coordinator.registerAction(callback2)

        coordinator.teardownAll()

        verify(exactly = 1) { callback1.invoke() }
        verify(exactly = 1) { callback2.invoke() }
    }
}
