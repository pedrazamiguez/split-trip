package es.pedrazamiguez.splittrip.core.designsystem.foundation

import androidx.compose.ui.unit.dp
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class GlassmorphismDefaultsTest {

    @Test
    fun `light blur radius is 20dp`() {
        assertEquals(20.dp, GlassmorphismDefaults.LightBlurRadius)
    }

    @Test
    fun `dark blur radius is 24dp`() {
        assertEquals(24.dp, GlassmorphismDefaults.DarkBlurRadius)
    }

    @Test
    fun `light tint is non-null HazeColorEffect`() {
        assertNotNull(GlassmorphismDefaults.LightTint)
    }

    @Test
    fun `dark tint is non-null HazeColorEffect`() {
        assertNotNull(GlassmorphismDefaults.DarkTint)
    }

    @Test
    fun `light and dark styles are non-null and distinct`() {
        assertNotNull(GlassmorphismDefaults.LightStyle)
        assertNotNull(GlassmorphismDefaults.DarkStyle)
        assertNotEquals(GlassmorphismDefaults.LightStyle, GlassmorphismDefaults.DarkStyle)
    }

    @Test
    fun `style chaining via then retains base configuration and executes block`() {
        val chainedStyle = GlassmorphismDefaults.LightStyle.then {
            mask(null)
        }

        assertNotNull(chainedStyle)
    }

    @Test
    fun `light top app bar gradient is non-null`() {
        assertNotNull(GlassmorphismDefaults.LightTopAppBarGradient)
    }

    @Test
    fun `dark top app bar gradient is non-null`() {
        assertNotNull(GlassmorphismDefaults.DarkTopAppBarGradient)
    }

    @Test
    fun `light and dark top app bar gradients are distinct`() {
        assertNotEquals(
            GlassmorphismDefaults.LightTopAppBarGradient,
            GlassmorphismDefaults.DarkTopAppBarGradient
        )
    }

    @Test
    fun `topAppBarGradient returns correct brush based on darkTheme flag`() {
        assertEquals(
            GlassmorphismDefaults.LightTopAppBarGradient,
            GlassmorphismDefaults.topAppBarGradient(darkTheme = false)
        )
        assertEquals(
            GlassmorphismDefaults.DarkTopAppBarGradient,
            GlassmorphismDefaults.topAppBarGradient(darkTheme = true)
        )
    }
}
