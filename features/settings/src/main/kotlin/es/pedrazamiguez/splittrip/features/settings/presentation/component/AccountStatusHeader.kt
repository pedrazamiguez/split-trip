package es.pedrazamiguez.splittrip.features.settings.presentation.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Calendar
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTag
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTagStyle
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.BodyText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CaptionText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CardTitleText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SectionHeadingText
import es.pedrazamiguez.splittrip.features.settings.R

@Composable
internal fun AccountStatusHeader(
    email: String,
    joinDateText: String,
    isAnonymous: Boolean = false,
    modifier: Modifier = Modifier
) {
    val statusText = stringResource(
        if (isAnonymous) R.string.account_security_provider_guest else R.string.account_security_status_verified
    )
    val statusStyle = if (isAnonymous) PassportTagStyle.WARNING else PassportTagStyle.SUCCESS

    FlatCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.Large)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                AccountStatusHeaderBadge(isAnonymous = isAnonymous)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = MaterialTheme.spacing.Medium)
                ) {
                    if (isAnonymous) {
                        SectionHeadingText(text = stringResource(R.string.account_status_guest_label))
                    } else {
                        CaptionText(text = stringResource(R.string.account_status_email_label))
                        CardTitleText(text = email)
                    }
                }
                PassportTag(text = statusText, style = statusStyle)
            }

            if (isAnonymous) {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.Medium))
                BodyText(
                    text = stringResource(R.string.account_status_guest_warning),
                    color = MaterialTheme.colorScheme.error
                )
            } else if (joinDateText.isNotBlank()) {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.Medium))
                PassportTag(
                    text = stringResource(R.string.account_status_member_since, joinDateText),
                    icon = TablerIcons.Outline.Calendar,
                    style = PassportTagStyle.NEUTRAL
                )
            }
        }
    }
}
