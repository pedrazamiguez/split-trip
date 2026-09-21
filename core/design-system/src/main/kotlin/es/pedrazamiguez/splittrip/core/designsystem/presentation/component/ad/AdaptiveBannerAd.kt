package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.ad

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    modifier: Modifier = Modifier
) {
    if (!isAdEnabled || adUnitId.isBlank()) {
        Spacer(modifier = modifier.height(0.dp))
        return
    }

    var isAdLoaded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    AnimatedVisibility(
        visible = isAdLoaded,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        AndroidView(
            modifier = modifier.fillMaxWidth(),
            factory = { ctx ->
                AdView(ctx).apply {
                    setAdUnitId(adUnitId)
                    setAdSize(calculateAdaptiveBannerSize(ctx))
                    adListener = object : AdListener() {
                        override fun onAdLoaded() {
                            isAdLoaded = true
                            Timber.d("AdaptiveBannerAd: Ad loaded successfully for adUnitId=%s", adUnitId)
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            isAdLoaded = false
                            Timber.w(
                                "AdaptiveBannerAd: Failed to load ad (%s: %s) for adUnitId=%s",
                                error.code,
                                error.message,
                                adUnitId
                            )
                        }
                    }
                    loadAd(AdRequest.Builder().build())
                }
            }
        )
    }
}

private fun calculateAdaptiveBannerSize(context: Context): AdSize {
    val displayMetrics = context.resources.displayMetrics
    val widthPixels = displayMetrics.widthPixels.toFloat()
    val density = displayMetrics.density
    val adWidth = (widthPixels / density).toInt()
    return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth)
}
