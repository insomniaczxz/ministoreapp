package com.example.ministoreapp.data.repository

import android.content.Context
import com.example.ministoreapp.data.local.AppDatabase
import com.example.ministoreapp.data.local.entities.AuditLogEntity
import com.example.ministoreapp.data.local.entities.BanRecordEntity
import com.example.ministoreapp.data.local.entities.BrandEntity
import com.example.ministoreapp.data.local.entities.CashierEntity
import com.example.ministoreapp.data.local.entities.CategoryEntity
import com.example.ministoreapp.data.local.entities.CreditTransactionEntity
import com.example.ministoreapp.data.local.entities.CustomerEntity
import com.example.ministoreapp.data.local.entities.EventEntity
import com.example.ministoreapp.data.local.entities.PriceHistoryEntity
import com.example.ministoreapp.data.local.entities.ProductEntity
import com.example.ministoreapp.data.local.entities.SecuritySettingsEntity
import com.example.ministoreapp.data.local.entities.StoreEntity
import com.example.ministoreapp.data.local.entities.UnitEntity
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DatabaseChangeset(
    val products: List<ProductEntity> = emptyList(),
    val cashiers: List<CashierEntity> = emptyList(),
    val customers: List<CustomerEntity> = emptyList(),
    val creditTransactions: List<CreditTransactionEntity> = emptyList(),
    val banRecords: List<BanRecordEntity> = emptyList(),
    val events: List<EventEntity> = emptyList(),
    val priceHistory: List<PriceHistoryEntity> = emptyList(),
    val auditLogs: List<AuditLogEntity> = emptyList(),
    val securitySettings: SecuritySettingsEntity? = null,
    val stores: List<StoreEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val brands: List<BrandEntity> = emptyList(),
    val units: List<UnitEntity> = emptyList()
)

class SyncRepository(private val db: AppDatabase) {

    private val gson = Gson()

    suspend fun exportChangesetJson(): String = withContext(Dispatchers.IO) {
        val changeset = DatabaseChangeset(
            products = db.productDao().getAllProducts(),
            cashiers = db.cashierDao().getActiveCashiers(),
            customers = db.customerDao().getAllCustomers(),
            creditTransactions = db.creditTransactionDao().getAllTransactions(),
            banRecords = db.banRecordDao().getAllBanRecords(),
            events = db.eventDao().getAllEvents(),
            priceHistory = db.priceHistoryDao().getAllPriceHistory(),
            auditLogs = db.auditLogDao().getAllAuditLogs(),
            securitySettings = db.securitySettingsDao().getSettings(),
            stores = db.masterDataDao().getStores(),
            categories = db.masterDataDao().getCategories(),
            brands = db.masterDataDao().getBrands(),
            units = db.masterDataDao().getUnits()
        )
        gson.toJson(changeset)
    }

