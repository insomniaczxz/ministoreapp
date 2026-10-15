package com.example.ministoreapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.ministoreapp.data.local.entities.AuditLogEntity
import com.example.ministoreapp.data.local.entities.BanRecordEntity
import com.example.ministoreapp.data.local.entities.BrandEntity
import com.example.ministoreapp.data.local.entities.CashierEntity
import com.example.ministoreapp.data.local.entities.CategoryEntity
import com.example.ministoreapp.data.local.entities.CreditTransactionEntity
import com.example.ministoreapp.data.local.entities.CustomerEntity
import com.example.ministoreapp.data.local.entities.EventEntity
import com.example.ministoreapp.data.local.entities.RestockItemEntity
import com.example.ministoreapp.data.local.entities.PriceHistoryEntity
import com.example.ministoreapp.data.local.entities.ProductEntity
import com.example.ministoreapp.data.local.entities.SecuritySettingsEntity
import com.example.ministoreapp.data.local.entities.StoreEntity
import com.example.ministoreapp.data.local.entities.UnitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProductsFlow(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products")
    suspend fun getAllProducts(): List<ProductEntity>

    // Variant Display Rule: If a product has variants (hasVariants == 1), hide parent product from search display and show ONLY child variants.
    @Query("""
        SELECT * FROM products 
        WHERE (hasVariants = 0 OR hasVariants IS NULL) 
        AND (:query = '' OR name LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' OR brand LIKE '%' || :query || '%' OR (:query = 'Wholesale' AND (isWholesaleParent = 1 OR parentProductId IN (SELECT id FROM products WHERE isWholesaleParent = 1))))
        ORDER BY name ASC
    """)
    fun getSearchProductsFlow(query: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE isWholesaleParent = 1 OR parentProductId IN (SELECT id FROM products WHERE isWholesaleParent = 1) ORDER BY name ASC")
    fun getWholesaleProductsFlow(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: String): ProductEntity?

    @Query("SELECT * FROM products WHERE parentProductId = :parentId ORDER BY name ASC")
    suspend fun getChildVariants(parentId: String): List<ProductEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Delete
    suspend fun deleteProduct(product: ProductEntity)

    @Query("DELETE FROM products WHERE id = :id OR parentProductId = :id")
    suspend fun deleteProductAndVariants(id: String)
}

@Dao
interface CashierDao {
    @Query("SELECT * FROM cashiers WHERE isActive = 1 ORDER BY name ASC")
    fun getAllCashiersFlow(): Flow<List<CashierEntity>>

    @Query("SELECT * FROM cashiers WHERE isActive = 1 ORDER BY name ASC")
    suspend fun getActiveCashiers(): List<CashierEntity>

    @Query("SELECT * FROM cashiers WHERE id = :id")
    suspend fun getCashierById(id: String): CashierEntity?

    @Query("SELECT COUNT(*) FROM cashiers WHERE isActive = 1")
    suspend fun getActiveCashierCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCashier(cashier: CashierEntity)

    @Update
    suspend fun updateCashier(cashier: CashierEntity)

    @Delete
    suspend fun deleteCashier(cashier: CashierEntity)
}

@Dao
interface SecuritySettingsDao {
    @Query("SELECT * FROM security_settings WHERE id = 'default'")
    fun getSettingsFlow(): Flow<SecuritySettingsEntity?>

    @Query("SELECT * FROM security_settings WHERE id = 'default'")
    suspend fun getSettings(): SecuritySettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: SecuritySettingsEntity)
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomersFlow(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers ORDER BY name ASC")
    suspend fun getAllCustomers(): List<CustomerEntity>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomerById(id: String): CustomerEntity?


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity)

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Delete
    suspend fun deleteCustomer(customer: CustomerEntity)

    @Query("UPDATE customers SET balance = :newBalance, updatedAt = :updatedAt WHERE id = :customerId")
    suspend fun updateBalance(customerId: String, newBalance: Double, updatedAt: Long = System.currentTimeMillis())
}

@Dao
interface CreditTransactionDao {
    @Query("SELECT * FROM credit_transactions WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun getTransactionsForCustomerFlow(customerId: String): Flow<List<CreditTransactionEntity>>

    @Query("SELECT * FROM credit_transactions ORDER BY timestamp DESC")
    suspend fun getAllTransactions(): List<CreditTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: CreditTransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: CreditTransactionEntity)
}

