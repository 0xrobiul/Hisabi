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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
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
import me.robiul.hisabi.data.local.SavingsGoalEntity
import me.robiul.hisabi.ui.theme.AccentGreen
import me.robiul.hisabi.ui.theme.InterFontFamily
import me.robiul.hisabi.ui.theme.SemanticSavings
import me.robiul.hisabi.ui.viewmodel.GoalStatus
import me.robiul.hisabi.ui.viewmodel.LedgerUiState
import me.robiul.hisabi.util.Formatters
import me.robiul.hisabi.util.InputValidation
import me.robiul.hisabi.util.iconForKey

@Composable
fun SavingsGoalsScreen(
    state: LedgerUiState,
    onNavigateBack: () -> Unit,
    onSaveGoal: (SavingsGoalEntity) -> Unit,
    onDeleteGoal: (Long) -> Unit,
    onContribute: (Long, Long, Long, String) -> Unit,
    onWithdraw: (Long, Long, Long, String) -> Unit,
) {
    val currency = state.preferences.currencySymbol
    val hideBalances = state.preferences.hideBalances
    val datePattern = state.preferences.dateFormatPattern

    var showGoalEditor by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf<SavingsGoalEntity?>(null) }
    var transferDialogGoal by remember { mutableStateOf<Pair<SavingsGoalEntity, Boolean>?>(null) } // Boolean: isContribute
    var pendingDeleteGoal by remember { mutableStateOf<SavingsGoalEntity?>(null) }

    pendingDeleteGoal?.let { g ->
        AlertDialog(
            onDismissRequest = { pendingDeleteGoal = null },
            title = { Text("Delete Savings Goal?") },
            text = { Text("Delete \"${g.name}\"? Consider withdrawing allocated funds back to an account first.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteGoal(g.id)
                        pendingDeleteGoal = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { pendingDeleteGoal = null }) { Text("Cancel") } },
        )
    }

    if (showGoalEditor) {
        GoalEditorDialog(
            existing = editingGoal,
            currency = currency,
            datePattern = datePattern,
            onDismiss = {
                showGoalEditor = false
                editingGoal = null
            },
            onSave = { g ->
                onSaveGoal(g)
                showGoalEditor = false
                editingGoal = null
            },
        )
    }

    transferDialogGoal?.let { (goal, isContribute) ->
        GoalFundingDialog(
            goal = goal,
            isContribute = isContribute,
            state = state,
            currency = currency,
            onDismiss = { transferDialogGoal = null },
            onConfirm = { accId, amountCents, note ->
                if (isContribute) onContribute(goal.id, accId, amountCents, note)
                else onWithdraw(goal.id, accId, amountCents, note)
                transferDialogGoal = null
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
                        text = "Savings Goals",
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Button(
                    onClick = {
                        editingGoal = null
                        showGoalEditor = true
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.height(44.dp).testTag("goals_add_button"),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Goal", maxLines = 1, softWrap = false)
                }
            }
        }

        if (state.goalStatuses.isEmpty()) {
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
                        text = "No savings goals yet. Set an Emergency Fund, Gadget, or Travel goal and allocate funds from any account.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            items(state.goalStatuses, key = { it.goal.id }) { status ->
                GoalCard(
                    status = status,
                    currency = currency,
                    datePattern = datePattern,
                    hideBalances = hideBalances,
                    canFund = state.activeAccounts.isNotEmpty(),
                    onContribute = { transferDialogGoal = status.goal to true },
                    onWithdraw = { transferDialogGoal = status.goal to false },
                    onDelete = { pendingDeleteGoal = status.goal },
                )
            }
        }
    }
}

