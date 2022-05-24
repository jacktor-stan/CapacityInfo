package com.ph03nix_x.capacityinfo.interfaces

import android.annotation.SuppressLint
import android.app.Activity
import android.widget.Toast
import com.anjlab.android.iab.v3.BillingProcessor
import com.anjlab.android.iab.v3.PurchaseInfo
import com.ph03nix_x.capacityinfo.R
import com.ph03nix_x.capacityinfo.activities.MainActivity
import com.ph03nix_x.capacityinfo.donationId
import com.ph03nix_x.capacityinfo.googlePlayLicenseKey
import com.ph03nix_x.capacityinfo.premiumId

/**
 * Created by Ph03niX-X on 04.12.2021
 * Ph03niX-X@outlook.com
 */

@SuppressLint("StaticFieldLeak")
interface DonateInterface: BillingProcessor.IBillingHandler {

    companion object {

        @Deprecated("Premium")
        var donateContext: Activity? = null
        var premiumContext: Activity? = null
        var billingProcessor: BillingProcessor? = null

        @Deprecated("Premium")
        var isDonation = false
        @Deprecated("Premium")
        var isDonated = false
        var isPurchasePremium = false
        var isPremium = false
    }

    override fun onProductPurchased(productId: String, details: PurchaseInfo?) {
        isDonation = false
        isPurchasePremium = false
        isDonated = billingProcessor?.isPurchased(donationId) == true
        isPremium = billingProcessor?.isPurchased(premiumId) == true
        if(isDonated) {
            if(donateContext != null) Toast.makeText(donateContext!!,
                R.string.thanks_for_the_donation, Toast.LENGTH_LONG).show()
            MainActivity.instance?.toolbar?.menu?.findItem(R.id.premium)?.isVisible = false
        }
        else if(isPremium) {
            if(premiumContext != null) Toast.makeText(premiumContext!!,
                R.string.premium_features_unlocked, Toast.LENGTH_LONG).show()
            MainActivity.instance?.toolbar?.menu?.findItem(R.id.premium)?.isVisible = false
        }
    }

    override fun onPurchaseHistoryRestored() {
        isDonation = false
        isPurchasePremium = false
        isDonated = billingProcessor?.isPurchased(donationId) == true
        isPremium = billingProcessor?.isPurchased(premiumId) == true
        if(isDonated || isPremium)
            MainActivity.instance?.toolbar?.menu?.findItem(R.id.premium)?.isVisible = false
    }

    override fun onBillingError(errorCode: Int, error: Throwable?) {
        isDonation = false
        isPurchasePremium = false
    }

    override fun onBillingInitialized() {
        if(donateContext != null && isDonation && BillingProcessor.isIabServiceAvailable(
                donateContext) && billingProcessor?.isInitialized == true) {
                    billingProcessor?.purchase(donateContext!!, donationId)
        }
        else if(premiumContext != null && isPurchasePremium &&
            BillingProcessor.isIabServiceAvailable(premiumContext)
            && billingProcessor?.isInitialized == true){
            billingProcessor?.purchase(premiumContext, premiumId)
        }
    }

    @Deprecated("Premium")
    fun openDonate() {
        isDonation = true
        if(donateContext != null && BillingProcessor.isIabServiceAvailable(donateContext!!))
            billingProcessor = BillingProcessor(donateContext, googlePlayLicenseKey, this)
        if(billingProcessor?.isInitialized != true) billingProcessor?.initialize()
    }

    @Deprecated("Premium")
    fun isDonated(): Boolean {
        if (donateContext != null && BillingProcessor.isIabServiceAvailable(donateContext))
            billingProcessor = BillingProcessor(donateContext, googlePlayLicenseKey, this)
        if (billingProcessor?.isInitialized != true) billingProcessor?.initialize()
        return billingProcessor?.isPurchased(donationId) ?: false
    }

    fun getOrderId(): String? {
        if(premiumContext != null && BillingProcessor.isIabServiceAvailable(premiumContext))
            billingProcessor = BillingProcessor(premiumContext, googlePlayLicenseKey, this)
        if(billingProcessor?.isInitialized != true) billingProcessor?.initialize()
        if(isDonated()) return billingProcessor?.getPurchaseInfo(donationId)?.purchaseData?.orderId
        else if(isPremium()) return billingProcessor?.getPurchaseInfo(premiumId)?.purchaseData?.orderId
        return null
    }

    fun purchasePremium() {
        isPurchasePremium = true
        if(premiumContext != null && BillingProcessor.isIabServiceAvailable(premiumContext!!))
            billingProcessor = BillingProcessor(premiumContext, googlePlayLicenseKey, this)
        if(billingProcessor?.isInitialized != true) billingProcessor?.initialize()
    }

    fun isPremium(): Boolean {
        if (premiumContext != null && BillingProcessor.isIabServiceAvailable(premiumContext))
            billingProcessor = BillingProcessor(premiumContext, googlePlayLicenseKey, this)
        if (billingProcessor?.isInitialized != true) billingProcessor?.initialize()
        return billingProcessor?.isPurchased(premiumId) ?: false
    }
}