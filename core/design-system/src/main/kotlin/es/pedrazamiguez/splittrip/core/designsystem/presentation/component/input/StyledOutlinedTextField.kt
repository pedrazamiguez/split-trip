package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing

/**
 * A styled text field component (Horizon "Soft Field" design) that provides consistent
 * styling across the app.
 *
 * Implements the Horizon Narrative §5 "Soft Field" pattern:
 * - The [label] is rendered as a **static external label** above the field — no floating
 *   animation, no spring-bounce.
 * - Background: `surfaceContainerLow` — no visible border at rest.
 * - On focus: a full `outline` "ghost border" at 20 % opacity fades in (all four sides).
 * - Error state: always shows a visible `error`-coloured border.
 *
 * The public API is intentionally unchanged — all call-sites require no modification.
 *
 * @param value The input text to be shown in the text field
 * @param onValueChange The callback that is triggered when the input value changes
 * @param modifier Modifier applied to the outer [Column] wrapper
 * @param label Optional label rendered statically above the field
 * @param placeholder Optional placeholder shown inside the field when empty
 * @param leadingIcon Optional leading icon at the beginning of the field
 * @param trailingIcon Optional trailing icon at the end of the field
 * @param prefix Optional prefix text displayed before the input
 * @param suffix Optional suffix text displayed after the input
 * @param supportingText Optional supporting / error text displayed below the field
 * @param isError Whether the field is in error state
 * @param enabled Whether the field is enabled
 * @param readOnly Whether the field is read-only (no editing but can be focused)
 * @param singleLine Whether the field should be a single line
 * @param maxLines Maximum number of visible lines
 * @param minLines Minimum number of visible lines
 * @param visualTransformation Transforms the visual representation of the input
 * @param keyboardType The type of keyboard to show
 * @param imeAction The IME action to show
 * @param capitalization The capitalization behavior
 * @param keyboardActions Keyboard actions to perform
 * @param onClick Optional click handler for read-only fields (e.g., dropdown triggers)
 * @param focusRequester Optional [FocusRequester] to programmatically request focus
 * @param moveCursorToEndOnFocus When `true`, the cursor moves to the end of the text on
 *                               programmatic focus. Use with [focusRequester] +
 *                               [rememberAutoFocusRequester].
 * @param focusable Whether the field can receive focus. Set to `false` for dropdown triggers
 *                  that must not steal focus from an adjacent editable field.
 * @param shape The shape of the field container
 * @param colors Custom colors for the field
 */
@Suppress("LongMethod", "LongParameterList", "CognitiveComplexMethod") // Compose UI builder DSL — not procedural logic
@Composable
fun StyledOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Default,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    onClick: (() -> Unit)? = null,
    focusRequester: FocusRequester? = null,
    moveCursorToEndOnFocus: Boolean = false,
    focusable: Boolean = true,
    shape: Shape = OutlinedTextFieldDefaults.shape,
    colors: TextFieldColors = softFieldColors()
) {
    val interactionSource = remember { MutableInteractionSource() }

    // Focus-related modifiers scoped to the inner field only (not the outer Column).
    // The label's text is injected as a content description so TalkBack can announce
    // the field's accessible name when `label = null` is passed to OutlinedTextField.
    val innerModifier = Modifier
        .fillMaxWidth()
        .then(if (label != null) Modifier.semantics { contentDescription = label } else Modifier)
        .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
        .then(if (!focusable) Modifier.focusProperties { canFocus = false } else Modifier)

    val fieldConfig = TextFieldConfig(
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        prefix = prefix,
        suffix = suffix,
        supportingText = supportingText,
        isError = isError,
        enabled = enabled,
        readOnly = readOnly,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction,
            capitalization = capitalization
        ),
        keyboardActions = keyboardActions,
        interactionSource = interactionSource,
        shape = shape,
        colors = colors
    )

    Column(modifier = modifier) {
        // Static external label — placed above the field with no animation.
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = when {
                    !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    isError -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.padding(start = MaterialTheme.spacing.ExtraSmall, bottom = 6.dp)
            )
        }

        when {
            readOnly && onClick != null -> ReadOnlyClickableTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = innerModifier,
                config = fieldConfig,
                onClick = onClick
            )
            moveCursorToEndOnFocus -> CursorToEndTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = innerModifier,
                config = fieldConfig
            )
            else -> StandardTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = innerModifier,
                config = fieldConfig
            )
        }
    }
}

/**
 * Bundles the common [OutlinedTextField] visual/behavioral parameters so they can be
 * forwarded to the three internal variants without repeating each one.
 *
 * Note: [label] is intentionally absent — the external static label above the field is
 * rendered directly in [StyledOutlinedTextField] and does not participate in M3's floating
 * label animation.
 */
private data class TextFieldConfig(
    val placeholder: String?,
    val leadingIcon: @Composable (() -> Unit)?,
    val trailingIcon: @Composable (() -> Unit)?,
    val prefix: @Composable (() -> Unit)?,
    val suffix: @Composable (() -> Unit)?,
    val supportingText: String?,
    val isError: Boolean,
    val enabled: Boolean,
    val readOnly: Boolean,
    val singleLine: Boolean,
    val maxLines: Int,
    val minLines: Int,
    val visualTransformation: VisualTransformation,
    val keyboardOptions: KeyboardOptions,
    val keyboardActions: KeyboardActions,
    val interactionSource: MutableInteractionSource,
    val shape: Shape,
    val colors: TextFieldColors
)

