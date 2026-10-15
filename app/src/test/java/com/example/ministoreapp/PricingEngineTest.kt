package com.example.ministoreapp

import com.example.ministoreapp.data.local.entities.ProductEntity
import com.example.ministoreapp.domain.PricingEngine
import org.junit.Assert.assertEquals
import org.junit.Test

class PricingEngineTest {

    @Test
    fun testFixedTieredMarkupLadderFormula() {
        // Cost 1-5 => +3 markup
        assertEquals(4.0, PricingEngine.calculateSellingPrice(cost = 1.0, markupType = "FIXED"), 0.001)
        assertEquals(8.0, PricingEngine.calculateSellingPrice(cost = 5.0, markupType = "FIXED"), 0.001)

        // Cost 6-10 => +4 markup
        assertEquals(10.0, PricingEngine.calculateSellingPrice(cost = 6.0, markupType = "FIXED"), 0.001)
        assertEquals(14.0, PricingEngine.calculateSellingPrice(cost = 10.0, markupType = "FIXED"), 0.001)

        // Cost 11-15 => +5 markup
        assertEquals(16.0, PricingEngine.calculateSellingPrice(cost = 11.0, markupType = "FIXED"), 0.001)
        assertEquals(20.0, PricingEngine.calculateSellingPrice(cost = 15.0, markupType = "FIXED"), 0.001)

        // Cost 20 => +6 markup
        assertEquals(26.0, PricingEngine.calculateSellingPrice(cost = 20.0, markupType = "FIXED"), 0.001)
    }

    @Test
    fun testPercentageMarkup() {
        val sellingPrice = PricingEngine.calculateSellingPrice(
            cost = 100.0,
            markupType = "PERCENTAGE",
            markupValue = 15.0
        )
        assertEquals(115.0, sellingPrice, 0.001)
    }

    @Test
    fun testManualPriceOverride() {
        val sellingPrice = PricingEngine.calculateSellingPrice(
            cost = 50.0,
            markupType = "MANUAL",
            manualSellingPrice = 75.0
        )
        assertEquals(75.0, sellingPrice, 0.001)
    }

    @Test
    fun testWholesaleChildCostFormula() {
        val childCost = PricingEngine.calculateWholesaleChildCost(parentCost = 240.0, itemsPerPack = 24)
        assertEquals(10.0, childCost, 0.001)
    }

    @Test
    fun testBeverageVariantsGeneration_BothNormalAndCold() {
        val parent = ProductEntity(name = "Iced Tea Juice", cost = 20.0, isBeverage = true)
        val variants = PricingEngine.generateBeverageVariants(
            parentProduct = parent,
            normalPrice = 25.0,
            coldPrice = 30.0
        )

        assertEquals(2, variants.size)
        assertEquals("Iced Tea Juice Normal", variants[0].name)
        assertEquals(25.0, variants[0].sellingPrice, 0.001)
        assertEquals("NORMAL", variants[0].variantType)

        assertEquals("Iced Tea Juice Cold", variants[1].name)
        assertEquals(30.0, variants[1].sellingPrice, 0.001)
        assertEquals("COLD", variants[1].variantType)
    }

    @Test
    fun testBeverageVariantsGeneration_ColdOnly() {
        val parent = ProductEntity(name = "Soda", cost = 15.0, isBeverage = true)
        val variants = PricingEngine.generateBeverageVariants(
            parentProduct = parent,
            normalPrice = null,
            coldPrice = 22.0
        )

        assertEquals(1, variants.size)
        assertEquals("Soda Cold", variants[0].name)
        assertEquals(22.0, variants[0].sellingPrice, 0.001)
        assertEquals("COLD", variants[0].variantType)
    }
}
