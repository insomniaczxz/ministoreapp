package com.example.ministoreapp.domain

import com.example.ministoreapp.data.local.entities.ProductEntity
import com.example.ministoreapp.data.local.entities.RestockItemEntity

data class StoreRecommendationResult(
    val recommendedStoreName: String,
    val totalCost: Double,
    val storeCosts: Map<String, Double> // storeName -> totalCost
)

object RecommendationEngine {

    /**
     * Analyzes active restock list and historical product vendor prices to recommend
     * which store offers the lowest combined cost.
     */
    fun recommendStore(
        restockItems: List<RestockItemEntity>,
        allProducts: List<ProductEntity>
    ): StoreRecommendationResult? {
        if (restockItems.isEmpty()) return null

        // Group products by non-empty storeName
        val storeProductsMap = allProducts
            .filter { it.storeName.isNotBlank() }
            .groupBy { it.storeName }

        if (storeProductsMap.isEmpty()) {
            // Fallback to default costs in restock items
            val defaultTotal = restockItems.sumOf { it.cost * it.quantity }
            val primaryStore = restockItems.map { it.storeName }.firstOrNull { it.isNotBlank() } ?: "Default Store"
            return StoreRecommendationResult(
                recommendedStoreName = primaryStore,
                totalCost = defaultTotal,
                storeCosts = mapOf(primaryStore to defaultTotal)
            )
        }

        val storeTotals = mutableMapOf<String, Double>()

        for ((storeName, storeProducts) in storeProductsMap) {
            var storeTotalCost = 0.0
            for (item in restockItems) {
                // Find matching product in this store
                val matchInStore = storeProducts.find { 
                    (item.productId != null && it.id == item.productId) ||
                    it.name.equals(item.productName, ignoreCase = true)
                }
                val unitCost = matchInStore?.cost ?: item.cost
                storeTotalCost += unitCost * item.quantity
            }
            storeTotals[storeName] = storeTotalCost
        }

        val bestStoreEntry = storeTotals.minByOrNull { it.value } ?: return null

        return StoreRecommendationResult(
            recommendedStoreName = bestStoreEntry.key,
            totalCost = bestStoreEntry.value,
            storeCosts = storeTotals
        )
    }
}
