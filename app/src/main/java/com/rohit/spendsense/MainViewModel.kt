package com.rohit.spendsense

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

private const val PREFS_NAME = "spendsense_prefs"
private const val KEY_BUDGET = "monthly_budget"
private const val KEY_THEME = "theme_mode"
private const val KEY_WELCOME_DONE = "welcome_done"
private const val KEY_SEEDED = "sample_seeded"
private const val KEY_NAME = "profile_name"
private const val KEY_EMAIL = "profile_email"
private const val KEY_CURRENCY = "currency_code"
private const val KEY_NOTIFY = "notifications_on"
private const val DEFAULT_BUDGET = 40000.0

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getInstance(application).transactionDao()
    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val transactions: StateFlow<List<Transaction>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _monthlyBudget = MutableStateFlow(
        prefs.getString(KEY_BUDGET, null)?.toDoubleOrNull() ?: DEFAULT_BUDGET
    )
    val monthlyBudget: StateFlow<Double> = _monthlyBudget.asStateFlow()

    private val _themeMode = MutableStateFlow(prefs.getString(KEY_THEME, "system") ?: "system")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _profileName = MutableStateFlow(prefs.getString(KEY_NAME, "") ?: "")
    val profileName: StateFlow<String> = _profileName.asStateFlow()

    private val _profileEmail = MutableStateFlow(prefs.getString(KEY_EMAIL, "") ?: "")
    val profileEmail: StateFlow<String> = _profileEmail.asStateFlow()

    private val _currencyCode = MutableStateFlow(prefs.getString(KEY_CURRENCY, "INR") ?: "INR")
    val currencyCode: StateFlow<String> = _currencyCode.asStateFlow()

    private val _notificationsOn = MutableStateFlow(prefs.getBoolean(KEY_NOTIFY, false))
    val notificationsOn: StateFlow<Boolean> = _notificationsOn.asStateFlow()

    val welcomeDone: Boolean = prefs.getBoolean(KEY_WELCOME_DONE, false)

    init {
        applyCurrency(_currencyCode.value)
        viewModelScope.launch {
            if (!prefs.getBoolean(KEY_SEEDED, false)) {
                if (dao.count() == 0) {
                    dao.insertAll(sampleTransactions.map { it.copy(id = 0) })
                }
                prefs.edit().putBoolean(KEY_SEEDED, true).apply()
            }
        }
    }

    private fun applyCurrency(code: String) {
        CurrencySettings.symbol =
            currencyOptions.firstOrNull { it.code == code }?.symbol ?: "₹"
    }

    fun addTransaction(transaction: Transaction) {
        viewModelScope.launch {
            val before = transactions.value
            dao.insert(transaction)
            if (_notificationsOn.value) {
                NotificationHelper.checkBudgetAlert(
                    getApplication(), before, before + transaction, _monthlyBudget.value
                )
            }
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch { dao.delete(transaction) }
    }

    fun addTransactions(list: List<Transaction>) {
        viewModelScope.launch { dao.insertAll(list) }
    }

    fun setMonthlyBudget(value: Double) {
        prefs.edit().putString(KEY_BUDGET, value.toString()).apply()
        _monthlyBudget.value = value
    }

    fun setThemeMode(mode: String) {
        prefs.edit().putString(KEY_THEME, mode).apply()
        _themeMode.value = mode
    }

    fun setProfile(name: String, email: String) {
        prefs.edit().putString(KEY_NAME, name).putString(KEY_EMAIL, email).apply()
        _profileName.value = name
        _profileEmail.value = email
    }

    fun setCurrency(code: String) {
        prefs.edit().putString(KEY_CURRENCY, code).apply()
        _currencyCode.value = code
        applyCurrency(code)
    }

    fun setNotifications(on: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFY, on).apply()
        _notificationsOn.value = on
    }

    fun markWelcomeDone() {
        prefs.edit().putBoolean(KEY_WELCOME_DONE, true).apply()
    }

    fun showWelcomeNextLaunch() {
        prefs.edit().putBoolean(KEY_WELCOME_DONE, false).apply()
    }

    fun deleteAllData() {
        viewModelScope.launch { dao.deleteAll() }
        setMonthlyBudget(DEFAULT_BUDGET)
    }
}

// Rules: only the current month counts, and income is never counted as an expense.
fun buildSummary(
    transactions: List<Transaction>,
    today: LocalDate = LocalDate.now(),
    monthlyBudget: Double = DEFAULT_BUDGET
): DashboardSummary {
    val thisMonth = transactions.filter {
        it.date.year == today.year && it.date.month == today.month
    }
    return DashboardSummary(
        totalExpenses = thisMonth.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount },
        totalIncome = thisMonth.filter { it.type == TransactionType.INCOME }.sumOf { it.amount },
        monthlyBudget = monthlyBudget,
        daysElapsed = today.dayOfMonth
    )
}