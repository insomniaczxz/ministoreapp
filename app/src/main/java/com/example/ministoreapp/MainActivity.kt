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
            MaterialTheme {
                MainAppContent()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainAppContent() {
        var currentTab by remember { mutableStateOf<TabScreen>(TabScreen.Search) }

        // Live States from Database Flows
        val securitySettings by db.securitySettingsDao().getSettingsFlow().collectAsState(initial = null)
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

        // App Launch Guard State
        var isAppUnlocked by remember { mutableStateOf(false) }
        var showSettingsPinPrompt by remember { mutableStateOf(false) }

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
                    currentTab = TabScreen.Settings
                }
            )
        }

        Scaffold(
            topBar = {
                GlobalHeader(
                    storeName = settings.storeName,
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
                        securitySettings = settings,
                        products = products,
                        cashiers = cashiers,
                        auditLogs = auditLogs,
                        banRecords = banRecords,
                        stores = stores,
                        categories = categories,
                        brands = brands,
                        units = units,
                        onSaveProduct = { parentProduct, cashier, variants ->
                            lifecycleScope.launch {
                                val existing = db.productDao().getProductById(parentProduct.id)
                                if (existing != null && existing.sellingPrice != parentProduct.sellingPrice) {
                                    db.priceHistoryDao().insertPriceHistory(
                                        PriceHistoryEntity(
                                            productId = parentProduct.id,
                                            productName = parentProduct.name,
                                            oldPrice = existing.sellingPrice,
                                            newPrice = parentProduct.sellingPrice,
                                            cashierId = cashier.id,
                                            cashierName = cashier.name
                                        )
                                    )
                                }
                                db.productDao().insertProduct(parentProduct)
                                if (variants.isNotEmpty()) {
                                    db.productDao().insertProducts(variants)
                                }
                                db.auditLogDao().insertAuditLog(
                                    AuditLogEntity(
                                        actionType = if (existing == null) "PRODUCT_ADD" else "PRODUCT_EDIT",
                                        description = "Saved product '${parentProduct.name}' price ₱${parentProduct.sellingPrice}",
                                        cashierId = cashier.id,
                                        cashierName = cashier.name
                                    )
                                )
                            }
                        },
                        onDeleteProduct = { product, cashier ->
                            lifecycleScope.launch {
                                db.productDao().deleteProductAndVariants(product.id)
                                db.auditLogDao().insertAuditLog(
                                    AuditLogEntity(
                                        actionType = "PRODUCT_DELETE",
                                        description = "DELETED product '${product.name}' (ID: ${product.id})",
                                        cashierId = cashier.id,
                                        cashierName = cashier.name
                                    )
                                )
                            }
                        },
                        onSaveCashier = { cashier ->
                            lifecycleScope.launch {
                                db.cashierDao().insertCashier(cashier)
                                db.auditLogDao().insertAuditLog(
                                    AuditLogEntity(
                                        actionType = "CASHIER_ADD",
                                        description = "Added/Updated Cashier '${cashier.name}'",
                                        cashierId = cashier.id,
                                        cashierName = cashier.name
                                    )
                                )
                            }
                        },
                        onDeleteCashier = { cashier ->
                            lifecycleScope.launch {
                                db.cashierDao().deleteCashier(cashier)
                                db.auditLogDao().insertAuditLog(
                                    AuditLogEntity(
                                        actionType = "CASHIER_DELETE",
                                        description = "DELETED Cashier '${cashier.name}'",
                                        cashierId = cashier.id,
                                        cashierName = cashier.name
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
                                }
                            }
                        },
                        onUpdateSecuritySettings = { updatedSettings ->
                            lifecycleScope.launch {
                                db.securitySettingsDao().insertOrUpdate(updatedSettings)
                            }
                        },
                        onUpdateSecurityPin = { oldPin, newPin ->
                            if (oldPin == settings.securityPin) {
                                lifecycleScope.launch {
                                    db.securitySettingsDao().insertOrUpdate(settings.copy(securityPin = newPin))
                                }
                                true
                            } else {
                                false
                            }
                        }
                    )
                }
            }
        }
    }
}
