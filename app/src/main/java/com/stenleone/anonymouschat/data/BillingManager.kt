package com.stenleone.anonymouschat.data

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.*
import com.android.billingclient.api.*
import kotlinx.coroutines.*

object BillingManager: DefaultLifecycleObserver, PurchasesUpdatedListener, BillingClientStateListener {

    private lateinit var context: Context

    private lateinit var billingClient: BillingClient
    private var reconnectHandler = Handler(Looper.getMainLooper())
    private var reconnectedCallback: Runnable? = null
    private var lastReconnectMillis = 1L
    private var lifecycle: Lifecycle? = null

    var liveData: MutableLiveData<PurchaseResult>? = MutableLiveData<PurchaseResult>()

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    fun setup(lifecycle: Lifecycle, context: Context) {
        BillingManager.lifecycle = lifecycle
        BillingManager.context = context
        lifecycle.addObserver(this)
    }

    override fun onCreate(owner: LifecycleOwner) {
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases()
            .build()
        if (!billingClient.isReady) {
            billingClient.startConnection(this)
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        liveData = null
        lifecycle?.removeObserver(this)
        reconnectedCallback?.let { reconnectHandler.removeCallbacks(it) }
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        val responseCode = billingResult.responseCode
        val debugMessage = billingResult.debugMessage

        when (responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                liveData?.postValue(PurchaseResult.Ok)
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                liveData?.postValue(PurchaseResult.UserCancelled)
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                liveData?.postValue(PurchaseResult.AlreadyOwned)
            }
            BillingClient.BillingResponseCode.DEVELOPER_ERROR -> {
                liveData?.postValue(PurchaseResult.DeveloperError)
            }
        }
    }

    override fun onBillingServiceDisconnected() {
        reconnectedCallback?.let { reconnectHandler.removeCallbacks(it) }
        lastReconnectMillis = lastReconnectMillis * 2
        reconnectedCallback = object : Runnable {
            override fun run() {
                billingClient.startConnection(this@BillingManager)
            }
        }.apply {
            reconnectHandler.postDelayed(this, lastReconnectMillis)
        }
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        val responseCode = billingResult.responseCode
        val debugMessage = billingResult.debugMessage

        if (responseCode == BillingClient.BillingResponseCode.OK) {
            reconnectedCallback?.let { reconnectHandler.removeCallbacks(it) }
            reconnectedCallback = null
            lastReconnectMillis = 1


        }
    }

    fun startBillingFlow(activity: Activity, sku: String) {
        scope.cancel()
        scope.launch {
            runCatching {
                val flowParams = BillingFlowParams.newBuilder()
                    .setSkuDetails(
                        billingClient.querySkuDetails(
                            SkuDetailsParams.newBuilder().setSkusList(arrayListOf(sku)).setType(
                                BillingClient.SkuType.SUBS
                            ).build()
                        ).skuDetailsList?.first()!!
                    )
                    .build()

                billingClient.launchBillingFlow(activity, flowParams).apply {
                    if (responseCode == BillingClient.BillingResponseCode.OK) {

                    } else {
                        liveData?.postValue(PurchaseResult.UnknownError)
                    }
                }
            }.onFailure {
                liveData?.postValue(PurchaseResult.UnknownError)
            }
        }
    }

    sealed interface PurchaseResult {
        object Ok : PurchaseResult
        object UserCancelled : PurchaseResult
        object AlreadyOwned : PurchaseResult
        object DeveloperError : PurchaseResult
        object UnknownError : PurchaseResult

        fun isSuccess() = this is Ok || this is AlreadyOwned

    }

}