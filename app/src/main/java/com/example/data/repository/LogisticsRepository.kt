package com.example.data.repository

import android.util.Log
import com.example.data.local.LogisticsDao
import com.example.data.local.ShipmentEntity
import com.example.data.local.WebhookLogEntity
import com.example.data.local.WebhookSubscriptionEntity
import com.example.data.model.*
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.random.Random

class LogisticsRepository(private val dao: LogisticsDao) {

    val allShipments: Flow<List<ShipmentEntity>> = dao.getAllShipments()
    val webhookLogs: Flow<List<WebhookLogEntity>> = dao.getAllWebhookLogs()

    // Route Cache to store recently searched rate options
    private val rateCache = mutableMapOf<String, List<RateOption>>()

    // Pre-configured global Warehouses (Multi-warehouse shipping logic)
    val warehouses = listOf(
        Warehouse(
            id = "WH-LON-01",
            name = "London Gateway Hub",
            code = "LON-01",
            city = "London",
            country = "United Kingdom",
            address = Address("1 Logistics Way", "Port of London", "London", "Greater London", "RM18 7EH", "United Kingdom", true, "1 Logistics Way, London, RM18 7EH, UK", 51.5074, 0.1278)
        ),
        Warehouse(
            id = "WH-BER-02",
            name = "Berlin Central Depot",
            code = "BER-02",
            city = "Berlin",
            country = "Germany",
            address = Address("Zerbster Str. 12", "Schöneberg", "Berlin", "Berlin", "10827", "Germany", true, "Zerbster Str. 12, 10827 Berlin, Germany", 52.5200, 13.4050)
        ),
        Warehouse(
            id = "WH-NYC-03",
            name = "JFK Airport Fulfilment",
            code = "NYC-03",
            city = "New York",
            country = "United States",
            address = Address("150 Rockaway Blvd", "Queens", "New York", "NY", "11430", "United States", true, "150 Rockaway Blvd, New York, NY 11430, USA", 40.7128, -74.0060)
        ),
        Warehouse(
            id = "WH-TYO-04",
            name = "Tokyo Haneda Logistic Complex",
            code = "TYO-04",
            city = "Tokyo",
            country = "Japan",
            address = Address("2-1 Haneda Airport", "Ota City", "Tokyo", "Tokyo", "144-0041", "Japan", true, "2-1 Haneda Airport, Tokyo 144-0041, Japan", 35.6762, 139.6503)
        )
    )

    // 1. ADDRESS VALIDATION ENGINE
    fun validateAndNormalizeAddress(address: Address): Address {
        // Simple standardization rules matching different country validation specs
        val cleanedLine1 = address.line1.trim()
        val cleanedCity = address.city.trim()
        val cleanedZip = address.postalCode.trim().uppercase()
        val cleanedCountry = address.country.trim()

        val isZipValid = when (cleanedCountry.lowercase()) {
            "united kingdom", "uk" -> cleanedZip.matches(Regex("^[A-Z]{1,2}[0-9R][0-9A-Z]? [0-9][A-Z]{2}$")) || cleanedZip.replace(" ", "").length in 5..7
            "united states", "usa", "us" -> cleanedZip.matches(Regex("^\\d{5}(-\\d{4})?$"))
            "germany", "de" -> cleanedZip.matches(Regex("^\\d{5}$"))
            "japan", "jp" -> cleanedZip.matches(Regex("^\\d{3}-\\d{4}$")) || cleanedZip.matches(Regex("^\\d{7}$"))
            else -> cleanedZip.isNotEmpty()
        }

        val isValid = cleanedLine1.length >= 3 && cleanedCity.length >= 2 && isZipValid && cleanedCountry.isNotEmpty()

        // Generate geocodes based on postal code length to simulate realistic geolocation coordinates
        val (lat, lon) = if (isValid) {
            val randomOffset = (cleanedZip.hashCode() % 1000) / 1000.0
            val latBase = when (cleanedCountry.lowercase()) {
                "united kingdom", "uk" -> 51.5074
                "united states", "usa", "us" -> 40.7128
                "germany", "de" -> 52.5200
                "japan", "jp" -> 35.6762
                else -> 45.0
            }
            val lonBase = when (cleanedCountry.lowercase()) {
                "united kingdom", "uk" -> -0.1278
                "united states", "usa", "us" -> -74.0060
                "germany", "de" -> 13.4050
                "japan", "jp" -> 139.6503
                else -> 10.0
            }
            Pair(latBase + (randomOffset * 0.1), lonBase + (randomOffset * 0.1))
        } else {
            Pair(null, null)
        }

        // Standardized format string
        val stateStr = if (!address.state.isNullOrEmpty()) " ${address.state}" else ""
        val normalized = if (isValid) {
            "${cleanedLine1.capitalize()}, ${cleanedCity.capitalize()}$stateStr, $cleanedZip, ${cleanedCountry.uppercase()}"
        } else {
            "Invalid address format provided. Minimum fields not met."
        }

        return address.copy(
            line1 = cleanedLine1,
            city = cleanedCity,
            postalCode = cleanedZip,
            state = address.state?.trim(),
            isValid = isValid,
            normalizedAddress = normalized,
            latitude = lat,
            longitude = lon
        )
    }

