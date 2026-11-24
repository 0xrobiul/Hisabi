package me.robiul.hisabi

import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import me.robiul.hisabi.data.local.AccountEntity
import me.robiul.hisabi.data.local.TransactionEntity
import me.robiul.hisabi.data.local.TransactionType
import me.robiul.hisabi.ui.components.AccountEditorSheet
import me.robiul.hisabi.ui.components.TransactionDetailSheet
import me.robiul.hisabi.ui.screens.AccountDetailScreen
import me.robiul.hisabi.ui.screens.AccountsManagerScreen
import me.robiul.hisabi.ui.screens.ActivityScreen
import me.robiul.hisabi.ui.screens.AddTransactionScreen
import me.robiul.hisabi.ui.screens.BudgetsScreen
import me.robiul.hisabi.ui.screens.CategoriesScreen
import me.robiul.hisabi.ui.screens.InsightsScreen
import me.robiul.hisabi.ui.screens.LoansScreen
import me.robiul.hisabi.ui.screens.LockScreen
import me.robiul.hisabi.ui.screens.RecurringScreen
import me.robiul.hisabi.ui.screens.SavingsGoalsScreen
import me.robiul.hisabi.ui.screens.SettingsScreen
import me.robiul.hisabi.ui.screens.WalletHomeScreen
import me.robiul.hisabi.ui.theme.CardVaultTheme
import me.robiul.hisabi.ui.theme.ChampagneGold
import me.robiul.hisabi.ui.theme.DarkBgPrimary
import me.robiul.hisabi.ui.viewmodel.LedgerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val ROUTE_HOME = "home"
const val ROUTE_ACTIVITY = "activity"
const val ROUTE_ADD = "add_transaction"
const val ROUTE_INSIGHTS = "insights"
const val ROUTE_SETTINGS = "settings"

const val ROUTE_ACCOUNTS = "accounts"
const val ROUTE_ACCOUNT_DETAIL = "account_detail/{accountId}"
const val ROUTE_EDIT_TX = "edit_transaction/{txId}"
const val ROUTE_BUDGETS = "budgets"
const val ROUTE_GOALS = "goals"
const val ROUTE_RECURRING = "recurring"
const val ROUTE_CATEGORIES = "categories"
const val ROUTE_LOANS = "loans"

private data class BottomTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
    val isPrimaryAdd: Boolean = false,
)

