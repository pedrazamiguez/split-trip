package es.pedrazamiguez.splittrip

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import es.pedrazamiguez.splittrip.core.designsystem.foundation.SplitTripTheme
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.features.main.navigation.DeepLinkHolder
import es.pedrazamiguez.splittrip.navigation.AppNavHost
import org.koin.android.ext.android.inject

class MainActivity : AppCompatActivity() {

    private var navHostController: NavHostController? = null
    private val deepLinkHolder: DeepLinkHolder by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Save pending deep link before NavHost consumes the intent.
        // On cold start, if the user is not authenticated, the NavHost graph starts
        // with Routes.LOGIN — the deep link intent targets Routes.MAIN which is in
        // the graph but behind an auth/onboarding gate, so the deep link is silently
        // dropped. We preserve it here for replay after the gate completes.
        // When the user IS authenticated, NavHost natively processes the intent's
        // deep link on first composition (startDestination = Routes.MAIN).
        if (intent?.action == Intent.ACTION_VIEW && intent?.data != null) {
            deepLinkHolder.pendingDeepLink = intent.data
        }

        enableEdgeToEdge()
        setContent {
            SplitTripTheme {
                val navController = rememberNavController()
                navHostController = navController
                AppNavHost(navController = navController)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_VIEW && intent.data != null) {
            // Deep link intent — check if we're past the auth gate before forwarding.
            // If the user is on login/onboarding, forwarding the deep link would
            // navigate directly to Routes.MAIN, bypassing authentication.
            val currentRoute = navHostController?.currentDestination?.route
            if (currentRoute == Routes.LOGIN || currentRoute == Routes.ONBOARDING) {
                // Buffer for later replay after auth/onboarding completes
                deepLinkHolder.pendingDeepLink = intent.data
            } else {
                navHostController?.handleDeepLink(intent)
            }
        } else {
            navHostController?.handleDeepLink(intent)
        }
    }
}
