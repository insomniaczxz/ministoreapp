package com.example.ministoreapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.lifecycleScope
import com.example.ministoreapp.data.local.AppDatabase
import com.example.ministoreapp.data.local.entities.AuditLogEntity
import com.example.ministoreapp.data.local.entities.CreditTransactionEntity
import com.example.ministoreapp.data.local.entities.PriceHistoryEntity
import com.example.ministoreapp.data.local.entities.RestockItemEntity
import com.example.ministoreapp.data.local.entities.SecuritySettingsEntity
import com.example.ministoreapp.data.repository.SyncRepository
import com.example.ministoreapp.ui.components.GlobalHeader
import com.example.ministoreapp.ui.components.SecurityPinDialog
import com.example.ministoreapp.ui.screens.credit.CreditScreen
import com.example.ministoreapp.ui.screens.events.EventsScreen
import com.example.ministoreapp.ui.screens.info.InfoScreen
import com.example.ministoreapp.ui.screens.restock.RestockScreen
import com.example.ministoreapp.ui.screens.search.SearchScreen
import com.example.ministoreapp.ui.screens.settings.SettingsScreen
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

sealed class TabScreen(val route: String, val title: String, val icon: ImageVector) {
    object Search : TabScreen("search", "Search", Icons.Default.Search)
    object Events : TabScreen("events", "Events", Icons.AutoMirrored.Filled.EventNote)
    object Credit : TabScreen("credit", "Credit", Icons.Default.Payment)
    object Restock : TabScreen("restock", "Restock", Icons.Default.Inventory2)
    object Info : TabScreen("info", "Info", Icons.Default.Info)
    object Settings : TabScreen("settings", "Settings", Icons.Default.Settings)
}

fun formatGroupDisplayTitle(baseName: String, variants: String, unitSize: String, containerType: String): String {
    return buildString {
        if (baseName.contains("*")) {
            val variantsList = variants.split(",").map { it.trim() }.filter { it.isNotBlank() }.joinToString(", ")
            val cleanBase = baseName.replace("*", "").trim()
            if (variantsList.isNotBlank()) {
                append(variantsList)
                if (cleanBase.isNotBlank()) append(" — ")
            }
            append(cleanBase)
        } else {
            append(baseName)
        }
        if (unitSize.isNotBlank()) append(" $unitSize")
        if (containerType.isNotBlank()) append(" $containerType")
    }.ifBlank { baseName }
}

class MainActivity : ComponentActivity() {

    private lateinit var db: AppDatabase
    private lateinit var syncRepo: SyncRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        db = AppDatabase.getInstance(this)
        syncRepo = SyncRepository(db)

        setContent {
            var isDarkTheme by remember { mutableStateOf(false) }

            val lightColors = lightColorScheme(
                primary = androidx.compose.ui.graphics.Color(0xFF0B5E38),
                onPrimary = androidx.compose.ui.graphics.Color.White,
                primaryContainer = androidx.compose.ui.graphics.Color(0xFFDCECE2),
                onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF042113),
                surface = androidx.compose.ui.graphics.Color.White,
                onSurface = androidx.compose.ui.graphics.Color(0xFF191C1A),
                background = androidx.compose.ui.graphics.Color(0xFFEFF4F1),
                onBackground = androidx.compose.ui.graphics.Color(0xFF191C1A)
            )

