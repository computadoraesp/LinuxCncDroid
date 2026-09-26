package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.example.R
import com.example.model.*
import com.example.service.CncSecurityScanner
import com.example.ui.theme.*

@Composable
fun GCodeSecurityLoaderDialog(
    onDismiss: () -> Unit,
    onLoadValidatedGCode: (String, String) -> Unit,
) {
    val scanner = remember { CncSecurityScanner() }
    var selectedPresetTitle by remember { mutableStateOf("Trochoidal Aluminum Pocket (Safe)") }
    var gcodeContent by remember {
        mutableStateOf(
            """
            (SAFE HIGH-SPEED TROCHOIDAL MILLING 6061)
            G21 G90 G54
            G0 Z15.000
            G0 X10.000 Y10.000
            M3 S18000
            G1 Z-3.000 F400
            G1 X40.000 Y10.000 F2200
            G2 X50.000 Y20.000 I0.0 J10.0
            G1 X50.000 Y50.000
            G2 X40.000 Y60.000 I-10.0 J0.0
            G1 X10.000 Y60.000
            G2 X0.000 Y50.000 I0.0 J-10.0
            G1 X0.000 Y20.000
            G2 X10.000 Y10.000 I10.0 J0.0
            G0 Z20.000 M5
            G0 X0 Y0
            M30
            """.trimIndent()
        )
    }

    var scanResult by remember { mutableStateOf(scanner.scanGCode("trochoidal_pocket.ngc", gcodeContent)) }

    LaunchedEffect(gcodeContent) {
        scanResult = scanner.scanGCode("user_program.ngc", gcodeContent)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Shield, contentDescription = stringResource(R.string.sl_header), tint = CncCyberCyan, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.sl_header), fontWeight = FontWeight.Black, fontSize = 14.sp, color = CncTextPrimary)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Presets Bar
                Text(stringResource(R.string.sl_presets_header), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CncTextSecondary)
                val presetSafeTitle = stringResource(R.string.security_preset_safe)
                val presetTrojanTitle = stringResource(R.string.security_preset_trojan)
                val presetCollisionTitle = stringResource(R.string.security_preset_collision)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(
                        presetSafeTitle to """
                            (SAFE TROCHOIDAL TEST)
                            G21 G90 G54
                            G0 Z10.000
                            G0 X5 Y5
                            M3 S16000
                            G1 Z-2.000 F350
                            G1 X45 Y5 F1800
                            G1 X45 Y45
                            G1 X5 Y45
                            G1 X5 Y5
                            G0 Z15 M5
                            M30
                        """.trimIndent(),
                        presetTrojanTitle to """
                            #!/bin/bash
                            (INJECTED MALICIOUS SCRIPT DISGUISED AS G-CODE)
                            G21 G90
                            O<../../../../etc/shadow> CALL
                            (AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8gISIjJCUmJygpKissLS4vMDEyMzQ1Njc4OTo7PD0+P0BBQkNERUZHSElKS0xNTk9QUVJTVFVWV1hZWltcXV5fYGFiY2RlZmdoaWprbG1ub3BxcnN0dXZ3eHl6)
                            M102 P1 (TRIGGER HOST OS ROOT SCRIPT)
                            G0 X100 Y100
                            M30
                        """.trimIndent(),
                        presetCollisionTitle to """
                            (DANGEROUS COLLISION BENCHMARK)
                            G21 G90
                            G0 Z10.000
                            G1 Z-350.000 F2000 (EXTREME NEGATIVE PLUNGE INTO TABLE)
                            G1 X100 Y100
                            M30
                        """.trimIndent()
                    ).forEach { (label, code) ->
                        val isSelected = selectedPresetTitle == label
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSelected) CncCyberCyan else CncSurface)
                                .border(1.dp, CncCardBorder, RoundedCornerShape(4.dp))
                                .clickable {
                                    selectedPresetTitle = label
                                    gcodeContent = code
                                }
                        ) {
                            Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color(0xFF00363D) else CncTextPrimary, maxLines = 1)
                        }
                    }
                }

                // Security Analysis Status Banner
                val threatLevel = scanResult.threatLevel
                val bannerBg = when (threatLevel) {
                    ThreatLevel.CLEAN -> CncActiveGreen.copy(alpha = 0.15f)
                    ThreatLevel.SUSPICIOUS -> CncWarningAmber.copy(alpha = 0.15f)
                    ThreatLevel.MALWARE_BLOCKED -> CncEstopRed.copy(alpha = 0.2f)
                }
                val bannerBorder = when (threatLevel) {
                    ThreatLevel.CLEAN -> CncActiveGreen
                    ThreatLevel.SUSPICIOUS -> CncWarningAmber
                    ThreatLevel.MALWARE_BLOCKED -> CncEstopRed
                }

                Surface(
                    color = bannerBg,
                    shape = RoundedCornerShape(8.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(bannerBorder)),
                    modifier = Modifier.fillMaxWidth()
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
                                imageVector = when (threatLevel) {
                                    ThreatLevel.CLEAN -> Icons.Default.VerifiedUser
                                    ThreatLevel.SUSPICIOUS -> Icons.Default.WarningAmber
                                    ThreatLevel.MALWARE_BLOCKED -> Icons.Default.GppBad
                                },
                                contentDescription = stringResource(threatLevel.displayNameRes),
                                tint = bannerBorder,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(stringResource(threatLevel.displayNameRes), fontWeight = FontWeight.Black, fontSize = 12.sp, color = bannerBorder)
                                val motionText = if (scanResult.hasMotion) stringResource(R.string.security_motion_detected) else stringResource(R.string.security_no_motion)
                                Text(stringResource(R.string.sl_threats_info, scanResult.threats.size, motionText, scanResult.totalLines), fontSize = 10.sp, color = CncTextSecondary)
                            }
                        }

                        Text(
                            text = if (scanResult.isExecutable) stringResource(R.string.sl_pass) else stringResource(R.string.sl_block),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = if (scanResult.isExecutable) CncActiveGreen else CncEstopRed
                        )
                    }
                }

                // Detailed Threats List (if any)
                if (scanResult.threats.isNotEmpty()) {
                    Text(stringResource(R.string.sl_analysis_header), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CncTextSecondary)
                    Surface(
                        color = CncSurface,
                        shape = RoundedCornerShape(6.dp),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CncCardBorder)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                    ) {
                        LazyColumn(modifier = Modifier.padding(6.dp)) {
                            items(scanResult.threats) { threat ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "[${threat.code}]",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (threat.severity == LogSeverity.CRITICAL) CncEstopRed else CncWarningAmber
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Line ${threat.lineNumber}: ${threat.title}",
                                        fontSize = 10.sp,
                                        color = CncTextPrimary,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                // Hash & Envelope Metrics
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "SHA-256: ${scanResult.sha256Fingerprint.take(16)}...",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = CncTextMuted
                    )
                    Text(
                        "Box: X[${scanResult.boundingBoxX.second.toInt()}] Y[${scanResult.boundingBoxY.second.toInt()}] Z[${scanResult.boundingBoxZ.first.toInt()}]",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = CncCyberCyan
                    )
                }

                // GCode Editor Box
                OutlinedTextField(
                    value = gcodeContent,
                    onValueChange = { gcodeContent = it },
                    label = { Text(stringResource(R.string.gcode_script_content_label), fontSize = 10.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = CncCyberCyan,
                        unfocusedTextColor = CncTextPrimary,
                        focusedContainerColor = CncSurface,
                        unfocusedContainerColor = CncSurface
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (scanResult.isExecutable) {
                        onLoadValidatedGCode("program.ngc", gcodeContent)
                        onDismiss()
                    }
                },
                enabled = scanResult.isExecutable,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CncCyberCyan,
                    contentColor = Color(0xFF00363D),
                    disabledContainerColor = CncSurfaceVariant,
                    disabledContentColor = CncTextMuted
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = stringResource(R.string.sl_load_btn))
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.sl_load_btn), fontWeight = FontWeight.Black, fontSize = 11.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel), color = CncTextSecondary)
            }
        },
        containerColor = CncCardBg
    )
}
