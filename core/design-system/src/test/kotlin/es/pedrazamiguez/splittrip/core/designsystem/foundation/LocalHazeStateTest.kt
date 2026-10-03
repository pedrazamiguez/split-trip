package es.pedrazamiguez.splittrip.core.designsystem.foundation

import androidx.compose.runtime.CompositionLocal
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalTopPadding
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class LocalHazeStateTest {

    @Test
    fun `LocalHazeState defaults to null`() {
        assertNotNull(LocalHazeState)
        val field = CompositionLocal::class.java.getDeclaredField("defaultValueHolder")
        field.isAccessible = true
        val holder = field.get(LocalHazeState)
        val getCurrentMethod = holder.javaClass.getDeclaredMethod("getCurrent")
        getCurrentMethod.isAccessible = true
        val defaultValue = getCurrentMethod.invoke(holder)
        assertNull(defaultValue)
    }

    @Test
    fun `LocalTopPadding defaults to 0dp`() {
        assertNotNull(LocalTopPadding)
        val field = CompositionLocal::class.java.getDeclaredField("defaultValueHolder")
        field.isAccessible = true
        val holder = field.get(LocalTopPadding)
        val getCurrentMethod = holder.javaClass.getDeclaredMethod("getCurrent")
        getCurrentMethod.isAccessible = true
        val defaultValue = getCurrentMethod.invoke(holder)
        assertEquals(0.dp, defaultValue)
    }
}
