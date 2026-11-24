package me.robiul.hisabi.ui.components

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.robiul.hisabi.data.local.AccountEntity
import me.robiul.hisabi.data.local.AccountType
import me.robiul.hisabi.data.local.CardNetwork
import me.robiul.hisabi.ui.theme.CardPalettes
import me.robiul.hisabi.util.Formatters
import me.robiul.hisabi.util.InputValidation
import me.robiul.hisabi.util.iconForKey

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AccountEditorSheet(
    existingAccount: AccountEntity?,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (AccountEntity) -> Unit,
    onArchiveToggle: ((AccountEntity) -> Unit)? = null,
    onDelete: ((Long) -> Unit)? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var accountType by remember { mutableStateOf(existingAccount?.accountType ?: AccountType.CASH) }
    var customTypeName by remember { mutableStateOf(existingAccount?.customTypeName ?: "") }
    var customTypeTouched by remember { mutableStateOf(false) }
    var network by remember { mutableStateOf(existingAccount?.network ?: CardNetwork.CASH) }
    var name by remember { mutableStateOf(existingAccount?.name ?: AccountType.CASH.label) }
    var isNameAutoManaged by remember { mutableStateOf(existingAccount == null) }
    var nameTouched by remember { mutableStateOf(false) }
    var lastFour by remember { mutableStateOf(existingAccount?.lastFourDigits?.filter { it.isDigit() } ?: "") }
    var openingBalanceInput by remember {
        mutableStateOf(
            existingAccount?.let { Formatters.centsToEditableString(it.openingBalanceCents) } ?: ""
        )
    }
    var creditLimitInput by remember {
        mutableStateOf(
            existingAccount?.takeIf { it.creditLimitCents > 0L }
                ?.let { Formatters.centsToEditableString(it.creditLimitCents) } ?: ""
        )
    }
    var styleIndex by remember { mutableIntStateOf(existingAccount?.cardStyleIndex ?: accountType.defaultPaletteIndex) }
    var iconKey by remember { mutableStateOf(existingAccount?.iconKey ?: accountType.defaultIconKey) }
    var isPinned by remember { mutableStateOf(existingAccount?.isPinned ?: false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val parsedOpening = Formatters.parseAmountToCents(openingBalanceInput) ?: 0L
    val parsedLimit = Formatters.parseAmountToCents(creditLimitInput) ?: 0L
    val previewBalance = if (existingAccount != null) {
        existingAccount.currentBalanceCents + (parsedOpening - existingAccount.openingBalanceCents)
    } else {
        parsedOpening
    }

    val previewAccount = AccountEntity(
        id = existingAccount?.id ?: 0L,
        name = name.ifBlank { accountType.label },
        accountType = accountType,
        customTypeName = customTypeName,
        network = network,
        lastFourDigits = lastFour.filter { it.isDigit() }.takeLast(4).padStart(4, '•'),
        openingBalanceCents = parsedOpening,
        currentBalanceCents = previewBalance,
        creditLimitCents = if (accountType == AccountType.CREDIT_CARD) parsedLimit else 0L,
        cardStyleIndex = styleIndex,
        iconKey = iconKey,
        isArchived = existingAccount?.isArchived ?: false,
        isPinned = isPinned,
    )

    if (showDeleteConfirm && existingAccount != null && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete ${existingAccount.name}?") },
            text = {
                Text("This permanently deletes this account and all transactions associated with it. Consider archiving instead to preserve history.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(existingAccount.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
        )
    }

    val nameError = if (nameTouched && name.trim().isEmpty()) "Account name is required" else null
    val customTypeError = if (accountType == AccountType.CUSTOM && customTypeTouched && customTypeName.trim().isEmpty()) {
        "Enter custom account label"
    } else null
    val openingError = InputValidation.validateOptionalNonNegativeAmountError(openingBalanceInput)
    val lastFourError = InputValidation.validateLastFourDigitsError(lastFour)
    val creditLimitError = if (accountType == AccountType.CREDIT_CARD) {
        InputValidation.validateOptionalNonNegativeAmountError(creditLimitInput)
    } else null

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (existingAccount == null) "New Account / Wallet" else "Edit Account",
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (existingAccount != null) {
                    Row {
                        if (onArchiveToggle != null) {
                            TextButton(onClick = { onArchiveToggle(existingAccount) }) {
                                Icon(
                                    imageVector = if (existingAccount.isArchived) Icons.Rounded.Unarchive else Icons.Rounded.Archive,
                                    contentDescription = "Archive toggle",
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(if (existingAccount.isArchived) " Restore" else " Archive")
                            }
                        }
                        if (onDelete != null) {
                            TextButton(
                                onClick = { showDeleteConfirm = true },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            ) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            BankCardVisual(
                account = previewAccount,
                currencySymbol = currencySymbol,
                hideBalances = false,
                isCompact = true,
            )

            // Default Account Types
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Account Type",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    AccountType.entries.forEach { type ->
                        FilterChip(
                            selected = accountType == type,
                            onClick = {
                                accountType = type
                                styleIndex = type.defaultPaletteIndex
                                iconKey = type.defaultIconKey
                                network = when (type) {
                                    AccountType.CASH -> CardNetwork.CASH
                                    AccountType.BANK -> CardNetwork.BANK
                                    AccountType.CREDIT_CARD -> CardNetwork.VISA
                                    AccountType.BKASH, AccountType.NAGAD, AccountType.ROCKET, AccountType.UPAY -> CardNetwork.MFS
                                    AccountType.CUSTOM -> CardNetwork.BANK
                                }
                                if (existingAccount == null && isNameAutoManaged) {
                                    name = if (type == AccountType.CUSTOM) "" else type.label
                                    nameTouched = false
                                }
                            },
                            label = { Text(type.label, maxLines = 1, softWrap = false) },
                        )
                    }
                }
            }

            if (accountType == AccountType.CUSTOM) {
                OutlinedTextField(
                    value = customTypeName,
                    onValueChange = {
                        customTypeTouched = true
                        if (it.length <= 24) customTypeName = it
                    },
                    label = { Text("Custom Type Label (e.g. FDR, Savings Box)") },
                    singleLine = true,
                    isError = customTypeError != null,
                    supportingText = customTypeError?.let { msg -> { Text(msg) } },
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (accountType == AccountType.CREDIT_CARD || accountType == AccountType.BANK) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Card / Bank Network",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        listOf(CardNetwork.VISA, CardNetwork.MASTERCARD, CardNetwork.AMEX, CardNetwork.BANK).forEach { net ->
                            FilterChip(
                                selected = network == net,
                                onClick = { network = net },
                                label = { Text(net.displayName, maxLines = 1, softWrap = false) },
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = {
                    nameTouched = true
                    if (it.length <= 40) {
                        name = it
                        if (existingAccount == null) {
                            isNameAutoManaged = it.isBlank()
                        }
                    }
                },
                label = { Text("Account Name (e.g. bKash Personal, City Bank)") },
                singleLine = true,
                isError = nameError != null,
                supportingText = nameError?.let { msg -> { Text(msg) } },
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_account_name"),
            )

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val stackFields = maxWidth < 380.dp
                if (stackFields) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedTextField(
                            value = openingBalanceInput,
                            onValueChange = { openingBalanceInput = InputValidation.sanitizeDecimalInput(it) },
                            label = {
                                Text(
                                    if (accountType == AccountType.CREDIT_CARD) "Opening Spent ($currencySymbol)"
                                    else "Opening Balance ($currencySymbol)",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            singleLine = true,
                            isError = openingError != null,
                            supportingText = openingError?.let { msg -> { Text(msg) } },
                            shape = RoundedCornerShape(16.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_opening_balance"),
                        )
                        OutlinedTextField(
                            value = lastFour,
                            onValueChange = { v -> lastFour = InputValidation.sanitizeDigitsOnly(v, 4) },
                            label = { Text("Last 4 Digits", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            singleLine = true,
                            isError = lastFourError != null,
                            supportingText = lastFourError?.let { msg -> { Text(msg) } },
                            shape = RoundedCornerShape(16.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedTextField(
                            value = openingBalanceInput,
                            onValueChange = { openingBalanceInput = InputValidation.sanitizeDecimalInput(it) },
                            label = {
                                Text(
                                    if (accountType == AccountType.CREDIT_CARD) "Opening Spent ($currencySymbol)"
                                    else "Opening Balance ($currencySymbol)",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            singleLine = true,
                            isError = openingError != null,
                            supportingText = openingError?.let { msg -> { Text(msg) } },
                            shape = RoundedCornerShape(16.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1.3f)
                                .testTag("input_opening_balance"),
                        )
                        OutlinedTextField(
                            value = lastFour,
                            onValueChange = { v -> lastFour = InputValidation.sanitizeDigitsOnly(v, 4) },
                            label = { Text("Last 4 Digits", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            singleLine = true,
                            isError = lastFourError != null,
                            supportingText = lastFourError?.let { msg -> { Text(msg) } },
                            shape = RoundedCornerShape(16.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(0.9f),
                        )
                    }
                }
            }

            if (accountType == AccountType.CREDIT_CARD) {
                OutlinedTextField(
                    value = creditLimitInput,
                    onValueChange = { creditLimitInput = InputValidation.sanitizeDecimalInput(it) },
                    label = { Text("Credit Limit ($currencySymbol)") },
                    singleLine = true,
                    isError = creditLimitError != null,
                    supportingText = creditLimitError?.let { msg -> { Text(msg) } },
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Card Gradient Picker
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Card Gradient",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CardPalettes.forEach { palette ->
                        val selected = styleIndex == palette.id
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(palette.startColor, palette.endColor)))
                                .then(
                                    if (selected) Modifier.border(2.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    else Modifier.border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                                )
                                .clickable { styleIndex = palette.id },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (selected) {
                                Icon(Icons.Rounded.Check, contentDescription = palette.name, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            // Account Icon Picker
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf("wallet", "bank", "card", "mfs", "savings", "star").forEach { key ->
                    FilterChip(
                        selected = iconKey == key,
                        onClick = { iconKey = key },
                        label = {
                            Icon(
                                imageVector = iconForKey(key),
                                contentDescription = key,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Text("Pin Account First", style = MaterialTheme.typography.titleSmall)
                }
                Switch(checked = isPinned, onCheckedChange = { isPinned = it })
            }

            val validCustom = accountType != AccountType.CUSTOM || customTypeName.trim().isNotEmpty()
            val canSave = name.trim().isNotEmpty() &&
                validCustom &&
                openingError == null &&
                lastFourError == null &&
                creditLimitError == null

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f).height(52.dp),
                ) { Text("Cancel", maxLines = 1) }

                Button(
                    onClick = {
                        val opening = Formatters.parseAmountToCents(openingBalanceInput) ?: 0L
                        val limit = Formatters.parseAmountToCents(creditLimitInput) ?: 0L
                        onSave(
                            AccountEntity(
                                id = existingAccount?.id ?: 0L,
                                name = name.trim(),
                                accountType = accountType,
                                customTypeName = customTypeName.trim(),
                                network = network,
                                lastFourDigits = lastFour.filter { it.isDigit() }.takeLast(4).padStart(4, '0'),
                                openingBalanceCents = opening,
                                currentBalanceCents = previewBalance,
                                creditLimitCents = if (accountType == AccountType.CREDIT_CARD) limit else 0L,
                                cardStyleIndex = styleIndex,
                                iconKey = iconKey,
                                isArchived = existingAccount?.isArchived ?: false,
                                isPinned = isPinned,
                                createdAt = existingAccount?.createdAt ?: System.currentTimeMillis(),
                            )
                        )
                    },
                    enabled = canSave,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1.4f)
                        .height(52.dp)
                        .testTag("save_account_button"),
                ) {
                    Text(
                        text = if (existingAccount == null) "Create Account" else "Save Changes",
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}
