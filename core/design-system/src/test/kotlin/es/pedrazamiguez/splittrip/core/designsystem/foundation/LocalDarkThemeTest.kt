package es.pedrazamiguez.splittrip.core.designsystem.foundation

import androidx.compose.runtime.CompositionLocal
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class LocalDarkThemeTest {

    @Test
    fun `LocalDarkTheme defaults to null`() {
        assertNotNull(LocalDarkTheme)
        val field = CompositionLocal::class.java.getDeclaredField("defaultValueHolder")
        field.isAccessible = true
        val holder = field.get(LocalDarkTheme)
        val getCurrentMethod = holder.javaClass.getDeclaredMethod("getCurrent")
        getCurrentMethod.isAccessible = true
        val defaultValue = getCurrentMethod.invoke(holder)
        assertNull(defaultValue)
    }
}
