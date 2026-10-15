package com.example.ministoreapp.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: String = "",
    val brand: String = "",
    val unit: String = "",
    val unitSize: String = "",
    val storeName: String = "",
    val cost: Double = 0.0,
    val markupType: String = "FIXED", // "PERCENTAGE", "FIXED", "MANUAL"
    val markupValue: Double = 0.0,
    val manualSellingPrice: Double? = null,
    val sellingPrice: Double = 0.0,
    val isBeverage: Boolean = false,
    val hasVariants: Boolean = false,
    val parentProductId: String? = null,
    val isWholesaleParent: Boolean = false,
    val itemsPerWholesalePack: Int? = null,
    val variantType: String? = null, // e.g. "NORMAL", "COLD", or custom variant name
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cashiers")
data class CashierEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val pin: String, // 4-digit PIN
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "security_settings")
data class SecuritySettingsEntity(
    @PrimaryKey val id: String = "default",
    val storeName: String = "StoreName Sari-Sari and Wholesale",
    val securityPin: String = "526375", // 6-digit PIN
    val pinOnAppLaunchEnabled: Boolean = true,
    val pinOnSettingsOpeningEnabled: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val nickname: String = "",
    val balance: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "credit_transactions")
data class CreditTransactionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val customerId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String, // "CREDIT_ADD", "PAYMENT_FULL", "PAYMENT_PARTIAL"
    val totalAmount: Double,
    val amountTendered: Double = 0.0,
    val remainingBalance: Double = 0.0,
    val cashierId: String,
    val cashierName: String,
    val notes: String = "",
    val itemsJson: String = "[]", // JSON array of items added or manual product entry
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "ban_records")
data class BanRecordEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val fullName: String,
    val nickname: String = "",
    val reason: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val cashierId: String,
    val cashierName: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val type: String, // "BUY", "INFO", "FUND_TRANSFER"
    val description: String = "",
    val cost: Double = 0.0,
    val quantity: Int = 1,
    val total: Double = 0.0,
    val details: String = "",
    val transferType: String? = null, // "GCash to Store Cash" or "Store Cash to GCash"
    val isSettled: Boolean = false,
    val settlementTimestamp: Long? = null,
    val settlementCashierId: String? = null,
    val settlementCashierName: String? = null,
    val settlementNote: String? = null,
    val cashierId: String,
    val cashierName: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "order_items")
data class RestockItemEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val productId: String? = null,
    val productName: String,
    val cost: Double = 0.0,
    val sellingPrice: Double = 0.0,
    val quantity: Int = 1,
    val storeName: String = "",
    val unit: String = "",
    val unitSize: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

// Backwards compatibility alias
typealias OrderItemEntity = RestockItemEntity

@Entity(tableName = "price_history")
data class PriceHistoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val productId: String,
    val productName: String,
    val oldPrice: Double,
    val newPrice: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val cashierId: String,
    val cashierName: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val actionType: String, // "PRODUCT_ADD", "PRODUCT_EDIT", "PRODUCT_DELETE", "CUSTOMER_ADD", "CUSTOMER_DELETE", "CREDIT_ADD", "CREDIT_PAYMENT", "EVENT_ADD", "EVENT_DELETE", "EVENT_SETTLE", "CASHIER_ADD", "CASHIER_EDIT", "CASHIER_DELETE", "PRICE_EDIT", "SETTINGS_EDIT"
    val description: String,
    val cashierId: String,
    val cashierName: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "stores")
data class StoreEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "brands")
data class BrandEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "units")
data class UnitEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
