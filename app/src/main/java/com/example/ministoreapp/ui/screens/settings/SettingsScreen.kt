package com.example.ministoreapp.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ministoreapp.data.local.entities.AuditLogEntity
import com.example.ministoreapp.data.local.entities.BanRecordEntity
import com.example.ministoreapp.data.local.entities.BrandEntity
import com.example.ministoreapp.data.local.entities.CashierEntity
import com.example.ministoreapp.data.local.entities.CategoryEntity
import com.example.ministoreapp.data.local.entities.ContainerEntity
import com.example.ministoreapp.data.local.entities.ProductEntity
import com.example.ministoreapp.data.local.entities.ProductGroupEntity
import com.example.ministoreapp.data.local.entities.SecuritySettingsEntity
import com.example.ministoreapp.data.local.entities.UnitEntity
import com.example.ministoreapp.data.local.entities.VendorEntity
import com.example.ministoreapp.data.local.entities.WholesaleUnitEntity
import com.example.ministoreapp.domain.AppReleaseInfo
import com.example.ministoreapp.domain.AppUpdateManager
import com.example.ministoreapp.domain.PricingEngine
import com.example.ministoreapp.ui.components.CashierVerificationDialog
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    activeDetailView: String? = null,
    onActiveDetailViewChange: (String?) -> Unit = {},
    securitySettings: SecuritySettingsEntity?,
    products: List<ProductEntity>,
    productGroups: List<ProductGroupEntity> = emptyList(),
    cashiers: List<CashierEntity>,
    auditLogs: List<AuditLogEntity>,
    banRecords: List<BanRecordEntity>,
    categories: List<CategoryEntity>,
    brands: List<BrandEntity>,
    units: List<UnitEntity>,
    containers: List<ContainerEntity> = emptyList(),
    wholesaleUnits: List<WholesaleUnitEntity> = emptyList(),
    vendors: List<VendorEntity> = emptyList(),
    isDarkTheme: Boolean = false,
    onToggleDarkTheme: (Boolean) -> Unit = {},
    onSaveProductGroup: (group: ProductGroupEntity, generatedItems: List<ProductEntity>, cashier: CashierEntity) -> Unit,
    onDeleteSingleProduct: (product: ProductEntity, cashier: CashierEntity) -> Unit,
    onDeleteProductGroup: (groupId: String, cashier: CashierEntity) -> Unit,
    onSaveCashier: (CashierEntity, performerName: String, reason: String) -> Unit,
    onDeleteCashier: (CashierEntity, performerName: String, reason: String) -> Unit,
    onSaveBanRecord: (BanRecordEntity, CashierEntity) -> Unit,
    onDeleteBanRecord: (BanRecordEntity, CashierEntity) -> Unit,
    onSaveMasterData: (type: String, name: String, cashier: CashierEntity) -> Unit,
    onDeleteMasterData: (type: String, id: String, name: String, cashier: CashierEntity) -> Unit,
    onSaveVendor: (VendorEntity, CashierEntity) -> Unit = { _, _ -> },
    onDeleteVendor: (VendorEntity, CashierEntity) -> Unit = { _, _ -> },
    onExportBackupJson: () -> String = { "" },
    onImportBackupJson: (String) -> Unit = {},
    onSwitchGoogleAccount: () -> Unit = {},
    googleAccountEmail: String? = null,
    initialBarcode: String = "",
    onPendingBarcodeConsumed: () -> Unit = {},
    onUpdateSecuritySettings: (SecuritySettingsEntity) -> Unit
) {
    val context = LocalContext.current
    val currentSettings = securitySettings ?: SecuritySettingsEntity()
    var activeVerifiedCashier by remember { mutableStateOf<CashierEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (activeDetailView != null) {
            Box(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                when (activeDetailView) {
                    "PRODUCTS" -> ProductsManagementTab(
                        products = products,
                        productGroups = productGroups,
                        initialBarcode = initialBarcode,
                        onPendingBarcodeConsumed = onPendingBarcodeConsumed,
                        cashiers = cashiers,
                        categories = categories,
                        brands = brands,
                        units = units,
                        containers = containers,
                        wholesaleUnits = wholesaleUnits,
                        vendors = vendors,
                        securitySettings = currentSettings,
                        onUpdateSecuritySettings = onUpdateSecuritySettings,
                        activeVerifiedCashier = activeVerifiedCashier,
                        onCashierVerified = { activeVerifiedCashier = it },
                        onSaveProductGroup = onSaveProductGroup,
                        onDeleteSingleProduct = onDeleteSingleProduct,
                        onDeleteProductGroup = onDeleteProductGroup
                    )
                    "CASHIERS" -> CashiersManagementTab(
                        cashiers = cashiers,
                        securitySettings = currentSettings,
                        onSaveCashier = onSaveCashier,
                        onDeleteCashier = onDeleteCashier
                    )
                    "VENDORS" -> VendorsManagementTab(
                        vendors = vendors,
                        cashiers = cashiers,
                        activeVerifiedCashier = activeVerifiedCashier,
                        onCashierVerified = { activeVerifiedCashier = it },
                        onSaveVendor = onSaveVendor,
                        onDeleteVendor = onDeleteVendor
                    )
                    "DROPDOWNS" -> MasterDataManagementTab(
                        categories = categories,
                        brands = brands,
                        units = units,
                        containers = containers,
                        wholesaleUnits = wholesaleUnits,
                        banRecords = banRecords,
                        cashiers = cashiers,
                        activeVerifiedCashier = activeVerifiedCashier,
                        onCashierVerified = { activeVerifiedCashier = it },
                        onSaveMasterData = onSaveMasterData,
                        onDeleteMasterData = onDeleteMasterData,
                        onSaveBanRecord = onSaveBanRecord,
                        onDeleteBanRecord = onDeleteBanRecord
                    )
                    "SECURITY" -> SecurityManagementTab(
                        securitySettings = securitySettings,
                        onUpdateSecuritySettings = onUpdateSecuritySettings
                    )
                    "BAN_LIST" -> BanRecordsSection(banRecords, cashiers, onSaveBanRecord, onDeleteBanRecord)
                    "SYNC" -> SyncBackupPlaceholder(securitySettings, googleAccountEmail, onExportBackupJson, onImportBackupJson, onSwitchGoogleAccount)
                    "AUDIT_LOGS" -> AuditLogsTab(auditLogs = auditLogs)
                    "STORE_PROFILE" -> StoreProfileManagementTab(
                        securitySettings = securitySettings,
                        onUpdateSecuritySettings = onUpdateSecuritySettings
                    )
                    "ABOUT" -> AboutAppPlaceholder()
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Products
                item {
                    SettingsTileRow(
                        title = "Products",
                        subtitle = "${productGroups.size.let { if (it > 0) it else products.size }} product recipes · bulk calculator",
                        icon = Icons.Default.Inventory2,
                        onClick = { onActiveDetailViewChange("PRODUCTS") }
                    )
                }

                // 2. Cashiers & PIN
                item {
                    SettingsTileRow(
                        title = "Cashiers & PIN",
                        subtitle = "${cashiers.size} active · 6-digit PIN set",
                        icon = Icons.Default.People,
                        onClick = { onActiveDetailViewChange("CASHIERS") }
                    )
                }

                // 3. Vendors
                if (com.example.ministoreapp.domain.FeatureFlagManager.isFeatureActive(com.example.ministoreapp.domain.AppFeature.VENDOR_MANAGEMENT)) {
                    item {
                        SettingsTileRow(
                            title = "Vendors",
                            subtitle = "${vendors.size} suppliers & truck panel routes",
                            icon = Icons.Default.Store,
                            onClick = { onActiveDetailViewChange("VENDORS") }
                        )
                    }
                }

                // 4. Dropdown Options
                if (com.example.ministoreapp.domain.FeatureFlagManager.isFeatureActive(com.example.ministoreapp.domain.AppFeature.CUSTOM_DROPDOWNS)) {
                    item {
                        SettingsTileRow(
                            title = "Dropdown Options",
                            subtitle = "${categories.size} categories · ${brands.size} brands · ${containers.size} containers",
                            icon = Icons.AutoMirrored.Filled.ListAlt,
                            onClick = { onActiveDetailViewChange("DROPDOWNS") }
                        )
                    }
                }

                // 5. Credit Ban List
                if (com.example.ministoreapp.domain.FeatureFlagManager.isFeatureActive(com.example.ministoreapp.domain.AppFeature.CUSTOMER_BAN_LIST)) {
                    item {
                        SettingsTileRow(
                            title = "Credit Ban List",
                            subtitle = "${banRecords.size} people flagged",
                            icon = Icons.Default.Block,
                            onClick = { onActiveDetailViewChange("BAN_LIST") }
                        )
                    }
                }

                // 6. Audit Logs
                if (com.example.ministoreapp.domain.FeatureFlagManager.isFeatureActive(com.example.ministoreapp.domain.AppFeature.AUDIT_LOGS)) {
                    item {
                        SettingsTileRow(
                            title = "Audit Logs",
                            subtitle = "${auditLogs.size} recorded actions",
                            icon = Icons.Default.Shield,
                            onClick = { onActiveDetailViewChange("AUDIT_LOGS") }
                        )
                    }
                }

                // 7. Security
                if (com.example.ministoreapp.domain.FeatureFlagManager.isFeatureActive(com.example.ministoreapp.domain.AppFeature.CASHIER_SECURITY_PIN)) {
                    item {
                        SettingsTileRow(
                            title = "Security",
                            subtitle = "App Launch, Settings PIN & 6-digit PIN change",
                            icon = Icons.Default.Lock,
                            onClick = { onActiveDetailViewChange("SECURITY") }
                        )
                    }
                }

                // 8. Store Profile
                item {
                    SettingsTileRow(
                        title = "Store Profile",
                        subtitle = currentSettings.storeName,
                        icon = Icons.Default.Storefront,
                        onClick = { onActiveDetailViewChange("STORE_PROFILE") }
                    )
                }

                // 9. Dark Theme Switch
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DarkMode,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = "Dark Theme",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isDarkTheme) "Dark mode active" else "Light mode active",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Switch(
                                checked = isDarkTheme,
                                onCheckedChange = onToggleDarkTheme
                            )
                        }
                    }
                }

                // 10. Sync & Backup
                if (com.example.ministoreapp.domain.FeatureFlagManager.isFeatureActive(com.example.ministoreapp.domain.AppFeature.CLOUD_SYNC)) {
                    item {
                        SettingsTileRow(
                            title = "Sync & Backup",
                            subtitle = "Google Drive delta sync & JSON backup/restore",
                            icon = Icons.Default.Sync,
                            onClick = { onActiveDetailViewChange("SYNC") }
                        )
                    }
                }

                item {
                    StoreFeaturesAndModulesCard()
                }

                val isDebug = (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
                if (isDebug) {
                    item {
                        DeveloperOptionsCard()
                    }
                }

                // 11. About + Update App
                item {
                    val context = LocalContext.current
                    val installedVersion = remember { AppUpdateManager.getCurrentVersion(context) }
                    var latestGithubVersion by remember { mutableStateOf("Checking...") }
                    var downloadUrl by remember { mutableStateOf<String?>(null) }

                    LaunchedEffect(Unit) {
                        val result = AppUpdateManager.fetchLatestRelease(context)
                        if (result.isSuccess) {
                            val info = result.getOrNull()!!
                            latestGithubVersion = info.version
                            downloadUrl = info.downloadUrl
                        } else {
                            latestGithubVersion = "Error (${result.exceptionOrNull()?.message ?: "Unknown"})"
                        }
                    }

                    SettingsTileRow(
                        title = "About + Update App",
                        subtitle = "Installed: v$installedVersion • GitHub Latest: v$latestGithubVersion",
                        icon = Icons.Default.Info,
                        onClick = { onActiveDetailViewChange("ABOUT") }
                    )
                }
            }
        }
    }
}

fun getDetailTitle(key: String?): String {
    return when (key) {
        "PRODUCTS" -> "Products"
        "CASHIERS" -> "Cashiers & PIN"
        "VENDORS" -> "Vendors"
        "DROPDOWNS" -> "Dropdown Options"
        "BAN_LIST" -> "Credit Ban List"
        "AUDIT_LOGS" -> "Audit Logs"
        "SECURITY" -> "Security"
        "STORE_PROFILE" -> "Store Profile"
        "SYNC" -> "Sync & Backup"
        "ABOUT" -> "About + Update App"
        else -> "Settings"
    }
}

