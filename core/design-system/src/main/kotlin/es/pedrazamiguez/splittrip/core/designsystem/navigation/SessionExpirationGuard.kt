package es.pedrazamiguez.splittrip.core.designsystem.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import es.pedrazamiguez.splittrip.core.designsystem.R
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.TopPillController
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * Observes mid-session authentication drops (`true -> false`) that were not initiated
 * by an explicit sign-out, executes session teardown, shows a localized top pill,
 * and redirects to [Routes.LOGIN] once the [NavHost] is ready.
 */
@Composable
fun SessionExpirationGuard(
    isUserLoggedIn: Boolean?,
    isExplicitSignOut: () -> Boolean,
    isNavHostReady: Boolean,
    stableStartDestination: String?,
    navController: NavHostController,
    pillController: TopPillController,
    onHandleSessionExpired: suspend () -> Unit
) {
    val sessionExpiredMessage = stringResource(R.string.session_expired_message)
    var previousLoggedIn by remember { mutableStateOf<Boolean?>(null) }
    var pendingSessionExpiredRedirect by remember { mutableStateOf(false) }

    LaunchedEffect(isUserLoggedIn) {
        val wasLoggedIn = previousLoggedIn
        previousLoggedIn = isUserLoggedIn

        if (NavigationUtils.shouldHandleSessionExpired(
                previousLoggedIn = wasLoggedIn,
                currentLoggedIn = isUserLoggedIn,
                isExplicitSignOut = isExplicitSignOut()
            )
        ) {
            withContext(NonCancellable) {
                onHandleSessionExpired()
            }
            pillController.showPill(sessionExpiredMessage)
            if (stableStartDestination != Routes.LOGIN) {
                pendingSessionExpiredRedirect = true
            }
        }
    }

    LaunchedEffect(pendingSessionExpiredRedirect, isNavHostReady) {
        if (pendingSessionExpiredRedirect && isNavHostReady) {
            val currentRoute = navController.currentDestination?.route
            if (NavigationUtils.shouldNavigateOnSessionExpired(currentRoute)) {
                navController.navigate(Routes.LOGIN) {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            }
            pendingSessionExpiredRedirect = false
        }
    }
}
