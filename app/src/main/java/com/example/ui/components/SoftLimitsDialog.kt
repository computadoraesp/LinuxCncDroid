package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.SoftLimitsCheckResult
import java.util.Locale

@Composable
fun SoftLimitsDialog(
    checkResult: SoftLimitsCheckResult,
    onOpenWcsTable: () -> Unit,
    onForceCycleStart: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(16.dp))
                .testTag("soft_limits_dialog"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (checkResult.isWithinLimits) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (checkResult.isWithinLimits) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Soft Limits Trajectory Pre-Check",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Active Coordinate System: ${checkResult.activeWcs}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_limits_dialog_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Overall Status Banner
                val bannerColor = if (checkResult.isWithinLimits) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                val bannerBorder = if (checkResult.isWithinLimits) Color(0xFF81C784) else Color(0xFFE57373)
                val textColor = if (checkResult.isWithinLimits) Color(0xFF2E7D32) else Color(0xFFC62828)

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, bannerBorder, RoundedCornerShape(8.dp)),
                    color = bannerColor
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (checkResult.isWithinLimits) Icons.Default.VerifiedUser else Icons.Default.ReportProblem,
                            contentDescription = null,
                            tint = textColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (checkResult.isWithinLimits) "PROGRAM TRAJECTORY WITHIN SAFE TRAVEL ENVELOPE" else "CRITICAL: OVERTRAVEL LIMIT EXCEEDED",
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (checkResult.isWithinLimits)
                                    "The loaded toolpath fits completely within mechanical limits under ${checkResult.activeWcs}."
                                else
                                    "Physical limits will be violated during execution! Cycle Start is safety-locked.",
                                fontSize = 12.sp,
                                color = textColor.copy(alpha = 0.85f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Violations List (if any)
                if (!checkResult.isWithinLimits) {
                    Text(
                        text = "DETECTED OVERTRAVEL VIOLATIONS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.4f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(checkResult.violations) { v ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Axis ${v.axis} Overtravel",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        Text(
                                            text = "G53 Span: [${String.format(Locale.US, "%.2f", v.programMin)} .. ${String.format(Locale.US, "%.2f", v.programMax)}] mm",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = "Machine Limit: [${String.format(Locale.US, "%.2f", v.machineMinLimit)} .. ${String.format(Locale.US, "%.2f", v.machineMaxLimit)}] mm",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                    val excess = if (v.excessMinMm > 0) v.excessMinMm else v.excessMaxMm
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    ) {
                                        Text(
                                            text = String.format(Locale.US, "+%.2f mm", excess),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Bounding Box Details
                Text(
                    text = "PROGRAM BOUNDING BOX (WORK vs MACHINE ENVELOPE)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                val workBox = checkResult.boundingBoxWork
                val machBox = checkResult.boundingBoxMachine

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.6f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    BoundingBoxRow("X Axis", workBox.x.min, workBox.x.max, machBox.x.min, machBox.x.max)
                    BoundingBoxRow("Y Axis", workBox.y.min, workBox.y.max, machBox.y.min, machBox.y.max)
                    BoundingBoxRow("Z Axis", workBox.z.min, workBox.z.max, machBox.z.min, machBox.z.max)

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Estimated Toolpath Trajectory Length:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = String.format(Locale.US, "%.1f mm", workBox.totalMotionLengthMm),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onOpenWcsTable()
                        },
                        modifier = Modifier.weight(1f).testTag("adjust_wcs_limits_button")
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Adjust WCS Offset")
                    }

                    if (!checkResult.isWithinLimits) {
                        Button(
                            onClick = {
                                onDismiss()
                                onForceCycleStart()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f).testTag("override_limits_start_button")
                        ) {
                            Icon(Icons.Default.Dangerous, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Override & Start")
                        }
                    } else {
                        Button(
                            onClick = {
                                onDismiss()
                                onForceCycleStart()
                            },
                            modifier = Modifier.weight(1f).testTag("confirm_limits_start_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cycle Start")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BoundingBoxRow(
    axisName: String,
    workMin: Double,
    workMax: Double,
    machMin: Double,
    machMax: Double
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(axisName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = "Work: [${String.format(Locale.US, "%+7.2f", workMin)} .. ${String.format(Locale.US, "%+7.2f", workMax)}] mm",
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
        Text(
            text = "G53: [${String.format(Locale.US, "%+7.2f", machMin)} .. ${String.format(Locale.US, "%+7.2f", machMax)}] mm",
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.outline
        )
    }
}
