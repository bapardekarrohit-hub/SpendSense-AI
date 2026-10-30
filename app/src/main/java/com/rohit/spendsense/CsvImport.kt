package com.rohit.spendsense

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.Locale

data class ImportResult(
    val imported: List<Transaction> = emptyList(),
    val duplicates: Int = 0,
    val errors: List<String> = emptyList(),
    val totalRows: Int = 0,
    val fatal: String? = null
)

private val dateFormats = listOf(
    "uuuu-MM-dd", "dd/MM/uuuu", "d/M/uuuu", "dd-MM-uuuu", "d-M-uuuu", "dd MMM uuuu"
).map { DateTimeFormatter.ofPattern(it, Locale.ENGLISH).withResolverStyle(ResolverStyle.STRICT) }

fun dateOf(raw: String): LocalDate? {
    for (format in dateFormats) {
        try {
            return LocalDate.parse(raw, format)
        } catch (e: Exception) {
            // try the next format
        }
    }
    return null
}

fun amountOf(raw: String): Double? =
    raw.replace(Regex("[₹\$€£,\\s]"), "").toDoubleOrNull()?.takeIf { it.isFinite() }

fun typeOf(raw: String): TransactionType? =
    when (raw.trim().lowercase()) {
        "income", "credit", "cr" -> TransactionType.INCOME
        "expense", "debit", "dr", "" -> TransactionType.EXPENSE
        else -> null
    }

fun categoryOf(raw: String): Category {
    val key = raw.trim().lowercase()
    return Category.entries.firstOrNull {
        it.name.lowercase() == key || it.label.lowercase() == key
    } ?: Category.Other
}

fun cellOf(row: List<String>, index: Int?): String =
    if (index != null && index < row.size) row[index].trim() else ""

fun duplicateKeyOf(t: Transaction): String =
    "${t.title.lowercase()}|${t.amount}|${t.type}|${t.date}|${t.category}"