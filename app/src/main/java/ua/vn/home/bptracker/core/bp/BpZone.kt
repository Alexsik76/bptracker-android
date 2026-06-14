package ua.vn.home.bptracker.core.bp

import androidx.compose.ui.graphics.Color

enum class BpZone(val label: String, val color: Color) {
    OPTIMAL("Optimal", Color(0xFF2E7D32)),
    NORMAL("Normal", Color(0xFF9E9D24)),
    STAGE1("Stage 1", Color(0xFFEF6C00)),
    STAGE2("Stage 2", Color(0xFFC62828));

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
