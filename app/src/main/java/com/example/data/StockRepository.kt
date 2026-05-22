package com.example.data

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlin.random.Random

class StockRepository(private val stockDao: StockDao) {

    val allHoldings: Flow<List<StockHolding>> = stockDao.getAllHoldings()
    val allAlerts: Flow<List<PriceAlert>> = stockDao.getAllAlerts()

    suspend fun insertHolding(holding: StockHolding) {
        val existing = stockDao.getHoldingBySymbol(holding.symbol.uppercase().trim())
        if (existing != null) {
            // Aggregate shares and calculate weighted average buy price
            val totalShares = existing.shares + holding.shares
            val totalCost = (existing.shares * existing.buyPrice) + (holding.shares * holding.buyPrice)
            val avgPrice = if (totalShares > 0) totalCost / totalShares else 0.0
            
            val updated = existing.copy(
                shares = totalShares,
                buyPrice = avgPrice,
                currentPrice = holding.currentPrice // Keep the new added stock's current market price
            )
            stockDao.updateHolding(updated)
        } else {
            stockDao.insertHolding(holding.copy(symbol = holding.symbol.uppercase().trim()))
        }
    }

    suspend fun updateHolding(holding: StockHolding) {
        stockDao.updateHolding(holding)
    }

    suspend fun deleteHolding(holding: StockHolding) {
        stockDao.deleteHolding(holding)
    }

    suspend fun clearAllHoldings() {
        stockDao.clearHoldings()
    }

    suspend fun insertAlert(alert: PriceAlert) {
        stockDao.insertAlert(alert.copy(symbol = alert.symbol.uppercase().trim()))
    }

    suspend fun deleteAlert(alert: PriceAlert) {
        stockDao.deleteAlert(alert)
    }

    suspend fun updateAlert(alert: PriceAlert) {
        stockDao.updateAlert(alert)
    }

    /**
     * Pre-populates the database with some standard default stocks if it is empty.
     */
    suspend fun prePopulateIfEmpty() {
        val list = stockDao.getAllHoldingsList()
        if (list.isEmpty()) {
            val defaults = listOf(
                StockHolding(symbol = "AAPL", companyName = "Apple Inc.", shares = 15.0, buyPrice = 175.50, currentPrice = 182.30),
                StockHolding(symbol = "GOOG", companyName = "Alphabet Inc.", shares = 10.0, buyPrice = 145.00, currentPrice = 152.45),
                StockHolding(symbol = "TSLA", companyName = "Tesla Inc.", shares = 8.0, buyPrice = 195.00, currentPrice = 178.50),
                StockHolding(symbol = "NVDA", companyName = "NVIDIA Corp.", shares = 20.0, buyPrice = 450.00, currentPrice = 485.20)
            )
            for (stock in defaults) {
                stockDao.insertHolding(stock)
            }
            
            // Add a mock alert that starts close to trigger for demo
            stockDao.insertAlert(PriceAlert(symbol = "AAPL", targetPrice = 185.00, isAbove = true))
            stockDao.insertAlert(PriceAlert(symbol = "TSLA", targetPrice = 175.00, isAbove = false))
        }
    }

    /**
     * Simulates stock price fluctuations and returns any newly triggered alerts.
     */
    suspend fun simulatePriceFluctuations(multiplier: Double = 1.0): List<TriggeredAlertInfo> {
        val holdings = stockDao.getAllHoldingsList()
        if (holdings.isEmpty()) return emptyList()

        val updatedHoldings = holdings.map { holding ->
            // Change price by a random percentage between -4% and +4%, scaled by multiplier
            val changePercent = (Random.nextDouble(-0.04, 0.04)) * multiplier
            val originalPrice = holding.currentPrice
            val newPrice = (originalPrice * (1 + changePercent)).coerceAtLeast(0.1)
            
            // Format to 2 decimal places
            val roundedPrice = Math.round(newPrice * 100.0) / 100.0
            holding.copy(currentPrice = roundedPrice)
        }

        // Update DB
        for (updated in updatedHoldings) {
            stockDao.updateHolding(updated)
        }

        // Check alerts
        val activeAlerts = stockDao.getActiveAlertsList()
        val newlyTriggered = mutableListOf<TriggeredAlertInfo>()

        for (alert in activeAlerts) {
            val associatedStock = updatedHoldings.find { it.symbol == alert.symbol } ?: continue
            val currentPrice = associatedStock.currentPrice
            
            val isTriggeredNow = if (alert.isAbove) {
                currentPrice >= alert.targetPrice
            } else {
                currentPrice <= alert.targetPrice
            }

            if (isTriggeredNow) {
                val nowTime = System.currentTimeMillis()
                stockDao.updateAlertTriggerStatus(alert.id, isTriggered = true, triggeredAt = nowTime)
                newlyTriggered.add(TriggeredAlertInfo(
                    alert = alert.copy(isTriggered = true, triggeredAt = nowTime),
                    stockPrice = currentPrice
                ))
            }
        }

        return newlyTriggered
    }

    /**
     * Directly force-updates a stock's current price (e.g. from custom input edit) and evaluates alerts.
     */
    suspend fun forceUpdateStockPrice(symbol: String, newPrice: Double): List<TriggeredAlertInfo> {
        val uppercaseSymbol = symbol.uppercase().trim()
        val holding = stockDao.getHoldingBySymbol(uppercaseSymbol) ?: return emptyList()
        
        val roundedPrice = Math.round(newPrice * 100.0) / 100.0
        val updated = holding.copy(currentPrice = roundedPrice)
        stockDao.updateHolding(updated)

        val activeAlerts = stockDao.getActiveAlertsList()
        val newlyTriggered = mutableListOf<TriggeredAlertInfo>()

        for (alert in activeAlerts) {
            if (alert.symbol != uppercaseSymbol) continue
            
            val isTriggeredNow = if (alert.isAbove) {
                roundedPrice >= alert.targetPrice
            } else {
                roundedPrice <= alert.targetPrice
            }

            if (isTriggeredNow) {
                val nowTime = System.currentTimeMillis()
                stockDao.updateAlertTriggerStatus(alert.id, isTriggered = true, triggeredAt = nowTime)
                newlyTriggered.add(TriggeredAlertInfo(
                    alert = alert.copy(isTriggered = true, triggeredAt = nowTime),
                    stockPrice = roundedPrice
                ))
            }
        }

        return newlyTriggered
    }
}

data class TriggeredAlertInfo(
    val alert: PriceAlert,
    val stockPrice: Double
)
