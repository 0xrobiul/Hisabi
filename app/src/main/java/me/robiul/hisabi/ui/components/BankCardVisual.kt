package me.robiul.hisabi.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Contactless
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.robiul.hisabi.data.local.AccountEntity
import me.robiul.hisabi.data.local.AccountType
import me.robiul.hisabi.data.local.CardNetwork
import me.robiul.hisabi.ui.theme.CardPalettes
import me.robiul.hisabi.ui.theme.ChampagneGold
import me.robiul.hisabi.ui.theme.ChampagneGoldSoft
import me.robiul.hisabi.ui.theme.InterFontFamily
import me.robiul.hisabi.ui.theme.JetBrainsMonoFontFamily
import me.robiul.hisabi.ui.theme.SemanticExpense
import me.robiul.hisabi.util.Formatters
import me.robiul.hisabi.util.iconForKey

@Composable
fun BankCardVisual(
    account: AccountEntity,
    currencySymbol: String,
    hideBalances: Boolean,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val palette = CardPalettes.getOrElse(account.cardStyleIndex) { CardPalettes[0] }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.975f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 420f),
        label = "card_press_scale",
    )

    val cardShape = RoundedCornerShape(24.dp)
    val typeDisplay = if (account.accountType == AccountType.CUSTOM && account.customTypeName.isNotBlank()) {
        account.customTypeName.uppercase()
    } else {
        account.accountType.shortLabel.uppercase()
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isCompact) Modifier.height(186.dp)
                else Modifier.aspectRatio(1.586f)
            )
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (account.isArchived) 0.6f else 1f
            }
            .clip(cardShape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = androidx.compose.material3.ripple(color = ChampagneGold),
                        onClick = onClick,
                    )
                } else Modifier
            )
            .testTag("bank_card_${account.id}"),
        shape = cardShape,
        shadowElevation = 6.dp,
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    ChampagneGold.copy(alpha = 0.55f),
                    Color(0xFF242424),
                    palette.accentColor.copy(alpha = 0.32f),
                )
            )
        ),
        color = Color.Transparent,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Deep matte-black obsidian finish
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(palette.startColor, palette.midColor, palette.endColor),
                        start = Offset.Zero,
                        end = Offset(size.width, size.height),
                    )
                )

                // Subtle diagonal brushed metallic sheen
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.035f),
                            ChampagneGold.copy(alpha = 0.04f),
                            Color.Transparent,
                        ),
                        start = Offset(size.width * 0.1f, 0f),
                        end = Offset(size.width * 0.85f, size.height),
                    )
                )

                // Warm champagne radial lighting at top-right corner
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(palette.accentColor.copy(alpha = 0.14f), Color.Transparent),
                        center = Offset(size.width * 0.88f, size.height * 0.14f),
                        radius = size.width * 0.58f,
                    ),
                    center = Offset(size.width * 0.88f, size.height * 0.14f),
                    radius = size.width * 0.58f,
                )

                // Subtle architectural concentric arcs
                val ringCenter = Offset(size.width * 0.08f, size.height * 0.98f)
                for (i in 1..4) {
                    drawCircle(
                        color = ChampagneGold.copy(alpha = 0.045f),
                        radius = size.width * (0.20f * i),
                        center = ringCenter,
                        style = Stroke(width = 0.8.dp.toPx()),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // Top Row: Hisabi luxury wordmark + Account name + type pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(ChampagneGold.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = iconForKey(account.iconKey),
                                contentDescription = account.name,
                                tint = palette.accentColor,
                                modifier = Modifier.size(17.dp),
                            )
                        }
                        Column {
                            Text(
                                text = account.name,
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = palette.onCardColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = if (account.isArchived) "$typeDisplay • ARCHIVED" else "HISABI • $typeDisplay",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 10.sp,
                                letterSpacing = 1.2.sp,
                                color = ChampagneGold.copy(alpha = 0.82f),
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (account.isPinned) {
                            Icon(
                                imageVector = Icons.Rounded.Star,
                                contentDescription = "Pinned",
                                tint = ChampagneGold,
                                modifier = Modifier.size(15.dp),
                            )
                        }
                        Text(
                            text = "HISABI",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 2.0.sp,
                            color = ChampagneGoldSoft.copy(alpha = 0.9f),
                        )
                    }
                }

                // Middle Row: Champagne Gold EMV Chip + Contactless + Available Balance
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = if (account.accountType == AccountType.CREDIT_CARD) "CURRENT SPENT" else "AVAILABLE BALANCE",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.sp,
                            letterSpacing = 1.1.sp,
                            color = Color(0xFFA1A7B2),
                        )
                        Text(
                            text = Formatters.formatMoney(
                                cents = account.currentBalanceCents,
                                currencySymbol = currencySymbol,
                                masked = hideBalances,
                            ),
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = if (isCompact) 22.sp else 25.sp,
                            letterSpacing = (-0.4).sp,
                            color = palette.onCardColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ChampagneGoldChipGraphic()
                        Icon(
                            imageVector = Icons.Rounded.Contactless,
                            contentDescription = "Contactless",
                            tint = ChampagneGold.copy(alpha = 0.72f),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                // Credit Limit Progress (when Credit Card with limit set)
                if (account.accountType == AccountType.CREDIT_CARD && account.creditLimitCents > 0L && !isCompact) {
                    val usedRatio = ((account.currentBalanceCents * 100L) / account.creditLimitCents)
                        .toInt().coerceIn(0, 100) / 100f
                    val availableCents = (account.creditLimitCents - account.currentBalanceCents).coerceAtLeast(0L)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "Avail ${Formatters.formatMoney(availableCents, currencySymbol, hideBalances)}",
                                fontFamily = InterFontFamily,
                                fontSize = 10.sp,
                                color = Color(0xFFA1A7B2),
                            )
                            Text(
                                text = "Limit ${Formatters.formatCompactMoney(account.creditLimitCents, currencySymbol)}",
                                fontFamily = InterFontFamily,
                                fontSize = 10.sp,
                                color = Color(0xFFA1A7B2),
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color.White.copy(alpha = 0.12f)),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(usedRatio)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (usedRatio > 0.8f) SemanticExpense else ChampagneGold),
                            )
                        }
                    }
                }

                // Bottom Row: Masked Account Number + Network Emblem
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    val maskText = when (account.accountType) {
                        AccountType.CASH -> "PHYSICAL CASH VAULT"
                        AccountType.BKASH, AccountType.NAGAD, AccountType.ROCKET, AccountType.UPAY ->
                            "${account.accountType.shortLabel.uppercase()}  ••••  ${account.lastFourDigits}"
                        else -> "••••   ••••   ••••   ${account.lastFourDigits}"
                    }
                    Text(
                        text = maskText,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        letterSpacing = 1.2.sp,
                        color = palette.onCardColor.copy(alpha = 0.86f),
                    )
                    NetworkBadge(network = account.network)
                }
            }
        }
    }
}

