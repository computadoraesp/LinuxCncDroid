package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChangeHistory
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.model.CncToolItem
import com.example.model.ToolType
import com.example.ui.theme.CncCardBg
import com.example.ui.theme.CncCardBorder
import com.example.ui.theme.CncCyberCyan
import com.example.ui.theme.CncEstopRed
import com.example.ui.theme.CncRunningGreen
import com.example.ui.theme.CncSurfaceBg
import com.example.ui.theme.CncSurfaceVariant
import com.example.ui.theme.CncTextPrimary
import com.example.ui.theme.CncTextSecondary
import com.example.ui.theme.CncWarningAmber

import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextFieldDefaults
import com.example.service.LinuxCncToolTableParser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolTableDialog(
    tools: List<CncToolItem>,
    activeTool: CncToolItem,
    currentSpindleZ: Double,
    onDismiss: () -> Unit,
    onMountTool: (Int) -> Unit,
    onUpdateTool: (CncToolItem) -> Unit,
    onDeleteTool: (Int) -> Unit,
    onTouchOffZ: (Int) -> Unit,
    onImportToolTable: (String) -> Int = { 0 },
    onExportToolTable: () -> String = { "" },
) {
    var selectedFilter by remember { mutableStateOf<ToolType?>(null) }
    var editingTool by remember { mutableStateOf<CncToolItem?>(null) }
    var showAddDialog by remember { mutableStateOf(value = false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }
    var exportStatusMessage by remember { mutableStateOf<String?>(null) }
    val clipboardManager = LocalClipboardManager.current

    val filteredTools = remember(tools, selectedFilter) {
        if (selectedFilter == null) tools else tools.filter { it.toolType == selectedFilter }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            val isCompact = maxWidth < 600.dp
            Card(
                colors = CardDefaults.cardColors(containerColor = CncSurfaceBg),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(CncCardBorder)),
                modifier = Modifier
                    .fillMaxWidth(if (isCompact) 1f else 0.92f)
                    .fillMaxHeight(if (isCompact) 0.96f else 0.90f),
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
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Construction,
                                contentDescription = stringResource(R.string.tt_header),
                                tint = CncCyberCyan,
                                modifier = Modifier.size(if (isCompact) 20.dp else 24.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.tt_header),
                                    fontWeight = FontWeight.Black,
                                    fontSize = if (isCompact) 12.sp else 14.sp,
                                    color = CncTextPrimary,
                                    maxLines = 1
                                )
                                Text(
                                    text = stringResource(R.string.tt_mounted_info, activeTool.id, activeTool.description),
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = CncWarningAmber,
                                    maxLines = 1
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalButton(
                                onClick = { showAddDialog = true },
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(containerColor = CncSurfaceVariant),
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(R.string.tool_new), modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(stringResource(R.string.tool_new), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CncCyberCyan)
                            }

                            FilledTonalButton(
                                onClick = {
                                    importText = LinuxCncToolTableParser.generateSampleToolTable()
                                    showImportDialog = true
                                },
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(containerColor = CncSurfaceVariant),
                            ) {
                                Icon(imageVector = Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(stringResource(R.string.tool_table_import_btn), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CncRunningGreen)
                            }

                            FilledTonalButton(
                                onClick = {
                                    val exported = onExportToolTable()
                                    clipboardManager.setText(AnnotatedString(exported))
                                    exportStatusMessage = "¡tool.tbl copiado al portapapeles!"
                                },
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(containerColor = CncSurfaceVariant),
                            ) {
                                Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(stringResource(R.string.tool_table_export_btn), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CncWarningAmber)
                            }

                            IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = CncTextSecondary)
                            }
                        }
                    }

                    if (exportStatusMessage != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(exportStatusMessage!!, fontSize = 9.sp, color = CncRunningGreen, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Filter chips in a scrollable Row for portrait/compact compatibility
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedFilter == null,
                                onClick = { selectedFilter = null },
                                label = { Text(stringResource(R.string.tt_all_filter, tools.size), fontSize = 9.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CncCyberCyan,
                                    selectedLabelColor = Color(0xFF00363D)
                                )
                            )
                        }

                        items(ToolType.entries.toTypedArray()) { type ->
                            val count = tools.count { it.toolType == type }
                            if (count > 0) {
                                FilterChip(
                                    selected = selectedFilter == type,
                                    onClick = { selectedFilter = if (selectedFilter == type) null else type },
                                    label = { Text(stringResource(type.displayNameRes) + " ($count)", fontSize = 9.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CncCyberCyan,
                                        selectedLabelColor = Color(0xFF00363D)
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Tool Table List
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                    items(filteredTools, key = { it.id }) { toolItem ->
                        ToolCardItem(
                            tool = toolItem,
                            isCurrentlyActive = (toolItem.id == activeTool.id),
                            currentSpindleZ = currentSpindleZ,
                            onMount = { onMountTool(toolItem.id) },
                            onEdit = { editingTool = toolItem },
                            onDelete = { onDeleteTool(toolItem.id) }
                        ) { onTouchOffZ(toolItem.id) }
                    }
                }
            }
        }
        }
    }

    // Edit Tool Dialog
    if (editingTool != null) {
        EditToolDetailsDialog(
            tool = editingTool!!,
            onDismiss = { editingTool = null },
        ) { updated ->
            onUpdateTool(updated)
            editingTool = null
        }
    }

    // Add New Tool Dialog
    if (showAddDialog) {
        val nextId = (tools.maxOfOrNull { it.id } ?: 0) + 1
        EditToolDetailsDialog(
            tool = CncToolItem(
                id = nextId,
                pocket = nextId,
                description = "New Carbide Cutter",
                diameter = 6.000,
                lengthOffset = 40.000,
                toolType = ToolType.ENDMILL,
                flutes = 3,
                maxRpm = 24000.0,
                lifeMinutesCurrent = 0.0,
                lifeMinutesMax = 120.0
            ),
            isNew = true,
            onDismiss = { showAddDialog = false }
        ) { newTool ->
            onUpdateTool(newTool)
            showAddDialog = false
        }
    }

    // Import LinuxCNC tool.tbl Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.tool_table_import_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = CncRunningGreen
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.tool_table_import_desc),
                        fontSize = 11.sp,
                        color = CncTextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importText,
                        onValueChange = { importText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        textStyle = LocalTextStyle.current.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = CncTextPrimary
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CncRunningGreen,
                            unfocusedBorderColor = CncCardBorder,
                            focusedContainerColor = CncSurfaceVariant,
                            unfocusedContainerColor = CncSurfaceVariant
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val count = onImportToolTable(importText)
                        exportStatusMessage = "¡$count herramientas importadas exitosamente!"
                        showImportDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CncRunningGreen)
                ) {
                    Text(stringResource(R.string.tool_table_import_confirm), color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text(stringResource(R.string.common_cancel), fontSize = 11.sp)
                }
            },
            containerColor = CncSurfaceBg
        )
    }
}

