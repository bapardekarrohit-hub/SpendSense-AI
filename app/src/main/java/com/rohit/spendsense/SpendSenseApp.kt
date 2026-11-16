package com.rohit.spendsense

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

enum class AppScreen(@StringRes val titleRes: Int, val icon: ImageVector) {
    Dashboard(R.string.nav_dashboard, Icons.Filled.Home),
    Transactions(R.string.nav_history, Icons.AutoMirrored.Filled.List),
    Analytics(R.string.nav_analytics, Icons.Filled.DateRange),
    Budgets(R.string.nav_budgets, Icons.Filled.AccountBox),
    AiInsights(R.string.nav_ai, Icons.Filled.Star),
    Settings(R.string.nav_settings, Icons.Filled.Settings)
}

@Composable
fun SpendSenseApp() {
    val appContext = LocalContext.current
    LaunchedEffect(Unit) { PrivacyControls.apply(appContext) }

    val viewModel: MainViewModel = viewModel()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val monthlyBudget by viewModel.monthlyBudget.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val summary = buildSummary(transactions, monthlyBudget = monthlyBudget)

    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    val screens = AppScreen.entries

    Scaffold(
        bottomBar = {
            NavigationBar {
                screens.forEachIndexed { index, screen ->
                    val title = stringResource(screen.titleRes)
                    NavigationBarItem(
                        selected = selectedIndex == index,
                        onClick = { selectedIndex = index },
                        icon = { Icon(screen.icon, contentDescription = title) },
                        label = { Text(text = title, maxLines = 1, fontSize = 11.sp) }
                    )
                }
            }
        }
    ) { innerPadding ->
        val screenModifier = Modifier.padding(innerPadding)
        when (screens[selectedIndex]) {
            AppScreen.Dashboard -> DashboardScreen(
                summary = summary,
                transactions = transactions,
                modifier = screenModifier
            )
            AppScreen.Transactions -> TransactionsScreen(
                transactions = transactions,
                onAddClick = { showAddDialog = true },
                onDelete = { viewModel.deleteTransaction(it) },
                modifier = screenModifier
            )
            AppScreen.Analytics -> AnalyticsScreen(
                transactions = transactions,
                modifier = screenModifier
            )
            AppScreen.Budgets -> BudgetsScreen(
                transactions = transactions,
                monthlyBudget = monthlyBudget,
                onSetBudget = { viewModel.setMonthlyBudget(it) },
                modifier = screenModifier
            )
            AppScreen.AiInsights -> AiInsightsScreen(
                transactions = transactions,
                monthlyBudget = monthlyBudget.toDouble(),
                modifier = screenModifier
            )
            AppScreen.Settings -> SettingsScreen(
                viewModel = viewModel,
                transactions = transactions,
                modifier = screenModifier
            )
        }

        if (showAddDialog) {
            AddTransactionDialog(
                onDismiss = { showAddDialog = false },
                onSave = {
                    viewModel.addTransaction(it)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun PlaceholderScreen(title: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}