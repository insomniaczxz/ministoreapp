package com.example.ministoreapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.ministoreapp.data.local.AppDatabase
import com.example.ministoreapp.data.local.entities.AuditLogEntity
import com.example.ministoreapp.data.local.entities.CreditTransactionEntity
import com.example.ministoreapp.data.local.entities.PriceHistoryEntity
import com.example.ministoreapp.data.local.entities.RestockItemEntity
import com.example.ministoreapp.data.local.entities.SecuritySettingsEntity
import com.example.ministoreapp.data.repository.SyncRepository
import com.example.ministoreapp.ui.components.GlobalHeader
import com.example.ministoreapp.ui.components.SecurityPinDialog
import com.example.ministoreapp.ui.screens.credit.CreditScreen
import com.example.ministoreapp.ui.screens.events.EventsScreen
import com.example.ministoreapp.ui.screens.info.InfoScreen
import com.example.ministoreapp.ui.screens.restock.RestockScreen
import com.example.ministoreapp.ui.screens.search.SearchScreen
import com.example.ministoreapp.ui.screens.settings.SettingsScreen
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

sealed class TabScreen(val route: String, val title: String, val icon: ImageVector) {
    object Search : TabScreen("search", "Search", Icons.Default.Search)
    object Events : TabScreen("events", "Events", Icons.AutoMirrored.Filled.EventNote)
    object Credit : TabScreen("credit", "Credit", Icons.Default.Payment)
    object Restock : TabScreen("restock", "Restock", Icons.Default.Inventory2)
    object Info : TabScreen("info", "Info", Icons.Default.Info)
    object Settings : TabScreen("settings", "Settings", Icons.Default.Settings)
}

fun formatGroupDisplayTitle(baseName: String, variants: String, unitSize: String, containerType: String): String {
    return buildString {
        if (baseName.contains("*")) {
            val variantsList = variants.split(",").map { it.trim() }.filter { it.isNotBlank() }.joinToString(", ")
            val cleanBase = baseName.replace("*", "").trim()
            if (variantsList.isNotBlank()) {
                append(variantsList)
                if (cleanBase.isNotBlank()) append(" — ")
            }
            append(cleanBase)
        } else {
            append(baseName)
        }
        if (unitSize.isNotBlank()) append(" $unitSize")
        if (containerType.isNotBlank()) append(" $containerType")
    }.ifBlank { baseName }
}

class MainActivity : ComponentActivity() {

    private lateinit var db: AppDatabase
    private lateinit var syncRepo: SyncRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        db = AppDatabase.getInstance(this)
        syncRepo = SyncRepository(db)

        setContent {
            val prefs = remember { getSharedPreferences("ministore_prefs", MODE_PRIVATE) }
            var isDarkTheme by remember { mutableStateOf(prefs.getBoolean("dark_theme_enabled", false)) }

            val lightColors = lightColorScheme(
                primary = androidx.compose.ui.graphics.Color(0xFF0B5E38),
                onPrimary = androidx.compose.ui.graphics.Color.White,
                primaryContainer = androidx.compose.ui.graphics.Color(0xFFDCECE2),
                onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF042113),
                surface = androidx.compose.ui.graphics.Color.White,
                onSurface = androidx.compose.ui.graphics.Color(0xFF191C1A),
                background = androidx.compose.ui.graphics.Color(0xFFEFF4F1),
                onBackground = androidx.compose.ui.graphics.Color(0xFF191C1A)
            )