@Composable
fun ToolCardItem(
    tool: CncToolItem,
    isCurrentlyActive: Boolean,
    @Suppress("UNUSED_PARAMETER") currentSpindleZ: Double,
    onMount: () -> Unit,
    onEdit: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onDelete: () -> Unit,
    onTouchOff: () -> Unit
) {
    val lifePct = (tool.lifeMinutesCurrent / tool.lifeMinutesMax).toFloat().coerceIn(0f, 1f)
    val lifeColor = if (lifePct > 0.85f) CncEstopRed else if (lifePct > 0.65f) CncWarningAmber else CncRunningGreen

    Surface(
        color = if (isCurrentlyActive) CncSurfaceVariant else CncCardBg,
        shape = RoundedCornerShape(10.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = SolidColor(if (isCurrentlyActive) CncCyberCyan else CncCardBorder)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = if (isCurrentlyActive) CncCyberCyan else CncSurfaceVariant,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "T${tool.id}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = if (isCurrentlyActive) Color(0xFF00363D) else CncTextPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = tool.description,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = CncTextPrimary
                            )
                            if (isCurrentlyActive) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = CncRunningGreen.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.tt_spindle_active),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        color = CncRunningGreen,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = "Pocket #${tool.pocket} • ${stringResource(tool.toolType.displayNameRes)} • ${tool.flutes} Flutes • Max ${tool.maxRpm.toInt()} RPM",
                            fontSize = 9.5.sp,
                            color = CncTextSecondary,
                        )
                    }
                }

                // Tool Icon from Type
                Icon(
                    imageVector = when (tool.toolType) {
                        ToolType.ENDMILL -> Icons.Default.Construction
                        ToolType.BALLNOSE -> Icons.Default.VerticalAlignBottom
                        ToolType.FACE_MILL -> Icons.Default.Layers
                        ToolType.DRILL -> Icons.Default.Edit
                        ToolType.CHAMFER -> Icons.Default.ChangeHistory
                        ToolType.TAP -> Icons.Default.Edit
                        ToolType.TOUCH_PROBE -> Icons.Default.GpsFixed
                        ToolType.FLY_CUTTER -> Icons.AutoMirrored.Filled.RotateRight
                    },
                    contentDescription = tool.toolType.iconName,
                    tint = CncCyberCyan.copy(alpha = 0.4f),
                    modifier = Modifier.size(24.dp)
                )

                // Mount / Edit Actions
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (!isCurrentlyActive) {
                        FilledTonalButton(
                            onClick = onMount,
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = CncSurfaceVariant),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = stringResource(R.string.tool_mount), modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.tool_table_mount_btn, tool.id), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CncCyberCyan)
                        }
                    }

                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Tool", tint = CncTextSecondary, modifier = Modifier.size(14.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tool Geometry & Wear Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Diameter
                Surface(
                    color = CncSurfaceBg,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(6.dp)) {
                        Text(stringResource(R.string.tool_diameter), fontSize = 8.sp, color = CncTextSecondary)
                        Text("Ø${String.format(java.util.Locale.US, "%.3f", tool.diameter)} mm", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = CncTextPrimary)
                    }
                }

                // Length Offset (H)
                Surface(
                    color = CncSurfaceBg,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(6.dp)) {
                        Text(stringResource(R.string.tool_length_offset), fontSize = 8.sp, color = CncTextSecondary)
                        Text("${String.format(java.util.Locale.US, "%.3f", tool.lengthOffset)} mm", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = CncWarningAmber)
                    }
                }

                // Touch-Off Tool Z Button
                Surface(
                    color = CncSurfaceBg,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .weight(1.2f)
                        .clickable { onTouchOff() }
                ) {
                    Row(
                        modifier = Modifier.padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(stringResource(R.string.tool_touch_off), fontSize = 8.sp, color = CncCyberCyan, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.tool_set_current_z), fontSize = 9.sp, fontWeight = FontWeight.Black, color = CncTextPrimary)
                        }
                        Icon(imageVector = Icons.Default.VerticalAlignBottom, contentDescription = stringResource(R.string.tool_touch_off), tint = CncCyberCyan, modifier = Modifier.size(14.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Tool Wear & Life Progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.tool_life, tool.lifeMinutesCurrent, tool.lifeMinutesMax.toInt(), (lifePct * 100).toInt()),
                    fontSize = 8.5.sp,
                    fontFamily = FontFamily.Monospace,
                    color = CncTextSecondary
                )
                LinearProgressIndicator(
                    progress = { lifePct },
                    color = lifeColor,
                    trackColor = CncSurfaceVariant,
                    modifier = Modifier
                        .width(100.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                )
            }
        }
    }
}