    private fun String.capitalize(): String {
        return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
    }

    // 2. RATE COMPARISON ENGINE (CRITICAL)
    suspend fun compareRates(request: RateComparisonRequest): RateComparisonResponse = withContext(Dispatchers.Default) {
        val cacheKey = "${request.fromAddress.postalCode}-${request.toAddress.postalCode}-${request.parcel.weightKg}"
        
        // Caching verification
        if (rateCache.containsKey(cacheKey)) {
            val cachedOpts = rateCache[cacheKey]!!.map { it.copy(routeCacheHit = true) }
            return@withContext RateComparisonResponse(
                from = request.fromAddress.normalizedAddress ?: request.fromAddress.line1,
                to = request.toAddress.normalizedAddress ?: request.toAddress.line1,
                options = cachedOpts
            )
        }

        val weight = request.parcel.weightKg
        val isInternational = request.fromAddress.country.lowercase() != request.toAddress.country.lowercase()
        
        // Base distance factor
        val distanceFactor = if (isInternational) 5.0 else 1.2
        
        // Standardized rates generation for couriers
        val rawOptions = mutableListOf(
            // DHL Express adapter rate simulation
            RateOption(
                courier = "DHL",
                serviceName = "DHL Express Worldwide",
                price = (12.50 + (weight * 3.20)) * distanceFactor,
                etaDays = if (isInternational) 2 else 1,
                rating = 4.8
            ),
            // UPS Ground / priority adapter rate simulation
            RateOption(
                courier = "UPS",
                serviceName = if (isInternational) "UPS Worldwide Expedited" else "UPS Ground Saver",
                price = (8.20 + (weight * 2.10)) * distanceFactor,
                etaDays = if (isInternational) 4 else 3,
                rating = 4.4
            ),
            // FedEx prioritization adapter rate simulation
            RateOption(
                courier = "FedEx",
                serviceName = if (isInternational) "FedEx International Priority" else "FedEx Home Delivery",
                price = (10.10 + (weight * 2.80)) * distanceFactor,
                etaDays = if (isInternational) 3 else 2,
                rating = 4.6
            ),
            // Royal Mail / regional postal adapter rate simulation
            RateOption(
                courier = "Royal Mail",
                serviceName = if (isInternational) "International Tracked & Signed" else "1st Class Tracked",
                price = (4.50 + (weight * 1.50)) * distanceFactor,
                etaDays = if (isInternational) 7 else 2,
                rating = 4.2
            )
        )

        // Find cheapest, fastest, and recommended
        val cheapestPrice = rawOptions.minOf { it.price }
        val fastestEta = rawOptions.minOf { it.etaDays }

        // Weighted Recommended Scoring: lower price, lower eta, higher rating are better
        // Score = (100 / Price) * 40% + (20 / ETA) * 40% + (Rating * 4) * 20%
        val optionsWithBadges = rawOptions.map { opt ->
            val isCheapest = opt.price == cheapestPrice
            val isFastest = opt.etaDays == fastestEta
            
            val score = (50.0 / opt.price) * 0.4 + (2.0 / opt.etaDays) * 0.4 + (opt.rating / 5.0) * 0.2
            
            opt.copy(
                isCheapest = isCheapest,
                isFastest = isFastest,
                price = Math.round(opt.price * 100).toDouble() / 100.0,
                rating = score // Show computed recommendation score
            )
        }

        // Decorate Recommended badge (the highest score)
        val highestScore = optionsWithBadges.maxOf { it.rating }
        val finalOptions = optionsWithBadges.map { opt ->
            opt.copy(
                isRecommended = opt.rating == highestScore,
                rating = rawOptions.first { it.courier == opt.courier }.rating // Restore actual reliability rating
            )
        }

        rateCache[cacheKey] = finalOptions
        
        RateComparisonResponse(
            from = request.fromAddress.normalizedAddress ?: "${request.fromAddress.line1}, ${request.fromAddress.city}",
            to = request.toAddress.normalizedAddress ?: "${request.toAddress.line1}, ${request.toAddress.city}",
            options = finalOptions
        )
    }

