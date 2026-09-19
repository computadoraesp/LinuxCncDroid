package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.data.local.MachineProfileEntity
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.ScreenLockPortrait
import androidx.compose.material.icons.filled.StayCurrentPortrait
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import com.example.model.BatterySafetyState
import com.example.model.CapabilitiesManifest
import com.example.model.ConnectionTelemetry
import com.example.model.HardwareArchitecture
import com.example.model.ScreenTimeoutPolicy
import com.example.model.SimulatedFaultType
import com.example.ui.theme.CncActiveGreen
import com.example.ui.theme.CncCardBg
import com.example.ui.theme.CncCardBorder
import com.example.ui.theme.CncCyberCyan
import com.example.ui.theme.CncEstopRed
import com.example.ui.theme.CncSurface
import com.example.ui.theme.CncSurfaceVariant
import com.example.ui.theme.CncTextPrimary
import com.example.ui.theme.CncTextSecondary
import com.example.ui.theme.CncWarningAmber

@Composable
fun MachineConfigView(
    capabilities: CapabilitiesManifest,
    profiles: List<MachineProfileEntity>,
    onSwitchArchitecture: (HardwareArchitecture) -> Unit,
    onConnectHost: (String, Int) -> Unit,
    onSaveProfile: (String, String, Int, String) -> Unit,
    modifier: Modifier = Modifier,
    onDeleteProfile: (Long) -> Unit = {},
    onWipeAllData: () -> Unit = {},
    onOpenMetrologyCalibration: () -> Unit = {},
    onOpenManual: () -> Unit = {},
    onOpenHalMonitor: () -> Unit = {},
    onOpenWcsTable: () -> Unit = {},
    screenTimeoutPolicy: ScreenTimeoutPolicy = ScreenTimeoutPolicy.ALWAYS_ON,
    onSelectScreenTimeoutPolicy: (ScreenTimeoutPolicy) -> Unit = {},
    batterySafetyState: BatterySafetyState = BatterySafetyState(),
    // Centralized Simulation Panel Parameters
    isSimulatedMode: Boolean = true,
    onToggleSimulatedMode: (Boolean) -> Unit = {},
    connectionTelemetry: ConnectionTelemetry = ConnectionTelemetry(),
    onSimulateWeakSignal: (Boolean) -> Unit = {},
    onSimulateDisconnect: () -> Unit = {},
    onRestoreConnection: () -> Unit = {},
    onSimulateBattery: (Int, Boolean) -> Unit = { _, _ -> },
    onRestoreBattery: () -> Unit = {},
    onSimulateCharging: (Boolean) -> Unit = {},
    onInjectFault: (SimulatedFaultType) -> Unit = {},
    onSimulateProbeTouch: () -> Unit = {},
) {
    var hostIpText by remember { mutableStateOf(capabilities.hostIp) }
    var portText by remember { mutableStateOf(capabilities.port.toString()) }
    var profileNameText by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(value = false) }
    var showWipeConfirm by remember { mutableStateOf(value = false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = CncCardBg),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CncCardBorder)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Quick Tools Bar (Metrology & Manual)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onOpenMetrologyCalibration,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CncActiveGreen, containerColor = CncSurfaceVariant),
                    border = BorderStroke(1.dp, CncActiveGreen.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f).height(36.dp),
                ) {
                    Icon(imageVector = Icons.Default.Straighten, contentDescription = null, tint = CncActiveGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.config_metrology_calib_btn), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onOpenManual,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CncCyberCyan, containerColor = CncSurfaceVariant),
                    border = BorderStroke(1.dp, CncCyberCyan.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f).height(36.dp),
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = CncCyberCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.config_manual_btn), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.SettingsEthernet, contentDescription = "Config", tint = CncCyberCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.config_title), fontWeight = FontWeight.Black, fontSize = 12.sp, color = CncTextPrimary)
                }

                FilledTonalButton(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = CncSurfaceVariant,
                        contentColor = CncCyberCyan,
                    ),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(R.string.config_save_profile), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.config_save_profile), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            // App Identity & Icon Banner
            Surface(
                color = CncSurface,
                shape = RoundedCornerShape(8.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CncCardBorder)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = R.drawable.linuxcnc_droid_icon_1787496771237),
                        contentDescription = "LinuxCNC Droid Logo",
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, CncCyberCyan, RoundedCornerShape(10.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(stringResource(R.string.config_app_name), fontWeight = FontWeight.Black, fontSize = 14.sp, color = CncTextPrimary)
                        Text(stringResource(R.string.config_app_subtitle), fontSize = 11.sp, color = CncCyberCyan, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.config_app_tech_stack), fontSize = 10.sp, color = CncTextSecondary)
                    }
                }
            }

            // Section 1: Active Connection Settings (IP / Port / Connect)
            Surface(
                color = CncSurface,
                shape = RoundedCornerShape(8.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CncCardBorder)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.config_middleware_header), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CncTextSecondary)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = hostIpText,
                            onValueChange = { hostIpText = it },
                            label = { Text(stringResource(R.string.config_host_ip_label), fontSize = 10.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = CncCyberCyan,
                                unfocusedTextColor = CncTextPrimary
                            ),
                            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(2f)
                        )

                        OutlinedTextField(
                            value = portText,
                            onValueChange = { portText = it },
                            label = { Text(stringResource(R.string.config_port_label), fontSize = 10.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = CncCyberCyan,
                                unfocusedTextColor = CncTextPrimary
                            ),
                            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )

                        Button(
                            onClick = { onConnectHost(hostIpText, portText.toIntOrNull() ?: 8000) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CncCyberCyan,
                                contentColor = Color(0xFF00363D)
                            ),
                            modifier = Modifier.height(52.dp)
                        ) {
                            Text(stringResource(R.string.config_connect), fontWeight = FontWeight.Black, fontSize = 11.sp)
                        }
                    }
                }
            }

            // Section 2: Hardware Architecture Simulation & Overrides
            Text(stringResource(R.string.config_architecture), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CncTextSecondary)

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                HardwareArchitecture.entries.forEach { arch ->
                    val isSelected = capabilities.architecture == arch

                    Surface(
                        color = if (isSelected) CncSurfaceVariant else CncSurface,
                        shape = RoundedCornerShape(8.dp),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(if (isSelected) CncCyberCyan else CncCardBorder)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSwitchArchitecture(arch) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) CncCyberCyan else CncCardBorder)
                                ) {
                                    Text("L${arch.level}", fontWeight = FontWeight.Black, fontSize = 11.sp, color = if (isSelected) Color(0xFF00363D) else CncTextPrimary)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(stringResource(arch.displayNameRes), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = CncTextPrimary)
                                    Text(stringResource(arch.descriptionRes), fontSize = 10.sp, color = CncTextSecondary)
                                }
                            }

                            if (isSelected) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Active", tint = CncActiveGreen, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }

            // Section 3: Saved Machine Profiles List
            Text(stringResource(R.string.config_saved_profiles_header), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CncTextSecondary)

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                profiles.forEach { profile ->
                    Surface(
                        color = CncSurface,
                        shape = RoundedCornerShape(8.dp),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CncCardBorder)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                hostIpText = profile.hostIp
                                portText = profile.port.toString()
                                val archEnum = HardwareArchitecture.entries.find { it.name == profile.architecture } ?: HardwareArchitecture.ETHERCAT_DELTA
                                onSwitchArchitecture(archEnum)
                                onConnectHost(profile.hostIp, profile.port)
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(profile.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = CncTextPrimary)
                                Text("${profile.hostIp}:${profile.port} • ${profile.architecture}", fontSize = 10.sp, color = CncTextSecondary, fontFamily = FontFamily.Monospace)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(
                                    onClick = { onDeleteProfile(profile.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = CncEstopRed.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                                }
                                Icon(imageVector = Icons.Default.PlayCircleOutline, contentDescription = null, tint = CncCyberCyan)
                            }
                        }
                    }
                }
            }

            // Section 4: Display Sleep & Battery Fail-Safe Management
            Text(
                stringResource(R.string.config_safety_power_header),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = CncCyberCyan,
            )

            // Screen Sleep / Wake Lock Selector
            Text(
                stringResource(R.string.config_screen_timeout_label),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = CncTextSecondary,
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ScreenTimeoutPolicy.entries.forEach { policy ->
                    val isSelected = screenTimeoutPolicy == policy
                    Surface(
                        color = if (isSelected) CncSurfaceVariant else CncSurface,
                        shape = RoundedCornerShape(8.dp),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(if (isSelected) CncActiveGreen else CncCardBorder)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectScreenTimeoutPolicy(policy) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = when (policy) {
                                        ScreenTimeoutPolicy.ALWAYS_ON -> Icons.Default.ScreenLockPortrait
                                        ScreenTimeoutPolicy.MACHINE_ACTIVE -> Icons.Default.Power
                                        ScreenTimeoutPolicy.SYSTEM_TIMEOUT -> Icons.Default.StayCurrentPortrait
                                    },
                                    contentDescription = null,
                                    tint = if (isSelected) CncActiveGreen else CncTextSecondary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        stringResource(policy.displayNameRes),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = CncTextPrimary
                                    )
                                    Text(
                                        stringResource(policy.descriptionRes),
                                        fontSize = 10.sp,
                                        color = CncTextSecondary
                                    )
                                }
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Active",
                                    tint = CncActiveGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Battery Health & Safety Status Card
            Surface(
                color = CncSurface,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, CncCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                imageVector = when {
                                    batterySafetyState.isCharging -> Icons.Default.BatteryChargingFull
                                    batterySafetyState.isCriticalBattery || batterySafetyState.isLowBattery -> Icons.Default.BatteryAlert
                                    else -> Icons.Default.BatteryFull
                                },
                                contentDescription = null,
                                tint = when {
                                    batterySafetyState.isCriticalBattery -> CncEstopRed
                                    batterySafetyState.isLowBattery -> CncWarningAmber
                                    batterySafetyState.isCharging -> CncActiveGreen
                                    else -> CncCyberCyan
                                },
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = stringResource(R.string.config_battery_health_label),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = CncTextPrimary
                            )
                        }

                        Text(
                            text = "${batterySafetyState.levelPct}% ${if (batterySafetyState.isCharging) "(CARGANDO)" else ""}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (batterySafetyState.isLowBattery) CncWarningAmber else CncActiveGreen
                        )
                    }
                }
            }

            // -------------------------------------------------------------
            // SECTION 6: CENTRO DE CONTROL DE SIMULACIONES Y PRUEBAS
            // -------------------------------------------------------------
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                color = CncSurface,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.5.dp, if (isSimulatedMode) CncActiveGreen.copy(alpha = 0.5f) else CncCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header of Simulation Hub
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                color = if (isSimulatedMode) CncActiveGreen.copy(alpha = 0.2f) else CncSurfaceVariant,
                                shape = CircleShape,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Science,
                                        contentDescription = null,
                                        tint = if (isSimulatedMode) CncActiveGreen else CncTextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = stringResource(R.string.config_simulations_hub_header),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    color = CncTextPrimary
                                )
                                Text(
                                    text = stringResource(R.string.config_simulations_hub_desc),
                                    fontSize = 10.sp,
                                    color = CncTextSecondary
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = CncCardBorder, thickness = 1.dp)

                    // 1. MASTER CONTROLLER SIMULATION SWITCH
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CncCardBg, RoundedCornerShape(8.dp))
                            .border(1.dp, CncCardBorder, RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.config_sim_master_label),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = CncTextPrimary
                                )
                                Text(
                                    text = stringResource(R.string.config_sim_master_desc),
                                    fontSize = 9.sp,
                                    color = CncTextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = isSimulatedMode,
                                onCheckedChange = onToggleSimulatedMode,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = CncActiveGreen,
                                    checkedTrackColor = CncActiveGreen.copy(alpha = 0.3f),
                                    uncheckedThumbColor = CncTextSecondary,
                                    uncheckedTrackColor = CncSurfaceVariant
                                )
                            )
                        }

                        Surface(
                            color = if (isSimulatedMode) CncActiveGreen.copy(alpha = 0.15f) else CncCyberCyan.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(if (isSimulatedMode) CncActiveGreen else CncCyberCyan, CircleShape)
                                )
                                Text(
                                    text = if (isSimulatedMode) stringResource(R.string.config_sim_badge_active) else stringResource(R.string.config_sim_badge_real),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    color = if (isSimulatedMode) CncActiveGreen else CncCyberCyan
                                )
                            }
                        }
                    }

                    // 2. OPERATOR TABLET BATTERY SIMULATION
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CncCardBg, RoundedCornerShape(8.dp))
                            .border(1.dp, CncCardBorder, RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.config_sim_battery_section),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = CncTextPrimary
                                )
                                Text(
                                    text = stringResource(R.string.config_sim_battery_desc),
                                    fontSize = 9.sp,
                                    color = CncTextSecondary
                                )
                            }
                            if (batterySafetyState.isSimulated) {
                                Surface(
                                    color = CncWarningAmber.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "SIMULADO",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 8.sp,
                                        color = CncWarningAmber,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        // Battery Level Presets
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val levels = listOf(
                                Triple(8, stringResource(R.string.config_sim_battery_crit), CncEstopRed),
                                Triple(15, stringResource(R.string.config_sim_battery_low), CncWarningAmber),
                                Triple(50, stringResource(R.string.config_sim_battery_med), CncCyberCyan),
                                Triple(100, stringResource(R.string.config_sim_battery_full), CncActiveGreen),
                            )
                            levels.forEach { (pct, label, color) ->
                                val isCurrent = batterySafetyState.isSimulated && batterySafetyState.levelPct == pct
                                OutlinedButton(
                                    onClick = { onSimulateBattery(pct, batterySafetyState.isCharging) },
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isCurrent) color.copy(alpha = 0.2f) else Color.Transparent,
                                        contentColor = if (isCurrent) color else CncTextSecondary
                                    ),
                                    border = BorderStroke(1.dp, if (isCurrent) color else CncCardBorder),
                                    modifier = Modifier.weight(1f).height(32.dp),
                                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
                                ) {
                                    Text(label, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Charger Toggle & Restore Hardware Sensor
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { onSimulateCharging(!batterySafetyState.isCharging) },
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (batterySafetyState.isCharging) CncActiveGreen.copy(alpha = 0.15f) else Color.Transparent,
                                    contentColor = if (batterySafetyState.isCharging) CncActiveGreen else CncTextSecondary
                                ),
                                border = BorderStroke(1.dp, if (batterySafetyState.isCharging) CncActiveGreen else CncCardBorder),
                                modifier = Modifier.weight(1f).height(32.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = if (batterySafetyState.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.Power,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (batterySafetyState.isCharging) "CARGADOR: ON" else "CARGADOR: OFF",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            FilledTonalButton(
                                onClick = onRestoreBattery,
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = CncSurfaceVariant,
                                    contentColor = CncTextPrimary
                                ),
                                modifier = Modifier.weight(1f).height(32.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(stringResource(R.string.config_sim_battery_restore), fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // 3. NETWORK & LATENCY SIMULATION
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CncCardBg, RoundedCornerShape(8.dp))
                            .border(1.dp, CncCardBorder, RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.config_sim_network_section),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = CncTextPrimary
                        )
                        Text(
                            text = stringResource(R.string.config_sim_network_desc),
                            fontSize = 9.sp,
                            color = CncTextSecondary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onSimulateWeakSignal(!connectionTelemetry.isWeakSignal) },
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f).height(34.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (connectionTelemetry.isWeakSignal) CncWarningAmber.copy(alpha = 0.15f) else Color.Transparent,
                                    contentColor = if (connectionTelemetry.isWeakSignal) CncWarningAmber else CncTextSecondary
                                ),
                                border = BorderStroke(1.dp, if (connectionTelemetry.isWeakSignal) CncWarningAmber else CncCardBorder),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (connectionTelemetry.isWeakSignal) "SEÑAL DÉBIL (380ms)" else "SIMULAR SEÑAL DÉBIL",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    if (connectionTelemetry.isConnected && !connectionTelemetry.isReconnecting) {
                                        onSimulateDisconnect()
                                    } else {
                                        onRestoreConnection()
                                    }
                                },
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f).height(34.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (!connectionTelemetry.isConnected) CncEstopRed.copy(alpha = 0.15f) else Color.Transparent,
                                    contentColor = if (!connectionTelemetry.isConnected) CncActiveGreen else CncEstopRed
                                ),
                                border = BorderStroke(1.dp, if (!connectionTelemetry.isConnected) CncActiveGreen else CncEstopRed.copy(alpha = 0.5f)),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = if (!connectionTelemetry.isConnected) Icons.Default.CheckCircle else Icons.Default.WifiOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (!connectionTelemetry.isConnected) stringResource(R.string.config_sim_restore_network_btn) else stringResource(R.string.config_sim_drop_network_btn),
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // 4. INDUSTRIAL FAULT & ALARM INJECTION
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CncCardBg, RoundedCornerShape(8.dp))
                            .border(1.dp, CncCardBorder, RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = CncWarningAmber, modifier = Modifier.size(16.dp))
                            Text(
                                text = stringResource(R.string.config_sim_faults_section),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = CncTextPrimary
                            )
                        }
                        Text(
                            text = stringResource(R.string.config_sim_faults_desc),
                            fontSize = 9.sp,
                            color = CncTextSecondary
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            SimulatedFaultType.entries.forEach { fault ->
                                OutlinedButton(
                                    onClick = { onInjectFault(fault) },
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.fillMaxWidth().height(32.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = CncSurfaceVariant.copy(alpha = 0.5f),
                                        contentColor = when (fault) {
                                            SimulatedFaultType.LIMIT_SWITCH_X, SimulatedFaultType.SERVO_OVERTORQUE -> CncEstopRed
                                            SimulatedFaultType.SPINDLE_THERMAL, SimulatedFaultType.DOOR_INTERLOCK -> CncWarningAmber
                                            SimulatedFaultType.LOW_COOLANT -> CncCyberCyan
                                        }
                                    ),
                                    border = BorderStroke(1.dp, CncCardBorder),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(stringResource(fault.displayNameRes), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Text(stringResource(fault.descriptionRes), fontSize = 8.sp, color = CncTextSecondary)
                                    }
                                }
                            }
                        }
                    }

                    // 5. METROLOGY & 3D TOUCH PROBE SIMULATION
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CncCardBg, RoundedCornerShape(8.dp))
                            .border(1.dp, CncCardBorder, RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.config_sim_probe_section),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = CncTextPrimary
                        )
                        Text(
                            text = stringResource(R.string.config_sim_probe_desc),
                            fontSize = 9.sp,
                            color = CncTextSecondary
                        )

                        FilledTonalButton(
                            onClick = onSimulateProbeTouch,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth().height(36.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = CncCyberCyan.copy(alpha = 0.18f),
                                contentColor = CncCyberCyan
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Straighten, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.config_sim_probe_btn), fontSize = 9.5.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }

            // Pro Subsystems: WCS Table & HAL Diagnostics
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                color = CncSurface,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, CncCyberCyan.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "LINUXCNC PRO SUBSYSTEMS",
                        fontWeight = FontWeight.Black,
                        fontSize = 10.sp,
                        color = CncCyberCyan,
                        letterSpacing = 1.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onOpenWcsTable,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(40.dp)
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("WCS Table", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onOpenHalMonitor,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(40.dp)
                        ) {
                            Icon(Icons.Default.SettingsEthernet, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("HAL Pins", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Compliance: Data Management (GDPR/Play Store)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { showWipeConfirm = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CncEstopRed),
                border = BorderStroke(1.dp, CncEstopRed.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().height(40.dp)
            ) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.config_wipe_data_btn), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showWipeConfirm) {
        AlertDialog(
            onDismissRequest = { showWipeConfirm = false },
            title = { Text(stringResource(R.string.config_factory_reset_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.config_factory_reset_text)) },
            confirmButton = {
                Button(
                    onClick = {
                        onWipeAllData()
                        showWipeConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CncEstopRed)
                ) {
                    Text(stringResource(R.string.config_factory_reset_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeConfirm = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
            containerColor = CncCardBg
        )
    }

    // Add Profile Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(stringResource(R.string.config_add_profile_title), fontWeight = FontWeight.Bold, color = CncTextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = profileNameText,
                        onValueChange = { profileNameText = it },
                        label = { Text(stringResource(R.string.config_machine_name_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = hostIpText,
                        onValueChange = { hostIpText = it },
                        label = { Text(stringResource(R.string.config_ip_address_placeholder)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (profileNameText.isNotBlank()) {
                            onSaveProfile(profileNameText, hostIpText, portText.toIntOrNull() ?: 8000, capabilities.architecture.name)
                            showAddDialog = false
                            profileNameText = ""
                        }
                    }
                ) {
                    Text(stringResource(R.string.common_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
            containerColor = CncCardBg
        )
    }
}
