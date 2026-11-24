package me.robiul.hisabi.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.robiul.hisabi.ui.theme.ChampagneGold
import me.robiul.hisabi.ui.theme.DarkBgPrimary
import me.robiul.hisabi.ui.theme.DarkBgSecondary
import me.robiul.hisabi.ui.theme.DarkBorder
import me.robiul.hisabi.ui.theme.InterFontFamily
import me.robiul.hisabi.ui.theme.SemanticExpense
import me.robiul.hisabi.util.PinSecurity
import kotlinx.coroutines.delay

@Composable
fun LockScreen(
    storedPinHash: String,
    biometricEnabled: Boolean,
    externalStatusMessage: String? = null,
    onClearExternalStatus: () -> Unit = {},
    onUnlocked: () -> Unit,
    onTriggerBiometric: () -> Unit,
    onExitApp: () -> Unit = {},
) {
    var enteredPin by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }
    var failedAttempts by remember { mutableIntStateOf(0) }
    var cooldownSeconds by remember { mutableIntStateOf(0) }

    BackHandler(enabled = true) {
        onExitApp()
    }

    LaunchedEffect(cooldownSeconds) {
        if (cooldownSeconds > 0) {
            delay(1000L)
            cooldownSeconds--
            if (cooldownSeconds == 0) {
                errorText = null
            }
        }
    }

    val activeMessage = when {
        cooldownSeconds > 0 -> "Too many incorrect attempts. Try again in ${cooldownSeconds}s."
        errorText != null -> errorText
        else -> externalStatusMessage
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBgPrimary)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 360.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(ChampagneGold.copy(alpha = 0.14f))
                    .border(1.dp, ChampagneGold.copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = "Vault Locked",
                    tint = ChampagneGold,
                    modifier = Modifier.size(28.dp),
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "Hisabi Vault",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = if (biometricEnabled) {
                        "Verify biometrics or enter your 4-digit PIN"
                    } else {
                        "Enter your 4-digit PIN to access your balances"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                repeat(4) { idx ->
                    val filled = idx < enteredPin.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(if (filled) ChampagneGold else DarkBgSecondary)
                            .border(
                                width = 1.dp,
                                color = if (errorText != null || cooldownSeconds > 0) {
                                    SemanticExpense
                                } else if (filled) {
                                    ChampagneGold
                                } else {
                                    DarkBorder
                                },
                                shape = CircleShape,
                            ),
                    )
                }
            }

            if (activeMessage != null) {
                Text(
                    text = activeMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (errorText != null || cooldownSeconds > 0) {
                        SemanticExpense
                    } else {
                        ChampagneGold
                    },
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("BIO", "0", "DEL"),
            )

            val padEnabled = cooldownSeconds == 0

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                rows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        row.forEach { key ->
                            when (key) {
                                "BIO" -> {
                                    if (biometricEnabled) {
                                        PinKeyButton(
                                            onClick = onTriggerBiometric,
                                            enabled = true,
                                            modifier = Modifier.testTag("pin_biometric_button"),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Fingerprint,
                                                contentDescription = "Unlock with Biometrics",
                                                tint = ChampagneGold,
                                                modifier = Modifier.size(28.dp),
                                            )
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.size(68.dp))
                                    }
                                }
                                "DEL" -> {
                                    PinKeyButton(
                                        onClick = {
                                            errorText = null
                                            onClearExternalStatus()
                                            if (enteredPin.isNotEmpty()) {
                                                enteredPin = enteredPin.dropLast(1)
                                            }
                                        },
                                        enabled = padEnabled,
                                        modifier = Modifier.testTag("pin_backspace_button"),
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.Backspace,
                                            contentDescription = "Backspace",
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(24.dp),
                                        )
                                    }
                                }
                                else -> {
                                    PinKeyButton(
                                        onClick = {
                                            if (padEnabled && enteredPin.length < 4) {
                                                errorText = null
                                                onClearExternalStatus()
                                                val next = enteredPin + key
                                                enteredPin = next
                                                if (next.length == 4) {
                                                    if (PinSecurity.verifyPin(next, storedPinHash)) {
                                                        failedAttempts = 0
                                                        onUnlocked()
                                                    } else {
                                                        failedAttempts++
                                                        enteredPin = ""
                                                        if (failedAttempts >= 5) {
                                                            failedAttempts = 0
                                                            cooldownSeconds = 15
                                                        } else {
                                                            errorText = "Incorrect PIN. Try again."
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                        enabled = padEnabled,
                                        modifier = Modifier.testTag("pin_key_$key"),
                                    ) {
                                        Text(
                                            text = key,
                                            fontFamily = InterFontFamily,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 24.sp,
                                            color = if (padEnabled) {
                                                MaterialTheme.colorScheme.onSurface
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PinKeyButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier
            .size(68.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        shape = CircleShape,
        color = DarkBgSecondary,
        border = BorderStroke(1.dp, DarkBorder),
    ) {
        Box(contentAlignment = Alignment.Center) {
            content()
        }
    }
}
