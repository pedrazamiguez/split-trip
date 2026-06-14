package es.pedrazamiguez.splittrip.features.group.presentation.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import es.pedrazamiguez.splittrip.features.group.R

@Composable
internal fun GroupImagePreview(
    localGroupImagePath: String?,
    groupName: String,
    modifier: Modifier = Modifier
) {
    val coverHeight = 200.dp
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(coverHeight)
            .clip(MaterialTheme.shapes.large)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = MaterialTheme.shapes.large
            )
    ) {
        if (localGroupImagePath != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(localGroupImagePath)
                    .crossfade(true)
                    .build(),
                contentDescription = stringResource(
                    R.string.group_cover_image_description,
                    groupName
                ),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(coverHeight)
            )
        } else {
            GroupImagePlaceholder(coverHeight = coverHeight)
        }
    }
}
