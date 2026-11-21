package com.example

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.ads.AdMobCoordinator
import com.example.ads.AppOpenAdManager
import com.example.ads.InterstitialAdManager
import com.example.billing.PremiumBillingManager
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val TAG = "PhotoStudioApplication"

class PhotoStudioApplication :
    Application(),
    Application.ActivityLifecycleCallbacks,
    DefaultLifecycleObserver {

    lateinit var premiumBillingManager: PremiumBillingManager
        private set

    lateinit var appOpenAdManager: AppOpenAdManager
        private set

    lateinit var interstitialAdManager: InterstitialAdManager
        private set

    private var currentActivity: Activity? = null
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super<Application>.onCreate()
        registerActivityLifecycleCallbacks(this)

        appOpenAdManager = AppOpenAdManager(this)
        interstitialAdManager = InterstitialAdManager(this)
        premiumBillingManager = PremiumBillingManager(this) { isAdFree ->
            AdMobCoordinator.isAdFree = isAdFree
            if (isAdFree) {
                appOpenAdManager.clearAd()
                interstitialAdManager.clearAd()
            } else {
                appOpenAdManager.loadAd()
                interstitialAdManager.loadAd()
            }
        }
        AdMobCoordinator.isAdFree = premiumBillingManager.isPremiumAdFree
        premiumBillingManager.initialize()

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        if (!premiumBillingManager.isPremiumAdFree) {
            appScope.launch {
                try {
                    MobileAds.initialize(this@PhotoStudioApplication) {
                        CoroutineScope(Dispatchers.Main).launch {
                            if (!premiumBillingManager.isPremiumAdFree) {
                                appOpenAdManager.loadAd()
                                interstitialAdManager.loadAd()
                            }
                        }
                    }
                } catch (t: Throwable) {
                    Log.w(TAG, "MobileAds initialization skipped or failed in test environment", t)
                }
            }
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        super<DefaultLifecycleObserver>.onStart(owner)
        if (premiumBillingManager.isPremiumAdFree) return
        val activity = currentActivity ?: return
        appOpenAdManager.showAdIfAvailable(activity)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    override fun onActivityStarted(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity === activity) {
            currentActivity = null
        }
    }

    fun showInterstitialIfAvailable(activity: Activity, onComplete: () -> Unit = {}) {
        interstitialAdManager.showAdIfAvailable(activity, onAdComplete = onComplete)
    }
}
