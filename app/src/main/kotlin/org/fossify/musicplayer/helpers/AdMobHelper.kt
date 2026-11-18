package org.fossify.musicplayer.helpers

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import org.fossify.musicplayer.BuildConfig

object AdMobHelper {
    private const val TAG = "PulseBeatAdMob"
    private const val INTERSTITIAL_COOLDOWN_MS = 90_000L
    private const val TRANSITIONS_BEFORE_SHOW = 2

    @Volatile
    private var isInitialized = false
    private var interstitialAd: InterstitialAd? = null
    private var isLoadingInterstitial = false
    private var lastInterstitialShownAt = 0L
    private var transitionCounter = 0

    fun initialize(context: Context) {
        if (isInitialized) return
        try {
            MobileAds.initialize(context.applicationContext) {
                isInitialized = true
                preloadInterstitial(context.applicationContext)
            }
        } catch (e: Exception) {
            Log.w(TAG, "AdMob init skipped: ${e.message}")
        }
    }

    fun loadAdaptiveBanner(activity: Activity, container: FrameLayout): AdView? {
        return try {
            val adUnitId = BuildConfig.ADMOB_BANNER_ID
            if (adUnitId.isBlank()) {
                container.visibility = View.GONE
                return null
            }

            val adView = AdView(activity).apply {
                this.adUnitId = adUnitId
                setAdSize(getAdaptiveBannerSize(activity, container))
                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        container.visibility = View.VISIBLE
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.d(TAG, "Banner failed to load: ${error.message}")
                    }
                }
            }

            container.removeAllViews()
            container.addView(
                adView,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
            container.visibility = View.VISIBLE
            adView.loadAd(AdRequest.Builder().build())
            adView
        } catch (e: Exception) {
            Log.w(TAG, "Banner load skipped: ${e.message}")
            container.visibility = View.GONE
            null
        }
    }

    fun preloadInterstitial(context: Context) {
        if (isLoadingInterstitial || interstitialAd != null) return
        val adUnitId = BuildConfig.ADMOB_INTERSTITIAL_ID
        if (adUnitId.isBlank()) return

        isLoadingInterstitial = true
        try {
            InterstitialAd.load(
                context.applicationContext,
                adUnitId,
                AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        interstitialAd = ad
                        isLoadingInterstitial = false
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        interstitialAd = null
                        isLoadingInterstitial = false
                        Log.d(TAG, "Interstitial failed to load: ${error.message}")
                    }
                }
            )
        } catch (e: Exception) {
            isLoadingInterstitial = false
            Log.w(TAG, "Interstitial preload skipped: ${e.message}")
        }
    }

    fun maybeShowInterstitial(activity: Activity, onContinue: () -> Unit) {
        transitionCounter += 1
        val now = SystemClock.elapsedRealtime()
        val cooldownPassed = lastInterstitialShownAt == 0L || (now - lastInterstitialShownAt) >= INTERSTITIAL_COOLDOWN_MS
        val readyAd = interstitialAd

        if (readyAd != null && cooldownPassed && transitionCounter >= TRANSITIONS_BEFORE_SHOW) {
            transitionCounter = 0
            interstitialAd = null
            lastInterstitialShownAt = now

            readyAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    preloadInterstitial(activity.applicationContext)
                    onContinue()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    preloadInterstitial(activity.applicationContext)
                    onContinue()
                }
            }
            readyAd.show(activity)
        } else {
            if (readyAd == null) {
                preloadInterstitial(activity.applicationContext)
            }
            onContinue()
        }
    }

    private fun getAdaptiveBannerSize(activity: Activity, container: FrameLayout): AdSize {
        val displayMetrics = activity.resources.displayMetrics
        var adWidthPixels = container.width.toFloat()
        if (adWidthPixels <= 0f) {
            adWidthPixels = displayMetrics.widthPixels.toFloat()
        }
        val adWidthDp = (adWidthPixels / displayMetrics.density).toInt().coerceAtLeast(320)
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidthDp)
    }
}
