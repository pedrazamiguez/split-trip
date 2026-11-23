package es.pedrazamiguez.splittrip.features.profile.presentation.component

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ProfileAvatarInitialsTest {

    @Test
    fun `extractAvatarInitials returns two uppercase initials for two-word display name`() {
        assertEquals("AG", extractAvatarInitials("Antonio García"))
    }

    @Test
    fun `extractAvatarInitials returns first two initials for multi-word display name`() {
        assertEquals("JC", extractAvatarInitials("Juan Carlos Pérez"))
    }

    @Test
    fun `extractAvatarInitials returns single uppercase initial for single-word display name`() {
        assertEquals("A", extractAvatarInitials("andrés"))
    }

    @Test
    fun `extractAvatarInitials extracts initials from email local-part when display name is an email`() {
        assertEquals("JD", extractAvatarInitials("jane.doe@example.com"))
        assertEquals("T", extractAvatarInitials("test@example.com"))
    }

    @Test
    fun `extractAvatarInitials returns empty string when display name is blank`() {
        assertEquals("", extractAvatarInitials(""))
        assertEquals("", extractAvatarInitials("   "))
    }
}
