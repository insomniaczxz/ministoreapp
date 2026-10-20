package com.example.ministoreapp.data.repository

import android.content.Context
import com.example.ministoreapp.data.local.AppDatabase
import com.example.ministoreapp.data.local.entities.AuditLogEntity
import com.example.ministoreapp.data.local.entities.BanRecordEntity
import com.example.ministoreapp.data.local.entities.BrandEntity
import com.example.ministoreapp.data.local.entities.CashierEntity
import com.example.ministoreapp.data.local.entities.CategoryEntity
import com.example.ministoreapp.data.local.entities.ContainerEntity
import com.example.ministoreapp.data.local.entities.CreditTransactionEntity
import com.example.ministoreapp.data.local.entities.CustomerEntity
import com.example.ministoreapp.data.local.entities.EventEntity
import com.example.ministoreapp.data.local.entities.PriceHistoryEntity
import com.example.ministoreapp.data.local.entities.ProductEntity
import com.example.ministoreapp.data.local.entities.ProductGroupEntity
import com.example.ministoreapp.data.local.entities.RestockItemEntity
import com.example.ministoreapp.data.local.entities.SecuritySettingsEntity
import com.example.ministoreapp.data.local.entities.UnitEntity
import com.example.ministoreapp.data.local.entities.VendorEntity
import com.example.ministoreapp.data.local.entities.WholesaleUnitEntity
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DatabaseChangeset(
    val schemaVersion: Int = 3,
    val exportedAt: Long = System.currentTimeMillis(),
    val isDarkMode: Boolean = false,
    val securitySettings: SecuritySettingsEntity? = null,
    val cashiers: List<CashierEntity> = emptyList(),
    val productGroups: List<ProductGroupEntity> = emptyList(),
    val products: List<ProductEntity> = emptyList(),
    val customers: List<CustomerEntity> = emptyList(),
    val creditTransactions: List<CreditTransactionEntity> = emptyList(),
    val banRecords: List<BanRecordEntity> = emptyList(),
    val events: List<EventEntity> = emptyList(),
    val restockItems: List<RestockItemEntity> = emptyList(),
    val priceHistory: List<PriceHistoryEntity> = emptyList(),
    val auditLogs: List<AuditLogEntity> = emptyList(),
    val vendors: List<VendorEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val brands: List<BrandEntity> = emptyList(),
    val units: List<UnitEntity> = emptyList(),
    val containers: List<ContainerEntity> = emptyList(),
    val wholesaleUnits: List<WholesaleUnitEntity> = emptyList()
)

class SyncRepository(private val db: AppDatabase) {

    private val gson = Gson()

    suspend fun exportChangesetJson(context: Context): String = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences("ministore_prefs", Context.MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("dark_theme_enabled", false)
        val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
        val changeset = DatabaseChangeset(
            schemaVersion = 3,
            exportedAt = System.currentTimeMillis(),
            isDarkMode = isDarkMode,
            securitySettings = db.securitySettingsDao().getSettings(),
            cashiers = db.cashierDao().getActiveCashiers(),
            productGroups = db.productGroupDao().getAllGroups(),
            products = db.productDao().getAllProducts(),
            customers = db.customerDao().getAllCustomers(),
            creditTransactions = db.creditTransactionDao().getAllTransactions(),
            banRecords = db.banRecordDao().getAllBanRecords(),
            events = db.eventDao().getAllEvents().filter { it.timestamp >= sevenDaysAgo },
            restockItems = db.restockItemDao().getRestockItems(),
            priceHistory = db.priceHistoryDao().getAllPriceHistory().filter { it.timestamp >= sevenDaysAgo },
            auditLogs = db.auditLogDao().getAllAuditLogs().filter { it.timestamp >= sevenDaysAgo },
            vendors = db.masterDataDao().getVendors(),
            categories = db.masterDataDao().getCategories(),
            brands = db.masterDataDao().getBrands(),
            units = db.masterDataDao().getUnits(),
            containers = db.masterDataDao().getContainers(),
            wholesaleUnits = db.masterDataDao().getWholesaleUnits()
        )
        gson.toJson(changeset)
    }

    suspend fun importAndMergeChangeset(context: Context, jsonString: String, forceOverwrite: Boolean = false): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 1. Create Pre-sync safety snapshot
            AppDatabase.createPreSyncBackup(context)

            // 2. Parse incoming changeset
            val incoming = gson.fromJson(jsonString, DatabaseChangeset::class.java) ?: return@withContext Result.failure(Exception("Invalid changeset format"))

            // 3. Merge Security Settings & Store Profile
            val prefs = context.getSharedPreferences("ministore_prefs", Context.MODE_PRIVATE)

            if (incoming.securitySettings != null) {
                val localSettings = db.securitySettingsDao().getSettings()
                if (localSettings == null || forceOverwrite || incoming.securitySettings.updatedAt >= localSettings.updatedAt) {
                    val settingsToSave = if (forceOverwrite) {
                        incoming.securitySettings.copy(isDarkMode = incoming.isDarkMode, updatedAt = System.currentTimeMillis())
                    } else {
                        incoming.securitySettings.copy(isDarkMode = incoming.isDarkMode)
                    }
                    db.securitySettingsDao().insertOrUpdate(settingsToSave)
                    prefs.edit().putBoolean("dark_theme_enabled", incoming.isDarkMode).apply()
                }
            } else {
                prefs.edit().putBoolean("dark_theme_enabled", incoming.isDarkMode).apply()
            }