    // 3. SHIPMENT CREATION (Unified booking manager)
    suspend fun createShipment(request: ShipmentCreateRequest, isReturn: Boolean = false, originalShipmentId: String? = null): ShipmentEntity = withContext(Dispatchers.IO) {
        val rand = Random.nextInt(100000, 999999)
        val courierCode = when (request.selectedCourier) {
            "DHL" -> "DH"
            "UPS" -> "UP"
            "FedEx" -> "FX"
            else -> "RM"
        }
        val trackingNumber = "ST-$courierCode-$rand"
        val shipmentId = "SH-${UUID.randomUUID().toString().take(8).uppercase()}"

        val shipment = ShipmentEntity(
            shipmentId = shipmentId,
            trackingNumber = trackingNumber,
            courier = request.selectedCourier,
            serviceName = request.serviceName,
            fromAddress = request.fromAddress,
            toAddress = request.toAddress,
            weightKg = request.parcel.weightKg,
            price = request.price,
            eta = "${request.price} via ${request.serviceName}",
            status = "CREATED",
            packageContent = request.packageContent,
            isReturn = isReturn,
            originalShipmentId = originalShipmentId,
            isInternational = request.isInternational,
            customsForm = request.customsForm
        )

        dao.insertShipment(shipment)
        
        // Dispatch "shipment.created" event simulation
        dispatchWebhookEvent(
            eventName = if (isReturn) "return.created" else "shipment.created",
            shipmentId = shipmentId,
            data = ShipmentResponse(
                shipmentId = shipmentId,
                trackingNumber = trackingNumber,
                courier = request.selectedCourier,
                status = "CREATED",
                labelUrl = "/api/v1/labels/$shipmentId",
                pickupScheduledTime = "Tomorrow, 10:00 AM",
                eta = if (request.selectedCourier == "DHL") "1-2 days" else "3-5 days"
            )
        )

        return@withContext shipment
    }

    // 4. SHIPMENT STATUS STATE MACHINE (TRACKING & WEBHOOK EVENT SIMULATION)
    suspend fun updateShipmentStatus(shipmentId: String, newStatus: String): Boolean = withContext(Dispatchers.IO) {
        val shipment = dao.getShipmentById(shipmentId) ?: return@withContext false
        val count = dao.updateShipmentStatus(shipmentId, newStatus)
        val success = count > 0

        if (success) {
            val eventName = when (newStatus) {
                "IN_TRANSIT" -> "shipment.dispatched"
                "DELIVERED" -> "shipment.delivered"
                "EXCEPTION" -> "shipment.failed"
                else -> "shipment.updated"
            }

            val trackInfo = TrackingResponse(
                trackingNumber = shipment.trackingNumber,
                courier = shipment.courier,
                currentStatus = newStatus,
                estimatedDelivery = "Expected in 2 days",
                events = generateTrackingEvents(newStatus, shipment.courier)
            )

            dispatchWebhookEvent(
                eventName = eventName,
                shipmentId = shipmentId,
                data = trackInfo
            )
        }
        return@withContext success
    }

