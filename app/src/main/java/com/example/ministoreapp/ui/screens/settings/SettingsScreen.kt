package com.example.ministoreapp.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ministoreapp.data.local.entities.AuditLogEntity
import com.example.ministoreapp.data.local.entities.BanRecordEntity
import com.example.ministoreapp.data.local.entities.BrandEntity
import com.example.ministoreapp.data.local.entities.CashierEntity
import com.example.ministoreapp.data.local.entities.CategoryEntity
import com.example.ministoreapp.data.local.entities.ProductEntity
import com.example.ministoreapp.data.local.entities.SecuritySettingsEntity
import com.example.ministoreapp.data.local.entities.StoreEntity
import com.example.ministoreapp.data.local.entities.UnitEntity
import com.example.ministoreapp.domain.PricingEngine
import com.example.ministoreapp.ui.components.CashierVerificationDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    securitySettings: SecuritySettingsEntity?,
    products: List<ProductEntity>,
    cashiers: List<CashierEntity>,
    auditLogs: List<AuditLogEntity>,
    banRecords: List<BanRecordEntity>,
    stores: List<StoreEntity>,
    categories: List<CategoryEntity>,
    brands: List<BrandEntity>,
    units: List<UnitEntity>,
    onSaveProduct: (ProductEntity, CashierEntity, List<ProductEntity>) -> Unit,
    onDeleteProduct: (ProductEntity, CashierEntity) -> Unit,
    onSaveCashier: (CashierEntity) -> Unit,
    onDeleteCashier: (CashierEntity) -> Unit,
    onSaveBanRecord: (BanRecordEntity, CashierEntity) -> Unit,
    onDeleteBanRecord: (BanRecordEntity, CashierEntity) -> Unit,
    onSaveMasterData: (type: String, name: String) -> Unit,
    onDeleteMasterData: (type: String, id: String) -> Unit,
    onUpdateSecuritySettings: (SecuritySettingsEntity) -> Unit,
    onUpdateSecurityPin: (oldPin: String, newPin: String) -> Boolean
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Products", "Cashiers", "Master Data", "Security", "Audit Logs")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 0.dp) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontWeight = FontWeight.SemiBold) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedTab) {
            0 -> ProductsManagementTab(
                products = products,
                cashiers = cashiers,
                stores = stores,
                categories = categories,
                brands = brands,
                units = units,
                onSaveProduct = onSaveProduct,
                onDeleteProduct = onDeleteProduct
            )
            1 -> CashiersManagementTab(
                cashiers = cashiers,
                onSaveCashier = onSaveCashier,
                onDeleteCashier = onDeleteCashier
            )
            2 -> MasterDataManagementTab(
                stores = stores,
                categories = categories,
                brands = brands,
                units = units,
                banRecords = banRecords,
                cashiers = cashiers,
                onSaveMasterData = onSaveMasterData,
                onDeleteMasterData = onDeleteMasterData,
                onSaveBanRecord = onSaveBanRecord,
                onDeleteBanRecord = onDeleteBanRecord
            )
            3 -> SecurityManagementTab(
                securitySettings = securitySettings,
                onUpdateSecuritySettings = onUpdateSecuritySettings,
                onUpdateSecurityPin = onUpdateSecurityPin
            )
            4 -> AuditLogsTab(auditLogs = auditLogs)
        }
    }
}

