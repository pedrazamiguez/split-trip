package es.pedrazamiguez.splittrip.features.profile.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.common.util.QrCodePayloadParser
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Qrcode
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.QrCodeImage
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.BodyText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SectionHeadingText
import es.pedrazamiguez.splittrip.features.profile.R

private val QR_BADGE_SIZE = 40.dp
private val QR_BADGE_ICON_SIZE = 20.dp
private val QR_IMAGE_SIZE = 184.dp
private const val QR_BADGE_BACKGROUND_ALPHA = 0.4f

@Composable
internal fun ProfileQrShareCard(
    email: String,
    userId: String,
    modifier: Modifier = Modifier
) {
    FlatCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.Large),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(QR_BADGE_SIZE)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = QR_BADGE_BACKGROUND_ALPHA)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = TablerIcons.Outline.Qrcode,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(QR_BADGE_ICON_SIZE)
                )
            }
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.Medium))
            SectionHeadingText(text = stringResource(R.string.profile_qr_title))
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.ExtraSmall))
            BodyText(
                text = stringResource(R.string.profile_qr_subtitle),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.Large))
            Box(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(MaterialTheme.spacing.Medium),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.medium)
                        .background(Color.White)
                        .padding(MaterialTheme.spacing.Small),
                    contentAlignment = Alignment.Center
                ) {
                    QrCodeImage(
                        content = QrCodePayloadParser.createSharePayload(email, userId),
                        modifier = Modifier
                            .size(QR_IMAGE_SIZE)
                            .clip(MaterialTheme.shapes.small)
                    )
                }
            }
        }
    }
}
