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
import androidx.compose.runtime.rememberUpdatedState
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
import es.pedrazamiguez.splittrip.domain.converter.CurrencyConverter
import es.pedrazamiguez.splittrip.domain.service.calculator.ExpressionCalculatorService
import es.pedrazamiguez.splittrip.domain.service.calculator.ExpressionResult
import java.math.BigDecimal
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
    requirePositive: Boolean = false,
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
    var evaluationResult by remember {
        mutableStateOf<ExpressionResult?>(evaluator.evaluate(value))
    }
    var lastEmittedValue by remember { mutableStateOf(value) }
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentMaxDecimalPlaces by rememberUpdatedState(maxDecimalPlaces)
    val keyboardState = LocalArithmeticKeyboardState.current
    val locale = rememberLocale()

    LaunchedEffect(value, isFocused) {
        if (!isFocused) {
            textFieldValue = TextFieldValue(text = value, selection = TextRange(value.length))
            evaluationResult = evaluator.evaluate(value)
            lastEmittedValue = value
        }
    }

    val updateFocusedInput: (TextFieldValue) -> Unit = { newTfv ->
        val textChanged = newTfv.text != textFieldValue.text
        textFieldValue = newTfv
        if (textChanged) {
            val result = evaluator.evaluate(newTfv.text)
            evaluationResult = result
            val syncedValue = resolveFocusedSyncValue(
                evaluationResult = result,
                maxDecimalPlaces = currentMaxDecimalPlaces
            )
            if (syncedValue != lastEmittedValue) {
                lastEmittedValue = syncedValue
                currentOnValueChange(syncedValue)
            }
        }
    }

    val commitResult = {
        val res = evaluator.evaluate(textFieldValue.text)
        evaluationResult = res
        if (res is ExpressionResult.Success) {
            val rawValue = formatEvaluatedValue(res.value, currentMaxDecimalPlaces)
            lastEmittedValue = rawValue
            currentOnValueChange(rawValue)
            textFieldValue = TextFieldValue(text = rawValue, selection = TextRange(rawValue.length))
        } else {
            lastEmittedValue = textFieldValue.text
            currentOnValueChange(textFieldValue.text)
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
                    updateFocusedInput(TextFieldValue(text = "", selection = TextRange.Zero))
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
                    updateFocusedInput(
                        TextFieldValue(
                            text = newText,
                            selection = TextRange(newCursorPosition)
                        )
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

    val isDivisionByZero = isFocused && evaluationResult is ExpressionResult.Failure.DivisionByZero
    val effectiveIsError = if (requirePositive) {
        if (isFocused) {
            val res = evaluationResult
            isError || isDivisionByZero || (res is ExpressionResult.Success && res.value <= BigDecimal.ZERO)
        } else {
            isError || (value.isNotBlank() && !CurrencyConverter.isPositiveAmount(value))
        }
    } else {
        isError || isDivisionByZero
    }

    StyledOutlinedTextField(
        value = currentTextFieldValue,
        onValueChange = { newTfv ->
            if (isFocused) {
                updateFocusedInput(newTfv)
            } else if (newTfv.text != textFieldValue.text) {
                val result = evaluator.evaluate(newTfv.text)
                evaluationResult = result
                val syncedValue = resolveFocusedSyncValue(result, currentMaxDecimalPlaces)
                if (syncedValue != lastEmittedValue) {
                    lastEmittedValue = syncedValue
                    currentOnValueChange(syncedValue)
                }
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
                evaluationResult = evaluator.evaluate(value)
                lastEmittedValue = value
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
        isError = effectiveIsError,
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

internal fun formatEvaluatedValue(
    value: BigDecimal,
    maxDecimalPlaces: Int
): String = value
    .setScale(maxDecimalPlaces, RoundingMode.HALF_UP)
    .stripTrailingZeros()
    .toPlainString()

internal fun resolveFocusedSyncValue(
    evaluationResult: ExpressionResult,
    maxDecimalPlaces: Int
): String = when (evaluationResult) {
    is ExpressionResult.Success -> formatEvaluatedValue(evaluationResult.value, maxDecimalPlaces)
    is ExpressionResult.Failure -> ""
}
