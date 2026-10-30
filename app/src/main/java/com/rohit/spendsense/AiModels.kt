package com.rohit.spendsense

data class AiForecastDay(val date: String, val predicted: Double, val low: Double, val high: Double)
data class AiMetric(val name: String, val mae: Double, val rmse: Double)
data class AiForecast(
    val methodUsed: String,
    val metrics: List<AiMetric>,
    val days: List<AiForecastDay>,
    val totalPredicted: Double,
    val totalLow: Double,
    val totalHigh: Double,
    val metricsNote: String,
    val disclaimer: String
)
data class AiAnomaly(
    val key: String, val date: String, val title: String,
    val category: String, val amount: Double, val why: String
)
data class AiInsight(val text: String, val calculation: String)
data class AiReport(
    val asOf: String,
    val forecast: AiForecast?,
    val forecastReason: String,
    val anomalies: List<AiAnomaly>,
    val insights: List<AiInsight>
)

sealed interface AiState {
    data object Idle : AiState
    data object Loading : AiState
    data class Error(val message: String) : AiState
    data class Success(val report: AiReport) : AiState
}