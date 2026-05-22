package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StockDao {
    // Stock holdings queries
    @Query("SELECT * FROM stock_holdings ORDER BY symbol ASC")
    fun getAllHoldings(): Flow<List<StockHolding>>

    @Query("SELECT * FROM stock_holdings")
    suspend fun getAllHoldingsList(): List<StockHolding>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHolding(holding: StockHolding)

    @Update
    suspend fun updateHolding(holding: StockHolding)

    @Delete
    suspend fun deleteHolding(holding: StockHolding)

    @Query("SELECT * FROM stock_holdings WHERE symbol = :symbol LIMIT 1")
    suspend fun getHoldingBySymbol(symbol: String): StockHolding?

    @Query("DELETE FROM stock_holdings")
    suspend fun clearHoldings()

    // Price alerts queries
    @Query("SELECT * FROM price_alerts ORDER BY id DESC")
    fun getAllAlerts(): Flow<List<PriceAlert>>

    @Query("SELECT * FROM price_alerts WHERE isTriggered = 0")
    suspend fun getActiveAlertsList(): List<PriceAlert>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: PriceAlert)

    @Update
    suspend fun updateAlert(alert: PriceAlert)

    @Delete
    suspend fun deleteAlert(alert: PriceAlert)

    @Query("UPDATE price_alerts SET isTriggered = :isTriggered, triggeredAt = :triggeredAt WHERE id = :id")
    suspend fun updateAlertTriggerStatus(id: Int, isTriggered: Boolean, triggeredAt: Long?)
}
