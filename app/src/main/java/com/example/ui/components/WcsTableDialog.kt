package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.WcsOffset
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WcsTableDialog(
    currentCoordSystem: String,
    wcsOffsets: Map<String, WcsOffset>,
    onSelectWcs: (String) -> Unit,
    onTouchOff: (axis: String, targetVal: Double) -> Unit,
    onSetOffset: (wcsName: String, axis: String, offsetVal: Double) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTabWcs by remember { mutableStateOf(currentCoordSystem) }
    var touchOffAxis by remember { mutableStateOf("X") }
    var touchOffTargetText by remember { mutableStateOf("0.000") }
    var showEditOffsetDialog by remember { mutableStateOf<Triple<String, String, Double>?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            contentAlignment = Alignment.Center,
        ) {
            Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .clip(RoundedCornerShape(16.dp))
                .testTag("wcs_table_dialog"),
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
                            Icons.Default.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "WCS Offset Table (G54 - G59.3)",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Active Coordinate System: $currentCoordSystem",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_wcs_dialog_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // WCS Selector Chips
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(wcsOffsets.keys.toList()) { wcsName ->
                        val isActive = wcsName == currentCoordSystem
                        val isSelectedTab = wcsName == selectedTabWcs
                        FilterChip(
                            selected = isSelectedTab,
                            onClick = { selectedTabWcs = wcsName },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(wcsName, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal)
                                    if (isActive) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ) {
                                            Text("ACTIVE", fontSize = 9.sp)
                                        }
                                    }
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Active WCS Detail & Touch-Off Card
                val activeOffsetObj = wcsOffsets[selectedTabWcs] ?: WcsOffset(selectedTabWcs, 1)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "System $selectedTabWcs: ${activeOffsetObj.comment.ifEmpty { "Work Origin" }}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (selectedTabWcs != currentCoordSystem) {
                                Button(
                                    onClick = { onSelectWcs(selectedTabWcs) },
                                    modifier = Modifier.height(36.dp).testTag("activate_wcs_button")
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Set as Active WCS", fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Touch-Off Panel
                        Text(
                            text = "WORKSHOP TOUCH-OFF (SET WORK ZERO / TARGET)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf("X", "Y", "Z", "A").forEach { axis ->
                                FilterChip(
                                    selected = touchOffAxis == axis,
                                    onClick = { touchOffAxis = axis },
                                    label = { Text(axis, fontWeight = FontWeight.Bold) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = touchOffTargetText,
                                onValueChange = { touchOffTargetText = it },
                                label = { Text("Target Work Pos (mm)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )

                            Button(
                                onClick = {
                                    val targetVal = touchOffTargetText.toDoubleOrNull() ?: 0.0
                                    onTouchOff(touchOffAxis, targetVal)
                                },
                                modifier = Modifier.height(56.dp).testTag("apply_touch_off_button")
                            ) {
                                Icon(Icons.Default.MyLocation, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Touch-Off $touchOffAxis")
                            }

                            OutlinedButton(
                                onClick = {
                                    touchOffTargetText = "0.000"
                                    onTouchOff(touchOffAxis, 0.0)
                                },
                                modifier = Modifier.height(56.dp).testTag("zero_touch_off_button")
                            ) {
                                Text("Zero $touchOffAxis")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "ALL WORK COORDINATE SYSTEMS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Table of all offsets
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(wcsOffsets.values.toList()) { offset ->
                        val isItemActive = offset.name == currentCoordSystem
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    width = if (isItemActive) 1.5.dp else 1.dp,
                                    color = if (isItemActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedTabWcs = offset.name },
                            color = if (isItemActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.width(110.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = offset.name,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        if (isItemActive) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = "Active",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = offset.comment.ifEmpty { "G53 offset" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline,
                                        maxLines = 1
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    WcsAxisValChip("X", offset.x) {
                                        showEditOffsetDialog = Triple(offset.name, "X", offset.x)
                                    }
                                    WcsAxisValChip("Y", offset.y) {
                                        showEditOffsetDialog = Triple(offset.name, "Y", offset.y)
                                    }
                                    WcsAxisValChip("Z", offset.z) {
                                        showEditOffsetDialog = Triple(offset.name, "Z", offset.z)
                                    }
                                    WcsAxisValChip("A", offset.a) {
                                        showEditOffsetDialog = Triple(offset.name, "A", offset.a)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    }

    // Direct Offset Value Edit Dialog
    showEditOffsetDialog?.let { (wcsName, axis, currentVal) ->
        var editText by remember { mutableStateOf(String.format(Locale.US, "%.3f", currentVal)) }
        AlertDialog(
            onDismissRequest = { showEditOffsetDialog = null },
            title = { Text("Edit $wcsName Offset: Axis $axis") },
            text = {
                Column {
                    Text(
                        text = "Specify absolute offset from Machine Zero (G53) in millimeters.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editText,
                        onValueChange = { editText = it },
                        label = { Text("$axis Offset (mm)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val num = editText.toDoubleOrNull() ?: currentVal
                        onSetOffset(wcsName, axis, num)
                        showEditOffsetDialog = null
                    }
                ) {
                    Text("Save Offset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditOffsetDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun WcsAxisValChip(axis: String, value: Double, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(axis, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(
            text = String.format(Locale.US, "%+8.3f", value),
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
