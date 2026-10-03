package es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel.state

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class EditProfileUiStateTest {

    @Nested
    inner class IsSaveEnabled {

        @Test
        fun `isSaveEnabled is true by default when not loading and no errors`() {
            val state = EditProfileUiState()
            assertTrue(state.isSaveEnabled)
        }

        @Test
        fun `isSaveEnabled is false when isSaving is true`() {
            val state = EditProfileUiState(isSaving = true)
            assertFalse(state.isSaveEnabled)
        }

        @Test
        fun `isSaveEnabled is false when isLoading is true`() {
            val state = EditProfileUiState(isLoading = true)
            assertFalse(state.isSaveEnabled)
        }

        @Test
        fun `isSaveEnabled is false when displayNameError is present`() {
            val state = EditProfileUiState(displayNameError = UiText.DynamicString("Name error"))
            assertFalse(state.isSaveEnabled)
        }

        @Test
        fun `isSaveEnabled is false when bioError is present`() {
            val state = EditProfileUiState(bioError = UiText.DynamicString("Bio error"))
            assertFalse(state.isSaveEnabled)
        }
    }
}
