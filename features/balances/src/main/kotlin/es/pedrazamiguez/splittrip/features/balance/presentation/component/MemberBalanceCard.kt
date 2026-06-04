package es.pedrazamiguez.splittrip.features.balance.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard

@Composable
internal fun MemberBalanceCard(
    expandedStateDesc: String,
    toggleContentDesc: String,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    FlatCard(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                stateDescription = expandedStateDesc
                contentDescription = toggleContentDesc
            }
            .clickable(onClick = onToggle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.spacing.Default, vertical = MaterialTheme.spacing.Medium),
            content = { content() }
        )
    }
}
