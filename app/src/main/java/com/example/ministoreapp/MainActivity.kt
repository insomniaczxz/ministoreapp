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
import kotlinx.coroutines.launch

sealed class TabScreen(val route: String, val title: String, val icon: ImageVector) {
    object Search : TabScreen("search", "Search", Icons.Default.Search)
    object Events : TabScreen("events", "Events", Icons.AutoMirrored.Filled.EventNote)
    object Credit : TabScreen("credit", "Credit", Icons.Default.Payment)
    object Info : TabScreen("info", "Info", Icons.Default.Info)
    object Restock : TabScreen("restock", "Restock", Icons.Default.Inventory2)
    object Settings : TabScreen("settings", "Settings", Icons.Default.Settings)
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

        // Live States from Database Flows
        val securitySettings by db.securitySettingsDao().getSettingsFlow().collectAsState(initial = null)
        val productGroups by db.productGroupDao().getAllGroupsFlow().collectAsState(initial = emptyList())
        val products by db.productDao().getAllProductsFlow().collectAsState(initial = emptyList())
        val searchProducts by db.productDao().getSearchProductsFlow("").collectAsState(initial = emptyList())
        val wholesaleProducts by db.productDao().getWholesaleProductsFlow().collectAsState(initial = emptyList())
        val cashiers by db.cashierDao().getAllCashiersFlow().collectAsState(initial = emptyList())
        val customers by db.customerDao().getAllCustomersFlow().collectAsState(initial = emptyList())
        val events by db.eventDao().getAllEventsFlow().collectAsState(initial = emptyList())
        val restockItems by db.restockItemDao().getRestockItemsFlow().collectAsState(initial = emptyList())
        val banRecords by db.banRecordDao().getAllBanRecordsFlow().collectAsState(initial = emptyList())
        val auditLogs by db.auditLogDao().getAllAuditLogsFlow().collectAsState(initial = emptyList())
        val priceHistory by db.priceHistoryDao().getRecentPriceChangesFlow(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L).collectAsState(initial = emptyList())

        val stores by db.masterDataDao().getStoresFlow().collectAsState(initial = emptyList())
        val categories by db.masterDataDao().getCategoriesFlow().collectAsState(initial = emptyList())
        val brands by db.masterDataDao().getBrandsFlow().collectAsState(initial = emptyList())
        val units by db.masterDataDao().getUnitsFlow().collectAsState(initial = emptyList())
        val containers by db.masterDataDao().getContainersFlow().collectAsState(initial = emptyList())
        val wholesaleUnits by db.masterDataDao().getWholesaleUnitsFlow().collectAsState(initial = emptyList())

        // App Launch Guard State
        var isAppUnlocked by remember { mutableStateOf(false) }
        var showSettingsPinPrompt by remember { mutableStateOf(false) }
        var activeSettingsDetailView by remember { mutableStateOf<String?>(null) }

        val settings = securitySettings ?: SecuritySettingsEntity()

        // App Launch Security PIN Check
        if (settings.pinOnAppLaunchEnabled && !isAppUnlocked) {
            SecurityPinDialog(
                title = "G&J Store PIN Protection",
                expectedPin = settings.securityPin,
                onDismiss = { finish() },
                onSuccess = { isAppUnlocked = true }
            )
            return
        }

        // Settings Opening Security PIN Check
        if (showSettingsPinPrompt) {
            SecurityPinDialog(
                title = "Settings Security PIN Required",
                expectedPin = settings.securityPin,
                onDismiss = { showSettingsPinPrompt = false },
                onSuccess = {
                    showSettingsPinPrompt = false
                    activeSettingsDetailView = null
                    currentTab = TabScreen.Settings
                }
            )
        }

        val headerTitle = when {
            currentTab == TabScreen.Settings && activeSettingsDetailView != null ->
                com.example.ministoreapp.ui.screens.settings.getDetailTitle(activeSettingsDetailView)
            currentTab == TabScreen.Settings -> "Settings"
            else -> settings.storeName
        }

        val showHeaderBackButton = currentTab == TabScreen.Settings

        val onHeaderBackClick: (() -> Unit)? = if (currentTab == TabScreen.Settings) {
            {
                if (activeSettingsDetailView != null) {
                    activeSettingsDetailView = null
                } else {
                    currentTab = TabScreen.Search
                }
            }
        } else null

