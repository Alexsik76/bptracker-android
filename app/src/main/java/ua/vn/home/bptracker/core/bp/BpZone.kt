package ua.vn.home.bptracker.core.bp

import androidx.compose.ui.graphics.Color

import ua.vn.home.bptracker.R

enum class BpZone(val labelRes: Int, val color: Color) {
    OPTIMAL(R.string.bp_zone_optimal, Color(0xFF2E7D32)),
    NORMAL(R.string.bp_zone_normal, Color(0xFF9E9D24)),
    STAGE1(R.string.bp_zone_stage1, Color(0xFFEF6C00)),
    STAGE2(R.string.bp_zone_stage2, Color(0xFFC62828));

    companion object {
        fun classify(sys: Int, dia: Int): BpZone {
            return when {
                sys >= 160 || dia >= 100 -> STAGE2
                sys >= 140 || dia >= 90 -> STAGE1
                sys >= 120 || dia >= 80 -> NORMAL
                else -> OPTIMAL
            }
        }
    }
}