            val darkColors = darkColorScheme(
                primary = androidx.compose.ui.graphics.Color(0xFF83D7A4),
                onPrimary = androidx.compose.ui.graphics.Color(0xFF00381E),
                primaryContainer = androidx.compose.ui.graphics.Color(0xFF00522E),
                onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF9FE2BD),
                surface = androidx.compose.ui.graphics.Color(0xFF1E211F),
                onSurface = androidx.compose.ui.graphics.Color(0xFFE1E3E0),
                background = androidx.compose.ui.graphics.Color(0xFF121413),
                onBackground = androidx.compose.ui.graphics.Color(0xFFE1E3E0)
            )

            MaterialTheme(colorScheme = if (isDarkTheme) darkColors else lightColors) {
                MainAppContent(
                    isDarkTheme = isDarkTheme,
                    onToggleDarkTheme = { enabled ->
                        isDarkTheme = enabled
                        prefs.edit().putBoolean("dark_theme_enabled", enabled).apply()
                        lifecycleScope.launch {
                            val currentSettings = db.securitySettingsDao().getSettings() ?: com.example.ministoreapp.data.local.entities.SecuritySettingsEntity()
                            db.securitySettingsDao().insertOrUpdate(currentSettings.copy(isDarkMode = enabled, updatedAt = System.currentTimeMillis()))
                        }
                    }
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainAppContent(
        isDarkTheme: Boolean,
        onToggleDarkTheme: (Boolean) -> Unit
    ) {
        var currentTab by remember { mutableStateOf<TabScreen>(TabScreen.Search) }
        var searchQuery by remember { mutableStateOf("") }

        // Live States from Database Flows
        val securitySettings by db.securitySettingsDao().getSettingsFlow().collectAsState(initial = null)
        val productGroups by db.productGroupDao().getAllGroupsFlow().collectAsState(initial = emptyList())
        val products by db.productDao().getAllProductsFlow().collectAsState(initial = emptyList())
        val cashiers by db.cashierDao().getAllCashiersFlow().collectAsState(initial = emptyList())
        val customers by db.customerDao().getAllCustomersFlow().collectAsState(initial = emptyList())
        val events by db.eventDao().getAllEventsFlow().collectAsState(initial = emptyList())
        val restockItems by db.restockItemDao().getRestockItemsFlow().collectAsState(initial = emptyList())
        val banRecords by db.banRecordDao().getAllBanRecordsFlow().collectAsState(initial = emptyList())
        val auditLogs by db.auditLogDao().getAllAuditLogsFlow().collectAsState(initial = emptyList())
        val priceHistory by db.priceHistoryDao().getRecentPriceChangesFlow(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L).collectAsState(initial = emptyList())
        val creditTransactions by db.creditTransactionDao().getAllTransactionsFlow().collectAsState(initial = emptyList())

        val categories by db.masterDataDao().getCategoriesFlow().collectAsState(initial = emptyList())
        val brands by db.masterDataDao().getBrandsFlow().collectAsState(initial = emptyList())
        val units by db.masterDataDao().getUnitsFlow().collectAsState(initial = emptyList())
        val containers by db.masterDataDao().getContainersFlow().collectAsState(initial = emptyList())
        val wholesaleUnits by db.masterDataDao().getWholesaleUnitsFlow().collectAsState(initial = emptyList())
        val vendors by db.masterDataDao().getVendorsFlow().collectAsState(initial = emptyList())

        val context = androidx.compose.ui.platform.LocalContext.current
        val prefs = remember { context.getSharedPreferences("ministore_prefs", MODE_PRIVATE) }
        var isOnboardingComplete by remember { mutableStateOf(prefs.getBoolean("is_onboarding_complete", false)) }
        var googleAccountEmail by remember { mutableStateOf(prefs.getString("google_account_email", null)) }

        val settings = securitySettings ?: SecuritySettingsEntity()

        val gso = remember {
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestProfile()
                .build()
        }
        val googleSignInClient = remember(context) {
            GoogleSignIn.getClient(context, gso)
        }

        var pendingGoogleSignInCallback by remember { mutableStateOf<((String?) -> Unit)?>(null) }

        val recoverableAuthLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
            contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                Toast.makeText(context, "Permission granted! Please click the button again.", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Drive Access Permission Denied.", Toast.LENGTH_SHORT).show()
                pendingGoogleSignInCallback?.invoke(null)
                pendingGoogleSignInCallback = null
            }
        }

        val driveFilePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
            contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
        ) { uri: android.net.Uri? ->
            if (uri != null) {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                val email = "Google Drive Linked"
                googleAccountEmail = email
                prefs.edit().putString("google_account_email", email).putString("google_drive_file_uri", uri.toString()).apply()
                Toast.makeText(context, "Linked: Google Drive File", Toast.LENGTH_SHORT).show()

                lifecycleScope.launch {
                    val cloudJson = try {
                        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    } catch (e: Exception) {
                        null
                    }
                    pendingGoogleSignInCallback?.invoke(cloudJson)
                    pendingGoogleSignInCallback = null
                }
            } else {
                pendingGoogleSignInCallback?.invoke(null)
                pendingGoogleSignInCallback = null
            }
        }

        val googleSignInLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
            contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            var email: String? = null
            var errorMsg: String? = null

            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    email = account.email ?: account.displayName
                }
            } catch (e: ApiException) {
                e.printStackTrace()
                errorMsg = "Google Sign-In Error (${e.statusCode})"
            } catch (e: Exception) {
                e.printStackTrace()
                errorMsg = "Google Sign-In Error: ${e.message}"
            }

            if (email.isNullOrBlank()) {
                val lastAcc = GoogleSignIn.getLastSignedInAccount(context)
                email = lastAcc?.email ?: lastAcc?.displayName
            }

            if (!email.isNullOrBlank()) {
                googleAccountEmail = email
                prefs.edit().putString("google_account_email", email).apply()
                Toast.makeText(context, "Linked: $email", Toast.LENGTH_SHORT).show()

                lifecycleScope.launch {
                    val accountObj = GoogleSignIn.getLastSignedInAccount(context)
                    var cloudJson: String? = null
                    var shouldLaunchPicker = false

                    if (accountObj != null) {
                        try {
                            val token = com.example.ministoreapp.data.repository.GoogleDriveRestService.getOAuthToken(context, accountObj)
                            if (!token.isNullOrBlank()) {
                                val dlRes = com.example.ministoreapp.data.repository.GoogleDriveRestService.downloadFromDriveAppData(context, token)
                                if (dlRes.isSuccess) {
                                    cloudJson = dlRes.getOrNull()
                                    if (cloudJson == null) {
                                        Toast.makeText(context, "No ministore_sync.json found in Google Drive appDataFolder.", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "Found cloud backup (${cloudJson.toByteArray().size} bytes) - Restoring...", Toast.LENGTH_LONG).show()
                                    }
                                } else {
                                    val exMsg = dlRes.exceptionOrNull()?.message ?: ""
                                    // Make sure we show the error so you can see exactly why it's failing
                                    android.util.Log.e("MiniStoreSync", "Drive Sync Error: $exMsg", dlRes.exceptionOrNull())
                                    Toast.makeText(context, "Drive Sync Error: $exMsg", Toast.LENGTH_LONG).show()
                                    if (exMsg.contains("403") || exMsg.contains("401") || exMsg.contains("Forbidden") || exMsg.contains("404")) {
                                        shouldLaunchPicker = true
                                    } else {
                                        // Still launch picker if it's an unknown HTTP error so user isn't stuck
                                        shouldLaunchPicker = true
                                    }
                                }
                            }
                        } catch (e: com.google.android.gms.auth.UserRecoverableAuthException) {
                            android.util.Log.w("MiniStoreSync", "UserRecoverableAuthException: Needs permission", e)
                            recoverableAuthLauncher.launch(e.intent)
                            return@launch
                        } catch (e: Exception) {
                            e.printStackTrace()
                            android.util.Log.e("MiniStoreSync", "Drive Auth Error: ${e.message}", e)
                            Toast.makeText(context, "Drive Auth Error: ${e.message}", Toast.LENGTH_LONG).show()
                            shouldLaunchPicker = true
                        }
                    } else {
                        android.util.Log.e("MiniStoreSync", "accountObj is null after sign in!")
                    }

                    if (shouldLaunchPicker) {
                        Toast.makeText(context, "Falling back to File Picker...", Toast.LENGTH_LONG).show()
                        driveFilePickerLauncher.launch(arrayOf("*/*"))
                    } else {
                        pendingGoogleSignInCallback?.invoke(cloudJson)
                        pendingGoogleSignInCallback = null
                    }
                }
            } else {
                if (errorMsg != null) {
                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                }
                pendingGoogleSignInCallback?.invoke(null)
                pendingGoogleSignInCallback = null
            }
        }

        if (!isOnboardingComplete) {
            com.example.ministoreapp.ui.screens.onboarding.OnboardingScreen(
                defaultPin = settings.securityPin.ifBlank { "526375" },
                onGoogleSignInRequested = { onSuccess ->
                    pendingGoogleSignInCallback = { json ->
                        lifecycleScope.launch {
                            if (!json.isNullOrBlank()) {
                                syncRepo.importAndMergeChangeset(this@MainActivity, json, forceOverwrite = true)
                                onToggleDarkTheme(prefs.getBoolean("dark_theme_enabled", false))
                            }
                            onSuccess(json)
                        }
                    }
                    googleSignInLauncher.launch(googleSignInClient.signInIntent)
                },
                onCreateCashier = { firstCashier ->
                    lifecycleScope.launch {
                        db.cashierDao().insertCashier(firstCashier)
                    }
                },
                onUpdateSecurityPin = { newPin ->
                    lifecycleScope.launch {
                        val updated = settings.copy(securityPin = newPin, updatedAt = System.currentTimeMillis())
                        db.securitySettingsDao().insertOrUpdate(updated)
                    }
                },
                onOnboardingComplete = {
                    prefs.edit().putBoolean("is_onboarding_complete", true).apply()
                    isOnboardingComplete = true
                }
            )
            return
        }

        // App Launch Guard State
        var isAppUnlocked by remember { mutableStateOf(false) }
        var showSettingsPinPrompt by remember { mutableStateOf(false) }
        var activeSettingsDetailView by remember { mutableStateOf<String?>(null) }

        // App Launch Security PIN Check
        if (settings.pinOnAppLaunchEnabled && !isAppUnlocked) {
            SecurityPinDialog(
                title = "Store PIN Protection",
                expectedPin = settings.securityPin,
                onDismiss = { finish() },
                onSuccess = { isAppUnlocked = true }
            )
            return
        }

        // Settings Opening Security PIN Check
        if (showSettingsPinPrompt) {
            SecurityPinDialog(
                title = "Settings Security PIN",
                expectedPin = settings.securityPin,
                onDismiss = { showSettingsPinPrompt = false },
                onSuccess = {
                    showSettingsPinPrompt = false
                    activeSettingsDetailView = null
                    currentTab = TabScreen.Settings
                }
            )
        }

        val activity = (context as? android.app.Activity)
        var lastBackPressTime by remember { mutableLongStateOf(0L) }

        androidx.activity.compose.BackHandler(enabled = true) {
            when {
                activeSettingsDetailView != null -> {
                    activeSettingsDetailView = null
                }
                currentTab != TabScreen.Search -> {
                    currentTab = TabScreen.Search
                }
                else -> {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastBackPressTime < 2000) {
                        activity?.finish()
                    } else {
                        lastBackPressTime = currentTime
                        Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        val headerTitle = when {
            currentTab == TabScreen.Settings && activeSettingsDetailView != null ->
                com.example.ministoreapp.ui.screens.settings.getDetailTitle(activeSettingsDetailView)
            currentTab == TabScreen.Settings -> "Settings"
            else -> settings.storeName
        }

        val showHeaderBackButton = currentTab == TabScreen.Settings && activeSettingsDetailView != null

        var isSyncing by remember { mutableStateOf(false) }
        var syncSuccess by remember { mutableStateOf(false) }

        val showHeaderActions = currentTab != TabScreen.Settings

        Scaffold(
            topBar = {
                GlobalHeader(
                    title = headerTitle,
                    showBackButton = showHeaderBackButton,
                    onBackClick = { activeSettingsDetailView = null },
                    showActions = showHeaderActions,
                    isSyncing = isSyncing,
                    syncSuccess = syncSuccess,
                    onSyncClick = {
                        lifecycleScope.launch {
                            if (!com.example.ministoreapp.util.NetworkUtils.isNetworkAvailable(this@MainActivity)) {
                                Toast.makeText(this@MainActivity, "No internet connection. Changes saved locally.", Toast.LENGTH_SHORT).show()
                                return@launch
                            }

                            isSyncing = true
                            syncSuccess = false

                            try {
                                val accountObj = GoogleSignIn.getLastSignedInAccount(this@MainActivity)
                                val token = try {
                                    if (accountObj != null) {
                                        com.example.ministoreapp.data.repository.GoogleDriveRestService.getOAuthToken(this@MainActivity, accountObj)
                                    } else null
                                } catch (e: com.google.android.gms.auth.UserRecoverableAuthException) {
                                    isSyncing = false
                                    recoverableAuthLauncher.launch(e.intent)
                                    return@launch
                                }

                                if (!token.isNullOrBlank()) {
                                    Toast.makeText(this@MainActivity, "Uploading to Drive appDataFolder...", Toast.LENGTH_SHORT).show()

                                    val dlRes = com.example.ministoreapp.data.repository.GoogleDriveRestService.downloadFromDriveAppData(this@MainActivity, token)
                                    if (dlRes.isSuccess && !dlRes.getOrNull().isNullOrBlank()) {
                                        syncRepo.importAndMergeChangeset(this@MainActivity, dlRes.getOrNull()!!, forceOverwrite = true)
                                    }

                                    val updatedJson = syncRepo.exportChangesetJson(this@MainActivity)
                                    val pushRes = com.example.ministoreapp.data.repository.GoogleDriveRestService.uploadToDriveAppData(this@MainActivity, token, updatedJson)

                                    if (pushRes.isSuccess) {
                                        isSyncing = false
                                        syncSuccess = true
                                        onToggleDarkTheme(prefs.getBoolean("dark_theme_enabled", false))
                                        val fileId = pushRes.getOrNull() ?: "unknown"
                                        Toast.makeText(this@MainActivity, "Success! Drive File ID: $fileId (${updatedJson.toByteArray().size} bytes)", Toast.LENGTH_LONG).show()
                                        kotlinx.coroutines.delay(2000L)
                                        syncSuccess = false
                                    } else {
                                        val ex = pushRes.exceptionOrNull()
                                        val rawMsg = ex?.message ?: ""
                                        android.util.Log.e("MiniStoreSync", "Drive Upload Error: $rawMsg", ex)
                                        Toast.makeText(this@MainActivity, "Upload Error: $rawMsg", Toast.LENGTH_LONG).show()
                                        
                                        val driveUriStr = prefs.getString("google_drive_file_uri", null)

                                        if ((rawMsg.contains("403") || rawMsg.contains("401") || rawMsg.contains("Forbidden")) && !driveUriStr.isNullOrBlank()) {
                                            val uri = android.net.Uri.parse(driveUriStr)
                                            val remoteJson = try {
                                                contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                                            } catch (e: Exception) { null }

                                            if (!remoteJson.isNullOrBlank()) {
                                                syncRepo.importAndMergeChangeset(this@MainActivity, remoteJson, forceOverwrite = true)
                                            }

                                            val finalUpdatedJson = syncRepo.exportChangesetJson(this@MainActivity)
                                            contentResolver.openOutputStream(uri, "wt")?.use { out ->
                                                out.write(finalUpdatedJson.toByteArray())
                                            }

                                            isSyncing = false
                                            syncSuccess = true
                                            onToggleDarkTheme(prefs.getBoolean("dark_theme_enabled", false))
                                            Toast.makeText(this@MainActivity, "Sync Successful (via linked Drive File)!", Toast.LENGTH_SHORT).show()
                                            kotlinx.coroutines.delay(2000L)
                                            syncSuccess = false
                                        } else if (rawMsg.contains("403") || rawMsg.contains("401") || rawMsg.contains("Forbidden")) {
                                            isSyncing = false
                                            Toast.makeText(this@MainActivity, "Drive Access Denied (HTTP 403). Select a Google Drive backup file...", Toast.LENGTH_LONG).show()
                                            driveFilePickerLauncher.launch(arrayOf("*/*"))
                                        } else {
                                            isSyncing = false
                                            val err = com.example.ministoreapp.util.NetworkUtils.getFriendlyErrorMessage(ex)
                                            Toast.makeText(this@MainActivity, "Drive Error: $err", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                } else {
                                    val driveUriStr = prefs.getString("google_drive_file_uri", null)
                                    if (!driveUriStr.isNullOrBlank()) {
                                        val uri = android.net.Uri.parse(driveUriStr)
                                        val remoteJson = try {
                                            contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                                        } catch (e: Exception) { null }

                                        if (!remoteJson.isNullOrBlank()) {
                                            syncRepo.importAndMergeChangeset(this@MainActivity, remoteJson, forceOverwrite = true)
                                        }

                                        val updatedJson = syncRepo.exportChangesetJson(this@MainActivity)
                                        contentResolver.openOutputStream(uri, "wt")?.use { out ->
                                            out.write(updatedJson.toByteArray())
                                        }

                                        isSyncing = false
                                        syncSuccess = true
                                        onToggleDarkTheme(prefs.getBoolean("dark_theme_enabled", false))
                                        Toast.makeText(this@MainActivity, "Sync Successful (via linked Drive File)!", Toast.LENGTH_SHORT).show()
                                        kotlinx.coroutines.delay(2000L)
                                        syncSuccess = false
                                    } else {
                                        isSyncing = false
                                        Toast.makeText(this@MainActivity, "Drive Access Denied. Opening File Picker to link Google Drive...", Toast.LENGTH_LONG).show()
                                        driveFilePickerLauncher.launch(arrayOf("*/*"))
                                    }
                                }
                            } catch (e: Exception) {
                                isSyncing = false
                                e.printStackTrace()
                                Toast.makeText(this@MainActivity, "Sync Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onSettingsClick = {
                        if (settings.pinOnSettingsOpeningEnabled) {
                            showSettingsPinPrompt = true
                        } else {
                            activeSettingsDetailView = null
                            currentTab = TabScreen.Settings
                        }
                    }
                )
            },
            bottomBar = {
                val navContainerColor = if (isDarkTheme) {
                    androidx.compose.ui.graphics.Color(0xFF1B1E1C)
                } else {
                    MaterialTheme.colorScheme.surface
                }

                NavigationBar(
                    containerColor = navContainerColor
                ) {
                    val items = listOf(
                        TabScreen.Search,
                        TabScreen.Events,
                        TabScreen.Credit,
                        TabScreen.Restock,
                        TabScreen.Info
                    )
                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentTab == screen,
                            onClick = {
                                if (screen == TabScreen.Settings) {
                                    if (settings.pinOnSettingsOpeningEnabled && currentTab != TabScreen.Settings) {
                                        showSettingsPinPrompt = true
                                    } else {
                                        activeSettingsDetailView = null
                                        currentTab = TabScreen.Settings
                                    }
                                } else {
                                    currentTab = screen
                                }
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentTab) {
                    TabScreen.Search -> SearchScreen(
                        products = products,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        onAddToOrder = { restockItem ->
                            lifecycleScope.launch {
                                db.restockItemDao().insertRestockItem(restockItem)
                            }
                        }
                    )
                    TabScreen.Events -> EventsScreen(
                        events = events,
                        cashiers = cashiers,
                        onCreateEvent = { newEvent ->
                            lifecycleScope.launch {
                                db.eventDao().insertEvent(newEvent)
                                val descName = newEvent.description.ifBlank { newEvent.transferType ?: newEvent.type }
                                db.auditLogDao().insertAuditLog(
                                    AuditLogEntity(
                                        cashierId = newEvent.cashierId,
                                        cashierName = newEvent.cashierName,
                                        actionType = "LOG_EVENT",
                                        description = "Logged event '$descName' (₱${newEvent.total.roundToInt()})"
                                    )
                                )
                            }
                        },
                        onSettleEvent = { event, cashier, note ->
                            lifecycleScope.launch {
                                val updated = event.copy(
                                    isSettled = true,
                                    settlementCashierId = cashier.id,
                                    settlementCashierName = cashier.name,
                                    settlementNote = note,
                                    updatedAt = System.currentTimeMillis()
                                )
                                db.eventDao().insertEvent(updated)
                                db.auditLogDao().insertAuditLog(
                                    AuditLogEntity(
                                        cashierId = cashier.id,
                                        cashierName = cashier.name,
                                        actionType = "SETTLE_EVENT",
                                        description = "Settled event '${event.description}'"
                                    )
                                )
                            }
                        },
                        onDeleteEvent = { event, cashier ->
                            lifecycleScope.launch {
                                db.eventDao().deleteEvent(event)
                                db.auditLogDao().insertAuditLog(
                                    AuditLogEntity(
                                        cashierId = cashier.id,
                                        cashierName = cashier.name,
                                        actionType = "DELETE_EVENT",
                                        description = "Deleted event '${event.description}'"
                                    )
                                )
                            }
                        }
                    )
                    TabScreen.Credit -> CreditScreen(
                        customers = customers,
                        banRecords = banRecords,
                        cashiers = cashiers,
                        products = products,
                        onAddCustomer = { customer, cashier ->
                            lifecycleScope.launch {
                                db.customerDao().insertCustomer(customer)
                                db.auditLogDao().insertAuditLog(
                                    AuditLogEntity(
                                        cashierId = cashier.id,
                                        cashierName = cashier.name,
                                        actionType = "ADD_CUSTOMER",
                                        description = "Added customer ${customer.name}"
                                    )
                                )
                            }
                        },
                        onAddCredit = { customer, amount, note, borrowedAt, cashier ->
                            lifecycleScope.launch {
                                val updated = customer.copy(balance = customer.balance + amount, updatedAt = System.currentTimeMillis())
                                db.customerDao().insertCustomer(updated)
                                db.creditTransactionDao().insertTransaction(
                                    CreditTransactionEntity(
                                        customerId = customer.id,
                                        totalAmount = amount,
                                        type = "CREDIT_ADD",
                                        notes = note,
                                        borrowedAt = borrowedAt,
                                        cashierId = cashier.id,
                                        cashierName = cashier.name
                                    )
                                )
                                db.auditLogDao().insertAuditLog(
                                    AuditLogEntity(
                                        cashierId = cashier.id,
                                        cashierName = cashier.name,
                                        actionType = "CREDIT_BORROW",
                                        description = "Utang of ₱${amount.roundToInt()} for customer ${customer.name}"
                                    )
                                )
                            }
                        },
                        onMakePayment = { customer, amount, cashier ->
                            lifecycleScope.launch {
                                val updated = customer.copy(balance = (customer.balance - amount).coerceAtLeast(0.0), updatedAt = System.currentTimeMillis())
                                db.customerDao().insertCustomer(updated)
                                db.creditTransactionDao().insertTransaction(
                                    CreditTransactionEntity(
                                        customerId = customer.id,
                                        totalAmount = amount,
                                        type = "PAYMENT_PARTIAL",
                                        cashierId = cashier.id,
                                        cashierName = cashier.name
                                    )
                                )
                                db.auditLogDao().insertAuditLog(
                                    AuditLogEntity(
                                        cashierId = cashier.id,
                                        cashierName = cashier.name,
                                        actionType = "CREDIT_PAYMENT",
                                        description = "Bayad of ₱${amount.roundToInt()} from customer ${customer.name}"
                                    )
                                )
                            }
                        },
                        getTransactionsForCustomerFlow = { customerId ->
                            db.creditTransactionDao().getTransactionsForCustomerFlow(customerId)
                        }
                    )
                    TabScreen.Restock -> RestockScreen(
                        restockItems = restockItems,
                        allProducts = products,
                        onUpdateQuantity = { item, newQty ->
                            lifecycleScope.launch {
                                db.restockItemDao().insertRestockItem(item.copy(quantity = newQty))
                            }
                        },
                        onRemoveItem = { item ->
                            lifecycleScope.launch {
                                db.restockItemDao().deleteRestockItem(item)
                            }
                        },
                        onClearRestock = {
                            lifecycleScope.launch {
                                db.restockItemDao().clearRestockItems()
                            }
                        }
                    )
                    TabScreen.Info -> InfoScreen(
                        securitySettings = securitySettings,
                        priceHistory = priceHistory,
                        vendors = vendors,
                        allProducts = products,
                        banRecords = banRecords,
                        creditTransactions = creditTransactions,
                        customers = customers,
                        wholesaleProducts = products.filter { it.isWholesaleParent }
                    )
                    TabScreen.Settings -> SettingsScreen(
                        activeDetailView = activeSettingsDetailView,
                        onActiveDetailViewChange = { activeSettingsDetailView = it },
                        securitySettings = securitySettings,
                        products = products,
                        productGroups = productGroups,
                        cashiers = cashiers,
                        auditLogs = auditLogs,
                        banRecords = banRecords,
                        categories = categories,
                        brands = brands,
                        units = units,
                        containers = containers,
                        wholesaleUnits = wholesaleUnits,
                        vendors = vendors,
                        isDarkTheme = isDarkTheme,
                        onToggleDarkTheme = onToggleDarkTheme,
                        googleAccountEmail = googleAccountEmail,
                        onSwitchGoogleAccount = {
                            pendingGoogleSignInCallback = { _ -> }
                            googleSignInClient.signOut().addOnCompleteListener {
                                googleSignInLauncher.launch(googleSignInClient.signInIntent)
                            }
                        },
                        onSaveProductGroup = { group, generatedItems, cashier ->
                            lifecycleScope.launch {
                                db.productGroupDao().insertGroup(group)

                                val oldProducts = db.productDao().getAllProducts().filter { it.groupId == group.id }
                                oldProducts.forEach { old ->
                                    val priceDiff = generatedItems.find { it.comboKey == old.comboKey }?.sellingPrice?.minus(old.sellingPrice) ?: 0.0
                                    if (kotlin.math.abs(priceDiff) >= 0.01) {
                                        db.priceHistoryDao().insertPriceHistory(
                                            PriceHistoryEntity(
                                                productId = old.id,
                                                productName = old.name,
                                                oldPrice = old.sellingPrice,
                                                newPrice = old.sellingPrice + priceDiff,
                                                cashierId = cashier.id,
                                                cashierName = cashier.name
                                            )
                                        )
                                    }
                                }

                                generatedItems.forEach { db.productDao().insertProduct(it) }

                                val displayTitle = formatGroupDisplayTitle(group.baseName, group.variants, group.unitSize, group.containerType)
                                db.auditLogDao().insertAuditLog(
                                    AuditLogEntity(
                                        cashierId = cashier.id,
                                        cashierName = cashier.name,
                                        actionType = "SAVE_PRODUCT_RECIPE",
                                        description = "Saved product recipe '$displayTitle' (${generatedItems.size} items generated)"
                                    )
                                )
                            }
                        },
                        onDeleteSingleProduct = { product, cashier ->
                            lifecycleScope.launch {
                                db.productDao().deleteProduct(product)
                                db.auditLogDao().insertAuditLog(
                                    AuditLogEntity(
                                        cashierId = cashier.id,
                                        cashierName = cashier.name,
                                        actionType = "DELETE_PRODUCT",
                                        description = "Deleted item '${product.name}'"
                                    )
                                )
                            }
                        },
                        onDeleteProductGroup = { groupId, cashier ->
                            lifecycleScope.launch {
                                val groupObj = db.productGroupDao().getGroupById(groupId)
                                val groupProducts = db.productDao().getAllProducts().filter { it.groupId == groupId }
                                groupProducts.forEach { db.productDao().deleteProduct(it) }
                                if (groupObj != null) {
                                    db.productGroupDao().deleteGroup(groupObj)
                                    val displayTitle = formatGroupDisplayTitle(groupObj.baseName, groupObj.variants, groupObj.unitSize, groupObj.containerType)
                                    db.auditLogDao().insertAuditLog(
                                        AuditLogEntity(
                                            cashierId = cashier.id,
                                            cashierName = cashier.name,
                                            actionType = "DELETE_PRODUCT_RECIPE",
                                            description = "Deleted product recipe '$displayTitle'"
                                        )
                                    )
                                }
                            }
                        },
                        onSaveCashier = { cashier, performerName, reason ->
                            lifecycleScope.launch {
                                db.cashierDao().insertCashier(cashier)
                                db.auditLogDao().insertAuditLog(
                                    AuditLogEntity(
                                        cashierId = cashier.id,
                                        cashierName = performerName,
                                        actionType = "SAVE_CASHIER",
                                        description = "Saved cashier '${cashier.name}' (Note: $reason)"
                                    )
                                )
                            }
                        },
                        onDeleteCashier = { cashier, performerName, reason ->
                            lifecycleScope.launch {
                                db.cashierDao().deleteCashier(cashier)
                                db.auditLogDao().insertAuditLog(
                                    AuditLogEntity(
                                        cashierId = cashier.id,
                                        cashierName = performerName,
                                        actionType = "DELETE_CASHIER",
                                        description = "Deleted cashier '${cashier.name}' (Reason: $reason)"
                                    )
                                )
                            }
                        },
                        onSaveBanRecord = { ban, cashier ->
                            lifecycleScope.launch {
                                db.banRecordDao().insertBanRecord(ban)
                                db.auditLogDao().insertAuditLog(
                                    AuditLogEntity(
                                        cashierId = cashier.id,
                                        cashierName = cashier.name,
                                        actionType = "ADD_BAN_RECORD",
                                        description = "Added credit ban record for ${ban.fullName}"
                                    )
                                )
                            }
                        },
                        onDeleteBanRecord = { ban, cashier ->
                            lifecycleScope.launch {
                                db.banRecordDao().deleteBanRecord(ban)
                                db.auditLogDao().insertAuditLog(
                                    AuditLogEntity(
                                        cashierId = cashier.id,
                                        cashierName = cashier.name,
                                        actionType = "DELETE_BAN_RECORD",
                                        description = "Deleted credit ban record for ${ban.fullName}"
                                    )
                                )
                            }
                        },
                        onSaveMasterData = { type, name ->
                            lifecycleScope.launch {
                                when (type) {
                                    "Category" -> db.masterDataDao().insertCategory(com.example.ministoreapp.data.local.entities.CategoryEntity(name = name))
                                    "Brand" -> db.masterDataDao().insertBrand(com.example.ministoreapp.data.local.entities.BrandEntity(name = name))
                                    "Unit" -> db.masterDataDao().insertUnit(com.example.ministoreapp.data.local.entities.UnitEntity(name = name))
                                    "Container" -> db.masterDataDao().insertContainer(com.example.ministoreapp.data.local.entities.ContainerEntity(name = name))
                                    "WholesaleUnit" -> db.masterDataDao().insertWholesaleUnit(com.example.ministoreapp.data.local.entities.WholesaleUnitEntity(name = name))
                                }
                            }
                        },
                        onDeleteMasterData = { type, id ->
                            lifecycleScope.launch {
                                when (type) {
                                    "Category" -> db.masterDataDao().deleteCategory(com.example.ministoreapp.data.local.entities.CategoryEntity(id = id, name = ""))
                                    "Brand" -> db.masterDataDao().deleteBrand(com.example.ministoreapp.data.local.entities.BrandEntity(id = id, name = ""))
                                    "Unit" -> db.masterDataDao().deleteUnit(com.example.ministoreapp.data.local.entities.UnitEntity(id = id, name = ""))
                                    "Container" -> db.masterDataDao().deleteContainer(com.example.ministoreapp.data.local.entities.ContainerEntity(id = id, name = ""))
                                    "WholesaleUnit" -> db.masterDataDao().deleteWholesaleUnit(com.example.ministoreapp.data.local.entities.WholesaleUnitEntity(id = id, name = ""))
                                }
                            }
                        },
                        onUpdateSecuritySettings = { updatedSettings ->
                            lifecycleScope.launch {
                                db.securitySettingsDao().insertOrUpdate(updatedSettings)
                            }
                        },
                        onSaveVendor = { vendor ->
                            lifecycleScope.launch {
                                db.masterDataDao().insertVendor(vendor)
                            }
                        },
                        onDeleteVendor = { vendor ->
                            lifecycleScope.launch {
                                db.masterDataDao().deleteVendor(vendor)
                            }
                        },
                        onExportBackupJson = {
                            kotlinx.coroutines.runBlocking {
                                syncRepo.exportChangesetJson(this@MainActivity)
                            }
                        },
                        onImportBackupJson = { jsonStr ->
                            lifecycleScope.launch {
                                val result = syncRepo.importAndMergeChangeset(this@MainActivity, jsonStr, forceOverwrite = true)
                                if (result.isSuccess) {
                                    onToggleDarkTheme(prefs.getBoolean("dark_theme_enabled", false))
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
