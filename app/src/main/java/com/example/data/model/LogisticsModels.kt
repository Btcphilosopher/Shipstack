package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Address(
    val line1: String,
    val line2: String? = null,
    val city: String,
    val state: String? = null,
    val postalCode: String,
    val country: String,
    val isValid: Boolean = true,
    val normalizedAddress: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

@JsonClass(generateAdapter = true)
data class Parcel(
    val lengthCm: Double,
    val widthCm: Double,
    val heightCm: Double,
    val weightKg: Double,
    val category: String = "General Goods"
)

@JsonClass(generateAdapter = true)
data class RateOption(
    val courier: String, // DHL, UPS, FedEx, Royal Mail
    val serviceName: String,
    val price: Double,
    val etaDays: Int,
    val rating: Double, // 1.0 to 5.0 weighted score
    val isCheapest: Boolean = false,
    val isFastest: Boolean = false,
    val isRecommended: Boolean = false,
    val routeCacheHit: Boolean = false
)

@JsonClass(generateAdapter = true)
data class RateComparisonRequest(
    val fromAddress: Address,
    val toAddress: Address,
    val parcel: Parcel,
    val selectedWarehouseId: String? = "WH-LON-01"
)

@JsonClass(generateAdapter = true)
data class RateComparisonResponse(
    val from: String,
    val to: String,
    val options: List<RateOption>,
    val timestamp: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class ShipmentCreateRequest(
    val fromAddress: Address,
    val toAddress: Address,
    val parcel: Parcel,
    val selectedCourier: String,
    val serviceName: String,
    val price: Double,
    val packageContent: String,
    val declaredValue: Double = 50.0,
    val isInternational: Boolean = false,
    val customsForm: CustomsForm? = null
)

@JsonClass(generateAdapter = true)
data class CustomsForm(
    val contentDescription: String,
    val hsCode: String, // Harmonized System tariff code
    val tarrifTax: Double,
    val declarationStatement: String
)

@JsonClass(generateAdapter = true)
data class ShipmentResponse(
    val shipmentId: String,
    val trackingNumber: String,
    val courier: String,
    val status: String,
    val labelUrl: String,
    val pickupScheduledTime: String?,
    val eta: String
)

@JsonClass(generateAdapter = true)
data class TrackingEvent(
    val timestamp: String,
    val status: String,
    val location: String,
    val description: String
)

@JsonClass(generateAdapter = true)
data class TrackingResponse(
    val trackingNumber: String,
    val courier: String,
    val currentStatus: String, // CREATED, IN_TRANSIT, OUT_FOR_DELIVERY, DELIVERED, EXCEPTION
    val estimatedDelivery: String,
    val events: List<TrackingEvent>
)

@JsonClass(generateAdapter = true)
data class WebhookRegistration(
    val url: String,
    val events: List<String>, // e.g. ["shipment.created", "shipment.dispatched", "shipment.delivered"]
    val secretToken: String
)

@JsonClass(generateAdapter = true)
data class WebhookPayload(
    val eventId: String,
    val event: String, // shipment.created etc
    val timestamp: Long,
    val data: Any // Usually ShipmentResponse or TrackingResponse
)

data class Warehouse(
    val id: String,
    val name: String,
    val code: String,
    val city: String,
    val country: String,
    val address: Address
)
