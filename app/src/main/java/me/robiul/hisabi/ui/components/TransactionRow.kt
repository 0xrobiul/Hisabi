package me.robiul.hisabi.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import me.robiul.hisabi.data.local.CategoryEntity
import me.robiul.hisabi.data.local.TransactionEntity
import me.robiul.hisabi.data.local.TransactionType
import me.robiul.hisabi.ui.theme.ChampagneGold
import me.robiul.hisabi.ui.theme.InterFontFamily
import me.robiul.hisabi.ui.theme.SemanticExpense
import me.robiul.hisabi.ui.theme.SemanticIncome
import me.robiul.hisabi.ui.theme.SemanticSavings
import me.robiul.hisabi.ui.theme.SemanticTransfer
import me.robiul.hisabi.ui.theme.SemanticWarning
import me.robiul.hisabi.util.Formatters
import me.robiul.hisabi.util.iconForKey

@Composable
fun TransactionRow(
    transaction: TransactionEntity,
    accountMap: Map<Long, AccountEntity>,
    categoryMap: Map<Long, CategoryEntity>,
    currencySymbol: String,
    datePattern: String,
    hideBalances: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val fromAcc = accountMap[transaction.accountId]
    val toAcc = transaction.toAccountId?.let { accountMap[it] }
    val cat = transaction.categoryId?.let { categoryMap[it] }

    val badgeColor = when (transaction.type) {
        TransactionType.TRANSFER -> SemanticTransfer
        TransactionType.SAVINGS_CONTRIBUTION -> SemanticSavings
        TransactionType.ADJUSTMENT -> SemanticWarning
        else -> cat?.let { Color(it.colorHex) }
            ?: if (transaction.type == TransactionType.INCOME) SemanticIncome else SemanticExpense
    }

    val iconKey = when (transaction.type) {
        TransactionType.TRANSFER -> "transfer"
        TransactionType.SAVINGS_CONTRIBUTION -> "savings"
        TransactionType.ADJUSTMENT -> "adjust"
        else -> cat?.iconKey ?: "others"
    }

    val amountColor = when (transaction.type) {
        TransactionType.EXPENSE -> SemanticExpense
        TransactionType.INCOME -> SemanticIncome
        TransactionType.TRANSFER -> SemanticTransfer
        TransactionType.SAVINGS_CONTRIBUTION -> SemanticSavings
        TransactionType.ADJUSTMENT -> if (transaction.amountCents >= 0) SemanticIncome else SemanticExpense
    }

    val amountText = when (transaction.type) {
        TransactionType.EXPENSE -> Formatters.formatMoney(-(transaction.amountCents + transaction.feeCents), currencySymbol, hideBalances)
        TransactionType.INCOME -> Formatters.formatMoney(transaction.amountCents, currencySymbol, hideBalances, showPlus = true)
        TransactionType.TRANSFER -> "⇄ ${Formatters.formatMoney(transaction.amountCents, currencySymbol, hideBalances)}"
        TransactionType.SAVINGS_CONTRIBUTION -> Formatters.formatMoney(transaction.amountCents, currencySymbol, hideBalances)
        TransactionType.ADJUSTMENT -> Formatters.formatMoney(transaction.amountCents, currencySymbol, hideBalances, showPlus = transaction.amountCents > 0)
    }

    val subtitleText = buildString {
        val catLabel = cat?.name ?: transaction.categoryNameSnapshot.ifBlank { transaction.type.label }
        append(catLabel)
        if (transaction.subcategoryName.isNotBlank()) {
            append(" / ${transaction.subcategoryName}")
        }
        append(" • ")
        if (transaction.type == TransactionType.TRANSFER && toAcc != null) {
            append("${fromAcc?.name ?: "Account"} → ${toAcc.name}")
        } else {
            append(fromAcc?.name ?: "Account")
        }
    }

    val shape = RoundedCornerShape(18.dp)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .testTag("transaction_row_${transaction.id}"),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(badgeColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = iconForKey(iconKey),
                    contentDescription = transaction.title,
                    tint = badgeColor,
                    modifier = Modifier.size(20.dp),
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = transaction.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (transaction.receiptPath != null) {
                        Icon(
                            imageVector = Icons.Rounded.AttachFile,
                            contentDescription = "Has receipt",
                            tint = ChampagneGold,
                            modifier = Modifier.size(13.dp),
                        )
                    }
                    if (transaction.recurringRuleId != null) {
                        Icon(
                            imageVector = Icons.Rounded.Repeat,
                            contentDescription = "Recurring",
                            tint = ChampagneGold,
                            modifier = Modifier.size(13.dp),
                        )
                    }
                }
                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = Formatters.formatDateTime(transaction.timestamp, datePattern),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = amountText,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = amountColor,
                )
                if (transaction.feeCents > 0L) {
                    Text(
                        text = "Fee ${Formatters.formatMoney(transaction.feeCents, currencySymbol, hideBalances)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = SemanticWarning,
                    )
                }
            }
        }
    }
}
