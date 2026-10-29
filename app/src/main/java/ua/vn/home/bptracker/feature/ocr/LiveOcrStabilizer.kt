package ua.vn.home.bptracker.feature.ocr

data class AcceptedReading(
    val sys: Int,
    val dia: Int,
    val pul: Int,
    val minConf: Float = 0f,
    val meanConf: Float = 0f,
)

data class Consensus(
    val sys: Int,
    val dia: Int,
    val pul: Int,
)

data class FieldVotes(
    val sys: Map<Int, Int>,
    val dia: Map<Int, Int>,
    val pul: Map<Int, Int>,
) {
    val maxSysVotes: Int get() = sys.values.maxOrNull() ?: 0
    val maxDiaVotes: Int get() = dia.values.maxOrNull() ?: 0
    val maxPulVotes: Int get() = pul.values.maxOrNull() ?: 0
    val minOfMaxVotes: Int get() = if (sys.isEmpty() || dia.isEmpty() || pul.isEmpty()) 0 else minOf(maxSysVotes, maxDiaVotes, maxPulVotes)
}

sealed interface LiveOcrVerdict {
    val name: String

    data object Accepted : LiveOcrVerdict {
        override val name: String get() = "accepted"
        override fun toString(): String = "accepted"
    }

    data class Rejected(val reason: String) : LiveOcrVerdict {
        override val name: String get() = "rejected"
        override fun toString(): String = "rejected: $reason"
    }

    data class Empty(val reason: String) : LiveOcrVerdict {
        override val name: String get() = "empty"
        override fun toString(): String = "empty: $reason"
    }

    data object Reset : LiveOcrVerdict {
        override val name: String get() = "reset"
        override fun toString(): String = "reset"
    }
}

data class StabilizerResult(
    val verdict: LiveOcrVerdict,
    val window: List<AcceptedReading>,
    val votes: FieldVotes,
    val consensus: Consensus? = null,
) {
    val isConsensus: Boolean get() = consensus != null
    val minVotes: Int get() = votes.minOfMaxVotes
}

sealed interface LiveOcrInput {
    data class Reading(
        val sys: Int,
        val dia: Int,
        val pul: Int,
        val minConf: Float = 0f,
        val meanConf: Float = 0f,
    ) : LiveOcrInput

    data class Empty(val reason: String) : LiveOcrInput
}

class LiveOcrStabilizer(
    val windowSize: Int = 5,
    val majority: Int = 3,
    val resetCount: Int = 3,
) {
    private val window = ArrayDeque<AcceptedReading>()
    private var consecutiveEmptyFrames = 0

    fun submit(input: LiveOcrInput): StabilizerResult = when (input) {
        is LiveOcrInput.Reading -> submitReading(input.sys, input.dia, input.pul, input.minConf, input.meanConf)
        is LiveOcrInput.Empty -> submitEmpty(input.reason)
    }

    fun submitReading(
        sys: Int,
        dia: Int,
        pul: Int,
        minConf: Float = 0f,
        meanConf: Float = 0f,
    ): StabilizerResult {
        val rejectionReason = validate(sys, dia, pul)
        if (rejectionReason != null) {
            consecutiveEmptyFrames = 0
            val votes = calculateVotes()
            return StabilizerResult(
                verdict = LiveOcrVerdict.Rejected(rejectionReason),
                window = window.toList(),
                votes = votes,
                consensus = calculateConsensus(votes),
            )
        }

        consecutiveEmptyFrames = 0
        window.addLast(AcceptedReading(sys, dia, pul, minConf, meanConf))
        while (window.size > windowSize) {
            window.removeFirst()
        }

        val votes = calculateVotes()
        return StabilizerResult(
            verdict = LiveOcrVerdict.Accepted,
            window = window.toList(),
            votes = votes,
            consensus = calculateConsensus(votes),
        )
    }

    fun submitEmpty(reason: String): StabilizerResult {
        consecutiveEmptyFrames++
        if (consecutiveEmptyFrames >= resetCount) {
            window.clear()
            consecutiveEmptyFrames = 0
            val emptyVotes = FieldVotes(emptyMap(), emptyMap(), emptyMap())
            return StabilizerResult(
                verdict = LiveOcrVerdict.Reset,
                window = emptyList(),
                votes = emptyVotes,
                consensus = null,
            )
        }

        val votes = calculateVotes()
        return StabilizerResult(
            verdict = LiveOcrVerdict.Empty(reason),
            window = window.toList(),
            votes = votes,
            consensus = calculateConsensus(votes),
        )
    }

    fun reset() {
        window.clear()
        consecutiveEmptyFrames = 0
    }

    private fun validate(sys: Int, dia: Int, pul: Int): String? {
        if (sys !in 40..300) return "SYS out of range: $sys (expected 40..300)"
        if (dia !in 20..200) return "DIA out of range: $dia (expected 20..200)"
        if (pul !in 30..250) return "PUL out of range: $pul (expected 30..250)"
        if (sys <= dia) return "SYS must be greater than DIA: sys=$sys, dia=$dia"
        return null
    }

    private fun calculateVotes(): FieldVotes {
        if (window.isEmpty()) {
            return FieldVotes(emptyMap(), emptyMap(), emptyMap())
        }
        val sysVotes = mutableMapOf<Int, Int>()
        val diaVotes = mutableMapOf<Int, Int>()
        val pulVotes = mutableMapOf<Int, Int>()

        for (item in window) {
            sysVotes[item.sys] = (sysVotes[item.sys] ?: 0) + 1
            diaVotes[item.dia] = (diaVotes[item.dia] ?: 0) + 1
            pulVotes[item.pul] = (pulVotes[item.pul] ?: 0) + 1
        }
        return FieldVotes(sysVotes, diaVotes, pulVotes)
    }

    private fun calculateConsensus(votes: FieldVotes): Consensus? {
        val maxSys = votes.sys.maxByOrNull { it.value }
        val maxDia = votes.dia.maxByOrNull { it.value }
        val maxPul = votes.pul.maxByOrNull { it.value }

        return if (maxSys != null && maxSys.value >= majority &&
            maxDia != null && maxDia.value >= majority &&
            maxPul != null && maxPul.value >= majority
        ) {
            Consensus(sys = maxSys.key, dia = maxDia.key, pul = maxPul.key)
        } else {
            null
        }
    }
}
