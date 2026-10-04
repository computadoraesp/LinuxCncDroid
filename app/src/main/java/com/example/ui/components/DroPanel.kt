package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.testTag
import com.example.R
import com.example.model.AxisCoord
import com.example.model.MachineStateEnum
import com.example.model.UnitSystem
import com.example.ui.theme.*
import java.util.Locale

enum class DroDisplayMode {
    WORK, MACHINE, DTG
}

@Composable
fun DroPanel(
    modifier: Modifier = Modifier,
    machineState: MachineStateEnum = MachineStateEnum.IDLE,
    axes: Map<String, AxisCoord>,
    currentCoordSystem: String,
    hasServoTorque: Boolean,
    unitSystem: UnitSystem = UnitSystem.METRIC,
    onZeroAxis: (String) -> Unit,
    onSetAxisPosition: (axis: String, position: Double) -> Unit = { _, _ -> },
    onZeroAll: () -> Unit,
    onHomeAxis: (String) -> Unit,
    onHomeAll: () -> Unit,
    onOpenWcsTable: () -> Unit = {},
) {
    var displayMode by remember { mutableStateOf(DroDisplayMode.WORK) }
    var presetTargetAxis by remember { mutableStateOf<String?>(null) }

    val isEnabled = machineState != MachineStateEnum.RUNNING && machineState != MachineStateEnum.ESTOP && machineState != MachineStateEnum.ERROR

    Card(
        colors = CardDefaults.cardColors(containerColor = CncCardBg),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CncCardBorder)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // DRO Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = stringResource(R.string.dro_title),
                        tint = CncCyberCyan,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.dro_title),
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        letterSpacing = 0.5.sp,
                        color = CncTextPrimary,
                    )
                }

                // WCS Selector / Config Chip
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CncCyberCyan.copy(alpha = 0.6f)),
                    modifier = Modifier.clickable(onClick = onOpenWcsTable)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(currentCoordSystem, fontSize = 11.sp, fontWeight = FontWeight.Black, color = CncCyberCyan)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.Tune, contentDescription = stringResource(R.string.wcs_dialog_title), tint = CncCyberCyan, modifier = Modifier.size(13.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Coordinate System Mode Selector Tabs (WORK / MACHINE / DTG) - clean horizontal full-width row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(CncSurfaceVariant)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    DroDisplayMode.WORK to stringResource(R.string.dro_work_pos, currentCoordSystem),
                    DroDisplayMode.MACHINE to stringResource(R.string.dro_mach_pos),
                    DroDisplayMode.DTG to stringResource(R.string.dro_dtg),
                ).forEach { (mode, label) ->
                    val isSelected = displayMode == mode
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) CncCyberCyan else Color.Transparent)
                            .clickable { displayMode = mode }
                            .padding(vertical = 6.dp, horizontal = 2.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color(0xFF00363D) else CncTextSecondary,
                            maxLines = 1,
                            softWrap = false,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Axes List
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                axes.values.forEach { axis ->
                    DroAxisRow(
                        axis = axis,
                        displayMode = displayMode,
                        hasServoTorque = hasServoTorque,
                        unitSystem = unitSystem,
                        enabled = isEnabled,
                        onZero = { onZeroAxis(axis.name) },
                        onSetPosition = { presetTargetAxis = axis.name },
                        onHome = { onHomeAxis(axis.name) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bulk Actions (Zero All, Home All)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onZeroAll,
                    enabled = isEnabled,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = CncSurfaceVariant,
                        contentColor = CncCyberCyan
                    ),
                    modifier = Modifier.weight(1f).height(38.dp)
                ) {
                    Icon(imageVector = Icons.Default.Adjust, contentDescription = stringResource(R.string.dro_zero_all), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.dro_zero_all), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onHomeAll,
                    enabled = isEnabled,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = CncSurfaceVariant,
                        contentColor = CncActiveGreen
                    ),
                    modifier = Modifier.weight(1f).height(38.dp)
                ) {
                    Icon(imageVector = Icons.Default.Home, contentDescription = stringResource(R.string.dro_home_all), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.dro_home_all), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    presetTargetAxis?.let { axisName ->
        val currentWorkPos = axes[axisName]?.workPos ?: 0.0
        PresetAxisCoordDialog(
            axisName = axisName,
            initialValue = currentWorkPos,
            unitLabel = if (unitSystem == UnitSystem.METRIC) "mm" else "in",
            onDismiss = { presetTargetAxis = null },
            onConfirm = { newPos ->
                onSetAxisPosition(axisName, newPos)
                presetTargetAxis = null
            }
        )
    }
}

@Composable
fun DroAxisRow(
    axis: AxisCoord,
    displayMode: DroDisplayMode,
    hasServoTorque: Boolean,
    unitSystem: UnitSystem = UnitSystem.METRIC,
    enabled: Boolean = true,
    onZero: () -> Unit,
    onSetPosition: () -> Unit,
    onHome: () -> Unit,
) {
    val axisColor = when (axis.name) {
        "X" -> AxisXColor
        "Y" -> AxisYColor
        "Z" -> AxisZColor
        "A" -> AxisAColor
        "B" -> AxisBColor
        else -> AxisCColor
    }

    val isRotaryAxis = axis.name in listOf("A", "B", "C")

    val valueToDisplay = when (displayMode) {
        DroDisplayMode.WORK -> axis.workPos
        DroDisplayMode.MACHINE -> axis.machinePos
        DroDisplayMode.DTG -> axis.dtgPos
    }

    val formattedNumber = if (isRotaryAxis) {
        String.format(Locale.US, "%+08.3f", valueToDisplay)
    } else {
        unitSystem.formatPosition(valueToDisplay)
    }

    val unitLabel = if (isRotaryAxis) "°" else unitSystem.lengthUnit

    Surface(
        color = CncSurface,
        shape = RoundedCornerShape(8.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CncCardBorder.copy(alpha = 0.5f))),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Axis Badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(axisColor.copy(alpha = 0.2f))
                            .border(1.5.dp, axisColor, RoundedCornerShape(6.dp))
                    ) {
                        Text(
                            text = axis.name,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = axisColor,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Home indicator dot
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (axis.isHomed) CncActiveGreen else CncEstopRed)
                            .border(
                                1.5.dp,
                                if (axis.isHomed) CncActiveGreenGlow else CncEstopGlow,
                                CircleShape,
                            ),
                    )
                }

                // Numeric Coordinate Display (Large Digital Font) + Unit
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.clickable(enabled = enabled, onClick = onSetPosition)
                ) {
                    Text(
                        text = formattedNumber,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = CncDroDigits,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = unitLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = CncTextSecondary,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }

                // Quick Action Buttons (Zero, Set & Home)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilledTonalButton(
                        onClick = onZero,
                        enabled = enabled,
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (enabled) CncSurfaceVariant else CncSurfaceVariant.copy(alpha = 0.5f),
                            contentColor = if (enabled) CncCyberCyan else CncTextMuted
                        ),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(stringResource(R.string.common_zero), fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }

                    FilledTonalButton(
                        onClick = onSetPosition,
                        enabled = enabled,
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (enabled) CncSurfaceVariant else CncSurfaceVariant.copy(alpha = 0.5f),
                            contentColor = if (enabled) CncWarningAmber else CncTextMuted
                        ),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(stringResource(R.string.dro_btn_touch_off), fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }

                    IconButton(
                        onClick = onHome,
                        enabled = enabled,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (enabled) CncSurfaceVariant else CncSurfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = stringResource(R.string.dro_home_axis_desc, axis.name),
                            tint = if (enabled && axis.isHomed) CncActiveGreen else if (enabled) CncTextSecondary else CncTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Level 3 Smart Servo Telemetry Sub-row (Torque load & Temp)
            AnimatedVisibility(visible = hasServoTorque) {
                Column(modifier = Modifier.padding(top = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.dro_torque, String.format(Locale.US, "%.1f", axis.loadTorquePct)),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (axis.loadTorquePct > 80) CncEstopRed else if (axis.loadTorquePct > 50) CncWarningAmber else CncTextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = stringResource(R.string.dro_drive_temp, String.format(Locale.US, "%.1f", axis.driveTempC)),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CncTextSecondary
                            )
                            val counts = if (axis.encoderCounts != 0L) axis.encoderCounts else (axis.machinePos * 10000.0).toLong()
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "ENC: $counts",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CncTextSecondary
                            )
                        }

                        // Mini Load Bar
                        LinearProgressIndicator(
                            progress = { (axis.loadTorquePct / 100.0).toFloat().coerceIn(0f, 1f) },
                            color = if (axis.loadTorquePct > 80) CncEstopRed else if (axis.loadTorquePct > 50) CncWarningAmber else axisColor,
                            trackColor = CncSurfaceVariant,
                            modifier = Modifier
                                .width(80.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PresetAxisCoordDialog(
    axisName: String,
    initialValue: Double,
    unitLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit,
) {
    var textValue by remember { mutableStateOf(String.format(Locale.US, "%.4f", initialValue)) }
    val axisColor = when (axisName) {
        "X" -> AxisXColor
        "Y" -> AxisYColor
        "Z" -> AxisZColor
        "A" -> AxisAColor
        "B" -> AxisBColor
        else -> AxisCColor
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CncSurface),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, axisColor.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(axisColor.copy(alpha = 0.2f))
                                .border(1.dp, axisColor, RoundedCornerShape(6.dp))
                        ) {
                            Text(axisName, fontWeight = FontWeight.Black, fontSize = 14.sp, color = axisColor)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.dro_set_axis_title, axisName),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            color = CncTextPrimary
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = CncTextMuted, modifier = Modifier.size(16.dp))
                    }
                }

                Text(
                    text = stringResource(R.string.dro_touch_off_prompt),
                    fontSize = 10.sp,
                    color = CncTextSecondary
                )

                // Input Field
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = LocalTextStyle.current.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = CncDroDigits
                    ),
                    suffix = { Text(unitLabel, fontSize = 12.sp, color = CncTextSecondary, fontWeight = FontWeight.Bold) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CncSurfaceVariant,
                        unfocusedContainerColor = CncSurfaceVariant,
                        focusedBorderColor = axisColor,
                        unfocusedBorderColor = CncCardBorder,
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("dro_preset_input")
                )

                // Quick Preset Chips (common CNC edge finding offsets)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(0.0, -5.0, 5.0, -10.0, 10.0, 25.0).forEach { presetVal ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = CncSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, CncCardBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { textValue = String.format(Locale.US, "%.4f", presetVal) }
                        ) {
                            Text(
                                text = (if (presetVal > 0) "+$presetVal" else "$presetVal").removeSuffix(".0"),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = CncCyberCyan,
                                modifier = Modifier.padding(vertical = 4.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Text(stringResource(R.string.common_cancel), fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            val parsed = textValue.toDoubleOrNull() ?: 0.0
                            onConfirm(parsed)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = axisColor),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(40.dp).testTag("dro_preset_confirm")
                    ) {
                        Text(stringResource(R.string.common_apply), fontWeight = FontWeight.Black, fontSize = 11.sp, color = Color.Black)
                    }
                }
            }
        }
    }
}

