package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.model.EtherCatMasterInfo
import com.example.model.EtherCatSlaveInfo
import com.example.ui.theme.CncActiveGreen
import com.example.ui.theme.CncCardBg
import com.example.ui.theme.CncCardBorder
import com.example.ui.theme.CncCyberCyan
import com.example.ui.theme.CncEstopRed
import com.example.ui.theme.CncSurface
import com.example.ui.theme.CncSurfaceVariant
import com.example.ui.theme.CncTextMuted
import com.example.ui.theme.CncTextPrimary
import com.example.ui.theme.CncTextSecondary
import com.example.ui.theme.CncWarningAmber
import java.util.Locale

@Composable
fun EtherCatTelemetryView(
    masterInfo: EtherCatMasterInfo,
    slaves: List<EtherCatSlaveInfo>,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val isCompact = maxWidth < 600.dp

        Card(
            colors = CardDefaults.cardColors(containerColor = CncCardBg),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, CncCardBorder),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(if (isCompact) 10.dp else 12.dp),
                verticalArrangement = Arrangement.spacedBy(if (isCompact) 8.dp else 10.dp),
            ) {
                // Header (Adaptive for Portrait & Landscape)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Hub,
                            contentDescription = stringResource(R.string.tab_ethercat),
                            tint = CncActiveGreen,
                            modifier = Modifier.size(if (isCompact) 16.dp else 18.dp),
                        )
                        Spacer(modifier = Modifier.width(if (isCompact) 4.dp else 6.dp))
                        Text(
                            text = stringResource(R.string.ethercat_title),
                            fontWeight = FontWeight.Black,
                            fontSize = if (isCompact) 11.sp else 12.sp,
                            color = CncTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(CncActiveGreen.copy(alpha = 0.15f))
                            .border(1.dp, CncActiveGreen, RoundedCornerShape(6.dp))
                            .padding(
                                horizontal = if (isCompact) 6.dp else 8.dp,
                                vertical = if (isCompact) 3.dp else 4.dp,
                            ),
                    ) {
                        Text(
                            text = stringResource(R.string.ethercat_deterministic),
                            fontSize = if (isCompact) 8.5.sp else 9.sp,
                            fontWeight = FontWeight.Black,
                            color = CncActiveGreen,
                        )
                    }
                }

                // Master Bus Statistics Bar (Adaptive: 2x2 grid in vertical/compact, single-row in landscape)
                Surface(
                    color = CncSurface,
                    shape = RoundedCornerShape(8.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CncCardBorder)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (isCompact) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text(
                                        stringResource(R.string.ethercat_bus_cycle),
                                        fontSize = 8.5.sp,
                                        color = CncTextMuted,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        "${masterInfo.busCycleTimeUs} µs (1kHz)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = CncCyberCyan,
                                    )
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text(
                                        stringResource(R.string.ethercat_cycle_jitter),
                                        fontSize = 8.5.sp,
                                        color = CncTextMuted,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        "±${masterInfo.dcOffsetNs} ns",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = CncActiveGreen,
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text(
                                        stringResource(R.string.ethercat_slave_nodes),
                                        fontSize = 8.5.sp,
                                        color = CncTextMuted,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        "${masterInfo.slaveCount} ONLINE",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = CncTextPrimary,
                                    )
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text(
                                        stringResource(R.string.ethercat_lost_frames),
                                        fontSize = 8.5.sp,
                                        color = CncTextMuted,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        "0.00 %",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = CncActiveGreen,
                                    )
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(stringResource(R.string.ethercat_bus_cycle), fontSize = 9.sp, color = CncTextMuted, fontWeight = FontWeight.Bold)
                                Text("${masterInfo.busCycleTimeUs} µs (1kHz)", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = CncCyberCyan)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(stringResource(R.string.ethercat_cycle_jitter), fontSize = 9.sp, color = CncTextMuted, fontWeight = FontWeight.Bold)
                                Text("±${masterInfo.dcOffsetNs} ns", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = CncActiveGreen)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(stringResource(R.string.ethercat_slave_nodes), fontSize = 9.sp, color = CncTextMuted, fontWeight = FontWeight.Bold)
                                Text("${masterInfo.slaveCount} ONLINE", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = CncTextPrimary)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(stringResource(R.string.ethercat_lost_frames), fontSize = 9.sp, color = CncTextMuted, fontWeight = FontWeight.Bold)
                                Text("0.00 %", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = CncActiveGreen)
                            }
                        }
                    }
                }

            // Servo Drives Live Status Cards (Delta B3 / A3)
            Text(stringResource(R.string.ethercat_smart_drives), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CncTextSecondary)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                slaves.forEach { slave ->
                    val torque = slave.actualTorquePct
                    val isTorqueHigh = torque > 75.0
                    val isTorqueWarning = torque > 45.0

                    Surface(
                        color = CncSurface,
                        shape = RoundedCornerShape(8.dp),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CncCardBorder)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (slave.isFault) CncEstopRed else CncActiveGreen)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "NODE #${slave.slaveIndex}: ${slave.name}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CncTextPrimary
                                    )
                                }

                                Text(
                                    text = slave.state,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CncActiveGreen,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            // Torque Load Progress Bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(R.string.ethercat_torque_load, String.format(Locale.US, "%.1f", torque)),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isTorqueHigh) CncEstopRed else if (isTorqueWarning) CncWarningAmber else CncCyberCyan
                                )

                                Text(
                                    text = stringResource(R.string.ethercat_drive_temp, String.format(Locale.US, "%.1f", slave.driveTempC)),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = CncTextSecondary
                                )
                            }

                            LinearProgressIndicator(
                                progress = { (torque / 100.0).toFloat().coerceIn(0f, 1f) },
                                color = if (isTorqueHigh) CncEstopRed else if (isTorqueWarning) CncWarningAmber else CncCyberCyan,
                                trackColor = CncSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                            )

                            // Fault / Alarm Code
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(stringResource(R.string.ethercat_diagnostic_status), fontSize = 9.sp, color = CncTextMuted)
                                Text(
                                    text = slave.alarmCode,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (slave.isFault) CncEstopRed else CncActiveGreen
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
}
