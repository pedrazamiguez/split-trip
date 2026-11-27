package es.pedrazamiguez.splittrip.core.designsystem.presentation.notification

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("TopPillController")
class TopPillControllerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private lateinit var controller: TopPillController

    @BeforeEach
    fun setUp() {
        controller = TopPillController(testScope)
    }

    @Nested
    @DisplayName("Initial state")
    inner class InitialState {

        @Test
        fun `pillState is null initially`() {
            assertNull(controller.pillState.value)
        }
    }

    @Nested
    @DisplayName("showPill")
    inner class ShowPill {

        @Test
        fun `sets pill state with the given message`() = testScope.runTest {
            controller.showPill("Group deleted")
            advanceUntilIdle()

            val state = controller.pillState.value
            assertNotNull(state)
            assertEquals("Group deleted", state?.message)
        }

        @Test
        fun `each call generates a unique id`() = testScope.runTest {
            controller.showPill("First")
            advanceUntilIdle()
            val firstId = controller.pillState.value?.id

            controller.showPill("Second")
            advanceUntilIdle()
            val secondId = controller.pillState.value?.id

            assertNotNull(firstId)
            assertNotNull(secondId)
            assertNotEquals(firstId, secondId, "Expected different IDs for successive pills")
        }

        @Test
        fun `replaces previous pill when called again`() = testScope.runTest {
            controller.showPill("Old message")
            advanceUntilIdle()

            controller.showPill("New message")
            advanceUntilIdle()

            assertEquals("New message", controller.pillState.value?.message)
        }

        @Test
        fun `defaults style to NEUTRAL when style is omitted`() = testScope.runTest {
            controller.showPill("Saved")
            advanceUntilIdle()

            assertEquals(TopPillStyle.NEUTRAL, controller.pillState.value?.style)
        }

        @Test
        fun `sets pill state with the given semantic style`() = testScope.runTest {
            TopPillStyle.entries.forEach { style ->
                controller.showPill("Message", style = style)
                advanceUntilIdle()

                assertEquals(style, controller.pillState.value?.style)
            }
        }

        @Test
        fun `replaces previous pill style when called again with a different style`() = testScope.runTest {
            controller.showPill("Failed", style = TopPillStyle.ERROR)
            advanceUntilIdle()

            controller.showPill("Recovered", style = TopPillStyle.SUCCESS)
            advanceUntilIdle()

            assertEquals("Recovered", controller.pillState.value?.message)
            assertEquals(TopPillStyle.SUCCESS, controller.pillState.value?.style)
        }
    }

    @Nested
    @DisplayName("dismiss")
    inner class Dismiss {

        @Test
        fun `clears pill state`() = testScope.runTest {
            controller.showPill("Will be dismissed")
            advanceUntilIdle()
            assertNotNull(controller.pillState.value)

            controller.dismiss()

            assertNull(controller.pillState.value)
        }

        @Test
        fun `is a no-op when no pill is showing`() {
            controller.dismiss()

            assertNull(controller.pillState.value)
        }
    }
}
