package es.pedrazamiguez.splittrip.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun SubscriptionReconciliationEffect(
    isUserLoggedIn: Boolean?,
    onReconcile: suspend () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val currentOnReconcile by rememberUpdatedState(onReconcile)

    DisposableEffect(lifecycleOwner, isUserLoggedIn) {
        if (isUserLoggedIn != true) {
            return@DisposableEffect onDispose {}
        }
        var reconciliationJob: Job? = null
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && reconciliationJob?.isActive != true) {
                reconciliationJob = coroutineScope.launch {
                    currentOnReconcile()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            reconciliationJob?.cancel()
        }
    }
}
