package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.model.DocSectionItem
import com.example.ui.theme.CncActiveGreen
import com.example.ui.theme.CncCardBorder
import com.example.ui.theme.CncCyberCyan
import com.example.ui.theme.CncEstopRed
import com.example.ui.theme.CncSurface
import com.example.ui.theme.CncSurfaceVariant
import com.example.ui.theme.CncTextPrimary
import com.example.ui.theme.CncTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppManualDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sections = remember { getManualSections() }
    var selectedSectionId by remember { mutableStateOf(sections.first().id) }
    var searchQuery by remember { mutableStateOf("") }
    val manualTabsState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val filteredSections = remember(searchQuery) {
        if (searchQuery.isBlank()) sections
        else sections.filter { sec ->
            val title = context.getString(sec.titleRes)
            val summary = context.getString(sec.summaryRes)
            val content = context.getString(sec.detailedContentRes)
            title.contains(searchQuery, ignoreCase = true) ||
            summary.contains(searchQuery, ignoreCase = true) ||
            content.contains(searchQuery, ignoreCase = true)
        }
    }

    val activeSection = filteredSections.find { it.id == selectedSectionId } ?: filteredSections.firstOrNull() ?: sections.first()

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
                modifier = modifier
                    .fillMaxWidth(if (isCompact) 1f else 0.94f)
                    .fillMaxHeight(if (isCompact) 0.96f else 0.92f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CncSurface),
                border = BorderStroke(1.dp, CncCyberCyan.copy(alpha = 0.5f)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(if (isCompact) 10.dp else 16.dp),
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(if (isCompact) 28.dp else 36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CncCyberCyan.copy(alpha = 0.15f))
                                    .border(1.dp, CncCyberCyan, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = "Manual",
                                    tint = CncCyberCyan,
                                    modifier = Modifier.size(if (isCompact) 16.dp else 20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = stringResource(R.string.manual_title),
                                    color = CncCyberCyan,
                                    fontSize = if (isCompact) 11.sp else 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1
                                )
                                Text(
                                    text = stringResource(R.string.manual_subtitle),
                                    color = CncTextSecondary,
                                    fontSize = 8.5.sp,
                                    maxLines = 1
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(28.dp)
                                .background(CncSurfaceVariant, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = CncTextPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    HorizontalDivider(
                        color = CncCardBorder,
                        modifier = Modifier.padding(vertical = if (isCompact) 6.dp else 10.dp)
                    )

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.manual_search_placeholder), fontSize = 9.5.sp) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = CncTextSecondary, modifier = Modifier.size(16.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = null, tint = CncTextSecondary, modifier = Modifier.size(14.dp))
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CncSurfaceVariant,
                        unfocusedContainerColor = CncSurfaceVariant,
                        focusedBorderColor = CncCyberCyan,
                        unfocusedBorderColor = CncCardBorder,
                        focusedTextColor = CncTextPrimary,
                        unfocusedTextColor = CncTextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Horizontal Section Navigator Tabs with Carousel Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CarouselNavButton(
                        direction = "<",
                        enabled = manualTabsState.canScrollBackward,
                        height = 30.dp,
                        width = 22.dp,
                        onClick = {
                            coroutineScope.launch {
                                manualTabsState.animateScrollBy(-180f)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    LazyRow(
                        state = manualTabsState,
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(filteredSections) { sec ->
                            val isSelected = sec.id == activeSection.id
                            val bg = if (isSelected) CncCyberCyan else CncSurfaceVariant
                            val textColor = if (isSelected) Color.Black else CncTextPrimary

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(bg)
                                    .border(1.dp, if (isSelected) CncCyberCyan else CncCardBorder, RoundedCornerShape(6.dp))
                                    .clickable { selectedSectionId = sec.id }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = stringResource(sec.titleRes),
                                    color = textColor,
                                    fontSize = 9.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    CarouselNavButton(
                        direction = ">",
                        enabled = manualTabsState.canScrollForward,
                        height = 30.dp,
                        width = 22.dp,
                        onClick = {
                            coroutineScope.launch {
                                manualTabsState.animateScrollBy(180f)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Detail View for Active Section
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CncSurfaceVariant),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, CncCyberCyan.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(activeSection.titleRes).uppercase(),
                                color = CncActiveGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                            )
                            Text(
                                text = stringResource(activeSection.summaryRes),
                                color = CncCyberCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            HorizontalDivider(color = CncCardBorder, modifier = Modifier.padding(vertical = 4.dp))

                            Text(
                                text = stringResource(activeSection.detailedContentRes),
                                color = CncTextPrimary,
                                fontSize = 9.5.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    // Standard Operating Steps (SOP)
                    if (activeSection.standardStepsRes != 0) {
                        val steps = stringArrayResource(activeSection.standardStepsRes)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = CncSurfaceVariant),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = CncActiveGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.manual_sop_title),
                                        color = CncActiveGreen,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                steps.forEachIndexed { i, step ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(
                                            text = "${i + 1}.",
                                            color = CncCyberCyan,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = step,
                                            color = CncTextPrimary,
                                            fontSize = 9.sp,
                                            lineHeight = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Safety & Interlock Warnings
                    if (activeSection.safetyTipsRes != 0) {
                        val tips = stringArrayResource(activeSection.safetyTipsRes)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = CncEstopRed.copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, CncEstopRed.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = CncEstopRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.manual_safety_title),
                                        color = CncEstopRed,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                tips.forEach { tip ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text("•", color = CncEstopRed, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Text(tip, color = CncTextPrimary, fontSize = 9.sp, lineHeight = 14.sp)
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
}

private fun getManualSections(): List<DocSectionItem> {
    return listOf(
        DocSectionItem(
            id = "safety_interlocks",
            titleRes = R.string.manual_sec1_title,
            category = "SECURITY",
            iconName = "ic_security",
            summaryRes = R.string.manual_sec1_summary,
            detailedContentRes = R.string.manual_sec1_content,
            standardStepsRes = R.array.manual_sec1_sop,
            safetyTipsRes = R.array.manual_sec1_safety,
        ),

        DocSectionItem(
            id = "metrology_calib",
            titleRes = R.string.manual_sec2_title,
            category = "CALIBRACIÓN",
            iconName = "ic_calib",
            summaryRes = R.string.manual_sec2_summary,
            detailedContentRes = R.string.manual_sec2_content,
            standardStepsRes = R.array.manual_sec2_sop,
            safetyTipsRes = R.array.manual_sec2_safety,
        ),

        DocSectionItem(
            id = "dro_wcs",
            titleRes = R.string.manual_sec3_title,
            category = "OPERACIÓN",
            iconName = "ic_dro",
            summaryRes = R.string.manual_sec3_summary,
            detailedContentRes = R.string.manual_sec3_content,
            standardStepsRes = R.array.manual_sec3_sop,
            safetyTipsRes = R.array.manual_sec3_safety,
        ),

        DocSectionItem(
            id = "virtual_mpg",
            titleRes = R.string.manual_sec4_title,
            category = "JOG",
            iconName = "ic_mpg",
            summaryRes = R.string.manual_sec4_summary,
            detailedContentRes = R.string.manual_sec4_content,
            standardStepsRes = R.array.manual_sec4_sop,
            safetyTipsRes = R.array.manual_sec4_safety,
        ),

        DocSectionItem(
            id = "tool_table",
            titleRes = R.string.manual_sec5_title,
            category = "TOOLS",
            iconName = "ic_tools",
            summaryRes = R.string.manual_sec5_summary,
            detailedContentRes = R.string.manual_sec5_content,
            standardStepsRes = R.array.manual_sec5_sop,
            safetyTipsRes = R.array.manual_sec5_safety,
        ),

        DocSectionItem(
            id = "probing_cycles",
            titleRes = R.string.manual_sec6_title,
            category = "PALPADO",
            iconName = "ic_probe",
            summaryRes = R.string.manual_sec6_summary,
            detailedContentRes = R.string.manual_sec6_content,
            standardStepsRes = R.array.manual_sec6_sop,
            safetyTipsRes = R.array.manual_sec6_safety,
        ),

        DocSectionItem(
            id = "ethercat_bus",
            titleRes = R.string.manual_sec7_title,
            category = "DIAGNÓSTICO",
            iconName = "ic_ethercat",
            summaryRes = R.string.manual_sec7_summary,
            detailedContentRes = R.string.manual_sec7_content,
            standardStepsRes = R.array.manual_sec7_sop,
            safetyTipsRes = R.array.manual_sec7_safety,
        ),

        DocSectionItem(
            id = "power_screen_network",
            titleRes = R.string.manual_sec8_title,
            category = "FAIL-SAFE",
            iconName = "ic_security",
            summaryRes = R.string.manual_sec8_summary,
            detailedContentRes = R.string.manual_sec8_content,
            standardStepsRes = R.array.manual_sec8_sop,
            safetyTipsRes = R.array.manual_sec8_safety,
        ),

        DocSectionItem(
            id = "camera_metrology",
            titleRes = R.string.manual_sec9_title,
            category = "VISIÓN Y METROLOGÍA",
            iconName = "ic_camera",
            summaryRes = R.string.manual_sec9_summary,
            detailedContentRes = R.string.manual_sec9_content,
            standardStepsRes = R.array.manual_sec9_sop,
            safetyTipsRes = R.array.manual_sec9_safety,
        ),

        DocSectionItem(
            id = "simulation_center",
            titleRes = R.string.manual_sec10_title,
            category = "SIMULACIÓN Y PRUEBAS",
            iconName = "ic_simulation",
            summaryRes = R.string.manual_sec10_summary,
            detailedContentRes = R.string.manual_sec10_content,
            standardStepsRes = R.array.manual_sec10_sop,
            safetyTipsRes = R.array.manual_sec10_safety,
        ),

        DocSectionItem(
            id = "toolpath_visualizer",
            titleRes = R.string.manual_sec11_title,
            category = "TRAYECTORIA 3D Y SIMULACIÓN",
            iconName = "ic_toolpath",
            summaryRes = R.string.manual_sec11_summary,
            detailedContentRes = R.string.manual_sec11_content,
            standardStepsRes = R.array.manual_sec11_sop,
            safetyTipsRes = R.array.manual_sec11_safety,
        ),
    )
}
