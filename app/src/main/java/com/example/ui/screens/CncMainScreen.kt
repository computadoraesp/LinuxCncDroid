package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.example.ui.components.CarouselNavButton
import com.example.ui.components.CncErrorBoundary
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ScreenLockPortrait
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.StayCurrentPortrait
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.model.CncNavigationTab
import com.example.model.LogSeverity
import com.example.model.ScreenTimeoutPolicy
import com.example.service.CncAuditExporter
import com.example.ui.components.AlarmEventLogView
import com.example.ui.components.AppManualDialog
import com.example.ui.components.AxisCalibrationDialog
import com.example.ui.components.DroPanel
import com.example.ui.components.EtherCatTelemetryView
import com.example.ui.components.GCodeSecurityLoaderDialog
import com.example.ui.components.HalMonitorDialog
import com.example.ui.components.IndustrialCameraView
import com.example.ui.components.IndustrialTopBar
import com.example.ui.components.JogControlPad
import com.example.ui.components.MachineConfigView
import com.example.ui.components.MdiView
import com.example.ui.components.MiniDroBar
import com.example.ui.components.ModalGCodeBar
import com.example.ui.components.ProbingView
import com.example.ui.components.RunFromLineDialog
import com.example.ui.components.SoftLimitsDialog
import com.example.ui.components.SpeedsFeedsCalculatorDialog
import com.example.ui.components.SpindleFeedPanel
import com.example.ui.components.ToolTableDialog
import com.example.ui.components.ToolpathVisualizer3D
import com.example.ui.components.WcsTableDialog
import com.example.ui.theme.CncActiveGreen
import com.example.ui.theme.CncBackground
import com.example.ui.theme.CncCardBorder
import com.example.ui.theme.CncCyberCyan
import com.example.ui.theme.CncEstopRed
import com.example.ui.theme.CncSurface
import com.example.ui.theme.CncSurfaceVariant
import com.example.ui.theme.CncTextPrimary
import com.example.ui.theme.CncTextSecondary
import com.example.ui.theme.CncWarningAmber
import com.example.viewmodel.CncViewModel

