package me.robiul.hisabi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.robiul.hisabi.data.local.BudgetEntity
import me.robiul.hisabi.data.local.BudgetPeriod
import me.robiul.hisabi.data.local.TransactionType
import me.robiul.hisabi.ui.components.BudgetStatusCard
import me.robiul.hisabi.ui.viewmodel.BudgetStatus
import me.robiul.hisabi.ui.viewmodel.LedgerUiState
import me.robiul.hisabi.util.Formatters
import me.robiul.hisabi.util.InputValidation
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(
    state: LedgerUiState,
    onNavigateBack: () -> Unit,
    onSaveBudget: (BudgetEntity) -> Unit,
    onDeleteBudget: (Long) -> Unit,
) {
    val currency = state.preferences.currencySymbol
    val hideBalances = state.preferences.hideBalances
    val datePattern = state.preferences.dateFormatPattern
    val categoryMap = remember(state.categories) { state.categories.associateBy { it.id } }

    var editingBudget by remember { mutableStateOf<BudgetEntity?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<BudgetEntity?>(null) }
    var historyMonth by remember { mutableStateOf(YearMonth.now().minusMonths(1)) }

    val historicalStatuses = remember(state.budgets, state.transactions, historyMonth, state.preferences.firstDayOfMonth) {
        val (startMs, endMs) = Formatters.billingMonthBounds(
            historyMonth.year,
            historyMonth.monthValue,
            state.preferences.firstDayOfMonth,
        )
        state.budgets.filter { it.periodType == BudgetPeriod.MONTHLY }.map { b ->
            val spent = state.transactions.filter { tx ->
                tx.type == TransactionType.EXPENSE &&
                    tx.timestamp in startMs until endMs &&
                    (b.categoryId == null || tx.categoryId == b.categoryId)
            }.sumOf { it.amountCents + it.feeCents }
            val util = if (b.limitCents > 0L) ((spent * 100L) / b.limitCents).toInt() else 0
            BudgetStatus(
                budget = b,
                categoryName = b.categoryId?.let { categoryMap[it]?.name },
                spentCents = spent,
                remainingCents = b.limitCents - spent,
                utilizationPercent = util,
            )
        }
    }

    pendingDelete?.let { b ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete Budget?") },
            text = { Text("Remove budget \"${b.name}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteBudget(b.id)
                        pendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } },
        )
    }

    if (showEditor) {
        BudgetEditorDialog(
            existing = editingBudget,
            state = state,
            currency = currency,
            datePattern = datePattern,
            onDismiss = {
                showEditor = false
                editingBudget = null
            },
            onSave = { budget ->
                onSaveBudget(budget)
                showEditor = false
                editingBudget = null
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
                        text = "Budgets",
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Button(
                    onClick = {
                        editingBudget = null
                        showEditor = true
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.height(44.dp).testTag("budgets_add_button"),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add", maxLines = 1, softWrap = false)
                }
            }
        }

        if (state.budgetStatuses.isEmpty()) {
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
                        text = "No budgets yet. Create a monthly, weekly, or custom spending limit to receive threshold alerts.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            items(state.budgetStatuses, key = { it.budget.id }) { status ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    BudgetStatusCard(
                        status = status,
                        currencySymbol = currency,
                        hideBalances = hideBalances,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(
                            onClick = {
                                editingBudget = status.budget
                                showEditor = true
                            },
                        ) {
                            Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit")
                        }
                        TextButton(
                            onClick = { pendingDelete = status.budget },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        ) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete")
                        }
                    }
                }
            }
        }

        // Historical Budget Adherence
        if (state.budgets.any { it.periodType == BudgetPeriod.MONTHLY }) {
            item {
                Text(
                    text = "Historical Monthly Budgets",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { historyMonth = historyMonth.minusMonths(1) }) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Previous month")
                    }
                    Text(
                        text = Formatters.formatBillingMonthLabel(historyMonth.year, historyMonth.monthValue),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    IconButton(onClick = { historyMonth = historyMonth.plusMonths(1) }) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next month")
                    }
                }
            }
            items(historicalStatuses, key = { "hist_${it.budget.id}" }) { status ->
                BudgetStatusCard(
                    status = status,
                    currencySymbol = currency,
                    hideBalances = hideBalances,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetEditorDialog(
    existing: BudgetEntity?,
    state: LedgerUiState,
    currency: String,
    datePattern: String,
    onDismiss: () -> Unit,
    onSave: (BudgetEntity) -> Unit,
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var periodType by remember { mutableStateOf(existing?.periodType ?: BudgetPeriod.MONTHLY) }
    var categoryId by remember { mutableStateOf(existing?.categoryId) }
    var limitInput by remember {
        mutableStateOf(existing?.let { Formatters.centsToEditableString(it.limitCents) } ?: "")
    }
    var limitTouched by remember { mutableStateOf(false) }
    var threshold by remember { mutableIntStateOf(existing?.alertThresholdPercent ?: 80) }
    var alertsEnabled by remember { mutableStateOf(existing?.alertsEnabled ?: true) }
    var customStartMs by remember { mutableLongStateOf(existing?.startDateMs?.takeIf { it > 0L } ?: System.currentTimeMillis()) }
    var customEndMs by remember {
        mutableLongStateOf(
            existing?.endDateMs?.takeIf { it > 0L } ?: (System.currentTimeMillis() + 30L * 24 * 3600 * 1000)
        )
    }
    var showRangePicker by remember { mutableStateOf(false) }

    val limitError = if (limitTouched) {
        InputValidation.validatePositiveAmountError(limitInput, currencySymbol = currency)
    } else null
    val customRangeError = if (periodType == BudgetPeriod.CUSTOM && customEndMs <= customStartMs) {
        "End date must be after start date"
    } else null
    val limitCents = Formatters.parseAmountToCents(limitInput) ?: 0L
    val canSave = limitCents > 0L && customRangeError == null

    if (showRangePicker) {
        val rangeState = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { showRangePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        rangeState.selectedStartDateMillis?.let { customStartMs = it }
                        rangeState.selectedEndDateMillis?.let {
                            customEndMs = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault())
                                .toLocalDate().plusDays(1).atStartOfDay(ZoneId.systemDefault())
                                .toInstant().toEpochMilli()
                        }
                        showRangePicker = false
                    }
                ) { Text("Apply") }
            },
            dismissButton = { TextButton(onClick = { showRangePicker = false }) { Text("Cancel") } },
        ) { DateRangePicker(state = rangeState) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New Budget" else "Edit Budget") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Budget Name (e.g. Monthly Food)") },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("budget_name_input"),
                )
                OutlinedTextField(
                    value = limitInput,
                    onValueChange = {
                        limitTouched = true
                        limitInput = InputValidation.sanitizeDecimalInput(it)
                    },
                    label = { Text("Spending Limit ($currency)") },
                    isError = limitError != null,
                    supportingText = limitError?.let { msg -> { Text(msg) } },
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("budget_limit_input"),
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    BudgetPeriod.entries.forEach { p ->
                        FilterChip(
                            selected = periodType == p,
                            onClick = { periodType = p },
                            label = { Text(p.label, maxLines = 1, softWrap = false) },
                        )
                    }
                }
                if (periodType == BudgetPeriod.CUSTOM) {
                    OutlinedButton(onClick = { showRangePicker = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "${Formatters.formatDateOnly(customStartMs, datePattern)} – ${Formatters.formatDateOnly(customEndMs, datePattern)}",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    customRangeError?.let { err ->
                        Text(
                            text = err,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                Text("Scope", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    FilterChip(
                        selected = categoryId == null,
                        onClick = { categoryId = null },
                        label = { Text("All Expenses", maxLines = 1, softWrap = false) },
                    )
                    state.expenseCategories.forEach { cat ->
                        FilterChip(
                            selected = categoryId == cat.id,
                            onClick = { categoryId = cat.id },
                            label = { Text(cat.name, maxLines = 1, softWrap = false) },
                        )
                    }
                }
                Text("Alert Threshold: $threshold%", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = threshold.toFloat(),
                    onValueChange = { threshold = it.roundToInt().coerceIn(50, 100) },
                    valueRange = 50f..100f,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Notify on threshold",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                    )
                    Switch(checked = alertsEnabled, onCheckedChange = { alertsEnabled = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    limitTouched = true
                    if (!canSave) return@Button
                    val autoName = name.trim().ifBlank {
                        val scopeName = categoryId?.let { id -> state.expenseCategories.firstOrNull { it.id == id }?.name }
                            ?: "Overall"
                        "${periodType.label} $scopeName"
                    }
                    onSave(
                        BudgetEntity(
                            id = existing?.id ?: 0L,
                            name = autoName,
                            periodType = periodType,
                            categoryId = categoryId,
                            limitCents = limitCents,
                            startDateMs = if (periodType == BudgetPeriod.CUSTOM) customStartMs else 0L,
                            endDateMs = if (periodType == BudgetPeriod.CUSTOM) customEndMs else 0L,
                            alertThresholdPercent = threshold,
                            alertsEnabled = alertsEnabled,
                            lastAlertPeriodKey = existing?.lastAlertPeriodKey ?: "",
                            isActive = true,
                            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                        )
                    )
                },
                enabled = canSave,
                modifier = Modifier.testTag("budget_save_button"),
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
