package com.example.ministoreapp.domain

import com.example.ministoreapp.data.local.entities.ProductEntity
import kotlin.math.floor

object PricingEngine {

    /**
     * Calculates selling price based on cost, markupType, markupValue, and manualSellingPrice.
     * Fixed Tiered Markup Ladder Formula:
     * Fixed Markup = floor((Cost - 1) / 5) + 3  (for Cost >= 1)
     */
    fun calculateSellingPrice(
        cost: Double,
        markupType: String,
        markupValue: Double = 0.0,
        manualSellingPrice: Double? = null,
        baseMarkup: Double = 3.0,
        bracketSize: Double = 5.0,
        markupIncrease: Double = 1.0
    ): Double {
        if (cost <= 0.0) return manualSellingPrice ?: 0.0

        return when (markupType.uppercase()) {
            "FIXED" -> {
                val bSize = if (bracketSize <= 0.0) 5.0 else bracketSize
                val fixedMarkup = if (cost >= 1.0) {
                    floor((cost - 1.0) / bSize) * markupIncrease + baseMarkup
                } else {
                    baseMarkup
                }
                cost + fixedMarkup
            }
            "PERCENTAGE" -> {
                cost + (cost * markupValue / 100.0)
            }
            "MANUAL" -> {
                manualSellingPrice ?: cost
            }
            else -> {
                cost
            }
        }
    }

    /**
     * Calculates child item cost for wholesale products:
     * Child Cost = Wholesale Parent Cost / Items per Wholesale Pack
     */
    fun calculateWholesaleChildCost(parentCost: Double, itemsPerPack: Int): Double {
        if (itemsPerPack <= 0) return parentCost
        return parentCost / itemsPerPack.toDouble()
    }

    /**
     * Price-Driven Cold/Normal Generation:
     * - If Normal Price set & Cold Price blank/<=0 -> creates Normal variant only.
     * - If Cold Price set & Normal Price blank/<=0 -> creates Cold variant only.
     * - If both are provided (>0) -> creates BOTH Normal and Cold variants.
     */
    fun generateBeverageVariants(
        parentProduct: ProductEntity,
        normalPrice: Double?,
        coldPrice: Double?,
        normalCost: Double = parentProduct.cost,
        coldCost: Double = parentProduct.cost
    ): List<ProductEntity> {
        val variants = mutableListOf<ProductEntity>()

        val hasNormal = normalPrice != null && normalPrice > 0.0
        val hasCold = coldPrice != null && coldPrice > 0.0

        if (hasNormal) {
            variants.add(
                ProductEntity(
                    name = "${parentProduct.name} Normal",
                    category = parentProduct.category,
                    brand = parentProduct.brand,
                    unit = parentProduct.unit,
                    unitSize = parentProduct.unitSize,
                    vendorName = parentProduct.vendorName,
                    cost = normalCost,
                    markupType = "MANUAL",
                    manualSellingPrice = normalPrice,
                    sellingPrice = normalPrice,

                    isBeverage = true,
                    hasVariants = false,
                    parentProductId = parentProduct.id,
                    variantType = "NORMAL"
                )
            )
        }

        if (hasCold) {
            variants.add(
                ProductEntity(
                    name = "${parentProduct.name} Cold",
                    category = parentProduct.category,
                    brand = parentProduct.brand,
                    unit = parentProduct.unit,
                    unitSize = parentProduct.unitSize,
                    vendorName = parentProduct.vendorName,
                    cost = coldCost,
                    markupType = "MANUAL",
                    manualSellingPrice = coldPrice,
                    sellingPrice = coldPrice,
                    isBeverage = true,
                    hasVariants = false,
                    parentProductId = parentProduct.id,
                    variantType = "COLD"
                )
            )
        }

        return variants
    }
}
