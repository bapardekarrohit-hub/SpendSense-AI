package com.rohit.spendsense

private const val HELP = "title,amount,type,category,date,payment_method,notes"

// Checks every row. Nothing is saved here; the result is shown as a preview first.
fun parseTransactionCsv(text: String, existing: List<Transaction>): ImportResult {
    if (text.length > 2_000_000) {
        return ImportResult(fatal = "The file is too large (limit about 2 MB).")
    }
    val rows = parseCsv(text)
    if (rows.isEmpty()) return ImportResult(fatal = "The file is empty.")

    val headers = rows.first().map { it.trim().lowercase().replace(' ', '_') }
    fun col(vararg names: String): Int? =
        names.firstNotNullOfOrNull { name -> headers.indexOf(name).takeIf { it >= 0 } }

    val titleCol = col("title", "description", "name", "details")
    val amountCol = col("amount", "value")
    val dateCol = col("date", "transaction_date")
    val typeCol = col("type")
    val categoryCol = col("category")
    val paymentCol = col("payment_method", "payment", "method", "mode")
    val notesCol = col("notes", "note", "remarks")

    if (titleCol == null || amountCol == null || dateCol == null) {
        val missing = listOfNotNull(
            "title".takeIf { titleCol == null },
            "amount".takeIf { amountCol == null },
            "date".takeIf { dateCol == null }
        )
        return ImportResult(
            fatal = "Missing required column(s): ${missing.joinToString(", ")}.\n\n" +
                    "The first line of the file must be a header such as:\n$HELP"
        )
    }

    val dataRows = rows.drop(1)
    if (dataRows.isEmpty()) {
        return ImportResult(fatal = "The file has a header but no transactions.")
    }
    if (dataRows.size > 5000) {
        return ImportResult(fatal = "Too many rows (${dataRows.size}). The limit is 5000 per import.")
    }

    val seen = existing.map { duplicateKeyOf(it) }.toMutableSet()
    val imported = mutableListOf<Transaction>()
    val errors = mutableListOf<String>()
    var duplicates = 0

    dataRows.forEachIndexed { index, row ->
        val line = index + 2
        val title = cellOf(row, titleCol)
        val amountRaw = cellOf(row, amountCol)
        val dateRaw = cellOf(row, dateCol)
        val typeRaw = cellOf(row, typeCol)
        val amount = amountOf(amountRaw)
        val type = typeOf(typeRaw)
        val date = dateOf(dateRaw)

        val problem = when {
            title.isEmpty() -> "title is empty"
            title.length > 60 -> "title is longer than 60 characters"
            amount == null -> "amount '$amountRaw' is not a number"
            amount <= 0.0 -> "amount must be greater than 0"
            amount > 10_000_000.0 -> "amount is too large"
            type == null -> "type '$typeRaw' is not income or expense"
            date == null -> "date '$dateRaw' is not valid (use yyyy-MM-dd or dd/MM/yyyy)"
            date.year < 2000 || date.year > 2100 -> "date '$dateRaw' is out of range"
            else -> null
        }
        if (problem != null || amount == null || type == null || date == null) {
            errors.add("Row $line: ${problem ?: "invalid row"}")
            return@forEachIndexed
        }

        val transaction = Transaction(
            title = title,
            amount = amount,
            type = type,
            category = categoryOf(cellOf(row, categoryCol)),
            date = date,
            paymentMethod = cellOf(row, paymentCol).ifEmpty { "Other" }.take(30),
            notes = cellOf(row, notesCol).take(200)
        )
        if (seen.add(duplicateKeyOf(transaction))) imported.add(transaction) else duplicates++
    }

    return ImportResult(imported, duplicates, errors, dataRows.size)
}