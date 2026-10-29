package ua.vn.home.bptracker.feature.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveOcrStabilizerTest {

    @Test
    fun `consensus with one outlier frame`() {
        val stabilizer = LiveOcrStabilizer(windowSize = 5, majority = 3, resetCount = 3)

        // Frame 1: 120, 80, 70
        val r1 = stabilizer.submitReading(120, 80, 70, 0.9f, 0.9f)
        assertEquals(LiveOcrVerdict.Accepted, r1.verdict)
        assertFalse(r1.isConsensus)
        assertEquals(1, r1.minVotes)

        // Frame 2: outlier frame 125, 80, 70
        val r2 = stabilizer.submitReading(125, 80, 70, 0.9f, 0.9f)
        assertEquals(LiveOcrVerdict.Accepted, r2.verdict)
        assertFalse(r2.isConsensus)
        assertEquals(1, r2.minVotes) // sys has 120(1), 125(1) -> minOfMax is 1

        // Frame 3: 120, 80, 70
        val r3 = stabilizer.submitReading(120, 80, 70, 0.9f, 0.9f)
        assertEquals(LiveOcrVerdict.Accepted, r3.verdict)
        assertFalse(r3.isConsensus)
        assertEquals(2, r3.minVotes) // sys 120 has 2, dia 80 has 3, pul 70 has 3 -> min is 2

        // Frame 4: 120, 80, 70
        val r4 = stabilizer.submitReading(120, 80, 70, 0.9f, 0.9f)
        assertEquals(LiveOcrVerdict.Accepted, r4.verdict)
        assertTrue(r4.isConsensus)
        assertEquals(3, r4.minVotes)
        assertEquals(Consensus(sys = 120, dia = 80, pul = 70), r4.consensus)
    }

    @Test
    fun `per-field majority with different outliers per field`() {
        val stabilizer = LiveOcrStabilizer(windowSize = 5, majority = 3, resetCount = 3)

        // Frame 1: target reading
        stabilizer.submitReading(120, 80, 70)
        // Frame 2: sys outlier
        stabilizer.submitReading(130, 80, 70)
        // Frame 3: dia outlier
        stabilizer.submitReading(120, 90, 70)
        // Frame 4: pul outlier
        stabilizer.submitReading(120, 80, 60)
        // Frame 5: target reading
        val r5 = stabilizer.submitReading(120, 80, 70)

        // Window size is 5:
        // SYS: 120 appears 4 times, 130 appears 1 time -> majority 120
        // DIA: 80 appears 4 times, 90 appears 1 time -> majority 80
        // PUL: 70 appears 4 times, 60 appears 1 time -> majority 70
        assertEquals(5, r5.window.size)
        assertTrue(r5.isConsensus)
        assertEquals(Consensus(sys = 120, dia = 80, pul = 70), r5.consensus)
        assertEquals(4, r5.minVotes) // max votes are 4, 4, 4
    }

    @Test
    fun `validation rejects ranges and sys less than or equal to dia`() {
        val stabilizer = LiveOcrStabilizer()

        // sys < 40
        val rSysLow = stabilizer.submitReading(39, 30, 70)
        assertTrue(rSysLow.verdict is LiveOcrVerdict.Rejected)
        assertTrue((rSysLow.verdict as LiveOcrVerdict.Rejected).reason.contains("SYS"))
        assertEquals(0, rSysLow.window.size)

        // sys > 300
        val rSysHigh = stabilizer.submitReading(301, 80, 70)
        assertTrue(rSysHigh.verdict is LiveOcrVerdict.Rejected)
        assertTrue((rSysHigh.verdict as LiveOcrVerdict.Rejected).reason.contains("SYS"))
        assertEquals(0, rSysHigh.window.size)

        // dia < 20
        val rDiaLow = stabilizer.submitReading(120, 19, 70)
        assertTrue(rDiaLow.verdict is LiveOcrVerdict.Rejected)
        assertTrue((rDiaLow.verdict as LiveOcrVerdict.Rejected).reason.contains("DIA"))
        assertEquals(0, rDiaLow.window.size)

        // dia > 200
        val rDiaHigh = stabilizer.submitReading(220, 201, 70)
        assertTrue(rDiaHigh.verdict is LiveOcrVerdict.Rejected)
        assertTrue((rDiaHigh.verdict as LiveOcrVerdict.Rejected).reason.contains("DIA"))
        assertEquals(0, rDiaHigh.window.size)

        // pul < 30
        val rPulLow = stabilizer.submitReading(120, 80, 29)
        assertTrue(rPulLow.verdict is LiveOcrVerdict.Rejected)
        assertTrue((rPulLow.verdict as LiveOcrVerdict.Rejected).reason.contains("PUL"))
        assertEquals(0, rPulLow.window.size)

        // pul > 250
        val rPulHigh = stabilizer.submitReading(120, 80, 251)
        assertTrue(rPulHigh.verdict is LiveOcrVerdict.Rejected)
        assertTrue((rPulHigh.verdict as LiveOcrVerdict.Rejected).reason.contains("PUL"))
        assertEquals(0, rPulHigh.window.size)

        // sys == dia
        val rSysEqDia = stabilizer.submitReading(100, 100, 70)
        assertTrue(rSysEqDia.verdict is LiveOcrVerdict.Rejected)
        assertTrue((rSysEqDia.verdict as LiveOcrVerdict.Rejected).reason.contains("greater than"))
        assertEquals(0, rSysEqDia.window.size)

        // sys < dia
        val rSysLtDia = stabilizer.submitReading(90, 100, 70)
        assertTrue(rSysLtDia.verdict is LiveOcrVerdict.Rejected)
        assertTrue((rSysLtDia.verdict as LiveOcrVerdict.Rejected).reason.contains("greater than"))
        assertEquals(0, rSysLtDia.window.size)

        // Valid boundaries: 40, 20, 30 with sys > dia (e.g. 40, 20, 30)
        val rValidBoundary = stabilizer.submitReading(40, 20, 30)
        assertEquals(LiveOcrVerdict.Accepted, rValidBoundary.verdict)
        assertEquals(1, rValidBoundary.window.size)
    }

    @Test
    fun `reset after 3 empty frames`() {
        val stabilizer = LiveOcrStabilizer(windowSize = 5, majority = 3, resetCount = 3)

        // Add 2 valid readings
        stabilizer.submitReading(120, 80, 70)
        val r2 = stabilizer.submitReading(120, 80, 70)
        assertEquals(2, r2.window.size)

        // Empty frame 1
        val e1 = stabilizer.submitEmpty("display-not-found")
        assertTrue(e1.verdict is LiveOcrVerdict.Empty)
        assertEquals(2, e1.window.size)

        // Empty frame 2
        val e2 = stabilizer.submitEmpty("digits-not-found")
        assertTrue(e2.verdict is LiveOcrVerdict.Empty)
        assertEquals(2, e2.window.size)

        // Empty frame 3 -> triggers reset
        val e3 = stabilizer.submitEmpty("display-not-found")
        assertEquals(LiveOcrVerdict.Reset, e3.verdict)
        assertEquals(0, e3.window.size)
        assertNull(e3.consensus)
        assertEquals(0, e3.minVotes)
    }

    @Test
    fun `no reset on rejected frames`() {
        val stabilizer = LiveOcrStabilizer(windowSize = 5, majority = 3, resetCount = 3)

        // Add 2 valid readings
        stabilizer.submitReading(120, 80, 70)
        val r2 = stabilizer.submitReading(120, 80, 70)
        assertEquals(2, r2.window.size)

        // Submit 3 consecutive rejected frames
        val rej1 = stabilizer.submitReading(80, 80, 70) // sys <= dia
        assertTrue(rej1.verdict is LiveOcrVerdict.Rejected)
        assertEquals(2, rej1.window.size)

        val rej2 = stabilizer.submitReading(30, 20, 70) // sys < 40
        assertTrue(rej2.verdict is LiveOcrVerdict.Rejected)
        assertEquals(2, rej2.window.size)

        val rej3 = stabilizer.submitReading(120, 10, 70) // dia < 20
        assertTrue(rej3.verdict is LiveOcrVerdict.Rejected)
        assertEquals(2, rej3.window.size)

        // 4th rejected frame
        val rej4 = stabilizer.submitReading(120, 80, 20) // pul < 30
        assertTrue(rej4.verdict is LiveOcrVerdict.Rejected)
        assertEquals(2, rej4.window.size)
    }

    @Test
    fun `window sliding`() {
        val stabilizer = LiveOcrStabilizer(windowSize = 5, majority = 3, resetCount = 3)

        // Fill window with 5 distinct readings
        for (i in 0 until 5) {
            stabilizer.submitReading(120 + i, 80, 70)
        }
        val r5 = stabilizer.submitReading(125, 80, 70)
        assertEquals(5, r5.window.size)
        // First reading (120) should have slid out; window should have 121, 122, 123, 124, 125
        val sysValues = r5.window.map { it.sys }
        assertEquals(listOf(121, 122, 123, 124, 125), sysValues)

        // Submit 7th frame
        val r6 = stabilizer.submitReading(126, 80, 70)
        assertEquals(5, r6.window.size)
        assertEquals(listOf(122, 123, 124, 125, 126), r6.window.map { it.sys })
    }
}