@Composable
private fun ChampagneGoldChipGraphic() {
    Canvas(modifier = Modifier.size(width = 36.dp, height = 26.dp)) {
        val w = size.width
        val h = size.height
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFF3E2B3), ChampagneGold, Color(0xFF9F7E39), Color(0xFFE8CE8A))
            ),
            cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx()),
        )
        val strokeColor = Color(0xFF3E2D0B).copy(alpha = 0.48f)
        val sw = 0.9.dp.toPx()
        drawLine(strokeColor, Offset(w * 0.34f, 0f), Offset(w * 0.34f, h), sw)
        drawLine(strokeColor, Offset(w * 0.66f, 0f), Offset(w * 0.66f, h), sw)
        drawLine(strokeColor, Offset(0f, h * 0.5f), Offset(w * 0.34f, h * 0.5f), sw)
        drawLine(strokeColor, Offset(w * 0.66f, h * 0.5f), Offset(w, h * 0.5f), sw)
        drawRoundRect(
            color = strokeColor,
            topLeft = Offset(w * 0.34f, h * 0.24f),
            size = Size(w * 0.32f, h * 0.52f),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
            style = Stroke(width = sw),
        )
    }
}

@Composable
private fun NetworkBadge(network: CardNetwork) {
    if (network == CardNetwork.MASTERCARD) {
        Box(modifier = Modifier.width(34.dp).height(20.dp)) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(ChampagneGold.copy(alpha = 0.92f))
            )
            Box(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE5A93C).copy(alpha = 0.72f))
            )
        }
    } else {
        Text(
            text = network.displayName,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 1.4.sp,
            color = ChampagneGoldSoft.copy(alpha = 0.92f),
        )
    }
}
