package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import com.example.R
import com.example.data.model.RenderingEngineInfo
import com.example.data.model.RootShizukuStatus
import com.example.data.model.SystemTelemetry
import com.example.tuner.BenchmarkEngine
import com.example.tuner.FloatingFpsService
import com.example.tuner.GameOptimizer
import com.example.tuner.NetworkLatencyOptimizer
import com.example.tuner.RenderingEngineDetector
import com.example.tuner.ShellExecutor
import com.example.tuner.SystemTelemetryManager
import com.example.tuner.ThermalSafetyManager
import com.example.ui.components.InAppFloatingDock
import com.example.ui.components.StatusBadge
import com.example.ui.screens.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class MainNavTab(val label: String, val icon: ImageVector) {
    DASHBOARD("Dasbor", Icons.Default.Dashboard),
    RENDERING("Rendering", Icons.Default.Layers),
    GAMES("Games", Icons.Default.SportsEsports),
    TUNING("Tuning", Icons.Default.Tune),
    TOOLS("Alat & Root", Icons.Default.Build)
}

enum class TuningSubTab(val label: String) {
    GRAPHICS("Grafis"),
    FPS("FPS & Frame"),
    CPU_GPU("CPU & GPU"),
    OPTIMIZATION("Optimasi"),
    NETWORK("Jaringan")
}

enum class ToolsSubTab(val label: String) {
    ROOT_SHIZUKU("Root / Shizuku"),
    BENCHMARK("Benchmark"),
    CALIBRATION("Sentuh & Hz"),
    SETTINGS("Alat & Log")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeoFragxApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Tuner Managers
    val telemetryManager = remember { SystemTelemetryManager(context) }
    val renderingDetector = remember { RenderingEngineDetector(context) }
    val shellExecutor = remember { ShellExecutor(context) }
    val gameOptimizer = remember { GameOptimizer(context) }
    val benchmarkEngine = remember { BenchmarkEngine() }
    val networkOptimizer = remember { NetworkLatencyOptimizer(context) }
    val thermalManager = remember { ThermalSafetyManager(context) }

    // Live States
    var telemetry by remember { mutableStateOf(SystemTelemetry()) }
    var renderingInfo by remember { mutableStateOf(RenderingEngineInfo()) }
    var rootStatus by remember { mutableStateOf(RootShizukuStatus()) }

