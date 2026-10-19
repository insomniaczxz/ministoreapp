package com.example.ministoreapp.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "product_groups")
data class ProductGroupEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val baseName: String,
    val aliases: String = "",
    val namingMode: String = "append", // "append" or "replace"
    val variants: String = "",
    val brand: String = "",
    val category: String = "",
    val vendorName: String = "", // Vendor / Supplier Name (e.g. Coke Panel)
    val unitSize: String = "",
    val containerType: String = "",
    val hasWholesale: Boolean = false,
    val wholesaleUnit: String = "",
    val wholesaleCost: Double = 0.0,
    val itemsPerPack: Int = 1,
    val wholesaleMarkupType: String = "FIXED",
    val wholesaleMarkupValue: Double = 0.0,
    val wholesaleSellingPrice: Double = 0.0,
    val showWholesaleInSearch: Boolean = true,
    val hasRetail: Boolean = true,
    val retailCost: Double = 0.0,
    val retailMarkupType: String = "FIXED",
    val retailMarkupValue: Double = 0.0,
    val retailSellingPrice: Double = 0.0,
    val bundleQty: Int = 1, // e.g. 3 pcs for ₱5
    val bundlePrice: Double = 0.0,
    val isBeverage: Boolean = false,
    val coldRetailMarkup: Double = 0.0,
    val coldCaseMarkup: Double = 0.0,
    val excludedKeys: String = "", // Comma-separated list of comboKey strings excluded/deleted
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val storeName: String get() = vendorName
}

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val groupId: String? = null, // Shared GUID/UUID pointing to ProductGroupEntity
    val comboKey: String = "", // Unique combination key (e.g. "Coca-Cola_wholesale_reg")
    val name: String,
    val aliases: String = "", // Comma-separated alias search keywords (e.g. "Coke, Softdrink")
    val category: String = "",
    val brand: String = "",
    val unit: String = "",
    val unitSize: String = "",
    val vendorName: String = "", // Supplier / Vendor Name
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
    val variantType: String? = null, // e.g. "WHOLESALE", "RETAIL", "COLD_RETAIL", "COLD_WHOLESALE", or variant tag
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val storeName: String get() = vendorName
}

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
    val storeAddress: String = "",
    val storeContactNumber: String = "",
    val storeWebsite: String = "",
    val storeSocials: String = "",
    val storeOwner: String = "",
    val securityPin: String = "526375", // 6-digit PIN
    val pinOnAppLaunchEnabled: Boolean = true,
    val pinOnSettingsOpeningEnabled: Boolean = true,
    val isDarkMode: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val nickname: String = "",
    val balance: Double = 0.0,
    val borrowedAt: Long = System.currentTimeMillis(),
    val dueDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "credit_transactions")
data class CreditTransactionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val customerId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val borrowedAt: Long = System.currentTimeMillis(),
    val dueDate: Long? = null,
    val type: String, // "CREDIT_ADD", "PAYMENT_FULL", "PAYMENT_PARTIAL"
    val totalAmount: Double,
    val amountTendered: Double = 0.0,
    val remainingBalance: Double = 0.0,
    val cashierId: String,
    val cashierName: String,
    val notes: String = "",
    val itemsJson: String = "[]",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "ban_records")
data class BanRecordEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val fullName: String,
    val nickname: String = "",
    val reason: String,
    val cashierId: String,
    val cashierName: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val type: String, // "BUY", "FUND_TRANSFER", "TASK", "INFO"
    val description: String,
    val cost: Double = 0.0,
    val quantity: Int = 1,
    val total: Double = 0.0,
    val details: String = "",
    val transferType: String? = null, // e.g. "CASH TO GCASH", "GCASH TO CASH"
    val isSettled: Boolean = false,
    val settlementCashierId: String? = null,
    val settlementCashierName: String? = null,
    val settlementNote: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val cashierId: String,
    val cashierName: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "order_items")
data class RestockItemEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val cost: Double = 0.0,
    val sellingPrice: Double = 0.0,
    val quantity: Int = 1,
    val isWholesale: Boolean = false,
    val unit: String = "",
    val unitSize: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

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
    val actionType: String,
    val description: String,
    val cashierId: String,
    val cashierName: String,
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

@Entity(tableName = "containers")
data class ContainerEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "wholesale_units")
data class WholesaleUnitEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "vendors")
data class VendorEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val ownerName: String = "",
    val productsSold: String = "",
    val address: String = "",
    val contactNumber: String = "",
    val agentName: String = "",
    val agentContactNumber: String = "",
    val isTruckPanel: Boolean = false,
    val routeScheduleType: String = "DAILY", // "DAILY" or "SPECIFIC_DAYS"
    val routeSchedule: String = "", // e.g. "Mon: Route | Wed: Route"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