/**
 * Read-only variant wrapped in a clickable overlay — used for dropdown triggers.
 */
@Composable
private fun ReadOnlyClickableTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
    config: TextFieldConfig,
    onClick: () -> Unit
) {
    Box {
        CoreOutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            config = config.copy(readOnly = true)
        )
        // Invisible overlay to capture click without ripple
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onClick
                )
        )
    }
}

/**
 * Variant that uses [TextFieldValue] internally to move the cursor to the end
 * when the field gains programmatic focus.
 */
@Composable
private fun CursorToEndTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
    config: TextFieldConfig
) {
    var internalTfv by remember { mutableStateOf(TextFieldValue(value)) }
    // Sync external value changes (e.g. ViewModel clears or resets the field).
    // Guard against overriding the cursor while the user is actively typing.
    LaunchedEffect(value) {
        if (internalTfv.text != value) {
            internalTfv = TextFieldValue(value, TextRange(value.length))
        }
    }
    OutlinedTextField(
        value = internalTfv,
        onValueChange = { newTfv ->
            internalTfv = newTfv
            onValueChange(newTfv.text)
        },
        modifier = modifier.onFocusChanged { focusState ->
            if (focusState.isFocused &&
                internalTfv.selection.start == 0 &&
                internalTfv.text.isNotEmpty()
            ) {
                internalTfv = internalTfv.copy(selection = TextRange(internalTfv.text.length))
            }
        },
        label = null,
        placeholder = config.placeholder?.let { { Text(it) } },
        leadingIcon = config.leadingIcon,
        trailingIcon = config.trailingIcon,
        prefix = config.prefix,
        suffix = config.suffix,
        supportingText = config.supportingText?.let { { Text(it) } },
        isError = config.isError,
        enabled = config.enabled,
        readOnly = config.readOnly,
        singleLine = config.singleLine,
        maxLines = config.maxLines,
        minLines = config.minLines,
        visualTransformation = config.visualTransformation,
        keyboardOptions = config.keyboardOptions,
        keyboardActions = config.keyboardActions,
        interactionSource = config.interactionSource,
        shape = config.shape,
        colors = config.colors
    )
}

/**
 * Plain editable text field — the default branch.
 */
@Composable
private fun StandardTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
    config: TextFieldConfig
) {
    CoreOutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        config = config
    )
}

/**
 * Shared [OutlinedTextField] call used by [ReadOnlyClickableTextField] and
 * [StandardTextField] — avoids duplicating the full parameter list.
 */
@Composable
private fun CoreOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
    config: TextFieldConfig
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = null,
        placeholder = config.placeholder?.let { { Text(it) } },
        leadingIcon = config.leadingIcon,
        trailingIcon = config.trailingIcon,
        prefix = config.prefix,
        suffix = config.suffix,
        supportingText = config.supportingText?.let { { Text(it) } },
        isError = config.isError,
        enabled = config.enabled,
        readOnly = config.readOnly,
        singleLine = config.singleLine,
        maxLines = config.maxLines,
        minLines = config.minLines,
        visualTransformation = config.visualTransformation,
        keyboardOptions = config.keyboardOptions,
        keyboardActions = config.keyboardActions,
        interactionSource = config.interactionSource,
        shape = config.shape,
        colors = config.colors
    )
}

/**
 * Returns the Horizon "Soft Field" colors for [StyledOutlinedTextField].
 *
 * Implements the §5 "Soft Field" pattern from the Horizon Narrative:
 * - **Container**: `surfaceContainerLow` in all states — fields look like soft,
 *   borderless containers that barely lift from the page background, keeping the
 *   overall UI light and bright without appearing disabled.
 * - **Border at rest**: fully transparent — no outline is visible.
 * - **Border on focus**: `outline` colour at 20 % opacity — a subtle ghost border appears
 *   on all four sides, giving the user clear focus feedback without visual noise.
 * - **Border in error**: full `error` colour — always visible regardless of focus state.
 * - **Disabled/read-only**: transparent border and same container colour for consistency
 *   with the rest of the form (used by dropdown triggers).
 */
@Composable
fun softFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    // Container — Soft Field background (surfaceContainerLow in all states)
    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    errorContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,

    // Border — transparent at rest, ghost on focus, full error colour in error state
    focusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.20f),
    unfocusedBorderColor = Color.Transparent,
    disabledBorderColor = Color.Transparent,
    errorBorderColor = MaterialTheme.colorScheme.error,

    // Text
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    disabledTextColor = MaterialTheme.colorScheme.onSurface,
    errorTextColor = MaterialTheme.colorScheme.onSurface,

    // Placeholder (shown when field is empty, no label inside)
    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
    errorPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,

    // Icon colors for disabled state (e.g., read-only dropdown triggers)
    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,

    // Supporting text (error message etc.)
    disabledSupportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
    errorSupportingTextColor = MaterialTheme.colorScheme.error,

    // Cursor and selection
    cursorColor = MaterialTheme.colorScheme.primary,
    errorCursorColor = MaterialTheme.colorScheme.error
)
