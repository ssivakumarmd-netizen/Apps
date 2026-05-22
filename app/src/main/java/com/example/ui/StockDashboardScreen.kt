package com.example.ui

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.BorderStroke
import com.example.data.PriceAlert
import com.example.data.StockHolding
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockDashboardScreen(
    viewModel: StockViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val holdings by viewModel.holdings.collectAsStateWithLifecycle()
    val alerts by viewModel.alerts.collectAsStateWithLifecycle()
    val summary by viewModel.portfolioSummary.collectAsStateWithLifecycle()
    val isAutoSimulating by viewModel.isAutoSimulating.collectAsStateWithLifecycle()

    // Dialog state controllers
    var showAddHoldingDialog by remember { mutableStateOf(false) }
    var showAddAlertDialog by remember { mutableStateOf(false) }
    var selectedStockForPriceEdit by remember { mutableStateOf<StockHolding?>(null) }

    // Segmented tab state holding
    var selectedTab by remember { mutableStateOf(0) } // 0 = Holdings, 1 = Price Targets

    // Request Notification permission for Android 13+
    var showPermissionBanner by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShowChart,
                                contentDescription = "Stock Logo",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "MarketWatch",
                            fontWeight = FontWeight.Bold,
                            fontSize = 21.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (selectedTab == 0) {
                    FloatingActionButton(
                        onClick = { showAddHoldingDialog = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        elevation = FloatingActionButtonDefaults.elevation(6.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Stock")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Buy Stock", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    FloatingActionButton(
                        onClick = { showAddAlertDialog = true },
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = Color.Black,
                        elevation = FloatingActionButtonDefaults.elevation(6.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = "Set Alert")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Set Alert", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Permission requester banner for Android 13+
            if (showPermissionBanner && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.NotificationsActive,
                            contentDescription = "Alert Rules Action",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Enable system alerts",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                "Allow price alerts to pop up in your notifications bar.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = {
                                showPermissionBanner = false
                                Toast.makeText(context, "System alerts notification channel registered!", Toast.LENGTH_SHORT).show()
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("Allow", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Top Portfolio Metric Summary Section
            PortfolioMetricsCard(
                summary = summary,
                isAutoSimulating = isAutoSimulating,
                alertsCount = alerts.size
            )

            // Direct simulation control toolbar
            SimulationControllerRow(
                isAutoSimulating = isAutoSimulating,
                onToggleSim = { viewModel.toggleSimulation() },
                onTriggerVolatile = { 
                    viewModel.triggerQuickFluctuation()
                    Toast.makeText(context, "Simulated market volatilities!", Toast.LENGTH_SHORT).show()
                },
                onReset = {
                    viewModel.resetPortfolio()
                    Toast.makeText(context, "Reset demo data!", Toast.LENGTH_SHORT).show()
                }
            )

            // Bottom Rounded sheet style content container from High Density design specs
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 16.dp)
                ) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.PieChart, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("My Holdings (${holdings.size})", fontWeight = FontWeight.Bold)
                                }
                            }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Alert Rules (${alerts.size})", fontWeight = FontWeight.Bold)
                                }
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // Scrollable Content
                    Box(modifier = Modifier.weight(1f)) {
                if (selectedTab == 0) {
                    if (holdings.isEmpty()) {
                        EmptyStateView(
                            icon = Icons.Default.FolderOpen,
                            title = "No holdings listed",
                            subtitle = "Click 'Buy Stock' below to add purchase records."
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(holdings, key = { it.id }) { holding ->
                                HoldingStockCard(
                                    holding = holding,
                                    onEditPriceSelected = { selectedStockForPriceEdit = holding },
                                    onDeleteHolding = { viewModel.deleteHolding(holding) }
                                )
                            }
                        }
                    }
                } else {
                    if (alerts.isEmpty()) {
                        EmptyStateView(
                            icon = Icons.Default.NotificationsOff,
                            title = "No active alert conditions",
                            subtitle = "Set price alerts to get notified of targets."
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(alerts, key = { it.id }) { alert ->
                                AlertSettingsCard(
                                    alert = alert,
                                    currentStockPrice = holdings.find { it.symbol == alert.symbol }?.currentPrice,
                                    onResetAlert = { viewModel.resetAlert(alert) },
                                    onDeleteAlert = { viewModel.deleteAlert(alert) }
                                )
                            }
                        }
                    }
                }
            }
                }
            }
        }

        // Add Holding Form Dialog
        if (showAddHoldingDialog) {
            AddHoldingDialog(
                onDismiss = { showAddHoldingDialog = false },
                onAddHolding = { symbol, company, shares, buyPrice, currentPrice ->
                    viewModel.addHolding(symbol, company, shares, buyPrice, currentPrice)
                    showAddHoldingDialog = false
                    Toast.makeText(context, "Added stock to portfolio!", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Add Alert Form Dialog
        if (showAddAlertDialog) {
            val symbolsList = holdings.map { it.symbol }.distinct()
            AddAlertDialog(
                availableSymbols = symbolsList,
                onDismiss = { showAddAlertDialog = false },
                onAddAlert = { symbol, target, isAbove ->
                    viewModel.createAlert(symbol, target, isAbove)
                    showAddAlertDialog = false
                    Toast.makeText(context, "Added price alert rule!", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Custom Manual Stock Price Editor Dialog
        selectedStockForPriceEdit?.let { stock ->
            EditStockPriceDialog(
                holding = stock,
                onDismiss = { selectedStockForPriceEdit = null },
                onConfirmPriceChange = { newPrice ->
                    viewModel.updateHoldingPrice(stock, newPrice)
                    selectedStockForPriceEdit = null
                    Toast.makeText(context, "Price modified! Alert engine executed.", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

// Dynamic avatar style helper based on design specifications
@Composable
fun getStockAvatarColors(symbol: String): Pair<Color, Color> {
    return when (symbol.uppercase()) {
        "AAPL" -> Pair(Color(0xFF0F172A), Color.White) // Dark Slate
        "TSLA" -> Pair(Color(0xFFDDE1FF), Color(0xFF001D35)) // Light Lavender / Navy
        "NVDA" -> Pair(Color(0xFFBAF0B3), Color(0xFF14532D)) // Emerald Green / Dark Green
        "GOOG" -> Pair(Color(0xFFFEF3C7), Color(0xFF78350F)) // Warm Gold / Amber
        "MSFT" -> Pair(Color(0xFFCFFAFE), Color(0xFF0891B2)) // Cyan / Teal
        else -> Pair(Color(0xFFE2E8F0), Color(0xFF334155)) // Neutral Gray
    }
}

// Portfolio metrics block
@Composable
fun PortfolioMetricsCard(
    summary: PortfolioSummary,
    isAutoSimulating: Boolean,
    alertsCount: Int
) {
    val isPositive = summary.totalProfitLoss >= 0
    val pnlColor = if (isPositive) HighDensityPillTextGreen else HighDensityPillTextRed
    val pnlBg = if (isPositive) HighDensityPillBgGreen else HighDensityPillBgRed
    val pnlSign = if (isPositive) "+" else ""

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(2.dp, shape = RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = HighDensityCardBg
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "Total Balance",
                        color = Color(0xFF334155), // Slate-700
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$${String.format("%,.2f", summary.totalCurrentValue)}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.SansSerif,
                        color = Color(0xFF0F172A), // Slate-900
                        lineHeight = 32.sp
                    )
                }

                // Dynamic gain pill from High Density spec
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(pnlBg)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                        contentDescription = null,
                        tint = pnlColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$pnlSign${String.format("%.2f", summary.totalProfitLossPercentage)}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = pnlColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Subcards row (glassmorphism overlay from HTML spec: bg-white/40 rounded-2xl p-3)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Day Gain Subcard
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.4f))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "DAY GAIN / PNL",
                        color = Color(0xFF475569),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$pnlSign$${String.format("%,.2f", summary.totalProfitLoss)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = pnlColor
                    )
                }

                // Active Alerts Subcard
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.4f))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "ACTIVE ALERTS",
                        color = Color(0xFF475569),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format("%02d", alertsCount),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }
            }

            // Embedded live simulation indicator at bottom
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    modifier = Modifier.size(6.dp),
                    shape = CircleShape,
                    color = if (isAutoSimulating) HighDensityPillTextGreen else Color.Gray
                ) {}
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isAutoSimulating) "LIVE BROADCAST FEED ACTIVE" else "LIVE BROADCAST FEED SUSPENDED",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isAutoSimulating) Color(0xFF1E293B) else Color(0xFF64748B)
                )
            }
        }
    }
}

