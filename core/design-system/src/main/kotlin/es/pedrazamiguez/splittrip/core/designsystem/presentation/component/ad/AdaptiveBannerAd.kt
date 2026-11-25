package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.ad

import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import timber.log.Timber

@SuppressLint("MissingPermission")
@Composable
fun AdaptiveBannerAd(
    adUnitId: String,
    isAdEnabled: Boolean,
    modifier: Modifier = Modifier,
    onAdLoaded: () -> Unit = {},
    onAdImpression: () -> Unit = {},
    onAdFailedToLoad: (errorCode: Int) -> Unit = {}
) {
    if (!isAdEnabled || adUnitId.isBlank()) {
        Spacer(modifier = modifier.height(0.dp))
        return
    }

    var isAdLoaded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val safeHorizontalInsets = WindowInsets.navigationBars
        .union(WindowInsets.displayCutout)
        .only(WindowInsetsSides.Horizontal)
    val adWidthDp = rememberAdaptiveBannerWidthDp(context, safeHorizontalInsets)
    val currentOnAdLoaded by rememberUpdatedState(onAdLoaded)
    val currentOnAdImpression by rememberUpdatedState(onAdImpression)
    val currentOnAdFailedToLoad by rememberUpdatedState(onAdFailedToLoad)

    val adView = remember(adUnitId, adWidthDp) {
        AdView(context).apply {
            setAdUnitId(adUnitId)
            setAdSize(calculateAdaptiveBannerSize(context, adWidthDp))
            adListener = createAdaptiveBannerAdListener(
                onLoaded = {
                    isAdLoaded = true
                    Timber.d("AdaptiveBannerAd: Ad loaded successfully for adUnitId=%s", adUnitId)
                    currentOnAdLoaded()
                },
                onImpression = {
                    currentOnAdImpression()
                },
                onFailedToLoad = { errorCode ->
                    isAdLoaded = false
                    currentOnAdFailedToLoad(errorCode)
                }
            )
            loadAd(AdRequest.Builder().build())
        }
    }

    DisposableEffect(adView) {
        onDispose {
            adView.destroy()
        }
    }

    AnimatedVisibility(
        visible = isAdLoaded,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        AndroidView(
            modifier = modifier
                .fillMaxWidth()
                .windowInsetsPadding(safeHorizontalInsets),
            factory = {
                (adView.parent as? ViewGroup)?.removeView(adView)
                adView
            }
        )
    }
}

@Composable
private fun rememberAdaptiveBannerWidthDp(
    context: Context,
    safeHorizontalInsets: WindowInsets
): Int {
    // Recompute when configuration (orientation/screen size) or insets change.
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val horizontalInsetPx = safeHorizontalInsets.getLeft(density, layoutDirection) +
        safeHorizontalInsets.getRight(density, layoutDirection)
    return remember(configuration, horizontalInsetPx, density.density) {
        calculateAdaptiveBannerWidthDp(
            containerWidthPx = context.resources.displayMetrics.widthPixels,
            horizontalInsetPx = horizontalInsetPx,
            density = density.density
        )
    }
}

internal fun calculateAdaptiveBannerWidthDp(
    containerWidthPx: Int,
    horizontalInsetPx: Int,
    density: Float
): Int {
    val safeWidthPx = (containerWidthPx - horizontalInsetPx).coerceAtLeast(0)
    return if (density > 0f) (safeWidthPx / density).toInt().coerceAtLeast(1) else 1
}

private fun calculateAdaptiveBannerSize(context: Context, adWidthDp: Int): AdSize =
    AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidthDp)

internal fun createAdaptiveBannerAdListener(
    onLoaded: () -> Unit,
    onImpression: () -> Unit,
    onFailedToLoad: (Int) -> Unit
): AdListener = object : AdListener() {
    override fun onAdLoaded() {
        onLoaded()
    }

    override fun onAdImpression() {
        onImpression()
    }

    override fun onAdFailedToLoad(error: LoadAdError) {
        Timber.w(
            "AdaptiveBannerAd: Failed to load ad (%s: %s)",
            error.code,
            error.message
        )
        onFailedToLoad(error.code)
    }
}
