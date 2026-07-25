package ua.vn.home.bptracker.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.vn.home.bptracker.R
import ua.vn.home.bptracker.core.bp.BpZone
import ua.vn.home.bptracker.ui.theme.*

@Composable
fun BpBottomNavBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onScanClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val bottomGap = MaterialTheme.spacing.cardPadding // 20dp
    
    // Total container height: 
    // 36dp (button top half) + 72dp (bar/button bottom half) + 20dp (bottom gap) = 128dp
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(108.dp + bottomGap)
            .navigationBarsPadding()
            .padding(horizontal = MaterialTheme.spacing.screenPadding),
        contentAlignment = Alignment.TopCenter // Align everything to top first
    ) {
        // 1. The main navigation bar surface
        // Positioned 36dp from the top of the 128dp container, making it 72dp high
        Surface(
            modifier = Modifier
                .padding(top = 36.dp)
                .fillMaxWidth()
                .height(72.dp),
            color = MaterialTheme.colorScheme.secondary,
            shape = MaterialTheme.shapes.extraLarge,
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
                NavItem(
                    label = stringResource(R.string.tab_dashboard),
                    icon = Icons.Default.GridView,
                    isSelected = selectedTab == 0,
                    onClick = { onTabSelected(0) },
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(72.dp))

                NavItem(
                    label = stringResource(R.string.tab_schedule),
                    icon = Icons.Default.CalendarMonth,
                    isSelected = selectedTab == 1,
                    onClick = { onTabSelected(1) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 2. The Scan FAB
        // Positioned at the very top of the container, 72dp high.
        // It overlaps the Surface by exactly 36dp.
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(ZoneOptimalDark)
                .clickable { onScanClick() }
                .border(4.dp, MaterialTheme.colorScheme.background, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CenterFocusStrong,
                contentDescription = stringResource(R.string.tab_scan),
                tint = DarkOnPrimary,
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
 * A field for numeric input with a colored dot and unit label.
 */
@Composable
fun ValueField(
    label: String,
    secondary: String,
    dotColor: Color,
    value: String,
    unit: String,
    onValueChange: (String) -> Unit,
    isValid: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor))
            Spacer(Modifier.width(8.dp))
            Text(label, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            if (secondary.isNotEmpty()) {
                Spacer(Modifier.width(8.dp))
                Text(secondary, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f))
                .padding(horizontal = MaterialTheme.spacing.cardPadding),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.displaySmall.copy(
                        color = if (value.isNotEmpty() && !isValid) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    singleLine = true
                )
                Text(
                    text = unit,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
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
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.07f)
        ),
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
    val isDark = isSystemInDarkTheme()
    val zoneColor = zone.color(isDark)

    Text(
        modifier = modifier,
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = zoneColor, fontFamily = MaterialTheme.typography.displayLarge.fontFamily)) {
                append(sys.toString())
            }
            withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f))) {
                append("/")
            }
            withStyle(SpanStyle(color = zoneColor, fontFamily = MaterialTheme.typography.displayLarge.fontFamily)) {
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
    
    Surface(
        modifier = modifier,
        color = zone.bgColor(isDark),
        shape = RoundedCornerShape(50) // Pill
    ) {
        Text(
            text = stringResource(zone.labelRes).uppercase(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = zone.color(isDark),
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
    modifier: Modifier = Modifier,
    sub: String? = null,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    BpCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .padding(MaterialTheme.spacing.medium)
                .fillMaxWidth()
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium, // DM Mono 17sp
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
        Box(Modifier.weight(0.25f).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(ZoneOptimalDark))
        Box(Modifier.weight(0.25f).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(ZoneNormalDark))
        Box(Modifier.weight(0.25f).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(ZoneStage1Dark))
        Box(Modifier.weight(0.25f).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(ZoneStage2Dark))
    }
}

@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            options.forEachIndexed { index, option ->
                val isSelected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f) else Color.Transparent)
                        .clickable { onSelect(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun SettingRow(
    label: String,
    value: String? = null,
    icon: ImageVector? = null,
    onClick: () -> Unit = {},
    showChevron: Boolean = true,
    labelColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = labelColor.copy(alpha = 0.7f),
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(16.dp))
        }
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = labelColor,
            modifier = Modifier.weight(1f)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (value != null) {
                Text(
                    value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (showChevron) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun ListGroupCard(
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
        if (title != null) {
            Text(
                title.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = MaterialTheme.spacing.extraSmall)
            )
        }
        BpCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(horizontal = MaterialTheme.spacing.cardPadding)) {
                content()
            }
        }
    }
}