@Composable
fun SettingsTileRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun ProductsManagementTab(
    products: List<ProductEntity>,
    productGroups: List<ProductGroupEntity> = emptyList(),
    initialBarcode: String = "",
    onPendingBarcodeConsumed: () -> Unit = {},
    cashiers: List<CashierEntity>,
    categories: List<CategoryEntity>,
    brands: List<BrandEntity>,
    units: List<UnitEntity>,
    containers: List<ContainerEntity> = emptyList(),
    wholesaleUnits: List<WholesaleUnitEntity> = emptyList(),
    vendors: List<VendorEntity> = emptyList(),
    securitySettings: SecuritySettingsEntity? = null,
    onUpdateSecuritySettings: (SecuritySettingsEntity) -> Unit = {},
    activeVerifiedCashier: CashierEntity? = null,
    onCashierVerified: (CashierEntity) -> Unit = {},
    onSaveProductGroup: (group: ProductGroupEntity, generatedItems: List<ProductEntity>, cashier: CashierEntity) -> Unit,
    onDeleteSingleProduct: (product: ProductEntity, cashier: CashierEntity) -> Unit,
    onDeleteProductGroup: (groupId: String, cashier: CashierEntity) -> Unit
) {
    var filterQuery by remember { mutableStateOf("") }
    var showAddProductDialog by remember { mutableStateOf(false) }
    var editingProductGroup by remember { mutableStateOf<ProductGroupEntity?>(null) }
    var activeDialogInitialBarcode by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(initialBarcode) {
        if (initialBarcode.isNotBlank()) {
            activeDialogInitialBarcode = initialBarcode
            showAddProductDialog = true
            onPendingBarcodeConsumed()
        }
    }

    var verifySingleDeleteProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var verifyGroupDeleteId by remember { mutableStateOf<String?>(null) }

    val filteredGroups = remember(productGroups, filterQuery) {
        productGroups.filter { group ->
            filterQuery.isBlank() ||
                group.baseName.contains(filterQuery, ignoreCase = true) ||
                group.aliases.contains(filterQuery, ignoreCase = true) ||
                group.brand.contains(filterQuery, ignoreCase = true) ||
                group.category.contains(filterQuery, ignoreCase = true)
        }
    }

    val standaloneProducts = remember(products, productGroups, filterQuery) {
        val groupIds = productGroups.map { it.id }.toSet()
        products.filter { it.groupId == null || !groupIds.contains(it.groupId) }.filter {
            filterQuery.isBlank() ||
                it.name.contains(filterQuery, ignoreCase = true) ||
                it.aliases.contains(filterQuery, ignoreCase = true) ||
                it.brand.contains(filterQuery, ignoreCase = true) ||
                it.category.contains(filterQuery, ignoreCase = true)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = filterQuery,
                onValueChange = { filterQuery = it },
                placeholder = { Text("Filter products, alias...", style = MaterialTheme.typography.bodyMedium) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (filterQuery.isNotEmpty()) {
                        IconButton(onClick = { filterQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                singleLine = true
            )

            Button(
                onClick = { showAddProductDialog = true },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                modifier = Modifier.height(50.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("New", fontWeight = FontWeight.Bold)
            }
        }

        if (com.example.ministoreapp.domain.FeatureFlagManager.isFeatureActive(com.example.ministoreapp.domain.AppFeature.PROFIT_AND_PRICING_ENGINE) && securitySettings != null) {
            var showPricingSettings by remember { mutableStateOf(false) }

            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = { showPricingSettings = !showPricingSettings },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (showPricingSettings) "Hide Pricing & Markup Rules" else "🧮 Customize Pricing & Markup Rules", fontWeight = FontWeight.Bold)
            }

            if (showPricingSettings) {
                Spacer(modifier = Modifier.height(10.dp))
                CustomPricingEngineCard(
                    securitySettings = securitySettings,
                    onUpdateSecuritySettings = onUpdateSecuritySettings
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredGroups.isEmpty() && standaloneProducts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (filterQuery.isBlank()) "No product recipes." else "No products matching '$filterQuery'",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredGroups, key = { it.id }) { group ->
                    val groupProducts = products.filter { it.groupId == group.id }
                    ProductGroupCardItem(
                        group = group,
                        groupProducts = groupProducts,
                        onEditClick = { editingProductGroup = group },
                        onDeleteGroupClick = { verifyGroupDeleteId = group.id },
                        onDeleteSingleProductClick = { verifySingleDeleteProduct = it }
                    )
                }

                items(standaloneProducts, key = { it.id }) { product ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                editingProductGroup = ProductGroupEntity(
                                    id = product.groupId ?: UUID.randomUUID().toString(),
                                    baseName = product.name,
                                    aliases = product.aliases,
                                    brand = product.brand,
                                    category = product.category,
                                    vendorName = product.vendorName,
                                    unitSize = product.unitSize,
                                    containerType = product.unit
                                )
                            },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFE3EFE9)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocalOffer,
                                        contentDescription = null,
                                        tint = Color(0xFF0B5E38),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = product.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Text(
                                        text = "Cost ₱${product.cost.roundToInt()}${if (product.storeName.isNotBlank()) " · ${product.storeName}" else ""}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "₱${product.sellingPrice.roundToInt()}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                IconButton(
                                    onClick = { verifySingleDeleteProduct = product },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddProductDialog || editingProductGroup != null) {
        val matchingProds = if (editingProductGroup != null) products.filter { it.groupId == editingProductGroup?.id } else emptyList()
        AddProductDialog(
            existingGroup = editingProductGroup,
            existingProductsForGroup = matchingProds,
            initialBarcode = activeDialogInitialBarcode ?: "",
            securitySettings = securitySettings,
            cashiers = cashiers,
            categories = categories,
            brands = brands,
            units = units,
            containers = containers,
            wholesaleUnits = wholesaleUnits,
            vendors = vendors,
            activeVerifiedCashier = activeVerifiedCashier,
            onCashierVerified = onCashierVerified,
            onDismiss = {
                showAddProductDialog = false
                editingProductGroup = null
                activeDialogInitialBarcode = null
            },
            onSaveGroup = { group, generatedItems, cashier, isDiffSize ->
                onSaveProductGroup(group, generatedItems, cashier)
                if (!isDiffSize) {
                    showAddProductDialog = false
                    editingProductGroup = null
                    activeDialogInitialBarcode = null
                }
            }
        )
    }

    verifySingleDeleteProduct?.let { prod ->
        if (com.example.ministoreapp.domain.FeatureFlagManager.isFeatureActive(com.example.ministoreapp.domain.AppFeature.CASHIER_SECURITY_PIN)) {
            CashierVerificationDialog(
                cashiers = cashiers,
                actionTitle = "Verify Cashier to Delete Item '${prod.name}'",
                onDismiss = { verifySingleDeleteProduct = null },
                onVerified = { cashier ->
                    onDeleteSingleProduct(prod, cashier)
                    verifySingleDeleteProduct = null
                }
            )
        } else {
            LaunchedEffect(prod) {
                onDeleteSingleProduct(prod, cashiers.firstOrNull() ?: com.example.ministoreapp.data.local.entities.CashierEntity(id = "default", name = "Store Owner", pin = "0000"))
                verifySingleDeleteProduct = null
            }
        }
    }

    verifyGroupDeleteId?.let { groupId ->
        if (com.example.ministoreapp.domain.FeatureFlagManager.isFeatureActive(com.example.ministoreapp.domain.AppFeature.CASHIER_SECURITY_PIN)) {
            CashierVerificationDialog(
                cashiers = cashiers,
                actionTitle = "Verify Cashier to Delete Product Group",
                onDismiss = { verifyGroupDeleteId = null },
                onVerified = { cashier ->
                    onDeleteProductGroup(groupId, cashier)
                    verifyGroupDeleteId = null
                }
            )
        } else {
            LaunchedEffect(groupId) {
                onDeleteProductGroup(groupId, cashiers.firstOrNull() ?: com.example.ministoreapp.data.local.entities.CashierEntity(id = "default", name = "Store Owner", pin = "0000"))
                verifyGroupDeleteId = null
            }
        }
    }
}

@Composable
fun ProductGroupCardItem(
    group: ProductGroupEntity,
    groupProducts: List<ProductEntity>,
    onEditClick: () -> Unit,
    onDeleteGroupClick: () -> Unit,
    onDeleteSingleProductClick: (ProductEntity) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    val title = remember(group) {
        buildString {
            if (group.baseName.contains("*")) {
                val variantsList = group.variants.split(",").map { it.trim() }.filter { it.isNotBlank() }.joinToString(", ")
                val cleanBase = group.baseName.replace("*", "").trim()
                if (variantsList.isNotBlank()) {
                    append(variantsList)
                    if (cleanBase.isNotBlank()) append(" — ")
                }
                append(cleanBase)
            } else {
                append(group.baseName)
            }
            if (group.unitSize.isNotBlank()) append(" ${group.unitSize}")
            if (group.containerType.isNotBlank()) append(" ${group.containerType}")
        }
    }

    val subtitle = buildString {
        if (group.hasWholesale && group.wholesaleCost > 0) {
            val perPcCost = if (group.itemsPerPack > 0) (group.wholesaleCost / group.itemsPerPack).roundToInt() else 0
            append("Case Cost: ₱${group.wholesaleCost.roundToInt()} (₱$perPcCost/pc)")
        } else if (group.retailCost > 0) {
            append("Cost: ₱${group.retailCost.roundToInt()}")
        }
        if (group.aliases.isNotBlank()) {
            if (this.isNotEmpty()) append(" · ")
            append("Aliases: ${group.aliases}")
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE3EFE9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalOffer,
                            contentDescription = null,
                            tint = Color(0xFF0B5E38),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (subtitle.isNotBlank()) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEditClick, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Group",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(onClick = onDeleteGroupClick, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Group",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Price Chips Row (including Cold Case chip)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (group.hasWholesale && group.wholesaleSellingPrice > 0) {
                    item {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("Case: ₱${group.wholesaleSellingPrice.roundToInt()}", style = MaterialTheme.typography.labelSmall) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
                if (group.hasRetail && group.retailSellingPrice > 0) {
                    item {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("Retail: ₱${group.retailSellingPrice.roundToInt()}", style = MaterialTheme.typography.labelSmall) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
                if (group.isBeverage && group.coldRetailMarkup > 0) {
                    item {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("Cold: ₱${(group.retailSellingPrice + group.coldRetailMarkup).roundToInt()}", style = MaterialTheme.typography.labelSmall) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
                if (group.isBeverage && group.hasWholesale && group.coldCaseMarkup > 0) {
                    item {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("Cold Case: ₱${(group.wholesaleSellingPrice + group.coldCaseMarkup).roundToInt()}", style = MaterialTheme.typography.labelSmall) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }

            // Expandable Dropdown Arrow
            if (groupProducts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isExpanded = !isExpanded }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isExpanded) "▲ Hide ${groupProducts.size} items" else "▼ Show ${groupProducts.size} generated items",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0B5E38)
                    )
                }

                if (isExpanded) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        groupProducts.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "₱${item.sellingPrice.roundToInt()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = { onDeleteSingleProductClick(item) },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Item",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoCompletableOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    suggestions: List<String>,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val filteredSuggestions = remember(value, suggestions) {
        if (value.isBlank()) suggestions else suggestions.filter { it.contains(value, ignoreCase = true) && !it.equals(value, ignoreCase = true) }
    }
    val isNewValue = value.isNotBlank() && suggestions.none { it.equals(value, ignoreCase = true) }
    val showDropdown = expanded && (filteredSuggestions.isNotEmpty() || isNewValue)

    ExposedDropdownMenuBox(
        expanded = showDropdown,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = true
            },
            label = { Text(label) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true).fillMaxWidth(),
            singleLine = true
        )

        if (filteredSuggestions.isNotEmpty() || isNewValue) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                filteredSuggestions.forEach { selectionOption ->
                    DropdownMenuItem(
                        text = { Text(selectionOption) },
                        onClick = {
                            onValueChange(selectionOption)
                            expanded = false
                        }
                    )
                }

                if (isNewValue) {
                    if (filteredSuggestions.isNotEmpty()) {
                        HorizontalDivider()
                    }
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "➕ Add new $label: \"$value\"",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        },
                        onClick = {
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

data class PreviewItem(
    val comboKey: String,
    val rowKey: String = "",
    val displayName: String,
    val unitSize: String,
    val unit: String,
    val cost: Double,
    val defaultSellingPrice: Double,
    val variantType: String,
    val isWholesale: Boolean,
    val itemsPerPack: Int?,
    val isCheckedState: MutableState<Boolean> = mutableStateOf(true),
    val customPriceInput: MutableState<String> = mutableStateOf(""),
    val barcodeState: MutableState<String> = mutableStateOf("")
)

fun buildProductNames(baseName: String, variantsInput: String): List<String> {
    val cleanVariants = variantsInput.split(",").map { it.trim() }.filter { it.isNotBlank() }
    if (cleanVariants.isEmpty()) {
        return listOf(baseName.replace("*", "").trim())
    }
    return cleanVariants.map { variant ->
        if (baseName.contains("*")) {
            baseName.replace("*", variant).trim()
        } else {
            "$baseName $variant".trim()
        }
    }
}

fun formatCostDisplay(cost: Double): String {
    if (cost <= 0.0) return ""
    return if (cost % 1.0 == 0.0) cost.roundToInt().toString() else cost.toString()
}

@Composable
fun AddProductDialog(
    existingGroup: ProductGroupEntity? = null,
    existingProductsForGroup: List<ProductEntity> = emptyList(),
    initialBarcode: String = "",
    securitySettings: SecuritySettingsEntity? = null,
    cashiers: List<CashierEntity>,
    categories: List<CategoryEntity>,
    brands: List<BrandEntity>,
    units: List<UnitEntity>,
    containers: List<ContainerEntity> = emptyList(),
    wholesaleUnits: List<WholesaleUnitEntity> = emptyList(),
    vendors: List<VendorEntity> = emptyList(),
    activeVerifiedCashier: CashierEntity? = null,
    onCashierVerified: (CashierEntity) -> Unit = {},
    onDismiss: () -> Unit,
    onSaveGroup: (group: ProductGroupEntity, generatedItems: List<ProductEntity>, cashier: CashierEntity, isDiffSize: Boolean) -> Unit
) {
    var baseName by remember { mutableStateOf(existingGroup?.baseName ?: "") }
    var defaultRetailBarcode by remember(initialBarcode) { mutableStateOf(initialBarcode) }
    val variantBarcodesMap = remember { mutableStateMapOf<String, String>() }

    // Track the persistent group ID and base name tracking for Diff Size saves
    val persistentGroupId = remember { mutableStateOf(existingGroup?.id ?: UUID.randomUUID().toString()) }
    var lastSavedBaseName by remember { mutableStateOf(existingGroup?.baseName ?: "") }

    LaunchedEffect(baseName) {
        val cleanCurrent = baseName.trim()
        val cleanLast = lastSavedBaseName.trim()
        if (cleanLast.isNotBlank() && cleanCurrent.isNotBlank() && !cleanCurrent.equals(cleanLast, ignoreCase = true)) {
            persistentGroupId.value = UUID.randomUUID().toString()
            lastSavedBaseName = cleanCurrent
        } else if (cleanLast.isBlank() && cleanCurrent.isNotBlank()) {
            lastSavedBaseName = cleanCurrent
        }
    }
    var aliases by remember { mutableStateOf(existingGroup?.aliases ?: "") }
    var brand by remember { mutableStateOf(existingGroup?.brand ?: "") }
    var category by remember { mutableStateOf(existingGroup?.category ?: "") }
    var storeName by remember { mutableStateOf(existingGroup?.vendorName ?: existingGroup?.storeName ?: "") }
    var unitSize by remember { mutableStateOf(existingGroup?.unitSize ?: "") }
    var containerType by remember { mutableStateOf(existingGroup?.containerType ?: "") }
    var variantsInput by remember { mutableStateOf(existingGroup?.variants ?: "") }

    // Block 2: Wholesale (Defaults cleared)
    var hasWholesale by remember { mutableStateOf(existingGroup?.hasWholesale ?: false) }
    var wholesaleUnit by remember { mutableStateOf(existingGroup?.wholesaleUnit ?: "") }
    var wholesaleCostInput by remember { mutableStateOf(if (existingGroup != null && existingGroup.wholesaleCost > 0) formatCostDisplay(existingGroup.wholesaleCost) else "") }
    var itemsPerPackInput by remember { mutableStateOf(if (existingGroup != null && existingGroup.itemsPerPack > 0) existingGroup.itemsPerPack.toString() else "") }
    var wholesaleMarkupType by remember { mutableStateOf("FIXED") }
    var wholesaleMarkupInput by remember { mutableStateOf(if (existingGroup != null && existingGroup.wholesaleMarkupValue > 0) existingGroup.wholesaleMarkupValue.roundToInt().toString() else "") }
    var wholesaleManualPriceInput by remember { mutableStateOf(if (existingGroup != null && existingGroup.wholesaleSellingPrice > 0) existingGroup.wholesaleSellingPrice.roundToInt().toString() else "") }
    var showWholesaleInSearch by remember { mutableStateOf(existingGroup?.showWholesaleInSearch ?: true) }

    // Block 3: Retail / Per Piece (Retail Fixed Markup Removed) & Tingi Bundle Pricing
    var hasRetail by remember { mutableStateOf(existingGroup?.hasRetail ?: true) }
    var retailCostInput by remember { mutableStateOf(if (existingGroup != null && existingGroup.retailCost > 0) formatCostDisplay(existingGroup.retailCost) else "") }
    var retailMarkupInput by remember { mutableStateOf(if (existingGroup != null && existingGroup.retailMarkupValue > 0) existingGroup.retailMarkupValue.roundToInt().toString() else "") }

    var hasBundle by remember { mutableStateOf(existingGroup?.bundlePrice != null && existingGroup.bundlePrice > 0) }
    var bundleQtyInput by remember { mutableStateOf(if (existingGroup != null && existingGroup.bundleQty > 1) existingGroup.bundleQty.toString() else "3") }
    var bundlePriceInput by remember { mutableStateOf(if (existingGroup != null && existingGroup.bundlePrice > 0) existingGroup.bundlePrice.roundToInt().toString() else "5") }

    // Block 4: Cold Beverage (Defaults cleared)
    var isBeverage by remember { mutableStateOf(existingGroup?.isBeverage ?: false) }
    var coldRetailMarkupInput by remember { mutableStateOf(if (existingGroup != null && existingGroup.coldRetailMarkup > 0) existingGroup.coldRetailMarkup.roundToInt().toString() else "") }
    var coldCaseMarkupInput by remember { mutableStateOf(if (existingGroup != null && existingGroup.coldCaseMarkup > 0) existingGroup.coldCaseMarkup.roundToInt().toString() else "") }

    // Master Data Lists for AutoComplete Dropdowns
    val brandSuggestions = remember(brands) { brands.map { it.name }.distinct() }
    val categorySuggestions = remember(categories) { categories.map { it.name }.distinct() }
    val containerSuggestions = remember(containers, units) { (containers.map { it.name } + units.map { it.name }).distinct() }
    // Wholesale Unit suggestions loaded ONLY from Wholesale Units created in Settings > Dropdown Options
    val wholesaleUnitSuggestions = remember(wholesaleUnits) { wholesaleUnits.map { it.name }.distinct() }
    val storeSuggestions = remember(vendors) { vendors.map { it.name }.distinct() }

    // Computed Values
    val wholesaleCost = wholesaleCostInput.toDoubleOrNull() ?: 0.0
    val itemsPerPack = itemsPerPackInput.toIntOrNull() ?: 1

    val computedRetailCost = if (hasWholesale && wholesaleCost > 0 && itemsPerPack > 0) {
        wholesaleCost / itemsPerPack
    } else {
        retailCostInput.toDoubleOrNull() ?: 0.0
    }

    val bMarkup = securitySettings?.pricingBaseMarkup ?: 3.0
    val bSize = securitySettings?.pricingBracketSize ?: 5.0
    val mInc = securitySettings?.pricingMarkupIncrease ?: 1.0

    // Wholesale Selling Price Calculation (Auto-pricing or Wholesale Cost + Wholesale Fixed Markup)
    val wholesaleSellingPrice = remember(wholesaleCost, wholesaleMarkupInput, wholesaleManualPriceInput, bMarkup, bSize, mInc) {
        val manualVal = wholesaleManualPriceInput.toDoubleOrNull()
        val markupVal = wholesaleMarkupInput.toDoubleOrNull() ?: 0.0
        if (manualVal != null && manualVal > 0) {
            manualVal
        } else if (wholesaleCost > 0) {
            if (markupVal > 0) {
                wholesaleCost + markupVal
            } else {
                PricingEngine.calculateSellingPrice(wholesaleCost, "FIXED", 0.0, null, bMarkup, bSize, mInc)
            }
        } else {
            0.0
        }
    }

    // Retail Selling Price Calculation (Auto-pricing or Computed Retail Cost + Retail Fixed Markup)
    val retailSellingPrice = remember(computedRetailCost, retailMarkupInput, bMarkup, bSize, mInc) {
        val markupVal = retailMarkupInput.toDoubleOrNull() ?: 0.0
        if (computedRetailCost > 0) {
            if (markupVal > 0) {
                computedRetailCost + markupVal
            } else {
                PricingEngine.calculateSellingPrice(computedRetailCost, "FIXED", 0.0, null, bMarkup, bSize, mInc)
            }
        } else {
            0.0
        }
    }

    val coldRetailPrice = remember(retailSellingPrice, coldRetailMarkupInput) {
        val markup = coldRetailMarkupInput.toDoubleOrNull() ?: 0.0
        if (markup > 0) retailSellingPrice + markup else 0.0
    }

    val coldCasePrice = remember(wholesaleSellingPrice, coldCaseMarkupInput) {
        val markup = coldCaseMarkupInput.toDoubleOrNull() ?: 0.0
        if (markup > 0) wholesaleSellingPrice + markup else 0.0
    }

    val excludedKeysList = remember(existingGroup) {
        existingGroup?.excludedKeys?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
    }

    // Live Preview Generation using * Placeholder Trick
    val previewItems = remember(
        baseName, aliases, unitSize, containerType, variantsInput,
        hasWholesale, wholesaleUnit, wholesaleCost, itemsPerPack, wholesaleSellingPrice, showWholesaleInSearch,
        hasRetail, computedRetailCost, retailSellingPrice, hasBundle, bundleQtyInput, bundlePriceInput,
        isBeverage, coldRetailPrice, coldCasePrice, excludedKeysList, existingProductsForGroup
    ) {
        val list = mutableListOf<PreviewItem>()
        if (baseName.isBlank()) return@remember list

        val generatedNames = buildProductNames(baseName, variantsInput)
        val bQty = if (hasBundle) bundleQtyInput.toIntOrNull() ?: 1 else 1
        val bPrice = if (hasBundle) bundlePriceInput.toDoubleOrNull() ?: 0.0 else 0.0

        generatedNames.forEachIndexed { variantIdx, name ->
            val sizePackagingText = buildString {
                if (unitSize.isNotBlank()) append(" $unitSize")
                if (containerType.isNotBlank()) append(" $containerType")
            }

            // Helper to extract saved item
            fun getSavedItem(key: String): ProductEntity? {
                return existingProductsForGroup.find { it.comboKey == key }
            }

            // 1. Wholesale Case
            if (hasWholesale && showWholesaleInSearch && wholesaleCost > 0) {
                val comboKey = "${name}_wholesale_reg"
                val rowKey = "variant_${variantIdx}_WHOLESALE"
                val saved = getSavedItem(comboKey)
                val savedBc = saved?.caseBarcode ?: saved?.barcode
                val activeBc = variantBarcodesMap[rowKey] ?: savedBc ?: (if (variantIdx == 0 && !hasRetail && defaultRetailBarcode.isNotBlank()) defaultRetailBarcode else "")
                if (!variantBarcodesMap.containsKey(rowKey) && activeBc.isNotBlank()) {
                    variantBarcodesMap[rowKey] = activeBc
                }

                list.add(
                    PreviewItem(
                        comboKey = comboKey,
                        rowKey = rowKey,
                        displayName = "$name$sizePackagingText (1 ${wholesaleUnit.ifBlank { "Case" }})",
                        unitSize = if (itemsPerPack > 1) "$itemsPerPack pcs" else unitSize,
                        unit = wholesaleUnit.ifBlank { "Case" },
                        cost = wholesaleCost,
                        defaultSellingPrice = wholesaleSellingPrice,
                        variantType = "WHOLESALE",
                        isWholesale = true,
                        itemsPerPack = itemsPerPack,
                        isCheckedState = mutableStateOf(!excludedKeysList.contains(comboKey)),
                        customPriceInput = mutableStateOf(saved?.manualSellingPrice?.roundToInt()?.toString() ?: ""),
                        barcodeState = mutableStateOf(variantBarcodesMap[rowKey] ?: activeBc)
                    )
                )

                if (isBeverage && coldCasePrice > 0) {
                    val coldComboKey = "${name}_wholesale_cold"
                    val coldRowKey = "variant_${variantIdx}_COLD_WHOLESALE"
                    val coldSaved = getSavedItem(coldComboKey)
                    val coldBc = coldSaved?.caseBarcode ?: coldSaved?.barcode ?: variantBarcodesMap[rowKey] ?: ""
                    list.add(
                        PreviewItem(
                            comboKey = coldComboKey,
                            rowKey = coldRowKey,
                            displayName = "$name$sizePackagingText (COLD) (1 ${wholesaleUnit.ifBlank { "Case" }})",
                            unitSize = if (itemsPerPack > 1) "$itemsPerPack pcs" else unitSize,
                            unit = wholesaleUnit.ifBlank { "Case" },
                            cost = wholesaleCost,
                            defaultSellingPrice = coldCasePrice,
                            variantType = "COLD_WHOLESALE",
                            isWholesale = true,
                            itemsPerPack = itemsPerPack,
                            isCheckedState = mutableStateOf(!excludedKeysList.contains(coldComboKey)),
                            customPriceInput = mutableStateOf(coldSaved?.manualSellingPrice?.roundToInt()?.toString() ?: ""),
                            barcodeState = mutableStateOf(coldBc)
                        )
                    )
                }
            }

            // 2. Retail Single Piece or Tingi Bundle
            if (hasRetail) {
                val pieceUnitText = containerType.ifBlank { "pcs" }

                // Add Retail Base (1 Piece)
                val retailComboKey = "${name}_main_reg"
                val rowKey = "variant_${variantIdx}_RETAIL"
                val retailSaved = getSavedItem(retailComboKey)
                val savedBc = retailSaved?.barcode
                val activeBc = variantBarcodesMap[rowKey] ?: savedBc ?: (if (variantIdx == 0 && defaultRetailBarcode.isNotBlank()) defaultRetailBarcode else "")
                if (!variantBarcodesMap.containsKey(rowKey) && activeBc.isNotBlank()) {
                    variantBarcodesMap[rowKey] = activeBc
                }

                list.add(
                    PreviewItem(
                        comboKey = retailComboKey,
                        rowKey = rowKey,
                        displayName = "$name$sizePackagingText (1 $pieceUnitText)",
                        unitSize = unitSize,
                        unit = pieceUnitText,
                        cost = computedRetailCost,
                        defaultSellingPrice = retailSellingPrice,
                        variantType = "RETAIL",
                        isWholesale = false,
                        itemsPerPack = 1,
                        isCheckedState = mutableStateOf(!excludedKeysList.contains(retailComboKey)),
                        customPriceInput = mutableStateOf(retailSaved?.manualSellingPrice?.roundToInt()?.toString() ?: ""),
                        barcodeState = mutableStateOf(variantBarcodesMap[rowKey] ?: activeBc)
                    )
                )

                // Add Tingi Bundle Price if defined and not identical to single piece
                if (hasBundle && bPrice > 0 && !(bQty == 1 && bPrice == retailSellingPrice)) {
                    val bundleComboKey = "${name}_bundle"
                    val bundleRowKey = "variant_${variantIdx}_BUNDLE"
                    val bundleSaved = getSavedItem(bundleComboKey)
                    val savedBc = bundleSaved?.barcode
                    val activeBc = variantBarcodesMap[bundleRowKey] ?: savedBc ?: ""
                    if (!variantBarcodesMap.containsKey(bundleRowKey) && activeBc.isNotBlank()) {
                        variantBarcodesMap[bundleRowKey] = activeBc
                    }

                    list.add(
                        PreviewItem(
                            comboKey = bundleComboKey,
                            rowKey = bundleRowKey,
                            displayName = "$name$sizePackagingText ($bQty for ₱${bPrice.roundToInt()})",
                            unitSize = unitSize,
                            unit = pieceUnitText,
                            cost = computedRetailCost * bQty,
                            defaultSellingPrice = bPrice,
                            variantType = "RETAIL",
                            isWholesale = false,
                            itemsPerPack = bQty,
                            isCheckedState = mutableStateOf(!excludedKeysList.contains(bundleComboKey)),
                            customPriceInput = mutableStateOf(bundleSaved?.manualSellingPrice?.roundToInt()?.toString() ?: ""),
                            barcodeState = mutableStateOf(variantBarcodesMap[bundleRowKey] ?: activeBc)
                        )
                    )
                }

                if (isBeverage && coldRetailPrice > 0) {
                    val coldComboKey = "${name}_main_cold"
                    val coldRowKey = "variant_${variantIdx}_COLD_RETAIL"
                    val coldSaved = getSavedItem(coldComboKey)
                    val coldBc = coldSaved?.barcode ?: variantBarcodesMap[rowKey] ?: ""
                    list.add(
                        PreviewItem(
                            comboKey = coldComboKey,
                            rowKey = coldRowKey,
                            displayName = "$name$sizePackagingText (COLD) (1 $pieceUnitText)",
                            unitSize = unitSize,
                            unit = pieceUnitText,
                            cost = computedRetailCost,
                            defaultSellingPrice = coldRetailPrice,
                            variantType = "COLD_RETAIL",
                            isWholesale = false,
                            itemsPerPack = 1,
                            isCheckedState = mutableStateOf(!excludedKeysList.contains(coldComboKey)),
                            customPriceInput = mutableStateOf(coldSaved?.manualSellingPrice?.roundToInt()?.toString() ?: ""),
                            barcodeState = mutableStateOf(coldBc)
                        )
                    )
                }
            }
        }
        list
    }

    var pendingSaveMode by remember { mutableStateOf<String?>(null) } // "SAVE" or "DIFF_SIZE"

    if (pendingSaveMode == null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(if (existingGroup != null) "Edit Product Calculator" else "Add Product Calculator", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Block 1: Shared Identity
                    Text("Block 1: Shared Identity", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    OutlinedTextField(
                        value = baseName,
                        onValueChange = { baseName = it },
                        label = { Text("Base Product Name (Use * for position, e.g. * Mismo)") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(value = aliases, onValueChange = { aliases = it }, label = { Text("Aliases / Search Keywords (e.g. Coke, Soda)") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), singleLine = true)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AutoCompletableOutlinedTextField(value = brand, onValueChange = { brand = it }, label = "Brand", suggestions = brandSuggestions, modifier = Modifier.weight(1f))
                        AutoCompletableOutlinedTextField(value = category, onValueChange = { category = it }, label = "Category", suggestions = categorySuggestions, modifier = Modifier.weight(1f))
                    }

                    if (com.example.ministoreapp.domain.FeatureFlagManager.isFeatureActive(com.example.ministoreapp.domain.AppFeature.VENDOR_MANAGEMENT)) {
                        AutoCompletableOutlinedTextField(value = storeName, onValueChange = { storeName = it }, label = "Vendor / Supplier (e.g. Coke Panel)", suggestions = storeSuggestions)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = unitSize, onValueChange = { unitSize = it }, label = { Text("Unit Size (1L / 65g)") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f), singleLine = true)
                        AutoCompletableOutlinedTextField(value = containerType, onValueChange = { containerType = it }, label = "Container (Bottle/Sachet)", suggestions = containerSuggestions, modifier = Modifier.weight(1f))
                    }

                    OutlinedTextField(value = variantsInput, onValueChange = { variantsInput = it }, label = { Text("Variants Tags (Coke, Sprite, Royal / Red, Blue)") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), singleLine = true)

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // Block 2: Wholesale
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = hasWholesale, onCheckedChange = { hasWholesale = it })
                        Text("Has Wholesale / Case?", fontWeight = FontWeight.Bold)
                    }

                    if (hasWholesale) {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), shape = RoundedCornerShape(12.dp)) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                AutoCompletableOutlinedTextField(value = wholesaleUnit, onValueChange = { wholesaleUnit = it }, label = "Wholesale Unit (Case/Box/Tie)", suggestions = wholesaleUnitSuggestions)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(value = wholesaleCostInput, onValueChange = { wholesaleCostInput = it }, label = { Text("Wholesale Cost (₱)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(value = itemsPerPackInput, onValueChange = { itemsPerPackInput = it }, label = { Text("Pcs per Pack") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f), singleLine = true)
                                }
                                OutlinedTextField(value = wholesaleMarkupInput, onValueChange = { wholesaleMarkupInput = it }, label = { Text("Wholesale Fixed Markup (+₱)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth(), singleLine = true)
                                Text("Calculated Wholesale Price: ₱${wholesaleSellingPrice.roundToInt()}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = showWholesaleInSearch, onCheckedChange = { showWholesaleInSearch = it })
                                    Text("Show Wholesale item in Main Search", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // Block 3: Retail & Tingi Bundle Pricing
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = hasRetail, onCheckedChange = { hasRetail = it })
                        Text("Main Product (Per Piece / Retail)", fontWeight = FontWeight.Bold)
                    }

                    if (hasRetail) {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), shape = RoundedCornerShape(12.dp)) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Cost per Piece: ₱${computedRetailCost.roundToInt()}", fontWeight = FontWeight.SemiBold)
                                if (!hasWholesale) {
                                    OutlinedTextField(value = retailCostInput, onValueChange = { retailCostInput = it }, label = { Text("Cost per Piece (₱)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth(), singleLine = true)
                                }
                                OutlinedTextField(value = retailMarkupInput, onValueChange = { retailMarkupInput = it }, label = { Text("Retail Fixed Markup (+₱)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth(), singleLine = true)
                                Text("Calculated Retail Price: ₱${retailSellingPrice.roundToInt()}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = hasBundle, onCheckedChange = { hasBundle = it })
                                    Text("Enable Tingi Bundle Price (e.g. 3 for ₱5)", fontWeight = FontWeight.Bold)
                                }

                                if (hasBundle) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = bundleQtyInput,
                                            onValueChange = { bundleQtyInput = it },
                                            label = { Text("Bundle Qty (e.g. 3)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = bundlePriceInput,
                                            onValueChange = { bundlePriceInput = it },
                                            label = { Text("Bundle Price (₱)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                    }
                                    val bQty = bundleQtyInput.toIntOrNull() ?: 1
                                    val bPrice = bundlePriceInput.toDoubleOrNull() ?: 0.0
                                    if (bQty >= 1 && bPrice > 0 && computedRetailCost > 0) {
                                        val bundleCost = computedRetailCost * bQty
                                        val profit = bPrice - bundleCost
                                        if (com.example.ministoreapp.domain.FeatureFlagManager.isFeatureActive(com.example.ministoreapp.domain.AppFeature.PROFIT_AND_PRICING_ENGINE)) {
                                            Text("Cost for $bQty pcs: ₱${bundleCost.roundToInt()} ➔ Profit: +₱${profit.roundToInt()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // Block 4: Beverage Cold
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isBeverage, onCheckedChange = { isBeverage = it })
                        Text("Is Beverage? (Generate Cold Prices)", fontWeight = FontWeight.Bold)
                    }

                    if (isBeverage) {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), shape = RoundedCornerShape(12.dp)) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = coldRetailMarkupInput, onValueChange = { coldRetailMarkupInput = it }, label = { Text("Cold Retail Fixed Markup (+₱)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth(), singleLine = true)
                                Text("Cold Retail Price: ₱${coldRetailPrice.roundToInt()}", fontWeight = FontWeight.Bold)
                                if (hasWholesale) {
                                    OutlinedTextField(value = coldCaseMarkupInput, onValueChange = { coldCaseMarkupInput = it }, label = { Text("Cold Wholesale Case Fixed Markup (+₱)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth(), singleLine = true)
                                    Text("Cold Case Price: ₱${coldCasePrice.roundToInt()}", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // Block 5: Live Preview with Editable Price Overrides
                    Text("Items that will be added to Search (${previewItems.count { it.isCheckedState.value }}):", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        previewItems.forEach { item ->
                            var showScannerForItem by remember { mutableStateOf(false) }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Checkbox(
                                            checked = item.isCheckedState.value,
                                            onCheckedChange = { item.isCheckedState.value = it }
                                        )
                                        Text(
                                            text = item.displayName,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    OutlinedTextField(
                                        value = if (item.customPriceInput.value.isNotBlank()) item.customPriceInput.value else item.defaultSellingPrice.roundToInt().toString(),
                                        onValueChange = { item.customPriceInput.value = it },
                                        label = { Text("₱") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.width(90.dp),
                                        singleLine = true
                                    )
                                }

                                if (item.isCheckedState.value) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    if (item.variantType == "COLD_RETAIL" || item.variantType == "COLD_WHOLESALE") {
                                        Text("🔗 Uses Retail/Case Barcode", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(start = 40.dp))
                                    } else {
                                        OutlinedTextField(
                                            value = item.barcodeState.value,
                                            onValueChange = { newBc ->
                                                item.barcodeState.value = newBc
                                                if (item.rowKey.isNotBlank()) {
                                                    variantBarcodesMap[item.rowKey] = newBc
                                                    if (item.rowKey == "variant_0_RETAIL" || (item.rowKey == "variant_0_WHOLESALE" && !hasRetail)) {
                                                        defaultRetailBarcode = newBc
                                                    }
                                                }
                                            },
                                            label = { Text("Barcode / QR (Optional)", style = MaterialTheme.typography.labelSmall) },
                                            trailingIcon = {
                                                if (com.example.ministoreapp.domain.FeatureFlagManager.isFeatureActive(com.example.ministoreapp.domain.AppFeature.CAMERA_BARCODE_SCANNER)) {
                                                    IconButton(onClick = { showScannerForItem = true }) {
                                                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan Barcode", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                                    }
                                                }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth().padding(start = 40.dp),
                                            singleLine = true,
                                            textStyle = MaterialTheme.typography.bodySmall
                                        )

                                        if (showScannerForItem) {
                                            com.example.ministoreapp.ui.components.CameraBarcodeScannerDialog(
                                                title = "Scan Barcode",
                                                onDismiss = { showScannerForItem = false },
                                                onBarcodeScanned = { code ->
                                                    val cleanCode = code.trim()
                                                    item.barcodeState.value = cleanCode
                                                    if (item.rowKey.isNotBlank()) {
                                                        variantBarcodesMap[item.rowKey] = cleanCode
                                                        if (item.rowKey == "variant_0_RETAIL" || (item.rowKey == "variant_0_WHOLESALE" && !hasRetail)) {
                                                            defaultRetailBarcode = cleanCode
                                                        }
                                                    }
                                                    showScannerForItem = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = { pendingSaveMode = "SAVE" },
                        enabled = baseName.isNotBlank() && previewItems.any { it.isCheckedState.value },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { pendingSaveMode = "DIFF_SIZE" },
                        enabled = baseName.isNotBlank() && previewItems.any { it.isCheckedState.value },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save - Add a Different Size", fontWeight = FontWeight.Bold)
                    }

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel", color = MaterialTheme.colorScheme.outline)
                    }
                }
            },
            dismissButton = null,
            shape = RoundedCornerShape(24.dp)
        )
    } else {
        if (activeVerifiedCashier != null) {
            val sharedGroupId = persistentGroupId.value
            val uncheckedKeys = previewItems.filter { !it.isCheckedState.value }.map { it.comboKey }
            val excludedKeysString = uncheckedKeys.joinToString(",")

            val groupRecord = ProductGroupEntity(
                id = sharedGroupId,
                baseName = baseName,
                aliases = aliases,
                namingMode = "append",
                variants = variantsInput,
                brand = brand,
                category = category,
                vendorName = storeName,
                unitSize = unitSize,
                containerType = containerType,
                hasWholesale = hasWholesale,
                wholesaleUnit = wholesaleUnit,
                wholesaleCost = wholesaleCost,
                itemsPerPack = itemsPerPack,
                wholesaleMarkupType = wholesaleMarkupType,
                wholesaleMarkupValue = wholesaleMarkupInput.toDoubleOrNull() ?: 0.0,
                wholesaleSellingPrice = wholesaleSellingPrice,
                showWholesaleInSearch = showWholesaleInSearch,
                hasRetail = hasRetail,
                retailCost = computedRetailCost,
                retailMarkupType = "FIXED",
                retailMarkupValue = retailMarkupInput.toDoubleOrNull() ?: 0.0,
                retailSellingPrice = retailSellingPrice,
                bundleQty = if (hasBundle) bundleQtyInput.toIntOrNull() ?: 1 else 1,
                bundlePrice = if (hasBundle) bundlePriceInput.toDoubleOrNull() ?: 0.0 else 0.0,
                isBeverage = isBeverage,
                coldRetailMarkup = coldRetailMarkupInput.toDoubleOrNull() ?: 0.0,
                coldCaseMarkup = coldCaseMarkupInput.toDoubleOrNull() ?: 0.0,
                excludedKeys = excludedKeysString,
                updatedAt = System.currentTimeMillis()
            )

            val selectedItems = previewItems.filter { it.isCheckedState.value }

            val generatedEntities = selectedItems.map { item ->
                val finalPrice = item.customPriceInput.value.toDoubleOrNull() ?: item.defaultSellingPrice
                ProductEntity(
                    groupId = sharedGroupId,
                    comboKey = item.comboKey,
                    name = item.displayName,
                    aliases = aliases,
                    category = category,
                    brand = brand,
                    unit = item.unit,
                    unitSize = item.unitSize,
                    vendorName = storeName,
                    cost = item.cost,
                    sellingPrice = finalPrice,
                    isBeverage = isBeverage,
                    isWholesaleParent = item.isWholesale,
                    itemsPerWholesalePack = item.itemsPerPack,
                    variantType = item.variantType,
                    updatedAt = System.currentTimeMillis()
                )
            }

            onSaveGroup(groupRecord, generatedEntities, activeVerifiedCashier, pendingSaveMode == "DIFF_SIZE")

            if (pendingSaveMode == "DIFF_SIZE") {
                lastSavedBaseName = baseName
                persistentGroupId.value = UUID.randomUUID().toString()
                unitSize = ""
                containerType = ""
                wholesaleCostInput = ""
                retailCostInput = ""
                wholesaleManualPriceInput = ""
                retailMarkupInput = ""
                bundleQtyInput = ""
                bundlePriceInput = ""
                defaultRetailBarcode = ""
                variantBarcodesMap.clear()
                pendingSaveMode = null
            } else {
                pendingSaveMode = null
                onDismiss()
            }
        } else {
            val effectiveCashiers = remember(cashiers) {
                if (cashiers.isEmpty()) listOf(CashierEntity(id = "default_owner", name = "Store Owner", pin = "0000")) else cashiers
            }
            CashierVerificationDialog(
                cashiers = effectiveCashiers,
                actionTitle = if (pendingSaveMode == "DIFF_SIZE") "Verify Cashier (Current Form Saved, Proceeding to Diff. Size)" else "Verify Cashier to Save Product Group",
                onDismiss = { pendingSaveMode = null },
                onVerified = { cashier ->
                    onCashierVerified(cashier)
                    val sharedGroupId = persistentGroupId.value

                    val uncheckedKeys = previewItems.filter { !it.isCheckedState.value }.map { it.comboKey }
                    val excludedKeysString = uncheckedKeys.joinToString(",")

                    val groupRecord = ProductGroupEntity(
                        id = sharedGroupId,
                        baseName = baseName,
                        aliases = aliases,
                        namingMode = "append",
                        variants = variantsInput,
                        brand = brand,
                        category = category,
                        vendorName = storeName,
                        unitSize = unitSize,
                        containerType = containerType,
                        hasWholesale = hasWholesale,
                        wholesaleUnit = wholesaleUnit,
                        wholesaleCost = wholesaleCost,
                        itemsPerPack = itemsPerPack,
                        wholesaleMarkupType = wholesaleMarkupType,
                        wholesaleMarkupValue = wholesaleMarkupInput.toDoubleOrNull() ?: 0.0,
                        wholesaleSellingPrice = wholesaleSellingPrice,
                        showWholesaleInSearch = showWholesaleInSearch,
                        hasRetail = hasRetail,
                        retailCost = computedRetailCost,
                        retailMarkupType = "FIXED",
                        retailMarkupValue = retailMarkupInput.toDoubleOrNull() ?: 0.0,
                        retailSellingPrice = retailSellingPrice,
                        bundleQty = if (hasBundle) bundleQtyInput.toIntOrNull() ?: 1 else 1,
                        bundlePrice = if (hasBundle) bundlePriceInput.toDoubleOrNull() ?: 0.0 else 0.0,
                        isBeverage = isBeverage,
                        coldRetailMarkup = coldRetailMarkupInput.toDoubleOrNull() ?: 0.0,
                        coldCaseMarkup = coldCaseMarkupInput.toDoubleOrNull() ?: 0.0,
                        excludedKeys = excludedKeysString,
                        updatedAt = System.currentTimeMillis()
                    )

                    val selectedItems = previewItems.filter { it.isCheckedState.value }

                    val generatedEntities = selectedItems.map { item ->
                        val finalPrice = item.customPriceInput.value.toDoubleOrNull() ?: item.defaultSellingPrice
                        val isCase = item.variantType == "WHOLESALE" || item.variantType == "COLD_WHOLESALE"
                        val bCode = if (item.variantType == "COLD_RETAIL") {
                            previewItems.find { it.variantType == "RETAIL" }?.barcodeState?.value ?: ""
                        } else if (item.variantType == "COLD_WHOLESALE") {
                            previewItems.find { it.variantType == "WHOLESALE" }?.barcodeState?.value ?: ""
                        } else {
                            item.barcodeState.value
                        }

                        ProductEntity(
                            groupId = sharedGroupId,
                            comboKey = item.comboKey,
                            name = item.displayName,
                            aliases = aliases,
                            category = category,
                            brand = brand,
                            unit = item.unit,
                            unitSize = item.unitSize,
                            vendorName = storeName,
                            cost = item.cost,
                            sellingPrice = finalPrice,
                            isBeverage = isBeverage,
                            isWholesaleParent = item.isWholesale,
                            itemsPerWholesalePack = item.itemsPerPack,
                            variantType = item.variantType,
                            barcode = if (!isCase) bCode else null,
                            caseBarcode = if (isCase) bCode else null,
                            updatedAt = System.currentTimeMillis()
                        )
                    }

                    onSaveGroup(groupRecord, generatedEntities, cashier, pendingSaveMode == "DIFF_SIZE")

                    if (pendingSaveMode == "DIFF_SIZE") {
                        lastSavedBaseName = baseName
                        persistentGroupId.value = UUID.randomUUID().toString()
                        unitSize = ""
                        containerType = ""
                        wholesaleCostInput = ""
                        retailCostInput = ""
                        wholesaleManualPriceInput = ""
                        retailMarkupInput = ""
                        bundleQtyInput = ""
                        bundlePriceInput = ""
                        defaultRetailBarcode = ""
                        variantBarcodesMap.clear()
                        pendingSaveMode = null
                    } else {
                        pendingSaveMode = null
                        onDismiss()
                    }
                }
            )
        }
    }
}

@Composable
fun VendorsManagementTab(
    vendors: List<VendorEntity>,
    cashiers: List<CashierEntity> = emptyList(),
    activeVerifiedCashier: CashierEntity? = null,
    onCashierVerified: (CashierEntity) -> Unit = {},
    onSaveVendor: (VendorEntity, CashierEntity) -> Unit,
    onDeleteVendor: (VendorEntity, CashierEntity) -> Unit
) {
    var showAddModal by remember { mutableStateOf(false) }
    var editingVendor by remember { mutableStateOf<VendorEntity?>(null) }
    var deletingVendor by remember { mutableStateOf<VendorEntity?>(null) }
    var pendingVendorToSave by remember { mutableStateOf<VendorEntity?>(null) }
    var pendingVendorToDelete by remember { mutableStateOf<VendorEntity?>(null) }

    val allVendorNames = remember(vendors) { vendors.map { it.name }.distinct() }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Suppliers & Vendors (${allVendorNames.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Button(
                onClick = { showAddModal = true },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Vendor", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (vendors.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No vendors registered.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(vendors, key = { it.id }) { vendor ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.Store, contentDescription = null, tint = Color(0xFF0B5E38))
                                    Text(text = vendor.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }

                                Row {
                                    IconButton(onClick = { editingVendor = vendor }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(onClick = { deletingVendor = vendor }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            if (vendor.ownerName.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Owner: ${vendor.ownerName}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            }

                            if (vendor.productsSold.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Products Sold: ${vendor.productsSold}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }

                            if (vendor.address.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Address: ${vendor.address}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            if (vendor.contactNumber.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Vendor Contact: ${vendor.contactNumber}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            }

                            if (vendor.agentName.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Agent: ${vendor.agentName}${if (vendor.agentContactNumber.isNotBlank()) " (${vendor.agentContactNumber})" else ""}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }

                            if (vendor.isTruckPanel) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        text = "🚛 Truck Panel Route${if (vendor.routeSchedule.isNotBlank()) ": ${vendor.routeSchedule}" else ""}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddModal || editingVendor != null) {
        val initial = editingVendor
        var nameInput by remember { mutableStateOf(initial?.name ?: "") }
        var ownerInput by remember { mutableStateOf(initial?.ownerName ?: "") }
        var productsSoldInput by remember { mutableStateOf(initial?.productsSold ?: "") }
        var addressInput by remember { mutableStateOf(initial?.address ?: "") }
        var contactInput by remember { mutableStateOf(initial?.contactNumber ?: "") }
        var agentNameInput by remember { mutableStateOf(initial?.agentName ?: "") }
        var agentContactInput by remember { mutableStateOf(initial?.agentContactNumber ?: "") }
        var isTruckPanelInput by remember { mutableStateOf(initial?.isTruckPanel ?: false) }
        var routeScheduleType by remember { mutableStateOf(initial?.routeScheduleType ?: "DAILY") }
        var dailyRouteInput by remember { mutableStateOf(initial?.routeSchedule ?: "") }

        // Specific Days State
        var monChecked by remember { mutableStateOf(false) }
        var monRoute by remember { mutableStateOf("") }
        var tueChecked by remember { mutableStateOf(false) }
        var tueRoute by remember { mutableStateOf("") }
        var wedChecked by remember { mutableStateOf(false) }
        var wedRoute by remember { mutableStateOf("") }
        var thuChecked by remember { mutableStateOf(false) }
        var thuRoute by remember { mutableStateOf("") }
        var friChecked by remember { mutableStateOf(false) }
        var friRoute by remember { mutableStateOf("") }
        var satChecked by remember { mutableStateOf(false) }
        var satRoute by remember { mutableStateOf("") }
        var sunChecked by remember { mutableStateOf(false) }
        var sunRoute by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = {
                showAddModal = false
                editingVendor = null
            },
            title = { Text(if (initial != null) "Edit Vendor" else "Add New Vendor", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(value = nameInput, onValueChange = { nameInput = it }, label = { Text("Name / Company Name") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = ownerInput, onValueChange = { ownerInput = it }, label = { Text("Owner Name") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = productsSoldInput, onValueChange = { productsSoldInput = it }, label = { Text("Product/s Sold (separated by comma)") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = addressInput, onValueChange = { addressInput = it }, label = { Text("Address") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = contactInput, onValueChange = { contactInput = it }, label = { Text("Vendor Contact #") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = agentNameInput, onValueChange = { agentNameInput = it }, label = { Text("Agent Name") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = agentContactInput, onValueChange = { agentContactInput = it }, label = { Text("Agent Contact #") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), singleLine = true)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isTruckPanelInput, onCheckedChange = { isTruckPanelInput = it })
                        Text("Is Truck Panel / Delivery Route?", fontWeight = FontWeight.Bold)
                    }

                    if (isTruckPanelInput) {
                        Text("Route Schedule Mode:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(
                                    selected = routeScheduleType == "DAILY",
                                    onClick = { routeScheduleType = "DAILY" },
                                    label = { Text("Daily") },
                                    shape = RoundedCornerShape(10.dp)
                                )
                                FilterChip(
                                    selected = routeScheduleType == "SPECIFIC_DAYS",
                                    onClick = { routeScheduleType = "SPECIFIC_DAYS" },
                                    label = { Text("Weekly") },
                                    shape = RoundedCornerShape(10.dp)
                                )
                                FilterChip(
                                    selected = routeScheduleType == "EVERY_2_WEEKS",
                                    onClick = { routeScheduleType = "EVERY_2_WEEKS" },
                                    label = { Text("Every 2 Weeks") },
                                    shape = RoundedCornerShape(10.dp)
                                )
                                FilterChip(
                                    selected = routeScheduleType == "MONTHLY",
                                    onClick = { routeScheduleType = "MONTHLY" },
                                    label = { Text("Monthly") },
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }

                        if (routeScheduleType == "DAILY" || routeScheduleType == "EVERY_2_WEEKS" || routeScheduleType == "MONTHLY") {
                            OutlinedTextField(
                                value = dailyRouteInput,
                                onValueChange = { dailyRouteInput = it },
                                label = {
                                    Text(
                                        when (routeScheduleType) {
                                            "EVERY_2_WEEKS" -> "Bi-Weekly Schedule Notes (e.g. Every 2nd Wed)"
                                            "MONTHLY" -> "Monthly Schedule Notes (e.g. 1st Mon of Month)"
                                            else -> "Daily Route Schedule & Notes"
                                        }
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                DayRouteRow("Monday", monChecked, { monChecked = it }, monRoute, { monRoute = it })
                                DayRouteRow("Tuesday", tueChecked, { tueChecked = it }, tueRoute, { tueRoute = it })
                                DayRouteRow("Wednesday", wedChecked, { wedChecked = it }, wedRoute, { wedRoute = it })
                                DayRouteRow("Thursday", thuChecked, { thuChecked = it }, thuRoute, { thuRoute = it })
                                DayRouteRow("Friday", friChecked, { friChecked = it }, friRoute, { friRoute = it })
                                DayRouteRow("Saturday", satChecked, { satChecked = it }, satRoute, { satRoute = it })
                                DayRouteRow("Sunday", sunChecked, { sunChecked = it }, sunRoute, { sunRoute = it })
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalSchedule = if (isTruckPanelInput) {
                            if (routeScheduleType == "DAILY" || routeScheduleType == "EVERY_2_WEEKS" || routeScheduleType == "MONTHLY") {
                                val prefix = when (routeScheduleType) {
                                    "EVERY_2_WEEKS" -> "Every 2 Weeks"
                                    "MONTHLY" -> "Monthly"
                                    else -> "Daily"
                                }
                                if (dailyRouteInput.isNotBlank()) "$prefix: $dailyRouteInput" else prefix
                            } else {
                                listOfNotNull(
                                    if (monChecked) "Mon: ${monRoute.ifBlank { "Route" }}" else null,
                                    if (tueChecked) "Tue: ${tueRoute.ifBlank { "Route" }}" else null,
                                    if (wedChecked) "Wed: ${wedRoute.ifBlank { "Route" }}" else null,
                                    if (thuChecked) "Thu: ${thuRoute.ifBlank { "Route" }}" else null,
                                    if (friChecked) "Fri: ${friRoute.ifBlank { "Route" }}" else null,
                                    if (satChecked) "Sat: ${satRoute.ifBlank { "Route" }}" else null,
                                    if (sunChecked) "Sun: ${sunRoute.ifBlank { "Route" }}" else null
                                ).joinToString(" | ")
                            }
                        } else ""

                        val vendorToSave = VendorEntity(
                            id = initial?.id ?: UUID.randomUUID().toString(),
                            name = nameInput,
                            ownerName = ownerInput,
                            productsSold = productsSoldInput,
                            address = addressInput,
                            contactNumber = contactInput,
                            agentName = agentNameInput,
                            agentContactNumber = agentContactInput,
                            isTruckPanel = isTruckPanelInput,
                            routeScheduleType = routeScheduleType,
                            routeSchedule = finalSchedule,
                            updatedAt = System.currentTimeMillis()
                        )

                        if (activeVerifiedCashier != null) {
                            onSaveVendor(vendorToSave, activeVerifiedCashier)
                            showAddModal = false
                            editingVendor = null
                        } else {
                            pendingVendorToSave = vendorToSave
                        }
                    },
                    enabled = nameInput.isNotBlank(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddModal = false
                    editingVendor = null
                }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    deletingVendor?.let { vendor ->
        AlertDialog(
            onDismissRequest = { deletingVendor = null },
            title = { Text("Delete Vendor '${vendor.name}'", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
            text = { Text("Are you sure you want to delete this vendor record?") },
            confirmButton = {
                Button(
                    onClick = {
                        if (activeVerifiedCashier != null) {
                            onDeleteVendor(vendor, activeVerifiedCashier)
                            deletingVendor = null
                        } else {
                            pendingVendorToDelete = vendor
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingVendor = null }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    pendingVendorToSave?.let { vendor ->
        val effectiveCashiers = remember(cashiers) {
            if (cashiers.isEmpty()) listOf(CashierEntity(id = "default_owner", name = "Store Owner", pin = "0000")) else cashiers
        }
        CashierVerificationDialog(
            cashiers = effectiveCashiers,
            actionTitle = "Verify Cashier to Save Vendor '${vendor.name}'",
            onDismiss = { pendingVendorToSave = null },
            onVerified = { cashier ->
                onCashierVerified(cashier)
                onSaveVendor(vendor, cashier)
                pendingVendorToSave = null
                showAddModal = false
                editingVendor = null
            }
        )
    }

    pendingVendorToDelete?.let { vendor ->
        val effectiveCashiers = remember(cashiers) {
            if (cashiers.isEmpty()) listOf(CashierEntity(id = "default_owner", name = "Store Owner", pin = "0000")) else cashiers
        }
        CashierVerificationDialog(
            cashiers = effectiveCashiers,
            actionTitle = "Verify Cashier to Delete Vendor '${vendor.name}'",
            onDismiss = { pendingVendorToDelete = null },
            onVerified = { cashier ->
                onCashierVerified(cashier)
                onDeleteVendor(vendor, cashier)
                pendingVendorToDelete = null
                deletingVendor = null
            }
        )
    }
}

@Composable
fun DayRouteRow(
    dayName: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    routeInput: String,
    onRouteInputChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Checkbox(checked = isChecked, onCheckedChange = onCheckedChange)
        Text(dayName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, modifier = Modifier.width(70.dp))
        if (isChecked) {
            OutlinedTextField(
                value = routeInput,
                onValueChange = onRouteInputChange,
                placeholder = { Text("Route for $dayName", style = MaterialTheme.typography.bodySmall) },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).height(48.dp),
                singleLine = true
            )
        }
    }
}

@Composable
fun CashiersManagementTab(
    cashiers: List<CashierEntity>,
    securitySettings: SecuritySettingsEntity,
    onSaveCashier: (CashierEntity, performerName: String, reason: String) -> Unit,
    onDeleteCashier: (CashierEntity, performerName: String, reason: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }

    var editingCashier by remember { mutableStateOf<CashierEntity?>(null) }
    var deletingCashier by remember { mutableStateOf<CashierEntity?>(null) }
    var pendingAddCashier by remember { mutableStateOf<CashierEntity?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Add New Cashier", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Cashier Name") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = pin, onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pin = it }, label = { Text("4-Digit PIN") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), singleLine = true)
                Button(
                    onClick = {
                        val newCashier = CashierEntity(name = name, pin = pin)
                        if (cashiers.isEmpty()) {
                            // First cashier setup: direct save
                            onSaveCashier(newCashier, name, "Initial Cashier Setup")
                            name = ""
                            pin = ""
                        } else {
                            // 2nd+ cashier: prompt cashier verification
                            pendingAddCashier = newCashier
                        }
                    },
                    enabled = name.isNotBlank() && pin.length == 4,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text("Active Cashiers List:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(6.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(cashiers, key = { it.id }) { cashier ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = cashier.name, fontWeight = FontWeight.Bold)
                            Text(text = "PIN: **** (${cashier.pin.take(1)}***)", style = MaterialTheme.typography.bodySmall)
                        }

                        Row {
                            IconButton(onClick = { editingCashier = cashier }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Cashier", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { deletingCashier = cashier }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Cashier", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    pendingAddCashier?.let { newCashier ->
        CashierVerificationDialog(
            cashiers = cashiers,
            actionTitle = "Verify Cashier to Add New Cashier '${newCashier.name}'",
            onDismiss = { pendingAddCashier = null },
            onVerified = { verifyingCashier ->
                onSaveCashier(newCashier, verifyingCashier.name, "Added cashier '${newCashier.name}'")
                name = ""
                pin = ""
                pendingAddCashier = null
            }
        )
    }

    editingCashier?.let { cashier ->
        var editName by remember { mutableStateOf(cashier.name) }
        var editPin by remember { mutableStateOf(cashier.pin) }
        var securityPinInput by remember { mutableStateOf("") }
        var performerName by remember { mutableStateOf("") }
        var reason by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { editingCashier = null },
            title = { Text("Edit Cashier '${cashier.name}'", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text("Cashier Name") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = editPin, onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) editPin = it }, label = { Text("4-Digit Cashier PIN") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), singleLine = true)

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("Security Verification Required:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                    OutlinedTextField(
                        value = securityPinInput,
                        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) securityPinInput = it },
                        label = { Text("6-Digit Security PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(value = performerName, onValueChange = { performerName = it }, label = { Text("Who is editing? (Your Name)") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Reason for edit") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), singleLine = true)

                    errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val expectedPin = securitySettings.securityPin
                        if (securityPinInput != expectedPin) {
                            errorMessage = "Incorrect 6-Digit Security PIN"
                        } else {
                            onSaveCashier(cashier.copy(name = editName, pin = editPin, updatedAt = System.currentTimeMillis()), performerName, reason)
                            editingCashier = null
                        }
                    },
                    enabled = editName.isNotBlank() && editPin.length == 4 && securityPinInput.length == 6 && performerName.isNotBlank() && reason.isNotBlank(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingCashier = null }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    deletingCashier?.let { cashier ->
        var securityPinInput by remember { mutableStateOf("") }
        var performerName by remember { mutableStateOf("") }
        var reason by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { deletingCashier = null },
            title = { Text("Delete Cashier '${cashier.name}'", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Are you sure you want to delete this cashier?")

                    OutlinedTextField(
                        value = securityPinInput,
                        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) securityPinInput = it },
                        label = { Text("6-Digit Security PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(value = performerName, onValueChange = { performerName = it }, label = { Text("Who is deleting? (Your Name)") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Reason for delete") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), singleLine = true)

                    errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val expectedPin = securitySettings.securityPin
                        if (securityPinInput != expectedPin) {
                            errorMessage = "Incorrect 6-Digit Security PIN"
                        } else {
                            onDeleteCashier(cashier, performerName, reason)
                            deletingCashier = null
                        }
                    },
                    enabled = securityPinInput.length == 6 && performerName.isNotBlank() && reason.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Confirm Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingCashier = null }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun CustomPricingEngineCard(
    securitySettings: SecuritySettingsEntity,
    onUpdateSecuritySettings: (SecuritySettingsEntity) -> Unit
) {
    var baseMarkupInput by remember(securitySettings) { mutableStateOf(securitySettings.pricingBaseMarkup.roundToInt().toString()) }
    var bracketSizeInput by remember(securitySettings) { mutableStateOf(securitySettings.pricingBracketSize.roundToInt().toString()) }
    var markupIncreaseInput by remember(securitySettings) { mutableStateOf(securitySettings.pricingMarkupIncrease.roundToInt().toString()) }
    var testCostInput by remember { mutableStateOf("8") }

    val baseMarkup = baseMarkupInput.toDoubleOrNull() ?: 3.0
    val bracketSize = bracketSizeInput.toDoubleOrNull() ?: 5.0
    val markupIncrease = markupIncreaseInput.toDoubleOrNull() ?: 1.0
    val testCost = testCostInput.toDoubleOrNull() ?: 8.0

    val testSellingPrice = com.example.ministoreapp.domain.PricingEngine.calculateSellingPrice(
        cost = testCost,
        markupType = "FIXED",
        baseMarkup = baseMarkup,
        bracketSize = bracketSize,
        markupIncrease = markupIncrease
    )
    val testProfit = testSellingPrice - testCost

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Calculate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Customize Pricing & Markup Calculation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            Text(
                text = "Configure how suggested selling prices and profit margins are automatically calculated based on item cost brackets:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Visual Rule Bracket Table
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("📊 Visual Bracket Rule Breakdown:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("• Cost ₱1 – ₱${bracketSize.roundToInt()} ➔ Add +₱${baseMarkup.roundToInt()} profit", style = MaterialTheme.typography.bodySmall)
                    Text("• Cost ₱${bracketSize.roundToInt() + 1} – ₱${(bracketSize * 2).roundToInt()} ➔ Add +₱${(baseMarkup + markupIncrease).roundToInt()} profit", style = MaterialTheme.typography.bodySmall)
                    Text("• Cost ₱${(bracketSize * 2).roundToInt() + 1} – ₱${(bracketSize * 3).roundToInt()} ➔ Add +₱${(baseMarkup + markupIncrease * 2).roundToInt()} profit", style = MaterialTheme.typography.bodySmall)
                    Text("• (Every +₱${bracketSize.roundToInt()} cost step increases profit by +₱${markupIncrease.roundToInt()})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
            }

            // Bracket Parameter Editors
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = baseMarkupInput,
                    onValueChange = { baseMarkupInput = it },
                    label = { Text("Base Profit (₱)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = bracketSizeInput,
                    onValueChange = { bracketSizeInput = it },
                    label = { Text("Bracket Size (₱)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = markupIncreaseInput,
                    onValueChange = { markupIncreaseInput = it },
                    label = { Text("Step Increase (₱)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            // Interactive Live Preview
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("💡 Live Preview Calculator:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = testCostInput,
                            onValueChange = { testCostInput = it },
                            label = { Text("Cost ₱") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.width(90.dp),
                            singleLine = true
                        )
                        Text(
                            text = "➔ Suggested Price: ₱${testSellingPrice.roundToInt()} (+₱${testProfit.roundToInt()} profit)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        val updated = securitySettings.copy(
                            pricingBaseMarkup = baseMarkup,
                            pricingBracketSize = bracketSize,
                            pricingMarkupIncrease = markupIncrease,
                            updatedAt = System.currentTimeMillis()
                        )
                        onUpdateSecuritySettings(updated)
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save Pricing Rules", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        baseMarkupInput = "3"
                        bracketSizeInput = "5"
                        markupIncreaseInput = "1"
                        val updated = securitySettings.copy(
                            pricingBaseMarkup = 3.0,
                            pricingBracketSize = 5.0,
                            pricingMarkupIncrease = 1.0,
                            updatedAt = System.currentTimeMillis()
                        )
                        onUpdateSecuritySettings(updated)
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Reset to Default", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SecurityManagementTab(
    securitySettings: SecuritySettingsEntity?,
    onUpdateSecuritySettings: (SecuritySettingsEntity) -> Unit
) {
    val currentSettings = securitySettings ?: SecuritySettingsEntity()

    var launchPinEnabled by remember(currentSettings) { mutableStateOf(currentSettings.pinOnAppLaunchEnabled) }
    var settingsPinEnabled by remember(currentSettings) { mutableStateOf(currentSettings.pinOnSettingsOpeningEnabled) }

    var newPinInput by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var showPinPromptDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("App Launch, Settings PIN & 6-Digit PIN Change", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Require 6-Digit PIN on App Launch", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        Text("Prompts for 6-digit PIN when opening app", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                    Switch(
                        checked = launchPinEnabled,
                        onCheckedChange = { launchPinEnabled = it }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Require 6-Digit PIN on Opening Settings", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        Text("Prompts for 6-digit PIN when entering settings tab", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                    Switch(
                        checked = settingsPinEnabled,
                        onCheckedChange = { settingsPinEnabled = it }
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Change 6-Digit Security PIN", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = newPinInput,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) newPinInput = it },
                    label = { Text("New 6-Digit PIN (Leave blank to keep current)") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                statusMessage?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = { showPinPromptDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Save Security Settings", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showPinPromptDialog) {
        var currentPinInput by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showPinPromptDialog = false },
            title = { Text("Verify Current Security PIN", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Enter your Current 6-Digit Security PIN to apply these changes:")
                    OutlinedTextField(
                        value = currentPinInput,
                        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) currentPinInput = it },
                        label = { Text("Current 6-Digit PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    errorMessage?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (currentPinInput == currentSettings.securityPin) {
                            val updatedPin = if (newPinInput.length == 6) newPinInput else currentSettings.securityPin
                            val updated = currentSettings.copy(
                                pinOnAppLaunchEnabled = launchPinEnabled,
                                pinOnSettingsOpeningEnabled = settingsPinEnabled,
                                securityPin = updatedPin,
                                updatedAt = System.currentTimeMillis()
                            )
                            onUpdateSecuritySettings(updated)
                            statusMessage = "Security settings updated successfully!"
                            newPinInput = ""
                            showPinPromptDialog = false
                        } else {
                            errorMessage = "Incorrect 6-Digit Security PIN"
                        }
                    },
                    enabled = currentPinInput.length == 6,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Confirm & Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinPromptDialog = false }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun StoreProfileManagementTab(
    securitySettings: SecuritySettingsEntity?,
    onUpdateSecuritySettings: (SecuritySettingsEntity) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val currentSettings = securitySettings ?: SecuritySettingsEntity()

    var storeName by remember(currentSettings) { mutableStateOf(currentSettings.storeName) }
    var ownerName by remember(currentSettings) { mutableStateOf(currentSettings.storeOwner) }
    var address by remember(currentSettings) { mutableStateOf(currentSettings.storeAddress) }
    var contactNumber by remember(currentSettings) { mutableStateOf(currentSettings.storeContactNumber) }
    var website by remember(currentSettings) { mutableStateOf(currentSettings.storeWebsite) }
    var socials by remember(currentSettings) { mutableStateOf(currentSettings.storeSocials) }

    var saveMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Edit Store Profile & Business Card", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = storeName,
                    onValueChange = { storeName = it },
                    label = { Text("Store Name (Displayed in Header)") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = ownerName,
                    onValueChange = { ownerName = it },
                    label = { Text("Store Owner Name") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Store Address") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = contactNumber,
                    onValueChange = { contactNumber = it },
                    label = { Text("Contact Number") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = website,
                    onValueChange = { website = it },
                    label = { Text("Website") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = socials,
                    onValueChange = { socials = it },
                    label = { Text("Social / Socials (e.g. FB Page / @handle)") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                saveMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val cardText = buildString {
                                appendLine("🏪 $storeName")
                                if (ownerName.isNotBlank()) appendLine("👤 Owner: $ownerName")
                                if (address.isNotBlank()) appendLine("📍 Address: $address")
                                if (contactNumber.isNotBlank()) appendLine("📞 Contact: $contactNumber")
                                if (website.isNotBlank()) appendLine("🌐 Website: $website")
                                if (socials.isNotBlank()) appendLine("💬 Socials: $socials")
                            }.trim()

                            clipboardManager.setText(AnnotatedString(cardText))
                            Toast.makeText(context, "Business Card copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy Card", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val updated = currentSettings.copy(
                                storeName = storeName,
                                storeOwner = ownerName,
                                storeAddress = address,
                                storeContactNumber = contactNumber,
                                storeWebsite = website,
                                storeSocials = socials,
                                updatedAt = System.currentTimeMillis()
                            )
                            onUpdateSecuritySettings(updated)
                            saveMessage = "Store info updated successfully!"
                        },
                        enabled = storeName.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save Profile", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun MasterDataManagementTab(
    categories: List<CategoryEntity>,
    brands: List<BrandEntity>,
    units: List<UnitEntity>,
    containers: List<ContainerEntity> = emptyList(),
    wholesaleUnits: List<WholesaleUnitEntity> = emptyList(),
    banRecords: List<BanRecordEntity>,
    cashiers: List<CashierEntity>,
    activeVerifiedCashier: CashierEntity? = null,
    onCashierVerified: (CashierEntity) -> Unit = {},
    onSaveMasterData: (type: String, name: String, cashier: CashierEntity) -> Unit,
    onDeleteMasterData: (type: String, id: String, name: String, cashier: CashierEntity) -> Unit,
    onSaveBanRecord: (BanRecordEntity, CashierEntity) -> Unit,
    onDeleteBanRecord: (BanRecordEntity, CashierEntity) -> Unit
) {
    var selectedDataTab by remember { mutableIntStateOf(0) }
    val dataTabs = listOf("Categories", "Brands", "Containers", "Wholesale Units")

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(selectedTabIndex = selectedDataTab, edgePadding = 0.dp) {
            dataTabs.forEachIndexed { idx, title ->
                Tab(selected = selectedDataTab == idx, onClick = { selectedDataTab = idx }, text = { Text(title, fontWeight = FontWeight.SemiBold) })
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (selectedDataTab) {
            0 -> SimpleMasterDataSection("Category", categories.map { it.id to it.name }, cashiers, activeVerifiedCashier, onCashierVerified, onSaveMasterData, onDeleteMasterData)
            1 -> SimpleMasterDataSection("Brand", brands.map { it.id to it.name }, cashiers, activeVerifiedCashier, onCashierVerified, onSaveMasterData, onDeleteMasterData)
            2 -> SimpleMasterDataSection("Container", containers.map { it.id to it.name }, cashiers, activeVerifiedCashier, onCashierVerified, onSaveMasterData, onDeleteMasterData)
            3 -> SimpleMasterDataSection("WholesaleUnit", wholesaleUnits.map { it.id to it.name }, cashiers, activeVerifiedCashier, onCashierVerified, onSaveMasterData, onDeleteMasterData)
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
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Add Credit Ban Record", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Full Name") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = nickname, onValueChange = { nickname = it }, label = { Text("Alias") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Reason") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), singleLine = true)
                Button(
                    onClick = { verifiedCashier = cashiers.firstOrNull() },
                    enabled = fullName.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(banRecords, key = { it.id }) { ban ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
            actionTitle = "Verify Cashier to Add Credit Ban Record",
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
    cashiers: List<CashierEntity> = emptyList(),
    activeVerifiedCashier: CashierEntity? = null,
    onCashierVerified: (CashierEntity) -> Unit = {},
    onSave: (String, String, CashierEntity) -> Unit,
    onDelete: (String, String, String, CashierEntity) -> Unit
) {
    var nameInput by remember { mutableStateOf("") }
    var pendingSaveName by remember { mutableStateOf<String?>(null) }
    var pendingDeletePair by remember { mutableStateOf<Pair<String, String>?>(null) } // (id, name)

    Column(modifier = Modifier.fillMaxSize()) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = nameInput, onValueChange = { nameInput = it }, label = { Text("New $typeLabel Name") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f), singleLine = true)
            Button(
                onClick = {
                    if (nameInput.isNotBlank()) {
                        if (activeVerifiedCashier != null) {
                            onSave(typeLabel, nameInput, activeVerifiedCashier)
                            nameInput = ""
                        } else {
                            pendingSaveName = nameInput
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Add")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(itemsList, key = { it.first }) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = item.second, fontWeight = FontWeight.Bold)
                        IconButton(onClick = {
                            if (activeVerifiedCashier != null) {
                                onDelete(typeLabel, item.first, item.second, activeVerifiedCashier)
                            } else {
                                pendingDeletePair = item
                            }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    pendingSaveName?.let { nameToSave ->
        val effectiveCashiers = remember(cashiers) {
            if (cashiers.isEmpty()) listOf(CashierEntity(id = "default_owner", name = "Store Owner", pin = "0000")) else cashiers
        }
        CashierVerificationDialog(
            cashiers = effectiveCashiers,
            actionTitle = "Verify Cashier to Add $typeLabel '$nameToSave'",
            onDismiss = { pendingSaveName = null },
            onVerified = { cashier ->
                onCashierVerified(cashier)
                onSave(typeLabel, nameToSave, cashier)
                pendingSaveName = null
                nameInput = ""
            }
        )
    }

    pendingDeletePair?.let { (id, name) ->
        val effectiveCashiers = remember(cashiers) {
            if (cashiers.isEmpty()) listOf(CashierEntity(id = "default_owner", name = "Store Owner", pin = "0000")) else cashiers
        }
        CashierVerificationDialog(
            cashiers = effectiveCashiers,
            actionTitle = "Verify Cashier to Delete $typeLabel '$name'",
            onDismiss = { pendingDeletePair = null },
            onVerified = { cashier ->
                onCashierVerified(cashier)
                onDelete(typeLabel, id, name, cashier)
                pendingDeletePair = null
            }
        )
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
                        containerColor = if (log.actionType.contains("DELETE")) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = log.actionType, fontWeight = FontWeight.Bold)
                        Text(text = log.description, style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Performer: ${log.cashierName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
    }
}

@Composable
fun SyncBackupPlaceholder(
    securitySettings: SecuritySettingsEntity?,
    googleAccountEmail: String? = null,
    onExportBackupJson: () -> String = { "" },
    onImportBackupJson: (String) -> Unit = {},
    onSwitchGoogleAccount: () -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val prefs = remember { context.getSharedPreferences("ministore_prefs", android.content.Context.MODE_PRIVATE) }

    val lastAccount = remember(context) { com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(context) }
    val displayEmail = googleAccountEmail
        ?: prefs.getString("google_account_email", null)
        ?: lastAccount?.email
        ?: lastAccount?.displayName
        ?: "Not linked"

    // Sync Preferences State
    var syncSettingsEnabled by remember { mutableStateOf(prefs.getBoolean("sync_settings_enabled", true)) }
    var syncRequirePin by remember { mutableStateOf(prefs.getBoolean("sync_require_pin", false)) }
    var syncProductsEnabled by remember { mutableStateOf(prefs.getBoolean("sync_products_enabled", true)) }
    var syncCreditEnabled by remember { mutableStateOf(prefs.getBoolean("sync_credit_enabled", true)) }
    var syncEventsEnabled by remember { mutableStateOf(prefs.getBoolean("sync_events_enabled", true)) }
    var syncRestockEnabled by remember { mutableStateOf(prefs.getBoolean("sync_restock_enabled", true)) }

    var showRestoreDialog by remember { mutableStateOf(false) }
    var restoreJsonInput by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val jsonText = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                if (jsonText.isNotBlank()) {
                    onImportBackupJson(jsonText)
                    statusMessage = "Database backup restored successfully from file!"
                    Toast.makeText(context, "Backup restored from file successfully!", Toast.LENGTH_LONG).show()
                    showRestoreDialog = false
                } else {
                    Toast.makeText(context, "Selected backup file is empty.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Restore Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Sync & Backup Options", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        // Card 1: Google Account Link
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Google Account Sync", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("Linked Account: $displayEmail", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)

                Button(
                    onClick = onSwitchGoogleAccount,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Switch / Replace Google Account", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Card 2: Sync Controls
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Cloud Sync Controls", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                SyncSwitchRow("Require Cashier / PIN Verification to Sync", syncRequirePin) {
                    syncRequirePin = it
                    prefs.edit().putBoolean("sync_require_pin", it).apply()
                }

                SyncSwitchRow("Create Pre-Sync Local Safety Snapshot", syncSettingsEnabled) {
                    syncSettingsEnabled = it
                    prefs.edit().putBoolean("sync_settings_enabled", it).apply()
                }

                Text(
                    text = "Note: Cloud Sync transfers 100% of your store's products, cashiers, PINs, vendors, credits, and settings for an exact multi-device match.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Card 3: Local Backup (Clipboard & File Chooser preserved!)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Local Database Backup & Restore", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("Export backup JSON to Downloads folder/clipboard, or restore from a JSON file/clipboard text.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                statusMessage?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            try {
                                val jsonStr = onExportBackupJson()
                                clipboardManager.setText(AnnotatedString(jsonStr))

                                val fileName = "gj_store_backup_${System.currentTimeMillis()}.json"
                                val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                                val file = java.io.File(downloadsDir, fileName)
                                java.io.FileOutputStream(file).use { out ->
                                    out.write(jsonStr.toByteArray())
                                }

                                statusMessage = "Exported! Saved to Downloads: $fileName (and copied to clipboard)"
                                Toast.makeText(context, "Backup exported & copied to clipboard!", Toast.LENGTH_LONG).show()
                            } catch (e: Exception) {
                                e.printStackTrace()
                                statusMessage = "Export failed: ${e.message}"
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export Backup (JSON)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { showRestoreDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Restore Backup (JSON)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Restore Database Backup (JSON)", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilledTonalButton(
                        onClick = { filePickerLauncher.launch("*/*") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open Downloads / Select File (*.json)", fontWeight = FontWeight.Bold)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                    Text("Or paste your backup JSON text below:", style = MaterialTheme.typography.labelMedium)
                    OutlinedTextField(
                        value = restoreJsonInput,
                        onValueChange = { restoreJsonInput = it },
                        placeholder = { Text("Paste JSON backup string here...") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp, max = 200.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        try {
                            if (restoreJsonInput.isNotBlank()) {
                                onImportBackupJson(restoreJsonInput)
                                statusMessage = "Database backup restored successfully!"
                                Toast.makeText(context, "Backup restored successfully!", Toast.LENGTH_LONG).show()
                                showRestoreDialog = false
                                restoreJsonInput = ""
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(context, "Restore Error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    },
                    enabled = restoreJsonInput.isNotBlank(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Restore Database", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun SyncSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun AboutAppPlaceholder() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isCheckingUpdates by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableIntStateOf(0) }

    var releaseInfo by remember { mutableStateOf<AppReleaseInfo?>(null) }
    var statusDialogMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("About + Update App", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        val isDebug = (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (isDebug) {
            DeveloperOptionsCard()
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("SariCloud", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("Powered by James Lasquite", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Current Version: v${AppUpdateManager.getCurrentVersion(LocalContext.current)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Button(
                    onClick = {
                        if (!com.example.ministoreapp.util.NetworkUtils.isNetworkAvailable(context)) {
                            statusDialogMessage = "No internet connection available. Please check your network and try again."
                            return@Button
                        }
                        isCheckingUpdates = true
                        coroutineScope.launch {
                            val result = AppUpdateManager.fetchLatestRelease(
                                context = context,
                                owner = "insomniaczxz",
                                repo = "ministoreapp"
                            )
                            isCheckingUpdates = false
                            if (result.isSuccess) {
                                val info = result.getOrNull()!!
                                releaseInfo = info
                            } else {
                                val errMsg = com.example.ministoreapp.util.NetworkUtils.getFriendlyErrorMessage(result.exceptionOrNull())
                                statusDialogMessage = "Update Check Result:\n\n$errMsg\n\nRunning version v${AppUpdateManager.getCurrentVersion(context)}."
                            }
                        }
                    },
                    enabled = !isCheckingUpdates && !isDownloading,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isCheckingUpdates) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Checking GitHub...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Check for Updates", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    releaseInfo?.let { info ->
        AlertDialog(
            onDismissRequest = { releaseInfo = null },
            icon = {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (info.hasUpdate) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = if (info.hasUpdate) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.outline
                    )
                }
            },
            title = {
                Text(
                    text = if (info.hasUpdate) "New Update Available (v${info.version})" else "You are Up to Date!",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Latest Release: v${info.version} (Current: v${AppUpdateManager.getCurrentVersion(context)})", fontWeight = FontWeight.SemiBold)
                    if (info.releaseNotes.isNotBlank()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 320.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text("Changelog:\n${info.releaseNotes}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    if (isDownloading) {
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { downloadProgress / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("Downloading APK... $downloadProgress%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val targetUrl = info.downloadUrl ?: "https://github.com/insomniaczxz/ministoreapp/releases"
                    Button(
                        onClick = {
                            if (!info.downloadUrl.isNullOrBlank()) {
                                isDownloading = true
                                coroutineScope.launch {
                                    val downloadResult = AppUpdateManager.downloadAndInstallApk(
                                        context = context,
                                        downloadUrl = info.downloadUrl,
                                        onProgress = { downloadProgress = it }
                                    )
                                    isDownloading = false
                                    if (downloadResult.isFailure) {
                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(targetUrl))
                                        context.startActivity(intent)
                                    } else {
                                        releaseInfo = null
                                    }
                                }
                            } else {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(targetUrl))
                                context.startActivity(intent)
                                releaseInfo = null
                            }
                        },
                        enabled = !isDownloading,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isDownloading) "Downloading APK... $downloadProgress%" else "Download & Install Update", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { releaseInfo = null },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Close", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = null,
            shape = RoundedCornerShape(24.dp)
        )
    }

    statusDialogMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { statusDialogMessage = null },
            icon = {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            },
            title = { Text("App Update Check", fontWeight = FontWeight.Bold) },
            text = { Text(msg, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                Button(
                    onClick = { statusDialogMessage = null },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun DeveloperOptionsCard() {
    val context = LocalContext.current
    val currentTier by com.example.ministoreapp.domain.FeatureFlagManager.selectedTierFlow.collectAsState()
    val isMonthlyActive by com.example.ministoreapp.domain.FeatureFlagManager.isSubscriptionActiveFlow.collectAsState()
    val effectiveTier by com.example.ministoreapp.domain.FeatureFlagManager.effectiveTierFlow.collectAsState()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.BugReport,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Developer Options (Freemium Tiers & Subscription)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Text(
                text = "Active Subscription Tier (Effective: ${effectiveTier.name.uppercase()})",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )

            // Radio Buttons for Free, Standard, Pro
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                com.example.ministoreapp.domain.AppTier.entries.forEach { tier ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { com.example.ministoreapp.domain.FeatureFlagManager.setTier(context, tier) }
                            .padding(vertical = 4.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentTier == tier,
                            onClick = { com.example.ministoreapp.domain.FeatureFlagManager.setTier(context, tier) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "${tier.name} Tier",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            val desc = when (tier) {
                                com.example.ministoreapp.domain.AppTier.FREE -> "Max 20 Customers · No Multi-Size · No Cloud Sync"
                                com.example.ministoreapp.domain.AppTier.STANDARD -> "Max 100 Customers · Multi-Size · Cloud Sync"
                                com.example.ministoreapp.domain.AppTier.PRO -> "Unlimited Customers · All Features Unlocked"
                            }
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Monthly Subscription Active",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isMonthlyActive) "Status: Active" else "Status: Inactive (Falls back to FREE tier)",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isMonthlyActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
                Switch(
                    checked = isMonthlyActive,
                    onCheckedChange = { com.example.ministoreapp.domain.FeatureFlagManager.setMonthlySubscriptionActive(context, it) }
                )
            }
            
            val isTrialActive by com.example.ministoreapp.domain.FeatureFlagManager.isProFreeTrialActiveFlow.collectAsState()
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "1-Month PRO Free Trial Active",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Switch(
                    checked = isTrialActive,
                    onCheckedChange = { com.example.ministoreapp.domain.FeatureFlagManager.setProFreeTrialActive(context, it) }
                )
            }
        }
    }
}

@Composable
fun StoreFeaturesAndModulesCard() {
    val context = LocalContext.current
    val userToggles by com.example.ministoreapp.domain.FeatureFlagManager.userFeatureTogglesFlow.collectAsState()
    val effectiveTier by com.example.ministoreapp.domain.FeatureFlagManager.effectiveTierFlow.collectAsState()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Store Features & Modules",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Customize your store UI. Disabled modules are completely hidden to keep your app simple and fast.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider()

            com.example.ministoreapp.domain.AppFeature.entries.forEach { feature ->
                val isUserEnabled = userToggles[feature] ?: feature.defaultEnabled
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            com.example.ministoreapp.domain.FeatureFlagManager.setUserFeatureToggle(context, feature, !isUserEnabled)
                        }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = feature.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = feature.description,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isUserEnabled,
                        onCheckedChange = {
                            com.example.ministoreapp.domain.FeatureFlagManager.setUserFeatureToggle(context, feature, it)
                        }

                    )
                }
            }
        }
    }
}