    // 5. TRACKING EVENTS GENERATION
    fun generateTrackingEvents(status: String, courier: String): List<TrackingEvent> {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val cal = Calendar.getInstance()
        val events = mutableListOf<TrackingEvent>()

        cal.add(Calendar.HOUR, -12)
        events.add(TrackingEvent(format.format(cal.time), "CREATED", "Shipment Origin Depot", "Shipment details uploaded & label active."))

        if (status == "CREATED") return events

        cal.add(Calendar.HOUR, 4)
        events.add(TrackingEvent(format.format(cal.time), "IN_TRANSIT", "Central Outbound facility", "Processed through local sorting center with $courier."))

        if (status == "IN_TRANSIT") return events

        cal.add(Calendar.HOUR, 4)
        events.add(TrackingEvent(format.format(cal.time), "OUT_FOR_DELIVERY", "Destination Regional Hub", "Arrived at destination courier unit, loaded onto delivery vehicle."))

        if (status == "OUT_FOR_DELIVERY") return events

        if (status == "DELIVERED") {
            cal.add(Calendar.HOUR, 2)
            events.add(TrackingEvent(format.format(cal.time), "DELIVERED", "Success (Front Door)", "Signed by receiver. Handed over directly."))
        } else if (status == "EXCEPTION") {
            cal.add(Calendar.HOUR, 1)
            events.add(TrackingEvent(format.format(cal.time), "EXCEPTION", "Customs Inspection Gate 4", "Held due to missing commercial HS tax code classification. Merchant alerted."))
        }

        return events.asReversed()
    }

    // 6. WEBHOOKS MANAGER & SIMULATION DISPATCHER
    private suspend fun dispatchWebhookEvent(eventName: String, shipmentId: String, data: Any) {
        val subscriptions = dao.getAllSubscriptions()
        val adapter = Moshi.Builder().build().adapter(Any::class.java)
        
        val payloadMap = mapOf(
            "eventId" to "EVT-${UUID.randomUUID().toString().take(6).uppercase()}",
            "event" to eventName,
            "timestamp" to System.currentTimeMillis(),
            "data" to data
        )
        
        val payloadJson = try {
            adapter.toJson(payloadMap)
        } catch (e: Exception) {
            "{\"event\":\"$eventName\",\"shipmentId\":\"$shipmentId\"}"
        }

        if (subscriptions.isEmpty()) {
            // Save a simulation log to show in Webhook logs terminal even if no external listener
            val customLog = WebhookLogEntity(
                shipmentId = shipmentId,
                eventName = eventName,
                url = "https://your-merchant-server.com/webhooks",
                payload = payloadJson,
                responseCode = 200 // Mock success
            )
            dao.insertWebhookLog(customLog)
        } else {
            for (sub in subscriptions) {
                if (sub.events.contains(eventName) || sub.events.contains("*")) {
                    val customLog = WebhookLogEntity(
                        shipmentId = shipmentId,
                        eventName = eventName,
                        url = sub.url,
                        payload = payloadJson,
                        responseCode = 202 // Simulated Event Received
                    )
                    dao.insertWebhookLog(customLog)
                }
            }
        }
    }

    suspend fun registerWebhook(url: String, events: List<String>, token: String) = withContext(Dispatchers.IO) {
        val sub = WebhookSubscriptionEntity(url = url, secretToken = token, events = events)
        dao.insertSubscription(sub)
    }

    suspend fun deleteWebhook(url: String) = withContext(Dispatchers.IO) {
        dao.deleteSubscription(url)
    }

    suspend fun clearWebhookLogs() = withContext(Dispatchers.IO) {
        dao.clearWebhookLogs()
    }

    suspend fun getShipmentById(id: String): ShipmentEntity? = withContext(Dispatchers.IO) {
        dao.getShipmentById(id)
    }
}
