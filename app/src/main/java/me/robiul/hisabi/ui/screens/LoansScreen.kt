package me.robiul.hisabi.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.robiul.hisabi.data.local.AccountEntity
import me.robiul.hisabi.data.local.LoanDirection
import me.robiul.hisabi.data.local.LoanRecordEntity
import me.robiul.hisabi.data.local.LoanRepaymentEntity
import me.robiul.hisabi.data.local.LoanStatus
import me.robiul.hisabi.ui.components.LoanDetailSheet
import me.robiul.hisabi.ui.components.LoanEditorDialog
import me.robiul.hisabi.ui.components.RepaymentEditorDialog
import me.robiul.hisabi.ui.components.loanStatusColor
import me.robiul.hisabi.ui.theme.ChampagneGold
import me.robiul.hisabi.ui.theme.InterFontFamily
import me.robiul.hisabi.ui.theme.SemanticExpense
import me.robiul.hisabi.ui.theme.SemanticIncome
import me.robiul.hisabi.ui.viewmodel.LedgerUiState
import me.robiul.hisabi.ui.viewmodel.LoanStatusItem
import me.robiul.hisabi.util.Formatters

private enum class LoanFilterOption(val label: String, val status: LoanStatus?) {
    ALL("All", null),
    ACTIVE("Active", LoanStatus.ACTIVE),
    PARTIALLY_REPAID("Partially Repaid", LoanStatus.PARTIALLY_REPAID),
    FULLY_REPAID("Fully Repaid", LoanStatus.FULLY_REPAID),
    OVERDUE("Overdue", LoanStatus.OVERDUE),
}

