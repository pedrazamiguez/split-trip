package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.scaffold

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ScrollAwareHeaderStateTest {

    @Test
    fun `initial state has zero offset, zero height, isCollapsed false, expandedFraction 1f`() {
        val state = ScrollAwareHeaderState()

        assertEquals(0, state.heightPx)
        assertEquals(0f, state.offsetPx)
        assertFalse(state.isCollapsed)
        assertEquals(1f, state.expandedFraction)
    }

    @Test
    fun `onScrollDelta is a no-op when heightPx is zero`() {
        val state = ScrollAwareHeaderState()

        state.onScrollDelta(-50f)

        assertEquals(0f, state.offsetPx)
    }

    @Test
    fun `onScrollDelta with negative delta collapses header and clamps at negative heightPx`() {
        val state = ScrollAwareHeaderState()
        state.updateHeight(100)

        state.onScrollDelta(-40f)
        assertEquals(-40f, state.offsetPx)
        assertEquals(0.6f, state.expandedFraction)
        assertFalse(state.isCollapsed)

        state.onScrollDelta(-200f)
        assertEquals(-100f, state.offsetPx)
        assertEquals(0f, state.expandedFraction)
        assertTrue(state.isCollapsed)
    }

    @Test
    fun `onScrollDelta with positive delta reveals header and clamps at zero`() {
        val state = ScrollAwareHeaderState()
        state.updateHeight(100)
        state.onScrollDelta(-100f)

        state.onScrollDelta(30f)
        assertEquals(-70f, state.offsetPx)

        state.onScrollDelta(200f)
        assertEquals(0f, state.offsetPx)
        assertEquals(1f, state.expandedFraction)
        assertFalse(state.isCollapsed)
    }

    @Test
    fun `resolveSnapTarget always returns 0f when first item is below top edge`() {
        val state = ScrollAwareHeaderState()
        state.updateHeight(100)
        state.onScrollDelta(-80f)

        val target = state.resolveSnapTarget(isFirstItemBelowTopEdge = true)

        assertEquals(0f, target)
    }

    @Test
    fun `resolveSnapTarget returns 0f when more than half of header is visible`() {
        val state = ScrollAwareHeaderState()
        state.updateHeight(100)
        state.onScrollDelta(-49f)

        val target = state.resolveSnapTarget(isFirstItemBelowTopEdge = false)

        assertEquals(0f, target)
    }

    @Test
    fun `resolveSnapTarget returns negative heightPx when half or less of header is visible`() {
        val state = ScrollAwareHeaderState()
        state.updateHeight(100)

        state.onScrollDelta(-50f)
        assertEquals(-100f, state.resolveSnapTarget(isFirstItemBelowTopEdge = false))

        state.onScrollDelta(-25f)
        assertEquals(-100f, state.resolveSnapTarget(isFirstItemBelowTopEdge = false))
    }

    @Test
    fun `resolveSnapTarget returns 0f when heightPx is zero`() {
        val state = ScrollAwareHeaderState()

        assertEquals(0f, state.resolveSnapTarget(isFirstItemBelowTopEdge = false))
    }

    @Test
    fun `expand resets offsetPx to zero immediately`() {
        val state = ScrollAwareHeaderState()
        state.updateHeight(100)
        state.onScrollDelta(-100f)
        assertTrue(state.isCollapsed)

        state.expand()

        assertEquals(0f, state.offsetPx)
        assertFalse(state.isCollapsed)
    }

    @Test
    fun `updateHeight re-clamps existing offsetPx when height shrinks`() {
        val state = ScrollAwareHeaderState()
        state.updateHeight(100)
        state.onScrollDelta(-90f)
        assertEquals(-90f, state.offsetPx)

        state.updateHeight(60)

        assertEquals(-60f, state.offsetPx)
        assertTrue(state.isCollapsed)
    }

    @Test
    fun `updateHeight coerces negative height to zero`() {
        val state = ScrollAwareHeaderState()

        state.updateHeight(-10)

        assertEquals(0, state.heightPx)
        assertEquals(0f, state.offsetPx)
    }
}