@Composable
fun EditToolDetailsDialog(
    tool: CncToolItem,
    isNew: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (CncToolItem) -> Unit
) {
    var toolNumberStr by remember { mutableStateOf(tool.id.toString()) }
    var pocketStr by remember { mutableStateOf(tool.pocket.toString()) }
    var description by remember { mutableStateOf(tool.description) }
    var diameterStr by remember { mutableStateOf(tool.diameter.toString()) }
    var lengthOffsetStr by remember { mutableStateOf(tool.lengthOffset.toString()) }
    var flutesStr by remember { mutableStateOf(tool.flutes.toString()) }
    var maxRpmStr by remember { mutableStateOf(tool.maxRpm.toInt().toString()) }
    var selectedType by remember { mutableStateOf(tool.toolType) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CncSurfaceBg),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(CncCardBorder)),
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                Text(
                    text = if (isNew) stringResource(R.string.tt_new_title) else stringResource(R.string.tt_edit_title, tool.id),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = CncTextPrimary
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = toolNumberStr,
                        onValueChange = { toolNumberStr = it },
                        label = { Text(stringResource(R.string.tt_tool_num_label), fontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = pocketStr,
                        onValueChange = { pocketStr = it },
                        label = { Text(stringResource(R.string.tt_pocket_num_label), fontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.tt_description_label), fontSize = 10.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = diameterStr,
                        onValueChange = { diameterStr = it },
                        label = { Text(stringResource(R.string.tt_diameter_label), fontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = lengthOffsetStr,
                        onValueChange = { lengthOffsetStr = it },
                        label = { Text(stringResource(R.string.tt_length_h_label), fontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = flutesStr,
                        onValueChange = { flutesStr = it },
                        label = { Text(stringResource(R.string.tt_flutes_label), fontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = maxRpmStr,
                        onValueChange = { maxRpmStr = it },
                        label = { Text(stringResource(R.string.tt_max_rpm_label), fontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.common_cancel), color = CncTextSecondary, fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val id = toolNumberStr.toIntOrNull() ?: tool.id
                            val pocket = pocketStr.toIntOrNull() ?: tool.pocket
                            val diam = diameterStr.toDoubleOrNull() ?: tool.diameter
                            val len = lengthOffsetStr.toDoubleOrNull() ?: tool.lengthOffset
                            val flutes = flutesStr.toIntOrNull() ?: tool.flutes
                            val rpm = maxRpmStr.toDoubleOrNull() ?: tool.maxRpm

                            onSave(
                                tool.copy(
                                    id = id,
                                    pocket = pocket,
                                    description = description,
                                    diameter = diam,
                                    lengthOffset = len,
                                    toolType = selectedType,
                                    flutes = flutes,
                                    maxRpm = rpm
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CncCyberCyan)
                    ) {
                        Text(stringResource(R.string.tt_save_btn), color = Color(0xFF00363D), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }
        }
    }
}
