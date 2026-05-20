package es.pedrazamiguez.splittrip.features.settings.presentation.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.GradientButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.SectionCard
import es.pedrazamiguez.splittrip.features.settings.R

@Composable
fun OcrOperationsCard(
    isLoading: Boolean,
    onRunOcrClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SectionCard(
        title = stringResource(R.string.developer_services_ocr_operations),
        modifier = modifier
    ) {
        GradientButton(
            text = stringResource(R.string.developer_services_run_ocr),
            onClick = onRunOcrClick,
            isLoading = isLoading,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
