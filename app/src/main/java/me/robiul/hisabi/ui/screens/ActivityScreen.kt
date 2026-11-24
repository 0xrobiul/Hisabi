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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.robiul.hisabi.data.local.TransactionEntity
import me.robiul.hisabi.data.local.TransactionType
import me.robiul.hisabi.ui.components.TransactionRow
import me.robiul.hisabi.ui.theme.InterFontFamily
import me.robiul.hisabi.ui.theme.SemanticExpense
import me.robiul.hisabi.ui.theme.SemanticIncome
import me.robiul.hisabi.ui.viewmodel.LedgerUiState
import me.robiul.hisabi.util.Formatters
import java.time.Instant
import java.time.ZoneId

enum class ActivitySort(val label: String) {
    DATE_DESC("Newest First"),
    DATE_ASC("Oldest First"),
    AMOUNT_DESC("Highest Amount"),
    AMOUNT_ASC("Lowest Amount"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(
    state: LedgerUiState,
    onSelectTransaction: (TransactionEntity) -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf<TransactionType?>(null) }
    var filterCategoryId by remember { mutableStateOf<Long?>(null) }
    var filterAccountId by remember { mutableStateOf<Long?>(null) }
    var sortBy by remember { mutableStateOf(ActivitySort.DATE_DESC) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var rangeStartMs by remember { mutableStateOf<Long?>(null) }
    var rangeEndMs by remember { mutableStateOf<Long?>(null) }

    val accountMap = remember(state.accounts) { state.accounts.associateBy { it.id } }
    val categoryMap = remember(state.categories) { state.categories.associateBy { it.id } }
    val currency = state.preferences.currencySymbol
    val hideBalances = state.preferences.hideBalances
    val datePattern = state.preferences.dateFormatPattern

    val filtered = remember(
        state.transactions,
        searchQuery,
        filterType,
        filterCategoryId,
        filterAccountId,
        sortBy,
        rangeStartMs,
        rangeEndMs,
    ) {
        val q = searchQuery.trim().lowercase()
        val list = state.transactions.filter { tx ->
            val matchesQuery = q.isEmpty() ||
                tx.title.lowercase().contains(q) ||
                tx.description.lowercase().contains(q) ||
                tx.notes.lowercase().contains(q) ||
                tx.tagsCsv.lowercase().contains(q) ||
                tx.categoryNameSnapshot.lowercase().contains(q)
            val matchesType = filterType == null || tx.type == filterType
            val matchesCat = filterCategoryId == null || tx.categoryId == filterCategoryId
            val matchesAcc = filterAccountId == null || tx.accountId == filterAccountId || tx.toAccountId == filterAccountId
            val matchesRange = (rangeStartMs == null || tx.timestamp >= rangeStartMs!!) &&
                (rangeEndMs == null || tx.timestamp <= rangeEndMs!!)
            matchesQuery && matchesType && matchesCat && matchesAcc && matchesRange
        }

        when (sortBy) {
            ActivitySort.DATE_DESC -> list.sortedByDescending { it.timestamp }
            ActivitySort.DATE_ASC -> list.sortedBy { it.timestamp }
            ActivitySort.AMOUNT_DESC -> list.sortedByDescending { kotlin.math.abs(it.amountCents) + it.feeCents }
            ActivitySort.AMOUNT_ASC -> list.sortedBy { kotlin.math.abs(it.amountCents) + it.feeCents }
        }
    }

    val groupedByDate = remember(filtered, sortBy) {
        if (sortBy == ActivitySort.DATE_DESC || sortBy == ActivitySort.DATE_ASC) {
            filtered.groupBy { Formatters.toLocalDate(it.timestamp) }
        } else {
            emptyMap()
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val s = pickerState.selectedStartDateMillis
                        val e = pickerState.selectedEndDateMillis
                        rangeStartMs = s
                        rangeEndMs = e?.let {
                            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault())
                                .toLocalDate().plusDays(1).atStartOfDay(ZoneId.systemDefault())
                                .toInstant().toEpochMilli() - 1L
                        }
                        showDatePicker = false
                    }
                ) { Text("Apply") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        rangeStartMs = null
                        rangeEndMs = null
                        showDatePicker = false
                    }
                ) { Text("Clear") }
            },
        ) {
            DateRangePicker(state = pickerState)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Activity", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        text = "${filtered.size} matching transaction(s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row {
                    IconButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.testTag("activity_date_range_button"),
                    ) {
                        Icon(
                            Icons.Rounded.CalendarMonth,
                            contentDescription = "Filter by date range",
                            tint = if (rangeStartMs != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    Box {
                        IconButton(
                            onClick = { showSortMenu = true },
                            modifier = Modifier.testTag("activity_sort_button"),
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = "Sort transactions")
                        }
                        DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                            ActivitySort.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label) },
                                    onClick = {
                                        sortBy = option
                                        showSortMenu = false
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search title, notes, category, #tags...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("activity_search_input"),
            )
        }

        // Type & Date Filter Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = filterType == null,
                    onClick = { filterType = null },
                    label = { Text("All Types") },
                )
                listOf(
                    TransactionType.EXPENSE,
                    TransactionType.INCOME,
                    TransactionType.TRANSFER,
                    TransactionType.SAVINGS_CONTRIBUTION,
                    TransactionType.ADJUSTMENT,
                ).forEach { type ->
                    FilterChip(
                        selected = filterType == type,
                        onClick = { filterType = if (filterType == type) null else type },
                        label = { Text(type.label) },
                    )
                }
            }
        }

        // Account & Category Filter Chips
        if (state.accounts.isNotEmpty() || state.categories.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.accounts.forEach { acc ->
                        FilterChip(
                            selected = filterAccountId == acc.id,
                            onClick = { filterAccountId = if (filterAccountId == acc.id) null else acc.id },
                            label = { Text(acc.name) },
                        )
                    }
                    state.categories.forEach { cat ->
                        FilterChip(
                            selected = filterCategoryId == cat.id,
                            onClick = { filterCategoryId = if (filterCategoryId == cat.id) null else cat.id },
                            label = { Text(cat.name) },
                        )
                    }
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
                        .padding(28.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No transactions match the current filters.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else if (groupedByDate.isNotEmpty()) {
            groupedByDate.forEach { (date, dayTxs) ->
                val dayIncome = dayTxs.filter { it.type == TransactionType.INCOME }.sumOf { it.amountCents }
                val dayExpense = dayTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountCents + it.feeCents }

                item(key = "header_$date") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = Formatters.formatGroupDateHeader(date, datePattern),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (dayIncome > 0L) {
                                Text(
                                    text = Formatters.formatMoney(dayIncome, currency, hideBalances, showPlus = true),
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = SemanticIncome,
                                )
                            }
                            if (dayExpense > 0L) {
                                Text(
                                    text = Formatters.formatMoney(-dayExpense, currency, hideBalances),
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = SemanticExpense,
                                )
                            }
                        }
                    }
                }

                items(dayTxs, key = { it.id }) { tx ->
                    TransactionRow(
                        transaction = tx,
                        accountMap = accountMap,
                        categoryMap = categoryMap,
                        currencySymbol = currency,
                        datePattern = datePattern,
                        hideBalances = hideBalances,
                        onClick = { onSelectTransaction(tx) },
                    )
                }
            }
        } else {
            items(filtered, key = { it.id }) { tx ->
                TransactionRow(
                    transaction = tx,
                    accountMap = accountMap,
                    categoryMap = categoryMap,
                    currencySymbol = currency,
                    datePattern = datePattern,
                    hideBalances = hideBalances,
                    onClick = { onSelectTransaction(tx) },
                )
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