            // 5. Merge Cashiers & PINs
            val cashiersList: List<CashierEntity> = incoming.cashiers
            for (item in cashiersList) {
                db.cashierDao().insertCashier(if (forceOverwrite) item.copy(updatedAt = System.currentTimeMillis()) else item)
            }

            // 6. Merge Product Groups
            val localGroupMap = db.productGroupDao().getAllGroups().associateBy { it.id }
            val groupsList: List<ProductGroupEntity> = incoming.productGroups
            for (group in groupsList) {
                val local = localGroupMap[group.id]
                if (local == null || forceOverwrite || group.updatedAt >= local.updatedAt) {
                    db.productGroupDao().insertGroup(if (forceOverwrite) group.copy(updatedAt = System.currentTimeMillis()) else group)
                }
            }

            // 7. Merge Products
            val localProductsMap = db.productDao().getAllProducts().associateBy { it.id }
            val productsList: List<ProductEntity> = incoming.products
            for (item in productsList) {
                val local = localProductsMap[item.id]
                if (local == null || forceOverwrite || item.updatedAt >= local.updatedAt) {
                    db.productDao().insertProduct(if (forceOverwrite) item.copy(updatedAt = System.currentTimeMillis()) else item)
                }
            }

            // 8. Merge Customers
            val localCustomersMap = db.customerDao().getAllCustomers().associateBy { it.id }
            val customersList: List<CustomerEntity> = incoming.customers
            for (item in customersList) {
                val local = localCustomersMap[item.id]
                if (local == null || forceOverwrite || item.updatedAt >= local.updatedAt) {
                    db.customerDao().insertCustomer(if (forceOverwrite) item.copy(updatedAt = System.currentTimeMillis()) else item)
                }
            }

            // 9. Merge Credit Transactions
            val localTxnsMap = db.creditTransactionDao().getAllTransactions().associateBy { it.id }
            val txnsList: List<CreditTransactionEntity> = incoming.creditTransactions
            for (item in txnsList) {
                if (!localTxnsMap.containsKey(item.id) || forceOverwrite) {
                    db.creditTransactionDao().insertTransaction(item) // txns are immutable anyway, but we can just insert them
                }
            }

            // 10. Merge Ban Records
            val bansList: List<BanRecordEntity> = incoming.banRecords
            for (item in bansList) {
                db.banRecordDao().insertBanRecord(item)
            }

            // 11. Merge Events
            val localEventsMap = db.eventDao().getAllEvents().associateBy { it.id }
            val eventsList: List<EventEntity> = incoming.events
            for (item in eventsList) {
                val local = localEventsMap[item.id]
                if (local == null || forceOverwrite || item.updatedAt >= local.updatedAt) {
                    db.eventDao().insertEvent(if (forceOverwrite) item.copy(updatedAt = System.currentTimeMillis()) else item)
                }
            }

            // 12. Merge Restock/Order Items
            val localRestockMap = db.restockItemDao().getRestockItems().associateBy { it.id }
            val restockList: List<RestockItemEntity> = incoming.restockItems
            for (item in restockList) {
                val local = localRestockMap[item.id]
                if (local == null || forceOverwrite || item.updatedAt >= local.updatedAt) {
                    db.restockItemDao().insertRestockItem(if (forceOverwrite) item.copy(updatedAt = System.currentTimeMillis()) else item)
                }
            }

            // 13. Merge Price History
            val localPricesMap = db.priceHistoryDao().getAllPriceHistory().associateBy { it.id }
            val pricesList: List<PriceHistoryEntity> = incoming.priceHistory
            for (item in pricesList) {
                if (!localPricesMap.containsKey(item.id)) {
                    db.priceHistoryDao().insertPriceHistory(item)
                }
            }

            // 14. Merge Audit Logs
            val localLogsMap = db.auditLogDao().getAllAuditLogs().associateBy { it.id }
            val logsList: List<AuditLogEntity> = incoming.auditLogs
            for (item in logsList) {
                if (!localLogsMap.containsKey(item.id)) {
                    db.auditLogDao().insertAuditLog(item)
                }
            }

            // 15. Merge Dropdowns & Master Data
            val vendorsList: List<VendorEntity> = incoming.vendors
            for (item in vendorsList) db.masterDataDao().insertVendor(item)

            val catsList: List<CategoryEntity> = incoming.categories
            for (item in catsList) db.masterDataDao().insertCategory(item)

            val brandsList: List<BrandEntity> = incoming.brands
            for (item in brandsList) db.masterDataDao().insertBrand(item)

            val unitsList: List<UnitEntity> = incoming.units
            for (item in unitsList) db.masterDataDao().insertUnit(item)

            val containersList: List<ContainerEntity> = incoming.containers
            for (item in containersList) db.masterDataDao().insertContainer(item)

            val wholesaleUnitsList: List<WholesaleUnitEntity> = incoming.wholesaleUnits
            for (item in wholesaleUnitsList) db.masterDataDao().insertWholesaleUnit(item)

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