    // Navigation States
    var currentTab by remember { mutableStateOf(MainNavTab.DASHBOARD) }
    var isDockVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (FloatingFpsService.isRunning) {
            isDockVisible = true
        }
    }

    val tuningTabs = remember { TuningSubTab.values() }
    val tuningPagerState = rememberPagerState(
        initialPage = TuningSubTab.GRAPHICS.ordinal,
        pageCount = { tuningTabs.size }
    )
    val toolsTabs = remember { ToolsSubTab.values() }
    val toolsPagerState = rememberPagerState(
        initialPage = ToolsSubTab.ROOT_SHIZUKU.ordinal,
        pageCount = { toolsTabs.size }
    )

    // Helper functions to jump subtabs smoothly
    fun navigateTuningTo(tab: TuningSubTab) {
        scope.launch {
            tuningPagerState.animateScrollToPage(tab.ordinal)
        }
    }

    fun navigateToolsTo(tab: ToolsSubTab) {
        scope.launch {
            toolsPagerState.animateScrollToPage(tab.ordinal)
        }
    }

    // Periodic telemetry update loop
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                rootStatus = shellExecutor.checkRootAndShizuku()
            } catch (_: Exception) {}
            try {
                renderingInfo = renderingDetector.detectRenderingEngine()
            } catch (_: Exception) {}
        }
        while (true) {
            try {
                telemetry = telemetryManager.getTelemetry()
            } catch (_: Exception) {}
            delay(1500)
        }
    }

    // Handle back button: if on sub-tab or other screen, go back to Dashboard
    BackHandler(enabled = currentTab != MainNavTab.DASHBOARD) {
        currentTab = MainNavTab.DASHBOARD
    }

    Scaffold(
        containerColor = CyberBlack,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Neo Fragx Emblem
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0D1726))
                                .border(1.dp, NeonCyan, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlashOn,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "NEO ",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "FRAGX",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black,
                                    color = NeonCyan
                                )
                            }
                            Text(
                                text = "Advanced Gaming Tuner",
                                fontSize = 10.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                },
                actions = {
                    // Floating Game Dock quick launcher
                    IconButton(
                        onClick = {
                            isDockVisible = !isDockVisible
                            if (isDockVisible) {
                                Toast.makeText(context, "Game Dock aktif! Geser atau ketuk gelembung untuk membuka menu.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Widgets,
                            contentDescription = "Toggle Game Dock",
                            tint = if (isDockVisible) NeonCyan else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Root & Thermal Status indicators
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        StatusBadge(
                            text = if (rootStatus.hasRoot) "ROOT" else "NON-ROOT",
                            color = if (rootStatus.hasRoot) StatusSuccess else StatusWarning
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF0F1722))
                                .border(1.dp, CyberCardStroke, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "${telemetry.usedRamPercent.toInt()}% RAM",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CyberDark
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = CyberDark,
                contentColor = TextPrimary,
                tonalElevation = 8.dp
            ) {
                MainNavTab.values().forEach { tab ->
                    val isSelected = currentTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                tint = if (isSelected) NeonCyan else TextSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = tab.label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) NeonCyan else TextSecondary
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = NeonCyan.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(CyberBlack)
        ) {
            when (currentTab) {
                MainNavTab.DASHBOARD -> {
                    DashboardScreen(
                        telemetry = telemetry,
                        rootStatus = rootStatus,
                        gameOptimizer = gameOptimizer,
                        onNavigateToGameApi = {
                            currentTab = MainNavTab.GAMES
                        },
                        onNavigateToAngleDriver = {
                            currentTab = MainNavTab.RENDERING
                        },
                        onNavigateToArtRuntime = {
                            currentTab = MainNavTab.TUNING
                            navigateTuningTo(TuningSubTab.OPTIMIZATION)
                        },
                        onNavigateToGameDriver = {
                            currentTab = MainNavTab.TUNING
                            navigateTuningTo(TuningSubTab.GRAPHICS)
                        },
                        onNavigateToRendering = {
                            currentTab = MainNavTab.RENDERING
                        },
                        onNavigateToCpuBoost = {
                            currentTab = MainNavTab.TUNING
                            navigateTuningTo(TuningSubTab.CPU_GPU)
                        },
                        onNavigateToBatteryTuning = {
                            currentTab = MainNavTab.TUNING
                            navigateTuningTo(TuningSubTab.OPTIMIZATION)
                        },
                        onNavigateToFpsOverlay = {
                            currentTab = MainNavTab.TUNING
                            navigateTuningTo(TuningSubTab.FPS)
                        },
                        onNavigateToAddons = {
                            currentTab = MainNavTab.TOOLS
                            navigateToolsTo(ToolsSubTab.ROOT_SHIZUKU)
                        },
                        onNavigateToSystemLab = {
                            currentTab = MainNavTab.TUNING
                            navigateTuningTo(TuningSubTab.OPTIMIZATION)
                        },
                        onNavigateToRootTerminal = {
                            currentTab = MainNavTab.TOOLS
                            navigateToolsTo(ToolsSubTab.ROOT_SHIZUKU)
                        }
                    )
                }
                MainNavTab.RENDERING -> {
                    RenderingScreen(
                        engineInfo = renderingInfo,
                        onRefresh = {
                            scope.launch {
                                renderingInfo = renderingDetector.detectRenderingEngine()
                            }
                        }
                    )
                }
                MainNavTab.GAMES -> {
                    GameManagerScreen(
                        gameOptimizer = gameOptimizer
                    )
                }
                MainNavTab.TUNING -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Sub-navigation bar for Tuning with smooth horizontal scrolling & glowing indicator
                        ScrollableTabRow(
                            selectedTabIndex = tuningPagerState.currentPage,
                            containerColor = CyberDark,
                            contentColor = NeonCyan,
                            edgePadding = 16.dp,
                            indicator = { tabPositions ->
                                if (tuningPagerState.currentPage < tabPositions.size) {
                                    TabRowDefaults.SecondaryIndicator(
                                        modifier = Modifier.tabIndicatorOffset(tabPositions[tuningPagerState.currentPage]),
                                        color = NeonCyan,
                                        height = 3.dp
                                    )
                                }
                            },
                            divider = {
                                HorizontalDivider(color = CyberCardStroke, thickness = 1.dp)
                            }
                        ) {
                            tuningTabs.forEachIndexed { index, sub ->
                                val isSubSelected = tuningPagerState.currentPage == index
                                Tab(
                                    selected = isSubSelected,
                                    onClick = {
                                        scope.launch {
                                            tuningPagerState.animateScrollToPage(index)
                                        }
                                    },
                                    text = {
                                        Text(
                                            text = sub.label,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSubSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSubSelected) NeonCyan else TextSecondary
                                        )
                                    }
                                )
                            }
                        }

                        // HorizontalPager for swipeable navigation between tuning subtabs (slide left/right to navigate)
                        HorizontalPager(
                            state = tuningPagerState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) { page ->
                            when (tuningTabs[page]) {
                                TuningSubTab.GRAPHICS -> GraphicsScreen(gameOptimizer = gameOptimizer)
                                TuningSubTab.FPS -> FpsControlScreen()
                                TuningSubTab.CPU_GPU -> CpuGpuScreen(telemetry = telemetry, rootStatus = rootStatus, shellExecutor = shellExecutor)
                                TuningSubTab.OPTIMIZATION -> OptimizationScreen(
                                    gameOptimizer = gameOptimizer,
                                    telemetry = telemetry,
                                    onNavigateToTerminal = {
                                        currentTab = MainNavTab.TOOLS
                                        navigateToolsTo(ToolsSubTab.ROOT_SHIZUKU)
                                    }
                                )
                                TuningSubTab.NETWORK -> NetworkStabilizerScreen(networkOptimizer = networkOptimizer)
                            }
                        }
                    }
                }
                MainNavTab.TOOLS -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Sub-navigation bar for Tools & Root
                        ScrollableTabRow(
                            selectedTabIndex = toolsPagerState.currentPage,
                            containerColor = CyberDark,
                            contentColor = NeonCyan,
                            edgePadding = 16.dp,
                            indicator = { tabPositions ->
                                if (toolsPagerState.currentPage < tabPositions.size) {
                                    TabRowDefaults.SecondaryIndicator(
                                        modifier = Modifier.tabIndicatorOffset(tabPositions[toolsPagerState.currentPage]),
                                        color = NeonCyan,
                                        height = 3.dp
                                    )
                                }
                            },
                            divider = {
                                HorizontalDivider(color = CyberCardStroke, thickness = 1.dp)
                            }
                        ) {
                            toolsTabs.forEachIndexed { index, sub ->
                                val isSubSelected = toolsPagerState.currentPage == index
                                Tab(
                                    selected = isSubSelected,
                                    onClick = {
                                        scope.launch {
                                            toolsPagerState.animateScrollToPage(index)
                                        }
                                    },
                                    text = {
                                        Text(
                                            text = sub.label,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSubSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSubSelected) NeonCyan else TextSecondary
                                        )
                                    }
                                )
                            }
                        }

                        // HorizontalPager for swipeable navigation between tools subtabs
                        HorizontalPager(
                            state = toolsPagerState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) { page ->
                            when (toolsTabs[page]) {
                                ToolsSubTab.ROOT_SHIZUKU -> RootShizukuScreen(rootStatus = rootStatus, shellExecutor = shellExecutor, gameOptimizer = gameOptimizer)
                                ToolsSubTab.BENCHMARK -> BenchmarkScreen(telemetry = telemetry, benchmarkEngine = benchmarkEngine)
                                ToolsSubTab.CALIBRATION -> TouchCalibrationScreen()
                                ToolsSubTab.SETTINGS -> ToolsScreen(gameOptimizer = gameOptimizer)
                            }
                        }
                    }
                }
            }

            // In-App Draggable Floating Game Dock Overlay
            if (isDockVisible) {
                InAppFloatingDock(
                    telemetry = telemetry,
                    gameOptimizer = gameOptimizer,
                    networkOptimizer = networkOptimizer,
                    onClose = { isDockVisible = false }
                )
            }
        }
    }
}
