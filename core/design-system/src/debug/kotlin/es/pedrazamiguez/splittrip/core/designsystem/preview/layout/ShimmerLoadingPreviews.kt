package es.pedrazamiguez.splittrip.core.designsystem.preview.layout

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.DashboardShimmer
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.ShimmerDashboardCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.ShimmerItemCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.ShimmerLoadingList
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemes

@PreviewThemes
@Composable
private fun ShimmerLoadingListPreview() {
    PreviewThemeWrapper {
        ShimmerLoadingList()
    }
}

@PreviewThemes
@Composable
private fun ShimmerItemCardPreview() {
    PreviewThemeWrapper {
        ShimmerItemCard(
            modifier = Modifier.padding(16.dp)
        )
    }
}

@PreviewThemes
@Composable
private fun ShimmerDashboardCardPreview() {
    PreviewThemeWrapper {
        ShimmerDashboardCard(
            modifier = Modifier.padding(16.dp)
        )
    }
}

@PreviewThemes
@Composable
private fun DashboardShimmerPreview() {
    PreviewThemeWrapper {
        DashboardShimmer()
    }
}
