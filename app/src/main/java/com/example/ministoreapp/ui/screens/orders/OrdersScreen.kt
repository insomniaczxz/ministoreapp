package com.example.ministoreapp.ui.screens.orders

import androidx.compose.runtime.Composable
import com.example.ministoreapp.data.local.entities.ProductEntity
import com.example.ministoreapp.data.local.entities.RestockItemEntity
import com.example.ministoreapp.ui.screens.restock.RestockScreen

@Composable
fun OrdersScreen(
    orderItems: List<RestockItemEntity>,
    allProducts: List<ProductEntity>,
    onUpdateQuantity: (RestockItemEntity, Int) -> Unit,
    onRemoveItem: (RestockItemEntity) -> Unit,
    onClearOrders: () -> Unit
) {
    RestockScreen(
        restockItems = orderItems,
        allProducts = allProducts,
        onUpdateQuantity = onUpdateQuantity,
        onRemoveItem = onRemoveItem,
        onClearRestock = onClearOrders
    )
}
