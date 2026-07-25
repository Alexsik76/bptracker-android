package ua.vn.home.bptracker.feature.prescriptions

import androidx.annotation.StringRes
import ua.vn.home.bptracker.R
import ua.vn.home.bptracker.data.dto.CourseType
import ua.vn.home.bptracker.data.dto.DoseUnit
import ua.vn.home.bptracker.data.dto.FreqPeriodUnit
import ua.vn.home.bptracker.data.dto.WhenSlot

@StringRes
fun WhenSlot.labelRes(): Int = when (this) {
    WhenSlot.Morning -> R.string.med_enum_slot_morning
    WhenSlot.Day -> R.string.med_enum_slot_day
    WhenSlot.Evening -> R.string.med_enum_slot_evening
}

@StringRes
fun DoseUnit.labelRes(): Int = when (this) {
    DoseUnit.Tablet -> R.string.med_enum_unit_tablet
    DoseUnit.Mg -> R.string.med_enum_unit_mg
    DoseUnit.Ml -> R.string.med_enum_unit_ml
    DoseUnit.Drop -> R.string.med_enum_unit_drop
    DoseUnit.Mcg -> R.string.med_enum_unit_mcg
    DoseUnit.Iu -> R.string.med_enum_unit_iu
}

@StringRes
fun FreqPeriodUnit.labelRes(): Int = when (this) {
    FreqPeriodUnit.Hour -> R.string.med_enum_freq_h
    FreqPeriodUnit.Day -> R.string.med_enum_freq_d
    FreqPeriodUnit.Week -> R.string.med_enum_freq_wk
}

@StringRes
fun CourseType.labelRes(): Int = when (this) {
    CourseType.Ongoing -> R.string.med_items_course_ongoing
    CourseType.Course -> R.string.med_items_course_limited
}
