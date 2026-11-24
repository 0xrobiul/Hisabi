package me.robiul.hisabi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.robiul.hisabi.data.local.RecurringFrequency
import me.robiul.hisabi.data.local.RecurringRuleEntity
import me.robiul.hisabi.data.local.TransactionType
import me.robiul.hisabi.ui.theme.InterFontFamily
import me.robiul.hisabi.ui.theme.SemanticExpense
import me.robiul.hisabi.ui.theme.SemanticIncome
import me.robiul.hisabi.ui.viewmodel.LedgerUiState
import me.robiul.hisabi.util.Formatters
import me.robiul.hisabi.util.InputValidation

@Composable
fun RecurringScreen(
    state: LedgerUiState,
    onNavigateBack: () -> Unit,
    onSaveRule: (RecurringRuleEntity) -> Unit,
    onTogglePause: (RecurringRuleEntity) -> Unit,
    onSkipNext: (Long) -> Unit,
    onDeleteRule: (Long) -> Unit,
    onProcessDueNow: () -> Unit,
) {
    val currency = state.preferences.currencySymbol
    val hideBalances = state.preferences.hideBalances
    val datePattern = state.preferences.dateFormatPattern
    val accountMap = remember(state.accounts) { state.accounts.associateBy { it.id } }

    var showEditor by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<RecurringRuleEntity?>(null) }
    var pendingDelete by remember { mutableStateOf<RecurringRuleEntity?>(null) }

    pendingDelete?.let { rule ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete Recurring Schedule?") },
            text = { Text("Stop and delete \"${rule.title}\"? Past transactions already recorded will remain.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteRule(rule.id)
                        pendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } },
        )
    }

    if (showEditor) {
        RecurringEditorDialog(
            existing = editingRule,
            state = state,
            currency = currency,
            datePattern = datePattern,
            onDismiss = {
                showEditor = false
                editingRule = null
            },
            onSave = { rule ->
                onSaveRule(rule)
                showEditor = false
                editingRule = null
            },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                    Text(
                        text = "Recurring",
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = onProcessDueNow,
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier.height(44.dp),
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("Run", maxLines = 1, softWrap = false)
                    }
                    Button(
                        onClick = {
                            editingRule = null
                            showEditor = true
                        },
                        enabled = state.activeAccounts.isNotEmpty(),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier.height(44.dp).testTag("recurring_add_button"),
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Add", maxLines = 1, softWrap = false)
                    }
                }
            }
        }

        if (state.recurringRules.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No recurring bills or salaries scheduled yet. Automate house rent, WiFi, subscriptions, or monthly salary.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            items(state.recurringRules, key = { it.id }) { rule ->
                val accName = accountMap[rule.accountId]?.name ?: "Account"
                val color = if (rule.type == TransactionType.INCOME) SemanticIncome else SemanticExpense
                val cardShape = RoundedCornerShape(20.dp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(cardShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, cardShape)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(rule.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "${rule.frequency.label} (every ${rule.intervalCount}) • $accName",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = if (rule.isPaused) "Paused" else "Next due: ${Formatters.formatDateOnly(rule.nextDueDateMs, datePattern)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (rule.isPaused) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = Formatters.formatMoney(rule.amountCents, currency, hideBalances),
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = color,
                            )
                            Switch(
                                checked = !rule.isPaused,
                                onCheckedChange = { onTogglePause(rule) },
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = { onSkipNext(rule.id) },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f).height(46.dp),
                        ) {
                            Icon(Icons.Rounded.SkipNext, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Skip Next")
                        }
                        OutlinedButton(
                            onClick = { pendingDelete = rule },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.height(46.dp),
                        ) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete schedule", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecurringEditorDialog(
    existing: RecurringRuleEntity?,
    state: LedgerUiState,
    currency: String,
    datePattern: String,
    onDismiss: () -> Unit,
    onSave: (RecurringRuleEntity) -> Unit,
) {
    var txType by remember { mutableStateOf(existing?.type ?: TransactionType.EXPENSE) }
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var amountInput by remember {
        mutableStateOf(existing?.let { Formatters.centsToEditableString(it.amountCents) } ?: "")
    }
    var amountTouched by remember { mutableStateOf(false) }
    var accountId by remember { mutableStateOf(existing?.accountId ?: state.activeAccounts.firstOrNull()?.id) }
    val cats = if (txType == TransactionType.INCOME) state.incomeCategories else state.expenseCategories
    var categoryId by remember { mutableStateOf(existing?.categoryId ?: cats.firstOrNull()?.id) }
    var frequency by remember { mutableStateOf(existing?.frequency ?: RecurringFrequency.MONTHLY) }
    var intervalInput by remember { mutableStateOf((existing?.intervalCount ?: 1).toString()) }
    var intervalTouched by remember { mutableStateOf(false) }
    var startDateMs by remember { mutableLongStateOf(existing?.startDateMs ?: System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val amountError = if (amountTouched) {
        InputValidation.validatePositiveAmountError(amountInput, currencySymbol = currency)
    } else null
    val parsedInterval = intervalInput.toIntOrNull()
    val intervalError = if (frequency == RecurringFrequency.CUSTOM_DAYS && intervalTouched) {
        when {
            intervalInput.isBlank() -> "Interval days required"
            parsedInterval == null || parsedInterval !in 1..365 -> "Enter between 1 and 365 days"
            else -> null
        }
    } else null
    val amountCents = Formatters.parseAmountToCents(amountInput) ?: 0L
    val intervalValid = frequency != RecurringFrequency.CUSTOM_DAYS || (parsedInterval != null && parsedInterval in 1..365)
    val canSave = accountId != null && amountCents > 0L && intervalValid

    if (showDatePicker) {
        val dp = rememberDatePickerState(initialSelectedDateMillis = startDateMs)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        dp.selectedDateMillis?.let { startDateMs = it }
                        showDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
        ) { DatePicker(state = dp) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New Recurring Rule" else "Edit Recurring Rule") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(TransactionType.EXPENSE, TransactionType.INCOME).forEach { t ->
                        FilterChip(
                            selected = txType == t,
                            onClick = {
                                txType = t
                                categoryId = (if (t == TransactionType.INCOME) state.incomeCategories else state.expenseCategories).firstOrNull()?.id
                            },
                            label = { Text(t.label, maxLines = 1, softWrap = false) },
                        )
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title (e.g. House Rent, WiFi, Salary)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("recurring_title_input"),
                )
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = {
                        amountTouched = true
                        amountInput = InputValidation.sanitizeDecimalInput(it)
                    },
                    label = { Text("Amount ($currency)") },
                    isError = amountError != null,
                    supportingText = amountError?.let { msg -> { Text(msg) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("recurring_amount_input"),
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    RecurringFrequency.entries.forEach { f ->
                        FilterChip(
                            selected = frequency == f,
                            onClick = { frequency = f },
                            label = { Text(f.label, maxLines = 1, softWrap = false) },
                        )
                    }
                }
                if (frequency == RecurringFrequency.CUSTOM_DAYS) {
                    OutlinedTextField(
                        value = intervalInput,
                        onValueChange = { v ->
                            intervalTouched = true
                            intervalInput = InputValidation.sanitizeDigitsOnly(v, maxLength = 3)
                        },
                        label = { Text("Repeat every N days") },
                        isError = intervalError != null,
                        supportingText = intervalError?.let { msg -> { Text(msg) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    state.activeAccounts.forEach { acc ->
                        FilterChip(
                            selected = accountId == acc.id,
                            onClick = { accountId = acc.id },
                            label = { Text(acc.name, maxLines = 1, softWrap = false) },
                        )
                    }
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    cats.forEach { c ->
                        FilterChip(
                            selected = categoryId == c.id,
                            onClick = { categoryId = c.id },
                            label = { Text(c.name, maxLines = 1, softWrap = false) },
                        )
                    }
                }
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Start: ${Formatters.formatDateOnly(startDateMs, datePattern)}",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
        confirmButton = {
            val accId = accountId
            val selectedCat = cats.firstOrNull { it.id == categoryId }
            Button(
                onClick = {
                    amountTouched = true
                    intervalTouched = true
                    if (!canSave || accId == null) return@Button
                    val interval = (parsedInterval ?: 1).coerceIn(1, 365)
                    onSave(
                        RecurringRuleEntity(
                            id = existing?.id ?: 0L,
                            title = title.trim().ifBlank { selectedCat?.name ?: "Recurring" },
                            amountCents = amountCents,
                            type = txType,
                            categoryId = selectedCat?.id,
                            categoryNameSnapshot = selectedCat?.name ?: "",
                            accountId = accId,
                            frequency = frequency,
                            intervalCount = interval,
                            startDateMs = startDateMs,
                            nextDueDateMs = existing?.nextDueDateMs ?: startDateMs,
                            lastProcessedDateMs = existing?.lastProcessedDateMs,
                            isPaused = existing?.isPaused ?: false,
                            reminderEnabled = true,
                            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                        )
                    )
                },
                enabled = canSave,
                modifier = Modifier.testTag("recurring_save_button"),
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