@Composable
fun ProductsManagementTab(
    products: List<ProductEntity>,
    cashiers: List<CashierEntity>,
    stores: List<StoreEntity>,
    categories: List<CategoryEntity>,
    brands: List<BrandEntity>,
    units: List<UnitEntity>,
    onSaveProduct: (ProductEntity, CashierEntity, List<ProductEntity>) -> Unit,
    onDeleteProduct: (ProductEntity, CashierEntity) -> Unit
) {
    var showAddStandardDialog by remember { mutableStateOf(false) }
    var showAddWholesaleDialog by remember { mutableStateOf(false) }
    var productToDelete by remember { mutableStateOf<ProductEntity?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { showAddStandardDialog = true },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Standard", fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { showAddWholesaleDialog = true },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Wholesale", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(products, key = { it.id }) { product ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                text = "Cost: ₱${"%.2f".format(product.cost)} | Selling: ₱${"%.2f".format(product.sellingPrice)} (${product.markupType})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (product.isWholesaleParent) {
                                Text(
                                    text = "Wholesale Pack: ${product.itemsPerWholesalePack ?: 1} items",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                        IconButton(onClick = { productToDelete = product }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    if (showAddStandardDialog) {
        AddStandardProductDialog(
            cashiers = cashiers,
            stores = stores,
            categories = categories,
            brands = brands,
            units = units,
            onDismiss = { showAddStandardDialog = false },
            onSave = { prod, cashier, variants ->
                onSaveProduct(prod, cashier, variants)
                showAddStandardDialog = false
            }
        )
    }

    if (showAddWholesaleDialog) {
        AddWholesaleProductDialog(
            cashiers = cashiers,
            stores = stores,
            categories = categories,
            brands = brands,
            units = units,
            onDismiss = { showAddWholesaleDialog = false },
            onSave = { prod, cashier, variants ->
                onSaveProduct(prod, cashier, variants)
                showAddWholesaleDialog = false
            }
        )
    }

    productToDelete?.let { prod ->
        CashierVerificationDialog(
            cashiers = cashiers,
            actionTitle = "Verify Cashier to Delete Product '${prod.name}'",
            onDismiss = { productToDelete = null },
            onVerified = { cashier ->
                onDeleteProduct(prod, cashier)
                productToDelete = null
            }
        )
    }
}

@Composable
fun AddStandardProductDialog(
    cashiers: List<CashierEntity>,
    stores: List<StoreEntity>,
    categories: List<CategoryEntity>,
    brands: List<BrandEntity>,
    units: List<UnitEntity>,
    onDismiss: () -> Unit,
    onSave: (ProductEntity, CashierEntity, List<ProductEntity>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }
    var unitSize by remember { mutableStateOf("") }
    var storeName by remember { mutableStateOf("") }
    var costInput by remember { mutableStateOf("") }
    var markupType by remember { mutableStateOf("FIXED") }
    var markupValueInput by remember { mutableStateOf("") }
    var manualPriceInput by remember { mutableStateOf("") }
    var isBeverage by remember { mutableStateOf(false) }
    var normalPriceInput by remember { mutableStateOf("") }
    var coldPriceInput by remember { mutableStateOf("") }

    var verifiedCashier by remember { mutableStateOf<CashierEntity?>(null) }

    val cost = costInput.toDoubleOrNull() ?: 0.0
    val markupValue = markupValueInput.toDoubleOrNull() ?: 0.0
    val manualSellingPrice = manualPriceInput.toDoubleOrNull()
    val calculatedSellingPrice = PricingEngine.calculateSellingPrice(cost, markupType, markupValue, manualSellingPrice)

    if (verifiedCashier == null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Add Standard Product", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Product Name") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text("Brand") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f))
                        OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = unit, onValueChange = { unit = it }, label = { Text("Unit (pcs)") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f))
                        OutlinedTextField(value = unitSize, onValueChange = { unitSize = it }, label = { Text("Size (500ml)") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(value = storeName, onValueChange = { storeName = it }, label = { Text("Vendor Store") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = costInput, onValueChange = { costInput = it }, label = { Text("Item Cost (₱)") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())

                    Text("Markup Formula:", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = markupType == "FIXED", onClick = { markupType = "FIXED" }, label = { Text("Fixed Ladder") }, shape = RoundedCornerShape(10.dp))
                        FilterChip(selected = markupType == "PERCENTAGE", onClick = { markupType = "PERCENTAGE" }, label = { Text("% Markup") }, shape = RoundedCornerShape(10.dp))
                        FilterChip(selected = markupType == "MANUAL", onClick = { markupType = "MANUAL" }, label = { Text("Manual") }, shape = RoundedCornerShape(10.dp))
                    }

                    if (markupType == "PERCENTAGE") {
                        OutlinedTextField(value = markupValueInput, onValueChange = { markupValueInput = it }, label = { Text("Markup %") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                    } else if (markupType == "MANUAL") {
                        OutlinedTextField(value = manualPriceInput, onValueChange = { manualPriceInput = it }, label = { Text("Manual Price (₱)") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                    }

                    Text("Calculated Selling Price: ₱${"%.2f".format(calculatedSellingPrice)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isBeverage, onCheckedChange = { isBeverage = it })
                        Text("Beverage (Enable Normal/Cold options)")
                    }

                    if (isBeverage) {
                        OutlinedTextField(value = normalPriceInput, onValueChange = { normalPriceInput = it }, label = { Text("Normal Price (₱) - Optional") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = coldPriceInput, onValueChange = { coldPriceInput = it }, label = { Text("Cold Price (₱) - Optional") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                    }
                }
            },
            confirmButton = {
                Button(onClick = { verifiedCashier = cashiers.firstOrNull() }, enabled = name.isNotBlank() && cost > 0, shape = RoundedCornerShape(12.dp)) {
                    Text("Verify & Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            },
            shape = RoundedCornerShape(24.dp)
        )
    } else {
        CashierVerificationDialog(
            cashiers = cashiers,
            actionTitle = "Verify Cashier to Save Product",
            onDismiss = onDismiss,
            onVerified = { cashier ->
                val hasBeverageVariants = isBeverage && (normalPriceInput.toDoubleOrNull() ?: 0.0 > 0 || coldPriceInput.toDoubleOrNull() ?: 0.0 > 0)
                val parentProduct = ProductEntity(
                    name = name,
                    category = category,
                    brand = brand,
                    unit = unit,
                    unitSize = unitSize,
                    storeName = storeName,
                    cost = cost,
                    markupType = markupType,
                    markupValue = markupValue,
                    manualSellingPrice = manualSellingPrice,
                    sellingPrice = calculatedSellingPrice,
                    isBeverage = isBeverage,
                    hasVariants = hasBeverageVariants
                )

                val variants = if (hasBeverageVariants) {
                    PricingEngine.generateBeverageVariants(
                        parentProduct = parentProduct,
                        normalPrice = normalPriceInput.toDoubleOrNull(),
                        coldPrice = coldPriceInput.toDoubleOrNull()
                    )
                } else emptyList()

                onSave(parentProduct, cashier, variants)
            }
        )
    }
}

@Composable
fun AddWholesaleProductDialog(
    cashiers: List<CashierEntity>,
    stores: List<StoreEntity>,
    categories: List<CategoryEntity>,
    brands: List<BrandEntity>,
    units: List<UnitEntity>,
    onDismiss: () -> Unit,
    onSave: (ProductEntity, CashierEntity, List<ProductEntity>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("pack") }
    var unitSize by remember { mutableStateOf("") }
    var storeName by remember { mutableStateOf("") }
    var wholesaleCostInput by remember { mutableStateOf("") }
    var itemsPerPackInput by remember { mutableStateOf("12") }

    var verifiedCashier by remember { mutableStateOf<CashierEntity?>(null) }

    val wholesaleCost = wholesaleCostInput.toDoubleOrNull() ?: 0.0
    val itemsPerPack = itemsPerPackInput.toIntOrNull() ?: 1
    val childCost = PricingEngine.calculateWholesaleChildCost(wholesaleCost, itemsPerPack)
    val childSellingPrice = PricingEngine.calculateSellingPrice(childCost, "FIXED")

    if (verifiedCashier == null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Add Wholesale Product", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Wholesale Name") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text("Brand") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f))
                        OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(value = wholesaleCostInput, onValueChange = { wholesaleCostInput = it }, label = { Text("Wholesale Cost (₱)") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = itemsPerPackInput, onValueChange = { itemsPerPackInput = it }, label = { Text("Items per Wholesale Pack") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())

                    Text("Calculated Child Cost: ₱${"%.2f".format(childCost)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("Calculated Child Selling Price: ₱${"%.2f".format(childSellingPrice)}", style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {
                Button(onClick = { verifiedCashier = cashiers.firstOrNull() }, enabled = name.isNotBlank() && wholesaleCost > 0, shape = RoundedCornerShape(12.dp)) {
                    Text("Verify & Save Wholesale", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            },
            shape = RoundedCornerShape(24.dp)
        )
    } else {
        CashierVerificationDialog(
            cashiers = cashiers,
            actionTitle = "Verify Cashier to Save Wholesale Product",
            onDismiss = onDismiss,
            onVerified = { cashier ->
                val parentProduct = ProductEntity(
                    name = "$name (Wholesale Pack)",
                    category = category,
                    brand = brand,
                    unit = unit,
                    unitSize = "$itemsPerPack items",
                    storeName = storeName,
                    cost = wholesaleCost,
                    markupType = "FIXED",
                    sellingPrice = PricingEngine.calculateSellingPrice(wholesaleCost, "FIXED"),
                    isWholesaleParent = true,
                    itemsPerWholesalePack = itemsPerPack,
                    hasVariants = true
                )

                val childVariant = ProductEntity(
                    name = name,
                    category = category,
                    brand = brand,
                    unit = "pcs",
                    unitSize = "1s",
                    storeName = storeName,
                    cost = childCost,
                    markupType = "FIXED",
                    sellingPrice = childSellingPrice,
                    parentProductId = parentProduct.id,
                    variantType = "WHOLESALE_CHILD"
                )

                onSave(parentProduct, cashier, listOf(childVariant))
            }
        )
    }
}

@Composable
fun CashiersManagementTab(
    cashiers: List<CashierEntity>,
    onSaveCashier: (CashierEntity) -> Unit,
    onDeleteCashier: (CashierEntity) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Add New Cashier", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Cashier Name") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = pin, onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pin = it }, label = { Text("4-Digit PIN") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                Button(
                    onClick = {
                        onSaveCashier(CashierEntity(name = name, pin = pin))
                        name = ""
                        pin = ""
                    },
                    enabled = name.isNotBlank() && pin.length == 4,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Add Cashier", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Active Cashiers List:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(cashiers, key = { it.id }) { cashier ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(text = cashier.name, fontWeight = FontWeight.Bold)
                            Text(text = "PIN: **** (${cashier.pin.take(1)}***)", style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { onDeleteCashier(cashier) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MasterDataManagementTab(
    stores: List<StoreEntity>,
    categories: List<CategoryEntity>,
    brands: List<BrandEntity>,
    units: List<UnitEntity>,
    banRecords: List<BanRecordEntity>,
    cashiers: List<CashierEntity>,
    onSaveMasterData: (type: String, name: String) -> Unit,
    onDeleteMasterData: (type: String, id: String) -> Unit,
    onSaveBanRecord: (BanRecordEntity, CashierEntity) -> Unit,
    onDeleteBanRecord: (BanRecordEntity, CashierEntity) -> Unit
) {
    var selectedDataTab by remember { mutableIntStateOf(0) }
    val dataTabs = listOf("Ban Records", "Stores", "Categories", "Brands", "Units")

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(selectedTabIndex = selectedDataTab, edgePadding = 0.dp) {
            dataTabs.forEachIndexed { idx, title ->
                Tab(selected = selectedDataTab == idx, onClick = { selectedDataTab = idx }, text = { Text(title, fontWeight = FontWeight.SemiBold) })
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedDataTab) {
            0 -> BanRecordsSection(banRecords, cashiers, onSaveBanRecord, onDeleteBanRecord)
            1 -> SimpleMasterDataSection("Store", stores.map { it.id to it.name }, onSaveMasterData, onDeleteMasterData)
            2 -> SimpleMasterDataSection("Category", categories.map { it.id to it.name }, onSaveMasterData, onDeleteMasterData)
            3 -> SimpleMasterDataSection("Brand", brands.map { it.id to it.name }, onSaveMasterData, onDeleteMasterData)
            4 -> SimpleMasterDataSection("Unit", units.map { it.id to it.name }, onSaveMasterData, onDeleteMasterData)
        }
    }
}

@Composable
fun BanRecordsSection(
    banRecords: List<BanRecordEntity>,
    cashiers: List<CashierEntity>,
    onSaveBanRecord: (BanRecordEntity, CashierEntity) -> Unit,
    onDeleteBanRecord: (BanRecordEntity, CashierEntity) -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var verifiedCashier by remember { mutableStateOf<CashierEntity?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Add Ban Record", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Full Name") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = nickname, onValueChange = { nickname = it }, label = { Text("Alias") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Reason") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                Button(
                    onClick = { verifiedCashier = cashiers.firstOrNull() },
                    enabled = fullName.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Add Ban Record", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(banRecords, key = { it.id }) { ban ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(text = ban.fullName, fontWeight = FontWeight.Bold)
                            Text(text = "Reason: ${ban.reason}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }

    verifiedCashier?.let {
        CashierVerificationDialog(
            cashiers = cashiers,
            actionTitle = "Verify Cashier to Add Ban Record",
            onDismiss = { verifiedCashier = null },
            onVerified = { cashier ->
                onSaveBanRecord(BanRecordEntity(fullName = fullName, nickname = nickname, reason = reason, cashierId = cashier.id, cashierName = cashier.name), cashier)
                fullName = ""
                nickname = ""
                reason = ""
                verifiedCashier = null
            }
        )
    }
}

@Composable
fun SimpleMasterDataSection(
    typeLabel: String,
    itemsList: List<Pair<String, String>>,
    onSave: (String, String) -> Unit,
    onDelete: (String, String) -> Unit
) {
    var nameInput by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = nameInput, onValueChange = { nameInput = it }, label = { Text("New $typeLabel Name") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f))
            Button(
                onClick = {
                    if (nameInput.isNotBlank()) {
                        onSave(typeLabel, nameInput)
                        nameInput = ""
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Add")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(itemsList, key = { it.first }) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = item.second, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { onDelete(typeLabel, item.first) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SecurityManagementTab(
    securitySettings: SecuritySettingsEntity?,
    onUpdateSecuritySettings: (SecuritySettingsEntity) -> Unit,
    onUpdateSecurityPin: (String, String) -> Boolean
) {
    var currentPinInput by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var pinMessage by remember { mutableStateOf<String?>(null) }

    val currentSettings = securitySettings ?: SecuritySettingsEntity()

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Security Protections", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Require PIN on App Launch", fontWeight = FontWeight.SemiBold)
                    Switch(
                        checked = currentSettings.pinOnAppLaunchEnabled,
                        onCheckedChange = { onUpdateSecuritySettings(currentSettings.copy(pinOnAppLaunchEnabled = it)) }
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Require PIN on Opening Settings", fontWeight = FontWeight.SemiBold)
                    Switch(
                        checked = currentSettings.pinOnSettingsOpeningEnabled,
                        onCheckedChange = { onUpdateSecuritySettings(currentSettings.copy(pinOnSettingsOpeningEnabled = it)) }
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Change 6-Digit Security PIN", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                OutlinedTextField(value = currentPinInput, onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) currentPinInput = it }, label = { Text("Current 6-Digit PIN") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = newPinInput, onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) newPinInput = it }, label = { Text("New 6-Digit PIN") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())

                pinMessage?.let { Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }

                Button(
                    onClick = {
                        val success = onUpdateSecurityPin(currentPinInput, newPinInput)
                        if (success) {
                            pinMessage = "Security PIN updated successfully!"
                            currentPinInput = ""
                            newPinInput = ""
                        } else {
                            pinMessage = "Incorrect current PIN."
                        }
                    },
                    enabled = currentPinInput.length == 6 && newPinInput.length == 6,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Update PIN", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AuditLogsTab(auditLogs: List<AuditLogEntity>) {
    if (auditLogs.isEmpty()) {
        Text("No audit log records found.", style = MaterialTheme.typography.bodyMedium)
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(auditLogs, key = { it.id }) { log ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (log.actionType.contains("DELETE")) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = log.actionType, fontWeight = FontWeight.Bold)
                        Text(text = log.description, style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Cashier: ${log.cashierName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
    }
}
