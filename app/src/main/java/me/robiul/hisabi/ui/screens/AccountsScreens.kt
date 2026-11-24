package me.robiul.hisabi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.robiul.hisabi.data.local.AccountEntity
import me.robiul.hisabi.data.local.TransactionEntity
import me.robiul.hisabi.ui.components.BankCardVisual
import me.robiul.hisabi.ui.components.TransactionRow
import me.robiul.hisabi.ui.viewmodel.LedgerUiState
import me.robiul.hisabi.util.Formatters
import me.robiul.hisabi.util.InputValidation

@Composable
fun AccountsManagerScreen(
    state: LedgerUiState,
    onNavigateBack: () -> Unit,
    onAddAccount: () -> Unit,
    onOpenAccountDetail: (Long) -> Unit,
    onOpenTransfer: (Long) -> Unit,
) {
    val currency = state.preferences.currencySymbol
    val hideBalances = state.preferences.hideBalances
    val active = state.accounts.filter { !it.isArchived }
    val archived = state.accounts.filter { it.isArchived }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                    Column {
                        Text(
                            text = "Accounts & Wallets",
                            style = MaterialTheme.typography.headlineSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${active.size} active • ${archived.size} archived",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
                Button(
                    onClick = onAddAccount,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.height(44.dp).testTag("accounts_add_button"),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New", maxLines = 1)
                }
            }
        }

        if (active.isEmpty() && archived.isEmpty()) {
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
                        text = "No accounts yet. Add Cash, Bank, bKash, Nagad, Rocket, Upay, or Credit Card.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        items(active, key = { it.id }) { acc ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BankCardVisual(
                    account = acc,
                    currencySymbol = currency,
                    hideBalances = hideBalances,
                    onClick = { onOpenAccountDetail(acc.id) },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = { onOpenAccountDetail(acc.id) },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f).height(48.dp),
                    ) { Text("Details & Adjust", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    if (active.size >= 2) {
                        OutlinedButton(
                            onClick = { onOpenTransfer(acc.id) },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f).height(48.dp),
                        ) {
                            Icon(Icons.Rounded.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Transfer", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }

        if (archived.isNotEmpty()) {
            item {
                Text(
                    text = "Archived Accounts",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            items(archived, key = { it.id }) { acc ->
                BankCardVisual(
                    account = acc,
                    currencySymbol = currency,
                    hideBalances = hideBalances,
                    isCompact = true,
                    onClick = { onOpenAccountDetail(acc.id) },
                )
            }
        }
    }
}

@Composable
fun AccountDetailScreen(
    accountId: Long,
    state: LedgerUiState,
    onNavigateBack: () -> Unit,
    onEditAccount: (AccountEntity) -> Unit,
    onAdjustBalance: (Long, Long, String) -> Unit,
    onTransferFromAccount: (Long) -> Unit,
    onSelectTransaction: (TransactionEntity) -> Unit,
) {
    val account = state.accounts.firstOrNull { it.id == accountId }
    if (account == null) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Account not found")
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onNavigateBack) { Text("Go Back") }
        }
        return
    }

    val currency = state.preferences.currencySymbol
    val hideBalances = state.preferences.hideBalances
    val datePattern = state.preferences.dateFormatPattern
    val accountMap = remember(state.accounts) { state.accounts.associateBy { it.id } }
    val categoryMap = remember(state.categories) { state.categories.associateBy { it.id } }
    val accountTxs = remember(state.transactions, accountId) {
        state.transactions.filter { it.accountId == accountId || it.toAccountId == accountId }
    }

    var showAdjustDialog by remember { mutableStateOf(false) }

    if (showAdjustDialog) {
        var targetInput by remember { mutableStateOf(Formatters.centsToEditableString(account.currentBalanceCents)) }
        var noteInput by remember { mutableStateOf("") }
        val parsed = Formatters.parseAmountToCents(targetInput)
        val adjustError = if (parsed == null) "Enter a valid balance amount" else null
        AlertDialog(
            onDismissRequest = { showAdjustDialog = false },
            title = { Text("Adjust ${account.name} Balance") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Enter the actual current balance. An Adjustment entry will be logged in transaction history.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = targetInput,
                        onValueChange = { targetInput = InputValidation.sanitizeDecimalInput(it, allowNegative = true) },
                        label = { Text("Actual Balance ($currency)") },
                        isError = adjustError != null,
                        supportingText = adjustError?.let { msg -> { Text(msg) } },
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("adjust_balance_input"),
                    )
                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it },
                        label = { Text("Reason / Note (optional)") },
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (parsed != null) {
                            onAdjustBalance(account.id, parsed, noteInput)
                            showAdjustDialog = false
                        }
                    },
                    enabled = parsed != null,
                    modifier = Modifier.testTag("confirm_adjust_balance_button"),
                ) { Text("Apply Adjustment") }
            },
            dismissButton = {
                TextButton(onClick = { showAdjustDialog = false }) { Text("Cancel") }
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
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = { onEditAccount(account) }, modifier = Modifier.testTag("account_detail_edit_button")) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Edit account")
                }
            }
        }

        item {
            BankCardVisual(
                account = account,
                currencySymbol = currency,
                hideBalances = hideBalances,
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = { showAdjustDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f).height(48.dp).testTag("account_adjust_balance_button"),
                ) {
                    Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Adjust Balance")
                }
                OutlinedButton(
                    onClick = { onTransferFromAccount(account.id) },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f).height(48.dp),
                ) {
                    Icon(Icons.Rounded.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Transfer")
                }
            }
        }

        item {
            Text(
                text = "Account History (${accountTxs.size})",
                style = MaterialTheme.typography.titleMedium,
            )
        }

        if (accountTxs.isEmpty()) {
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
                        text = "No transactions recorded for this account yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            items(accountTxs, key = { it.id }) { tx ->
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
    }
}
