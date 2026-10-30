package ua.vn.home.bptracker.core.utils

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

fun localDateToDatePickerMillis(date: LocalDate): Long {
    return date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}

fun datePickerMillisToLocalDate(millis: Long): LocalDate {
    return Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
}

fun localDateToUtcMillis(date: LocalDate): Long = localDateToDatePickerMillis(date)

fun utcMillisToLocalDate(millis: Long): LocalDate = datePickerMillisToLocalDate(millis)