@Dao
interface BanRecordDao {
    @Query("SELECT * FROM ban_records ORDER BY fullName ASC")
    fun getAllBanRecordsFlow(): Flow<List<BanRecordEntity>>

    @Query("SELECT * FROM ban_records ORDER BY fullName ASC")
    suspend fun getAllBanRecords(): List<BanRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBanRecord(banRecord: BanRecordEntity)

    @Delete
    suspend fun deleteBanRecord(banRecord: BanRecordEntity)
}

@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY timestamp DESC")
    fun getAllEventsFlow(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events ORDER BY timestamp DESC")
    suspend fun getAllEvents(): List<EventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EventEntity)

    @Update
    suspend fun updateEvent(event: EventEntity)

    @Delete
    suspend fun deleteEvent(event: EventEntity)
}

@Dao
interface RestockItemDao {
    @Query("SELECT * FROM order_items ORDER BY createdAt ASC")
    fun getRestockItemsFlow(): Flow<List<RestockItemEntity>>

    @Query("SELECT * FROM order_items ORDER BY createdAt ASC")
    suspend fun getRestockItems(): List<RestockItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRestockItem(item: RestockItemEntity)

    @Update
    suspend fun updateRestockItem(item: RestockItemEntity)

    @Delete
    suspend fun deleteRestockItem(item: RestockItemEntity)

    @Query("DELETE FROM order_items")
    suspend fun clearRestockItems()

    // Aliases for backwards compatibility
    @Query("SELECT * FROM order_items ORDER BY createdAt ASC")
    fun getOrdersFlow(): Flow<List<RestockItemEntity>> = getRestockItemsFlow()

    @Query("SELECT * FROM order_items ORDER BY createdAt ASC")
    suspend fun getOrders(): List<RestockItemEntity> = getRestockItems()

    suspend fun insertOrderItem(item: RestockItemEntity) = insertRestockItem(item)

    suspend fun updateOrderItem(item: RestockItemEntity) = updateRestockItem(item)

    suspend fun deleteOrderItem(item: RestockItemEntity) = deleteRestockItem(item)

    suspend fun clearOrders() = clearRestockItems()
}

@Dao
interface PriceHistoryDao {
    @Query("SELECT * FROM price_history WHERE timestamp >= :sevenDaysAgo ORDER BY timestamp DESC")
    fun getRecentPriceChangesFlow(sevenDaysAgo: Long): Flow<List<PriceHistoryEntity>>

    @Query("SELECT * FROM price_history ORDER BY timestamp DESC")
    suspend fun getAllPriceHistory(): List<PriceHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPriceHistory(priceHistory: PriceHistoryEntity)
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun getAllAuditLogsFlow(): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    suspend fun getAllAuditLogs(): List<AuditLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntity)
}

@Dao
interface MasterDataDao {
    @Query("SELECT * FROM stores ORDER BY name ASC")
    fun getStoresFlow(): Flow<List<StoreEntity>>
    @Query("SELECT * FROM stores ORDER BY name ASC")
    suspend fun getStores(): List<StoreEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStore(store: StoreEntity)
    @Delete
    suspend fun deleteStore(store: StoreEntity)

    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getCategoriesFlow(): Flow<List<CategoryEntity>>
    @Query("SELECT * FROM categories ORDER BY name ASC")
    suspend fun getCategories(): List<CategoryEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)
    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Query("SELECT * FROM brands ORDER BY name ASC")
    fun getBrandsFlow(): Flow<List<BrandEntity>>
    @Query("SELECT * FROM brands ORDER BY name ASC")
    suspend fun getBrands(): List<BrandEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBrand(brand: BrandEntity)
    @Delete
    suspend fun deleteBrand(brand: BrandEntity)

    @Query("SELECT * FROM units ORDER BY name ASC")
    fun getUnitsFlow(): Flow<List<UnitEntity>>
    @Query("SELECT * FROM units ORDER BY name ASC")
    suspend fun getUnits(): List<UnitEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnit(unit: UnitEntity)
    @Delete
    suspend fun deleteUnit(unit: UnitEntity)
}
