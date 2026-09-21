package es.pedrazamiguez.splittrip.core.designsystem.extension

import androidx.compose.foundation.Indication
import androidx.compose.foundation.interaction.MutableInteractionSource

/**
 * Encapsulates interaction source, indication, and spring press feedback configuration.
 *
 * @param interactionSource The [MutableInteractionSource] tracking touch interactions.
 * @param indication The [Indication] to show on touch, or `null` for no indication.
 * @param enableSpringPress Whether to apply spring scale feedback on press.
 */
data class ClickableInteraction(
    val interactionSource: MutableInteractionSource,
    val indication: Indication?,
    val enableSpringPress: Boolean = false
)
