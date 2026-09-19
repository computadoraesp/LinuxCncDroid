package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.MdiHistoryEntity
import com.example.data.local.MdiMacroEntity
import com.example.model.MachineStateEnum
import com.example.model.MdiValidationResult
import com.example.ui.theme.*

@Composable
fun MdiView(
    machineState: MachineStateEnum = MachineStateEnum.IDLE,
    commandText: String,
    history: List<String> = emptyList(),
    historyEntities: List<MdiHistoryEntity> = emptyList(),
    macros: List<MdiMacroEntity> = emptyList(),
    validationResult: MdiValidationResult? = null,
    onCommandTextChange: (String) -> Unit,
    onExecuteCommand: (String) -> Unit,
    onToggleFavorite: (id: Long, isFavorite: Boolean) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    val isEnabled = machineState != MachineStateEnum.RUNNING &&
            machineState != MachineStateEnum.ESTOP &&
            machineState != MachineStateEnum.ERROR

    var selectedHistoryTab by remember { mutableStateOf(0) } // 0: Recent, 1: Favorites

    Card(
        colors = CardDefaults.cardColors(containerColor = CncCardBg),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CncCardBorder)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = stringResource(R.string.mdi_title),
                        tint = CncCyberCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.mdi_title),
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        color = CncTextPrimary
                    )
                }

                // Live Syntax Indicator Badge
                if (commandText.isNotBlank() && validationResult != null) {
                    if (validationResult.isValid) {
                        Surface(
                            color = Color(0xFF1B5E20).copy(alpha = 0.3f),
                            shape = RoundedCornerShape(4.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4CAF50))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("RS274 VALID", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF81C784))
                            }
                        }
                    } else {
                        Surface(
                            color = Color(0xFFB71C1C).copy(alpha = 0.3f),
                            shape = RoundedCornerShape(4.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE57373))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFE57373), modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("SYNTAX ERROR", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF9A9A))
                            }
                        }
                    }
                }
            }

            // Live Error / Token Hint Banner
            if (commandText.isNotBlank() && validationResult != null && !validationResult.isValid) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = validationResult.errorMessage ?: "Syntax Error",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else if (commandText.isNotBlank() && validationResult != null && validationResult.parsedTokens.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(validationResult.parsedTokens) { token ->
                        Surface(
                            color = CncSurfaceVariant,
                            shape = RoundedCornerShape(4.dp),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, CncCardBorder)
                        ) {
                            Text(
                                text = token,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CncCyberCyan,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Command Input Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = commandText,
                    onValueChange = onCommandTextChange,
                    enabled = isEnabled,
                    placeholder = { Text(stringResource(R.string.mdi_placeholder), color = CncTextMuted, fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CncSurface,
                        unfocusedContainerColor = CncSurface,
                        focusedTextColor = CncCyberCyan,
                        unfocusedTextColor = CncTextPrimary,
                        focusedBorderColor = if (validationResult?.isValid == false) MaterialTheme.colorScheme.error else CncCyberCyan,
                        unfocusedBorderColor = CncCardBorder
                    ),
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("mdi_input_field")
                )

                Button(
                    onClick = { onExecuteCommand(commandText) },
                    enabled = isEnabled && commandText.isNotBlank() && (validationResult == null || validationResult.isValid),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CncCyberCyan,
                        contentColor = Color(0xFF00363D)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("mdi_send_button")
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.mdi_execute))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.mdi_execute), fontWeight = FontWeight.Black)
                }
            }

            // Quick G-Code Helper Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("G0 X0 Y0", "G0 Z10", "M3 S12000", "M5", "G28", "G54", "G90", "G91").forEach { gcode ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(28.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(CncSurfaceVariant)
                            .border(1.dp, CncCardBorder, RoundedCornerShape(4.dp))
                            .clickable(enabled = isEnabled) { onCommandTextChange(gcode) }
                    ) {
                        Text(gcode, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = CncCyberCyan)
                    }
                }
            }

            HorizontalDivider(Modifier, DividerDefaults.Thickness, color = CncCardBorder)

            // Programmable Macros Grid
            Text(stringResource(R.string.mdi_macros_header), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CncTextSecondary)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                macros.take(4).forEach { macro ->
                    Surface(
                        color = CncSurface,
                        shape = RoundedCornerShape(8.dp),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CncCardBorder)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable(enabled = isEnabled) { onExecuteCommand(macro.command) }
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(macro.label, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = CncWarningAmber)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(macro.description, fontSize = 9.sp, color = CncTextSecondary, maxLines = 2)
                        }
                    }
                }
            }

            HorizontalDivider(Modifier, DividerDefaults.Thickness, color = CncCardBorder)

            // Command Execution History & Favorites Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedHistoryTab == 0,
                        onClick = { selectedHistoryTab = 0 },
                        label = { Text("Recent History", fontSize = 10.sp) }
                    )
                    FilterChip(
                        selected = selectedHistoryTab == 1,
                        onClick = { selectedHistoryTab = 1 },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Starred", fontSize = 10.sp)
                            }
                        }
                    )
                }
            }

            Surface(
                color = CncSurface,
                shape = RoundedCornerShape(8.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CncCardBorder)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                if (historyEntities.isNotEmpty()) {
                    val displayList = if (selectedHistoryTab == 1) {
                        historyEntities.filter { it.isFavorite }
                    } else {
                        historyEntities
                    }

                    if (displayList.isEmpty()) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text("No commands in this view", fontSize = 11.sp, color = CncTextMuted)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.padding(6.dp)) {
                            items(displayList, key = { it.id }) { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable(enabled = isEnabled) { onCommandTextChange(item.command) }
                                        .padding(horizontal = 6.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Text(">", color = if (item.executionStatus == "SUCCESS") CncActiveGreen else MaterialTheme.colorScheme.error, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(item.command, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = CncTextPrimary)
                                    }

                                    IconButton(
                                        onClick = { onToggleFavorite(item.id, !item.isFavorite) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (item.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                            contentDescription = "Favorite",
                                            tint = if (item.isFavorite) CncWarningAmber else CncTextMuted,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Fallback to memory history
                    LazyColumn(modifier = Modifier.padding(6.dp)) {
                        items(history) { cmd ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable(enabled = isEnabled) { onCommandTextChange(cmd) }
                                    .padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(">", color = CncActiveGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(cmd, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = CncTextPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}
