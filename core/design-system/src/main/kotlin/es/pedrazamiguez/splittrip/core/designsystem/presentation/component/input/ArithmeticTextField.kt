package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import es.pedrazamiguez.splittrip.core.designsystem.constant.UiConstants
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.rememberLocale
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.formatForDisplay
import es.pedrazamiguez.splittrip.domain.service.calculator.ExpressionCalculatorService
import es.pedrazamiguez.splittrip.domain.service.calculator.ExpressionResult
import java.math.RoundingMode
import java.util.Locale

@Suppress("LongMethod", "LongParameterList", "CognitiveComplexMethod", "CyclomaticComplexMethod")
@Composable
fun ArithmeticTextField(
    value: String,
    onValueChange: (String) -> Unit,
    evaluator: ExpressionCalculatorService,
    modifier: Modifier = Modifier,
    displayValue: String? = null,
    maxDecimalPlaces: Int = UiConstants.DEFAULT_MAX_DECIMAL_PLACES,
    minDecimalPlaces: Int = 0,
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
    moveCursorToEndOnFocus: Boolean = true,
    focusable: Boolean = true,
    shape: Shape = OutlinedTextFieldDefaults.shape,
    colors: TextFieldColors = softFieldColors()
) {
    val fieldId = remember { Any() }
    var isFocused by remember { mutableStateOf(false) }
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = value, selection = TextRange(value.length)))
    }
    var evaluationResult by remember { mutableStateOf<ExpressionResult?>(null) }
    val keyboardState = LocalArithmeticKeyboardState.current
    val locale = rememberLocale()

    LaunchedEffect(textFieldValue.text) {
        evaluationResult = evaluator.evaluate(textFieldValue.text)
    }

    LaunchedEffect(value, isFocused) {
        if (!isFocused) {
            textFieldValue = TextFieldValue(text = value, selection = TextRange(value.length))
        }
    }

    val commitResult = {
        val res = evaluationResult
        if (res is ExpressionResult.Success) {
            val scaledValue = res.value.setScale(maxDecimalPlaces, RoundingMode.HALF_UP).stripTrailingZeros()
            val rawValue = scaledValue.toPlainString()
            onValueChange(rawValue)
            textFieldValue = TextFieldValue(text = rawValue, selection = TextRange(rawValue.length))
        } else {
            onValueChange(textFieldValue.text)
        }
    }

    val resolvedDisplayValue = resolveDisplayValue(
        value = value,
        displayValue = displayValue,
        locale = locale,
        maxDecimalPlaces = maxDecimalPlaces,
        minDecimalPlaces = minDecimalPlaces
    )

    LaunchedEffect(isFocused, textFieldValue, evaluationResult) {
        if (isFocused) {
            keyboardState.value = ArithmeticKeyboardState(
                isVisible = true,
                expressionBuffer = textFieldValue.text,
                evaluationResult = evaluationResult,
                maxDecimalPlaces = maxDecimalPlaces,
                minDecimalPlaces = minDecimalPlaces,
                owner = fieldId,
                onClear = {
                    textFieldValue = TextFieldValue(text = "", selection = TextRange.Zero)
                },
                onOperatorClick = { op ->
                    val text = textFieldValue.text
                    val selection = textFieldValue.selection
                    val newText = buildString {
                        append(text.substring(0, selection.min))
                        append(op)
                        append(text.substring(selection.max))
                    }
                    val newCursorPosition = selection.min + op.length
                    textFieldValue = TextFieldValue(
                        text = newText,
                        selection = TextRange(newCursorPosition)
                    )
                },
                onCommit = commitResult
            )
        } else {
            // Delay hide to allow other fields to take focus smoothly?
            // Simple hide is enough
            if (keyboardState.value.owner == fieldId) {
                keyboardState.value = keyboardState.value.copy(isVisible = false)
            }
        }
    }

    DisposableEffect(fieldId) {
        onDispose {
            if (keyboardState.value.owner == fieldId) {
                keyboardState.value = keyboardState.value.copy(isVisible = false)
            }
        }
    }

    val wrappedKeyboardActions = KeyboardActions(
        onDone = {
            commitResult()
            keyboardActions.onDone?.invoke(this)
        },
        onGo = keyboardActions.onGo,
        onNext = keyboardActions.onNext,
        onPrevious = keyboardActions.onPrevious,
        onSearch = keyboardActions.onSearch,
        onSend = keyboardActions.onSend
    )

    val currentTextFieldValue = if (isFocused) {
        textFieldValue
    } else {
        TextFieldValue(
            text = resolvedDisplayValue,
            selection = TextRange(resolvedDisplayValue.length)
        )
    }

    StyledOutlinedTextField(
        value = currentTextFieldValue,
        onValueChange = { newTfv ->
            if (isFocused) {
                textFieldValue = newTfv
            } else {
                onValueChange(newTfv.text)
            }
        },
        modifier = modifier.onFocusChanged {
            if (it.isFocused && !isFocused) {
                val selection = if (moveCursorToEndOnFocus) {
                    TextRange(value.length)
                } else {
                    TextRange.Zero
                }
                textFieldValue = TextFieldValue(text = value, selection = selection)
            } else if (!it.isFocused && isFocused) {
                commitResult()
            }
            isFocused = it.isFocused
        },
        label = label,
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
        keyboardType = keyboardType,
        imeAction = imeAction,
        capitalization = capitalization,
        keyboardActions = wrappedKeyboardActions,
        onClick = onClick,
        focusRequester = focusRequester,
        moveCursorToEndOnFocus = moveCursorToEndOnFocus,
        focusable = focusable,
        shape = shape,
        colors = colors
    )
}

private fun resolveDisplayValue(
    value: String,
    displayValue: String?,
    locale: Locale,
    maxDecimalPlaces: Int,
    minDecimalPlaces: Int
): String {
    if (displayValue != null) return displayValue
    return value.toBigDecimalOrNull()?.stripTrailingZeros()?.formatForDisplay(
        locale = locale,
        maxDecimalPlaces = maxDecimalPlaces,
        minDecimalPlaces = minDecimalPlaces
    ) ?: value
}
