package com.example.swpp.tododapp.data.source.local

import androidx.room.TypeConverter
import java.time.LocalDate

/**
 * Stores [LocalDate] as the number of days since 1970-01-01.
 */
class Converters {

    @TypeConverter
    fun fromEpochDay(value: Long?): LocalDate? = value?.let { LocalDate.ofEpochDay(it) }

    @TypeConverter
    fun toEpochDay(date: LocalDate?): Long? = date?.toEpochDay()
}
