package com.rohit.spendsense

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

enum class TransactionType { INCOME, EXPENSE }

enum class Category(val label: String) {
    Food("Food"),
    Transport("Transport"),
    Shopping("Shopping"),
    Education("Education"),
    Bills("Bills"),
    Healthcare("Healthcare"),
    Entertainment("Entertainment"),
    Rent("Rent"),
    Travel("Travel"),
    Other("Other")
}

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val category: Category,
    val date: LocalDate,
    val paymentMethod: String,
    val notes: String = ""
)

// Sample data (still used by the screens until Step 6C).
val sampleTransactions = listOf(
    Transaction(1, "Salary", 55000.0, TransactionType.INCOME, Category.Other, LocalDate.of(2026, 10, 1), "Bank transfer"),
    Transaction(2, "House rent", 15000.0, TransactionType.EXPENSE, Category.Rent, LocalDate.of(2026, 10, 2), "UPI"),
    Transaction(3, "Groceries", 4200.0, TransactionType.EXPENSE, Category.Food, LocalDate.of(2026, 10, 5), "UPI"),
    Transaction(4, "Electricity bill", 2300.0, TransactionType.EXPENSE, Category.Bills, LocalDate.of(2026, 10, 8), "Card"),
    Transaction(5, "Online course", 3500.0, TransactionType.EXPENSE, Category.Education, LocalDate.of(2026, 10, 10), "Card"),
    Transaction(6, "Freelance project", 5000.0, TransactionType.INCOME, Category.Other, LocalDate.of(2026, 10, 12), "UPI"),
    Transaction(7, "Cab rides", 1450.0, TransactionType.EXPENSE, Category.Transport, LocalDate.of(2026, 10, 14), "UPI"),
    Transaction(8, "Clothes", 2800.0, TransactionType.EXPENSE, Category.Shopping, LocalDate.of(2026, 10, 17), "Card"),
    Transaction(9, "Movie night", 900.0, TransactionType.EXPENSE, Category.Entertainment, LocalDate.of(2026, 10, 19), "Cash"),
    Transaction(10, "Doctor visit", 1200.0, TransactionType.EXPENSE, Category.Healthcare, LocalDate.of(2026, 10, 21), "Cash"),
    Transaction(11, "Weekend trip", 1100.0, TransactionType.EXPENSE, Category.Travel, LocalDate.of(2026, 10, 24), "UPI")
)