package ua.vn.home.bptracker.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.vn.home.bptracker.R
import ua.vn.home.bptracker.core.bp.BpZone

@Composable
fun BpBottomNavBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onScanClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    
    // Using a Box to allow the FAB to overlap the bottom bar
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp), // Island effect: floating above bottom
        contentAlignment = Alignment.BottomCenter
    ) {
        // The "Island" Bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            color = MaterialTheme.colorScheme.secondary, // #11151C in Dark
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(
                1.dp, 
                if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.07f)
            ),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dashboard Tab
                NavItem(
                    label = stringResource(R.string.tab_dashboard),
                    icon = Icons.Default.GridView,
                    isSelected = selectedTab == 0,
                    onClick = { onTabSelected(0) },
                    modifier = Modifier.weight(1f)
                )

                // Placeholder for the overlapping FAB
                Spacer(modifier = Modifier.width(72.dp))

                // Schedule Tab
                NavItem(
                    label = stringResource(R.string.tab_schedule),
                    icon = Icons.Default.CalendarMonth,
                    isSelected = selectedTab == 1,
                    onClick = { onTabSelected(1) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Protruding Scan Button (FAB)
        Box(
            modifier = Modifier
                .offset(y = (-36).dp) // Move up to overlap
                .size(72.dp)
                .clip(CircleShape)
                .background(Color(0xFF22C55E))
                .clickable { onScanClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CenterFocusStrong, // Icon with the dash in the center
                contentDescription = stringResource(R.string.tab_scan),
                tint = Color.Black,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun NavItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontSize = 11.sp
        )
    }
}

/**
 * Base container for all cards in the app.
 */
@Composable
fun BpCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = if (isDark) BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)) else BorderStroke(1.dp, Color.Black.copy(alpha = 0.07f)),
        elevation = if (isDark) CardDefaults.cardElevation(0.dp) else CardDefaults.cardElevation(2.dp),
        content = content
    )
}

/**
 * Displays BP values (SYS/DIA) using DM Mono font and zone-aware coloring.
 */
@Composable
fun ReadingValue(
    sys: Int,
    dia: Int,
    zone: BpZone,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 56.sp
) {
    Text(
        modifier = modifier,
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = zone.color, fontFamily = MaterialTheme.typography.displayLarge.fontFamily)) {
                append(sys.toString())
            }
            withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f))) {
                append("/")
            }
            withStyle(SpanStyle(color = zone.color, fontFamily = MaterialTheme.typography.displayLarge.fontFamily)) {
                append(dia.toString())
            }
        },
        fontSize = fontSize,
        fontWeight = FontWeight.Medium
    )
}

/**
 * A pill-shaped badge showing the BP zone name with appropriate colors.
 */
@Composable
fun ZoneBadge(zone: BpZone, modifier: Modifier = Modifier) {
    val isDark = isSystemInDarkTheme()
    val (bgColor, fgColor) = if (isDark) {
        zone.color.copy(alpha = 0.12f) to zone.color
    } else {
        when(zone) {
            BpZone.OPTIMAL -> Color(0xFFDCFCE7) to Color(0xFF16A34A)
            BpZone.NORMAL -> Color(0xFFECFCCB) to Color(0xFF65A30D)
            BpZone.STAGE1 -> Color(0xFFFED7AA) to Color(0xFFC2410C)
            BpZone.STAGE2 -> Color(0xFFFEE2E2) to Color(0xFFDC2626)
        }
    }

    Surface(
        modifier = modifier,
        color = bgColor,
        shape = RoundedCornerShape(50) // Pill
    ) {
        Text(
            text = stringResource(zone.labelRes).uppercase(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = fgColor,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

/**
 * A small tile for KPI metrics (2-column grid item).
 */
@Composable
fun KpiTile(
    label: String,
    value: String,
    sub: String? = null,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    BpCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium, // DM Mono 25sp
                color = valueColor
            )
            if (sub != null) {
                Text(
                    text = sub,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Horizontal scale bar optimal -> stage2 with a marker.
 */
@Composable
fun PositionScaleBar(
    sys: Int,
    dia: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Box(Modifier.weight(0.25f).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(Color(0xFF22C55E)))
        Box(Modifier.weight(0.25f).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(Color(0xFF84CC16)))
        Box(Modifier.weight(0.25f).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(Color(0xFFF97316)))
        Box(Modifier.weight(0.25f).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(Color(0xFFEF4444)))
    }
}
