package com.example.ministoreapp.domain

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppTier(val rank: Int) {
    FREE(0),
    STANDARD(1),
    PRO(2)
}

enum class AppFeature(val title: String, val description: String, val requiredTier: AppTier, val defaultEnabled: Boolean = true) {
    EVENTS_MODULE("Events & Tasks", "Daily store logs, fund in/out, and task tracking", AppTier.PRO),
    CREDIT_MODULE("Credit (Utang) Ledger", "Customer credit tracking and payment settlement", AppTier.FREE),
    RESTOCK_MODULE("Restock & Inventory", "Restock lists and quick '+ Restock' buttons on search cards", AppTier.STANDARD),
    INFO_MODULE("Store Info & Audit Logs", "Activity logs, store directory, and audit history", AppTier.STANDARD),
    ORDERS_MODULE("Orders Management", "Order tracking and management screen", AppTier.PRO),
    ADVANCED_PRICE_VARIANTS("Cold, Case & Bundle Pricing", "Support Cold, Wholesale Case, and Bundle price tags", AppTier.FREE),
    PROFIT_AND_PRICING_ENGINE("Cost & Profit Calculator", "Automatic profit margin and pricing calculations when adding products", AppTier.STANDARD),
    RECOMMENDATION_ENGINE("Smart Restock & Store Recommendations", "Automated product and restock recommendations", AppTier.PRO),
    VENDOR_MANAGEMENT("Vendor Directory", "Manage suppliers and link vendors to products", AppTier.STANDARD),
    CASHIER_SECURITY_PIN("Cashier & Security PIN", "Require Cashier verification and PIN for sensitive store actions", AppTier.STANDARD),
    CUSTOM_DROPDOWNS("Custom Categories & Dropdowns", "Manage custom categories, containers, and search filters", AppTier.FREE),
    CLOUD_SYNC("Google Drive Cloud Sync", "Automatic cloud backup and multi-device sync", AppTier.STANDARD),
    CUSTOMER_BAN_LIST("Customer Credit Ban List", "Block restricted customers from taking new credit", AppTier.PRO),
    AUDIT_LOGS("Audit Logs View", "Dedicated view to audit cashier actions", AppTier.STANDARD),
    TABLET_POS_LAYOUT("Tablet & Landscape POS Layout", "Multi-column product grids, side navigation rail, and split-screen layouts for tablets", AppTier.FREE),
    CAMERA_BARCODE_SCANNER("Camera Barcode Scanner", "Use device camera to scan product barcodes during search, POS checkout, credit, and restocking", AppTier.STANDARD),
    EXTERNAL_BARCODE_SCANNER("External USB / Bluetooth Scanner", "Support plug-and-play handheld USB OTG and Bluetooth HID barcode scanners for counter POS", AppTier.PRO),
    POS_CART_CHECKOUT("POS Cart & Quick Checkout", "Build multi-item carts from Search for cash checkout or direct Utang charging", AppTier.FREE)
}

object FeatureFlagManager {
    private const val PREFS_NAME = "ministore_subscription_prefs"
    private const val KEY_CURRENT_TIER = "current_tier"
    private const val KEY_IS_MONTHLY_ACTIVE = "is_monthly_subscription_active"
    private const val KEY_IS_PRO_FREE_TRIAL_ACTIVE = "is_pro_free_trial_active"
    // hasClaimedFreeTrial is maintained in SecuritySettingsEntity, but we can cache it here for debug toggles if needed. We'll use the DB value though.

    private val _selectedTierFlow = MutableStateFlow(AppTier.PRO)
    val selectedTierFlow: StateFlow<AppTier> = _selectedTierFlow.asStateFlow()

    private val _isSubscriptionActiveFlow = MutableStateFlow(true)
    val isSubscriptionActiveFlow: StateFlow<Boolean> = _isSubscriptionActiveFlow.asStateFlow()

    private val _isProFreeTrialActiveFlow = MutableStateFlow(false)
    val isProFreeTrialActiveFlow: StateFlow<Boolean> = _isProFreeTrialActiveFlow.asStateFlow()

    private val _userFeatureTogglesFlow = MutableStateFlow<Map<AppFeature, Boolean>>(emptyMap())
    val userFeatureTogglesFlow: StateFlow<Map<AppFeature, Boolean>> = _userFeatureTogglesFlow.asStateFlow()

    private val _effectiveTierFlow = MutableStateFlow(AppTier.PRO)
    val effectiveTierFlow: StateFlow<AppTier> = _effectiveTierFlow.asStateFlow()

    fun init(context: Context) {
        val prefs = getPrefs(context)
        
        val tierName = prefs.getString(KEY_CURRENT_TIER, AppTier.PRO.name) ?: AppTier.PRO.name
        _selectedTierFlow.value = try { AppTier.valueOf(tierName) } catch (e: Exception) { AppTier.PRO }
        
        _isSubscriptionActiveFlow.value = prefs.getBoolean(KEY_IS_MONTHLY_ACTIVE, true)
        _isProFreeTrialActiveFlow.value = prefs.getBoolean(KEY_IS_PRO_FREE_TRIAL_ACTIVE, false)
        
        val toggles = mutableMapOf<AppFeature, Boolean>()
        for (feature in AppFeature.entries) {
            toggles[feature] = prefs.getBoolean("feature_toggle_${feature.name}", feature.defaultEnabled)
        }
        _userFeatureTogglesFlow.value = toggles
        
        updateEffectiveTier()
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun updateEffectiveTier() {
        val effective = when {
            _isProFreeTrialActiveFlow.value -> AppTier.PRO
            _isSubscriptionActiveFlow.value -> _selectedTierFlow.value
            else -> AppTier.FREE
        }
        _effectiveTierFlow.value = effective
    }

    fun setTier(context: Context, tier: AppTier) {
        _selectedTierFlow.value = tier
        getPrefs(context).edit().putString(KEY_CURRENT_TIER, tier.name).apply()
        updateEffectiveTier()
    }

    fun setMonthlySubscriptionActive(context: Context, active: Boolean) {
        _isSubscriptionActiveFlow.value = active
        getPrefs(context).edit().putBoolean(KEY_IS_MONTHLY_ACTIVE, active).apply()
        updateEffectiveTier()
    }

    fun setProFreeTrialActive(context: Context, active: Boolean) {
        _isProFreeTrialActiveFlow.value = active
        getPrefs(context).edit().putBoolean(KEY_IS_PRO_FREE_TRIAL_ACTIVE, active).apply()
        updateEffectiveTier()
    }

    fun setUserFeatureToggle(context: Context, feature: AppFeature, enabled: Boolean) {
        val currentToggles = _userFeatureTogglesFlow.value.toMutableMap()
        currentToggles[feature] = enabled
        _userFeatureTogglesFlow.value = currentToggles
        getPrefs(context).edit().putBoolean("feature_toggle_${feature.name}", enabled).apply()
    }

    val effectiveTier: AppTier get() = _effectiveTierFlow.value

    fun isFeatureActive(feature: AppFeature): Boolean {
        val userEnabled = _userFeatureTogglesFlow.value[feature] ?: feature.defaultEnabled
        return (effectiveTier.rank >= feature.requiredTier.rank) && userEnabled
    }

    fun maxCustomersAllowed(): Int = when (effectiveTier) {
        AppTier.FREE -> 20
        AppTier.STANDARD -> 100
        AppTier.PRO -> Int.MAX_VALUE
    }
}