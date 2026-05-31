package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.ShipmentEntity
import com.example.data.local.WebhookLogEntity
import com.example.data.model.*
import com.example.data.repository.LogisticsRepository
import com.example.data.gemini.GeminiService
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class LogisticsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repo = LogisticsRepository(db.logisticsDao())

    val shipments: StateFlow<List<ShipmentEntity>> = repo.allShipments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val webhookLogs: StateFlow<List<WebhookLogEntity>> = repo.webhookLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val warehouses = repo.warehouses

    // Tab view selection
    val activeTab = MutableStateFlow(0) // 0: Rate compare, 1: Book Shipment, 2: Active Fleet, 3: Address Normalizer, 4: Webhook Terminal, 5: AI Logistics Advisor

    // Rate Comparison State
    val fromCity = MutableStateFlow("London")
    val fromCountry = MutableStateFlow("United Kingdom")
    val toCity = MutableStateFlow("Berlin")
    val toCountry = MutableStateFlow("Germany")
    val parcelWeight = MutableStateFlow("2.5")
    val selectedWarehouseId = MutableStateFlow("WH-LON-01")
    
    val rateComparisonResponse = MutableStateFlow<RateComparisonResponse?>(null)
    val rateLoading = MutableStateFlow(false)

    // Booking state
    val bookFromLine1 = MutableStateFlow("100 Piccadilly Street")
    val bookFromCity = MutableStateFlow("London")
    val bookFromPostcode = MutableStateFlow("W1J 7NH")
    val bookFromCountry = MutableStateFlow("United Kingdom")
    
    val bookToLine1 = MutableStateFlow("Klingelhöferstraße 7")
    val bookToCity = MutableStateFlow("Berlin")
    val bookToPostcode = MutableStateFlow("10785")
    val bookToCountry = MutableStateFlow("Germany")

    val bookWeight = MutableStateFlow("3.0")
    val bookPackageContent = MutableStateFlow("Premium Wireless Headset")
    val bookItemCategory = MutableStateFlow("Electronics")
    val bookSelectedCourier = MutableStateFlow("DHL")
    val bookSelectedService = MutableStateFlow("DHL Express Worldwide")
    val bookSelectedPrice = MutableStateFlow("28.40")

    val isInternational = MutableStateFlow(true)
    val hsCode = MutableStateFlow("8518.30.0000") // Headphones

    val bookSuccessMessage = MutableStateFlow<String?>(null)
    val bookLoading = MutableStateFlow(false)

    // Address verification sandbox fields
    val valLine1 = MutableStateFlow("1600 Amphitheatre Pkwy")
    val valCity = MutableStateFlow("Mountain View")
    val valPostcode = MutableStateFlow("94043")
    val valCountry = MutableStateFlow("United States")
    val validatedAddressResult = MutableStateFlow<Address?>(null)

    // Merchant Webhook Manager inputs
    val merchantWebhookUrl = MutableStateFlow("https://api.merchant-shop.com/v1/logistics-webhooks")
    val secretToken = MutableStateFlow("sk_ship_sec_9381k2m")

    // Active simulated Sandbox REST outputs
    // Displays actual API payloads to fulfill " unified logistics standard api developer friendly "
    val sandboxUrl = MutableStateFlow("POST /api/v1/rates/compare")
    val sandboxPayload = MutableStateFlow("")

    // Intelligent AI Logistics Advisor State
    val aiOrigin = MutableStateFlow("United Kingdom")
    val aiDestination = MutableStateFlow("Japan")
    val aiWeight = MutableStateFlow("8.5")
    val aiCategory = MutableStateFlow("Precision Medical Sensors")
    val aiAdvice = MutableStateFlow("")
    val aiAdviceLoading = MutableStateFlow(false)

    init {
        // Build initial pre-seeded data so that the simulator dashboard looks fully fleshed on start!
        viewModelScope.launch {
            repo.registerWebhook(
                merchantWebhookUrl.value,
                listOf("shipment.created", "shipment.dispatched", "shipment.delivered", "shipment.failed"),
                secretToken.value
            )
            
            // Check if shipments is empty on start, and seed 2 shipments if so
            try {
                // To fetch synchronously once
                val currentList = db.logisticsDao().getAllSubscriptions() // dummy queries or fetch
                val countCheck = db.logisticsDao().getShipmentById("SH-SEED01")
                if (countCheck == null) {
                    // Seed some shipments
                    seedInitialData()
                }
            } catch (e: Exception) {
                Log.e("LogisticsViewModel", "Failed to seed or fetch subscriptions: ${e.message}")
            }
        }
    }

    private suspend fun seedInitialData() {
        val seed1 = ShipmentCreateRequest(
            fromAddress = Address("1 Logistics Way", "Port of London", "London", "Greater London", "RM18 7EH", "United Kingdom", true, "1 Logistics Way, London, RM18 7EH, UK", 51.5074, 0.1278),
            toAddress = Address("456 Park Avenue", "Manhattan", "New York", "NY", "10022", "United States", true, "456 Park Avenue, New York, NY 10022, USA", 40.7128, -74.006),
            parcel = Parcel(30.0, 20.0, 15.0, 4.2),
            selectedCourier = "FedEx",
            serviceName = "FedEx International Priority",
            price = 42.50,
            packageContent = "Organic Cotton Garments (V1-Shirt Batch)",
            isInternational = true,
            customsForm = CustomsForm("Organic T-Shirts", "6109.10", 0.05, "I certify this is a true declaration of garments content value.")
        )
        val seed2 = ShipmentCreateRequest(
            fromAddress = Address("Zerbster Str. 12", "Schöneberg", "Berlin", "Berlin", "10827", "Germany", true, "Zerbster Str. 12, 10827 Berlin, Germany", 52.520, 13.405),
            toAddress = Address("12 Rue de Rivoli", "Louvre", "Paris", "Île-de-France", "75001", "France", true, "12 Rue de Rivoli, Paris 75001, France", 48.8566, 2.3522),
            parcel = Parcel(15.0, 10.0, 5.0, 1.1),
            selectedCourier = "UPS",
            serviceName = "UPS Ground Saver",
            price = 14.20,
            packageContent = "Rechargeable Lithium Battery Accessory packs",
            isInternational = true,
            customsForm = CustomsForm("Batteries Accessories", "8507.60", 0.12, "Declared goods conform to aviation transport regulations.")
        )

        val s1 = repo.createShipment(seed1)
        val s2 = repo.createShipment(seed2)

        // Simulate some transport logs in background
        repo.updateShipmentStatus(s1.shipmentId, "IN_TRANSIT")
    }

    // Interactive Rate lookup trigger
    fun findRates() {
        viewModelScope.launch {
            rateLoading.value = true
            val fromAddress = Address("", null, fromCity.value, null, "", fromCountry.value)
            val toAddress = Address("", null, toCity.value, null, "", toCountry.value)
            val weight = parcelWeight.value.toDoubleOrNull() ?: 1.0
            
            val request = RateComparisonRequest(
                fromAddress = fromAddress,
                toAddress = toAddress,
                parcel = Parcel(25.0, 20.0, 15.0, weight),
                selectedWarehouseId = selectedWarehouseId.value
            )

            // Update developer sandbox request output representation
            sandboxUrl.value = "POST /api/v1/rates/compare"
            val moshi = Moshi.Builder().build()
            val requestAdapter = moshi.adapter(RateComparisonRequest::class.java)
            val responseAdapter = moshi.adapter(RateComparisonResponse::class.java)
            
            sandboxPayload.value = "REQUEST:\n" + requestAdapter.indent("  ").toJson(request)

            val result = repo.compareRates(request)
            rateComparisonResponse.value = result
            
            sandboxPayload.value += "\n\nRESPONSE:\n" + responseAdapter.indent("  ").toJson(result)
            rateLoading.value = false
        }
    }

    // Interactive Booking trigger
    fun bookShipment() {
        viewModelScope.launch {
            bookLoading.value = true
            val isIntl = isInternational.value
            val customs = if (isIntl) {
                CustomsForm(
                    contentDescription = bookPackageContent.value,
                    hsCode = hsCode.value,
                    tarrifTax = 0.08,
                    declarationStatement = "Standard cross-border classification declaration filed under electronic automated logistics system."
                )
            } else null

            val weight = bookWeight.value.toDoubleOrNull() ?: 2.0
            val price = bookSelectedPrice.value.toDoubleOrNull() ?: 20.0

            val fromAddr = Address(bookFromLine1.value, null, bookFromCity.value, null, bookFromPostcode.value, bookFromCountry.value)
            val toAddr = Address(bookToLine1.value, null, bookToCity.value, null, bookToPostcode.value, bookToCountry.value)

            val request = ShipmentCreateRequest(
                fromAddress = fromAddr,
                toAddress = toAddr,
                parcel = Parcel(30.0, 20.0, 20.0, weight),
                selectedCourier = bookSelectedCourier.value,
                serviceName = bookSelectedService.value,
                price = price,
                packageContent = bookPackageContent.value,
                isInternational = isIntl,
                customsForm = customs
            )

            sandboxUrl.value = "POST /api/v1/shipments/create"
            val moshi = Moshi.Builder().build()
            val requestAdapter = moshi.adapter(ShipmentCreateRequest::class.java)
            
            sandboxPayload.value = "REQUEST:\n" + requestAdapter.indent("  ").toJson(request)

            val shipment = repo.createShipment(request)
            
            val responseObj = ShipmentResponse(
                shipmentId = shipment.shipmentId,
                trackingNumber = shipment.trackingNumber,
                courier = shipment.courier,
                status = "CREATED",
                labelUrl = "/api/v1/labels/${shipment.shipmentId}",
                pickupScheduledTime = "Tomorrow, 10:00 AM",
                eta = "Calculated ${shipment.courier} Route ETA"
            )

            val responseAdapter = moshi.adapter(ShipmentResponse::class.java)
            sandboxPayload.value += "\n\nRESPONSE:\n" + responseAdapter.indent("  ").toJson(responseObj)

            bookSuccessMessage.value = "Shipment booked! Tracking Number: ${shipment.trackingNumber}"
            bookLoading.value = false
        }
    }

    // Address verification runner
    fun runAddressValidation() {
        val rawInput = Address(
            line1 = valLine1.value,
            line2 = null,
            city = valCity.value,
            state = null,
            postalCode = valPostcode.value,
            country = valCountry.value
        )
        val normalized = repo.validateAndNormalizeAddress(rawInput)
        validatedAddressResult.value = normalized

        sandboxUrl.value = "POST /api/v1/address/validate"
        val moshi = Moshi.Builder().build()
        val adapter = moshi.adapter(Address::class.java)
        sandboxPayload.value = "REQUEST:\n${adapter.indent("  ").toJson(rawInput)}\n\nRESPONSE:\n${adapter.indent("  ").toJson(normalized)}"
    }

    // Webhook subscription updates
    fun registerWebhookSubscription() {
        viewModelScope.launch {
            repo.registerWebhook(
                merchantWebhookUrl.value,
                listOf("shipment.created", "shipment.dispatched", "shipment.delivered", "shipment.failed", "return.created"),
                secretToken.value
            )
            sandboxUrl.value = "POST /api/v1/webhooks/register"
            val regObj = WebhookRegistration(
                url = merchantWebhookUrl.value,
                events = listOf("shipment.*", "return.created"),
                secretToken = secretToken.value
            )
            val moshi = Moshi.Builder().build()
            val adapter = moshi.adapter(WebhookRegistration::class.java)
            sandboxPayload.value = "REQUEST:\n${adapter.indent("  ").toJson(regObj)}\n\nRESPONSE:\n{\n  \"status\": \"registered\",\n  \"clientId\": \"cli_merchant_9281a\",\n  \"eventsCount\": 5\n}"
        }
    }

    // Trigger state changes in simulator
    fun triggerStatusTransition(shipmentId: String, currentStatus: String) {
        viewModelScope.launch {
            val nextStatus = when (currentStatus) {
                "CREATED" -> "IN_TRANSIT"
                "IN_TRANSIT" -> "OUT_FOR_DELIVERY"
                "OUT_FOR_DELIVERY" -> "DELIVERED"
                "DELIVERED" -> "EXCEPTION"
                else -> "CREATED"
            }
            repo.updateShipmentStatus(shipmentId, nextStatus)
        }
    }

    // Returns Management engine returns workflow
    fun initiateReturn(originalShipment: ShipmentEntity) {
        viewModelScope.launch {
            val fromAddr = originalShipment.toAddress // Reversed
            val toAddr = originalShipment.fromAddress // Reversed

            val request = ShipmentCreateRequest(
                fromAddress = fromAddr,
                toAddress = toAddr,
                parcel = Parcel(30.0, 20.0, 20.0, originalShipment.weightKg),
                selectedCourier = originalShipment.courier,
                serviceName = "${originalShipment.courier} Return Saver",
                price = 12.00, // Return flat rate
                packageContent = "RETURN: ${originalShipment.packageContent}",
                isInternational = originalShipment.isInternational,
                customsForm = originalShipment.customsForm
            )

            repo.createShipment(request, isReturn = true, originalShipmentId = originalShipment.shipmentId)
            bookSuccessMessage.value = "Reverse logistics return label queued successfully for ${originalShipment.trackingNumber}!"
        }
    }

    // Clear Webhook Log database stream
    fun clearLogs() {
        viewModelScope.launch {
            repo.clearWebhookLogs()
        }
    }

    // Intelligent AI Advisor route computation
    fun runSmartRouting() {
        viewModelScope.launch {
            aiAdviceLoading.value = true
            val weight = aiWeight.value.toDoubleOrNull() ?: 5.0
            val advice = GeminiService.getLogisticsRouteAdvice(
                fromCountry = aiOrigin.value,
                toCountry = aiDestination.value,
                weightKg = weight,
                itemCategory = aiCategory.value
            )
            aiAdvice.value = advice
            aiAdviceLoading.value = false
        }
    }
}

class LogisticsViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LogisticsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LogisticsViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
