package com.aliraza.dailytasks.data.db

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** Column formats: ISO dates, `HH:mm` wall-clock times, epoch milliseconds for instants. */
class Converters {
    @TypeConverter
    fun dateToText(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun textToDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun timeToText(value: LocalTime?): String? = value?.format(HOUR_MINUTE)

    @TypeConverter
    fun textToTime(value: String?): LocalTime? = value?.let { LocalTime.parse(it, HOUR_MINUTE) }

    @TypeConverter
    fun instantToMillis(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun millisToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun firedByToText(value: FiredBy): String = value.name

    @TypeConverter
    fun textToFiredBy(value: String): FiredBy = FiredBy.valueOf(value)

    private companion object {
        val HOUR_MINUTE: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
