package com.rohit.spendsense

import androidx.room.TypeConverter
import java.time.LocalDate

// Room only stores basic types, so these convert our types to and from them.
class Converters {
    @TypeConverter
    fun fromDate(date: LocalDate): Long = date.toEpochDay()

    @TypeConverter
    fun toDate(value: Long): LocalDate = LocalDate.ofEpochDay(value)

    @TypeConverter
    fun fromType(type: TransactionType): String = type.name

    @TypeConverter
    fun toType(value: String): TransactionType = TransactionType.valueOf(value)

    @TypeConverter
    fun fromCategory(category: Category): String = category.name

    @TypeConverter
    fun toCategory(value: String): Category = Category.valueOf(value)
}