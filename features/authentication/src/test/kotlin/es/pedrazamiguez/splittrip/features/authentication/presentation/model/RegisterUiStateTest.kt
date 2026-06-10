package es.pedrazamiguez.splittrip.features.authentication.presentation.model

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.logging.maskEmail
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RegisterUiStateTest {

    @Test
    fun `toString masks email and passwords correctly`() {
        val state = RegisterUiState(
            email = "test@example.com",
            displayName = "Explorer",
            password = "secretPassword",
            confirmPassword = "secretPassword",
            isLoading = true,
            error = UiText.DynamicString("Error")
        )

        val toStringResult = state.toString()

        assertTrue(toStringResult.contains("email=${state.email.maskEmail()}"))
        assertTrue(toStringResult.contains("displayName=Explorer"))
        assertTrue(toStringResult.contains("password=***"))
        assertTrue(toStringResult.contains("confirmPassword=***"))
        assertTrue(toStringResult.contains("isLoading=true"))
        assertTrue(toStringResult.contains("error=DynamicString(value=Error)"))
    }

    @Test
    fun `toString handles email without @ character`() {
        val state = RegisterUiState(
            email = "invalidemail",
            displayName = "Explorer",
            password = "secretPassword",
            confirmPassword = "secretPassword",
            isLoading = false,
            error = null
        )

        val toStringResult = state.toString()

        assertTrue(toStringResult.contains("email=***"))
    }
}
