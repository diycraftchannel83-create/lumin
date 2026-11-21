package com.example.ads

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.SystemClock
import android.util.Log
import com.example.R
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import java.util.Date

private const val TAG = "AdMobManagers"

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Coordinates App Open Ads and Interstitial Ads so two full-screen ads never overlap,
 * and suppresses App Open Ads when returning from system pickers/share sheets.
 */
object AdMobCoordinator {
    @Volatile
    var isAdFree: Boolean = false
        internal set

    @Volatile
    var isAnyFullscreenAdShowing: Boolean = false
        internal set

    @Volatile
    private var suppressAppOpenUntilElapsedMs: Long = 0L

    fun suppressNextAppOpen(durationMs: Long = 15_000L) {
        suppressAppOpenUntilElapsedMs = SystemClock.elapsedRealtime() + durationMs
    }

    internal fun isAppOpenSuppressed(): Boolean {
        val now = SystemClock.elapsedRealtime()
        return now < suppressAppOpenUntilElapsedMs
    }

    internal fun clearAppOpenSuppression() {
        suppressAppOpenUntilElapsedMs = 0L
    }
}

class AppOpenAdManager(private val appContext: Context) {

    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    private var loadTimeMs: Long = 0L

    private val adUnitId: String
        get() = appContext.getString(R.string.admob_app_open_unit_id)

    fun clearAd() {
        appOpenAd = null
        isLoadingAd = false
    }

    fun loadAd() {
        if (AdMobCoordinator.isAdFree || isLoadingAd || isAdAvailable()) return

        isLoadingAd = true
        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            appContext,
            adUnitId,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    isLoadingAd = false
                    loadTimeMs = Date().time
                    Log.d(TAG, "AppOpenAd loaded ($adUnitId)")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isLoadingAd = false
                    Log.w(TAG, "AppOpenAd failed to load: ${loadAdError.message}")
                }
            }
        )
    }

    private fun wasLoadTimeLessThanHoursAgo(hours: Long): Boolean {
        val diff = Date().time - loadTimeMs
        val millisPerHour = 3_600_000L
        return diff < millisPerHour * hours
    }

    fun isAdAvailable(): Boolean {
        return appOpenAd != null && wasLoadTimeLessThanHoursAgo(4L)
    }

    fun showAdIfAvailable(
        activity: Activity,
        onShowAdComplete: () -> Unit = {}
    ) {
        if (AdMobCoordinator.isAdFree) {
            clearAd()
            onShowAdComplete()
            return
        }
        if (AdMobCoordinator.isAnyFullscreenAdShowing || activity.isFinishing || activity.isDestroyed) {
            onShowAdComplete()
            return
        }

        if (AdMobCoordinator.isAppOpenSuppressed()) {
            AdMobCoordinator.clearAppOpenSuppression()
            onShowAdComplete()
            return
        }

        val ad = appOpenAd
        if (!isAdAvailable() || ad == null) {
            onShowAdComplete()
            loadAd()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                appOpenAd = null
                AdMobCoordinator.isAnyFullscreenAdShowing = false
                onShowAdComplete()
                loadAd()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                appOpenAd = null
                AdMobCoordinator.isAnyFullscreenAdShowing = false
                Log.w(TAG, "AppOpenAd failed to show: ${adError.message}")
                onShowAdComplete()
                loadAd()
            }

            override fun onAdShowedFullScreenContent() {
                AdMobCoordinator.isAnyFullscreenAdShowing = true
                Log.d(TAG, "AppOpenAd displayed")
            }
        }
        AdMobCoordinator.isAnyFullscreenAdShowing = true
        ad.show(activity)
    }
}

class InterstitialAdManager(
    private val appContext: Context,
    private val minIntervalMs: Long = 45_000L
) {
    private var interstitialAd: InterstitialAd? = null
    private var isLoadingAd = false
    private var lastShownElapsedMs: Long = 0L

    private val adUnitId: String
        get() = appContext.getString(R.string.admob_interstitial_unit_id)

    fun clearAd() {
        interstitialAd = null
        isLoadingAd = false
    }

    fun loadAd() {
        if (AdMobCoordinator.isAdFree || isLoadingAd || interstitialAd != null) return

        isLoadingAd = true
        val request = AdRequest.Builder().build()
        InterstitialAd.load(
            appContext,
            adUnitId,
            request,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoadingAd = false
                    Log.d(TAG, "InterstitialAd loaded ($adUnitId)")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    interstitialAd = null
                    isLoadingAd = false
                    Log.w(TAG, "InterstitialAd failed to load: ${loadAdError.message}")
                }
            }
        )
    }

    fun isAdAvailable(): Boolean = interstitialAd != null

    fun showAdIfAvailable(
        activity: Activity,
        respectCooldown: Boolean = true,
        onAdComplete: () -> Unit = {}
    ) {
        if (AdMobCoordinator.isAdFree) {
            clearAd()
            onAdComplete()
            return
        }
        if (AdMobCoordinator.isAnyFullscreenAdShowing || activity.isFinishing || activity.isDestroyed) {
            onAdComplete()
            return
        }

        val now = SystemClock.elapsedRealtime()
        if (respectCooldown && lastShownElapsedMs > 0L && (now - lastShownElapsedMs) < minIntervalMs) {
            onAdComplete()
            return
        }

        val ad = interstitialAd
        if (ad == null) {
            onAdComplete()
            loadAd()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                AdMobCoordinator.isAnyFullscreenAdShowing = false
                lastShownElapsedMs = SystemClock.elapsedRealtime()
                onAdComplete()
                loadAd()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                interstitialAd = null
                AdMobCoordinator.isAnyFullscreenAdShowing = false
                Log.w(TAG, "InterstitialAd failed to show: ${adError.message}")
                onAdComplete()
                loadAd()
            }

            override fun onAdShowedFullScreenContent() {
                AdMobCoordinator.isAnyFullscreenAdShowing = true
                Log.d(TAG, "InterstitialAd displayed")
            }
        }
        AdMobCoordinator.isAnyFullscreenAdShowing = true
        ad.show(activity)
    }
}
