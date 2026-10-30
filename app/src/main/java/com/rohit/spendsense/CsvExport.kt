package com.rohit.spendsense

const val CSV_HEADER = "title,amount,type,category,date,payment_method,notes"

// Wraps a value in quotes when it contains a comma, quote or new line.
private fun csvCell(value: String): String =
    if (value.contains(',') || value.contains('"') || value.contains('\n')) {
        "\"" + value.replace("\"", "\"\"") + "\""
    } else {
        value
    }

fun transactionsToCsv(list: List<Transaction>): String =
    buildString {
        appendLine(CSV_HEADER)
        list.forEach { t ->
            val cells = listOf(
                t.title,
                t.amount.toString(),
                t.type.name,
                t.category.name,
                t.date.toString(),
                t.paymentMethod,
                t.notes
            )
            appendLine(cells.joinToString(",") { csvCell(it) })
        }
    }