package me.robiul.hisabi.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import me.robiul.hisabi.data.local.CategoryEntity
import me.robiul.hisabi.data.local.RecurringFrequency
import me.robiul.hisabi.data.local.RecurringRuleEntity
import me.robiul.hisabi.data.local.TransactionEntity
import me.robiul.hisabi.data.local.TransactionType
import me.robiul.hisabi.ui.theme.ChampagneGold
import me.robiul.hisabi.ui.theme.InterFontFamily
import me.robiul.hisabi.ui.theme.SemanticExpense
import me.robiul.hisabi.ui.theme.SemanticIncome
import me.robiul.hisabi.ui.theme.SemanticTransfer
import me.robiul.hisabi.ui.theme.SemanticWarning
import me.robiul.hisabi.ui.viewmodel.LedgerUiState
import me.robiul.hisabi.util.Formatters
import me.robiul.hisabi.util.InputValidation
import me.robiul.hisabi.util.iconForKey
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTransactionScreen(
    state: LedgerUiState,
    initialType: TransactionType = TransactionType.EXPENSE,
    initialAccountId: Long? = null,
    existingTransaction: TransactionEntity? = null,
    onCopyReceiptUri: suspend (android.net.Uri) -> String?,
    onSaveTransaction: (TransactionEntity) -> Unit,
    onSaveRecurringRule: (RecurringRuleEntity) -> Unit,
    onOpenCategories: () -> Unit,
    onOpenAddAccount: () -> Unit,
    onBeforeExternalPicker: () -> Unit = {},
    onNavigateBack: (() -> Unit)? = null,
) {
    val currency = state.preferences.currencySymbol
    val datePattern = state.preferences.dateFormatPattern
    val activeAccounts = state.activeAccounts.ifEmpty { state.accounts }
    val scope = rememberCoroutineScope()

    var txType by remember(existingTransaction, initialType) {
        mutableStateOf(
            existingTransaction?.type ?: when (initialType) {
                TransactionType.INCOME, TransactionType.TRANSFER -> initialType
                else -> TransactionType.EXPENSE
            }
        )
    }

    var amountInput by remember(existingTransaction) {
        mutableStateOf(existingTransaction?.let { Formatters.centsToEditableString(it.amountCents) } ?: "")
    }
    var amountTouched by remember(existingTransaction) { mutableStateOf(false) }
    var feeInput by remember(existingTransaction) {
        mutableStateOf(
            existingTransaction?.takeIf { it.feeCents > 0L }
                ?.let { Formatters.centsToEditableString(it.feeCents) } ?: ""
        )
    }
    var selectedChargePercentBps by remember(existingTransaction) { mutableStateOf<Int?>(null) }
    var titleInput by remember(existingTransaction) {
        mutableStateOf(existingTransaction?.title ?: "")
    }
    var descriptionInput by remember(existingTransaction) {
        mutableStateOf(existingTransaction?.description ?: "")
    }
    var notesInput by remember(existingTransaction) {
        mutableStateOf(existingTransaction?.notes ?: "")
    }
    var tagsInput by remember(existingTransaction) {
        mutableStateOf(existingTransaction?.tagsCsv ?: "")
    }

    var selectedAccountId by remember(existingTransaction, initialAccountId, activeAccounts) {
        mutableStateOf(
            existingTransaction?.accountId
                ?: initialAccountId
                ?: state.preferences.defaultAccountId?.takeIf { id -> activeAccounts.any { it.id == id } }
                ?: activeAccounts.firstOrNull()?.id
        )
    }
    var toAccountId by remember(existingTransaction, activeAccounts) {
        mutableStateOf(
            existingTransaction?.toAccountId
                ?: activeAccounts.firstOrNull { it.id != selectedAccountId }?.id
        )
    }

    val categoriesForType: List<CategoryEntity> = when (txType) {
        TransactionType.INCOME -> state.incomeCategories
        else -> state.expenseCategories
    }
    var selectedCategoryId by remember(existingTransaction, txType, categoriesForType) {
        mutableStateOf(
            existingTransaction?.categoryId?.takeIf { id -> categoriesForType.any { it.id == id } }
                ?: if (txType == TransactionType.EXPENSE) {
                    state.preferences.defaultExpenseCategoryId?.takeIf { id -> categoriesForType.any { it.id == id } }
                        ?: categoriesForType.firstOrNull()?.id
                } else {
                    categoriesForType.firstOrNull()?.id
                }
        )
    }
    val selectedCategory = categoriesForType.firstOrNull { it.id == selectedCategoryId }
    var selectedSubcategory by remember(existingTransaction, selectedCategoryId) {
        mutableStateOf(existingTransaction?.subcategoryName ?: "")
    }

    var timestampMs by remember(existingTransaction) {
        mutableLongStateOf(existingTransaction?.timestamp ?: System.currentTimeMillis())
    }
    var receiptPath by remember(existingTransaction) {
        mutableStateOf(existingTransaction?.receiptPath)
    }

    var makeRecurring by remember { mutableStateOf(false) }
    var recurringFreq by remember { mutableStateOf(RecurringFrequency.MONTHLY) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val receiptPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val copied = onCopyReceiptUri(uri)
                if (copied != null) receiptPath = copied
            }
        }
    }

    if (showDatePicker) {
        val dateState = rememberDatePickerState(initialSelectedDateMillis = timestampMs)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val pickedMs = dateState.selectedDateMillis
                        if (pickedMs != null) {
                            val zone = ZoneId.systemDefault()
                            val oldTime = Instant.ofEpochMilli(timestampMs).atZone(zone).toLocalTime()
                            val newDate = Instant.ofEpochMilli(pickedMs).atZone(zone).toLocalDate()
                            timestampMs = LocalDateTime.of(newDate, oldTime).atZone(zone).toInstant().toEpochMilli()
                        }
                        showDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
        ) {
            DatePicker(state = dateState)
        }
    }

    if (showTimePicker) {
        val currentZdt = Instant.ofEpochMilli(timestampMs).atZone(ZoneId.systemDefault())
        val timeState = rememberTimePickerState(
            initialHour = currentZdt.hour,
            initialMinute = currentZdt.minute,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select Time") },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val zone = ZoneId.systemDefault()
                        val date = Instant.ofEpochMilli(timestampMs).atZone(zone).toLocalDate()
                        timestampMs = LocalDateTime.of(date, LocalTime.of(timeState.hour, timeState.minute))
                            .atZone(zone).toInstant().toEpochMilli()
                        showTimePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (onNavigateBack != null) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
            }
            Text(
                text = if (existingTransaction == null) "Add Transaction" else "Edit Transaction",
                style = MaterialTheme.typography.headlineMedium,
            )
        }

        if (activeAccounts.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Create an Account First",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "Add a Cash, Bank, bKash, Nagad, or Credit Card wallet before recording transactions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = onOpenAddAccount,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.height(48.dp),
                ) {
                    Text("Create Account")
                }
            }
            return
        }

        // Type Switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
                .padding(5.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            listOf(TransactionType.EXPENSE, TransactionType.INCOME, TransactionType.TRANSFER).forEach { type ->
                val selected = txType == type
                val bg = when {
                    !selected -> Color.Transparent
                    type == TransactionType.EXPENSE -> SemanticExpense.copy(alpha = 0.16f)
                    type == TransactionType.INCOME -> SemanticIncome.copy(alpha = 0.16f)
                    else -> SemanticTransfer.copy(alpha = 0.16f)
                }
                val borderCol = when {
                    !selected -> Color.Transparent
                    type == TransactionType.EXPENSE -> SemanticExpense.copy(alpha = 0.45f)
                    type == TransactionType.INCOME -> SemanticIncome.copy(alpha = 0.45f)
                    else -> SemanticTransfer.copy(alpha = 0.45f)
                }
                val fg = when {
                    !selected -> MaterialTheme.colorScheme.onSurfaceVariant
                    type == TransactionType.EXPENSE -> SemanticExpense
                    type == TransactionType.INCOME -> SemanticIncome
                    else -> SemanticTransfer
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(bg)
                        .border(1.dp, borderCol, RoundedCornerShape(13.dp))
                        .clickable {
                            txType = type
                            selectedSubcategory = ""
                        }
                        .testTag("tx_type_${type.name.lowercase()}"),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = type.label,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        softWrap = false,
                        color = fg,
                    )
                }
            }
        }

        // Amount Input + Fast BDT Quick Increments
        val amountError = if (amountTouched) {
            InputValidation.validatePositiveAmountError(amountInput, currency)
        } else null
        OutlinedTextField(
            value = amountInput,
            onValueChange = {
                amountTouched = true
                val sanitized = InputValidation.sanitizeDecimalInput(it)
                amountInput = sanitized
                selectedChargePercentBps?.let { bps ->
                    val amtCents = Formatters.parseAmountToCents(sanitized) ?: 0L
                    val calcFee = Formatters.computePercentageCents(amtCents, bps)
                    feeInput = if (calcFee > 0L) Formatters.centsToEditableString(calcFee) else ""
                }
            },
            label = { Text("Amount ($currency)") },
            textStyle = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
            ),
            isError = amountError != null,
            supportingText = amountError?.let { msg -> { Text(msg) } },
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("tx_amount_input"),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(50L, 100L, 500L, 1000L, 5000L).forEach { step ->
                FilterChip(
                    selected = false,
                    onClick = {
                        amountTouched = true
                        val cur = Formatters.parseAmountToCents(amountInput) ?: 0L
                        val nextCents = cur + step * 100L
                        amountInput = Formatters.centsToEditableString(nextCents)
                        selectedChargePercentBps?.let { bps ->
                            val calcFee = Formatters.computePercentageCents(nextCents, bps)
                            feeInput = if (calcFee > 0L) Formatters.centsToEditableString(calcFee) else ""
                        }
                    },
                    label = { Text("+$currency$step", maxLines = 1, softWrap = false) },
                )
            }
        }

        val feeError = InputValidation.validateOptionalNonNegativeAmountError(feeInput)
        if (txType == TransactionType.EXPENSE) {
            OutlinedTextField(
                value = feeInput,
                onValueChange = {
                    selectedChargePercentBps = null
                    feeInput = InputValidation.sanitizeDecimalInput(it)
                },
                label = { Text("Fee / Cashout Charge ($currency, optional)") },
                isError = feeError != null,
                supportingText = feeError?.let { msg -> { Text(msg) } },
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Title
        OutlinedTextField(
            value = titleInput,
            onValueChange = { if (it.length <= 80) titleInput = it },
            label = { Text("Title (e.g. Lunch, Rickshaw, Freelance Pay)") },
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("tx_title_input"),
        )

        // Account Selector
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = if (txType == TransactionType.TRANSFER) "From Account" else "Account",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                activeAccounts.forEach { acc ->
                    FilterChip(
                        selected = selectedAccountId == acc.id,
                        onClick = {
                            selectedAccountId = acc.id
                            if (toAccountId == acc.id) {
                                toAccountId = activeAccounts.firstOrNull { it.id != acc.id }?.id
                            }
                        },
                        label = { Text(acc.name) },
                    )
                }
            }
        }

        if (txType == TransactionType.TRANSFER) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "To Account",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val destAccounts = activeAccounts.filter { it.id != selectedAccountId }
                if (destAccounts.isEmpty()) {
                    Text(
                        text = "Add a second account to perform transfers.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SemanticExpense,
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        destAccounts.forEach { acc ->
                            FilterChip(
                                selected = toAccountId == acc.id,
                                onClick = { toAccountId = acc.id },
                                label = { Text(acc.name, maxLines = 1, softWrap = false) },
                            )
                        }
                    }
                }
            }

            // Dedicated Transfer Charge / Fee Section
            val curAmtCents = Formatters.parseAmountToCents(amountInput) ?: 0L
            val curFeeCents = (Formatters.parseAmountToCents(feeInput) ?: 0L).coerceAtLeast(0L)
            val fromAcc = activeAccounts.firstOrNull { it.id == selectedAccountId }
            val toAcc = activeAccounts.firstOrNull { it.id == toAccountId }
            val chargeCardShape = RoundedCornerShape(18.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(chargeCardShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, chargeCardShape)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Transfer Charge (Optional)",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = if (curFeeCents > 0L) Formatters.formatMoney(curFeeCents, currency) else "Free",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = if (curFeeCents > 0L) SemanticWarning else SemanticIncome,
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    FilterChip(
                        selected = feeInput.isBlank() && selectedChargePercentBps == null,
                        onClick = {
                            selectedChargePercentBps = null
                            feeInput = ""
                        },
                        label = { Text("No Charge", maxLines = 1, softWrap = false) },
                        modifier = Modifier.testTag("transfer_charge_free"),
                    )
                    listOf(
                        "1.85%" to 185,
                        "1.20%" to 120,
                        "1.50%" to 150,
                    ).forEach { (label, bps) ->
                        FilterChip(
                            selected = selectedChargePercentBps == bps,
                            onClick = {
                                selectedChargePercentBps = bps
                                val calcFee = Formatters.computePercentageCents(curAmtCents, bps)
                                feeInput = if (calcFee > 0L) Formatters.centsToEditableString(calcFee) else ""
                            },
                            label = { Text(label, maxLines = 1, softWrap = false) },
                            modifier = Modifier.testTag("transfer_charge_pct_$bps"),
                        )
                    }
                    listOf(5L, 10L, 15L, 20L).forEach { flat ->
                        val flatCents = flat * 100L
                        FilterChip(
                            selected = selectedChargePercentBps == null && curFeeCents == flatCents,
                            onClick = {
                                selectedChargePercentBps = null
                                feeInput = Formatters.centsToEditableString(flatCents)
                            },
                            label = { Text("$currency$flat", maxLines = 1, softWrap = false) },
                            modifier = Modifier.testTag("transfer_charge_flat_$flat"),
                        )
                    }
                }

                OutlinedTextField(
                    value = feeInput,
                    onValueChange = {
                        selectedChargePercentBps = null
                        feeInput = InputValidation.sanitizeDecimalInput(it)
                    },
                    label = { Text("Custom Charge ($currency, optional)") },
                    isError = feeError != null,
                    supportingText = feeError?.let { msg -> { Text(msg) } },
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transfer_charge_input"),
                )

                if (fromAcc != null && toAcc != null && curAmtCents > 0L) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "From ${fromAcc.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f).padding(end = 8.dp),
                            )
                            Text(
                                text = "-${Formatters.formatMoney(curAmtCents + curFeeCents, currency)}",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = SemanticExpense,
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "To ${toAcc.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f).padding(end = 8.dp),
                            )
                            Text(
                                text = "+${Formatters.formatMoney(curAmtCents, currency)}",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = SemanticIncome,
                            )
                        }
                        if (curFeeCents > 0L) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = "Transfer charge (expense)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                                )
                                Text(
                                    text = Formatters.formatMoney(curFeeCents, currency),
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = ChampagneGold,
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Category + Subcategory Selection
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = onOpenCategories) { Text("Manage Categories") }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    categoriesForType.forEach { cat ->
                        FilterChip(
                            selected = selectedCategoryId == cat.id,
                            onClick = {
                                selectedCategoryId = cat.id
                                selectedSubcategory = ""
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = iconForKey(cat.iconKey),
                                    contentDescription = null,
                                    tint = Color(cat.colorHex),
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                            label = { Text(cat.name) },
                        )
                    }
                }

                val subs = selectedCategory?.subcategories().orEmpty()
                if (subs.isNotEmpty()) {
                    Text(
                        text = "Subcategory",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        subs.forEach { sub ->
                            FilterChip(
                                selected = selectedSubcategory == sub,
                                onClick = { selectedSubcategory = if (selectedSubcategory == sub) "" else sub },
                                label = { Text(sub) },
                            )
                        }
                    }
                }
            }
        }

        // Date & Time Row
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val stackDateTime = maxWidth < 310.dp
            if (stackDateTime) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                    ) {
                        Icon(Icons.Rounded.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(Formatters.formatDateOnly(timestampMs, datePattern), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    OutlinedButton(
                        onClick = { showTimePicker = true },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                    ) {
                        Icon(Icons.Rounded.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        val zdt = Instant.ofEpochMilli(timestampMs).atZone(ZoneId.systemDefault())
                        Text(String.format("%02d:%02d", zdt.hour, zdt.minute), maxLines = 1)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1.1f).height(48.dp),
                    ) {
                        Icon(Icons.Rounded.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(Formatters.formatDateOnly(timestampMs, datePattern), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    OutlinedButton(
                        onClick = { showTimePicker = true },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(0.8f).height(48.dp),
                    ) {
                        Icon(Icons.Rounded.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        val zdt = Instant.ofEpochMilli(timestampMs).atZone(ZoneId.systemDefault())
                        Text(String.format("%02d:%02d", zdt.hour, zdt.minute), maxLines = 1)
                    }
                }
            }
        }

        // Description, Notes & Tags
        OutlinedTextField(
            value = descriptionInput,
            onValueChange = { descriptionInput = it },
            label = { Text("Description (optional)") },
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = notesInput,
            onValueChange = { notesInput = it },
            label = { Text("Notes (optional)") },
            shape = RoundedCornerShape(16.dp),
            minLines = 2,
            maxLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = tagsInput,
            onValueChange = { tagsInput = it },
            label = { Text("Tags (comma separated, e.g. office, eid, urgent)") },
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        // Optional Receipt Image (Zero-permission Android Photo Picker)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = {
                        onBeforeExternalPicker()
                        receiptPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.height(48.dp),
                ) {
                    Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (receiptPath == null) "Attach Receipt" else "Change Receipt")
                }
                if (receiptPath != null) {
                    IconButton(onClick = { receiptPath = null }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Remove receipt")
                    }
                }
            }
            receiptPath?.let { path ->
                val f = File(path)
                if (f.exists()) {
                    AsyncImage(
                        model = f,
                        contentDescription = "Receipt preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
                    )
                }
            }
        }

        // Optional Recurring Toggle when creating new Expense/Income
        if (existingTransaction == null && txType != TransactionType.TRANSFER) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Repeat, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Make this recurring", style = MaterialTheme.typography.titleSmall)
                    }
                    Switch(checked = makeRecurring, onCheckedChange = { makeRecurring = it })
                }
                if (makeRecurring) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(
                            RecurringFrequency.DAILY,
                            RecurringFrequency.WEEKLY,
                            RecurringFrequency.MONTHLY,
                            RecurringFrequency.YEARLY,
                        ).forEach { freq ->
                            FilterChip(
                                selected = recurringFreq == freq,
                                onClick = { recurringFreq = freq },
                                label = { Text(freq.label) },
                            )
                        }
                    }
                }
            }
        }

        val parsedAmount = Formatters.parseAmountToCents(amountInput) ?: 0L
        val parsedFee = Formatters.parseAmountToCents(feeInput) ?: 0L
        val validAccounts = selectedAccountId != null &&
            (txType != TransactionType.TRANSFER || (toAccountId != null && toAccountId != selectedAccountId))
        val canSubmit = parsedAmount > 0L && validAccounts && feeError == null

        Button(
            onClick = {
                val srcId = selectedAccountId ?: return@Button
                val resolvedTitle = titleInput.trim().ifBlank {
                    when (txType) {
                        TransactionType.TRANSFER -> "Account Transfer"
                        else -> selectedSubcategory.ifBlank { selectedCategory?.name ?: txType.label }
                    }
                }
                val tx = TransactionEntity(
                    id = existingTransaction?.id ?: 0L,
                    title = resolvedTitle,
                    amountCents = parsedAmount,
                    feeCents = parsedFee.coerceAtLeast(0L),
                    type = txType,
                    categoryId = if (txType == TransactionType.TRANSFER) null else selectedCategory?.id,
                    categoryNameSnapshot = if (txType == TransactionType.TRANSFER) "Transfer" else (selectedCategory?.name ?: ""),
                    subcategoryName = if (txType == TransactionType.TRANSFER) "" else selectedSubcategory,
                    accountId = srcId,
                    toAccountId = if (txType == TransactionType.TRANSFER) toAccountId else null,
                    savingsGoalId = existingTransaction?.savingsGoalId,
                    timestamp = timestampMs,
                    description = descriptionInput.trim(),
                    notes = notesInput.trim(),
                    receiptPath = receiptPath,
                    tagsCsv = tagsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }.joinToString(", "),
                    recurringRuleId = existingTransaction?.recurringRuleId,
                )
                onSaveTransaction(tx)

                if (existingTransaction == null && makeRecurring && txType != TransactionType.TRANSFER) {
                    val nextDue = Formatters.computeNextDueDate(
                        fromMs = timestampMs,
                        frequency = recurringFreq,
                        intervalCount = 1,
                    )
                    onSaveRecurringRule(
                        RecurringRuleEntity(
                            title = resolvedTitle,
                            amountCents = parsedAmount,
                            type = txType,
                            categoryId = selectedCategory?.id,
                            categoryNameSnapshot = selectedCategory?.name ?: "",
                            subcategoryName = selectedSubcategory,
                            accountId = srcId,
                            frequency = recurringFreq,
                            intervalCount = 1,
                            startDateMs = timestampMs,
                            nextDueDateMs = nextDue,
                            lastProcessedDateMs = timestampMs,
                            notes = notesInput.trim(),
                        )
                    )
                }
                if (existingTransaction == null && onNavigateBack == null) {
                    amountInput = ""
                    amountTouched = false
                    feeInput = ""
                    selectedChargePercentBps = null
                    titleInput = ""
                    descriptionInput = ""
                    notesInput = ""
                    tagsInput = ""
                    receiptPath = null
                    makeRecurring = false
                }
            },
            enabled = canSubmit,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("tx_save_button"),
        ) {
            Text(
                text = if (existingTransaction == null) "Save ${txType.label}" else "Update ${txType.label}",
                fontFamily = InterFontFamily,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