        val showHeaderActions = currentTab != TabScreen.Settings

        Scaffold(
            topBar = {
                GlobalHeader(
                    title = headerTitle,
                    showBackButton = showHeaderBackButton,
                    onBackClick = onHeaderBackClick,
                    showActions = showHeaderActions,
                    onSyncClick = {
                        lifecycleScope.launch {
                            try {
                                val json = syncRepo.exportChangesetJson()
                                val result = syncRepo.importAndMergeChangeset(this@MainActivity, json)
                                if (result.isSuccess) {
                                    Toast.makeText(this@MainActivity, "Google Drive Delta Sync & Merge Complete!", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(this@MainActivity, "Sync Error: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(this@MainActivity, "Sync Failed: ${e.message}", Toast.LENGTH_SHORT).show()
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
                NavigationBar(
                    containerColor = androidx.compose.ui.graphics.Color.White
                ) {
                    val items = listOf(TabScreen.Search, TabScreen.Events, TabScreen.Credit, TabScreen.Info, TabScreen.Restock)
                    items.forEach { tab ->
                        NavigationBarItem(
                            icon = { Icon(tab.icon, contentDescription = tab.title) },
                            label = { Text(tab.title, fontWeight = FontWeight.SemiBold) },
                            selected = currentTab == tab,
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = androidx.compose.ui.graphics.Color(0xFFDCECE2),
                                selectedIconColor = androidx.compose.ui.graphics.Color(0xFF0B5E38),
                                selectedTextColor = androidx.compose.ui.graphics.Color(0xFF0B5E38),
                                unselectedIconColor = androidx.compose.ui.graphics.Color(0xFF526058),
                                unselectedTextColor = androidx.compose.ui.graphics.Color(0xFF526058)
                            ),
                            onClick = {
                                if (tab == TabScreen.Settings && settings.pinOnSettingsOpeningEnabled) {
                                    showSettingsPinPrompt = true
                                } else {
                                    currentTab = tab
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
                        products = searchProducts,
                        searchQuery = "",
                        onSearchQueryChange = {},
                        onAddToOrder = { restockItem ->
                            lifecycleScope.launch {
                                db.restockItemDao().insertRestockItem(restockItem)
                                Toast.makeText(this@MainActivity, "Added '${restockItem.productName}' to Restock List!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    TabScreen.Events -> EventsScreen(
                        events = events,
                        cashiers = cashiers,
                        onCreateEvent = { event ->
                            lifecycleScope.launch {
                                db.eventDao().insertEvent(event)
                                db.auditLogDao().insertAuditLog(
                                    AuditLogEntity(
                                        actionType = "EVENT_ADD",
                                        description = "Logged event '${event.type}' total ₱${event.total}",
                                        cashierId = event.cashierId,
                                        cashierName = event.cashierName
                                    )
                                )
                            }
                        },
                        onSettleEvent = { event, cashier, note ->
                            lifecycleScope.launch {
                                val settledEvent = event.copy(
                                    isSettled = true,
                                    settlementTimestamp = System.currentTimeMillis(),
                                    settlementCashierId = cashier.id,
                                    settlementCashierName = cashier.name,
                                    settlementNote = note,
                                    updatedAt = System.currentTimeMillis()
                                )
                                db.eventDao().updateEvent(settledEvent)
                                db.auditLogDao().insertAuditLog(
                                    AuditLogEntity(
                                        actionType = if (event.type == "TASK") "TASK_COMPLETE" else "EVENT_SETTLE",
                                        description = if (event.type == "TASK") "Completed Task '${event.description}'. Note: $note" else "Settled Fund Transfer event total ₱${event.total}. Note: $note",
                                        cashierId = cashier.id,
                                        cashierName = cashier.name
                                    )
                                )
                            }
                        },
                        onDeleteEvent = { event, cashier ->
                            lifecycleScope.launch {
                                db.eventDao().deleteEvent(event)
                                db.auditLogDao().insertAuditLog(
                                    AuditLogEntity(
                                        actionType = "EVENT_DELETE",
                                        description = "DELETED event '${event.type}' (${event.description})",
                                        cashierId = cashier.id,
                                        cashierName = cashier.name
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
                                        actionType = "CUSTOMER_ADD",
                                        description = "Added customer '${customer.name}'",
                                        cashierId = cashier.id,
                                        cashierName = cashier.name
                                    )
                                )
                            }
                        },
                        onAddCredit = { customer, amount, notes, cashier ->
                            lifecycleScope.launch {
                                val newBalance = customer.balance + amount
                                db.customerDao().updateBalance(customer.id, newBalance)
                                db.creditTransactionDao().insertTransaction(
                                    CreditTransactionEntity(
                                        customerId = customer.id,
                                        type = "CREDIT_ADD",
                                        totalAmount = amount,
                                        remainingBalance = newBalance,
                                        cashierId = cashier.id,
                                        cashierName = cashier.name,
                                        notes = notes
                                    )
                                )
                                db.auditLogDao().insertAuditLog(
                                    AuditLogEntity(
                                        actionType = "CREDIT_ADD",
                                        description = "Added ₱$amount credit to '${customer.name}'. New Balance: ₱$newBalance",
                                        cashierId = cashier.id,
                                        cashierName = cashier.name
                                    )
                                )
                            }
                        },
                        onMakePayment = { customer, amount, cashier ->
                            lifecycleScope.launch {
                                val newBalance = (customer.balance - amount).coerceAtLeast(0.0)
                                db.customerDao().updateBalance(customer.id, newBalance)
                                db.creditTransactionDao().insertTransaction(
                                    CreditTransactionEntity(
                                        customerId = customer.id,
                                        type = if (newBalance == 0.0) "PAYMENT_FULL" else "PAYMENT_PARTIAL",
                                        totalAmount = amount,
                                        amountTendered = amount,
                                        remainingBalance = newBalance,
                                        cashierId = cashier.id,
                                        cashierName = cashier.name,
                                        notes = "Payment received"
                                    )
                                )
                                db.auditLogDao().insertAuditLog(
                                    AuditLogEntity(
                                        actionType = "CREDIT_PAYMENT",
                                        description = "Received ₱$amount payment from '${customer.name}'. Remaining: ₱$newBalance",
                                        cashierId = cashier.id,
                                        cashierName = cashier.name
                                    )
                                )
                            }
                        },
                        getTransactionsForCustomerFlow = { custId ->
                            db.creditTransactionDao().getTransactionsForCustomerFlow(custId)
                        }

                    )
                    TabScreen.Info -> InfoScreen(
                        securitySettings = settings,
                        priceHistory = priceHistory,
                        vendorStores = stores,
                        allProducts = products,
                        banRecords = banRecords,
                        wholesaleProducts = wholesaleProducts
                    )
                    TabScreen.Restock -> RestockScreen(
                        restockItems = restockItems,
                        allProducts = products,
                        onUpdateQuantity = { item, newQty ->
                            lifecycleScope.launch {
                                db.restockItemDao().updateRestockItem(item.copy(quantity = newQty))
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
                    TabScreen.Settings -> SettingsScreen(
                        activeDetailView = activeSettingsDetailView,
                        onActiveDetailViewChange = { activeSettingsDetailView = it },
                        securitySettings = settings,
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
                        isDarkTheme = isDarkTheme,
                        onToggleDarkTheme = onToggleDarkTheme,
                        onSaveProductGroup = { group, generatedItems, cashier ->
                            lifecycleScope.launch {
                                db.productGroupDao().insertGroup(group)
                                db.productDao().deleteProductsByGroupId(group.id)
                                if (generatedItems.isNotEmpty()) {
                                    db.productDao().insertProducts(generatedItems)
                                }
                                if (group.brand.isNotBlank()) {
                                    db.masterDataDao().insertBrand(com.example.ministoreapp.data.local.entities.BrandEntity(name = group.brand))
                                }
                                if (group.category.isNotBlank()) {
                                    db.masterDataDao().insertCategory(com.example.ministoreapp.data.local.entities.CategoryEntity(name = group.category))
                                }
                                if (group.containerType.isNotBlank()) {
                                    db.masterDataDao().insertContainer(com.example.ministoreapp.data.local.entities.ContainerEntity(name = group.containerType))
                                }
                                if (group.wholesaleUnit.isNotBlank()) {
                                    db.masterDataDao().insertWholesaleUnit(com.example.ministoreapp.data.local.entities.WholesaleUnitEntity(name = group.wholesaleUnit))
                                }
                                db.auditLogDao().insertAuditLog(
                                    AuditLogEntity(
                                        actionType = "PRODUCT_GROUP_SAVE",
                                        description = "Saved product group '${group.baseName}' (${generatedItems.size} search items)",
                                        cashierId = cashier.id,
                                        cashierName = cashier.name
                                    )
                                )
                            }
                        },
                        onDeleteSingleProduct = { product, cashier ->
                            lifecycleScope.launch {
                                db.productDao().deleteProduct(product)
                                val remainingCount = if (product.groupId != null) db.productDao().getSearchItemCountForGroup(product.groupId) else 0

                                if (remainingCount == 0 && product.groupId != null) {
                                    db.productGroupDao().deleteGroupById(product.groupId)
                                } else if (product.groupId != null) {
                                    val group = db.productGroupDao().getGroupById(product.groupId)
                                    if (group != null && product.comboKey.isNotBlank()) {
                                        val existingKeys = group.excludedKeys.split(",").map { it.trim() }.filter { it.isNotBlank() }.toMutableList()
                                        if (!existingKeys.contains(product.comboKey)) {
                                            existingKeys.add(product.comboKey)
                                            db.productGroupDao().updateGroup(group.copy(excludedKeys = existingKeys.joinToString(",")))
                                        }
                                    }
                                }

                                db.auditLogDao().insertAuditLog(
                                    AuditLogEntity(
                                        actionType = "PRODUCT_DELETE_SINGLE",
                                        description = "DELETED single product '${product.name}'",
                                        cashierId = cashier.id,
                                        cashierName = cashier.name
                                    )
                                )
                            }
                        },
                        onDeleteProductGroup = { groupId, cashier ->
                            lifecycleScope.launch {
                                db.productDao().deleteProductsByGroupId(groupId)
                                db.productGroupDao().deleteGroupById(groupId)
                                db.auditLogDao().insertAuditLog(
                                    AuditLogEntity(
                                        actionType = "PRODUCT_GROUP_DELETE",
                                        description = "DELETED product group ID: $groupId",
                                        cashierId = cashier.id,
                                        cashierName = cashier.name
                                    )
                                )
                            }
                        },
                        onSaveCashier = { cashier, performer, reason ->
                            lifecycleScope.launch {
                                db.cashierDao().insertCashier(cashier)
                                db.auditLogDao().insertAuditLog(
                                    AuditLogEntity(
                                        actionType = "CASHIER_SAVE",
                                        description = "Saved/Edited Cashier '${cashier.name}'. Performer: $performer, Reason: $reason",
                                        cashierId = cashier.id,
                                        cashierName = if (performer.isNotBlank()) performer else cashier.name
                                    )
                                )
                            }
                        },
                        onDeleteCashier = { cashier, performer, reason ->
                            lifecycleScope.launch {
                                db.cashierDao().deleteCashier(cashier)
                                db.auditLogDao().insertAuditLog(
                                    AuditLogEntity(
                                        actionType = "CASHIER_DELETE",
                                        description = "DELETED Cashier '${cashier.name}'. Performer: $performer, Reason: $reason",
                                        cashierId = cashier.id,
                                        cashierName = if (performer.isNotBlank()) performer else cashier.name
                                    )
                                )
                            }
                        },
                        onSaveBanRecord = { ban, cashier ->
                            lifecycleScope.launch {
                                db.banRecordDao().insertBanRecord(ban)
                                db.auditLogDao().insertAuditLog(
                                    AuditLogEntity(
                                        actionType = "BAN_ADD",
                                        description = "Added Ban Record for '${ban.fullName}'",
                                        cashierId = cashier.id,
                                        cashierName = cashier.name
                                    )
                                )
                            }
                        },
                        onDeleteBanRecord = { ban, cashier ->
                            lifecycleScope.launch {
                                db.banRecordDao().deleteBanRecord(ban)
                                db.auditLogDao().insertAuditLog(
                                    AuditLogEntity(
                                        actionType = "BAN_DELETE",
                                        description = "DELETED Ban Record for '${ban.fullName}'",
                                        cashierId = cashier.id,
                                        cashierName = cashier.name
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
                        }
                    )
                }
            }
        }
    }
}
