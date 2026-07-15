package ua.vn.home.bptracker.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class WhenSlot {
    @SerialName("Morning") Morning,
    @SerialName("Day") Day,
    @SerialName("Evening") Evening
}

@Serializable
enum class DoseUnit {
    @SerialName("tablet") Tablet,
    @SerialName("mg") Mg,
    @SerialName("ml") Ml,
    @SerialName("drop") Drop,
    @SerialName("mcg") Mcg,
    @SerialName("IU") Iu
}

@Serializable
enum class FreqPeriodUnit {
    @SerialName("h") Hour,
    @SerialName("d") Day,
    @SerialName("wk") Week
}

@Serializable
enum class CourseType {
    @SerialName("ongoing") Ongoing,
    @SerialName("course") Course
}
