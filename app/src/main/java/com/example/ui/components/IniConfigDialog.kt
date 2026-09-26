package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.service.LinuxCncMachineConfig
import com.example.service.LinuxCncIniParser
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IniConfigDialog(
    onDismiss: () -> Unit,
    onApplyConfig: (LinuxCncMachineConfig) -> Unit,
) {
    var rawIniText by remember { mutableStateOf(LinuxCncIniParser.generateSampleIni()) }
    var parsedConfig by remember { mutableStateOf(LinuxCncIniParser.parseIni(rawIniText)) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Summary, 1 = Raw Editor
    var appliedSuccess by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            val isCompact = maxWidth < 600.dp

            Card(
                colors = CardDefaults.cardColors(containerColor = CncSurfaceBg),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(CncCardBorder)),
                modifier = Modifier
                    .fillMaxWidth(if (isCompact) 1f else 0.94f)
                    .fillMaxHeight(if (isCompact) 0.96f else 0.92f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(if (isCompact) 10.dp else 16.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = CncCyberCyan,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.ini_editor_title),
                                    fontWeight = FontWeight.Black,
                                    fontSize = if (isCompact) 13.sp else 15.sp,
                                    color = CncTextPrimary
                                )
                                Text(
                                    text = stringResource(R.string.ini_editor_desc),
                                    fontSize = 10.sp,
                                    color = CncTextSecondary,
                                    maxLines = 1
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("btn_close_ini_dialog")
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = stringResource(R.string.common_close), tint = CncTextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Tab Selector
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = CncSurfaceVariant,
                        contentColor = CncCyberCyan,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text(stringResource(R.string.ini_tab_summary), fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text(stringResource(R.string.ini_tab_raw), fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Tab Content
                    Box(modifier = Modifier.weight(1f)) {
                        if (selectedTab == 0) {
                            // Machine Summary Cards
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Machine Core Info
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = CncCardBg),
                                    border = BorderStroke(1.dp, CncCardBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = "MÁQUINA: ${parsedConfig.machineName}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = CncActiveGreen
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Cinemática: ${parsedConfig.kinematicsType}", fontSize = 11.sp, color = CncTextSecondary)
                                            Text("Unidades: ${parsedConfig.linearUnits}", fontSize = 11.sp, color = CncCyberCyan)
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Velocidad Máx: ${parsedConfig.maxLinearVelocity} mm/s", fontSize = 11.sp, color = CncTextSecondary)
                                            Text("Aceleración: ${parsedConfig.maxLinearAcceleration} mm/s²", fontSize = 11.sp, color = CncTextSecondary)
                                        }
                                    }
                                }

                                // Soft Limits Travel Table
                                Text("LÍMITES DE CARRERA (SOFT LIMITS) Y ESCALAS", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = CncTextPrimary)

                                AxisLimitCard("EJE X", parsedConfig.xMinLimit, parsedConfig.xMaxLimit, parsedConfig.xStepScale, CncActiveGreen)
                                AxisLimitCard("EJE Y", parsedConfig.yMinLimit, parsedConfig.yMaxLimit, parsedConfig.yStepScale, CncCyberCyan)
                                AxisLimitCard("EJE Z", parsedConfig.zMinLimit, parsedConfig.zMaxLimit, parsedConfig.zStepScale, CncWarningAmber)
                                AxisLimitCard("EJE A", parsedConfig.aMinLimit, parsedConfig.aMaxLimit, parsedConfig.aStepScale, Color(0xFFCE93D8))

                                // Spindle Config
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = CncCardBg),
                                    border = BorderStroke(1.dp, CncCardBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(stringResource(R.string.ini_spindle_title), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = CncTextPrimary)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(stringResource(R.string.ini_rpm_range, parsedConfig.spindleMinRpm.toInt(), parsedConfig.spindleMaxRpm.toInt()), fontSize = 11.sp, color = CncTextSecondary)
                                            Text(stringResource(R.string.ini_scale, parsedConfig.spindleScale.toString()), fontSize = 11.sp, color = CncCyberCyan)
                                        }
                                    }
                                }
                            }
                        } else {
                            // Raw INI Editor
                            OutlinedTextField(
                                value = rawIniText,
                                onValueChange = {
                                    rawIniText = it
                                    parsedConfig = LinuxCncIniParser.parseIni(it)
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("input_raw_ini"),
                                textStyle = LocalTextStyle.current.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = CncTextPrimary
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CncCyberCyan,
                                    unfocusedBorderColor = CncCardBorder,
                                    focusedContainerColor = CncSurfaceVariant,
                                    unfocusedContainerColor = CncSurfaceVariant
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Bottom Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(rawIniText))
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("btn_copy_ini")
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.ini_copy_btn), fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                rawIniText = LinuxCncIniParser.generateSampleIni()
                                parsedConfig = LinuxCncIniParser.parseIni(rawIniText)
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("btn_reset_ini")
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.ini_reset_default_btn), fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                onApplyConfig(parsedConfig)
                                appliedSuccess = true
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CncActiveGreen),
                            modifier = Modifier
                                .weight(1.4f)
                                .height(48.dp)
                                .testTag("btn_apply_ini")
                        ) {
                            Icon(
                                imageVector = if (appliedSuccess) Icons.Default.Check else Icons.Default.Tune,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (appliedSuccess) "¡LÍMITES APLICADOS!" else stringResource(R.string.ini_apply_btn),
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AxisLimitCard(
    axisName: String,
    minLimit: Double,
    maxLimit: Double,
    stepScale: Double,
    accentColor: Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CncCardBg),
        border = BorderStroke(1.dp, CncCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(accentColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(axisName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = accentColor)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Límite: [${minLimit.toInt()} mm .. ${maxLimit.toInt()} mm] (Δ ${(maxLimit - minLimit).toInt()} mm)",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = CncTextPrimary
                )
                Text(
                    text = "Escala: $stepScale pasos/mm",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = CncTextSecondary
                )
            }
        }
    }
}
