package es.pedrazamiguez.splittrip.features.balance.presentation.preview

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewNarrowThemes
import es.pedrazamiguez.splittrip.features.balance.presentation.component.MemberBalanceItem

private const val NARROW_LARGE_FONT_SCALE = 1.3f

@PreviewComplete
@Composable
private fun MemberBalanceItemPositivePreview() {
    MemberBalanceItemPreviewHelper(domainBalance = PREVIEW_MEMBER_BALANCE_POSITIVE) {
        MemberBalanceItem(
            memberBalance = it,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@PreviewComplete
@Composable
private fun MemberBalanceItemNegativePreview() {
    MemberBalanceItemPreviewHelper(domainBalance = PREVIEW_MEMBER_BALANCE_NEGATIVE) {
        MemberBalanceItem(
            memberBalance = it,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@PreviewComplete
@Composable
private fun MemberBalanceItemNegativeCashPreview() {
    MemberBalanceItemPreviewHelper(domainBalance = PREVIEW_MEMBER_BALANCE_NEGATIVE_CASH) {
        MemberBalanceItem(
            memberBalance = it,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@PreviewNarrowThemes
@Composable
private fun MemberBalanceItemNarrowPreview() {
    MemberBalanceItemPreviewHelper(
        domainBalance = PREVIEW_MEMBER_BALANCE_POSITIVE,
        currency = "RON"
    ) {
        MemberBalanceItem(
            memberBalance = it,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@PreviewNarrowThemes
@Composable
private fun MemberBalanceItemNarrowLargeFontPreview() {
    val currentDensity = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = currentDensity.density,
            fontScale = NARROW_LARGE_FONT_SCALE
        )
    ) {
        MemberBalanceItemPreviewHelper(
            domainBalance = PREVIEW_MEMBER_BALANCE_NEGATIVE,
            currency = "RON"
        ) {
            MemberBalanceItem(
                memberBalance = it,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
