package es.pedrazamiguez.splittrip.features.balance.presentation.component.export

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import es.pedrazamiguez.splittrip.features.balance.presentation.util.CreateTripReportDocumentContract
import es.pedrazamiguez.splittrip.features.balance.presentation.util.CreateTripReportDocumentRequest
import es.pedrazamiguez.splittrip.features.balance.presentation.util.ReportSaveLauncher

@Composable
internal fun TripReportSaveEffect(
    pendingSaveRequest: CreateTripReportDocumentRequest?,
    onRequestConsumed: () -> Unit,
    onDestinationSelected: (String) -> Unit,
    onCancelled: () -> Unit,
    onLaunchFailed: () -> Unit
) {
    val currentOnRequestConsumed by rememberUpdatedState(onRequestConsumed)
    val currentOnDestinationSelected by rememberUpdatedState(onDestinationSelected)
    val currentOnCancelled by rememberUpdatedState(onCancelled)
    val currentOnLaunchFailed by rememberUpdatedState(onLaunchFailed)

    val launcher = rememberLauncherForActivityResult(
        contract = CreateTripReportDocumentContract()
    ) { uri ->
        if (uri != null) {
            currentOnDestinationSelected(uri.toString())
        } else {
            currentOnCancelled()
        }
    }

    LaunchedEffect(pendingSaveRequest) {
        val request = pendingSaveRequest ?: return@LaunchedEffect
        currentOnRequestConsumed()
        if (!ReportSaveLauncher.launchCreateDocument(launcher, request)) {
            currentOnLaunchFailed()
        }
    }
}