    suspend fun importAndMergeChangeset(context: Context, jsonString: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 1. Create Pre-sync safety snapshot
            AppDatabase.createPreSyncBackup(context)

            // 2. Parse incoming changeset
            val incoming = gson.fromJson(jsonString, DatabaseChangeset::class.java) ?: return@withContext Result.failure(Exception("Invalid changeset format"))

            // 3. Merge Products (Additive + Last-Write-Wins based on updatedAt)
            val localProductsMap = db.productDao().getAllProducts().associateBy { it.id }
            val productsList: List<ProductEntity> = incoming.products ?: emptyList()
            for (item in productsList) {
                val local = localProductsMap[item.id]
                if (local == null || item.updatedAt > local.updatedAt) {
                    db.productDao().insertProduct(item)
                }
            }

            // 4. Merge Cashiers
            val localCashiersMap = db.cashierDao().getActiveCashiers().associateBy { it.id }
            val cashiersList: List<CashierEntity> = incoming.cashiers ?: emptyList()
            for (item in cashiersList) {
                val local = localCashiersMap[item.id]
                if (local == null || item.updatedAt > local.updatedAt) {
                    db.cashierDao().insertCashier(item)
                }
            }

            // 5. Merge Customers
            val localCustomersMap = db.customerDao().getAllCustomers().associateBy { it.id }
            val customersList: List<CustomerEntity> = incoming.customers ?: emptyList()
            for (item in customersList) {
                val local = localCustomersMap[item.id]
                if (local == null || item.updatedAt > local.updatedAt) {
                    db.customerDao().insertCustomer(item)
                }
            }

            // 6. Merge Credit Transactions (Additive based on UUID)
            val localTxnsMap = db.creditTransactionDao().getAllTransactions().associateBy { it.id }
            val txnsList: List<CreditTransactionEntity> = incoming.creditTransactions ?: emptyList()
            for (item in txnsList) {
                if (!localTxnsMap.containsKey(item.id)) {
                    db.creditTransactionDao().insertTransaction(item)
                }
            }

            // 7. Merge Ban Records
            val localBansMap = db.banRecordDao().getAllBanRecords().associateBy { it.id }
            val bansList: List<BanRecordEntity> = incoming.banRecords ?: emptyList()
            for (item in bansList) {
                if (!localBansMap.containsKey(item.id)) {
                    db.banRecordDao().insertBanRecord(item)
                }
            }

            // 8. Merge Events (LWW for settlement edits)
            val localEventsMap = db.eventDao().getAllEvents().associateBy { it.id }
            val eventsList: List<EventEntity> = incoming.events ?: emptyList()
            for (item in eventsList) {
                val local = localEventsMap[item.id]
                if (local == null || item.updatedAt > local.updatedAt) {
                    db.eventDao().insertEvent(item)
                }
            }

            // 9. Merge Price History
            val localPricesMap = db.priceHistoryDao().getAllPriceHistory().associateBy { it.id }
            val pricesList: List<PriceHistoryEntity> = incoming.priceHistory ?: emptyList()
            for (item in pricesList) {
                if (!localPricesMap.containsKey(item.id)) {
                    db.priceHistoryDao().insertPriceHistory(item)
                }
            }

            // 10. Merge Audit Logs
            val localLogsMap = db.auditLogDao().getAllAuditLogs().associateBy { it.id }
            val logsList: List<AuditLogEntity> = incoming.auditLogs ?: emptyList()
            for (item in logsList) {
                if (!localLogsMap.containsKey(item.id)) {
                    db.auditLogDao().insertAuditLog(item)
                }
            }

            // 11. Merge Master Data
            val localStoresMap = db.masterDataDao().getStores().associateBy { it.id }
            val storesList: List<StoreEntity> = incoming.stores ?: emptyList()
            for (item in storesList) {
                if (!localStoresMap.containsKey(item.id)) db.masterDataDao().insertStore(item)
            }

            val localCatsMap = db.masterDataDao().getCategories().associateBy { it.id }
            val catsList: List<CategoryEntity> = incoming.categories ?: emptyList()
            for (item in catsList) {
                if (!localCatsMap.containsKey(item.id)) db.masterDataDao().insertCategory(item)
            }

            val localBrandsMap = db.masterDataDao().getBrands().associateBy { it.id }
            val brandsList: List<BrandEntity> = incoming.brands ?: emptyList()
            for (item in brandsList) {
                if (!localBrandsMap.containsKey(item.id)) db.masterDataDao().insertBrand(item)
            }

            val localUnitsMap = db.masterDataDao().getUnits().associateBy { it.id }
            val unitsList: List<UnitEntity> = incoming.units ?: emptyList()
            for (item in unitsList) {
                if (!localUnitsMap.containsKey(item.id)) db.masterDataDao().insertUnit(item)
            }

            // 12. Security Settings LWW
            if (incoming.securitySettings != null) {
                val localSettings = db.securitySettingsDao().getSettings()
                if (localSettings == null || incoming.securitySettings.updatedAt > localSettings.updatedAt) {
                    db.securitySettingsDao().insertOrUpdate(incoming.securitySettings)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
