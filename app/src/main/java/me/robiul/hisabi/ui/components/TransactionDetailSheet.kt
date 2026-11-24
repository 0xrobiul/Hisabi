package me.robiul.hisabi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import me.robiul.hisabi.data.local.AccountEntity
import me.robiul.hisabi.data.local.CategoryEntity
import me.robiul.hisabi.data.local.TransactionEntity
import me.robiul.hisabi.data.local.TransactionType
import me.robiul.hisabi.ui.theme.InterFontFamily
import me.robiul.hisabi.ui.theme.SemanticExpense
import me.robiul.hisabi.ui.theme.SemanticIncome
import me.robiul.hisabi.ui.theme.SemanticSavings
import me.robiul.hisabi.ui.theme.SemanticTransfer
import me.robiul.hisabi.ui.theme.SemanticWarning
import me.robiul.hisabi.util.Formatters
import me.robiul.hisabi.util.iconForKey
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionDetailSheet(
    transaction: TransactionEntity,
    accountMap: Map<Long, AccountEntity>,
    categoryMap: Map<Long, CategoryEntity>,
    currencySymbol: String,
    datePattern: String,
    hideBalances: Boolean,
    onDismiss: () -> Unit,
    onEdit: (TransactionEntity) -> Unit,
    onDuplicate: (TransactionEntity) -> Unit,
    onDelete: (Long) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val fromAccount = accountMap[transaction.accountId]
    val toAccount = transaction.toAccountId?.let { accountMap[it] }
    val category = transaction.categoryId?.let { categoryMap[it] }

    val typeColor = when (transaction.type) {
        TransactionType.EXPENSE -> SemanticExpense
        TransactionType.INCOME -> SemanticIncome
        TransactionType.TRANSFER -> SemanticTransfer
        TransactionType.SAVINGS_CONTRIBUTION -> SemanticSavings
        TransactionType.ADJUSTMENT -> SemanticWarning
    }

    val iconKey = when (transaction.type) {
        TransactionType.TRANSFER -> "transfer"
        TransactionType.SAVINGS_CONTRIBUTION -> "savings"
        TransactionType.ADJUSTMENT -> "adjust"
        else -> category?.iconKey ?: "others"
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Delete Transaction?") },
            text = {
                Text("Removing \"${transaction.title}\" will automatically restore the affected account balance.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(transaction.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
        )
    }

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
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(typeColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = iconForKey(iconKey),
                        contentDescription = transaction.title,
                        tint = typeColor,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transaction.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "${transaction.type.label} • ${Formatters.formatDateTime(transaction.timestamp, datePattern)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
                    .padding(16.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "AMOUNT",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val displayAmt = when (transaction.type) {
                        TransactionType.EXPENSE -> Formatters.formatMoney(-(transaction.amountCents + transaction.feeCents), currencySymbol, hideBalances)
                        TransactionType.INCOME -> Formatters.formatMoney(transaction.amountCents, currencySymbol, hideBalances, showPlus = true)
                        TransactionType.ADJUSTMENT -> Formatters.formatMoney(transaction.amountCents, currencySymbol, hideBalances, showPlus = transaction.amountCents > 0)
                        else -> Formatters.formatMoney(transaction.amountCents, currencySymbol, hideBalances)
                    }
                    Text(
                        text = displayAmt,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 26.sp,
                        color = typeColor,
                    )
                    if (transaction.feeCents > 0L) {
                        Text(
                            text = "Includes fee: ${Formatters.formatMoney(transaction.feeCents, currencySymbol, hideBalances)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SemanticWarning,
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                DetailLine(
                    label = "Category",
                    value = buildString {
                        append(category?.name ?: transaction.categoryNameSnapshot.ifBlank { transaction.type.label })
                        if (transaction.subcategoryName.isNotBlank()) append(" / ${transaction.subcategoryName}")
                    },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                DetailLine(
                    label = if (transaction.type == TransactionType.TRANSFER) "From Account" else "Account",
                    value = fromAccount?.name ?: "Account",
                )
                if (transaction.type == TransactionType.TRANSFER && toAccount != null) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    DetailLine(label = "To Account", value = toAccount.name)
                }
                if (transaction.type == TransactionType.TRANSFER && transaction.feeCents > 0L) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    DetailLine(
                        label = "Transfer Charge",
                        value = Formatters.formatMoney(transaction.feeCents, currencySymbol, hideBalances),
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    DetailLine(
                        label = "Total Deducted",
                        value = Formatters.formatMoney(transaction.amountCents + transaction.feeCents, currencySymbol, hideBalances),
                    )
                }
                if (transaction.description.isNotBlank()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    DetailLine(label = "Description", value = transaction.description)
                }
                if (transaction.notes.isNotBlank()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    DetailLine(label = "Notes", value = transaction.notes)
                }
                if (transaction.recurringRuleId != null) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(Icons.Rounded.Repeat, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Text(
                            text = "Generated from recurring schedule",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            val tags = transaction.tags()
            if (tags.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    tags.forEach { tag ->
                        AssistChip(
                            onClick = {},
                            label = { Text("#$tag") },
                        )
                    }
                }
            }

            transaction.receiptPath?.let { path ->
                val file = File(path)
                if (file.exists()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Receipt Attachment",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        AsyncImage(
                            model = file,
                            contentDescription = "Transaction receipt",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(16.dp)),
                        )
                    }
                }
            }

            val isEditable = transaction.type == TransactionType.EXPENSE ||
                transaction.type == TransactionType.INCOME ||
                transaction.type == TransactionType.TRANSFER

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (isEditable) {
                    Button(
                        onClick = { onEdit(transaction) },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("detail_edit_button"),
                    ) {
                        Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Edit")
                    }
                    OutlinedButton(
                        onClick = { onDuplicate(transaction) },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("detail_duplicate_button"),
                    ) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Duplicate")
                    }
                }
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .then(if (isEditable) Modifier.height(48.dp) else Modifier.fillMaxWidth().height(48.dp))
                        .testTag("detail_delete_button"),
                ) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete transaction", modifier = Modifier.size(18.dp))
                    if (!isEditable) {
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Delete Record")
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 16.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
        )
    }
}