            val darkColors = darkColorScheme(
                primary = androidx.compose.ui.graphics.Color(0xFF83D7A4),
                onPrimary = androidx.compose.ui.graphics.Color(0xFF00381E),
                primaryContainer = androidx.compose.ui.graphics.Color(0xFF00522E),
                onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF9FE2BD),
                surface = androidx.compose.ui.graphics.Color(0xFF1E211F),
                onSurface = androidx.compose.ui.graphics.Color(0xFFE1E3E0),
                background = androidx.compose.ui.graphics.Color(0xFF121413),
                onBackground = androidx.compose.ui.graphics.Color(0xFFE1E3E0)
            )

            MaterialTheme(colorScheme = if (isDarkTheme) darkColors else lightColors) {
                MainAppContent(
                    isDarkTheme = isDarkTheme,
                    onToggleDarkTheme = { isDarkTheme = it }
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainAppContent(
        isDarkTheme: Boolean,
        onToggleDarkTheme: (Boolean) -> Unit
    ) {
        var currentTab by remember { mutableStateOf<TabScreen>(TabScreen.Search) }
        var searchQuery by remember { mutableStateOf("") }

        // Live States from Database Flows
        val securitySettings by db.securitySettingsDao().getSettingsFlow().collectAsState(initial = null)
        val productGroups by db.productGroupDao().getAllGroupsFlow().collectAsState(initial = emptyList())
        val products by db.productDao().getAllProductsFlow().collectAsState(initial = emptyList())
        val cashiers by db.cashierDao().getAllCashiersFlow().collectAsState(initial = emptyList())
        val customers by db.customerDao().getAllCustomersFlow().collectAsState(initial = emptyList())
        val events by db.eventDao().getAllEventsFlow().collectAsState(initial = emptyList())
        val restockItems by db.restockItemDao().getRestockItemsFlow().collectAsState(initial = emptyList())
        val banRecords by db.banRecordDao().getAllBanRecordsFlow().collectAsState(initial = emptyList())
        val auditLogs by db.auditLogDao().getAllAuditLogsFlow().collectAsState(initial = emptyList())
        val priceHistory by db.priceHistoryDao().getRecentPriceChangesFlow(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L).collectAsState(initial = emptyList())
        val creditTransactions by db.creditTransactionDao().getAllTransactionsFlow().collectAsState(initial = emptyList())

        val stores by db.masterDataDao().getStoresFlow().collectAsState(initial = emptyList())
        val categories by db.masterDataDao().getCategoriesFlow().collectAsState(initial = emptyList())
        val brands by db.masterDataDao().getBrandsFlow().collectAsState(initial = emptyList())
        val units by db.masterDataDao().getUnitsFlow().collectAsState(initial = emptyList())
        val containers by db.masterDataDao().getContainersFlow().collectAsState(initial = emptyList())
        val wholesaleUnits by db.masterDataDao().getWholesaleUnitsFlow().collectAsState(initial = emptyList())
        val vendors by db.masterDataDao().getVendorsFlow().collectAsState(initial = emptyList())

        val context = androidx.compose.ui.platform.LocalContext.current
        val prefs = remember { context.getSharedPreferences("ministore_prefs", android.content.Context.MODE_PRIVATE) }
        var isOnboardingComplete by remember { mutableStateOf(prefs.getBoolean("is_onboarding_complete", false)) }

        val settings = securitySettings ?: SecuritySettingsEntity()

        val gso = remember {
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestProfile()
                .build()
        }
        val googleSignInClient = remember(context) {
            GoogleSignIn.getClient(context, gso)
        }

        var pendingGoogleSignInCallback by remember { mutableStateOf<((String?) -> Unit)?>(null) }

        val googleSignInLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
            contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    val email = account.email ?: account.displayName ?: "Google User"
                    prefs.edit().putString("google_account_email", email).apply()

                    val cloudJson: String? = null
                    pendingGoogleSignInCallback?.invoke(cloudJson)
                } else {
                    pendingGoogleSignInCallback?.invoke(null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                pendingGoogleSignInCallback?.invoke(null)
            } finally {
                pendingGoogleSignInCallback = null
            }
        }

        if (!isOnboardingComplete) {
            com.example.ministoreapp.ui.screens.onboarding.OnboardingScreen(
                defaultPin = settings.securityPin.ifBlank { "526375" },
                onGoogleSignInRequested = { onSuccess ->
                    pendingGoogleSignInCallback = onSuccess
                    googleSignInLauncher.launch(googleSignInClient.signInIntent)
                },
                onCreateCashier = { firstCashier ->
                    lifecycleScope.launch {
                        db.cashierDao().insertCashier(firstCashier)
                    }
                },
                onUpdateSecurityPin = { newPin ->
                    lifecycleScope.launch {
                        val updated = settings.copy(securityPin = newPin, updatedAt = System.currentTimeMillis())
                        db.securitySettingsDao().insertOrUpdate(updated)
                    }
                },
                onOnboardingComplete = {
                    prefs.edit().putBoolean("is_onboarding_complete", true).apply()
                    isOnboardingComplete = true
                }
            )
            return
        }

        // App Launch Guard State
        var isAppUnlocked by remember { mutableStateOf(false) }
        var showSettingsPinPrompt by remember { mutableStateOf(false) }
        var activeSettingsDetailView by remember { mutableStateOf<String?>(null) }

        // App Launch Security PIN Check
        if (settings.pinOnAppLaunchEnabled && !isAppUnlocked) {
            SecurityPinDialog(
                title = "Store PIN Protection",
                expectedPin = settings.securityPin,
                onDismiss = { finish() },
                onSuccess = { isAppUnlocked = true }
            )
            return
        }

        // Settings Opening Security PIN Check
        if (showSettingsPinPrompt) {
            SecurityPinDialog(
                title = "Settings Security PIN",
                expectedPin = settings.securityPin,
                onDismiss = { showSettingsPinPrompt = false },
                onSuccess = {
                    showSettingsPinPrompt = false
                    activeSettingsDetailView = null
                    currentTab = TabScreen.Settings
                }
            )
        }

        val activity = (context as? android.app.Activity)
        var lastBackPressTime by remember { mutableLongStateOf(0L) }

        androidx.activity.compose.BackHandler(enabled = true) {
            when {
                activeSettingsDetailView != null -> {
                    activeSettingsDetailView = null
                }
                currentTab != TabScreen.Search -> {
                    currentTab = TabScreen.Search
                }
                else -> {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastBackPressTime < 2000) {
                        activity?.finish()
                    } else {
                        lastBackPressTime = currentTime
                        Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        val headerTitle = when {
            currentTab == TabScreen.Settings && activeSettingsDetailView != null ->
                com.example.ministoreapp.ui.screens.settings.getDetailTitle(activeSettingsDetailView)
            currentTab == TabScreen.Settings -> "Settings"
            else -> settings.storeName
        }

        val showHeaderBackButton = currentTab == TabScreen.Settings && activeSettingsDetailView != null

        Scaffold(
            topBar = {
                GlobalHeader(
                    title = headerTitle,
                    showBackButton = showHeaderBackButton,
                    onBackClick = { activeSettingsDetailView = null },
                    onSyncClick = {
                        lifecycleScope.launch {
                            try {
                                val jsonStr = syncRepo.exportChangesetJson()
                                val result = syncRepo.importAndMergeChangeset(this@MainActivity, jsonStr)
                                if (result.isSuccess) {
                                    Toast.makeText(this@MainActivity, "Data synced successfully!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(this@MainActivity, "Sync completed with warnings", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(this@MainActivity, "Sync Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onSettingsClick = {
                        if (settings.pinOnSettingsOpeningEnabled) {
                            showSettingsPinPrompt = true
                        } else {
                            activeSettingsDetailView = null
                            currentTab = TabScreen.Settings
                        }
                    }
                )
            },
            bottomBar = {
                val navContainerColor = if (isDarkTheme) {
                    androidx.compose.ui.graphics.Color(0xFF1B1E1C)
                } else {
                    MaterialTheme.colorScheme.surface
                }

                NavigationBar(
                    containerColor = navContainerColor
                ) {
                    val items = listOf(
                        TabScreen.Search,
                        TabScreen.Events,
                        TabScreen.Credit,
                        TabScreen.Restock,
                        TabScreen.Info
                    )
                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentTab == screen,
                            onClick = {
                                if (screen == TabScreen.Settings) {
                                    if (settings.pinOnSettingsOpeningEnabled && currentTab != TabScreen.Settings) {
                                        showSettingsPinPrompt = true
                                    } else {
                                        activeSettingsDetailView = null
                                        currentTab = TabScreen.Settings
                                    }
                                } else {
                                    currentTab = screen
                                }
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentTab) {
                    TabScreen.Search -> SearchScreen(
                        products = products,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        onAddToOrder = { restockItem ->
                            lifecycleScope.launch {
                                db.restockItemDao().insertRestockItem(restockItem)
                            }
                        }
                    )
                    TabScreen.Events -> EventsScreen(
                        events = events,
                        cashiers = cashiers,
                        onCreateEvent = { newEvent ->
                            lifecycleScope.launch {
                                db.eventDao().insertEvent(newEvent)
                                val descName = newEvent.description.ifBlank { newEvent.transferType ?: newEvent.type }
                                db.auditLogDao().insertAuditLog(
                                    AuditLogEntity(
                                        cashierId = newEvent.cashierId,
                                        cashierName = newEvent.cashierName,
                                        actionType = "LOG_EVENT",
                                        description = "Logged event '$descName' (₱${newEvent.total.roundToInt()})"
                                    )
                                )
                            }
                        },
                        onSettleEvent = { event, cashier, note ->
                            lifecycleScope.launch {
                                val updated = event.copy(
                                    isSettled = true,
                                    settlementCashierId = cashier.id,
                                    settlementCashierName = cashier.name,
                                    settlementNote = note,
                                    updatedAt = System.currentTimeMillis()
                                )
                                db.eventDao().insertEvent(updated)
                                db.auditLogDao().insertAuditLog(
                                    AuditLogEntity(
                                        cashierId = cashier.id,
                                        cashierName = cashier.name,
                                        actionType = "SETTLE_EVENT",
                                        description = "Settled event '${event.description}'"
                                    )
                                )
                            }
                        },
                        onDeleteEvent = { event, cashier ->
                            lifecycleScope.launch {
                                db.eventDao().deleteEvent(event)
                                db.auditLogDao().insertAuditLog(
                                    AuditLogEntity(
                                        cashierId = cashier.id,
                                        cashierName = cashier.name,
                                        actionType = "DELETE_EVENT",
                                        description = "Deleted event '${event.description}'"
                                    )
                                )
                            }
                        }
                    )
                    TabScreen.Credit -> CreditScreen(
                        customers = customers,
                        banRecords = banRecords,
                        cashiers = cashiers,
                        products = products,
                        onAddCustomer = { customer, cashier ->
                            lifecycleScope.launch {
                                db.customerDao().insertCustomer(customer)
                                db.auditLogDao().insertAuditLog(
                                    AuditLogEntity(
                                        cashierId = cashier.id,
                                        cashierName = cashier.name,
                                        actionType = "ADD_CUSTOMER",
                                        description = "Added customer ${customer.name}"
                                    )
                                )
                            }
                        },
                        onAddCredit = { customer, amount, note, borrowedAt, cashier ->
                            lifecycleScope.launch {
                                val updated = customer.copy(balance = customer.balance + amount, updatedAt = System.currentTimeMillis())
                                db.customerDao().insertCustomer(updated)
                                db.creditTransactionDao().insertTransaction(
                                    CreditTransactionEntity(
                                        customerId = customer.id,
                                        totalAmount = amount,
                                        type = "CREDIT_ADD",
                                        notes = note,
                                        borrowedAt = borrowedAt,
                                        cashierId = cashier.id,
                                        cashierName = cashier.name
                                    )
                                )
                                db.auditLogDao().insertAuditLog(
                                    AuditLogEntity(
                                        cashierId = cashier.id,
                                        cashierName = cashier.name,
                                        actionType = "CREDIT_BORROW",
                                        description = "Utang of ₱${amount.roundToInt()} for customer ${customer.name}"
                                    )
                                )
                            }
                        },
                        onMakePayment = { customer, amount, cashier ->
                            lifecycleScope.launch {
                                val updated = customer.copy(balance = (customer.balance - amount).coerceAtLeast(0.0), updatedAt = System.currentTimeMillis())
                                db.customerDao().insertCustomer(updated)
                                db.creditTransactionDao().insertTransaction(
                                    CreditTransactionEntity(
                                        customerId = customer.id,
                                        totalAmount = amount,
                                        type = "PAYMENT_PARTIAL",
                                        cashierId = cashier.id,
                                        cashierName = cashier.name
                                    )
                                )
                                db.auditLogDao().insertAuditLog(
                                    AuditLogEntity(
                                        cashierId = cashier.id,
                                        cashierName = cashier.name,
                                        actionType = "CREDIT_PAYMENT",
                                        description = "Bayad of ₱${amount.roundToInt()} from customer ${customer.name}"
                                    )
                                )
                            }
                        },
                        getTransactionsForCustomerFlow = { customerId ->
                            db.creditTransactionDao().getTransactionsForCustomerFlow(customerId)
                        }
                    )
                    TabScreen.Restock -> RestockScreen(
                        restockItems = restockItems,
                        allProducts = products,
                        onUpdateQuantity = { item, newQty ->
                            lifecycleScope.launch {
                                db.restockItemDao().insertRestockItem(item.copy(quantity = newQty))
                            }
                        },
                        onRemoveItem = { item ->
                            lifecycleScope.launch {
                                db.restockItemDao().deleteRestockItem(item)
                            }
                        },
                        onClearRestock = {
                            lifecycleScope.launch {
                                db.restockItemDao().clearRestockItems()
                            }
                        }
                    )
                    TabScreen.Info -> InfoScreen(
                        securitySettings = securitySettings,
                        priceHistory = priceHistory,
                        vendorStores = stores,
                        vendors = vendors,
                        allProducts = products,
                        banRecords = banRecords,
                        creditTransactions = creditTransactions,
                        customers = customers,
                        wholesaleProducts = products.filter { it.isWholesaleParent }
                    )
                    TabScreen.Settings -> SettingsScreen(
                        activeDetailView = activeSettingsDetailView,
                        onActiveDetailViewChange = { activeSettingsDetailView = it },
                        securitySettings = securitySettings,
                        products = products,
                        productGroups = productGroups,
                        cashiers = cashiers,
                        auditLogs = auditLogs,
                        banRecords = banRecords,
                        stores = stores,
                        categories = categories,
                        brands = brands,
                        units = units,
                        containers = containers,
                        wholesaleUnits = wholesaleUnits,
                        vendors = vendors,
                        isDarkTheme = isDarkTheme,
                        onToggleDarkTheme = onToggleDarkTheme,
                        onSwitchGoogleAccount = {
                            pendingGoogleSignInCallback = { _ -> }
                            googleSignInLauncher.launch(googleSignInClient.signInIntent)
                        },
                        onSaveProductGroup = { group, generatedItems, cashier ->
                            lifecycleScope.launch {
                                db.productGroupDao().insertGroup(group)

                                val oldProducts = db.productDao().getAllProducts().filter { it.groupId == group.id }
                                oldProducts.forEach { old ->
                                    val priceDiff = generatedItems.find { it.comboKey == old.comboKey }?.sellingPrice?.minus(old.sellingPrice) ?: 0.0
                                    if (kotlin.math.abs(priceDiff) >= 0.01) {
                                        db.priceHistoryDao().insertPriceHistory(
                                            PriceHistoryEntity(
                                                productId = old.id,
                                                productName = old.name,
                                                oldPrice = old.sellingPrice,
                                                newPrice = old.sellingPrice + priceDiff,
                                                cashierId = cashier.id,
                                                cashierName = cashier.name
                                            )
                                        )
                                    }
                                }

                                generatedItems.forEach { db.productDao().insertProduct(it) }

                                val displayTitle = formatGroupDisplayTitle(group.baseName, group.variants, group.unitSize, group.containerType)
                                db.auditLogDao().insertAuditLog(
                                    AuditLogEntity(
                                        cashierId = cashier.id,
                                        cashierName = cashier.name,
                                        actionType = "SAVE_PRODUCT_RECIPE",
                                        description = "Saved product recipe '$displayTitle' (${generatedItems.size} items generated)"
                                    )
                                )
                            }
                        },
                        onDeleteSingleProduct = { product, cashier ->
                            lifecycleScope.launch {
                                db.productDao().deleteProduct(product)
                                db.auditLogDao().insertAuditLog(
                                    AuditLogEntity(
                                        cashierId = cashier.id,
                                        cashierName = cashier.name,
                                        actionType = "DELETE_PRODUCT",
                                        description = "Deleted item '${product.name}'"
                                    )
                                )
                            }
                        },
                        onDeleteProductGroup = { groupId, cashier ->
                            lifecycleScope.launch {
                                val groupObj = db.productGroupDao().getGroupById(groupId)
                                val groupProducts = db.productDao().getAllProducts().filter { it.groupId == groupId }
                                groupProducts.forEach { db.productDao().deleteProduct(it) }
                                if (groupObj != null) {
                                    db.productGroupDao().deleteGroup(groupObj)
                                    val displayTitle = formatGroupDisplayTitle(groupObj.baseName, groupObj.variants, groupObj.unitSize, groupObj.containerType)
                                    db.auditLogDao().insertAuditLog(
                                        AuditLogEntity(
                                            cashierId = cashier.id,
                                            cashierName = cashier.name,
                                            actionType = "DELETE_PRODUCT_RECIPE",
                                            description = "Deleted product recipe '$displayTitle'"
                                        )
                                    )
                                }
                            }
                        },
                        onSaveCashier = { cashier, performerName, reason ->
                            lifecycleScope.launch {
                                db.cashierDao().insertCashier(cashier)
                                db.auditLogDao().insertAuditLog(
                                    AuditLogEntity(
                                        cashierId = cashier.id,
                                        cashierName = performerName,
                                        actionType = "SAVE_CASHIER",
                                        description = "Saved cashier '${cashier.name}' (Note: $reason)"
                                    )
                                )
                            }
                        },
                        onDeleteCashier = { cashier, performerName, reason ->
                            lifecycleScope.launch {
                                db.cashierDao().deleteCashier(cashier)
                                db.auditLogDao().insertAuditLog(
                                    AuditLogEntity(
                                        cashierId = cashier.id,
                                        cashierName = performerName,
                                        actionType = "DELETE_CASHIER",
                                        description = "Deleted cashier '${cashier.name}' (Reason: $reason)"
                                    )
                                )
                            }
                        },
                        onSaveBanRecord = { ban, cashier ->
                            lifecycleScope.launch {
                                db.banRecordDao().insertBanRecord(ban)
                                db.auditLogDao().insertAuditLog(
                                    AuditLogEntity(
                                        cashierId = cashier.id,
                                        cashierName = cashier.name,
                                        actionType = "ADD_BAN_RECORD",
                                        description = "Added credit ban record for ${ban.fullName}"
                                    )
                                )
                            }
                        },
                        onDeleteBanRecord = { ban, cashier ->
                            lifecycleScope.launch {
                                db.banRecordDao().deleteBanRecord(ban)
                                db.auditLogDao().insertAuditLog(
                                    AuditLogEntity(
                                        cashierId = cashier.id,
                                        cashierName = cashier.name,
                                        actionType = "DELETE_BAN_RECORD",
                                        description = "Deleted credit ban record for ${ban.fullName}"
                                    )
                                )
                            }
                        },
                        onSaveMasterData = { type, name ->
                            lifecycleScope.launch {
                                when (type) {
                                    "Store" -> db.masterDataDao().insertStore(com.example.ministoreapp.data.local.entities.StoreEntity(name = name))
                                    "Category" -> db.masterDataDao().insertCategory(com.example.ministoreapp.data.local.entities.CategoryEntity(name = name))
                                    "Brand" -> db.masterDataDao().insertBrand(com.example.ministoreapp.data.local.entities.BrandEntity(name = name))
                                    "Unit" -> db.masterDataDao().insertUnit(com.example.ministoreapp.data.local.entities.UnitEntity(name = name))
                                    "Container" -> db.masterDataDao().insertContainer(com.example.ministoreapp.data.local.entities.ContainerEntity(name = name))
                                    "WholesaleUnit" -> db.masterDataDao().insertWholesaleUnit(com.example.ministoreapp.data.local.entities.WholesaleUnitEntity(name = name))
                                }
                            }
                        },
                        onDeleteMasterData = { type, id ->
                            lifecycleScope.launch {
                                when (type) {
                                    "Store" -> db.masterDataDao().deleteStore(com.example.ministoreapp.data.local.entities.StoreEntity(id = id, name = ""))
                                    "Category" -> db.masterDataDao().deleteCategory(com.example.ministoreapp.data.local.entities.CategoryEntity(id = id, name = ""))
                                    "Brand" -> db.masterDataDao().deleteBrand(com.example.ministoreapp.data.local.entities.BrandEntity(id = id, name = ""))
                                    "Unit" -> db.masterDataDao().deleteUnit(com.example.ministoreapp.data.local.entities.UnitEntity(id = id, name = ""))
                                    "Container" -> db.masterDataDao().deleteContainer(com.example.ministoreapp.data.local.entities.ContainerEntity(id = id, name = ""))
                                    "WholesaleUnit" -> db.masterDataDao().deleteWholesaleUnit(com.example.ministoreapp.data.local.entities.WholesaleUnitEntity(id = id, name = ""))
                                }
                            }
                        },
                        onUpdateSecuritySettings = { updatedSettings ->
                            lifecycleScope.launch {
                                db.securitySettingsDao().insertOrUpdate(updatedSettings)
                            }
                        },
                        onSaveVendor = { vendor ->
                            lifecycleScope.launch {
                                db.masterDataDao().insertVendor(vendor)
                            }
                        },
                        onDeleteVendor = { vendor ->
                            lifecycleScope.launch {
                                db.masterDataDao().deleteVendor(vendor)
                            }
                        },
                        onExportBackupJson = {
                            val exportObj = org.json.JSONObject().apply {
                                put("version", 2)
                                put("exportedAt", System.currentTimeMillis())
                                put("storeName", settings.storeName)

                                val groupsArray = org.json.JSONArray()
                                productGroups.forEach { g ->
                                    groupsArray.put(org.json.JSONObject().apply {
                                        put("id", g.id)
                                        put("baseName", g.baseName)
                                        put("aliases", g.aliases)
                                        put("variants", g.variants)
                                        put("brand", g.brand)
                                        put("category", g.category)
                                        put("storeName", g.storeName)
                                        put("unitSize", g.unitSize)
                                        put("containerType", g.containerType)
                                        put("hasWholesale", g.hasWholesale)
                                        put("wholesaleUnit", g.wholesaleUnit)
                                        put("wholesaleCost", g.wholesaleCost)
                                        put("itemsPerPack", g.itemsPerPack)
                                        put("wholesaleSellingPrice", g.wholesaleSellingPrice)
                                        put("hasRetail", g.hasRetail)
                                        put("retailCost", g.retailCost)
                                        put("retailSellingPrice", g.retailSellingPrice)
                                        put("bundleQty", g.bundleQty)
                                        put("bundlePrice", g.bundlePrice)
                                        put("isBeverage", g.isBeverage)
                                        put("coldRetailMarkup", g.coldRetailMarkup)
                                        put("coldCaseMarkup", g.coldCaseMarkup)
                                    })
                                }
                                put("productGroups", groupsArray)

                                val productsArray = org.json.JSONArray()
                                products.forEach { p ->
                                    productsArray.put(org.json.JSONObject().apply {
                                        put("id", p.id)
                                        put("groupId", p.groupId ?: "")
                                        put("comboKey", p.comboKey)
                                        put("name", p.name)
                                        put("cost", p.cost)
                                        put("sellingPrice", p.sellingPrice)
                                        put("variantType", p.variantType ?: "")
                                    })
                                }
                                put("products", productsArray)

                                val vendorsArray = org.json.JSONArray()
                                vendors.forEach { v ->
                                    vendorsArray.put(org.json.JSONObject().apply {
                                        put("id", v.id)
                                        put("name", v.name)
                                        put("ownerName", v.ownerName)
                                        put("productsSold", v.productsSold)
                                        put("address", v.address)
                                        put("contactNumber", v.contactNumber)
                                        put("agentName", v.agentName)
                                        put("agentContactNumber", v.agentContactNumber)
                                        put("isTruckPanel", v.isTruckPanel)
                                        put("routeSchedule", v.routeSchedule)
                                    })
                                }
                                put("vendors", vendorsArray)

                                val customersArray = org.json.JSONArray()
                                customers.forEach { c ->
                                    customersArray.put(org.json.JSONObject().apply {
                                        put("id", c.id)
                                        put("name", c.name)
                                        put("nickname", c.nickname)
                                        put("balance", c.balance)
                                        put("borrowedAt", c.borrowedAt)
                                    })
                                }
                                put("customers", customersArray)
                            }
                            exportObj.toString(2)
                        },
                        onImportBackupJson = { jsonStr ->
                            lifecycleScope.launch {
                                try {
                                    val root = org.json.JSONObject(jsonStr)

                                    if (root.has("productGroups")) {
                                        val arr = root.getJSONArray("productGroups")
                                        for (i in 0 until arr.length()) {
                                            val obj = arr.getJSONObject(i)
                                            val g = com.example.ministoreapp.data.local.entities.ProductGroupEntity(
                                                id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                                                baseName = obj.optString("baseName", ""),
                                                aliases = obj.optString("aliases", ""),
                                                variants = obj.optString("variants", ""),
                                                brand = obj.optString("brand", ""),
                                                category = obj.optString("category", ""),
                                                storeName = obj.optString("storeName", ""),
                                                unitSize = obj.optString("unitSize", ""),
                                                containerType = obj.optString("containerType", ""),
                                                hasWholesale = obj.optBoolean("hasWholesale", false),
                                                wholesaleUnit = obj.optString("wholesaleUnit", ""),
                                                wholesaleCost = obj.optDouble("wholesaleCost", 0.0),
                                                itemsPerPack = obj.optInt("itemsPerPack", 1),
                                                wholesaleSellingPrice = obj.optDouble("wholesaleSellingPrice", 0.0),
                                                hasRetail = obj.optBoolean("hasRetail", true),
                                                retailCost = obj.optDouble("retailCost", 0.0),
                                                retailSellingPrice = obj.optDouble("retailSellingPrice", 0.0),
                                                bundleQty = obj.optInt("bundleQty", 1),
                                                bundlePrice = obj.optDouble("bundlePrice", 0.0),
                                                isBeverage = obj.optBoolean("isBeverage", false),
                                                coldRetailMarkup = obj.optDouble("coldRetailMarkup", 0.0),
                                                coldCaseMarkup = obj.optDouble("coldCaseMarkup", 0.0)
                                            )
                                            db.productGroupDao().insertGroup(g)
                                        }
                                    }

                                    if (root.has("products")) {
                                        val arr = root.getJSONArray("products")
                                        for (i in 0 until arr.length()) {
                                            val obj = arr.getJSONObject(i)
                                            val p = com.example.ministoreapp.data.local.entities.ProductEntity(
                                                id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                                                groupId = obj.optString("groupId", "").ifBlank { null },
                                                comboKey = obj.optString("comboKey", ""),
                                                name = obj.optString("name", ""),
                                                cost = obj.optDouble("cost", 0.0),
                                                sellingPrice = obj.optDouble("sellingPrice", 0.0),
                                                variantType = obj.optString("variantType", "").ifBlank { null }
                                            )
                                            db.productDao().insertProduct(p)
                                        }
                                    }

                                    if (root.has("vendors")) {
                                        val arr = root.getJSONArray("vendors")
                                        for (i in 0 until arr.length()) {
                                            val obj = arr.getJSONObject(i)
                                            val v = com.example.ministoreapp.data.local.entities.VendorEntity(
                                                id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                                                name = obj.optString("name", ""),
                                                ownerName = obj.optString("ownerName", ""),
                                                productsSold = obj.optString("productsSold", ""),
                                                address = obj.optString("address", ""),
                                                contactNumber = obj.optString("contactNumber", ""),
                                                agentName = obj.optString("agentName", ""),
                                                agentContactNumber = obj.optString("agentContactNumber", ""),
                                                isTruckPanel = obj.optBoolean("isTruckPanel", false),
                                                routeSchedule = obj.optString("routeSchedule", "")
                                            )
                                            db.masterDataDao().insertVendor(v)
                                        }
                                    }

                                    if (root.has("customers")) {
                                        val arr = root.getJSONArray("customers")
                                        for (i in 0 until arr.length()) {
                                            val obj = arr.getJSONObject(i)
                                            val c = com.example.ministoreapp.data.local.entities.CustomerEntity(
                                                id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                                                name = obj.optString("name", ""),
                                                nickname = obj.optString("nickname", ""),
                                                balance = obj.optDouble("balance", 0.0),
                                                borrowedAt = obj.optLong("borrowedAt", System.currentTimeMillis())
                                            )
                                            db.customerDao().insertCustomer(c)
                                        }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
