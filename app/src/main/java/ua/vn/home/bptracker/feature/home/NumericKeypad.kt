package ua.vn.home.bptracker.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.vn.home.bptracker.R
import ua.vn.home.bptracker.ui.theme.*

private val KeyHeight = 60.dp
private val KeySpacing = 8.dp
private val KeyShape = RoundedCornerShape(14.dp)

private val KeyTextStyle = TextStyle(
    fontFamily = DmMono,
    fontWeight = FontWeight.SemiBold,
    fontSize = 26.sp
)

/**
 * In-app numeric keypad for the scan review focused edit mode.
 * Digits, backspace and confirm are reported to the caller; digit count limits are enforced by the caller.
 */
@Composable
internal fun NumericKeypad(
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    fun press(action: () -> Unit) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        action()
    }

    val keyBorder = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    val keyBackground = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
    val keyText = MaterialTheme.colorScheme.onSurface

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(KeySpacing)
    ) {
        listOf("123", "456", "789").forEach { digits ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(KeySpacing)
            ) {
                digits.forEach { digit ->
                    KeypadKey(
                        onClick = { press { onDigit(digit) } },
                        modifier = Modifier.weight(1f),
                        containerColor = keyBackground,
                        borderColor = keyBorder
                    ) {
                        Text(
                            text = digit.toString(),
                            style = KeyTextStyle,
                            color = keyText
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(KeySpacing)
        ) {
            KeypadKey(
                onClick = { press(onBackspace) },
                modifier = Modifier.weight(1f),
                containerColor = keyBackground,
                borderColor = keyBorder
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = stringResource(R.string.scan_review_backspace_cd),
                    modifier = Modifier.size(24.dp),
                    tint = keyText.copy(alpha = 0.70f)
                )
            }

            KeypadKey(
                onClick = { press { onDigit('0') } },
                modifier = Modifier.weight(1f),
                containerColor = keyBackground,
                borderColor = keyBorder
            ) {
                Text(
                    text = "0",
                    style = KeyTextStyle,
                    color = keyText
                )
            }

            KeypadKey(
                onClick = { press(onConfirm) },
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = stringResource(R.string.scan_review_done_cd),
                    modifier = Modifier.size(24.dp),
                    tint = DarkBgPrimary
                )
            }
        }
    }
}

@Composable
private fun KeypadKey(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color,
    borderColor: Color? = null,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .height(KeyHeight)
            .clip(KeyShape)
            .background(containerColor)
            .then(
                if (borderColor != null) Modifier.border(1.dp, borderColor, KeyShape) else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
        content = content
    )
}
