package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.model.CoolantInfo
import com.example.model.ExecutionModifiers
import com.example.model.FeedInfo
import com.example.model.MachineStateEnum
import com.example.model.SpindleInfo
import com.example.model.UnitSystem
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun SpindleFeedPanel(
    spindle: SpindleInfo,
    feed: FeedInfo,
    coolant: CoolantInfo,
    machineState: MachineStateEnum,
    onToggleSpindle: () -> Unit,
    onSetSpindleRpm: (Double) -> Unit,
    onSpindleOverride: (Int) -> Unit,
    onFeedOverride: (Int) -> Unit,
    onToggleMist: () -> Unit,
    onToggleFlood: () -> Unit,
    onCycleStart: () -> Unit,
    onFeedHold: () -> Unit,
    onCycleStop: () -> Unit,
    modifier: Modifier = Modifier,
    unitSystem: UnitSystem = UnitSystem.METRIC,
    executionModifiers: ExecutionModifiers = ExecutionModifiers(),
    onToggleSingleBlock: (Boolean) -> Unit = {},
    onToggleOptionalStop: (Boolean) -> Unit = {},
    onToggleBlockDelete: (Boolean) -> Unit = {},
    onSingleBlockStep: () -> Unit = {},
    onOpenRunFromLine: () -> Unit = {},
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CncCardBg),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CncCardBorder)),
        modifier = modifier.fillMaxWidth(),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isNarrow = maxWidth < 480.dp

            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // Section 1: Cycle Execution Controls (START / PAUSE / STOP)
                val isRunning = machineState == MachineStateEnum.RUNNING
                val isPaused = machineState == MachineStateEnum.PAUSED

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // CYCLE START
                    Button(
                        onClick = onCycleStart,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRunning) CncActiveGreen else Color(0xFF005327),
                            contentColor = if (isRunning) Color(0xFF00381B) else Color.White
                        ),
                        border = if (!isRunning) BorderStroke(1.dp, CncActiveGreen.copy(alpha = 0.6f)) else null,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
                        modifier = Modifier.weight(1f).height(if (isNarrow) 54.dp else 44.dp)
                    ) {
                        if (isNarrow) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = stringResource(R.string.btn_cycle_start),
                                    tint = if (isRunning) Color(0xFF00381B) else CncActiveGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    text = if (isRunning) "RUNNING" else "CYCLE\nSTART",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp,
                                    lineHeight = 11.sp,
                                    textAlign = TextAlign.Center,
                                    color = if (isRunning) Color(0xFF00381B) else Color.White
                                )
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = stringResource(R.string.btn_cycle_start), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = if (isRunning) stringResource(R.string.state_running_disp) else stringResource(R.string.btn_cycle_start), fontWeight = FontWeight.Black, fontSize = 11.sp)
                            }
                        }
                    }

                    // FEEDHOLD / PAUSE
                    Button(
                        onClick = onFeedHold,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPaused) CncWarningAmber else Color(0xFF422D00),
                            contentColor = if (isPaused) Color.Black else Color(0xFFFFE082)
                        ),
                        border = BorderStroke(1.dp, if (isPaused) Color(0xFFFFD54F) else CncWarningAmber.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
                        modifier = Modifier.weight(1f).height(if (isNarrow) 54.dp else 44.dp)
                    ) {
                        if (isNarrow) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Pause,
                                    contentDescription = stringResource(R.string.btn_feed_hold),
                                    tint = if (isPaused) Color.Black else CncWarningAmber,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    text = if (isPaused) "PAUSED" else "FEED\nHOLD",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp,
                                    lineHeight = 11.sp,
                                    textAlign = TextAlign.Center,
                                    color = if (isPaused) Color.Black else Color(0xFFFFE082)
                                )
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(imageVector = Icons.Default.Pause, contentDescription = stringResource(R.string.btn_feed_hold), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = stringResource(R.string.btn_feed_hold), fontWeight = FontWeight.Black, fontSize = 11.sp)
                            }
                        }
                    }

                    // STOP / ABORT
                    Button(
                        onClick = onCycleStop,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6E0017),
                            contentColor = Color.White
                        ),
                        border = BorderStroke(1.dp, CncEstopRed.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
                        modifier = Modifier.weight(1f).height(if (isNarrow) 54.dp else 44.dp)
                    ) {
                        if (isNarrow) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = stringResource(R.string.btn_cycle_stop),
                                    tint = CncEstopRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    text = "CYCLE\nSTOP",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp,
                                    lineHeight = 11.sp,
                                    textAlign = TextAlign.Center,
                                    color = Color.White
                                )
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(imageVector = Icons.Default.Stop, contentDescription = stringResource(R.string.btn_cycle_stop), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = stringResource(R.string.btn_cycle_stop), fontWeight = FontWeight.Black, fontSize = 11.sp)
                            }
                        }
                    }
                }

                // PRO SECTION 1B: Industrial Execution Modifiers (Single Block, Opt Stop, Block Delete, Run From Line)
                if (isNarrow) {
                    // Two-row responsive layout for narrow/portrait
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Row 1: The 3 execution modifiers chips evenly spaced
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // SINGLE BLOCK
                            Surface(
                                onClick = { onToggleSingleBlock(!executionModifiers.singleBlockMode) },
                                shape = RoundedCornerShape(6.dp),
                                color = if (executionModifiers.singleBlockMode) CncCyberCyan.copy(alpha = 0.25f) else CncSurfaceVariant,
                                border = BorderStroke(1.dp, if (executionModifiers.singleBlockMode) CncCyberCyan else CncCardBorder),
                                modifier = Modifier.weight(1f).height(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Text(
                                        "SINGLE (M0)",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (executionModifiers.singleBlockMode) CncCyberCyan else CncTextSecondary,
                                        maxLines = 1
                                    )
                                }
                            }

                            // OPT STOP M1
                            Surface(
                                onClick = { onToggleOptionalStop(!executionModifiers.optionalStopM1) },
                                shape = RoundedCornerShape(6.dp),
                                color = if (executionModifiers.optionalStopM1) CncWarningAmber.copy(alpha = 0.25f) else CncSurfaceVariant,
                                border = BorderStroke(1.dp, if (executionModifiers.optionalStopM1) CncWarningAmber else CncCardBorder),
                                modifier = Modifier.weight(1f).height(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Text(
                                        "OPT STOP",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (executionModifiers.optionalStopM1) CncWarningAmber else CncTextSecondary,
                                        maxLines = 1
                                    )
                                }
                            }

                            // BLOCK DELETE
                            Surface(
                                onClick = { onToggleBlockDelete(!executionModifiers.blockDelete) },
                                shape = RoundedCornerShape(6.dp),
                                color = if (executionModifiers.blockDelete) Color(0xFF64B5F6).copy(alpha = 0.25f) else CncSurfaceVariant,
                                border = BorderStroke(1.dp, if (executionModifiers.blockDelete) Color(0xFF64B5F6) else CncCardBorder),
                                modifier = Modifier.weight(1f).height(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Text(
                                        "BLOCK DEL",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (executionModifiers.blockDelete) Color(0xFF64B5F6) else CncTextSecondary,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        // Row 2: RUN FROM LINE Button prominent and full width
                        Surface(
                            onClick = onOpenRunFromLine,
                            shape = RoundedCornerShape(8.dp),
                            color = CncCyberCyan.copy(alpha = 0.12f),
                            border = BorderStroke(1.5.dp, CncCyberCyan.copy(alpha = 0.7f)),
                            modifier = Modifier.fillMaxWidth().height(40.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RestartAlt,
                                    contentDescription = null,
                                    tint = CncCyberCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "RUN FROM LINE… (INICIAR DESDE LÍNEA)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = CncCyberCyan,
                                    letterSpacing = 0.5.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                } else {
                    // Wide / Landscape inline layout
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // SINGLE BLOCK
                        FilterChip(
                            selected = executionModifiers.singleBlockMode,
                            onClick = { onToggleSingleBlock(!executionModifiers.singleBlockMode) },
                            label = { Text("SINGLE (M0)", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CncCyberCyan.copy(alpha = 0.25f),
                                selectedLabelColor = CncCyberCyan,
                                labelColor = CncTextSecondary
                            ),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(32.dp)
                        )

                        // OPT STOP M1
                        FilterChip(
                            selected = executionModifiers.optionalStopM1,
                            onClick = { onToggleOptionalStop(!executionModifiers.optionalStopM1) },
                            label = { Text("OPT STOP (M1)", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CncWarningAmber.copy(alpha = 0.25f),
                                selectedLabelColor = CncWarningAmber,
                                labelColor = CncTextSecondary
                            ),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(32.dp)
                        )

                        // BLOCK DELETE
                        FilterChip(
                            selected = executionModifiers.blockDelete,
                            onClick = { onToggleBlockDelete(!executionModifiers.blockDelete) },
                            label = { Text("BLOCK DEL (/)", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF64B5F6).copy(alpha = 0.25f),
                                selectedLabelColor = Color(0xFF64B5F6),
                                labelColor = CncTextSecondary
                            ),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(32.dp)
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        // RUN FROM LINE BUTTON
                        OutlinedButton(
                            onClick = onOpenRunFromLine,
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = CncCyberCyan.copy(alpha = 0.08f),
                                contentColor = CncCyberCyan
                            ),
                            border = BorderStroke(1.dp, CncCyberCyan.copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null, tint = CncCyberCyan, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("FROM LINE…", fontSize = 10.sp, fontWeight = FontWeight.Black, color = CncCyberCyan)
                        }
                    }
                }

            // Single Block Step button when Single Block is active
            if (executionModifiers.singleBlockMode) {
                Button(
                    onClick = onSingleBlockStep,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CncCyberCyan,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth().height(36.dp)
                ) {
                    Icon(imageVector = Icons.Default.FastForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("SINGLE BLOCK: EXECUTE NEXT LINE >", fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
            }

            HorizontalDivider(Modifier, DividerDefaults.Thickness, color = CncCardBorder)

            // Section 2: Spindle Control & RPM
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.RotateRight, contentDescription = stringResource(R.string.spindle_motor_header), tint = CncWarningAmber, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.spindle_motor_header), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = CncTextPrimary)
                    }

                    // Spindle Toggle Button
                    FilledTonalButton(
                        onClick = onToggleSpindle,
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (spindle.isEnabled) CncActiveGreen else CncSurfaceVariant,
                            contentColor = if (spindle.isEnabled) Color.Black else CncTextSecondary
                        ),
                        border = if (spindle.isEnabled) BorderStroke(2.dp, CncActiveGreenGlow) else null,
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text(if (spindle.isEnabled) stringResource(R.string.spindle_on) else stringResource(R.string.spindle_off), fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                }

                // RPM Readout (Actual vs Commanded)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(CncSurface)
                        .border(
                            width = if (spindle.isEnabled) 1.dp else 0.dp,
                            color = if (spindle.isEnabled) CncWarningAmberGlow else Color.Transparent,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(stringResource(R.string.spindle_actual_rpm), fontSize = 9.sp, color = CncTextMuted, fontWeight = FontWeight.Bold)
                        Text(
                            text = String.format(Locale.US, "%05.0f", spindle.actualRpm),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = if (spindle.isEnabled) CncWarningAmber else CncTextSecondary
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(stringResource(R.string.spindle_set_rpm, spindle.commandedRpm.toInt()), fontSize = 10.sp, color = CncTextSecondary, fontFamily = FontFamily.Monospace)
                        Text(stringResource(R.string.spindle_override_label, spindle.overridePct), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CncCyberCyan, fontFamily = FontFamily.Monospace)
                    }
                }

                // Spindle Presets
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(3000.0, 6000.0, 12000.0, 18000.0, 24000.0).forEach { presetRpm ->
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .height(26.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(CncSurfaceVariant)
                                .border(1.dp, CncCardBorder, RoundedCornerShape(4.dp))
                                .clickable { onSetSpindleRpm(presetRpm) }
                        ) {
                            Text("${(presetRpm / 1000).toInt()}k", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CncTextPrimary)
                        }
                    }
                }

                // Spindle Override Slider
                Slider(
                    value = spindle.overridePct.toFloat(),
                    onValueChange = { onSpindleOverride(it.toInt()) },
                    valueRange = 10f..200f,
                    steps = 19,
                    colors = SliderDefaults.colors(
                        thumbColor = CncWarningAmber,
                        activeTrackColor = CncWarningAmber,
                        inactiveTrackColor = CncSurfaceVariant
                    ),
                    modifier = Modifier.height(24.dp)
                )
            }

            HorizontalDivider(Modifier, DividerDefaults.Thickness, color = CncCardBorder)

            // Section 3: Feedrate Override & Coolant
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val effectiveFeed = feed.commandedFeed * (feed.feedOverridePct / 100.0)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.feed_override), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = CncTextPrimary)
                    Text(
                        stringResource(R.string.spindle_feed_info, feed.feedOverridePct, unitSystem.formatSpeed(effectiveFeed)),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = CncCyberCyan
                    )
                }

                Slider(
                    value = feed.feedOverridePct.toFloat(),
                    onValueChange = { onFeedOverride(it.toInt()) },
                    valueRange = 0f..200f,
                    steps = 20,
                    colors = SliderDefaults.colors(
                        thumbColor = CncCyberCyan,
                        activeTrackColor = CncCyberCyan,
                        inactiveTrackColor = CncSurfaceVariant
                    ),
                    modifier = Modifier.height(24.dp)
                )

                // Coolant Switches (Mist M7 & Flood M8)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = onToggleMist,
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (coolant.mist) Color(0xFF004F58) else CncSurfaceVariant,
                            contentColor = if (coolant.mist) CncCyberCyan else CncTextSecondary
                        ),
                        modifier = Modifier.weight(1f).height(34.dp)
                    ) {
                        Icon(imageVector = Icons.Default.WaterDrop, contentDescription = stringResource(R.string.coolant_mist), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (coolant.mist) stringResource(R.string.coolant_mist) else stringResource(R.string.coolant_mist_off), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    FilledTonalButton(
                        onClick = onToggleFlood,
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (coolant.flood) Color(0xFF00363D) else CncSurfaceVariant,
                            contentColor = if (coolant.flood) CncCyberCyan else CncTextSecondary
                        ),
                        modifier = Modifier.weight(1f).height(34.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Waves, contentDescription = stringResource(R.string.coolant_flood), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (coolant.flood) stringResource(R.string.coolant_flood) else stringResource(R.string.coolant_flood_off), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
}