@Composable
fun CncNavigationTab.getIcon(): ImageVector = when (this) {
    CncNavigationTab.CONTROL -> Icons.Default.Tune
    CncNavigationTab.TOOLPATH -> Icons.Default.ViewInAr
    CncNavigationTab.CAMERA -> Icons.Default.Videocam
    CncNavigationTab.PROBING -> Icons.Default.GpsFixed
    CncNavigationTab.ETHERCAT -> Icons.Default.Hub
    CncNavigationTab.MDI -> Icons.Default.Terminal
    CncNavigationTab.LOGS -> Icons.Default.Notifications
    CncNavigationTab.CONFIG -> Icons.Default.Settings
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CncMainScreen(
    modifier: Modifier = Modifier,
    viewModel: CncViewModel,
) {
    val machineState by viewModel.machineState.collectAsStateWithLifecycle()
    val taskMode by viewModel.taskMode.collectAsStateWithLifecycle()
    val currentCoordSystem by viewModel.currentCoordSystem.collectAsStateWithLifecycle()
    val axes by viewModel.axes.collectAsStateWithLifecycle()
    val spindle by viewModel.spindle.collectAsStateWithLifecycle()
    val feed by viewModel.feed.collectAsStateWithLifecycle()
    val coolant by viewModel.coolant.collectAsStateWithLifecycle()
    val probe by viewModel.probe.collectAsStateWithLifecycle()
    val tool by viewModel.tool.collectAsStateWithLifecycle()
    val etherCatMaster by viewModel.etherCatMaster.collectAsStateWithLifecycle()
    val etherCatSlaves by viewModel.etherCatSlaves.collectAsStateWithLifecycle()
    val capabilities by viewModel.capabilities.collectAsStateWithLifecycle()
    val loadedGCode by viewModel.loadedGCode.collectAsStateWithLifecycle()
    val loadedFileName by viewModel.loadedFileName.collectAsStateWithLifecycle()
    val activeGCodeLine by viewModel.activeGCodeLine.collectAsStateWithLifecycle()
    val isSimulated by viewModel.isSimulatedMode.collectAsStateWithLifecycle()
    val userRole by viewModel.userRole.collectAsStateWithLifecycle()
    val isContinuousJog by viewModel.isContinuousJog.collectAsStateWithLifecycle()
    val jogStep by viewModel.jogStep.collectAsStateWithLifecycle()
    val jogSpeed by viewModel.jogSpeedMmMin.collectAsStateWithLifecycle()
    val unitSystem by viewModel.unitSystem.collectAsStateWithLifecycle()
    val mdiText by viewModel.mdiCommandText.collectAsStateWithLifecycle()
    val mdiHistory by viewModel.mdiHistory.collectAsStateWithLifecycle()
    val profiles by viewModel.machineProfiles.collectAsStateWithLifecycle()
    val macros by viewModel.macros.collectAsStateWithLifecycle()
    val eventLogs by viewModel.eventLogs.collectAsStateWithLifecycle()
    val networkLatencyMs by viewModel.networkLatencyMs.collectAsStateWithLifecycle()
    val toolTable by viewModel.toolTable.collectAsStateWithLifecycle()
    val activeTool by viewModel.activeTool.collectAsStateWithLifecycle()
    val cycleElapsedSeconds by viewModel.cycleElapsedSeconds.collectAsStateWithLifecycle()
    val cycleEstimatedTotalSeconds by viewModel.cycleEstimatedTotalSeconds.collectAsStateWithLifecycle()
    val jogStyle by viewModel.jogStyle.collectAsStateWithLifecycle()
    val mpgAxis by viewModel.mpgAxis.collectAsStateWithLifecycle()
    val mpgMultiplier by viewModel.mpgMultiplier.collectAsStateWithLifecycle()
    val activeCalibrationSession by viewModel.activeCalibrationSession.collectAsStateWithLifecycle()
    val batteryLevelPct by viewModel.batteryLevelPct.collectAsStateWithLifecycle()
    val isCharging by viewModel.isCharging.collectAsStateWithLifecycle()
    val batterySafety by viewModel.batterySafety.collectAsStateWithLifecycle()
    val isWeakSignalDismissed by viewModel.isWeakSignalDismissed.collectAsStateWithLifecycle()
    val isBatteryAlertDismissed by viewModel.isBatteryAlertDismissed.collectAsStateWithLifecycle()
    val keepScreenOn by viewModel.keepScreenOn.collectAsStateWithLifecycle()
    val screenTimeoutPolicy by viewModel.screenTimeoutPolicy.collectAsStateWithLifecycle()
    val connectionTelemetry by viewModel.connectionTelemetry.collectAsStateWithLifecycle()

    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val showCyberScanDialog by viewModel.showCyberScanDialog.collectAsStateWithLifecycle()
    val showCalculatorDialog by viewModel.showCalculatorDialog.collectAsStateWithLifecycle()
    val showToolTableDialog by viewModel.showToolTableDialog.collectAsStateWithLifecycle()
    val showCalibrationDialog by viewModel.showCalibrationDialog.collectAsStateWithLifecycle()
    val showManualDialog by viewModel.showManualDialog.collectAsStateWithLifecycle()
    val showWcsTableDialog by viewModel.showWcsTableDialog.collectAsStateWithLifecycle()
    val showHalMonitorDialog by viewModel.showHalMonitorDialog.collectAsStateWithLifecycle()
    val showSoftLimitsDialog by viewModel.showSoftLimitsDialog.collectAsStateWithLifecycle()
    val showRunFromLineDialog by viewModel.showRunFromLineDialog.collectAsStateWithLifecycle()

    val wcsOffsets by viewModel.wcsOffsets.collectAsStateWithLifecycle()
    val modalState by viewModel.modalState.collectAsStateWithLifecycle()
    val executionModifiers by viewModel.executionModifiers.collectAsStateWithLifecycle()
    val halPins by viewModel.halPins.collectAsStateWithLifecycle()
    val softLimitsCheck by viewModel.softLimitsCheck.collectAsStateWithLifecycle()
    val mdiValidationLive by viewModel.mdiValidationLive.collectAsStateWithLifecycle()
    val mdiHistoryEntities by viewModel.mdiHistoryEntities.collectAsStateWithLifecycle()

    val errorCount = remember(eventLogs) {
        eventLogs.count { (it.severity == LogSeverity.ERROR) || (it.severity == LogSeverity.CRITICAL) }
    }
    val tabsListState = rememberLazyListState()
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    Scaffold(
        topBar = {
            IndustrialTopBar(
                machineState = machineState,
                currentCoordSystem = currentCoordSystem,
                architecture = capabilities.architecture,
                userRole = userRole,
                isSimulated = isSimulated,
                latencyMs = networkLatencyMs,
                errorCount = errorCount,
                unitSystem = unitSystem,
                onToggleUnitSystem = { viewModel.toggleUnitSystem() },
                onToggleEstop = { viewModel.toggleEstop() },
                onPowerOn = { viewModel.powerOn() },
                onPowerOff = { viewModel.powerOff() },
                onSelectCoordSystem = { viewModel.engine.setCoordinateSystem(it) },
                onSelectRole = { viewModel.setUserRole(it) },
                onOpenCyberScanner = { viewModel.setShowCyberScanDialog(show = true) },
                onOpenCalculator = { viewModel.setShowCalculatorDialog(show = true) },
                onOpenToolTable = { viewModel.setShowToolTableDialog(show = true) },
                onOpenLogs = { viewModel.setSelectedTab(CncNavigationTab.LOGS) },
                onOpenAxisCalibration = { viewModel.setShowCalibrationDialog(show = true) },
                batteryLevelPct = batteryLevelPct,
                isCharging = isCharging,
                isLowBattery = batterySafety.isLowBattery,
                isCriticalBattery = batterySafety.isCriticalBattery,
                keepScreenOn = keepScreenOn,
                screenTimeoutPolicy = screenTimeoutPolicy,
                connectionTelemetry = connectionTelemetry,
                onReconnectClick = { viewModel.retryConnectionNow() },
                onBatteryClick = { viewModel.setSelectedTab(CncNavigationTab.CONFIG) },
                onScreenPolicyClick = { viewModel.setSelectedTab(CncNavigationTab.CONFIG) },
                onOpenManual = { viewModel.setShowManualDialog(show = true) },
                onOpenHalMonitor = { viewModel.showHalMonitorDialog.value = true },
            )
        },
        bottomBar = {
            Surface(
                color = CncSurface,
                tonalElevation = 8.dp,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CarouselNavButton(
                        direction = "<",
                        enabled = tabsListState.canScrollBackward,
                        height = 38.dp,
                        width = 24.dp,
                        onClick = {
                            coroutineScope.launch {
                                tabsListState.animateScrollBy(-220f)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    LazyRow(
                        state = tabsListState,
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        items(CncNavigationTab.entries.toTypedArray()) { tab ->
                            val isSelected = selectedTab == tab
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) CncCyberCyan.copy(alpha = 0.18f) else CncSurfaceVariant.copy(alpha = 0.6f),
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) CncCyberCyan else CncCardBorder,
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        viewModel.setSelectedTab(tab)
                                    },
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    if ((tab == CncNavigationTab.LOGS) && (errorCount > 0)) {
                                        BadgedBox(
                                            badge = {
                                                Badge(containerColor = CncEstopRed, contentColor = Color.White) {
                                                    Text(errorCount.toString(), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                }
                                            },
                                        ) {
                                            Icon(
                                                imageVector = tab.getIcon(),
                                                contentDescription = stringResource(tab.titleRes),
                                                tint = if (isSelected) CncCyberCyan else CncTextSecondary,
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = tab.getIcon(),
                                            contentDescription = stringResource(tab.titleRes),
                                            tint = if (isSelected) CncCyberCyan else CncTextSecondary,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }

                                    Text(
                                        text = stringResource(tab.titleRes),
                                        color = if (isSelected) CncCyberCyan else CncTextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    CarouselNavButton(
                        direction = ">",
                        enabled = tabsListState.canScrollForward,
                        height = 38.dp,
                        width = 24.dp,
                        onClick = {
                            coroutineScope.launch {
                                tabsListState.animateScrollBy(220f)
                            }
                        }
                    )
                }
            }
        },
        containerColor = CncBackground,
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(CncBackground),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // --- CRITICAL / LOW BATTERY WARNING BANNER ---
                if ((batterySafety.isLowBattery || batterySafety.isCriticalBattery) && !batterySafety.isCharging && !isBatteryAlertDismissed) {
                    Surface(
                        color = if (batterySafety.isCriticalBattery) CncEstopRed.copy(alpha = 0.16f) else CncWarningAmber.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.5.dp, if (batterySafety.isCriticalBattery) CncEstopRed else CncWarningAmber),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BatteryAlert,
                                    contentDescription = null,
                                    tint = if (batterySafety.isCriticalBattery) CncEstopRed else CncWarningAmber,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = if (batterySafety.isCriticalBattery)
                                        stringResource(R.string.alarm_critical_battery_title)
                                    else
                                        stringResource(R.string.alarm_low_battery_title),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    color = if (batterySafety.isCriticalBattery) CncEstopRed else CncWarningAmber
                                )
                            }
                            Text(
                                text = if (batterySafety.isCriticalBattery)
                                    stringResource(R.string.alarm_critical_battery_msg, batterySafety.levelPct)
                                else
                                    stringResource(R.string.alarm_low_battery_msg, batterySafety.levelPct),
                                fontSize = 11.sp,
                                color = CncTextPrimary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (machineState == com.example.model.MachineStateEnum.RUNNING) {
                                    Button(
                                        onClick = { viewModel.feedHold() },
                                        colors = ButtonDefaults.buttonColors(containerColor = CncEstopRed),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(stringResource(R.string.alarm_pause_cycle_btn), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                OutlinedButton(
                                    onClick = { viewModel.dismissBatteryAlert() },
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(stringResource(R.string.alarm_dismiss_btn), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // --- RECONNECTION & WEAK SIGNAL BANNER ---
                if (!connectionTelemetry.isConnected || connectionTelemetry.isReconnecting) {
                    Surface(
                        color = CncEstopRed.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.5.dp, CncEstopRed),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = CncEstopRed,
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = stringResource(R.string.alarm_reconnecting_title),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    color = CncEstopRed
                                )
                            }
                            Text(
                                text = "${stringResource(R.string.alarm_reconnecting_msg, connectionTelemetry.secondsUntilReconnect, connectionTelemetry.reconnectAttempt)} • ${connectionTelemetry.lastDisconnectReason ?: "Enlace interrumpido"}",
                                fontSize = 11.sp,
                                color = CncTextPrimary
                            )
                            Button(
                                onClick = { viewModel.retryConnectionNow() },
                                colors = ButtonDefaults.buttonColors(containerColor = CncEstopRed),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.alarm_reconnect_now_btn), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else if (connectionTelemetry.isWeakSignal && !isWeakSignalDismissed) {
                    Surface(
                        color = CncWarningAmber.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, CncWarningAmber),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = null,
                                    tint = CncWarningAmber,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        stringResource(R.string.alarm_weak_signal_title),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = CncWarningAmber
                                    )
                                    Text(
                                        stringResource(R.string.alarm_weak_signal_msg, connectionTelemetry.latencyMs),
                                        fontSize = 10.sp,
                                        color = CncTextSecondary
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = { viewModel.dismissWeakSignalAlert() },
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(stringResource(R.string.alarm_dismiss_btn), fontSize = 10.sp)
                            }
                        }
                    }
                }

                // --- SCREEN TIMEOUT WARNING BANNER ---
                if (screenTimeoutPolicy == ScreenTimeoutPolicy.SYSTEM_TIMEOUT &&
                    (machineState == com.example.model.MachineStateEnum.RUNNING || machineState == com.example.model.MachineStateEnum.HOMING || machineState == com.example.model.MachineStateEnum.PAUSED)
                ) {
                    Surface(
                        color = CncWarningAmber.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, CncWarningAmber),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = Icons.Default.StayCurrentPortrait,
                                    contentDescription = null,
                                    tint = CncWarningAmber,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        "AVISO DE APAGADO DE PANTALLA",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = CncWarningAmber
                                    )
                                    Text(
                                        stringResource(R.string.status_screen_timeout_warning),
                                        fontSize = 10.sp,
                                        color = CncTextSecondary
                                    )
                                }
                            }
                            Button(
                                onClick = { viewModel.setScreenTimeoutPolicy(ScreenTimeoutPolicy.ALWAYS_ON) },
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("ACTIVAR WAKE-LOCK", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                when (selectedTab) {
                    CncNavigationTab.CONTROL -> {
                        // Digital Readout (DRO) Panel
                        DroPanel(
                            machineState = machineState,
                            axes = axes,
                            currentCoordSystem = currentCoordSystem,
                            hasServoTorque = capabilities.hasServoTorque,
                            unitSystem = unitSystem,
                            onZeroAxis = { viewModel.zeroAxis(it) },
                            onZeroAll = { viewModel.zeroAllAxes() },
                            onHomeAxis = { viewModel.homeAxis(it) },
                            onHomeAll = { viewModel.homeAllAxes() },
                            onOpenWcsTable = { viewModel.showWcsTableDialog.value = true },
                        )

                        // Soft Limits Diagnostic Quick-Banner
                        if (loadedGCode.isNotEmpty()) {
                            Surface(
                                color = if (softLimitsCheck.isWithinLimits) Color(0xFF1B5E20).copy(alpha = 0.25f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (softLimitsCheck.isWithinLimits) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error),
                                modifier = Modifier.fillMaxWidth().clickable { viewModel.showSoftLimitsDialog.value = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (softLimitsCheck.isWithinLimits) Icons.Default.CheckCircle else Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = if (softLimitsCheck.isWithinLimits) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (softLimitsCheck.isWithinLimits) "SOFT LIMITS: SAFE ENVELOPE (${softLimitsCheck.activeWcs})" else "SOFT LIMITS: OVERTRAVEL VIOLATION (${softLimitsCheck.activeWcs})",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (softLimitsCheck.isWithinLimits) Color(0xFF81C784) else MaterialTheme.colorScheme.error
                                        )
                                    }
                                    Text("DIAGNOSTICS >", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CncCyberCyan)
                                }
                            }
                        }

                        // LinuxCNC Modal G-Code Active State Bar
                        ModalGCodeBar(
                            modalState = modalState
                        )

                        // Jogging Controls (Button Pad + Virtual MPG Handwheel)
                        JogControlPad(
                            axes = capabilities.axes,
                            axesMap = axes,
                            machineState = machineState,
                            jogStyle = jogStyle,
                            taskMode = taskMode,
                            mpgAxis = mpgAxis,
                            mpgMultiplier = mpgMultiplier,
                            isContinuous = isContinuousJog,
                            selectedStepMm = jogStep,
                            jogSpeedMmMin = jogSpeed,
                            unitSystem = unitSystem,
                            onSelectJogStyle = { viewModel.setJogStyle(it) },
                            onSelectMpgAxis = { viewModel.setMpgAxis(it) },
                            onSelectMpgMultiplier = { viewModel.setMpgMultiplier(it) },
                            onMpgStep = { axis, dir, mult -> viewModel.sendMpgStep(axis, dir, mult) },
                            onZeroAxis = { viewModel.zeroAxis(it) },
                            onToggleContinuous = { viewModel.setJogMode(it) },
                            onSelectStep = { viewModel.setJogStep(it) },
                            onSpeedChange = { viewModel.setJogSpeed(it) },
                            onStartJog = { axis, dir, _ -> viewModel.startJog(axis, dir) },
                            onStopJog = { viewModel.stopJog() },
                        ) { axis, dir, _ -> viewModel.stepJog(axis, dir) }

                        // Spindle, Feedrate Overrides & Cycle Controls
                        SpindleFeedPanel(
                            spindle = spindle,
                            feed = feed,
                            coolant = coolant,
                            machineState = machineState,
                            unitSystem = unitSystem,
                            executionModifiers = executionModifiers,
                            onToggleSingleBlock = { viewModel.setSingleBlockMode(it) },
                            onToggleOptionalStop = { viewModel.setOptionalStop(it) },
                            onToggleBlockDelete = { viewModel.setBlockDelete(it) },
                            onSingleBlockStep = { viewModel.singleBlockStep() },
                            onOpenRunFromLine = { viewModel.showRunFromLineDialog.value = true },
                            onToggleSpindle = { viewModel.engine.toggleSpindle() },
                            onSetSpindleRpm = { viewModel.engine.setSpindleRpm(it) },
                            onSpindleOverride = { viewModel.engine.setSpindleOverride(it) },
                            onFeedOverride = { viewModel.engine.setFeedOverride(it) },
                            onToggleMist = { viewModel.engine.toggleMistCoolant() },
                            onToggleFlood = { viewModel.engine.toggleFloodCoolant() },
                            onCycleStart = { viewModel.cycleStart() },
                            onFeedHold = { viewModel.feedHold() },
                            onCycleStop = { viewModel.cycleStop() },
                        )
                    }

                    CncNavigationTab.TOOLPATH -> {
                        CncErrorBoundary(
                            componentName = "Visualizador 3D G-Code",
                            onReset = { /* resets locally */ }
                        ) {
                            ToolpathVisualizer3D(
                                gcodeList = loadedGCode,
                                activeLineIndex = activeGCodeLine,
                                axes = axes,
                                fileName = loadedFileName,
                                elapsedSeconds = cycleElapsedSeconds,
                                estimatedTotalSeconds = cycleEstimatedTotalSeconds,
                                feedRate = feed.actualFeed,
                                spindleRpm = spindle.actualRpm,
                                activeToolDiameter = activeTool.diameter,
                            ) { viewModel.setShowCyberScanDialog(show = true) }
                        }

                        // Compact Spindle / Cycle Control Bar below Toolpath
                        SpindleFeedPanel(
                            spindle = spindle,
                            feed = feed,
                            coolant = coolant,
                            machineState = machineState,
                            unitSystem = unitSystem,
                            onToggleSpindle = { viewModel.engine.toggleSpindle() },
                            onSetSpindleRpm = { viewModel.engine.setSpindleRpm(it) },
                            onSpindleOverride = { viewModel.engine.setSpindleOverride(it) },
                            onFeedOverride = { viewModel.engine.setFeedOverride(it) },
                            onToggleMist = { viewModel.engine.toggleMistCoolant() },
                            onToggleFlood = { viewModel.engine.toggleFloodCoolant() },
                            onCycleStart = { viewModel.cycleStart() },
                            onFeedHold = { viewModel.feedHold() },
                            onCycleStop = { viewModel.cycleStop() },
                        )
                    }

                    CncNavigationTab.CAMERA -> {
                        CncErrorBoundary(componentName = "Cámara & Visión Artificial") {
                            IndustrialCameraView(
                                machineState = machineState,
                                axes = axes,
                                currentWcs = currentCoordSystem,
                                unitSystem = unitSystem,
                                onJogAxis = { axis, delta -> viewModel.stepJog(axis, if (delta > 0) 1 else -1) },
                                onZeroAxis = { viewModel.zeroAxis(it) },
                                onZeroWithOffset = { offX, offY -> viewModel.setWorkOriginWithCameraOffset(offX, offY) },
                            )
                        }
                    }

                    CncNavigationTab.PROBING -> {
                        MiniDroBar(
                            axes = axes,
                            currentCoordSystem = currentCoordSystem,
                            unitSystem = unitSystem,
                        ) { viewModel.zeroAxis(it) }

                        CncErrorBoundary(componentName = "Ciclos de Palpado") {
                            ProbingView(
                                probeInfo = probe,
                                onExecuteRoutine = { viewModel.triggerProbe(it) },
                            )
                        }
                    }

                    CncNavigationTab.ETHERCAT -> {
                        CncErrorBoundary(componentName = "Bus de Campo EtherCAT") {
                            EtherCatTelemetryView(
                                masterInfo = etherCatMaster,
                                slaves = etherCatSlaves,
                            )
                        }
                    }

                    CncNavigationTab.MDI -> {
                        MiniDroBar(
                            axes = axes,
                            currentCoordSystem = currentCoordSystem,
                            unitSystem = unitSystem,
                        ) { viewModel.zeroAxis(it) }

                        MdiView(
                            machineState = machineState,
                            commandText = mdiText,
                            history = mdiHistory,
                            historyEntities = mdiHistoryEntities,
                            macros = macros,
                            validationResult = mdiValidationLive,
                            onCommandTextChange = { viewModel.setMdiText(it) },
                            onExecuteCommand = { viewModel.executeMdiCommand(it) },
                            onToggleFavorite = { id, fav -> viewModel.toggleMdiFavorite(id, fav) },
                        )
                    }

                    CncNavigationTab.LOGS -> {
                        AlarmEventLogView(
                            logs = eventLogs,
                            onClearLogs = { viewModel.clearLogs() },
                            onGenerateReport = {
                                CncAuditExporter.generateTextReport(
                                    machineState = machineState,
                                    capabilities = capabilities,
                                    axes = axes,
                                    spindle = spindle,
                                    feed = feed,
                                    etherCatMaster = etherCatMaster,
                                    etherCatSlaves = etherCatSlaves,
                                    logs = eventLogs,
                                )
                            },
                        )
                    }

                    CncNavigationTab.CONFIG -> {
                        MachineConfigView(
                            capabilities = capabilities,
                            profiles = profiles,
                            onSwitchArchitecture = { viewModel.engine.switchArchitecture(it) },
                            onConnectHost = { ip, port -> viewModel.engine.connectToHost(ip, port) },
                            onSaveProfile = { name, ip, port, arch -> viewModel.saveProfile(name, ip, port, arch) },
                            onDeleteProfile = { viewModel.deleteProfile(it) },
                            onWipeAllData = { viewModel.wipeAllAppData() },
                            onOpenMetrologyCalibration = { viewModel.setShowCalibrationDialog(show = true) },
                            onOpenManual = { viewModel.setShowManualDialog(show = true) },
                            onOpenHalMonitor = { viewModel.showHalMonitorDialog.value = true },
                            onOpenWcsTable = { viewModel.showWcsTableDialog.value = true },
                            screenTimeoutPolicy = screenTimeoutPolicy,
                            onSelectScreenTimeoutPolicy = { viewModel.setScreenTimeoutPolicy(it) },
                            batterySafetyState = batterySafety,
                            isSimulatedMode = isSimulated,
                            onToggleSimulatedMode = { viewModel.setEngineSimulatedMode(it) },
                            connectionTelemetry = connectionTelemetry,
                            onSimulateWeakSignal = { viewModel.simulateWeakSignal(it) },
                            onSimulateDisconnect = { viewModel.simulateConnectionLoss() },
                            onRestoreConnection = { viewModel.restoreConnectionSimulation() },
                            onSimulateBattery = { level, charging -> viewModel.simulateBatteryLevel(level, charging) },
                            onRestoreBattery = { viewModel.restoreBatterySensor() },
                            onSimulateCharging = { viewModel.setBatteryChargingSimulation(it) },
                            onInjectFault = { viewModel.injectSimulatedFault(it) },
                            onSimulateProbeTouch = { viewModel.simulateTouchProbe() },
                        )
                    }
                }
            }

            // Dialogs
            if (showCyberScanDialog) {
                GCodeSecurityLoaderDialog(
                    onDismiss = { viewModel.setShowCyberScanDialog(show = false) },
                ) { fileName, content ->
                    viewModel.loadAndScanGCode(fileName, content)
                    viewModel.setShowCyberScanDialog(show = false)
                }
            }

            if (showCalculatorDialog) {
                SpeedsFeedsCalculatorDialog(
                    onDismiss = { viewModel.setShowCalculatorDialog(show = false) },
                ) { rpm, feedRate ->
                    viewModel.applySpeedsFeeds(rpm, feedRate)
                    viewModel.setShowCalculatorDialog(show = false)
                }
            }

            if (showToolTableDialog) {
                ToolTableDialog(
                    tools = toolTable,
                    activeTool = activeTool,
                    currentSpindleZ = axes["Z"]?.workPos ?: 0.0,
                    onDismiss = { viewModel.setShowToolTableDialog(show = false) },
                    onMountTool = { viewModel.mountTool(it) },
                    onUpdateTool = { viewModel.updateTool(it) },
                    onDeleteTool = { viewModel.deleteTool(it) },
                ) {
                    viewModel.touchOffToolZ(it)
                    viewModel.engine.logEvent(LogSeverity.INFO, "TOOL", "Touch-off for T$it: Tool # ${tool.toolNumber}")
                }
            }

            if (showCalibrationDialog) {
                AxisCalibrationDialog(
                    session = activeCalibrationSession,
                    onDismiss = { viewModel.setShowCalibrationDialog(show = false) },
                    onStartSession = { axis, travel, interval, instName, instUncertainty ->
                        viewModel.startAxisCalibration(axis, travel, interval, instName, instUncertainty)
                    },
                    onRecordPoint = { stepIdx, measuredVal ->
                        viewModel.recordCalibrationMeasurement(stepIdx, measuredVal)
                    },
                    onMoveToNominal = { stepIdx ->
                        viewModel.moveAxisToNominal(stepIdx)
                    },
                    onGenerateCompTable = { sess ->
                        viewModel.generateLinuxCncCompTable(sess)
                    },
                )
            }

            if (showManualDialog) {
                AppManualDialog(
                    onDismiss = { viewModel.setShowManualDialog(show = false) },
                )
            }

            // PRO DIALOG 1: WCS Offset Table & Touch-Off
            if (showWcsTableDialog) {
                WcsTableDialog(
                    currentCoordSystem = currentCoordSystem,
                    wcsOffsets = wcsOffsets,
                    onSelectWcs = { viewModel.setCoordinateSystem(it) },
                    onTouchOff = { axis, targetVal -> viewModel.touchOffAxis(axis, targetVal) },
                    onSetOffset = { wcs, axis, offsetVal -> viewModel.setWcsOffset(wcs, axis, offsetVal) },
                    onDismiss = { viewModel.showWcsTableDialog.value = false }
                )
            }

            // PRO DIALOG 2: HAL Signals & Pin Monitor
            if (showHalMonitorDialog) {
                HalMonitorDialog(
                    pins = halPins,
                    onTogglePin = { viewModel.toggleHalPin(it) },
                    onDismiss = { viewModel.showHalMonitorDialog.value = false }
                )
            }

            // PRO DIALOG 3: Soft Limits Trajectory Pre-Check
            if (showSoftLimitsDialog) {
                SoftLimitsDialog(
                    checkResult = softLimitsCheck,
                    onOpenWcsTable = {
                        viewModel.showSoftLimitsDialog.value = false
                        viewModel.showWcsTableDialog.value = true
                    },
                    onForceCycleStart = {
                        viewModel.showSoftLimitsDialog.value = false
                        viewModel.cycleStart(forceOverrideLimits = true)
                    },
                    onDismiss = { viewModel.showSoftLimitsDialog.value = false }
                )
            }

            // PRO DIALOG 4: Run From Line Dialog
            if (showRunFromLineDialog) {
                RunFromLineDialog(
                    gcodeList = loadedGCode,
                    currentLineIndex = activeGCodeLine,
                    onDismiss = { viewModel.showRunFromLineDialog.value = false },
                    onConfirmRunFromLine = { targetIndex ->
                        viewModel.runFromLine(targetIndex)
                    }
                )
            }
        }
    }
}
