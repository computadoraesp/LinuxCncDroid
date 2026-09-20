package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.ScreenLockPortrait
import androidx.compose.material.icons.filled.StayCurrentPortrait
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import com.example.model.ConnectionTelemetry
import com.example.model.HardwareArchitecture
import com.example.model.MachineStateEnum
import com.example.model.ScreenTimeoutPolicy
import com.example.model.UnitSystem
import com.example.model.UserRole
import com.example.ui.theme.CncActiveGreen
import com.example.ui.theme.CncCardBg
import com.example.ui.theme.CncCardBorder
import com.example.ui.theme.CncCyberCyan
import com.example.ui.theme.CncEstopRed
import com.example.ui.theme.CncInfoBlue
import com.example.ui.theme.CncSurface
import com.example.ui.theme.CncSurfaceVariant
import com.example.ui.theme.CncTextMuted
import com.example.ui.theme.CncTextPrimary
import com.example.ui.theme.CncWarningAmber
import kotlinx.coroutines.launch

@Composable
fun IndustrialTopBar(
    machineState: MachineStateEnum,
    currentCoordSystem: String,
    architecture: HardwareArchitecture,
    userRole: UserRole,
    isSimulated: Boolean,
    onToggleEstop: () -> Unit,
    onPowerOn: () -> Unit,
    onPowerOff: () -> Unit,
    onSelectCoordSystem: (String) -> Unit,
    onSelectRole: (UserRole) -> Unit,
    modifier: Modifier = Modifier,
    latencyMs: Int = 2,
    errorCount: Int = 0,
    unitSystem: UnitSystem = UnitSystem.METRIC,
    onToggleUnitSystem: () -> Unit = {},
    onOpenCyberScanner: () -> Unit = {},
    onOpenCalculator: () -> Unit = {},
    onOpenToolTable: () -> Unit = {},
    onOpenLogs: () -> Unit = {},
    onOpenAxisCalibration: () -> Unit = {},
    onOpenManual: () -> Unit = {},
    onOpenHalMonitor: () -> Unit = {},
    onOpenConnectionWizard: () -> Unit = {},
    batteryLevelPct: Int = 100,
    isCharging: Boolean = false,
    isLowBattery: Boolean = false,
    isCriticalBattery: Boolean = false,
    keepScreenOn: Boolean = true,
    screenTimeoutPolicy: ScreenTimeoutPolicy = ScreenTimeoutPolicy.ALWAYS_ON,
    connectionTelemetry: ConnectionTelemetry = ConnectionTelemetry(),
    onReconnectClick: () -> Unit = {},
    onBatteryClick: () -> Unit = {},
    onScreenPolicyClick: () -> Unit = {},
) {
    var coordMenuExpanded by remember { mutableStateOf(value = false) }
    var roleMenuExpanded by remember { mutableStateOf(value = false) }

    val coordSystems = listOf("G54", "G55", "G56", "G57", "G58", "G59", "G59.1", "G59.2", "G59.3")
    val toolsScrollState = rememberScrollState()
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    Surface(
        color = CncSurface,
        shadowElevation = 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .border(width = 1.dp, color = CncCardBorder),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // =========================================================================
            // FIXED / PINNED LEFT CONTROLS: E-STOP, Machine Power, State Indicator
            // =========================================================================
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                // Emergency Stop Button
                val isEstop = machineState == MachineStateEnum.ESTOP
                val estopBg by animateColorAsState(
                    targetValue = if (isEstop) CncEstopRed else Color(0xFF3E1218),
                    label = "estop_color",
                )

                Button(
                    onClick = onToggleEstop,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = estopBg,
                        contentColor = Color.White,
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .border(
                            width = 2.dp,
                            color = if (isEstop) Color.White else CncEstopRed,
                            shape = RoundedCornerShape(8.dp),
                        ),
                ) {
                    Icon(
                        imageVector = if (isEstop) Icons.Default.Warning else Icons.Default.Block,
                        contentDescription = stringResource(R.string.topbar_estop),
                        tint = Color.White,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = stringResource(R.string.topbar_estop),
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp,
                    )
                }

                // Power ON / OFF Button
                if (!isEstop) {
                    val isPowerOn = (machineState == MachineStateEnum.ON) ||
                            (machineState == MachineStateEnum.RUNNING) ||
                            (machineState == MachineStateEnum.IDLE) ||
                            (machineState == MachineStateEnum.PAUSED)
                    IconButton(
                        onClick = { if (isPowerOn) onPowerOff() else onPowerOn() },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isPowerOn) Color(0xFF003919) else Color(0xFF263238))
                            .border(
                                1.dp,
                                if (isPowerOn) CncActiveGreen else CncTextMuted,
                                RoundedCornerShape(8.dp),
                            ),
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = stringResource(if (isPowerOn) R.string.topbar_power_off else R.string.topbar_power_on),
                            tint = if (isPowerOn) CncActiveGreen else CncTextMuted,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                // State Indicator (IDLE, RUNNING, HOMING, PAUSED, etc.)
                val stateColor = when (machineState) {
                    MachineStateEnum.ESTOP -> CncEstopRed
                    MachineStateEnum.RUNNING -> CncActiveGreen
                    MachineStateEnum.PAUSED -> CncWarningAmber
                    MachineStateEnum.ON, MachineStateEnum.IDLE -> CncCyberCyan
                    else -> CncTextMuted
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(stateColor.copy(alpha = 0.15f))
                        .border(1.dp, stateColor, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(stateColor)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = stringResource(machineState.displayNameRes),
                            color = stateColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Visual separation divider between Fixed items and Carousel
            VerticalDivider(
                modifier = Modifier
                    .height(26.dp)
                    .padding(horizontal = 4.dp),
                color = CncCardBorder
            )

            // =========================================================================
            // HORIZONTAL CAROUSEL: All other tools scroll smoothly with < and > controls
            // =========================================================================
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CarouselNavButton(
                    direction = "<",
                    enabled = toolsScrollState.value > 0,
                    height = 30.dp,
                    width = 18.dp,
                    onClick = {
                        coroutineScope.launch { toolsScrollState.animateScrollBy(-130f) }
                    }
                )

                Spacer(modifier = Modifier.width(3.dp))

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(toolsScrollState),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                // 1. Unit System Toggle Button (G21 MM / G20 INCH)
                OutlinedButton(
                    onClick = onToggleUnitSystem,
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (unitSystem == UnitSystem.METRIC) CncCyberCyan else CncWarningAmber,
                        containerColor = if (unitSystem == UnitSystem.METRIC) CncCyberCyan.copy(alpha = 0.12f) else CncWarningAmber.copy(alpha = 0.12f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (unitSystem == UnitSystem.METRIC) CncCyberCyan.copy(alpha = 0.8f) else CncWarningAmber.copy(alpha = 0.8f)
                    ),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(
                        text = "${unitSystem.code} ${unitSystem.shortLabel}",
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // 2. Coordinate System Dropdown (G54 - G59.3)
                Box {
                    OutlinedButton(
                        onClick = { coordMenuExpanded = true },
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = CncCyberCyan,
                            containerColor = CncSurfaceVariant
                        ),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                            brush = androidx.compose.ui.graphics.SolidColor(CncCyberCyan.copy(alpha = 0.6f))
                        ),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text(
                            text = currentCoordSystem,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    DropdownMenu(
                        expanded = coordMenuExpanded,
                        onDismissRequest = { coordMenuExpanded = false },
                        modifier = Modifier.background(CncCardBg)
                    ) {
                        coordSystems.forEach { gCoord ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(R.string.topbar_wcs_label, gCoord),
                                        color = if (gCoord == currentCoordSystem) CncCyberCyan else CncTextPrimary,
                                        fontWeight = if (gCoord == currentCoordSystem) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    onSelectCoordSystem(gCoord)
                                    coordMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // 3. User Role Selector
                Box {
                    IconButton(
                        onClick = { roleMenuExpanded = true },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(CncSurfaceVariant)
                            .border(1.dp, CncCardBorder, RoundedCornerShape(6.dp))
                    ) {
                        Icon(
                            imageVector = when (userRole) {
                                UserRole.ADMIN -> Icons.Default.AdminPanelSettings
                                UserRole.OPERATOR -> Icons.Default.Engineering
                                UserRole.VIEWER -> Icons.Default.Visibility
                            },
                            contentDescription = "User Role",
                            tint = when (userRole) {
                                UserRole.ADMIN -> CncWarningAmber
                                UserRole.OPERATOR -> CncActiveGreen
                                UserRole.VIEWER -> CncInfoBlue
                            },
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = roleMenuExpanded,
                        onDismissRequest = { roleMenuExpanded = false },
                        modifier = Modifier.background(CncCardBg)
                    ) {
                        UserRole.entries.forEach { role ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(role.displayNameRes),
                                        color = if (role == userRole) CncActiveGreen else CncTextPrimary
                                    )
                                },
                                onClick = {
                                    onSelectRole(role)
                                    roleMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // 4. Screen Sleep / Wake Lock Status Badge
                val screenColor = if (keepScreenOn) CncActiveGreen else CncWarningAmber
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = screenColor.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, screenColor.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .height(34.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onScreenPolicyClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (keepScreenOn) Icons.Default.ScreenLockPortrait else Icons.Default.StayCurrentPortrait,
                            contentDescription = stringResource(if (keepScreenOn) R.string.status_screen_always_on else R.string.status_screen_system_timeout),
                            tint = screenColor,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (keepScreenOn) "SCREEN ON" else "TIMEOUT",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = screenColor
                        )
                    }
                }

                // 5. Battery Level & Low/Critical Alarm Badge
                val batteryColor = when {
                    isCriticalBattery -> CncEstopRed
                    isLowBattery -> CncWarningAmber
                    isCharging -> CncActiveGreen
                    else -> CncCyberCyan
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = batteryColor.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, batteryColor.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .height(34.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onBatteryClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = when {
                                isCharging -> Icons.Default.BatteryChargingFull
                                isLowBattery || isCriticalBattery -> Icons.Default.BatteryAlert
                                else -> Icons.Default.BatteryFull
                            },
                            contentDescription = "Battery $batteryLevelPct%",
                            tint = batteryColor,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "$batteryLevelPct%",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = batteryColor
                        )
                        if (isCharging) {
                            Text(
                                text = "CHG",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                color = CncActiveGreen
                            )
                        }
                    }
                }

                // 6. Network Telemetry, Weak Signal & Reconnection Badge
                val isReconnecting = !connectionTelemetry.isConnected || connectionTelemetry.isReconnecting
                val isWeak = connectionTelemetry.isWeakSignal
                val netColor = when {
                    isReconnecting -> CncEstopRed
                    isWeak -> CncWarningAmber
                    else -> CncActiveGreen
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = netColor.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, netColor.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .height(34.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            if (isReconnecting) onReconnectClick()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = when {
                                isReconnecting -> Icons.Default.WifiOff
                                isWeak -> Icons.Default.Sync
                                else -> Icons.Default.Wifi
                            },
                            contentDescription = "Network telemetry",
                            tint = netColor,
                            modifier = Modifier.size(15.dp)
                        )
                        if (isReconnecting) {
                            Text(
                                text = if (connectionTelemetry.secondsUntilReconnect > 0)
                                    "RETRY ${connectionTelemetry.secondsUntilReconnect}s"
                                else "RECONNECT",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = CncEstopRed
                            )
                        } else {
                            Text(
                                text = "${if (isWeak) connectionTelemetry.latencyMs else latencyMs}ms",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = netColor
                            )
                            if (isWeak) {
                                Text(
                                    text = "WEAK",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    color = CncWarningAmber
                                )
                            }
                        }
                    }
                }

                // 7. Cybersecurity & G-Code Scanner Button
                IconButton(
                    onClick = onOpenCyberScanner,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(CncSurfaceVariant)
                        .border(1.dp, CncCyberCyan.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = stringResource(R.string.topbar_cyber_scanner_desc),
                        tint = CncCyberCyan,
                        modifier = Modifier.size(17.dp)
                    )
                }

                // 6. Speeds & Feeds Calculator Button
                IconButton(
                    onClick = onOpenCalculator,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(CncSurfaceVariant)
                        .border(1.dp, CncCardBorder, RoundedCornerShape(6.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Calculate,
                        contentDescription = stringResource(R.string.topbar_speeds_feeds_desc),
                        tint = CncWarningAmber,
                        modifier = Modifier.size(17.dp)
                    )
                }

                // 7. Tool Table & Pocket Manager Button
                IconButton(
                    onClick = onOpenToolTable,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(CncSurfaceVariant)
                        .border(1.dp, CncCyberCyan.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Construction,
                        contentDescription = stringResource(R.string.topbar_tool_table_desc),
                        tint = CncCyberCyan,
                        modifier = Modifier.size(17.dp)
                    )
                }

                // 8. Metrological Axis Calibration (ISO 230-2) Button
                IconButton(
                    onClick = onOpenAxisCalibration,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(CncSurfaceVariant)
                        .border(1.dp, CncActiveGreen.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Straighten,
                        contentDescription = stringResource(R.string.topbar_axis_metrology_desc),
                        tint = CncActiveGreen,
                        modifier = Modifier.size(17.dp)
                    )
                }

                // 9. In-App User Manual & Technical Reference Button
                IconButton(
                    onClick = onOpenManual,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(CncSurfaceVariant)
                        .border(1.dp, CncCyberCyan.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = stringResource(R.string.topbar_manual_desc),
                        tint = CncCyberCyan,
                        modifier = Modifier.size(17.dp)
                    )
                }

                // 9b. LinuxCNC Connection Wizard & Hardware Setup Tutorial Button
                IconButton(
                    onClick = onOpenConnectionWizard,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(CncSurfaceVariant)
                        .border(1.dp, CncActiveGreen.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Router,
                        contentDescription = stringResource(R.string.wizard_title),
                        tint = CncActiveGreen,
                        modifier = Modifier.size(17.dp)
                    )
                }

                // 10. HAL Signals & Pin Monitor Quick Access Button
                IconButton(
                    onClick = onOpenHalMonitor,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(CncSurfaceVariant)
                        .border(1.dp, CncCyberCyan.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.SettingsEthernet,
                        contentDescription = "HAL Signals Monitor",
                        tint = CncCyberCyan,
                        modifier = Modifier.size(17.dp)
                    )
                }

                // 11. Event & Alarm Logs Button with Badge
                IconButton(
                    onClick = onOpenLogs,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (errorCount > 0) CncEstopRed.copy(alpha = 0.2f) else CncSurfaceVariant)
                        .border(1.dp, if (errorCount > 0) CncEstopRed else CncCardBorder, RoundedCornerShape(6.dp))
                ) {
                    BadgedBox(
                        badge = {
                            if (errorCount > 0) {
                                Badge(
                                    containerColor = CncEstopRed,
                                    contentColor = Color.White
                                ) {
                                    Text(errorCount.toString(), fontSize = 8.sp)
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = stringResource(R.string.topbar_event_logs_desc),
                            tint = if (errorCount > 0) CncEstopRed else CncTextPrimary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(3.dp))

            CarouselNavButton(
                direction = ">",
                enabled = toolsScrollState.value < toolsScrollState.maxValue,
                height = 30.dp,
                width = 18.dp,
                onClick = {
                    coroutineScope.launch { toolsScrollState.animateScrollBy(130f) }
                }
            )
        }
        }
    }
}
