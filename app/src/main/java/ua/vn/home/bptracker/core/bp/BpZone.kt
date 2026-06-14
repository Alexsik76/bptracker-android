package ua.vn.home.bptracker.core.bp

import androidx.compose.ui.graphics.Color
import ua.vn.home.bptracker.R
import ua.vn.home.bptracker.ui.theme.*

enum class BpZone(val labelRes: Int) {
    OPTIMAL(R.string.bp_zone_optimal),
    NORMAL(R.string.bp_zone_normal),
    STAGE1(R.string.bp_zone_stage1),
    STAGE2(R.string.bp_zone_stage2);

    /**
     * Gets the appropriate foreground color for the current theme.
     */
    fun color(isDark: Boolean): Color = if (isDark) {
        when(this) {
            OPTIMAL -> ZoneOptimalDark
            NORMAL -> ZoneNormalDark
            STAGE1 -> ZoneStage1Dark
            STAGE2 -> ZoneStage2Dark
        }
    } else {
        when(this) {
            OPTIMAL -> ZoneOptimalLight
            NORMAL -> ZoneNormalLight
            STAGE1 -> ZoneStage1Light
            STAGE2 -> ZoneStage2Light
        }
    }

    /**
     * Gets the appropriate background chip color for the current theme.
     */
    fun bgColor(isDark: Boolean): Color = if (isDark) {
        color(true).copy(alpha = 0.12f)
    } else {
        when(this) {
            OPTIMAL -> ZoneOptimalBgLight
            NORMAL -> ZoneNormalBgLight
            STAGE1 -> ZoneStage1BgLight
            STAGE2 -> ZoneStage2BgLight
        }
    }

    companion object {
        fun classify(sys: Int, dia: Int): BpZone {
            // "Worst category wins" rule
            val s = when {
                sys >= 160 -> STAGE2
                sys >= 140 -> STAGE1
                sys >= 120 -> NORMAL
                else -> OPTIMAL
            }
            val d = when {
                dia >= 100 -> STAGE2
                dia >= 90 -> STAGE1
                dia >= 80 -> NORMAL
                else -> OPTIMAL
            }
            return if (s.ordinal >= d.ordinal) s else d
        }
    }
}
