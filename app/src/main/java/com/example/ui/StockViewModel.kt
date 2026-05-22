package com.example.ui

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class StockViewModel(
    private val application: Application,
    private val repository: StockRepository
) : AndroidViewModel(application) {

    val holdings: StateFlow<List<StockHolding>> = repository.allHoldings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val alerts: StateFlow<List<PriceAlert>> = repository.allAlerts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Calculated fields derived from holdings
    val portfolioSummary = holdings.map { holdingList ->
        val cost = holdingList.sumOf { it.costBasis }
        val value = holdingList.sumOf { it.currentValue }
        val pnl = value - cost
        val pnlPercent = if (cost > 0.0) (pnl / cost) * 100.0 else 0.0
        PortfolioSummary(
            totalCostBasis = cost,
            totalCurrentValue = value,
            totalProfitLoss = pnl,
            totalProfitLossPercentage = pnlPercent
        )
    }.stateIn(
        viewModelScope, 
        SharingStarted.WhileSubscribed(5000), 
        PortfolioSummary(0.0, 0.0, 0.0, 0.0)
    )

    private val _isAutoSimulating = MutableStateFlow(false)
    val isAutoSimulating: StateFlow<Boolean> = _isAutoSimulating.asStateFlow()

    private var simulationJob: Job? = null

    init {
        // Pre-populate data on start
        viewModelScope.launch {
            repository.prePopulateIfEmpty()
        }
    }

    // Portfolio CRUD Actions
    fun addHolding(symbol: String, companyName: String, shares: Double, buyPrice: Double, currentPrice: Double) {
        viewModelScope.launch {
            val formattedCompany = companyName.ifEmpty { getCompanyNameForSymbol(symbol) }
            val holding = StockHolding(
                symbol = symbol.uppercase().trim(),
                companyName = formattedCompany,
                shares = shares,
                buyPrice = buyPrice,
                currentPrice = currentPrice
            )
            repository.insertHolding(holding)
        }
    }

    fun updateHoldingPrice(holding: StockHolding, newPrice: Double) {
        viewModelScope.launch {
            val triggered = repository.forceUpdateStockPrice(holding.symbol, newPrice)
            handleTriggeredAlerts(triggered)
        }
    }

    fun deleteHolding(holding: StockHolding) {
        viewModelScope.launch {
            repository.deleteHolding(holding)
        }
    }

    fun resetPortfolio() {
        viewModelScope.launch {
            repository.clearAllHoldings()
            repository.prePopulateIfEmpty()
        }
    }

    // Alerts CRUD Actions
    fun createAlert(symbol: String, targetPrice: Double, isAbove: Boolean) {
        viewModelScope.launch {
            val alert = PriceAlert(
                symbol = symbol.uppercase().trim(),
                targetPrice = targetPrice,
                isAbove = isAbove,
                isTriggered = false
            )
            repository.insertAlert(alert)
        }
    }

    fun deleteAlert(alert: PriceAlert) {
        viewModelScope.launch {
            repository.deleteAlert(alert)
        }
    }

    fun resetAlert(alert: PriceAlert) {
        viewModelScope.launch {
            repository.updateAlert(alert.copy(isTriggered = false, triggeredAt = null))
        }
    }

    // Simulation controls
    fun toggleSimulation() {
        if (_isAutoSimulating.value) {
            _isAutoSimulating.value = false
            simulationJob?.cancel()
            simulationJob = null
        } else {
            _isAutoSimulating.value = true
            simulationJob = viewModelScope.launch {
                while (_isAutoSimulating.value) {
                    delay(4000) // update every 4 seconds for immediate feedback
                    val triggered = repository.simulatePriceFluctuations()
                    handleTriggeredAlerts(triggered)
                }
            }
        }
    }

    fun triggerQuickFluctuation() {
        viewModelScope.launch {
            val triggered = repository.simulatePriceFluctuations(multiplier = 2.0)
            handleTriggeredAlerts(triggered)
        }
    }

    private fun handleTriggeredAlerts(triggered: List<TriggeredAlertInfo>) {
        if (triggered.isNotEmpty()) {
            for (info in triggered) {
                NotificationHelper.showPriceAlertNotification(
                    context = application.applicationContext,
                    alert = info.alert,
                    currentPrice = info.stockPrice
                )
            }
        }
    }

    private fun getCompanyNameForSymbol(symbol: String): String {
        return when (symbol.uppercase().trim()) {
            "AAPL" -> "Apple Inc."
            "GOOG" -> "Alphabet Inc."
            "TSLA" -> "Tesla Inc."
            "NVDA" -> "NVIDIA Corporation"
            "MSFT" -> "Microsoft Corp."
            "AMZN" -> "Amazon.com, Inc."
            "META" -> "Meta Platforms, Inc."
            "NFLX" -> "Netflix, Inc."
            "NFL" -> "Netflix"
            "AMD" -> "Advanced Micro Devices"
            else -> "${symbol.uppercase().trim()} Corp."
        }
    }
}

data class PortfolioSummary(
    val totalCostBasis: Double,
    val totalCurrentValue: Double,
    val totalProfitLoss: Double,
    val totalProfitLossPercentage: Double
)

class StockViewModelFactory(
    private val application: Application,
    private val repository: StockRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StockViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StockViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
