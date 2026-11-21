package com.example.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.example.R
import com.example.ads.AdMobCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "PremiumBillingManager"
// Keep the legacy preference namespace to preserve previously stored premium entitlement.
private const val PREFS_NAME = "lumina_premium_billing_prefs"
private const val KEY_IS_PREMIUM_AD_FREE = "is_premium_ad_free"

data class PremiumBillingState(
    val isPremiumAdFree: Boolean = false,
    val formattedPrice: String? = null,
    val productTitle: String = "PhotoStudio Pro — Remove Ads",
    val isStoreReady: Boolean = false,
    val isPurchasing: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

class PremiumBillingManager(
    context: Context,
    private val onEntitlementChanged: (Boolean) -> Unit = {}
) : PurchasesUpdatedListener {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val removeAdsProductId: String =
        appContext.getString(R.string.play_product_remove_ads)

    private var cachedProductDetails: ProductDetails? = null
    private var isConnecting = false

    private val _state = MutableStateFlow(
        PremiumBillingState(
            isPremiumAdFree = prefs.getBoolean(KEY_IS_PREMIUM_AD_FREE, false)
        )
    )
    val state: StateFlow<PremiumBillingState> = _state.asStateFlow()

    val isPremiumAdFree: Boolean
        get() = _state.value.isPremiumAdFree

    private val billingClient: BillingClient? = try {
        BillingClient.newBuilder(appContext)
            .setListener(this)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .build()
    } catch (t: Throwable) {
        Log.w(TAG, "BillingClient unavailable in this environment", t)
        null
    }

    fun initialize() {
        connectIfNeeded()
    }

    private fun connectIfNeeded(onConnected: (() -> Unit)? = null) {
        val client = billingClient ?: return
        if (client.isReady) {
            onConnected?.invoke()
            return
        }
        if (isConnecting) return
        isConnecting = true

        try {
            client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    isConnecting = false
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        _state.update { it.copy(isStoreReady = true, errorMessage = null) }
                        scope.launch {
                            queryProductDetailsInternal()
                            queryExistingPurchasesInternal(explicitRestore = false)
                            onConnected?.invoke()
                        }
                    } else {
                        Log.w(
                            TAG,
                            "Billing setup failed (${billingResult.responseCode}): ${billingResult.debugMessage}"
                        )
                        _state.update {
                            it.copy(isStoreReady = false)
                        }
                    }
                }

                override fun onBillingServiceDisconnected() {
                    isConnecting = false
                    _state.update { it.copy(isStoreReady = false) }
                }
            })
        } catch (t: Throwable) {
            isConnecting = false
            Log.w(TAG, "Failed to start BillingClient connection", t)
        }
    }

    private suspend fun queryProductDetailsInternal(): ProductDetails? {
        val client = billingClient ?: return null
        if (!client.isReady) return null

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(removeAdsProductId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        val result = client.queryProductDetails(params)
        if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            val details = result.productDetailsList?.firstOrNull()
            cachedProductDetails = details
            if (details != null) {
                val price = details.oneTimePurchaseOfferDetails?.formattedPrice
                _state.update {
                    it.copy(
                        formattedPrice = price,
                        productTitle = details.name.ifBlank { it.productTitle }
                    )
                }
            }
            return details
        } else {
            Log.w(TAG, "queryProductDetails failed: ${result.billingResult.debugMessage}")
            return null
        }
    }

    private suspend fun queryExistingPurchasesInternal(explicitRestore: Boolean) {
        val client = billingClient ?: return
        if (!client.isReady) return

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        val result = client.queryPurchasesAsync(params)
        if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            var foundOwned = false
            for (purchase in result.purchasesList) {
                if (purchase.products.contains(removeAdsProductId) &&
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                ) {
                    foundOwned = true
                    acknowledgeIfNeeded(purchase)
                }
            }
            setEntitlement(foundOwned)
            if (explicitRestore) {
                _state.update {
                    it.copy(
                        statusMessage = if (foundOwned) {
                            "PhotoStudio Pro Ad-Free purchase restored."
                        } else {
                            "No previous Remove Ads purchase found for this Google Play account."
                        },
                        errorMessage = null
                    )
                }
            }
        } else if (explicitRestore) {
            _state.update {
                it.copy(
                    errorMessage = "Unable to query Play Store purchases: ${result.billingResult.debugMessage}"
                )
            }
        }
    }

    fun launchRemoveAdsPurchase(activity: Activity) {
        if (_state.value.isPremiumAdFree) {
            _state.update { it.copy(statusMessage = "Ads are already removed with PhotoStudio Pro.") }
            return
        }

        val client = billingClient
        if (client == null) {
            _state.update {
                it.copy(errorMessage = "Google Play Billing is not available on this device.")
            }
            return
        }

        _state.update { it.copy(isPurchasing = true, errorMessage = null, statusMessage = null) }

        if (!client.isReady) {
            connectIfNeeded {
                launchFlowWithConnectedClient(activity)
            }
            // If connection could not start synchronously, handle below
            if (!isConnecting && !client.isReady) {
                _state.update {
                    it.copy(
                        isPurchasing = false,
                        errorMessage = "Google Play Store is unavailable right now. Please sign in to Play Store and retry."
                    )
                }
            }
            return
        }

        launchFlowWithConnectedClient(activity)
    }

    private fun launchFlowWithConnectedClient(activity: Activity) {
        val client = billingClient ?: return
        scope.launch {
            val details = cachedProductDetails ?: queryProductDetailsInternal()
            if (details == null) {
                _state.update {
                    it.copy(
                        isPurchasing = false,
                        errorMessage = "In-app item '$removeAdsProductId' was not found on Google Play Console. Ensure the app is uploaded to Play Console with this product ID."
                    )
                }
                return@launch
            }

            val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .build()

            val flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(productParams))
                .build()

            AdMobCoordinator.suppressNextAppOpen()
            val launchResult = client.launchBillingFlow(activity, flowParams)
            if (launchResult.responseCode != BillingClient.BillingResponseCode.OK) {
                _state.update {
                    it.copy(
                        isPurchasing = false,
                        errorMessage = "Could not launch Play Store checkout (${launchResult.debugMessage})."
                    )
                }
            }
        }
    }

    fun restorePurchases() {
        val client = billingClient
        if (client == null) {
            _state.update {
                it.copy(errorMessage = "Google Play Billing is not available on this device.")
            }
            return
        }
        _state.update { it.copy(errorMessage = null, statusMessage = "Checking Google Play purchases...") }
        if (client.isReady) {
            scope.launch {
                queryExistingPurchasesInternal(explicitRestore = true)
            }
        } else {
            connectIfNeeded {
                scope.launch {
                    queryExistingPurchasesInternal(explicitRestore = true)
                }
            }
        }
    }

    fun clearMessages() {
        _state.update { it.copy(statusMessage = null, errorMessage = null) }
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (purchases != null) {
                    scope.launch {
                        var unlocked = false
                        var pending = false
                        for (purchase in purchases) {
                            if (purchase.products.contains(removeAdsProductId)) {
                                when (purchase.purchaseState) {
                                    Purchase.PurchaseState.PURCHASED -> {
                                        unlocked = true
                                        acknowledgeIfNeeded(purchase)
                                    }
                                    Purchase.PurchaseState.PENDING -> {
                                        pending = true
                                    }
                                }
                            }
                        }
                        if (unlocked) {
                            setEntitlement(true)
                            _state.update {
                                it.copy(
                                    isPurchasing = false,
                                    statusMessage = "Thank you! Ads have been permanently removed.",
                                    errorMessage = null
                                )
                            }
                        } else if (pending) {
                            _state.update {
                                it.copy(
                                    isPurchasing = false,
                                    statusMessage = "Purchase is pending Google Play confirmation. Ads will be removed once payment clears.",
                                    errorMessage = null
                                )
                            }
                        } else {
                            _state.update { it.copy(isPurchasing = false) }
                        }
                    }
                } else {
                    _state.update { it.copy(isPurchasing = false) }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _state.update {
                    it.copy(
                        isPurchasing = false,
                        statusMessage = "Purchase canceled.",
                        errorMessage = null
                    )
                }
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                setEntitlement(true)
                _state.update {
                    it.copy(
                        isPurchasing = false,
                        statusMessage = "PhotoStudio Pro is already owned — ads removed!",
                        errorMessage = null
                    )
                }
            }
            else -> {
                _state.update {
                    it.copy(
                        isPurchasing = false,
                        errorMessage = "Play Store purchase error (${billingResult.responseCode}): ${billingResult.debugMessage}"
                    )
                }
            }
        }
    }

    private suspend fun acknowledgeIfNeeded(purchase: Purchase) {
        if (purchase.isAcknowledged) return
        val client = billingClient ?: return
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        val ackResult = client.acknowledgePurchase(params)
        if (ackResult.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.w(TAG, "Failed to acknowledge purchase: ${ackResult.debugMessage}")
        }
    }

    private fun setEntitlement(adFree: Boolean) {
        prefs.edit().putBoolean(KEY_IS_PREMIUM_AD_FREE, adFree).apply()
        val changed = _state.value.isPremiumAdFree != adFree
        _state.update { it.copy(isPremiumAdFree = adFree) }
        if (changed) {
            onEntitlementChanged(adFree)
        }
    }
}