@Composable
private fun GoalCard(
    status: GoalStatus,
    currency: String,
    datePattern: String,
    hideBalances: Boolean,
    canFund: Boolean,
    onContribute: () -> Unit,
    onWithdraw: () -> Unit,
    onDelete: () -> Unit,
) {
    val g = status.goal
    val accent = Color(g.colorHex)
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(iconForKey(g.iconKey), contentDescription = g.name, tint = accent, modifier = Modifier.size(20.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = g.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (g.isCompleted) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = "Completed", tint = AccentGreen, modifier = Modifier.size(16.dp))
                        }
                    }
                    Text(
                        text = "Target: ${Formatters.formatDateOnly(g.targetDateMs, datePattern)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete goal", tint = MaterialTheme.colorScheme.error)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(status.progressPercent / 100f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (g.isCompleted) AccentGreen else SemanticSavings),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Saved ${Formatters.formatMoney(g.savedAmountCents, currency, hideBalances)} (${status.progressPercent}%)",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
            )
            Text(
                text = "Remaining ${Formatters.formatMoney(status.remainingCents, currency, hideBalances)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = onContribute,
                enabled = canFund,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f).height(46.dp).testTag("goal_contribute_${g.id}"),
            ) { Text("Contribute") }
            OutlinedButton(
                onClick = onWithdraw,
                enabled = canFund && g.savedAmountCents > 0L,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f).height(46.dp),
            ) { Text("Withdraw") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalEditorDialog(
    existing: SavingsGoalEntity?,
    currency: String,
    datePattern: String,
    onDismiss: () -> Unit,
    onSave: (SavingsGoalEntity) -> Unit,
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var targetInput by remember {
        mutableStateOf(existing?.let { Formatters.centsToEditableString(it.targetAmountCents) } ?: "")
    }
    var nameTouched by remember { mutableStateOf(false) }
    var targetTouched by remember { mutableStateOf(false) }
    var targetDateMs by remember {
        mutableLongStateOf(existing?.targetDateMs ?: (System.currentTimeMillis() + 90L * 24 * 3600 * 1000))
    }
    var showDatePicker by remember { mutableStateOf(false) }

    val nameError = if (nameTouched && name.trim().isEmpty()) "Goal name is required" else null
    val targetError = if (targetTouched) {
        InputValidation.validatePositiveAmountError(targetInput, currencySymbol = currency)
    } else null
    val targetCents = Formatters.parseAmountToCents(targetInput) ?: 0L
    val canSave = name.trim().isNotEmpty() && targetCents > 0L

    if (showDatePicker) {
        val dpState = rememberDatePickerState(initialSelectedDateMillis = targetDateMs)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        dpState.selectedDateMillis?.let { targetDateMs = it }
                        showDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
        ) { DatePicker(state = dpState) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New Savings Goal" else "Edit Savings Goal") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        nameTouched = true
                        name = it
                    },
                    label = { Text("Goal Name (e.g. Emergency Fund, Laptop)") },
                    isError = nameError != null,
                    supportingText = nameError?.let { msg -> { Text(msg) } },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("goal_name_input"),
                )
                OutlinedTextField(
                    value = targetInput,
                    onValueChange = {
                        targetTouched = true
                        targetInput = InputValidation.sanitizeDecimalInput(it)
                    },
                    label = { Text("Target Amount ($currency)") },
                    isError = targetError != null,
                    supportingText = targetError?.let { msg -> { Text(msg) } },
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("goal_target_input"),
                )
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Target: ${Formatters.formatDateOnly(targetDateMs, datePattern)}",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    nameTouched = true
                    targetTouched = true
                    if (!canSave) return@Button
                    onSave(
                        SavingsGoalEntity(
                            id = existing?.id ?: 0L,
                            name = name.trim(),
                            targetAmountCents = targetCents,
                            savedAmountCents = existing?.savedAmountCents ?: 0L,
                            targetDateMs = targetDateMs,
                            iconKey = existing?.iconKey ?: "savings",
                            colorHex = existing?.colorHex ?: 0xFFD6B46AL,
                            isCompleted = (existing?.savedAmountCents ?: 0L) >= targetCents,
                            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                        )
                    )
                },
                enabled = canSave,
                modifier = Modifier.testTag("goal_save_button"),
            ) { Text("Save Goal") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun GoalFundingDialog(
    goal: SavingsGoalEntity,
    isContribute: Boolean,
    state: LedgerUiState,
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (Long, Long, String) -> Unit,
) {
    var selectedAccountId by remember { mutableStateOf(state.activeAccounts.firstOrNull()?.id) }
    var amountInput by remember { mutableStateOf("") }
    var amountTouched by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }

    val amountError = if (amountTouched) {
        InputValidation.validatePositiveAmountError(
            input = amountInput,
            currencySymbol = currency,
            maxCents = if (isContribute) null else goal.savedAmountCents,
            maxErrorLabel = "Cannot exceed saved balance (${Formatters.formatMoney(goal.savedAmountCents, currency)})",
        )
    } else null
    val amountCents = Formatters.parseAmountToCents(amountInput) ?: 0L
    val canConfirm = selectedAccountId != null &&
        amountCents > 0L &&
        (isContribute || amountCents <= goal.savedAmountCents)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isContribute) "Contribute to ${goal.name}" else "Withdraw from ${goal.name}",
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = if (isContribute) "Select source account" else "Select destination account",
                    style = MaterialTheme.typography.labelMedium,
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    state.activeAccounts.forEach { acc ->
                        FilterChip(
                            selected = selectedAccountId == acc.id,
                            onClick = { selectedAccountId = acc.id },
                            label = { Text(acc.name, maxLines = 1, softWrap = false) },
                        )
                    }
                }
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = {
                        amountTouched = true
                        amountInput = InputValidation.sanitizeDecimalInput(it)
                    },
                    label = { Text("Amount ($currency)") },
                    isError = amountError != null,
                    supportingText = amountError?.let { msg -> { Text(msg) } }
                        ?: if (!isContribute) {
                            { Text("Available to withdraw: ${Formatters.formatMoney(goal.savedAmountCents, currency)}") }
                        } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("goal_fund_amount_input"),
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    amountTouched = true
                    val accId = selectedAccountId ?: return@Button
                    if (!canConfirm) return@Button
                    onConfirm(accId, amountCents, note)
                },
                enabled = canConfirm,
                modifier = Modifier.testTag("goal_fund_confirm_button"),
            ) { Text(if (isContribute) "Allocate" else "Withdraw") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