// Controller Toolbar Row
@Composable
fun SimulationControllerRow(
    isAutoSimulating: Boolean,
    onToggleSim: () -> Unit,
    onTriggerVolatile: () -> Unit,
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Toggle Feed Button
        Button(
            onClick = onToggleSim,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isAutoSimulating) Color(0xFFFEE2E2) else Color(0xFFE2E8F0),
                contentColor = if (isAutoSimulating) Color(0xFF991B1B) else Color(0xFF334155)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1.2f),
            contentPadding = PaddingValues(vertical = 10.dp)
        ) {
            Icon(
                imageVector = if (isAutoSimulating) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isAutoSimulating) "Stop Feed" else "Auto Feed",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Live Fluctuate Trigger
        Button(
            onClick = onTriggerVolatile,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFE2E8F0),
                contentColor = Color(0xFF334155)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Fluctuate",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Reset demo Button
        Button(
            onClick = onReset,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFF1F5F9),
                contentColor = Color(0xFF64748B)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(0.8f),
            contentPadding = PaddingValues(vertical = 10.dp)
        ) {
            Text(
                text = "Reset",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// UI component for standard holdings item (High Density styled)
@Composable
fun HoldingStockCard(
    holding: StockHolding,
    onEditPriceSelected: () -> Unit,
    onDeleteHolding: () -> Unit
) {
    val isPositive = holding.profitLoss >= 0
    val stockColor = if (isPositive) StockGreen else StockRed
    val pnlSign = if (isPositive) "+" else ""
    val (avatarBg, avatarText) = getStockAvatarColors(holding.symbol)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, shape = RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF8FAFC)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rounded corners Square Symbol Badge (from design HTML: w-12 h-12 bg-slate-900 rounded-xl)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(avatarBg)
            ) {
                Text(
                    text = holding.symbol.take(4),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = avatarText
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text info
            Column(modifier = Modifier.weight(1.3f)) {
                Text(
                    text = holding.companyName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${holding.shares} Shares · @\$${String.format("%.2f", holding.buyPrice)}",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Current price simulator block
            Column(
                modifier = Modifier
                    .weight(1.1f)
                    .clickable { onEditPriceSelected() }
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF1F5F9))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "\$${String.format("%.2f", holding.currentPrice)}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = Color(0xFF0F172A)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit Price",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(9.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "Edit Price",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Profit & Loss info
            Column(
                modifier = Modifier.weight(1.1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "$pnlSign$${String.format("%.2f", holding.profitLoss)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = stockColor
                )
                Text(
                    text = "$pnlSign${String.format("%.2f", holding.profitLossPercentage)}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = stockColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                // Delete holding button
                IconButton(
                    onClick = onDeleteHolding,
                    modifier = Modifier.size(18.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete Holding",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

// UI element for Alert display card (High Density styled)
@Composable
fun AlertSettingsCard(
    alert: PriceAlert,
    currentStockPrice: Double?,
    onResetAlert: () -> Unit,
    onDeleteAlert: () -> Unit
) {
    val conditionText = if (alert.isAbove) "Goes Above" else "Goes Below"
    val isTriggered = alert.isTriggered
    val (avatarBg, avatarText) = getStockAvatarColors(alert.symbol)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, shape = RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = if (isTriggered) Color(0xFFF1F5F9) else Color(0xFFF8FAFC)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rounded corners Square Symbol Badge
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(avatarBg)
            ) {
                Text(
                    text = alert.symbol.take(4),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = avatarText
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1.5f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = alert.symbol,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (alert.isAbove) HighDensityPillBgGreen
                                else HighDensityPillBgRed
                            )
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = conditionText.uppercase(),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (alert.isAbove) HighDensityPillTextGreen else HighDensityPillTextRed
                        )
                    }
                }
                
                Text(
                    text = "Target: \$${String.format("%.2f", alert.targetPrice)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF334155)
                )

                if (currentStockPrice != null) {
                    Text(
                        text = "Current: \$${String.format("%.2f", currentStockPrice)}",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            // Status details or trigger logs
            Column(
                modifier = Modifier.weight(1.2f),
                horizontalAlignment = Alignment.End
            ) {
                if (isTriggered) {
                    Text(
                        text = "TRIGGERED",
                        color = HighDensityPillTextGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                    alert.triggeredAt?.let { time ->
                        val date = Date(time)
                        val format = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                        Text(
                            text = "At ${format.format(date)}",
                            fontSize = 9.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    // Reset Button
                    Button(
                        onClick = onResetAlert,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Reset", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text(
                        text = "ACTIVE",
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Delete Button Actions
            IconButton(
                onClick = onDeleteAlert,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Alert Rule",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// Add New Holding Dialog Frame
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHoldingDialog(
    onDismiss: () -> Unit,
    onAddHolding: (symbol: String, companyName: String, shares: Double, buyPrice: Double, currentPrice: Double) -> Unit
) {
    var symbol by remember { mutableStateOf("") }
    var companyName by remember { mutableStateOf("") }
    var sharesStr by remember { mutableStateOf("") }
    var buyPriceStr by remember { mutableStateOf("") }
    var currentPriceStr by remember { mutableStateOf("") }

    val isInputValid = symbol.isNotEmpty() && 
            (sharesStr.toDoubleOrNull() ?: 0.0) > 0.0 && 
            (buyPriceStr.toDoubleOrNull() ?: 0.0) > 0.0

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .shadow(12.dp, shape = RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Add Stock Holding",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                // Standard Stock Presets picker row for frictionless entry
                Text(
                    text = "Stock Presets (Tap to fill)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val presets = listOf("AAPL", "GOOG", "TSLA", "NVDA", "MSFT")
                    presets.forEach { preset ->
                        AssistChip(
                            onClick = {
                                symbol = preset
                                companyName = when (preset) {
                                    "AAPL" -> "Apple Inc."
                                    "GOOG" -> "Alphabet Inc."
                                    "TSLA" -> "Tesla Inc."
                                    "NVDA" -> "NVIDIA Corporation"
                                    "MSFT" -> "Microsoft Corp."
                                    else -> ""
                                }
                                currentPriceStr = when (preset) {
                                    "AAPL" -> "182.30"
                                    "GOOG" -> "152.45"
                                    "TSLA" -> "178.50"
                                    "NVDA" -> "485.20"
                                    "MSFT" -> "415.60"
                                    else -> ""
                                }
                                buyPriceStr = when (preset) {
                                    "AAPL" -> "175.00"
                                    "GOOG" -> "145.00"
                                    "TSLA" -> "180.00"
                                    "NVDA" -> "430.00"
                                    "MSFT" -> "390.0"
                                    else -> ""
                                }
                            },
                            label = { Text(preset, fontSize = 11.sp) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (symbol == preset) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                else Color.Transparent
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = symbol,
                    onValueChange = { symbol = it.uppercase() },
                    label = { Text("Stock Symbol (e.g. AMZN)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = companyName,
                    onValueChange = { companyName = it },
                    label = { Text("Company Name (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = sharesStr,
                        onValueChange = { sharesStr = it },
                        label = { Text("Shares") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = buyPriceStr,
                        onValueChange = { buyPriceStr = it },
                        label = { Text("Buy Price ($)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = currentPriceStr,
                    onValueChange = { currentPriceStr = it },
                    label = { Text("Current Stock Price ($) - Optional") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val sharesVal = sharesStr.toDoubleOrNull() ?: 0.0
                            val buyVal = buyPriceStr.toDoubleOrNull() ?: 0.0
                            val currentVal = currentPriceStr.toDoubleOrNull() ?: buyVal // default to buy price if omitted
                            onAddHolding(symbol, companyName, sharesVal, buyVal, currentVal)
                        },
                        enabled = isInputValid
                    ) {
                        Text("Save Asset")
                    }
                }
            }
        }
    }
}

// Add Target Price Alert Dialog
@Composable
fun AddAlertDialog(
    availableSymbols: List<String>,
    onDismiss: () -> Unit,
    onAddAlert: (symbol: String, targetPrice: Double, isAbove: Boolean) -> Unit
) {
    var symbol by remember { mutableStateOf(availableSymbols.firstOrNull() ?: "") }
    var targetPriceStr by remember { mutableStateOf("") }
    var isAbove by remember { mutableStateOf(true) } // true = Above, false = Below

    val isInputValid = symbol.isNotEmpty() && 
            (targetPriceStr.toDoubleOrNull() ?: 0.0) > 0.0

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .shadow(12.dp, shape = RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Set Price Alert Rule",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                // Dropdown or Custom Text Input
                OutlinedTextField(
                    value = symbol,
                    onValueChange = { symbol = it.uppercase() },
                    label = { Text("Stock Symbol (e.g. AAPL)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (availableSymbols.isNotEmpty()) {
                    Text(
                        text = "Your Owned Stocks (Tap to set)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        availableSymbols.take(5).forEach { ownedSymbol ->
                            AssistChip(
                                onClick = { symbol = ownedSymbol },
                                label = { Text(ownedSymbol, fontSize = 11.sp) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (symbol == ownedSymbol) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    else Color.Transparent
                                )
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = targetPriceStr,
                    onValueChange = { targetPriceStr = it },
                    label = { Text("Price Target ($)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Trigger Alert Condition:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                // Toggle Selector for Above vs Below
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = isAbove,
                        onClick = { isAbove = true },
                        label = { 
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Goes ABOVE") 
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = !isAbove,
                        onClick = { isAbove = false },
                        label = { 
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.TrendingDown, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Goes BELOW") 
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val targetVal = targetPriceStr.toDoubleOrNull() ?: 0.0
                            onAddAlert(symbol, targetVal, isAbove)
                        },
                        enabled = isInputValid,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = Color.Black
                        )
                    ) {
                        Text("Enable Alert", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Edit custom price dialog to easily verify alerts matching
@Composable
fun EditStockPriceDialog(
    holding: StockHolding,
    onDismiss: () -> Unit,
    onConfirmPriceChange: (newPrice: Double) -> Unit
) {
    var rawInputPrice by remember { mutableStateOf(holding.currentPrice.toString()) }
    val isInputValid = (rawInputPrice.toDoubleOrNull() ?: 0.0) > 0.0

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .shadow(12.dp, shape = RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Edit Stock Price: ${holding.symbol}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Modify ${holding.symbol}'s current price to simulate a price shift immediately. Run this to test if your active target alerts are hit properly!",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = rawInputPrice,
                    onValueChange = { rawInputPrice = it },
                    label = { Text("Simulation Current Price ($)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val newPrice = rawInputPrice.toDoubleOrNull() ?: holding.currentPrice
                            onConfirmPriceChange(newPrice)
                        },
                        enabled = isInputValid
                    ) {
                        Text("Test Price Shift")
                    }
                }
            }
        }
    }
}

// Beautiful Empty States fallback Composable
@Composable
fun EmptyStateView(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}
