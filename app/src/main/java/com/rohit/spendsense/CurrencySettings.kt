package com.rohit.spendsense

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class CurrencyOption(val code: String, val symbol: String, val name: String)

val currencyOptions = listOf(
    CurrencyOption("INR", "₹", "Indian Rupee"),
    CurrencyOption("USD", "$", "US Dollar"),
    CurrencyOption("EUR", "€", "Euro"),
    CurrencyOption("GBP", "£", "British Pound")
)

// Compose watches this value, so every screen updates when it changes.
object CurrencySettings {
    var symbol by mutableStateOf("₹")
}