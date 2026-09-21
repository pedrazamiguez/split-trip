package es.pedrazamiguez.splittrip

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import es.pedrazamiguez.splittrip.core.designsystem.ad.AdConsentManager
import es.pedrazamiguez.splittrip.core.designsystem.foundation.SplitTripTheme
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.domain.enums.AppTheme
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetAppThemeUseCase
import es.pedrazamiguez.splittrip.features.main.navigation.DeepLinkHolder
import es.pedrazamiguez.splittrip.navigation.AppNavHost
import org.koin.android.ext.android.inject
import org.koin.compose.getKoin

class MainActivity : AppCompatActivity() {

    private var navHostController: NavHostController? = null
    private val deepLinkHolder: DeepLinkHolder by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AdConsentManager.gatherConsentAndInitialize(this)

        // Save pending deep link before NavHost consumes the intent.
        // On cold start, if the user is not authenticated, the NavHost graph starts
        // with Routes.LOGIN — the deep link intent targets Routes.MAIN which is in
        // the graph but behind an auth/onboarding gate, so the deep link is silently
        // dropped. We preserve it here for replay after the gate completes.
        // When the user IS authenticated, NavHost natively processes the intent's
        // deep link on first composition (startDestination = Routes.MAIN).
        val deepLinkUri = extractDeepLinkUri(intent)
        if (deepLinkUri != null) {
            deepLinkHolder.pendingDeepLink = deepLinkUri
            intent?.data = deepLinkUri
            intent?.action = Intent.ACTION_VIEW
        }

        setContent {
            val koin = getKoin()
            val getAppThemeUseCase = remember(koin) { koin.get<GetAppThemeUseCase>() }
            val themeState by getAppThemeUseCase().collectAsStateWithLifecycle(initialValue = AppTheme.SYSTEM.code)

            val darkTheme = when (AppTheme.fromCode(themeState)) {
                AppTheme.SYSTEM -> isSystemInDarkTheme()
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
            }

            SplitTripTheme(darkTheme = darkTheme) {
                val navController = rememberNavController()
                navHostController = navController
                AppNavHost(navController = navController)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val deepLinkUri = extractDeepLinkUri(intent)
        if (deepLinkUri != null) {
            intent.data = deepLinkUri
            intent.action = Intent.ACTION_VIEW
            // Deep link intent — check if we're past the auth gate before forwarding.
            // If the user is on login/onboarding, forwarding the deep link would
            // navigate directly to Routes.MAIN, bypassing authentication.
            val currentRoute = navHostController?.currentDestination?.route
            if (currentRoute == Routes.LOGIN || currentRoute == Routes.ONBOARDING) {
                // Buffer for later replay after auth/onboarding completes
                deepLinkHolder.pendingDeepLink = deepLinkUri
            } else {
                navHostController?.handleDeepLink(intent)
            }
        } else {
            navHostController?.handleDeepLink(intent)
        }
    }

    private fun extractDeepLinkUri(intent: Intent?): Uri? {
        if (intent == null) return null
        return intent.data ?: intent.extras?.getString(EXTRA_DEEP_LINK)?.takeIf { it.isNotBlank() }?.let {
            Uri.parse(it)
        }
    }
}

private const val EXTRA_DEEP_LINK = "deepLink"
