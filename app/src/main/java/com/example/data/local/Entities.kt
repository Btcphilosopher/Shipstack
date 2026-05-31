package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.example.data.model.Address
import com.example.data.model.CustomsForm
import com.example.data.model.TrackingEvent
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

@Entity(tableName = "shipments")
data class ShipmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shipmentId: String,
    val trackingNumber: String,
    val courier: String, // DHL, UPS, FedEx, Royal Mail
    val serviceName: String,
    val fromAddress: Address,
    val toAddress: Address,
    val weightKg: Double,
    val price: Double,
    val eta: String,
    val status: String, // CREATED, IN_TRANSIT, OUT_FOR_DELIVERY, DELIVERED, EXCEPTION
    val packageContent: String,
    val isReturn: Boolean = false,
    val originalShipmentId: String? = null, // for returns
    val isInternational: Boolean = false,
    val customsForm: CustomsForm? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "webhook_logs")
data class WebhookLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shipmentId: String,
    val eventName: String, // shipment.created, shipment.dispatched, shipment.delivered, shipment.failed
    val url: String,
    val payload: String, // JSON payload sent
    val responseCode: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "webhook_subscriptions")
data class WebhookSubscriptionEntity(
    @PrimaryKey val url: String,
    val secretToken: String,
    val events: List<String> // represented as a list of strings
)

class Converters {
    private val moshi = Moshi.Builder().build()
    
    @TypeConverter
    fun fromAddress(address: Address): String {
        val adapter = moshi.adapter(Address::class.java)
        return adapter.toJson(address)
    }

    @TypeConverter
    fun toAddress(json: String): Address {
        val adapter = moshi.adapter(Address::class.java)
        return adapter.fromJson(json) ?: Address("", null, "", null, "", "")
    }

    @TypeConverter
    fun fromCustomsForm(form: CustomsForm?): String {
        if (form == null) return ""
        val adapter = moshi.adapter(CustomsForm::class.java)
        return adapter.toJson(form)
    }

    @TypeConverter
    fun toCustomsForm(json: String): CustomsForm? {
        if (json.isEmpty()) return null
        val adapter = moshi.adapter(CustomsForm::class.java)
        return adapter.fromJson(json)
    }

    @TypeConverter
    fun fromStringList(list: List<String>): String {
        val type = Types.newParameterizedType(List::class.java, String::class.java)
        val adapter = moshi.adapter<List<String>>(type)
        return adapter.toJson(list)
    }

    @TypeConverter
    fun toStringList(json: String): List<String> {
        val type = Types.newParameterizedType(List::class.java, String::class.java)
        val adapter = moshi.adapter<List<String>>(type)
        return adapter.fromJson(json) ?: emptyList()
    }
}
