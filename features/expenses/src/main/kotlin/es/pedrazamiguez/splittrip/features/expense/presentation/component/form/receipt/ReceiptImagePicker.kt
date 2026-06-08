package es.pedrazamiguez.splittrip.features.expense.presentation.component.form.receipt

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Camera
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Receipt
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.X
import es.pedrazamiguez.splittrip.core.designsystem.navigation.SharedElementKeys
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.BodyText
import es.pedrazamiguez.splittrip.core.designsystem.transition.receiptSharedElementModifier
import es.pedrazamiguez.splittrip.features.expense.R

private const val MIME_PDF = "application/pdf"

@Suppress("LongMethod", "CyclomaticComplexMethod", "CognitiveComplexMethod")
@Composable
fun ReceiptImagePicker(
    receiptUri: String?,
    mimeType: String?,
    onPickerRequested: () -> Unit,
    onRemoveImage: () -> Unit,
    modifier: Modifier = Modifier,
    onViewImage: (() -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
    ) {
        if (receiptUri != null) {
            if (mimeType == MIME_PDF) {
                // PDF Preview inlined
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                ) {
                    Column(
                        modifier = Modifier.matchParentSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = TablerIcons.Outline.Receipt,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        BodyText(
                            text = stringResource(R.string.add_expense_receipt_pdf_attached),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = MaterialTheme.spacing.ExtraSmall)
                        )
                    }
                    IconButton(
                        onClick = onRemoveImage,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(MaterialTheme.spacing.ExtraSmall)
                    ) {
                        Icon(
                            imageVector = TablerIcons.Outline.X,
                            contentDescription = stringResource(R.string.add_expense_receipt_remove),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            } else {
                // Image Preview inlined
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(Uri.parse(receiptUri))
                            .crossfade(true)
                            .build(),
                        contentDescription = stringResource(R.string.add_expense_receipt_image_description),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .matchParentSize()
                            .then(receiptSharedElementModifier(SharedElementKeys.RECEIPT_VIEWER_SHARED_ELEMENT_KEY))
                            .clip(MaterialTheme.shapes.large)
                            .clickable(enabled = onViewImage != null) { onViewImage?.invoke() }
                    )
                    IconButton(
                        onClick = onRemoveImage,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(MaterialTheme.spacing.ExtraSmall)
                    ) {
                        Icon(
                            imageVector = TablerIcons.Outline.X,
                            contentDescription = stringResource(R.string.add_expense_receipt_remove),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        } else {
            // Picker Placeholder inlined
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .clickable(onClick = onPickerRequested)
                    .padding(vertical = MaterialTheme.spacing.ExtraLarge),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = TablerIcons.Outline.Camera,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                BodyText(
                    text = stringResource(R.string.add_expense_receipt_attach),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = MaterialTheme.spacing.Small)
                )
            }
        }
    }
}
