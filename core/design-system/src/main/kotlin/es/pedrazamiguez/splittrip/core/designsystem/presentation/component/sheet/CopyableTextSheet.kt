package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.R
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Check
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Copy
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.BodyText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SheetTitleText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val COPY_DISMISS_DELAY_MS = 400L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CopyableTextSheet(
    icon: ImageVector,
    title: String = "",
    copyableText: String? = null,
    notAvailableText: String = "",
    onDismiss: () -> Unit = { }
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val view = LocalView.current

    var isCopied by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.ExtraLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Default)
        ) {
            CopyableIconHeader(icon = icon, isCopied = isCopied)
            SheetTitleText(text = title)
            CopyableTextContent(copyableText = copyableText, notAvailableText = notAvailableText)
            Spacer(Modifier.size(MaterialTheme.spacing.Small))
            CopyActionButton(
                copyableText = copyableText,
                isCopied = isCopied,
                onCopy = {
                    @Suppress("DEPRECATION")
                    view.performHapticFeedback(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            HapticFeedbackConstants.CONFIRM
                        } else {
                            HapticFeedbackConstants.VIRTUAL_KEY
                        }
                    )
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText(title, copyableText))
                    isCopied = true
                    coroutineScope.launch {
                        delay(COPY_DISMISS_DELAY_MS)
                        sheetState.hide()
                    }.invokeOnCompletion { onDismiss() }
                }
            )
        }
    }
}

@Composable
private fun CopyableIconHeader(icon: ImageVector, isCopied: Boolean) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(
                if (isCopied) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = isCopied,
            transitionSpec = {
                scaleIn(spring(stiffness = Spring.StiffnessHigh)) togetherWith
                    scaleOut(spring(stiffness = Spring.StiffnessHigh))
            },
            label = "iconAnimation"
        ) { copied ->
            Icon(
                imageVector = if (copied) TablerIcons.Outline.Check else icon,
                contentDescription = null,
                tint = if (copied) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun CopyableTextContent(copyableText: String?, notAvailableText: String) {
    if (copyableText != null) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            tonalElevation = 0.dp
        ) {
            Text(
                text = copyableText,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(MaterialTheme.spacing.Default)
            )
        }
    } else {
        BodyText(
            text = notAvailableText,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CopyActionButton(
    copyableText: String?,
    isCopied: Boolean,
    onCopy: () -> Unit
) {
    FilledTonalButton(
        enabled = copyableText != null && !isCopied,
        onClick = onCopy,
        modifier = Modifier.fillMaxWidth()
    ) {
        AnimatedContent(
            targetState = isCopied,
            transitionSpec = {
                scaleIn(spring(stiffness = Spring.StiffnessHigh)) togetherWith
                    scaleOut(spring(stiffness = Spring.StiffnessHigh))
            },
            label = "buttonIconAnimation"
        ) { copied ->
            Icon(
                imageVector = if (copied) TablerIcons.Outline.Check else TablerIcons.Outline.Copy,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(MaterialTheme.spacing.Small))
        Text(
            text = if (isCopied) stringResource(R.string.action_copied) else stringResource(R.string.action_copy),
            style = MaterialTheme.typography.labelLarge
        )
    }
}
