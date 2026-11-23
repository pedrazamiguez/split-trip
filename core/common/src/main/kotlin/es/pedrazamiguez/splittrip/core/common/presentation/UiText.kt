package es.pedrazamiguez.splittrip.core.common.presentation

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes

/**
 * A sealed interface that represents text that can be displayed in the UI.
 *
 * This pattern allows ViewModels to remain context-free while handling complex
 * string formatting, including strings with arguments.
 *
 * Usage in ViewModel:
 * ```
 * _actions.emit(
 *     UiAction.ShowMessage(UiText.StringResource(R.string.welcome_user, userName))
 * )
 * ```
 *
 * Usage in Feature (with Context):
 * ```
 * LaunchedEffect(Unit) {
 *     viewModel.actions.collectLatest { action ->
 *         pillController.showPill(message = action.message.asString(context))
 *     }
 * }
 * ```
 *
 * Usage in Composable (with extension from design-system):
 * ```
 * Text(text = uiState.message.asString())
 * ```
 */
sealed interface UiText {

    /**
     * Represents a dynamic string value that doesn't need resource lookup.
     * Use this for strings that come from backend/API or are computed at runtime.
     */
    data class DynamicString(val value: String) : UiText

    /**
     * Represents a string resource with optional format arguments.
     * Use this for localized strings from resources.
     *
     * @param resId The string resource ID
     * @param args Optional format arguments for the string
     */
    class StringResource(
        @param:StringRes
        val resId: Int,
        vararg val args: Any
    ) : UiText {
        // Override equals/hashCode since vararg creates an array
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is StringResource) return false
            return resId == other.resId && args.contentEquals(other.args)
        }

        override fun hashCode(): Int {
            var result = resId
            result = 31 * result + args.contentHashCode()
            return result
        }
    }

    /**
     * Represents a plural string resource resolved with [quantity] and optional format arguments.
     *
     * @param resId The plurals resource ID
     * @param quantity The count used to select the plural form (e.g., `one` vs `other`)
     * @param args Optional format arguments for the selected plural string
     */
    class PluralResource(
        @param:PluralsRes
        val resId: Int,
        val quantity: Int,
        vararg val args: Any
    ) : UiText {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is PluralResource) return false
            return resId == other.resId && quantity == other.quantity && args.contentEquals(other.args)
        }

        override fun hashCode(): Int {
            var result = resId
            result = 31 * result + quantity
            result = 31 * result + args.contentHashCode()
            return result
        }
    }
}

/**
 * Resolves the [UiText] to a string using a Context.
 * Use this when you need to resolve the string outside of a Composable context,
 * such as in a LaunchedEffect or coroutine.
 */
@Suppress("SpreadOperator") // Spread is unavoidable for vararg-to-vararg delegation to Context.getString()
fun UiText.asString(context: Context): String = when (this) {
    is UiText.DynamicString -> value
    is UiText.StringResource -> {
        if (args.isEmpty()) {
            context.getString(resId)
        } else {
            context.getString(resId, *args.resolveUiTextArgs(context))
        }
    }
    is UiText.PluralResource -> {
        if (args.isEmpty()) {
            context.resources.getQuantityString(resId, quantity)
        } else {
            context.resources.getQuantityString(resId, quantity, *args.resolveUiTextArgs(context))
        }
    }
}

private fun Array<out Any>.resolveUiTextArgs(context: Context): Array<Any> =
    map { arg -> if (arg is UiText) arg.asString(context) else arg }.toTypedArray()
