package com.example.ministoreapp

import com.example.ministoreapp.data.local.entities.OrderItemEntity
import com.example.ministoreapp.data.local.entities.ProductEntity
import com.example.ministoreapp.domain.RecommendationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class RecommendationEngineTest {

    @Test
    fun testStoreRecommendation_SelectsLowestTotalCostStore() {
        val orderItems = listOf(
            OrderItemEntity(productName = "Rice 1kg", cost = 50.0, quantity = 2),
            OrderItemEntity(productName = "Cooking Oil 1L", cost = 80.0, quantity = 1)
        )

        // Store A prices: Rice = 48, Oil = 75 => Total = 48*2 + 75 = 171
        // Store B prices: Rice = 52, Oil = 70 => Total = 52*2 + 70 = 174
        val allProducts = listOf(
            ProductEntity(name = "Rice 1kg", cost = 48.0, storeName = "Store A"),
            ProductEntity(name = "Cooking Oil 1L", cost = 75.0, storeName = "Store A"),
            ProductEntity(name = "Rice 1kg", cost = 52.0, storeName = "Store B"),
            ProductEntity(name = "Cooking Oil 1L", cost = 70.0, storeName = "Store B")
        )

        val result = RecommendationEngine.recommendStore(orderItems, allProducts)

        assertNotNull(result)
        assertEquals("Store A", result?.recommendedStoreName)
        assertEquals(171.0, result?.totalCost ?: 0.0, 0.001)
    }
}
