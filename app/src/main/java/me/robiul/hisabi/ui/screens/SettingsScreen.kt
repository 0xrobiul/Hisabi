package me.robiul.hisabi.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import me.robiul.hisabi.BuildConfig
import me.robiul.hisabi.R
import me.robiul.hisabi.ui.theme.AccentGreen
import me.robiul.hisabi.ui.theme.ChampagneGold
import me.robiul.hisabi.ui.theme.InterFontFamily
import me.robiul.hisabi.ui.viewmodel.LedgerUiState
import me.robiul.hisabi.util.InputValidation

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    state: LedgerUiState,
    onSetThemeMode: (Int) -> Unit,
    onSetCurrency: (String) -> Unit,
    onSetDateFormat: (String) -> Unit,
    onSetFirstDayOfMonth: (Int) -> Unit,
    onToggleHideBalances: () -> Unit,
    onSetDefaultAccount: (Long?) -> Unit,
    onSetDefaultExpenseCategory: (Long?) -> Unit,
    onSetNotificationsEnabled: (Boolean) -> Unit,
    onSetBudgetAlertsEnabled: (Boolean) -> Unit,
    onSetRecurringRemindersEnabled: (Boolean) -> Unit,
    onSetPin: (String?) -> Unit,
    onSetBiometricEnabled: (Boolean) -> Unit,
    onLockNow: () -> Unit = {},
    onOpenAccounts: () -> Unit,
    onOpenCategories: () -> Unit,
    onBackupJson: () -> Unit,
    onRestoreJson: () -> Unit,
    onExportCsv: () -> Unit,
    onExportPdf: () -> Unit,
) {
    val prefs = state.preferences
    val context = LocalContext.current
    var showPinDialog by remember { mutableStateOf(false) }

    val biometricAvailable = remember(context) {
        BiometricManager.from(context).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        onSetNotificationsEnabled(granted)
    }

    if (showPinDialog) {
        var pinInput by remember { mutableStateOf("") }
        var confirmPinInput by remember { mutableStateOf("") }
        var pinTouched by remember { mutableStateOf(false) }
        var confirmTouched by remember { mutableStateOf(false) }
        val (pinErrorRaw, confirmErrorRaw) = InputValidation.validatePinPair(pinInput, confirmPinInput)
        val pinError = if (pinTouched && pinInput.length < 4) "PIN must be 4 digits" else pinErrorRaw
        val confirmError = if (confirmTouched && confirmPinInput.isEmpty()) {
            "Confirm your 4-digit PIN"
        } else confirmErrorRaw
        val valid = pinInput.length == 4 && pinInput.all { it.isDigit() } && pinInput == confirmPinInput

        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text(if (prefs.pinHash == null) "Set 4-Digit Vault PIN" else "Update Vault PIN") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = {
                            pinTouched = true
                            pinInput = InputValidation.sanitizeDigitsOnly(it, maxLength = 4)
                        },
                        label = { Text("Enter 4-digit PIN") },
                        isError = pinError != null,
                        supportingText = pinError?.let { msg -> { Text(msg) } },
                        shape = RoundedCornerShape(16.dp),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("settings_pin_input"),
                    )
                    OutlinedTextField(
                        value = confirmPinInput,
                        onValueChange = {
                            confirmTouched = true
                            confirmPinInput = InputValidation.sanitizeDigitsOnly(it, maxLength = 4)
                        },
                        label = { Text("Confirm 4-digit PIN") },
                        isError = confirmError != null,
                        supportingText = confirmError?.let { msg -> { Text(msg) } },
                        shape = RoundedCornerShape(16.dp),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("settings_pin_confirm_input"),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        pinTouched = true
                        confirmTouched = true
                        if (!valid) return@Button
                        onSetPin(pinInput)
                        showPinDialog = false
                    },
                    enabled = valid,
                    modifier = Modifier.testTag("settings_pin_save_button"),
                ) { Text("Save PIN", maxLines = 1, softWrap = false) }
            },
            dismissButton = {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (prefs.pinHash != null) {
                        TextButton(
                            onClick = {
                                onSetPin(null)
                                showPinDialog = false
                            }
                        ) { Text("Remove PIN", maxLines = 1, softWrap = false) }
                    }
                    TextButton(onClick = { showPinDialog = false }) { Text("Cancel", maxLines = 1, softWrap = false) }
                }
            },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column {
                Text("Settings", style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = "Theme, currency, billing cycle, security & offline backups",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Appearance & Localization
        item {
            SettingsSection(title = "Appearance & Currency") {
                Text("Theme", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1 to "Dark", 2 to "Light", 0 to "System").forEach { (mode, label) ->
                        FilterChip(
                            selected = prefs.darkThemePreference == mode,
                            onClick = { onSetThemeMode(mode) },
                            label = { Text(label) },
                            modifier = Modifier.testTag("settings_theme_${label.lowercase()}"),
                        )
                    }
                }

                Text("Currency Symbol", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf("৳", "$", "€", "£", "₹").forEach { sym ->
                        FilterChip(
                            selected = prefs.currencySymbol == sym,
                            onClick = { onSetCurrency(sym) },
                            label = { Text(sym) },
                        )
                    }
                }

                Text("Date Format", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf("dd MMM yyyy", "MM/dd/yyyy", "yyyy-MM-dd").forEach { fmt ->
                        FilterChip(
                            selected = prefs.dateFormatPattern == fmt,
                            onClick = { onSetDateFormat(fmt) },
                            label = { Text(fmt) },
                        )
                    }
                }

                Text(
                    text = "First Day of Billing Month: Day ${prefs.firstDayOfMonth}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf(1, 5, 10, 15, 20, 25).forEach { day ->
                        FilterChip(
                            selected = prefs.firstDayOfMonth == day,
                            onClick = { onSetFirstDayOfMonth(day) },
                            label = { Text("Day $day") },
                        )
                    }
                }

                SettingsSwitchRow(
                    title = "Hide Balances by Default",
                    subtitle = "Mask amounts across cards and lists",
                    checked = prefs.hideBalances,
                    onCheckedChange = { onToggleHideBalances() },
                )
            }
        }

        // Defaults & Shortcuts
        item {
            SettingsSection(title = "Accounts & Defaults") {
                if (state.activeAccounts.isNotEmpty()) {
                    Text("Default Account", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        state.activeAccounts.forEach { acc ->
                            FilterChip(
                                selected = prefs.defaultAccountId == acc.id,
                                onClick = { onSetDefaultAccount(if (prefs.defaultAccountId == acc.id) null else acc.id) },
                                label = { Text(acc.name) },
                            )
                        }
                    }
                }

                if (state.expenseCategories.isNotEmpty()) {
                    Text("Default Expense Category", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        state.expenseCategories.forEach { cat ->
                            FilterChip(
                                selected = prefs.defaultExpenseCategoryId == cat.id,
                                onClick = { onSetDefaultExpenseCategory(if (prefs.defaultExpenseCategoryId == cat.id) null else cat.id) },
                                label = { Text(cat.name) },
                            )
                        }
                    }
                }

                SettingsActionRow(
                    icon = Icons.Rounded.AccountBalanceWallet,
                    title = "Manage Accounts & Wallets",
                    subtitle = "${state.accounts.size} total accounts",
                    onClick = onOpenAccounts,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingsActionRow(
                    icon = Icons.Rounded.Category,
                    title = "Manage Categories & Subcategories",
                    subtitle = "${state.categories.size} categories configured",
                    onClick = onOpenCategories,
                )
            }
        }

        // Notifications
        item {
            SettingsSection(title = "Notifications & Alerts") {
                SettingsSwitchRow(
                    title = "Enable Notifications",
                    subtitle = "Allow local budget & recurring alerts",
                    checked = prefs.notificationsEnabled,
                    onCheckedChange = { wantEnable ->
                        if (wantEnable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val granted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS,
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                onSetNotificationsEnabled(true)
                            } else {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        } else {
                            onSetNotificationsEnabled(wantEnable)
                        }
                    },
                )
                SettingsSwitchRow(
                    title = "Budget Threshold Alerts",
                    subtitle = "Notify when spending crosses budget threshold",
                    checked = prefs.budgetAlertsEnabled,
                    enabled = prefs.notificationsEnabled,
                    onCheckedChange = onSetBudgetAlertsEnabled,
                )
                SettingsSwitchRow(
                    title = "Recurring Transaction Alerts",
                    subtitle = "Notify when scheduled items run",
                    checked = prefs.recurringRemindersEnabled,
                    enabled = prefs.notificationsEnabled,
                    onCheckedChange = onSetRecurringRemindersEnabled,
                )
            }
        }

        // Security & Lock
        item {
            SettingsSection(title = "Security & Vault Lock") {
                SettingsActionRow(
                    icon = Icons.Rounded.Lock,
                    title = if (prefs.pinHash == null) "Set 4-Digit PIN Lock" else "Change or Remove PIN Lock",
                    subtitle = if (prefs.pinHash == null) "Currently unlocked" else "PIN protection active",
                    onClick = { showPinDialog = true },
                    testTag = "settings_pin_row",
                )
                SettingsSwitchRow(
                    title = "Biometric Unlock",
                    subtitle = when {
                        prefs.pinHash == null -> "Set a PIN first to enable fingerprint/face unlock"
                        !biometricAvailable -> "Biometrics not enrolled on this device"
                        else -> "Unlock vault using fingerprint or face"
                    },
                    checked = prefs.biometricEnabled,
                    enabled = prefs.pinHash != null && biometricAvailable,
                    onCheckedChange = onSetBiometricEnabled,
                )
                if (prefs.pinHash != null) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsActionRow(
                        icon = Icons.Rounded.Lock,
                        title = "Lock Vault Now",
                        subtitle = "Require PIN or biometrics immediately",
                        onClick = onLockNow,
                        testTag = "settings_lock_now_row",
                    )
                }
            }
        }

        // Offline Backup & Exports
        item {
            SettingsSection(title = "Offline Backup & Exports") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = onBackupJson,
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier.weight(1f).height(48.dp).testTag("settings_backup_json_button"),
                    ) {
                        Icon(Icons.Rounded.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Backup JSON", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    OutlinedButton(
                        onClick = onRestoreJson,
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier.weight(1f).height(48.dp).testTag("settings_restore_json_button"),
                    ) {
                        Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Restore", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = onExportCsv,
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier.weight(1f).height(48.dp),
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export CSV", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    OutlinedButton(
                        onClick = onExportPdf,
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier.weight(1f).height(48.dp),
                    ) {
                        Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export PDF", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        // App Info
        item {
            val uriHandler = LocalUriHandler.current
            val websiteUrl = stringResource(R.string.app_website_url)
            SettingsSection(title = "App Information") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_hisabi_brand),
                        contentDescription = stringResource(R.string.app_name),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, ChampagneGold.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge,
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(ChampagneGold.copy(alpha = 0.14f))
                                    .border(1.dp, ChampagneGold.copy(alpha = 0.35f), RoundedCornerShape(50))
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = "v${BuildConfig.VERSION_NAME}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                        Text(
                            text = "Personal Expense & Multi-Wallet Ledger",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(vertical = 4.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(AccentGreen.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.VerifiedUser,
                            contentDescription = null,
                            tint = AccentGreen,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Private & 100% Offline",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = "Your accounts, transactions, and receipts stay securely on your device with zero cloud tracking.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(vertical = 4.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ChampagneGold.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.app_made_by_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = stringResource(R.string.app_made_by_value),
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { runCatching { uriHandler.openUri(websiteUrl) } }
                        .testTag("settings_website_row")
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(AccentGreen.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Language,
                            contentDescription = null,
                            tint = AccentGreen,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.app_website_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = websiteUrl,
                            style = MaterialTheme.typography.titleSmall,
                            color = AccentGreen,
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                        contentDescription = "Open website",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Release ${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                    )
                    Text(
                        text = "Offline Edition",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        content()
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
