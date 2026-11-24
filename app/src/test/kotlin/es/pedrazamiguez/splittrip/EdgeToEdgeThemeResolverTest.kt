package es.pedrazamiguez.splittrip

import es.pedrazamiguez.splittrip.domain.enums.AppTheme
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EdgeToEdgeThemeResolverTest {

    @Test
    fun `resolveDarkTheme returns true when theme is DARK and system is light`() {
        assertTrue(resolveDarkTheme(themeCode = AppTheme.DARK.code, isSystemDark = false))
    }

    @Test
    fun `resolveDarkTheme returns true when theme is DARK and system is dark`() {
        assertTrue(resolveDarkTheme(themeCode = AppTheme.DARK.code, isSystemDark = true))
    }

    @Test
    fun `resolveDarkTheme returns false when theme is LIGHT and system is dark`() {
        assertFalse(resolveDarkTheme(themeCode = AppTheme.LIGHT.code, isSystemDark = true))
    }

    @Test
    fun `resolveDarkTheme returns false when theme is LIGHT and system is light`() {
        assertFalse(resolveDarkTheme(themeCode = AppTheme.LIGHT.code, isSystemDark = false))
    }

    @Test
    fun `resolveDarkTheme follows isSystemDark when theme is SYSTEM`() {
        assertTrue(resolveDarkTheme(themeCode = AppTheme.SYSTEM.code, isSystemDark = true))
        assertFalse(resolveDarkTheme(themeCode = AppTheme.SYSTEM.code, isSystemDark = false))
    }

    @Test
    fun `resolveDarkTheme falls back to isSystemDark when themeCode is null or unknown`() {
        assertEquals(true, resolveDarkTheme(themeCode = null, isSystemDark = true))
        assertEquals(false, resolveDarkTheme(themeCode = null, isSystemDark = false))
        assertEquals(true, resolveDarkTheme(themeCode = "invalid", isSystemDark = true))
        assertEquals(false, resolveDarkTheme(themeCode = "invalid", isSystemDark = false))
    }
}
