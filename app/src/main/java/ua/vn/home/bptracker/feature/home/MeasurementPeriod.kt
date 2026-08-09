package ua.vn.home.bptracker.feature.home

import ua.vn.home.bptracker.R
import java.time.OffsetDateTime

enum class MeasurementPeriod(val labelRes: Int) {
    WEEK(R.string.period_week),
    MONTH(R.string.period_month),
    THREE_MONTHS(R.string.period_three_months),
    SINCE_PRESCRIPTION(R.string.period_since_prescription),
    ALL(R.string.period_all);

    fun getDateFrom(prescriptionStartDate: OffsetDateTime?): OffsetDateTime? {
        val now = OffsetDateTime.now()
        return when (this) {
            WEEK -> now.minusDays(7)
            MONTH -> now.minusMonths(1)
            THREE_MONTHS -> now.minusMonths(3)
            SINCE_PRESCRIPTION -> prescriptionStartDate ?: ALL.getDateFrom(null)
            ALL -> null
        }
    }
}
