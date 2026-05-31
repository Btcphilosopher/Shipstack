package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.ShipmentEntity
import com.example.data.local.WebhookLogEntity
import com.example.data.model.Address
import com.example.data.model.RateOption
import com.example.ui.LogisticsViewModel
import com.example.ui.LogisticsViewModelFactory
import com.example.ui.components.ShippingLabelView
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
    private val viewModel: LogisticsViewModel by viewModels {
        LogisticsViewModelFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme(darkTheme = true) { // Enforce luxurious terminal dark styling
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBg
                ) {
                    MainLogisticsConsole(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun MainLogisticsConsole(viewModel: LogisticsViewModel) {
    val activeTabState by viewModel.activeTab.collectAsStateWithLifecycle()
    val shipmentsList by viewModel.shipments.collectAsStateWithLifecycle()
    val webhookLogsList by viewModel.webhookLogs.collectAsStateWithLifecycle()
    
    val context = LocalContext.current
    var showSandboxPane by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBg,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkBg)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(CyanNeon, IndigoNeon)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Logo",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "SHIPSTACK / CORE",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = TextLight,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "MULTI-CARRIER LOGISTICS GATEWAY",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Live Status Pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(CardDark)
                            .border(1.dp, CardBorder, RoundedCornerShape(100.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(100.dp))
                                .background(EmeraldNeon)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "LIVE REGISTRY",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldNeon,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Divider(color = CardBorder, thickness = 1.dp)
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkBg)
                    .navigationBarsPadding()
            ) {
                Divider(color = CardBorder, thickness = 1.dp)
                
                // M3 styled navigation bar mapping standard adapter modules
                NavigationBar(
                    containerColor = DarkBg,
                    tonalElevation = 0.dp,
                    modifier = Modifier.height(68.dp)
                ) {
                    val tabs = listOf(
                        Triple(0, "Rates", Icons.Default.Search),
                        Triple(1, "Book", Icons.Default.Add),
                        Triple(2, "Fleet", Icons.Default.List),
                        Triple(3, "Optimise", Icons.Default.LocationOn),
                        Triple(4, "Webhooks", Icons.Default.Share),
                        Triple(5, "AI Advisor", Icons.Default.Info)
                    )

                    tabs.forEach { (tabIndex, label, icon) ->
                        NavigationBarItem(
                            selected = activeTabState == tabIndex,
                            onClick = { viewModel.activeTab.value = tabIndex },
                            icon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = DarkBg,
                                selectedTextColor = CyanNeon,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted,
                                indicatorColor = CyanNeon
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        // Main Screen Body with floating toggle to launch developer sandbox API pane
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // Main Tabbed Panel Switching Animations
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (activeTabState) {
                        0 -> RateCompareModule(viewModel)
                        1 -> BookShipmentModule(viewModel)
                        2 -> ActiveFleetModule(viewModel, shipmentsList)
                        3 -> AddressOptimizerModule(viewModel)
                        4 -> WebhookTerminalModule(viewModel, webhookLogsList)
                        5 -> AiLogisticsAdvisorModule(viewModel)
                    }
                }

                // Sandbox Toggle Footer
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clickable { showSandboxPane = !showSandboxPane },
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    border = BorderStroke(1.dp, if (showSandboxPane) CyanNeon else CardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(10.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🛡️", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "LIVE DEVELOPER API SANDBOX",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextLight,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = if (showSandboxPane) "Click to collapse JSON sandbox terminal" else "Click to inspect active REST payload schemas",
                                    fontSize = 8.sp,
                                    color = TextMuted,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        Text(
                            text = if (showSandboxPane) "COLLAPSE CLT ▲" else "OPEN CLT ▼",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = CyanNeon,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                AnimatedVisibility(
                    visible = showSandboxPane,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    DeveloperSandboxPane(viewModel)
                }
            }
        }
    }
}

// 1. RATE COMPARISON MODULE
@Composable
fun RateCompareModule(viewModel: LogisticsViewModel) {
    val fromCity by viewModel.fromCity.collectAsStateWithLifecycle()
    val fromCountry by viewModel.fromCountry.collectAsStateWithLifecycle()
    val toCity by viewModel.toCity.collectAsStateWithLifecycle()
    val toCountry by viewModel.toCountry.collectAsStateWithLifecycle()
    val weight by viewModel.parcelWeight.collectAsStateWithLifecycle()
    val warehouseId by viewModel.selectedWarehouseId.collectAsStateWithLifecycle()
    
    val rateResponse by viewModel.rateComparisonResponse.collectAsStateWithLifecycle()
    val isLoading by viewModel.rateLoading.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            Text(
                text = "🧪 Rate Orchestrator Engine",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextLight
            )
            Text(
                text = "Simulate real-time multi-courier pricing routing, optimized by shipping weight, carbon factors, and dispatch warehouse hubs.",
                fontSize = 11.sp,
                color = TextMuted,
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardDark),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "DISPATCH REGULATION ATTR",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanNeon,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Warehouses configuration selector
                    Text(text = "Outbound Warehouse Terminal", fontSize = 10.sp, color = TextLight)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        viewModel.warehouses.forEach { wh ->
                            val selected = warehouseId == wh.id
                            FilterChip(
                                selected = selected,
                                onClick = { 
                                    viewModel.selectedWarehouseId.value = wh.id 
                                    viewModel.fromCity.value = wh.city
                                    viewModel.fromCountry.value = wh.country
                                },
                                label = { Text(wh.name, fontSize = 9.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyanNeon,
                                    selectedLabelColor = DarkBg,
                                    containerColor = CardDark,
                                    labelColor = TextMuted
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1.5f)) {
                            Text(text = "Deliver To City", fontSize = 10.sp, color = TextLight)
                            TextField(
                                value = toCity,
                                onValueChange = { viewModel.toCity.value = it },
                                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = TextFieldDefaults.colors(focusedContainerColor = DarkBg, unfocusedContainerColor = DarkBg),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1.5f)) {
                            Text(text = "Deliver To Country", fontSize = 10.sp, color = TextLight)
                            TextField(
                                value = toCountry,
                                onValueChange = { viewModel.toCountry.value = it },
                                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = TextFieldDefaults.colors(focusedContainerColor = DarkBg, unfocusedContainerColor = DarkBg),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Weight (KG)", fontSize = 10.sp, color = TextLight)
                            TextField(
                                value = weight,
                                onValueChange = { viewModel.parcelWeight.value = it },
                                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = TextFieldDefaults.colors(focusedContainerColor = DarkBg, unfocusedContainerColor = DarkBg),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { viewModel.findRates() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = DarkBg)
                        } else {
                            Text("Query Shipping Rate Quotations", color = DarkBg, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Output rate list section
        rateResponse?.let { resp ->
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AVAILABLE PARCEL CARRIERS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextLight,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "ROUTING GATEWAY STABLE",
                        fontSize = 8.sp,
                        color = EmeraldNeon,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            items(resp.options) { opt ->
                CarrierRateCard(opt, selectAction = {
                    viewModel.bookSelectedCourier.value = opt.courier
                    viewModel.bookSelectedService.value = opt.serviceName
                    viewModel.bookSelectedPrice.value = opt.price.toString()
                    viewModel.bookToCity.value = toCity
                    viewModel.bookToCountry.value = toCountry
                    viewModel.bookWeight.value = weight
                    viewModel.activeTab.value = 1 // jump to book
                })
            }
        } ?: item {
            // Empy cache guide
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "📦", fontSize = 32.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Enter query fields above and click run to compare DHL, UPS, FedEx and regional carriers instantly.",
                        fontSize = 11.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CarrierRateCard(opt: RateOption, selectAction: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        border = BorderStroke(1.dp, if (opt.isRecommended) IndigoNeon else CardBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (opt.courier == "DHL") "🔴" else if (opt.courier == "UPS") "🟤" else if (opt.courier == "FedEx") "🟣" else "🔵",
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = opt.courier,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextLight
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            if (opt.routeCacheHit) {
                                Text(
                                    text = "⚡ CACHE HIT",
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyanNeon,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        Text(
                            text = opt.serviceName,
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "£${String.format("%.2f", opt.price)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = TextLight,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "ETA: ${opt.etaDays} days",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Rating meter and Badges indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (opt.isCheapest) {
                        BadgeTag("CHEAPEST", EmeraldNeon)
                    }
                    if (opt.isFastest) {
                        BadgeTag("FASTEST", CyanNeon)
                    }
                    if (opt.isRecommended) {
                        BadgeTag("RECOMMENDED", IndigoNeon)
                    }
                }

                Button(
                    onClick = selectAction,
                    colors = ButtonDefaults.buttonColors(containerColor = CardBorder),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(text = "Select Route", fontSize = 10.sp, color = TextLight)
                }
            }
        }
    }
}

@Composable
fun BadgeTag(text: String, col: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(col.copy(alpha = 0.15f))
            .border(0.5.dp, col, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text = text, fontSize = 7.sp, fontWeight = FontWeight.Black, color = col, fontFamily = FontFamily.Monospace)
    }
}

// 2. BOOK SHIPMENT MODULE
@Composable
fun BookShipmentModule(viewModel: LogisticsViewModel) {
    val bookFromLine1 by viewModel.bookFromLine1.collectAsStateWithLifecycle()
    val bookFromCity by viewModel.bookFromCity.collectAsStateWithLifecycle()
    val bookFromPostcode by viewModel.bookFromPostcode.collectAsStateWithLifecycle()
    val bookFromCountry by viewModel.bookFromCountry.collectAsStateWithLifecycle()
    
    val bookToLine1 by viewModel.bookToLine1.collectAsStateWithLifecycle()
    val bookToCity by viewModel.bookToCity.collectAsStateWithLifecycle()
    val bookToPostcode by viewModel.bookToPostcode.collectAsStateWithLifecycle()
    val bookToCountry by viewModel.bookToCountry.collectAsStateWithLifecycle()

    val bookWeight by viewModel.bookWeight.collectAsStateWithLifecycle()
    val bookPackageContent by viewModel.bookPackageContent.collectAsStateWithLifecycle()
    val bookItemCategory by viewModel.bookItemCategory.collectAsStateWithLifecycle()
    val bookSelectedCourier by viewModel.bookSelectedCourier.collectAsStateWithLifecycle()
    val bookSelectedService by viewModel.bookSelectedService.collectAsStateWithLifecycle()
    val bookSelectedPrice by viewModel.bookSelectedPrice.collectAsStateWithLifecycle()

    val isIntl by viewModel.isInternational.collectAsStateWithLifecycle()
    val hs by viewModel.hsCode.collectAsStateWithLifecycle()
    val successMsg by viewModel.bookSuccessMessage.collectAsStateWithLifecycle()
    val isLoading by viewModel.bookLoading.collectAsStateWithLifecycle()

    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            Text(
                text = "📦 Core Booking & Label Creator",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextLight
            )
            Text(
                text = "Draft outbound and inbound shipment routes, calculate international declarations, and register standard barcode labels into the fleet registry database.",
                fontSize = 11.sp,
                color = TextMuted,
                modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
            )
        }

        if (successMsg != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = EmeraldNeon.copy(alpha = 0.15f)),
                    border = BorderStroke(1.dp, EmeraldNeon)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "FULFILLMENT PROCESSED SUCCESSFULLY",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldNeon,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = successMsg!!,
                            fontSize = 11.sp,
                            color = TextLight
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.bookSuccessMessage.value = null },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldNeon),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Book Another", fontSize = 10.sp, color = DarkBg)
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardDark),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "1. ROUTING SENSORS (ORIGIN & DESTINATION)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanNeon,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(text = "Origin Line 1", fontSize = 9.sp, color = TextMuted)
                    TextField(
                        value = bookFromLine1,
                        onValueChange = { viewModel.bookFromLine1.value = it },
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = TextFieldDefaults.colors(focusedContainerColor = DarkBg, unfocusedContainerColor = DarkBg)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "City", fontSize = 9.sp, color = TextMuted)
                            TextField(
                                value = bookFromCity,
                                onValueChange = { viewModel.bookFromCity.value = it },
                                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = TextFieldDefaults.colors(focusedContainerColor = DarkBg, unfocusedContainerColor = DarkBg)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Postcode", fontSize = 9.sp, color = TextMuted)
                            TextField(
                                value = bookFromPostcode,
                                onValueChange = { viewModel.bookFromPostcode.value = it },
                                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = TextFieldDefaults.colors(focusedContainerColor = DarkBg, unfocusedContainerColor = DarkBg)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(text = "Destination Line 1", fontSize = 9.sp, color = TextMuted)
                    TextField(
                        value = bookToLine1,
                        onValueChange = { viewModel.bookToLine1.value = it },
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = TextFieldDefaults.colors(focusedContainerColor = DarkBg, unfocusedContainerColor = DarkBg)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "City", fontSize = 9.sp, color = TextMuted)
                            TextField(
                                value = bookToCity,
                                onValueChange = { viewModel.bookToCity.value = it },
                                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = TextFieldDefaults.colors(focusedContainerColor = DarkBg, unfocusedContainerColor = DarkBg)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Postcode", fontSize = 9.sp, color = TextMuted)
                            TextField(
                                value = bookToPostcode,
                                onValueChange = { viewModel.bookToPostcode.value = it },
                                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = TextFieldDefaults.colors(focusedContainerColor = DarkBg, unfocusedContainerColor = DarkBg)
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardDark),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "2. PACKAGE DECLARATION & SELECT CARRIER",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanNeon,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(text = "Package Contents Description", fontSize = 9.sp, color = TextMuted)
                    TextField(
                        value = bookPackageContent,
                        onValueChange = { viewModel.bookPackageContent.value = it },
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = TextFieldDefaults.colors(focusedContainerColor = DarkBg, unfocusedContainerColor = DarkBg)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isIntl,
                            onCheckedChange = { viewModel.isInternational.value = it },
                            colors = CheckboxDefaults.colors(checkedColor = CyanNeon)
                        )
                        Column {
                            Text("Cross-border Outbound (International)", fontSize = 11.sp, color = TextLight)
                            Text("Requires Customs declaration HS codes for compliance", fontSize = 9.sp, color = TextMuted)
                        }
                    }

                    if (isIntl) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "HS Classification Tariff Code", fontSize = 9.sp, color = TextMuted)
                        TextField(
                            value = hs,
                            onValueChange = { viewModel.hsCode.value = it },
                            textStyle = LocalTextStyle.current.copy(fontSize = 11.sp),
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = TextFieldDefaults.colors(focusedContainerColor = DarkBg, unfocusedContainerColor = DarkBg)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "Active Carrier Adapter Mode", fontSize = 10.sp, color = TextLight)
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("DHL", "UPS", "FedEx", "Royal Mail").forEach { courier ->
                            val selected = bookSelectedCourier == courier
                            Button(
                                onClick = { 
                                    viewModel.bookSelectedCourier.value = courier
                                    viewModel.bookSelectedService.value = when(courier) {
                                        "DHL" -> "DHL Express Worldwide"
                                        "UPS" -> "UPS Worldwide Premium"
                                        "FedEx" -> "FedEx Priority Freight"
                                        else -> "Royal Mail Tracked 24"
                                    }
                                    viewModel.bookSelectedPrice.value = when(courier) {
                                        "DHL" -> "28.40"
                                        "UPS" -> "19.50"
                                        "FedEx" -> "24.10"
                                        else -> "12.80"
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selected) CyanNeon else CardBorder
                                ),
                                contentPadding = PaddingValues(horizontal = 4.dp),
                                modifier = Modifier.weight(1f).height(32.dp),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(courier, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (selected) DarkBg else TextMuted)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Service: $bookSelectedService", fontSize = 10.sp, color = TextLight, fontWeight = FontWeight.Bold)
                            Text("Standard Rate Quote: £$bookSelectedPrice", fontSize = 9.sp, color = TextMuted)
                        }

                        Button(
                            onClick = { viewModel.bookShipment() },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
                            shape = RoundedCornerShape(8.dp),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = DarkBg)
                            } else {
                                Text("Generate Label & Dispatch", color = DarkBg, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// 3. ACTIVE FLEET MODULE - SHIPMENT LIFECYCLE TRACKER
@Composable
fun ActiveFleetModule(viewModel: LogisticsViewModel, shipments: List<ShipmentEntity>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            Text(
                text = "🚚 Warehouse Dispatch & Fleet Terminal",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextLight
            )
            Text(
                text = "Track active dispatches. Click 'CYCLE STATE' on shipment cards to simulate real-time status transitions. This registers automated events in standard webhook callbacks.",
                fontSize = 11.sp,
                color = TextMuted,
                modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
            )
        }

        if (shipments.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No active dispatches stored in SQL db. Generate some in 'Book' or 'Rates' compare tabs!", color = TextMuted, fontSize = 11.sp, textAlign = TextAlign.Center)
                }
            }
        }

        items(shipments) { shipment ->
            FleetShipmentCard(shipment, viewModel)
        }
    }
}

@Composable
fun FleetShipmentCard(shipment: ShipmentEntity, viewModel: LogisticsViewModel) {
    var expandedLabel by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        border = BorderStroke(1.dp, if (shipment.status == "EXCEPTION") RoseNeon else CardBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(CardBorder),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = if (shipment.isReturn) "↩️" else "📦", fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = shipment.trackingNumber,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextLight,
                                fontFamily = FontFamily.Monospace
                            )
                            if (shipment.isReturn) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "RETURN",
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Black,
                                    color = AmberNeon,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier
                                        .background(AmberNeon.copy(alpha = 0.15f))
                                        .border(0.5.dp, AmberNeon, RoundedCornerShape(2.dp))
                                        .padding(horizontal = 4.dp)
                                )
                            }
                        }
                        Text(
                            text = "${shipment.courier} • ${shipment.serviceName}",
                            fontSize = 9.sp,
                            color = TextMuted
                        )
                    }
                }

                // Styled Status Badge
                val statusColor = when (shipment.status) {
                    "CREATED" -> IndigoNeon
                    "IN_TRANSIT" -> CyanNeon
                    "OUT_FOR_DELIVERY" -> AmberNeon
                    "DELIVERED" -> EmeraldNeon
                    else -> RoseNeon
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = shipment.status,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Routing details
            Text(
                text = "📍 Route: ${shipment.fromAddress.city} (${shipment.fromAddress.country.take(3).uppercase()}) ➔ ${shipment.toAddress.city} (${shipment.toAddress.country.take(3).uppercase()})",
                fontSize = 11.sp,
                color = TextLight
            )
            Text(
                text = "Contents: ${shipment.packageContent} | Declared value: £${shipment.price}",
                fontSize = 10.sp,
                color = TextMuted,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Control Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Expanding thermal Label
                Button(
                    onClick = { expandedLabel = !expandedLabel },
                    colors = ButtonDefaults.buttonColors(containerColor = CardBorder),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier.height(28.dp),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (expandedLabel) "Hide Print Label" else "Render Label Preview",
                        fontSize = 9.sp,
                        color = TextLight
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Reverse Logistics return button
                    if (!shipment.isReturn && shipment.status == "DELIVERED") {
                        Button(
                            onClick = { viewModel.initiateReturn(shipment) },
                            colors = ButtonDefaults.buttonColors(containerColor = AmberNeon.copy(alpha = 0.2f)),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            modifier = Modifier
                                .height(28.dp)
                                .border(0.5.dp, AmberNeon, RoundedCornerShape(4.dp)),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text("Launch Return ↩️", fontSize = 9.sp, color = AmberNeon, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Status simulation loop trigger
                    Button(
                        onClick = { viewModel.triggerStatusTransition(shipment.shipmentId, shipment.status) },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier.height(28.dp),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("Cycle State ⚙️", fontSize = 9.sp, color = DarkBg, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Expandable Printable Shipping label component
            AnimatedVisibility(
                visible = expandedLabel,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Text(
                        text = "SIMULATED THERMAL SHIPPING LABEL (PNG/ZPL)",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    ShippingLabelView(shipment = shipment)
                }
            }
        }
    }
}

// 4. ADDRESS OPTIMIZER MODULE
@Composable
fun AddressOptimizerModule(viewModel: LogisticsViewModel) {
    val line1 by viewModel.valLine1.collectAsStateWithLifecycle()
    val city by viewModel.valCity.collectAsStateWithLifecycle()
    val postcode by viewModel.valPostcode.collectAsStateWithLifecycle()
    val country by viewModel.valCountry.collectAsStateWithLifecycle()
    val validatedResult by viewModel.validatedAddressResult.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            Text(
                text = "📌 Address Validation & Geocoder",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextLight
            )
            Text(
                text = "Standardise delivery formatting, correct zipcodes, normalise country codes, and resolve optional lat/long coordinates dynamically.",
                fontSize = 11.sp,
                color = TextMuted,
                modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardDark),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "LOCAL POSTAL PROTOCOL SCHEMAS",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanNeon,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(text = "Street Address Line", fontSize = 9.sp, color = TextMuted)
                    TextField(
                        value = line1,
                        onValueChange = { viewModel.valLine1.value = it },
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = TextFieldDefaults.colors(focusedContainerColor = DarkBg, unfocusedContainerColor = DarkBg)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1.5f)) {
                            Text(text = "City", fontSize = 9.sp, color = TextMuted)
                            TextField(
                                value = city,
                                onValueChange = { viewModel.valCity.value = it },
                                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = TextFieldDefaults.colors(focusedContainerColor = DarkBg, unfocusedContainerColor = DarkBg)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Postal Code", fontSize = 9.sp, color = TextMuted)
                            TextField(
                                value = postcode,
                                onValueChange = { viewModel.valPostcode.value = it },
                                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = TextFieldDefaults.colors(focusedContainerColor = DarkBg, unfocusedContainerColor = DarkBg)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(text = "Country ISO Standard Name", fontSize = 9.sp, color = TextMuted)
                    TextField(
                        value = country,
                        onValueChange = { viewModel.valCountry.value = it },
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = TextFieldDefaults.colors(focusedContainerColor = DarkBg, unfocusedContainerColor = DarkBg)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.runAddressValidation() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Verify & Normalize Format", color = DarkBg, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        validatedResult?.let { addr ->
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (addr.isValid) EmeraldNeon.copy(alpha = 0.1f) else RoseNeon.copy(alpha = 0.1f)),
                    border = BorderStroke(1.dp, if (addr.isValid) EmeraldNeon else RoseNeon)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = if (addr.isValid) "✅" else "❌", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (addr.isValid) "ADDRESS STANDARDISED & STABLE" else "ADDRESS VALIDATION REPORT [FAILED]",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (addr.isValid) EmeraldNeon else RoseNeon,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = addr.normalizedAddress ?: "Validation error. Postal code schema mismatch.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextLight
                        )

                        if (addr.latitude != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Resolved coordinates: Lat: ${addr.latitude}, Lon: ${addr.longitude}",
                                fontSize = 9.sp,
                                color = TextMuted,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

// 5. WEBHOOK TERMINAL MODULE
@Composable
fun WebhookTerminalModule(viewModel: LogisticsViewModel, logs: List<WebhookLogEntity>) {
    val merchantUrl by viewModel.merchantWebhookUrl.collectAsStateWithLifecycle()
    val token by viewModel.secretToken.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            Text(
                text = "📡 Webhook event dispatcher",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextLight
            )
            Text(
                text = "Configure merchant webhooks to push real-time status updates (`shipment.created`, `shipment.dispatched`, `shipment.delivered`) via automated event streaming.",
                fontSize = 11.sp,
                color = TextMuted,
                modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardDark),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "MERCHANT TARGET ENDPOINT URL",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanNeon,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(text = "Webhook URL Endpoint", fontSize = 9.sp, color = TextMuted)
                    TextField(
                        value = merchantUrl,
                        onValueChange = { viewModel.merchantWebhookUrl.value = it },
                        textStyle = LocalTextStyle.current.copy(fontSize = 11.sp),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = TextFieldDefaults.colors(focusedContainerColor = DarkBg, unfocusedContainerColor = DarkBg)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(text = "Secret HMAC Signing Key", fontSize = 9.sp, color = TextMuted)
                    TextField(
                        value = token,
                        onValueChange = { viewModel.secretToken.value = it },
                        textStyle = LocalTextStyle.current.copy(fontSize = 11.sp),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = TextFieldDefaults.colors(focusedContainerColor = DarkBg, unfocusedContainerColor = DarkBg),
                        trailingIcon = { Text("HMAC-256", fontSize = 8.sp, color = CyanNeon, modifier = Modifier.padding(end = 8.dp)) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.registerWebhookSubscription() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Register Subscriber", color = DarkBg, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        
                        OutlinedButton(
                            onClick = { viewModel.clearLogs() },
                            modifier = Modifier.weight(0.8f),
                            border = BorderStroke(1.dp, RoseNeon),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Clear Event logs", color = RoseNeon, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LATEST LOG DISPATCHES EVENT STREAM (KAFKA CONTROLLER)",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextLight,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "${logs.size} EVENTS",
                    fontSize = 8.sp,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        if (logs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 30.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No dispatch events logged yet. Seed active shipments or update transitions to trigger webhooks!", color = TextMuted, fontSize = 10.sp, textAlign = TextAlign.Center)
                }
            }
        }

        items(logs) { log ->
            WebhookLogItem(log)
        }
    }
}

@Composable
fun WebhookLogItem(log: WebhookLogEntity) {
    var showFullPayload by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkBg),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                when (log.eventName) {
                                    "shipment.created" -> IndigoNeon.copy(alpha = 0.2f)
                                    "shipment.dispatched" -> CyanNeon.copy(alpha = 0.2f)
                                    "shipment.delivered" -> EmeraldNeon.copy(alpha = 0.2f)
                                    else -> RoseNeon.copy(alpha = 0.2f)
                                }
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(log.eventName, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = TextLight)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Ref: ${log.shipmentId}", fontSize = 9.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "HTTP ${log.responseCode}", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = EmeraldNeon, fontFamily = FontFamily.Monospace)
                    IconButton(
                        onClick = { showFullPayload = !showFullPayload },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (showFullPayload) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Expand",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Text(
                text = "Target Receiver: ${log.url}",
                fontSize = 9.sp,
                color = TextMuted,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 2.dp)
            )

            AnimatedVisibility(visible = showFullPayload) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(CardDark)
                        .border(0.5.dp, CardBorder, RoundedCornerShape(4.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = log.payload,
                        fontSize = 8.sp,
                        color = EmeraldNeon,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 11.sp
                    )
                }
            }
        }
    }
}

// 6. AI LOGISTICS ADVISOR MODULE
@Composable
fun AiLogisticsAdvisorModule(viewModel: LogisticsViewModel) {
    val origin by viewModel.aiOrigin.collectAsStateWithLifecycle()
    val dest by viewModel.aiDestination.collectAsStateWithLifecycle()
    val weight by viewModel.aiWeight.collectAsStateWithLifecycle()
    val category by viewModel.aiCategory.collectAsStateWithLifecycle()
    
    val advice by viewModel.aiAdvice.collectAsStateWithLifecycle()
    val isLoading by viewModel.aiAdviceLoading.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            Text(
                text = "🧠 Smart Cargo Routing & Customs Advisor",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextLight
            )
            Text(
                text = "Leverage Gemini AI model to run intelligent audits. Predict customs clearance tax, estimate Brexit delays, and design cross-border transit logs.",
                fontSize = 11.sp,
                color = TextMuted,
                modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardDark),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "⚡", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "INTELLIGENT INTEGRATION PARAMETERS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyanNeon, fontFamily = FontFamily.Monospace)
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Origin Country", fontSize = 9.sp, color = TextMuted)
                            TextField(
                                value = origin,
                                onValueChange = { viewModel.aiOrigin.value = it },
                                textStyle = LocalTextStyle.current.copy(fontSize = 11.sp),
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = TextFieldDefaults.colors(focusedContainerColor = DarkBg, unfocusedContainerColor = DarkBg)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Destination Country", fontSize = 9.sp, color = TextMuted)
                            TextField(
                                value = dest,
                                onValueChange = { viewModel.aiDestination.value = it },
                                textStyle = LocalTextStyle.current.copy(fontSize = 11.sp),
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = TextFieldDefaults.colors(focusedContainerColor = DarkBg, unfocusedContainerColor = DarkBg)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Category (Cargo Class)", fontSize = 9.sp, color = TextMuted)
                            TextField(
                                value = category,
                                onValueChange = { viewModel.aiCategory.value = it },
                                textStyle = LocalTextStyle.current.copy(fontSize = 11.sp),
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = TextFieldDefaults.colors(focusedContainerColor = DarkBg, unfocusedContainerColor = DarkBg)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(0.6f)) {
                            Text(text = "Cargo Weight (KG)", fontSize = 9.sp, color = TextMuted)
                            TextField(
                                value = weight,
                                onValueChange = { viewModel.aiWeight.value = it },
                                textStyle = LocalTextStyle.current.copy(fontSize = 11.sp),
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = TextFieldDefaults.colors(focusedContainerColor = DarkBg, unfocusedContainerColor = DarkBg)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.runSmartRouting() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = DarkBg)
                        } else {
                            Text("Compute Intelligent Transit Routing", color = DarkBg, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        if (isLoading) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = CyanNeon)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Gemini computing multi-carrier tariff clearance audits & routing checks...", fontSize = 10.sp, color = TextMuted)
                        }
                    }
                }
            }
        } else if (advice.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    border = BorderStroke(1.dp, IndigoNeon)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🤖", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "GEMINI ADVICE REPORT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = CyanNeon,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Parse standard markdown or text beautiful rendering
                        Text(
                            text = advice,
                            fontSize = 11.sp,
                            color = TextLight,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

// REST DEVELOPER JSON SANDBOX TERMINAL PANE
@Composable
fun DeveloperSandboxPane(viewModel: LogisticsViewModel) {
    val url by viewModel.sandboxUrl.collectAsStateWithLifecycle()
    val payload by viewModel.sandboxPayload.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(DarkBg)
            .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(2.dp))
                            .background(RoseNeon.copy(alpha = 0.2f))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(text = "DEV-API", fontSize = 7.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = RoseNeon)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = url,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextLight,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Text(
                    text = "STANDARDISED JSON API SCHEMAS",
                    fontSize = 7.sp,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .background(CardDark)
                    .border(0.5.dp, CardBorder, RoundedCornerShape(4.dp))
                    .padding(8.dp)
            ) {
                Text(
                    text = if (payload.isEmpty()) {
                        "// No API requests made yet during this simulation session. Trigger some comparisons, book packets or standardize addresses to display interactive raw dev JSON request/response formats instantly."
                    } else payload,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    color = EmeraldNeon,
                    lineHeight = 10.sp
                )
            }
        }
    }
}
