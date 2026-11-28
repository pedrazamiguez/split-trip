package es.pedrazamiguez.splittrip

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import es.pedrazamiguez.splittrip.core.designsystem.ad.AdConsentManager
import es.pedrazamiguez.splittrip.core.designsystem.foundation.SplitTripTheme
import es.pedrazamiguez.splittrip.core.designsystem.navigation.NavigationUtils
import es.pedrazamiguez.splittrip.core.designsystem.navigation.NavigationUtils.DeepLinkDispatchAction
import es.pedrazamiguez.splittrip.core.logging.LogTag
import es.pedrazamiguez.splittrip.domain.enums.AppTheme
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetAppThemeUseCase
import es.pedrazamiguez.splittrip.features.main.navigation.DeepLinkHolder
import es.pedrazamiguez.splittrip.navigation.AppNavHost
import org.koin.android.ext.android.inject
import org.koin.compose.getKoin
import timber.log.Timber

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

            val darkTheme = resolveDarkTheme(themeState, isSystemInDarkTheme())

            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        lightScrim = Color.TRANSPARENT,
                        darkScrim = Color.TRANSPARENT
                    ) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(
                        lightScrim = DefaultLightNavScrim,
                        darkScrim = DefaultDarkNavScrim
                    ) { darkTheme }
                )
                onDispose {}
            }

            SplitTripTheme(darkTheme = darkTheme) {
                val navController = rememberNavController()
                navHostController = navController
                AppNavHost(navController = navController)
            }
        }
    }

    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val deepLinkUri = extractDeepLinkUri(intent)
        val currentRoute = navHostController?.currentDestination?.route
        when (NavigationUtils.resolveNewIntentAction(deepLinkUri != null, currentRoute)) {
            DeepLinkDispatchAction.IGNORE -> {
                Timber.tag(LogTag.NAVIGATION).i(
                    "onNewIntent ignored (no deep-link URI, currentRoute=%s)",
                    currentRoute
                )
            }
            DeepLinkDispatchAction.BUFFER -> {
                if (deepLinkUri != null) {
                    intent.data = deepLinkUri
                    intent.action = Intent.ACTION_VIEW
                    deepLinkHolder.pendingDeepLink = deepLinkUri
                    Timber.tag(LogTag.NAVIGATION).i(
                        "onNewIntent buffered deep link (scheme=%s, currentRoute=%s)",
                        deepLinkUri.scheme,
                        currentRoute
                    )
                }
            }
            DeepLinkDispatchAction.HANDLE_IMMEDIATELY -> {
                if (deepLinkUri != null) {
                    intent.data = deepLinkUri
                    intent.action = Intent.ACTION_VIEW
                    navHostController?.handleDeepLink(intent)
                }
            }
        }
    }

    private fun extractDeepLinkUri(intent: Intent?): Uri? {
        if (intent == null) return null
        return intent.data ?: intent.extras?.getString(EXTRA_DEEP_LINK)?.takeIf { it.isNotBlank() }?.let {
            Uri.parse(it)
        }
    }

    private companion object {
        const val EXTRA_DEEP_LINK = "deepLink"
        const val NAV_SCRIM_LIGHT_ALPHA = 0xE6
        const val NAV_SCRIM_DARK_ALPHA = 0x80
        const val NAV_SCRIM_DARK_CHANNEL = 0x1B
        const val COLOR_CHANNEL_MAX = 0xFF
        val DefaultLightNavScrim = Color.argb(
            NAV_SCRIM_LIGHT_ALPHA,
            COLOR_CHANNEL_MAX,
            COLOR_CHANNEL_MAX,
            COLOR_CHANNEL_MAX
        )
        val DefaultDarkNavScrim = Color.argb(
            NAV_SCRIM_DARK_ALPHA,
            NAV_SCRIM_DARK_CHANNEL,
            NAV_SCRIM_DARK_CHANNEL,
            NAV_SCRIM_DARK_CHANNEL
        )
    }
}
