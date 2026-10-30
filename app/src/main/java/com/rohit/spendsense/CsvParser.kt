package com.rohit.spendsense

// Reads CSV text into rows. Handles quoted cells, commas and new lines inside quotes, and Windows line endings.
fun parseCsv(text: String): List<List<String>> {
    val s = text.removePrefix("\uFEFF")
    val rows = mutableListOf<List<String>>()
    var row = mutableListOf<String>()
    val cell = StringBuilder()
    var inQuotes = false
    var i = 0

    while (i < s.length) {
        val c = s[i]
        if (inQuotes) {
            if (c == '"' && i + 1 < s.length && s[i + 1] == '"') {
                cell.append('"')
                i++
            } else if (c == '"') {
                inQuotes = false
            } else {
                cell.append(c)
            }
        } else if (c == '"') {
            inQuotes = true
        } else if (c == ',') {
            row.add(cell.toString())
            cell.clear()
        } else if (c == '\n' || c == '\r') {
            if (c == '\r' && i + 1 < s.length && s[i + 1] == '\n') i++
            row.add(cell.toString())
            cell.clear()
            if (row.any { it.isNotBlank() }) rows.add(row)
            row = mutableListOf()
        } else {
            cell.append(c)
        }
        i++
    }

    if (cell.isNotEmpty() || row.isNotEmpty()) {
        row.add(cell.toString())
        if (row.any { it.isNotBlank() }) rows.add(row)
    }
    return rows
}