package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LogisticsDao {
    @Query("SELECT * FROM shipments ORDER BY timestamp DESC")
    fun getAllShipments(): Flow<List<ShipmentEntity>>

    @Query("SELECT * FROM shipments WHERE shipmentId = :shipmentId LIMIT 1")
    suspend fun getShipmentById(shipmentId: String): ShipmentEntity?

    @Query("SELECT * FROM shipments WHERE trackingNumber = :trackingNumber LIMIT 1")
    suspend fun getShipmentByTrackingNumber(trackingNumber: String): ShipmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShipment(shipment: ShipmentEntity): Long

    @Query("UPDATE shipments SET status = :status WHERE shipmentId = :shipmentId")
    suspend fun updateShipmentStatus(shipmentId: String, status: String): Int

    @Delete
    suspend fun deleteShipment(shipment: ShipmentEntity)

    @Query("SELECT * FROM webhook_logs ORDER BY timestamp DESC")
    fun getAllWebhookLogs(): Flow<List<WebhookLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWebhookLog(log: WebhookLogEntity)

    @Query("DELETE FROM webhook_logs")
    suspend fun clearWebhookLogs()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(sub: WebhookSubscriptionEntity)

    @Query("DELETE FROM webhook_subscriptions WHERE url = :url")
    suspend fun deleteSubscription(url: String)

    @Query("SELECT * FROM webhook_subscriptions")
    suspend fun getAllSubscriptions(): List<WebhookSubscriptionEntity>
}
