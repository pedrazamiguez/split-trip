package es.pedrazamiguez.splittrip.core.designsystem.extension

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Modifier
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class ModifierExtensionsTest {

    @Test
    fun `springPressFeedback creates a valid modifier`() {
        val interactionSource = MutableInteractionSource()
        val modifier = Modifier.springPressFeedback(interactionSource)

        assertNotNull(modifier)
    }

    @Test
    fun `debouncedClickable with enableSpringPress creates a valid modifier`() {
        val modifier = Modifier.debouncedClickable(
            enableSpringPress = true,
            onClick = {}
        )

        assertNotNull(modifier)
    }

    @Test
    fun `debouncedClickable with interactionSource and enableSpringPress creates a valid modifier`() {
        val interactionSource = MutableInteractionSource()
        val modifier = Modifier.debouncedClickable(
            interactionSource = interactionSource,
            indication = null,
            enableSpringPress = true,
            onClick = {}
        )

        assertNotNull(modifier)
    }

    @Test
    fun `debouncedCombinedClickable with enableSpringPress creates a valid modifier`() {
        val modifier = Modifier.debouncedCombinedClickable(
            enableSpringPress = true,
            onClick = {}
        )

        assertNotNull(modifier)
    }
}