@Composable
fun LoansScreen(
    state: LedgerUiState,
    onNavigateBack: () -> Unit,
    onSaveLoan: (LoanRecordEntity) -> Unit,
    onDeleteLoan: (Long) -> Unit,
    onSaveRepayment: (LoanRepaymentEntity) -> Unit,
    onDeleteRepayment: (Long) -> Unit,
) {
    val currency = state.preferences.currencySymbol
    val hideBalances = state.preferences.hideBalances
    val datePattern = state.preferences.dateFormatPattern
    val activeAccounts = remember(state.activeAccounts, state.accounts) {
        state.activeAccounts.ifEmpty { state.accounts }
    }
    val accountMap = remember(state.accounts) { state.accounts.associateBy { it.id } }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(LoanFilterOption.ALL) }

    var showLoanEditor by remember { mutableStateOf(false) }
    var editingLoan by remember { mutableStateOf<LoanRecordEntity?>(null) }
    var defaultDirection by remember { mutableStateOf(LoanDirection.LENT) }

    var inspectingLoanId by remember { mutableStateOf<Long?>(null) }
    var repaymentDialogTarget by remember { mutableStateOf<Pair<LoanStatusItem, LoanRepaymentEntity?>?>(null) }

    if (showLoanEditor) {
        val dialogAccounts = remember(activeAccounts, state.accounts, editingLoan) {
            val extra = editingLoan?.accountId?.let { id ->
                state.accounts.firstOrNull { it.id == id && activeAccounts.none { a -> a.id == id } }
            }
            if (extra != null) activeAccounts + extra else activeAccounts
        }
        LoanEditorDialog(
            existing = editingLoan,
            initialDirection = defaultDirection,
            accounts = dialogAccounts,
            defaultAccountId = state.preferences.defaultAccountId,
            currency = currency,
            datePattern = datePattern,
            onDismiss = {
                showLoanEditor = false
                editingLoan = null
            },
            onSave = { loan ->
                onSaveLoan(loan)
                showLoanEditor = false
                editingLoan = null
            },
        )
    }

    repaymentDialogTarget?.let { (loanItem, existingRep) ->
        val repAccounts = remember(activeAccounts, state.accounts, existingRep) {
            val extra = existingRep?.accountId?.let { id ->
                state.accounts.firstOrNull { it.id == id && activeAccounts.none { a -> a.id == id } }
            }
            if (extra != null) activeAccounts + extra else activeAccounts
        }
        RepaymentEditorDialog(
            loanItem = loanItem,
            existingRepayment = existingRep,
            accounts = repAccounts,
            defaultAccountId = state.preferences.defaultAccountId,
            currency = currency,
            datePattern = datePattern,
            onDismiss = { repaymentDialogTarget = null },
            onSave = { rep ->
                onSaveRepayment(rep)
                repaymentDialogTarget = null
            },
        )
    }

    val inspectingItem = inspectingLoanId?.let { id -> state.loans.firstOrNull { it.loan.id == id } }
    inspectingItem?.let { item ->
        LoanDetailSheet(
            item = item,
            accountMap = accountMap,
            currency = currency,
            datePattern = datePattern,
            hideBalances = hideBalances,
            onDismiss = { inspectingLoanId = null },
            onAddRepayment = { repaymentDialogTarget = item to null },
            onEditLoan = {
                editingLoan = item.loan
                defaultDirection = item.loan.direction
                showLoanEditor = true
            },
            onDeleteLoan = {
                inspectingLoanId = null
                onDeleteLoan(item.loan.id)
            },
            onEditRepayment = { rep -> repaymentDialogTarget = item to rep },
            onDeleteRepayment = onDeleteRepayment,
        )
    }

    val filteredLoans = remember(state.loans, searchQuery, selectedFilter) {
        val q = searchQuery.trim().lowercase()
        state.loans.filter { item ->
            val matchesFilter = selectedFilter.status == null || item.status == selectedFilter.status
            val matchesQuery = q.isEmpty() ||
                item.loan.personName.lowercase().contains(q) ||
                item.loan.purpose.lowercase().contains(q)
            matchesFilter && matchesQuery
        }
    }

    val lentLoans = remember(filteredLoans) {
        filteredLoans.filter { it.loan.direction == LoanDirection.LENT }
    }
    val borrowedLoans = remember(filteredLoans) {
        filteredLoans.filter { it.loan.direction == LoanDirection.BORROWED }
    }

    val summary = state.loanSummary

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Loans & Borrowings",
                            style = MaterialTheme.typography.headlineSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "Receivables & Payables • Offline Ledger",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Button(
                    onClick = {
                        editingLoan = null
                        defaultDirection = LoanDirection.LENT
                        showLoanEditor = true
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.height(44.dp).testTag("loans_add_button"),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add", maxLines = 1, softWrap = false)
                }
            }
        }

        // 6 Summary Cards
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    LoanSummaryTile(
                        label = "Total Lent",
                        amountCents = summary.totalLentCents,
                        currency = currency,
                        hideBalances = hideBalances,
                        accentColor = ChampagneGold,
                        modifier = Modifier.weight(1f),
                    )
                    LoanSummaryTile(
                        label = "Total Borrowed",
                        amountCents = summary.totalBorrowedCents,
                        currency = currency,
                        hideBalances = hideBalances,
                        accentColor = ChampagneGold,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    LoanSummaryTile(
                        label = "Total Received Back",
                        amountCents = summary.totalReceivedBackCents,
                        currency = currency,
                        hideBalances = hideBalances,
                        accentColor = SemanticIncome,
                        modifier = Modifier.weight(1f),
                    )
                    LoanSummaryTile(
                        label = "Total Paid Back",
                        amountCents = summary.totalPaidBackCents,
                        currency = currency,
                        hideBalances = hideBalances,
                        accentColor = SemanticIncome,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    LoanSummaryTile(
                        label = "Outstanding Receivables",
                        amountCents = summary.outstandingReceivablesCents,
                        currency = currency,
                        hideBalances = hideBalances,
                        accentColor = if (summary.outstandingReceivablesCents > 0L) SemanticIncome else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    LoanSummaryTile(
                        label = "Outstanding Payables",
                        amountCents = summary.outstandingPayablesCents,
                        currency = currency,
                        hideBalances = hideBalances,
                        accentColor = if (summary.outstandingPayablesCents > 0L) SemanticExpense else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by person's name...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search loans") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("loans_search_input"),
            )
        }

        // Status Filter Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LoanFilterOption.entries.forEach { opt ->
                    val selected = selectedFilter == opt
                    FilterChip(
                        selected = selected,
                        onClick = { selectedFilter = opt },
                        label = { Text(opt.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = MaterialTheme.colorScheme.outlineVariant,
                            selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        ),
                        modifier = Modifier.testTag("loans_filter_${opt.name.lowercase()}"),
                    )
                }
            }
        }

        // Section 1: Money I Lent
        item {
            LoanSectionHeader(
                title = "Money I Lent",
                subtitle = "${lentLoans.size} receivable record(s)",
                icon = Icons.Rounded.ArrowUpward,
                accent = SemanticIncome,
                onQuickAdd = {
                    editingLoan = null
                    defaultDirection = LoanDirection.LENT
                    showLoanEditor = true
                },
            )
        }

        if (lentLoans.isEmpty()) {
            item {
                LoanEmptySectionCard(
                    message = if (state.loans.none { it.loan.direction == LoanDirection.LENT }) {
                        "No money lent recorded yet. Track money you give to friends, family, or colleagues."
                    } else {
                        "No lent records match the current filter."
                    },
                )
            }
        } else {
            items(lentLoans, key = { "lent_${it.loan.id}" }) { item ->
                LoanRecordCard(
                    item = item,
                    accountMap = accountMap,
                    currency = currency,
                    datePattern = datePattern,
                    hideBalances = hideBalances,
                    onClick = { inspectingLoanId = item.loan.id },
                    onQuickRepay = { repaymentDialogTarget = item to null },
                )
            }
        }

        // Section 2: Money I Borrowed
        item {
            LoanSectionHeader(
                title = "Money I Borrowed",
                subtitle = "${borrowedLoans.size} payable record(s)",
                icon = Icons.Rounded.ArrowDownward,
                accent = SemanticExpense,
                onQuickAdd = {
                    editingLoan = null
                    defaultDirection = LoanDirection.BORROWED
                    showLoanEditor = true
                },
            )
        }

        if (borrowedLoans.isEmpty()) {
            item {
                LoanEmptySectionCard(
                    message = if (state.loans.none { it.loan.direction == LoanDirection.BORROWED }) {
                        "No money borrowed recorded yet. Keep track of loans and personal payables here."
                    } else {
                        "No borrowed records match the current filter."
                    },
                )
            }
        } else {
            items(borrowedLoans, key = { "borrowed_${it.loan.id}" }) { item ->
                LoanRecordCard(
                    item = item,
                    accountMap = accountMap,
                    currency = currency,
                    datePattern = datePattern,
                    hideBalances = hideBalances,
                    onClick = { inspectingLoanId = item.loan.id },
                    onQuickRepay = { repaymentDialogTarget = item to null },
                )
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun LoanSummaryTile(
    label: String,
    amountCents: Long,
    currency: String,
    hideBalances: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = Formatters.formatMoney(amountCents, currency, hideBalances),
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = accentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun LoanSectionHeader(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    onQuickAdd: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f),
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        TextButton(onClick = onQuickAdd) {
            Text("+ Add", maxLines = 1, softWrap = false)
        }
    }
}

@Composable
private fun LoanRecordCard(
    item: LoanStatusItem,
    accountMap: Map<Long, AccountEntity>,
    currency: String,
    datePattern: String,
    hideBalances: Boolean,
    onClick: () -> Unit,
    onQuickRepay: () -> Unit,
) {
    val loan = item.loan
    val statusColor = loanStatusColor(item.status)
    val isLent = loan.direction == LoanDirection.LENT
    val linkedAccountName = loan.accountId?.let { accountMap[it]?.name ?: "Removed Account" }
    val remainingColor = when {
        item.remainingCents == 0L -> SemanticIncome
        item.status == LoanStatus.OVERDUE -> SemanticExpense
        isLent -> SemanticIncome
        else -> SemanticExpense
    }
    val shape = RoundedCornerShape(20.dp)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick)
            .testTag("loan_card_${loan.id}"),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = loan.personName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val metaParts = buildList {
                        if (linkedAccountName != null) {
                            add(if (isLent) "From: $linkedAccountName" else "To: $linkedAccountName")
                        }
                        if (loan.purpose.isNotBlank()) add(loan.purpose)
                    }
                    if (metaParts.isNotEmpty()) {
                        Text(
                            text = metaParts.joinToString(" • "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(statusColor.copy(alpha = 0.16f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = item.status.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "Original Amount",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = Formatters.formatMoney(loan.principalAmountCents, currency, hideBalances),
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Remaining",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = Formatters.formatMoney(item.remainingCents, currency, hideBalances),
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = remainingColor,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(item.progressPercent / 100f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(statusColor),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = loan.dueDateMs?.let { "Due: ${Formatters.formatDateOnly(it, datePattern)}" }
                        ?: "${if (isLent) "Given" else "Borrowed"}: ${Formatters.formatDateOnly(loan.loanDateMs, datePattern)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (item.status == LoanStatus.OVERDUE) SemanticExpense else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (item.remainingCents > 0L) {
                    OutlinedButton(
                        onClick = onQuickRepay,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(36.dp).testTag("loan_repay_${loan.id}"),
                    ) {
                        Text(
                            text = if (isLent) "Receive" else "Pay Back",
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoanEmptySectionCard(message: String) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Handshake,
            contentDescription = null,
            tint = ChampagneGold.copy(alpha = 0.7f),
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