private val PrimaryTabs = listOf(
    BottomTab(ROUTE_HOME, "Home", Icons.Rounded.Home, Icons.Outlined.Home, "nav_home"),
    BottomTab(
        ROUTE_ACTIVITY,
        "Activity",
        Icons.AutoMirrored.Rounded.ReceiptLong,
        Icons.AutoMirrored.Outlined.ReceiptLong,
        "nav_activity",
    ),
    BottomTab(ROUTE_ADD, "Add", Icons.Rounded.Add, Icons.Rounded.Add, "nav_add", isPrimaryAdd = true),
    BottomTab(ROUTE_INSIGHTS, "Insights", Icons.Rounded.Insights, Icons.Outlined.Insights, "nav_insights"),
    BottomTab(ROUTE_SETTINGS, "Settings", Icons.Rounded.Settings, Icons.Outlined.Settings, "nav_settings"),
)

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    private val ledgerViewModel: LedgerViewModel by viewModels()
    private var skipNextBackgroundRelock = false
    private var externalPickerStartedAtMs = 0L

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (!isChangingConfigurations && !skipNextBackgroundRelock) {
            ledgerViewModel.lockSession()
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations && !skipNextBackgroundRelock) {
            ledgerViewModel.lockSession()
        }
    }

    override fun onResume() {
        super.onResume()
        if (skipNextBackgroundRelock &&
            SystemClock.elapsedRealtime() - externalPickerStartedAtMs > 120_000L
        ) {
            ledgerViewModel.lockSession()
        }
        skipNextBackgroundRelock = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel = ledgerViewModel
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            val darkOverride = when (state.preferences.darkThemePreference) {
                1 -> true
                2 -> false
                else -> null
            }

            val lifecycleOwner = LocalLifecycleOwner.current
            var isAppForeground by remember {
                mutableStateOf(
                    lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
                )
            }
            var biometricPromptAttemptedForCurrentLock by remember { mutableStateOf(false) }
            var biometricFeedback by remember { mutableStateOf<String?>(null) }

            val hasPin = !state.preferences.pinHash.isNullOrBlank()
            val needsLock = !state.isLoading && hasPin && !viewModel.isSessionUnlocked
            val shouldSecureWindow = state.isLoading || hasPin

            SideEffect {
                if (shouldSecureWindow) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_START, Lifecycle.Event.ON_RESUME -> {
                            isAppForeground = true
                        }
                        Lifecycle.Event.ON_STOP -> {
                            isAppForeground = false
                            biometricPromptAttemptedForCurrentLock = false
                            biometricFeedback = null
                        }
                        else -> Unit
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            CardVaultTheme(darkThemeOverride = darkOverride) {
                when {
                    state.isLoading -> {
                        // Never expose balances or account info before DataStore finishes loading lock state.
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(DarkBgPrimary),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = "Checking vault security",
                                tint = ChampagneGold,
                                modifier = Modifier.size(36.dp),
                            )
                        }
                    }

                    needsLock -> {
                        val promptBiometric = remember(this) {
                            {
                                val executor = ContextCompat.getMainExecutor(this)
                                val prompt = BiometricPrompt(
                                    this,
                                    executor,
                                    object : BiometricPrompt.AuthenticationCallback() {
                                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                            biometricFeedback = null
                                            biometricPromptAttemptedForCurrentLock = false
                                            viewModel.unlockSession()
                                        }

                                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                            // Strict lock enforcement: cancellation or error NEVER bypasses LockScreen.
                                            biometricFeedback = when (errorCode) {
                                                BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                                                BiometricPrompt.ERROR_USER_CANCELED,
                                                BiometricPrompt.ERROR_CANCELED ->
                                                    "Biometric cancelled. Enter your 4-digit PIN to unlock."
                                                BiometricPrompt.ERROR_LOCKOUT,
                                                BiometricPrompt.ERROR_LOCKOUT_PERMANENT ->
                                                    "Biometric locked by system. Enter your 4-digit PIN."
                                                else -> "Authentication required. Enter your 4-digit PIN."
                                            }
                                        }

                                        override fun onAuthenticationFailed() {
                                            biometricFeedback = "Biometric not recognized. Try again or enter PIN."
                                        }
                                    },
                                )
                                val info = BiometricPrompt.PromptInfo.Builder()
                                    .setTitle("Unlock Hisabi Vault")
                                    .setSubtitle("Verify your identity to view balances")
                                    .setAllowedAuthenticators(
                                        BiometricManager.Authenticators.BIOMETRIC_STRONG or
                                            BiometricManager.Authenticators.BIOMETRIC_WEAK
                                    )
                                    .setNegativeButtonText("Use PIN")
                                    .build()
                                prompt.authenticate(info)
                            }
                        }

                        LaunchedEffect(needsLock, isAppForeground, state.preferences.biometricEnabled) {
                            if (needsLock &&
                                isAppForeground &&
                                state.preferences.biometricEnabled &&
                                !biometricPromptAttemptedForCurrentLock
                            ) {
                                biometricPromptAttemptedForCurrentLock = true
                                promptBiometric()
                            }
                        }

                        LockScreen(
                            storedPinHash = state.preferences.pinHash!!,
                            biometricEnabled = state.preferences.biometricEnabled,
                            externalStatusMessage = biometricFeedback,
                            onClearExternalStatus = { biometricFeedback = null },
                            onUnlocked = {
                                biometricFeedback = null
                                biometricPromptAttemptedForCurrentLock = false
                                viewModel.unlockSession()
                            },
                            onTriggerBiometric = {
                                biometricFeedback = null
                                promptBiometric()
                            },
                            onExitApp = {
                                moveTaskToBack(true)
                            },
                        )
                    }

                    else -> {
                        CardVaultApp(
                            viewModel = viewModel,
                            onBeforeExternalPicker = {
                                externalPickerStartedAtMs = SystemClock.elapsedRealtime()
                                skipNextBackgroundRelock = true
                            },
                            onExternalPickerFinished = { skipNextBackgroundRelock = false },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CardVaultApp(
    viewModel: LedgerViewModel,
    onBeforeExternalPicker: () -> Unit = {},
    onExternalPickerFinished: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var showAccountSheet by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var inspectingTx by remember { mutableStateOf<TransactionEntity?>(null) }

    var pendingAddType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var pendingAddAccountId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        onExternalPickerFinished()
        if (uri != null) {
            scope.launch {
                val csv = viewModel.generateCsv()
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(csv.toByteArray(Charsets.UTF_8))
                        }
                    }.isSuccess
                }
                snackbarHostState.showSnackbar(if (ok) "CSV exported" else "Failed to export CSV")
            }
        }
    }

    val exportPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        onExternalPickerFinished()
        if (uri != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            viewModel.writePdfReport(out)
                        }
                    }.isSuccess
                }
                snackbarHostState.showSnackbar(if (ok) "PDF report exported" else "Failed to export PDF")
            }
        }
    }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        onExternalPickerFinished()
        if (uri != null) {
            scope.launch {
                val json = viewModel.generateBackupJson()
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(json.toByteArray(Charsets.UTF_8))
                        }
                    }.isSuccess
                }
                snackbarHostState.showSnackbar(if (ok) "Offline backup saved" else "Failed to save backup")
            }
        }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        onExternalPickerFinished()
        if (uri != null) {
            scope.launch {
                val json = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    }.getOrNull()
                }
                if (json != null) viewModel.restoreBackupJson(json)
                else snackbarHostState.showSnackbar("Failed to read backup file")
            }
        }
    }

    if (showAccountSheet) {
        AccountEditorSheet(
            existingAccount = editingAccount,
            currencySymbol = state.preferences.currencySymbol,
            onDismiss = {
                showAccountSheet = false
                editingAccount = null
            },
            onSave = { account ->
                viewModel.saveAccount(account)
                showAccountSheet = false
                editingAccount = null
            },
            onArchiveToggle = { account ->
                viewModel.setAccountArchived(account.id, !account.isArchived)
                showAccountSheet = false
                editingAccount = null
            },
            onDelete = { accId ->
                viewModel.deleteAccount(accId)
                showAccountSheet = false
                editingAccount = null
            },
        )
    }

    inspectingTx?.let { tx ->
        val accountMap = remember(state.accounts) { state.accounts.associateBy { it.id } }
        val categoryMap = remember(state.categories) { state.categories.associateBy { it.id } }
        TransactionDetailSheet(
            transaction = tx,
            accountMap = accountMap,
            categoryMap = categoryMap,
            currencySymbol = state.preferences.currencySymbol,
            datePattern = state.preferences.dateFormatPattern,
            hideBalances = state.preferences.hideBalances,
            onDismiss = { inspectingTx = null },
            onEdit = { target ->
                inspectingTx = null
                navController.navigate("edit_transaction/${target.id}")
            },
            onDuplicate = { target ->
                inspectingTx = null
                viewModel.duplicateTransaction(target.id)
            },
            onDelete = { id ->
                inspectingTx = null
                viewModel.deleteTransaction(id)
            },
        )
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val isTopTab = PrimaryTabs.any { tab -> currentDestination?.hierarchy?.any { it.route == tab.route } == true }

    val navigateToTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val openQuickAdd: (TransactionType, Long?) -> Unit = { type, accId ->
        pendingAddType = type
        pendingAddAccountId = accId
        navigateToTab(ROUTE_ADD)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 700.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useNavRail && isTopTab) {
                    val navItemColors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Column(modifier = Modifier.fillMaxWidth()) {
                        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        NavigationBar(
                            windowInsets = WindowInsets.navigationBars,
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 0.dp,
                        ) {
                            PrimaryTabs.forEach { tab ->
                                val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                                if (tab.isPrimaryAdd) {
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = {
                                            pendingAddType = TransactionType.EXPENSE
                                            pendingAddAccountId = null
                                            navigateToTab(tab.route)
                                        },
                                        icon = {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primary)
                                                    .border(
                                                        width = if (selected) 2.dp else 0.dp,
                                                        color = if (selected) MaterialTheme.colorScheme.onBackground else Color.Transparent,
                                                        shape = CircleShape,
                                                    ),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Add,
                                                    contentDescription = "Add Transaction",
                                                    tint = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.size(25.dp),
                                                )
                                            }
                                        },
                                        label = {
                                            Text(
                                                text = tab.label,
                                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = Color.Transparent,
                                            unselectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        ),
                                        modifier = Modifier.testTag(tab.testTag),
                                    )
                                } else {
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { navigateToTab(tab.route) },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = tab.label,
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = tab.label,
                                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                            )
                                        },
                                        colors = navItemColors,
                                        modifier = Modifier.testTag(tab.testTag),
                                    )
                                }
                            }
                        }
                    }
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (useNavRail && isTopTab) {
                    val railColors = NavigationRailItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    NavigationRail(
                        modifier = Modifier.fillMaxHeight(),
                        containerColor = MaterialTheme.colorScheme.surface,
                    ) {
                        PrimaryTabs.forEach { tab ->
                            val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    if (tab.isPrimaryAdd) {
                                        pendingAddType = TransactionType.EXPENSE
                                        pendingAddAccountId = null
                                    }
                                    navigateToTab(tab.route)
                                },
                                icon = {
                                    if (tab.isPrimaryAdd) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Add,
                                                contentDescription = tab.label,
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label,
                                        )
                                    }
                                },
                                label = { Text(tab.label) },
                                colors = railColors,
                                modifier = Modifier.testTag(tab.testTag),
                            )
                        }
                    }
                    VerticalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_HOME,
                        enterTransition = { fadeIn(animationSpec = tween(220)) },
                        exitTransition = { fadeOut(animationSpec = tween(180)) },
                        popEnterTransition = { fadeIn(animationSpec = tween(220)) },
                        popExitTransition = { fadeOut(animationSpec = tween(180)) },
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 780.dp),
                    ) {
                        composable(ROUTE_HOME) {
                            WalletHomeScreen(
                                state = state,
                                onToggleHideBalances = { viewModel.toggleHideBalances() },
                                onSelectPeriod = { viewModel.setDashboardPeriod(it) },
                                onOpenAddTransaction = { type -> openQuickAdd(type, null) },
                                onOpenAddAccount = {
                                    editingAccount = null
                                    showAccountSheet = true
                                },
                                onOpenAccounts = { navController.navigate(ROUTE_ACCOUNTS) },
                                onOpenAccountDetail = { id -> navController.navigate("account_detail/$id") },
                                onOpenActivity = { navigateToTab(ROUTE_ACTIVITY) },
                                onOpenBudgets = { navController.navigate(ROUTE_BUDGETS) },
                                onOpenGoals = { navController.navigate(ROUTE_GOALS) },
                                onOpenRecurring = { navController.navigate(ROUTE_RECURRING) },
                                onOpenLoans = { navController.navigate(ROUTE_LOANS) },
                                onClickTransaction = { inspectingTx = it },
                            )
                        }

                        composable(ROUTE_ACTIVITY) {
                            ActivityScreen(
                                state = state,
                                onSelectTransaction = { inspectingTx = it },
                            )
                        }

                        composable(ROUTE_ADD) {
                            AddTransactionScreen(
                                state = state,
                                initialType = pendingAddType,
                                initialAccountId = pendingAddAccountId,
                                existingTransaction = null,
                                onBeforeExternalPicker = onBeforeExternalPicker,
                                onCopyReceiptUri = { uri ->
                                    onExternalPickerFinished()
                                    viewModel.copyReceiptImage(uri)
                                },
                                onSaveTransaction = { tx ->
                                    viewModel.saveTransaction(tx)
                                    navigateToTab(ROUTE_HOME)
                                },
                                onSaveRecurringRule = { rule -> viewModel.saveRecurringRule(rule) },
                                onOpenCategories = { navController.navigate(ROUTE_CATEGORIES) },
                                onOpenAddAccount = {
                                    editingAccount = null
                                    showAccountSheet = true
                                },
                            )
                        }

                        composable(
                            route = ROUTE_EDIT_TX,
                            arguments = listOf(navArgument("txId") { type = NavType.LongType }),
                        ) { entry ->
                            val txId = entry.arguments?.getLong("txId") ?: 0L
                            val targetTx = state.transactions.firstOrNull { it.id == txId }
                            AddTransactionScreen(
                                state = state,
                                existingTransaction = targetTx,
                                onBeforeExternalPicker = onBeforeExternalPicker,
                                onCopyReceiptUri = { uri ->
                                    onExternalPickerFinished()
                                    viewModel.copyReceiptImage(uri)
                                },
                                onSaveTransaction = { tx ->
                                    viewModel.saveTransaction(tx)
                                    navController.popBackStack()
                                },
                                onSaveRecurringRule = { rule -> viewModel.saveRecurringRule(rule) },
                                onOpenCategories = { navController.navigate(ROUTE_CATEGORIES) },
                                onOpenAddAccount = {
                                    editingAccount = null
                                    showAccountSheet = true
                                },
                                onNavigateBack = { navController.popBackStack() },
                            )
                        }

                        composable(ROUTE_INSIGHTS) {
                            InsightsScreen(
                                state = state,
                                onSelectGranularity = { viewModel.setInsightsGranularity(it) },
                                onShiftMonth = { viewModel.shiftInsightsMonth(it) },
                                onExportPdf = {
                                    onBeforeExternalPicker()
                                    exportPdfLauncher.launch("hisabi_report.pdf")
                                },
                                onExportCsv = {
                                    onBeforeExternalPicker()
                                    exportCsvLauncher.launch("hisabi_transactions.csv")
                                },
                                onOpenBudgets = { navController.navigate(ROUTE_BUDGETS) },
                                onOpenGoals = { navController.navigate(ROUTE_GOALS) },
                                onOpenRecurring = { navController.navigate(ROUTE_RECURRING) },
                                onOpenCategories = { navController.navigate(ROUTE_CATEGORIES) },
                                onOpenLoans = { navController.navigate(ROUTE_LOANS) },
                            )
                        }

                        composable(ROUTE_SETTINGS) {
                            SettingsScreen(
                                state = state,
                                onSetThemeMode = { viewModel.setThemeMode(it) },
                                onSetCurrency = { viewModel.setCurrencySymbol(it) },
                                onSetDateFormat = { viewModel.setDateFormat(it) },
                                onSetFirstDayOfMonth = { viewModel.setFirstDayOfMonth(it) },
                                onToggleHideBalances = { viewModel.toggleHideBalances() },
                                onSetDefaultAccount = { viewModel.setDefaultAccountId(it) },
                                onSetDefaultExpenseCategory = { viewModel.setDefaultExpenseCategoryId(it) },
                                onSetNotificationsEnabled = { viewModel.setNotificationsEnabled(it) },
                                onSetBudgetAlertsEnabled = { viewModel.setBudgetAlertsEnabled(it) },
                                onSetRecurringRemindersEnabled = { viewModel.setRecurringRemindersEnabled(it) },
                                onSetPin = { viewModel.setPin(it) },
                                onSetBiometricEnabled = { viewModel.setBiometricEnabled(it) },
                                onLockNow = { viewModel.lockSession() },
                                onOpenAccounts = { navController.navigate(ROUTE_ACCOUNTS) },
                                onOpenCategories = { navController.navigate(ROUTE_CATEGORIES) },
                                onBackupJson = {
                                    onBeforeExternalPicker()
                                    exportBackupLauncher.launch("hisabi_backup.json")
                                },
                                onRestoreJson = {
                                    onBeforeExternalPicker()
                                    importBackupLauncher.launch(arrayOf("application/json", "*/*"))
                                },
                                onExportCsv = {
                                    onBeforeExternalPicker()
                                    exportCsvLauncher.launch("hisabi_transactions.csv")
                                },
                                onExportPdf = {
                                    onBeforeExternalPicker()
                                    exportPdfLauncher.launch("hisabi_report.pdf")
                                },
                            )
                        }

                        composable(
                            route = ROUTE_ACCOUNTS,
                            enterTransition = { slideInHorizontally(tween(240)) { it / 5 } + fadeIn(tween(220)) },
                            popExitTransition = { slideOutHorizontally(tween(200)) { it / 5 } + fadeOut(tween(180)) },
                        ) {
                            AccountsManagerScreen(
                                state = state,
                                onNavigateBack = { navController.popBackStack() },
                                onAddAccount = {
                                    editingAccount = null
                                    showAccountSheet = true
                                },
                                onOpenAccountDetail = { id -> navController.navigate("account_detail/$id") },
                                onOpenTransfer = { fromId -> openQuickAdd(TransactionType.TRANSFER, fromId) },
                            )
                        }

                        composable(
                            route = ROUTE_ACCOUNT_DETAIL,
                            arguments = listOf(navArgument("accountId") { type = NavType.LongType }),
                            enterTransition = { slideInHorizontally(tween(240)) { it / 5 } + fadeIn(tween(220)) },
                            popExitTransition = { slideOutHorizontally(tween(200)) { it / 5 } + fadeOut(tween(180)) },
                        ) { entry ->
                            val accId = entry.arguments?.getLong("accountId") ?: 0L
                            AccountDetailScreen(
                                accountId = accId,
                                state = state,
                                onNavigateBack = { navController.popBackStack() },
                                onEditAccount = { acc ->
                                    editingAccount = acc
                                    showAccountSheet = true
                                },
                                onAdjustBalance = { id, target, note ->
                                    viewModel.adjustAccountBalance(id, target, note)
                                },
                                onTransferFromAccount = { fromId -> openQuickAdd(TransactionType.TRANSFER, fromId) },
                                onSelectTransaction = { inspectingTx = it },
                            )
                        }

                        composable(
                            route = ROUTE_BUDGETS,
                            enterTransition = { slideInHorizontally(tween(240)) { it / 5 } + fadeIn(tween(220)) },
                            popExitTransition = { slideOutHorizontally(tween(200)) { it / 5 } + fadeOut(tween(180)) },
                        ) {
                            BudgetsScreen(
                                state = state,
                                onNavigateBack = { navController.popBackStack() },
                                onSaveBudget = { viewModel.saveBudget(it) },
                                onDeleteBudget = { viewModel.deleteBudget(it) },
                            )
                        }

                        composable(
                            route = ROUTE_GOALS,
                            enterTransition = { slideInHorizontally(tween(240)) { it / 5 } + fadeIn(tween(220)) },
                            popExitTransition = { slideOutHorizontally(tween(200)) { it / 5 } + fadeOut(tween(180)) },
                        ) {
                            SavingsGoalsScreen(
                                state = state,
                                onNavigateBack = { navController.popBackStack() },
                                onSaveGoal = { viewModel.saveGoal(it) },
                                onDeleteGoal = { viewModel.deleteGoal(it) },
                                onContribute = { gId, aId, amt, note -> viewModel.contributeToGoal(gId, aId, amt, note) },
                                onWithdraw = { gId, aId, amt, note -> viewModel.withdrawFromGoal(gId, aId, amt, note) },
                            )
                        }

                        composable(
                            route = ROUTE_RECURRING,
                            enterTransition = { slideInHorizontally(tween(240)) { it / 5 } + fadeIn(tween(220)) },
                            popExitTransition = { slideOutHorizontally(tween(200)) { it / 5 } + fadeOut(tween(180)) },
                        ) {
                            RecurringScreen(
                                state = state,
                                onNavigateBack = { navController.popBackStack() },
                                onSaveRule = { viewModel.saveRecurringRule(it) },
                                onTogglePause = { viewModel.toggleRecurringPause(it) },
                                onSkipNext = { viewModel.skipNextRecurring(it) },
                                onDeleteRule = { viewModel.deleteRecurringRule(it) },
                                onProcessDueNow = { viewModel.processDueRecurringNow() },
                            )
                        }

                        composable(
                            route = ROUTE_CATEGORIES,
                            enterTransition = { slideInHorizontally(tween(240)) { it / 5 } + fadeIn(tween(220)) },
                            popExitTransition = { slideOutHorizontally(tween(200)) { it / 5 } + fadeOut(tween(180)) },
                        ) {
                            CategoriesScreen(
                                state = state,
                                onNavigateBack = { navController.popBackStack() },
                                onSaveCategory = { viewModel.saveCategory(it) },
                                onDeleteCategory = { viewModel.deleteCategory(it) },
                            )
                        }

                        composable(
                            route = ROUTE_LOANS,
                            enterTransition = { slideInHorizontally(tween(240)) { it / 5 } + fadeIn(tween(220)) },
                            popExitTransition = { slideOutHorizontally(tween(200)) { it / 5 } + fadeOut(tween(180)) },
                        ) {
                            LoansScreen(
                                state = state,
                                onNavigateBack = { navController.popBackStack() },
                                onSaveLoan = { viewModel.saveLoanRecord(it) },
                                onDeleteLoan = { viewModel.deleteLoanRecord(it) },
                                onSaveRepayment = { viewModel.saveLoanRepayment(it) },
                                onDeleteRepayment = { viewModel.deleteLoanRepayment(it) },
                            )
                        }
                    }
                }
            }
        }
    }
}
