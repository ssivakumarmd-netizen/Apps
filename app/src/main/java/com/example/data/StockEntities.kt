package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stock_holdings")
data class StockHolding(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val symbol: String,
    val companyName: String,
    val shares: Double,
    val buyPrice: Double,
    val currentPrice: Double
) {
    val costBasis: Double get() = shares * buyPrice
    val currentValue: Double get() = shares * currentPrice
    val profitLoss: Double get() = currentValue - costBasis
    val profitLossPercentage: Double 
        get() = if (costBasis > 0) (profitLoss / costBasis) * 100.0 else 0.0
}

@Entity(tableName = "price_alerts")
data class PriceAlert(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val symbol: String,
    val targetPrice: Double,
    val isAbove: Boolean, // true if "price goes above", false if "price goes below"
    val isTriggered: Boolean = false,
    val triggeredAt: Long? = null
)
