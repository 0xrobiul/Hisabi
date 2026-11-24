package me.robiul.hisabi.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.robiul.hisabi.data.local.AccountEntity
import me.robiul.hisabi.data.local.LoanDirection
import me.robiul.hisabi.data.local.LoanRecordEntity
import me.robiul.hisabi.data.local.LoanRepaymentEntity
import me.robiul.hisabi.data.local.LoanStatus
import me.robiul.hisabi.data.local.RepaymentMethod
import me.robiul.hisabi.ui.theme.ChampagneGold
import me.robiul.hisabi.ui.theme.InterFontFamily
import me.robiul.hisabi.ui.theme.SemanticExpense
import me.robiul.hisabi.ui.theme.SemanticIncome
import me.robiul.hisabi.ui.theme.SemanticTransfer
import me.robiul.hisabi.ui.viewmodel.LoanStatusItem
import me.robiul.hisabi.util.Formatters
import me.robiul.hisabi.util.InputValidation
import me.robiul.hisabi.util.LoanCalculations

@Composable
fun loanStatusColor(status: LoanStatus): Color = when (status) {
    LoanStatus.ACTIVE -> ChampagneGold
    LoanStatus.PARTIALLY_REPAID -> SemanticTransfer
    LoanStatus.FULLY_REPAID -> SemanticIncome
    LoanStatus.OVERDUE -> SemanticExpense
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanEditorDialog(
    existing: LoanRecordEntity?,
    initialDirection: LoanDirection = LoanDirection.LENT,
    accounts: List<AccountEntity>,
    defaultAccountId: Long? = null,
    currency: String,
    datePattern: String,
    onDismiss: () -> Unit,
    onSave: (LoanRecordEntity) -> Unit,
) {
    var direction by remember { mutableStateOf(existing?.direction ?: initialDirection) }
    var personName by remember { mutableStateOf(existing?.personName ?: "") }
    var personTouched by remember { mutableStateOf(false) }
    var amountInput by remember {
        mutableStateOf(existing?.let { Formatters.centsToEditableString(it.principalAmountCents) } ?: "")
    }
    var amountTouched by remember { mutableStateOf(false) }
    var selectedAccountId by remember {
        mutableStateOf(
            if (existing != null) existing.accountId
            else defaultAccountId?.takeIf { id -> accounts.any { it.id == id } }
                ?: accounts.firstOrNull()?.id
        )
    }
    var loanDateMs by remember { mutableLongStateOf(existing?.loanDateMs ?: System.currentTimeMillis()) }
    var hasDueDate by remember { mutableStateOf(existing?.dueDateMs != null) }
    var dueDateMs by remember {
        mutableLongStateOf(existing?.dueDateMs ?: (System.currentTimeMillis() + 30L * 24 * 3600 * 1000))
    }
    var purpose by remember { mutableStateOf(existing?.purpose ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var reminderEnabled by remember { mutableStateOf(existing?.reminderEnabled ?: true) }

    var showLoanDatePicker by remember { mutableStateOf(false) }
    var showDueDatePicker by remember { mutableStateOf(false) }

    if (showLoanDatePicker) {
        val dpState = rememberDatePickerState(initialSelectedDateMillis = loanDateMs)
        DatePickerDialog(
            onDismissRequest = { showLoanDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        dpState.selectedDateMillis?.let { loanDateMs = it }
                        showLoanDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showLoanDatePicker = false }) { Text("Cancel") } },
        ) { DatePicker(state = dpState) }
    }

    if (showDueDatePicker) {
        val dpState = rememberDatePickerState(initialSelectedDateMillis = dueDateMs)
        DatePickerDialog(
            onDismissRequest = { showDueDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        dpState.selectedDateMillis?.let {
                            dueDateMs = it
                            hasDueDate = true
                        }
                        showDueDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDueDatePicker = false }) { Text("Cancel") } },
        ) { DatePicker(state = dpState) }
    }

    val amountCents = Formatters.parseAmountToCents(amountInput) ?: 0L
    val alreadyRepaid = existing?.repaidAmountCents ?: 0L
    val belowRepaid = existing != null && amountCents > 0L && amountCents < alreadyRepaid
    val personError = if (personTouched && personName.trim().isEmpty()) "Name is required" else null
    val amountError = when {
        belowRepaid -> "Cannot be less than repaid ${Formatters.formatMoney(alreadyRepaid, currency)}"
        amountTouched -> InputValidation.validatePositiveAmountError(amountInput, currency)
        else -> null
    }
    val canSave = personName.trim().isNotEmpty() && amountCents > 0L && !belowRepaid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existing == null) "Record Loan / Borrowing" else "Edit Loan Record",
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    LoanDirection.entries.forEach { dir ->
                        val selected = direction == dir
                        FilterChip(
                            selected = selected,
                            onClick = { direction = dir },
                            label = {
                                Text(
                                    text = if (dir == LoanDirection.LENT) "I Lent" else "I Borrowed",
                                    maxLines = 1,
                                    softWrap = false,
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary,
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("loan_direction_${dir.name.lowercase()}"),
                        )
                    }
                }

                OutlinedTextField(
                    value = personName,
                    onValueChange = {
                        personTouched = true
                        if (it.length <= 50) personName = it
                    },
                    label = {
                        Text(if (direction == LoanDirection.LENT) "Person's Name" else "Lender's Name")
                    },
                    isError = personError != null,
                    supportingText = personError?.let { msg -> { Text(msg) } },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("loan_person_input"),
                )

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = {
                        amountTouched = true
                        amountInput = InputValidation.sanitizeDecimalInput(it)
                    },
                    label = { Text("Amount ($currency)") },
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = amountError != null,
                    supportingText = amountError?.let { msg -> { Text(msg) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("loan_amount_input"),
                )

                val accountTitle = if (direction == LoanDirection.LENT) {
                    "Source Account (Deducted From)"
                } else {
                    "Destination Account (Added To)"
                }
                val selectedAccount = accounts.firstOrNull { it.id == selectedAccountId }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = accountTitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        accounts.forEach { acc ->
                            FilterChip(
                                selected = selectedAccountId == acc.id,
                                onClick = { selectedAccountId = acc.id },
                                label = { Text(acc.name, maxLines = 1, softWrap = false) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                                ),
                                modifier = Modifier.testTag("loan_account_${acc.id}"),
                            )
                        }
                        FilterChip(
                            selected = selectedAccountId == null,
                            onClick = { selectedAccountId = null },
                            label = { Text("No Account", maxLines = 1, softWrap = false) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary,
                            ),
                            modifier = Modifier.testTag("loan_account_none"),
                        )
                    }
                    val accountHint = when {
                        selectedAccount != null -> {
                            val action = if (direction == LoanDirection.LENT) "Deducted from" else "Added to"
                            "$action ${selectedAccount.name} (${Formatters.formatMoney(selectedAccount.currentBalanceCents, currency)})"
                        }
                        accounts.isEmpty() -> "No accounts created yet. Tracked in Loans only."
                        else -> "Account balances won't change (track only)."
                    }
                    Text(
                        text = accountHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                OutlinedButton(
                    onClick = { showLoanDatePicker = true },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    Icon(Icons.Rounded.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    val datePrefix = if (direction == LoanDirection.LENT) "Given" else "Borrowed"
                    Text("$datePrefix: ${Formatters.formatDateOnly(loanDateMs, datePattern)}")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Expected Repayment Date", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = hasDueDate,
                        onCheckedChange = { hasDueDate = it },
                        modifier = Modifier.testTag("loan_due_date_switch"),
                    )
                }

                if (hasDueDate) {
                    OutlinedButton(
                        onClick = { showDueDatePicker = true },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                    ) {
                        Icon(Icons.Rounded.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Due: ${Formatters.formatDateOnly(dueDateMs, datePattern)}")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                Icons.Rounded.NotificationsActive,
                                contentDescription = null,
                                tint = ChampagneGold,
                                modifier = Modifier.size(16.dp),
                            )
                            Text("Due Reminder", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(
                            checked = reminderEnabled,
                            onCheckedChange = { reminderEnabled = it },
                        )
                    }
                }

                OutlinedTextField(
                    value = purpose,
                    onValueChange = { purpose = it },
                    label = { Text("Purpose / Description (optional)") },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("loan_purpose_input"),
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    shape = RoundedCornerShape(16.dp),
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("loan_notes_input"),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        LoanRecordEntity(
                            id = existing?.id ?: 0L,
                            direction = direction,
                            personName = personName.trim(),
                            principalAmountCents = amountCents,
                            repaidAmountCents = existing?.repaidAmountCents ?: 0L,
                            loanDateMs = loanDateMs,
                            dueDateMs = if (hasDueDate) dueDateMs else null,
                            accountId = selectedAccountId,
                            purpose = purpose.trim(),
                            notes = notes.trim(),
                            reminderEnabled = hasDueDate && reminderEnabled,
                            lastReminderKey = existing?.lastReminderKey ?: "",
                            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis(),
                        )
                    )
                },
                enabled = canSave,
                modifier = Modifier.testTag("loan_save_button"),
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepaymentEditorDialog(
    loanItem: LoanStatusItem,
    existingRepayment: LoanRepaymentEntity?,
    accounts: List<AccountEntity>,
    defaultAccountId: Long? = null,
    currency: String,
    datePattern: String,
    onDismiss: () -> Unit,
    onSave: (LoanRepaymentEntity) -> Unit,
) {
    val maxAllowedCents = loanItem.remainingCents + (existingRepayment?.amountCents ?: 0L)
    var amountInput by remember {
        mutableStateOf(existingRepayment?.let { Formatters.centsToEditableString(it.amountCents) } ?: "")
    }
    var amountTouched by remember { mutableStateOf(false) }
    var timestamp by remember { mutableLongStateOf(existingRepayment?.timestamp ?: System.currentTimeMillis()) }
    var selectedAccountId by remember {
        mutableStateOf(
            if (existingRepayment != null) existingRepayment.accountId
            else loanItem.loan.accountId?.takeIf { id -> accounts.any { it.id == id } }
                ?: defaultAccountId?.takeIf { id -> accounts.any { it.id == id } }
                ?: accounts.firstOrNull()?.id
        )
    }
    var method by remember {
        mutableStateOf(
            existingRepayment?.method
                ?: accounts.firstOrNull { it.id == selectedAccountId }
                    ?.let { LoanCalculations.defaultRepaymentMethodForAccount(it.accountType) }
                ?: RepaymentMethod.CASH
        )
    }
    var notes by remember { mutableStateOf(existingRepayment?.notes ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val dpState = rememberDatePickerState(initialSelectedDateMillis = timestamp)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        dpState.selectedDateMillis?.let { timestamp = it }
                        showDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
        ) { DatePicker(state = dpState) }
    }

    val amountCents = Formatters.parseAmountToCents(amountInput) ?: 0L
    val exceedsRemaining = amountCents > maxAllowedCents
    val repaymentError = when {
        exceedsRemaining -> "Cannot exceed remaining balance (${Formatters.formatMoney(maxAllowedCents, currency)})"
        amountTouched -> InputValidation.validatePositiveAmountError(amountInput, currency, maxCents = maxAllowedCents)
        else -> null
    }
    val isValid = LoanCalculations.validateRepaymentAmount(
        principalCents = loanItem.loan.principalAmountCents,
        currentRepaidCents = loanItem.loan.repaidAmountCents,
        newRepaymentCents = amountCents,
        existingRepaymentCents = existingRepayment?.amountCents ?: 0L,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existingRepayment == null) "Record Repayment" else "Edit Repayment",
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = loanItem.loan.personName,
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = "Max allowed: ${Formatters.formatMoney(maxAllowedCents, currency)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(
                            onClick = {
                                amountTouched = true
                                amountInput = Formatters.centsToEditableString(maxAllowedCents)
                            },
                            modifier = Modifier.testTag("repayment_full_remaining_button"),
                        ) { Text("Full") }
                    }
                }

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = {
                        amountTouched = true
                        amountInput = InputValidation.sanitizeDecimalInput(it)
                    },
                    label = { Text("Repayment Amount ($currency)") },
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = repaymentError != null,
                    supportingText = repaymentError?.let { msg -> { Text(msg) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("repayment_amount_input"),
                )

                OutlinedButton(
                    onClick = { showDatePicker = true },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    Icon(Icons.Rounded.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Date: ${Formatters.formatDateOnly(timestamp, datePattern)}")
                }

                val repAccountTitle = if (loanItem.loan.direction == LoanDirection.LENT) {
                    "Destination Account (Received Into)"
                } else {
                    "Source Account (Paid From)"
                }
                val repSelectedAccount = accounts.firstOrNull { it.id == selectedAccountId }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = repAccountTitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        accounts.forEach { acc ->
                            FilterChip(
                                selected = selectedAccountId == acc.id,
                                onClick = {
                                    selectedAccountId = acc.id
                                    method = LoanCalculations.defaultRepaymentMethodForAccount(acc.accountType)
                                },
                                label = { Text(acc.name, maxLines = 1, softWrap = false) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                                ),
                                modifier = Modifier.testTag("repayment_account_${acc.id}"),
                            )
                        }
                        FilterChip(
                            selected = selectedAccountId == null,
                            onClick = { selectedAccountId = null },
                            label = { Text("No Account", maxLines = 1, softWrap = false) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary,
                            ),
                            modifier = Modifier.testTag("repayment_account_none"),
                        )
                    }
                    val repHint = when {
                        repSelectedAccount != null -> {
                            val verb = if (loanItem.loan.direction == LoanDirection.LENT) "Added to" else "Deducted from"
                            "$verb ${repSelectedAccount.name} (${Formatters.formatMoney(repSelectedAccount.currentBalanceCents, currency)})"
                        }
                        accounts.isEmpty() -> "No accounts created yet. Tracked in Loans only."
                        else -> "Account balances won't change (track only)."
                    }
                    Text(
                        text = repHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Text("Payment Method", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    RepaymentMethod.entries.forEach { m ->
                        FilterChip(
                            selected = method == m,
                            onClick = { method = m },
                            label = { Text(m.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary,
                            ),
                            modifier = Modifier.testTag("repayment_method_${m.name.lowercase()}"),
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    shape = RoundedCornerShape(16.dp),
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth().testTag("repayment_notes_input"),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        LoanRepaymentEntity(
                            id = existingRepayment?.id ?: 0L,
                            loanId = loanItem.loan.id,
                            amountCents = amountCents,
                            timestamp = timestamp,
                            method = method,
                            accountId = selectedAccountId,
                            notes = notes.trim(),
                            createdAt = existingRepayment?.createdAt ?: System.currentTimeMillis(),
                        )
                    )
                },
                enabled = isValid,
                modifier = Modifier.testTag("repayment_save_button"),
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanDetailSheet(
    item: LoanStatusItem,
    accountMap: Map<Long, AccountEntity>,
    currency: String,
    datePattern: String,
    hideBalances: Boolean,
    onDismiss: () -> Unit,
    onAddRepayment: () -> Unit,
    onEditLoan: () -> Unit,
    onDeleteLoan: () -> Unit,
    onEditRepayment: (LoanRepaymentEntity) -> Unit,
    onDeleteRepayment: (Long) -> Unit,
) {
    val loan = item.loan
    val statusColor = loanStatusColor(item.status)
    val isLent = loan.direction == LoanDirection.LENT
    val remainingAccent = if (item.remainingCents == 0L) {
        SemanticIncome
    } else if (isLent) {
        SemanticIncome
    } else {
        SemanticExpense
    }

    var confirmDeleteLoan by remember { mutableStateOf(false) }
    var pendingDeleteRepayment by remember { mutableStateOf<LoanRepaymentEntity?>(null) }

    if (confirmDeleteLoan) {
        AlertDialog(
            onDismissRequest = { confirmDeleteLoan = false },
            title = { Text("Delete Loan Record?") },
            text = { Text("Delete the record with ${loan.personName} and all its repayment history?") },
            confirmButton = {
                Button(
                    onClick = {
                        confirmDeleteLoan = false
                        onDeleteLoan()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDeleteLoan = false }) { Text("Cancel") } },
        )
    }

    pendingDeleteRepayment?.let { rep ->
        AlertDialog(
            onDismissRequest = { pendingDeleteRepayment = null },
            title = { Text("Delete Repayment?") },
            text = {
                Text(
                    "Remove repayment of ${Formatters.formatMoney(rep.amountCents, currency)}? Outstanding balance will be recalculated."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = rep.id
                        pendingDeleteRepayment = null
                        onDeleteRepayment(id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { pendingDeleteRepayment = null }) { Text("Cancel") } },
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = if (isLent) "RECEIVABLE • MONEY LENT" else "PAYABLE • MONEY BORROWED",
                        style = MaterialTheme.typography.labelSmall,
                        color = ChampagneGold,
                    )
                    Text(
                        text = loan.personName,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(statusColor.copy(alpha = 0.16f))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    ) {
                        Text(
                            text = item.status.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = statusColor,
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close details")
                    }
                }
            }

            // Metrics Summary Card
            val boxShape = RoundedCornerShape(18.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(boxShape)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, boxShape)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    LoanMetricColumn(
                        label = "Original",
                        value = Formatters.formatMoney(loan.principalAmountCents, currency, hideBalances),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    LoanMetricColumn(
                        label = if (isLent) "Received Back" else "Paid Back",
                        value = Formatters.formatMoney(loan.repaidAmountCents, currency, hideBalances),
                        color = SemanticIncome,
                    )
                    LoanMetricColumn(
                        label = "Remaining",
                        value = Formatters.formatMoney(item.remainingCents, currency, hideBalances),
                        color = remainingAccent,
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(item.progressPercent / 100f)
                            .height(7.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (item.status == LoanStatus.OVERDUE) SemanticExpense else SemanticIncome),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "${if (isLent) "Given" else "Borrowed"}: ${Formatters.formatDateOnly(loan.loanDateMs, datePattern)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = loan.dueDateMs?.let { "Due: ${Formatters.formatDateOnly(it, datePattern)}" } ?: "No due date",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (item.status == LoanStatus.OVERDUE) SemanticExpense else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (loan.accountId != null) {
                    val accName = accountMap[loan.accountId]?.name ?: "Removed Account"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (isLent) "Source Account" else "Destination Account",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = accName,
                            style = MaterialTheme.typography.labelMedium,
                            color = ChampagneGold,
                        )
                    }
                }

                if (loan.purpose.isNotBlank()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Text(
                        text = "Purpose: ${loan.purpose}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                if (loan.notes.isNotBlank()) {
                    Text(
                        text = loan.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = onAddRepayment,
                    enabled = item.remainingCents > 0L,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("loan_detail_repay_button"),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (item.remainingCents > 0L) "Record Repayment" else "Fully Repaid",
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                OutlinedButton(
                    onClick = onEditLoan,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.height(48.dp).testTag("loan_detail_edit_button"),
                ) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Edit loan", modifier = Modifier.size(18.dp))
                }
                OutlinedButton(
                    onClick = { confirmDeleteLoan = true },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.height(48.dp).testTag("loan_detail_delete_button"),
                ) {
                    Icon(
                        Icons.Rounded.DeleteOutline,
                        contentDescription = "Delete loan",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            // Repayment History Section
            Text(
                text = "Repayment History (${item.repayments.size})",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (item.repayments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No repayments recorded yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item.repayments.forEach { rep ->
                        val rowShape = RoundedCornerShape(16.dp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(rowShape)
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, rowShape)
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(
                                        text = Formatters.formatMoney(rep.amountCents, currency, hideBalances),
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp,
                                        color = SemanticIncome,
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(ChampagneGold.copy(alpha = 0.14f))
                                            .padding(horizontal = 7.dp, vertical = 2.dp),
                                    ) {
                                        val accSuffix = rep.accountId?.let { id ->
                                            val name = accountMap[id]?.name ?: "Removed"
                                            val arrow = if (isLent) "→" else "←"
                                            " $arrow $name"
                                        } ?: ""
                                        Text(
                                            text = "${rep.method.label}$accSuffix",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ChampagneGold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                                Text(
                                    text = Formatters.formatDateOnly(rep.timestamp, datePattern),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (rep.notes.isNotBlank()) {
                                    Text(
                                        text = rep.notes,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Row {
                                IconButton(
                                    onClick = { onEditRepayment(rep) },
                                    modifier = Modifier.testTag("repayment_edit_${rep.id}"),
                                ) {
                                    Icon(
                                        Icons.Rounded.Edit,
                                        contentDescription = "Edit repayment",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                                IconButton(
                                    onClick = { pendingDeleteRepayment = rep },
                                    modifier = Modifier.testTag("repayment_delete_${rep.id}"),
                                ) {
                                    Icon(
                                        Icons.Rounded.DeleteOutline,
                                        contentDescription = "Delete repayment",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun LoanMetricColumn(label: String, value: String, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = color,
        )
    }
}
