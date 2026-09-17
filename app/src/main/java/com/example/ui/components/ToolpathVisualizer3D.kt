package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AxisCoord
import com.example.model.GCodeSegment
import com.example.ui.theme.AxisXColor
import com.example.ui.theme.AxisYColor
import com.example.ui.theme.AxisZColor
import com.example.ui.theme.CncActiveGreen
import com.example.ui.theme.CncCardBg
import com.example.ui.theme.CncCardBorder
import com.example.ui.theme.CncCyberCyan
import com.example.ui.theme.CncDroDigits
import com.example.ui.theme.CncEstopRed
import com.example.ui.theme.CncSurface
import com.example.ui.theme.CncSurfaceVariant
import com.example.ui.theme.CncTextMuted
import com.example.ui.theme.CncTextPrimary
import com.example.ui.theme.CncTextSecondary
import com.example.ui.theme.CncWarningAmber
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

enum class ViewPerspective {
    TOP_XY,
    FRONT_XZ,
    SIDE_YZ,
    ISO_3D
}

data class ToolpathMetrics(
    val xSpan: Float,
    val ySpan: Float,
    val zSpan: Float,
    val minX: Float,
    val maxX: Float,
    val minY: Float,
    val maxY: Float,
    val minZ: Float,
    val maxZ: Float,
    val zMinCut: Float,
    val zSafeRapid: Float,
    val totalCutDistanceMm: Double,
)

/**
 * High-performance 3D Toolpath Visualizer & Dry-Run Simulator.
 *
 * Combines high-fidelity industrial CAM visualization (dynamic chromatic history,
 * 3D stock material wireframe bounding box, WCS datum triad, arc vs linear highlighting)
 * with an ultra-lean 2.5D projection engine designed for maximum battery life and 60 FPS
 * on mobile phones and tablets.
 */
@Composable
fun ToolpathVisualizer3D(
    modifier: Modifier = Modifier,
    gcodeList: List<GCodeSegment>,
    activeLineIndex: Int,
    axes: Map<String, AxisCoord>,
    fileName: String = "face_pocket_contour.ngc",
    elapsedSeconds: Long = 0L,
    estimatedTotalSeconds: Long = 180L,
    @Suppress("UNUSED_PARAMETER") feedRate: Double = 1500.0,
    spindleRpm: Double = 18000.0,
    activeToolDiameter: Double = 6.0,
    onOpenLoader: () -> Unit = {},
) {
    var perspective by remember { mutableStateOf(ViewPerspective.ISO_3D) }
    var zoomScale by remember { mutableFloatStateOf(3.2f) }
    var panOffset by remember { mutableStateOf(Offset(180f, 190f)) }

    // Layer Toggles for performance & inspection flexibility
    var showRapids by remember { mutableStateOf(true) }
    var showStock by remember { mutableStateOf(true) }
    var showGrid by remember { mutableStateOf(true) }
    var showHud by remember { mutableStateOf(true) }
    var lodAutoEnabled by remember { mutableStateOf(true) }
    var followTool by remember { mutableStateOf(false) }
    var showCodePanel by remember { mutableStateOf(true) }

    // Interactive Dry-Run Simulation & Scrubbing
    var isDryRunning by remember { mutableStateOf(false) }
    var simSpeed by remember { mutableIntStateOf(1) } // 1x, 2x, 5x, 10x
    var scrubIndex by remember { mutableIntStateOf(0) }
    var canvasSize by remember { mutableStateOf(IntSize(400, 300)) }

    val listState = rememberLazyListState()

    // Synchronize scrubIndex with real machine when live cutting, or loop when dry-running
    LaunchedEffect(activeLineIndex) {
        if (!isDryRunning && gcodeList.isNotEmpty()) {
            scrubIndex = activeLineIndex.coerceIn(0, gcodeList.size - 1)
        }
    }

    // Dry-run virtual playback ticker
    LaunchedEffect(isDryRunning, simSpeed, gcodeList.size) {
        if (isDryRunning && gcodeList.isNotEmpty()) {
            while (isDryRunning) {
                val delayMs = max(15L, (120L / simSpeed))
                delay(delayMs)
                if (scrubIndex < gcodeList.size - 1) {
                    scrubIndex++
                } else {
                    // Loop or stop at end
                    scrubIndex = 0
                }
            }
        }
    }

    val effectiveIndex = if (isDryRunning || scrubIndex != activeLineIndex) {
        if (gcodeList.isEmpty()) 0 else scrubIndex.coerceIn(0, gcodeList.size - 1)
    } else {
        if (gcodeList.isEmpty()) 0 else activeLineIndex.coerceIn(0, gcodeList.size - 1)
    }

    // Auto-scroll G-Code inspector
    LaunchedEffect(effectiveIndex) {
        if (gcodeList.isNotEmpty() && (effectiveIndex in gcodeList.indices) && showCodePanel) {
            listState.animateScrollToItem(max(0, effectiveIndex - 2))
        }
    }

    // Workpiece Bounding Box and Critical Machining Metrics (precalculated once per gcode list)
    val toolpathMetrics = remember(gcodeList) {
        if (gcodeList.isEmpty()) {
            ToolpathMetrics(
                xSpan = 50f, ySpan = 50f, zSpan = 12.5f,
                minX = 0f, maxX = 50f,
                minY = 0f, maxY = 50f,
                minZ = -2.5f, maxZ = 10f,
                zMinCut = -2.5f,
                zSafeRapid = 10f,
                totalCutDistanceMm = 0.0
            )
        } else {
            var minX = Float.MAX_VALUE; var maxX = -Float.MAX_VALUE
            var minY = Float.MAX_VALUE; var maxY = -Float.MAX_VALUE
            var minZ = Float.MAX_VALUE; var maxZ = -Float.MAX_VALUE
            var zMinCut = 0f
            var zSafeRapid = 5f
            var cutDist = 0.0

            gcodeList.forEach { seg ->
                minX = min(minX, min(seg.startX, seg.endX))
                maxX = max(maxX, max(seg.startX, seg.endX))
                minY = min(minY, min(seg.startY, seg.endY))
                maxY = max(maxY, max(seg.startY, seg.endY))
                minZ = min(minZ, min(seg.startZ, seg.endZ))
                maxZ = max(maxZ, max(seg.startZ, seg.endZ))

                if (seg.isCut) {
                    zMinCut = min(zMinCut, min(seg.startZ, seg.endZ))
                    val dx = (seg.endX - seg.startX).toDouble()
                    val dy = (seg.endY - seg.startY).toDouble()
                    val dz = (seg.endZ - seg.startZ).toDouble()
                    cutDist += sqrt(dx * dx + dy * dy + dz * dz)
                }
                if (seg.isRapid) {
                    zSafeRapid = max(zSafeRapid, max(seg.startZ, seg.endZ))
                }
            }

            if (minX == Float.MAX_VALUE) {
                ToolpathMetrics(
                    xSpan = 50f, ySpan = 50f, zSpan = 12.5f,
                    minX = 0f, maxX = 50f,
                    minY = 0f, maxY = 50f,
                    minZ = -2.5f, maxZ = 10f,
                    zMinCut = -2.5f,
                    zSafeRapid = 10f,
                    totalCutDistanceMm = 0.0
                )
            } else {
                ToolpathMetrics(
                    xSpan = max(1f, maxX - minX),
                    ySpan = max(1f, maxY - minY),
                    zSpan = max(1f, maxZ - minZ),
                    minX = minX, maxX = maxX,
                    minY = minY, maxY = maxY,
                    minZ = minZ, maxZ = maxZ,
                    zMinCut = zMinCut,
                    zSafeRapid = zSafeRapid,
                    totalCutDistanceMm = cutDist
                )
            }
        }
    }

    // Adaptive Level of Detail (LOD) computation
    // Dynamically subsamples dense toolpaths on mobile devices while strictly preserving:
    // 1. All G0 rapid positioning moves (safety critical)
    // 2. All circular/helical arcs (G2/G3)
    // 3. Local neighborhood (+/- 25 blocks) around active tool position
    // 4. Critical start/end inflection points
    val (renderedSegments, lodStride) = remember(gcodeList, lodAutoEnabled, effectiveIndex) {
        val total = gcodeList.size
        if (!lodAutoEnabled || total <= 600) {
            gcodeList to 1
        } else {
            val stride = when {
                total > 15000 -> 16
                total > 8000 -> 8
                total > 3000 -> 4
                total > 1000 -> 2
                else -> 1
            }
            if (stride == 1) {
                gcodeList to 1
            } else {
                val activeStart = (effectiveIndex - 25).coerceAtLeast(0)
                val activeEnd = (effectiveIndex + 25).coerceAtMost(total - 1)

                val filtered = ArrayList<GCodeSegment>(total / stride + 60)
                for (i in 0 until total) {
                    val seg = gcodeList[i]
                    val isArc = seg.rawText.contains("G2") || seg.rawText.contains("G3") ||
                            seg.rawText.contains("G02") || seg.rawText.contains("G03")
                    if (seg.isRapid || isArc || (i in activeStart..activeEnd) || (i % stride == 0) || i == total - 1) {
                        filtered.add(seg)
                    }
                }
                filtered to stride
            }
        }
    }

    val activeSeg = if (gcodeList.isNotEmpty() && effectiveIndex in gcodeList.indices) gcodeList[effectiveIndex] else null
    val deltaX = if (activeSeg != null) activeSeg.endX - activeSeg.startX else 0f
    val deltaY = if (activeSeg != null) activeSeg.endY - activeSeg.startY else 0f
    val deltaZ = if (activeSeg != null) activeSeg.endZ - activeSeg.startZ else 0f

    // Distance To Go (DTG) calculation in mm
    val distanceToGoMm = remember(gcodeList, effectiveIndex) {
        if (gcodeList.isEmpty() || effectiveIndex !in gcodeList.indices) 0.0
        else {
            var sum = 0.0
            for (i in effectiveIndex until gcodeList.size) {
                val seg = gcodeList[i]
                if (seg.isCut) {
                    val dx = (seg.endX - seg.startX).toDouble()
                    val dy = (seg.endY - seg.startY).toDouble()
                    val dz = (seg.endZ - seg.startZ).toDouble()
                    sum += sqrt(dx * dx + dy * dy + dz * dz)
                }
            }
            sum
        }
    }

    val progressPct = remember(effectiveIndex, gcodeList) {
        if (gcodeList.isEmpty()) 0f else ((effectiveIndex + 1).toFloat() / gcodeList.size.toFloat()).coerceIn(0f, 1f)
    }

    val remainingSeconds = remember(progressPct, estimatedTotalSeconds, elapsedSeconds) {
        max(0L, (estimatedTotalSeconds * (1f - progressPct)).toLong())
    }

    val surfaceSpeedMMin = remember(activeToolDiameter, spindleRpm) {
        (PI * activeToolDiameter * spindleRpm) / 1000.0
    }

    // Auto-fit function: computes optimal scale and center offset
    val performAutoFit: () -> Unit = {
        val dx = max(10f, toolpathMetrics.xSpan)
        val dy = max(10f, toolpathMetrics.ySpan)
        val dz = max(5f, toolpathMetrics.zSpan)
        val centerX = (toolpathMetrics.minX + toolpathMetrics.maxX) / 2f
        val centerY = (toolpathMetrics.minY + toolpathMetrics.maxY) / 2f
        val centerZ = (toolpathMetrics.minZ + toolpathMetrics.maxZ) / 2f

        val (spanW, spanH) = when (perspective) {
            ViewPerspective.TOP_XY -> dx to dy
            ViewPerspective.FRONT_XZ -> dx to dz
            ViewPerspective.SIDE_YZ -> dy to dz
            ViewPerspective.ISO_3D -> (dx + dy * 0.707f) to (dz + (dx + dy) * 0.353f)
        }

        val availableW = max(100f, canvasSize.width.toFloat() - 40f)
        val availableH = max(100f, canvasSize.height.toFloat() - 40f)
        val targetScale = min(availableW / spanW, availableH / spanH).coerceIn(0.8f, 15f)

        val unpanned = projectPoint(centerX, centerY, centerZ, targetScale, Offset.Zero, perspective)
        panOffset = Offset(canvasSize.width / 2f - unpanned.x, canvasSize.height / 2f - unpanned.y)
        zoomScale = targetScale
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = CncCardBg),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(CncCardBorder)),
        modifier = modifier.fillMaxWidth(),
    ) {
        BoxWithConstraints(modifier = Modifier.padding(10.dp)) {
            val isCompactWidth = maxWidth < 680.dp

            Column {
                if (isCompactWidth) {
                    // --- PORTRAIT / COMPACT LAYOUT (No truncation, no ghost spaces) ---
                    // Row 1: Title, File Badge & Dimensions in a single cohesive row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ViewInAr,
                                contentDescription = stringResource(R.string.tp_header),
                                tint = CncCyberCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "3D TOOLPATH",
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                color = CncTextPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "[$fileName]",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CncWarningAmber,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }

                        // Workpiece Dimensions (compact badge)
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = CncSurfaceVariant.copy(alpha = 0.7f),
                            border = BorderStroke(1.dp, Color(0x2200E5FF))
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.tp_dim_info,
                                    toolpathMetrics.xSpan.toInt(),
                                    toolpathMetrics.ySpan.toInt(),
                                    toolpathMetrics.zSpan.toInt()
                                ),
                                fontSize = 8.5.sp,
                                color = CncCyberCyan,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Row 2: Action Buttons (Auto-Fit & Load) + 4 View Perspectives (Fixed, fully visible)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalButton(
                                onClick = performAutoFit,
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 7.dp, vertical = 2.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = CncSurfaceVariant,
                                    contentColor = CncCyberCyan
                                ),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Icon(imageVector = Icons.Default.FitScreen, contentDescription = stringResource(R.string.tp_autofit), modifier = Modifier.size(11.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(stringResource(R.string.tp_autofit), fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                            }

                            FilledTonalButton(
                                onClick = onOpenLoader,
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 7.dp, vertical = 2.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = CncSurfaceVariant,
                                    contentColor = CncCyberCyan
                                ),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Shield, contentDescription = stringResource(R.string.toolpath_load_scan), modifier = Modifier.size(11.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(stringResource(R.string.toolpath_load_scan), fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Perspective Selector Pills (Fixed width, fits perfectly on any portrait screen)
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(CncSurfaceVariant)
                                .padding(2.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            listOf(
                                ViewPerspective.TOP_XY to stringResource(R.string.view_top),
                                ViewPerspective.ISO_3D to stringResource(R.string.view_iso),
                                ViewPerspective.FRONT_XZ to stringResource(R.string.view_front),
                                ViewPerspective.SIDE_YZ to stringResource(R.string.view_side)
                            ).forEach { (view, label) ->
                                val isSelected = perspective == view
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isSelected) CncCyberCyan else Color.Transparent)
                                        .clickable { perspective = view }
                                        .padding(horizontal = 5.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color(0xFF00363D) else CncTextSecondary
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // --- LANDSCAPE / WIDE LAYOUT (Single elegant continuous row) ---
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
                                imageVector = Icons.Default.ViewInAr,
                                contentDescription = stringResource(R.string.tp_header),
                                tint = CncCyberCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.tp_header),
                                fontWeight = FontWeight.Black,
                                fontSize = 11.5.sp,
                                color = CncTextPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "[$fileName]",
                                fontSize = 9.5.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CncWarningAmber,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(
                                    R.string.tp_dim_info,
                                    toolpathMetrics.xSpan.toInt(),
                                    toolpathMetrics.ySpan.toInt(),
                                    toolpathMetrics.zSpan.toInt()
                                ),
                                fontSize = 9.sp,
                                color = CncTextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            FilledTonalButton(
                                onClick = performAutoFit,
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 7.dp, vertical = 2.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = CncSurfaceVariant,
                                    contentColor = CncCyberCyan
                                ),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Icon(imageVector = Icons.Default.FitScreen, contentDescription = stringResource(R.string.tp_autofit), modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(stringResource(R.string.tp_autofit), fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                            }

                            FilledTonalButton(
                                onClick = onOpenLoader,
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 7.dp, vertical = 2.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = CncSurfaceVariant,
                                    contentColor = CncCyberCyan
                                ),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Shield, contentDescription = stringResource(R.string.toolpath_load_scan), modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(stringResource(R.string.toolpath_load_scan), fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                            }

                            // Perspective Selector Pills
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CncSurfaceVariant)
                                    .padding(2.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                listOf(
                                    ViewPerspective.TOP_XY to stringResource(R.string.view_top),
                                    ViewPerspective.ISO_3D to stringResource(R.string.view_iso),
                                    ViewPerspective.FRONT_XZ to stringResource(R.string.view_front),
                                    ViewPerspective.SIDE_YZ to stringResource(R.string.view_side)
                                ).forEach { (view, label) ->
                                    val isSelected = perspective == view
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (isSelected) CncCyberCyan else Color.Transparent)
                                            .clickable { perspective = view }
                                            .padding(horizontal = 6.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color(0xFF00363D) else CncTextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

            Spacer(modifier = Modifier.height(6.dp))

            // Quick Layer Filters & Visual Toggles Toolbar (Adaptable: Multi-row or single row)
            if (isCompactWidth) {
                // Portrait compact layout: 2 structured rows, no awkward endless carousel feel
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(CncSurfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Row 1: Toggles (Rapids, Stock, Grid, HUD, LOD, Follow)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterToggleChip(
                            label = stringResource(R.string.tp_toggle_rapids),
                            isActive = showRapids,
                            activeColor = CncWarningAmber,
                            onClick = { showRapids = !showRapids }
                        )
                        FilterToggleChip(
                            label = stringResource(R.string.tp_toggle_stock),
                            isActive = showStock,
                            activeColor = CncCyberCyan,
                            onClick = { showStock = !showStock }
                        )
                        FilterToggleChip(
                            label = stringResource(R.string.tp_toggle_grid),
                            isActive = showGrid,
                            activeColor = Color(0xFF64B5F6),
                            onClick = { showGrid = !showGrid }
                        )
                        FilterToggleChip(
                            label = "HUD",
                            isActive = showHud,
                            activeColor = CncActiveGreen,
                            onClick = { showHud = !showHud }
                        )
                        FilterToggleChip(
                            label = if (lodAutoEnabled) stringResource(R.string.tp_toggle_lod) else stringResource(R.string.tp_toggle_lod_full),
                            isActive = lodAutoEnabled,
                            activeColor = Color(0xFF80D8FF),
                            icon = Icons.Default.Speed,
                            onClick = { lodAutoEnabled = !lodAutoEnabled }
                        )
                        FilterToggleChip(
                            label = stringResource(R.string.tp_toggle_follow),
                            isActive = followTool,
                            activeColor = CncActiveGreen,
                            onClick = { followTool = !followTool }
                        )
                    }

                    // Row 2: Visual Legend & G-Code Panel Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LegendDot(color = Color(0xFF2E7D32), label = stringResource(R.string.tp_past_cuts))
                            LegendDot(color = CncActiveGreen, label = stringResource(R.string.tp_current_tool))
                            LegendDot(color = Color(0xFFE040FB), label = stringResource(R.string.tp_arc_cut))
                            LegendDot(color = CncCyberCyan, label = "G1")
                        }

                        FilterToggleChip(
                            label = stringResource(R.string.tp_toggle_code),
                            isActive = showCodePanel,
                            activeColor = CncCyberCyan,
                            icon = Icons.Default.Code,
                            onClick = { showCodePanel = !showCodePanel }
                        )
                    }
                }
            } else {
                // Landscape / Wide toolbar: single clean line
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(CncSurfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterToggleChip(
                            label = stringResource(R.string.tp_toggle_rapids),
                            isActive = showRapids,
                            activeColor = CncWarningAmber,
                            onClick = { showRapids = !showRapids }
                        )
                        FilterToggleChip(
                            label = stringResource(R.string.tp_toggle_stock),
                            isActive = showStock,
                            activeColor = CncCyberCyan,
                            onClick = { showStock = !showStock }
                        )
                        FilterToggleChip(
                            label = stringResource(R.string.tp_toggle_grid),
                            isActive = showGrid,
                            activeColor = Color(0xFF64B5F6),
                            onClick = { showGrid = !showGrid }
                        )
                        FilterToggleChip(
                            label = "HUD",
                            isActive = showHud,
                            activeColor = CncActiveGreen,
                            onClick = { showHud = !showHud }
                        )
                        FilterToggleChip(
                            label = if (lodAutoEnabled) stringResource(R.string.tp_toggle_lod) else stringResource(R.string.tp_toggle_lod_full),
                            isActive = lodAutoEnabled,
                            activeColor = Color(0xFF80D8FF),
                            icon = Icons.Default.Speed,
                            onClick = { lodAutoEnabled = !lodAutoEnabled }
                        )
                        FilterToggleChip(
                            label = stringResource(R.string.tp_toggle_follow),
                            isActive = followTool,
                            activeColor = CncActiveGreen,
                            onClick = { followTool = !followTool }
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LegendDot(color = Color(0xFF2E7D32), label = stringResource(R.string.tp_past_cuts))
                            LegendDot(color = CncActiveGreen, label = stringResource(R.string.tp_current_tool))
                            LegendDot(color = Color(0xFFE040FB), label = stringResource(R.string.tp_arc_cut))
                            LegendDot(color = CncCyberCyan, label = "G1")
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        FilterToggleChip(
                            label = stringResource(R.string.tp_toggle_code),
                            isActive = showCodePanel,
                            activeColor = CncCyberCyan,
                            icon = Icons.Default.Code,
                            onClick = { showCodePanel = !showCodePanel }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Main Visualizer & GCode split
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(270.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 3D/2D Canvas Visualizer
                Box(
                    modifier = Modifier
                        .weight(if (showCodePanel) 1.6f else 1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF080D16))
                        .border(1.dp, CncCardBorder, RoundedCornerShape(8.dp))
                        .onGloballyPositioned { canvasSize = it.size }
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                zoomScale = (zoomScale * zoom).coerceIn(0.5f, 20.0f)
                                panOffset += pan
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height

                        // 1. Coordinate Grid
                        if (showGrid) {
                            val gridSpacing = 20f * zoomScale
                            if (gridSpacing >= 8f) {
                                val startX = (panOffset.x % gridSpacing)
                                val startY = (panOffset.y % gridSpacing)

                                var curX = startX
                                while (curX < canvasWidth) {
                                    drawLine(
                                        color = Color(0xFF111E2E),
                                        start = Offset(curX, 0f),
                                        end = Offset(curX, canvasHeight),
                                        strokeWidth = 1f
                                    )
                                    curX += gridSpacing
                                }

                                var curY = startY
                                while (curY < canvasHeight) {
                                    drawLine(
                                        color = Color(0xFF111E2E),
                                        start = Offset(0f, curY),
                                        end = Offset(canvasWidth, curY),
                                        strokeWidth = 1f
                                    )
                                    curY += gridSpacing
                                }
                            }
                        }

                        // 2. Origin & WCS Datum Triad (0, 0, 0)
                        val originProjected = projectPoint(0f, 0f, 0f, zoomScale, panOffset, perspective)
                        val triadLen = 38f

                        // X+ Axis (Red)
                        val ptX = projectPoint(1f, 0f, 0f, triadLen, originProjected, perspective)
                        drawLine(
                            color = AxisXColor,
                            start = originProjected,
                            end = ptX,
                            strokeWidth = 2.2f
                        )

                        // Y+ Axis (Green)
                        val ptY = projectPoint(0f, 1f, 0f, triadLen, originProjected, perspective)
                        drawLine(
                            color = AxisYColor,
                            start = originProjected,
                            end = ptY,
                            strokeWidth = 2.2f
                        )

                        // Z+ Axis (Blue)
                        val ptZ = projectPoint(0f, 0f, 1f, triadLen, originProjected, perspective)
                        drawLine(
                            color = AxisZColor,
                            start = originProjected,
                            end = ptZ,
                            strokeWidth = 2.2f
                        )

                        // Origin sphere point
                        drawCircle(
                            color = Color.White,
                            radius = 3.5f,
                            center = originProjected
                        )

                        // 3. 3D Workpiece Stock Bounding Box Wireframe & Safety Clearance Plane
                        if (showStock && gcodeList.isNotEmpty()) {
                            val x0 = toolpathMetrics.minX; val x1 = toolpathMetrics.maxX
                            val y0 = toolpathMetrics.minY; val y1 = toolpathMetrics.maxY
                            val z0 = toolpathMetrics.minZ; val z1 = toolpathMetrics.maxZ

                            // 8 corners of the stock bounding box
                            val p000 = projectPoint(x0, y0, z0, zoomScale, panOffset, perspective)
                            val p100 = projectPoint(x1, y0, z0, zoomScale, panOffset, perspective)
                            val p110 = projectPoint(x1, y1, z0, zoomScale, panOffset, perspective)
                            val p010 = projectPoint(x0, y1, z0, zoomScale, panOffset, perspective)

                            val p001 = projectPoint(x0, y0, z1, zoomScale, panOffset, perspective)
                            val p101 = projectPoint(x1, y0, z1, zoomScale, panOffset, perspective)
                            val p111 = projectPoint(x1, y1, z1, zoomScale, panOffset, perspective)
                            val p011 = projectPoint(x0, y1, z1, zoomScale, panOffset, perspective)

                            // Base platform tint
                            val basePoly = Path().apply {
                                moveTo(p000.x, p000.y)
                                lineTo(p100.x, p100.y)
                                lineTo(p110.x, p110.y)
                                lineTo(p010.x, p010.y)
                                close()
                            }
                            drawPath(basePoly, color = Color(0x0C00E5FF))

                            val wireColor = Color(0x4000E5FF)
                            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f)

                            // Bottom face
                            drawLine(wireColor, p000, p100, strokeWidth = 1.2f, pathEffect = dashEffect)
                            drawLine(wireColor, p100, p110, strokeWidth = 1.2f, pathEffect = dashEffect)
                            drawLine(wireColor, p110, p010, strokeWidth = 1.2f, pathEffect = dashEffect)
                            drawLine(wireColor, p010, p000, strokeWidth = 1.2f, pathEffect = dashEffect)

                            // Vertical pillars
                            drawLine(wireColor, p000, p001, strokeWidth = 1.2f, pathEffect = dashEffect)
                            drawLine(wireColor, p100, p101, strokeWidth = 1.2f, pathEffect = dashEffect)
                            drawLine(wireColor, p110, p111, strokeWidth = 1.2f, pathEffect = dashEffect)
                            drawLine(wireColor, p010, p011, strokeWidth = 1.2f, pathEffect = dashEffect)

                            // Top face
                            val topWireColor = Color(0x6000E5FF)
                            drawLine(topWireColor, p001, p101, strokeWidth = 1.5f)
                            drawLine(topWireColor, p101, p111, strokeWidth = 1.5f)
                            drawLine(topWireColor, p111, p011, strokeWidth = 1.5f)
                            drawLine(topWireColor, p011, p001, strokeWidth = 1.5f)

                            // Safety Clearance Plane (Z-Safe)
                            val zSafe = toolpathMetrics.zSafeRapid
                            val ps00 = projectPoint(x0, y0, zSafe, zoomScale, panOffset, perspective)
                            val ps10 = projectPoint(x1, y0, zSafe, zoomScale, panOffset, perspective)
                            val ps11 = projectPoint(x1, y1, zSafe, zoomScale, panOffset, perspective)
                            val ps01 = projectPoint(x0, y1, zSafe, zoomScale, panOffset, perspective)

                            val safePoly = Path().apply {
                                moveTo(ps00.x, ps00.y)
                                lineTo(ps10.x, ps10.y)
                                lineTo(ps11.x, ps11.y)
                                lineTo(ps01.x, ps01.y)
                                close()
                            }
                            drawPath(safePoly, color = Color(0x0C00E676))
                            val safeBorderColor = Color(0x3300E676)
                            drawLine(safeBorderColor, ps00, ps10, strokeWidth = 1f, pathEffect = dashEffect)
                            drawLine(safeBorderColor, ps10, ps11, strokeWidth = 1f, pathEffect = dashEffect)
                            drawLine(safeBorderColor, ps11, ps01, strokeWidth = 1f, pathEffect = dashEffect)
                            drawLine(safeBorderColor, ps01, ps00, strokeWidth = 1f, pathEffect = dashEffect)
                        }

                        // 4. Render G-Code Toolpath Segments with Dynamic Chromatic History & Adaptive LOD
                        val rapidDash = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                        val currentLineNum = gcodeList.getOrNull(effectiveIndex)?.lineNumber ?: -1

                        renderedSegments.forEach { seg ->
                            if (seg.isRapid && !showRapids) return@forEach

                            val p1 = projectPoint(seg.startX, seg.startY, seg.startZ, zoomScale, panOffset, perspective)
                            val p2 = projectPoint(seg.endX, seg.endY, seg.endZ, zoomScale, panOffset, perspective)

                            val isCurrent = seg.lineNumber == currentLineNum
                            val isPast = seg.lineNumber < currentLineNum
                            val isArc = seg.rawText.contains("G2") || seg.rawText.contains("G3") ||
                                    seg.rawText.contains("G02") || seg.rawText.contains("G03")

                            if (seg.isRapid) {
                                drawLine(
                                    color = if (isCurrent) CncActiveGreen else CncWarningAmber.copy(alpha = 0.65f),
                                    start = p1,
                                    end = p2,
                                    strokeWidth = if (isCurrent) 3.5f else 1.5f,
                                    pathEffect = rapidDash
                                )
                            } else if (seg.isCut) {
                                when {
                                    isCurrent -> {
                                        // Current active block: Neon Green with extra stroke width
                                        drawLine(
                                            color = CncActiveGreen,
                                            start = p1,
                                            end = p2,
                                            strokeWidth = 4.5f
                                        )
                                    }
                                    isPast -> {
                                        // Machined history: Dimmed Forest Green
                                        drawLine(
                                            color = Color(0xFF2E7D32).copy(alpha = 0.65f),
                                            start = p1,
                                            end = p2,
                                            strokeWidth = 2f
                                        )
                                    }
                                    isArc -> {
                                        // Circular & helical arcs (G2/G3): Electric Magenta
                                        drawLine(
                                            color = Color(0xFFE040FB),
                                            start = p1,
                                            end = p2,
                                            strokeWidth = 2.2f
                                        )
                                    }
                                    else -> {
                                        // Future linear cuts (G1): Industrial Cyan
                                        drawLine(
                                            color = CncCyberCyan,
                                            start = p1,
                                            end = p2,
                                            strokeWidth = 2f
                                        )
                                    }
                                }
                            }
                        }

                        // 5. Toolhead & Carbide Endmill Position
                        val toolX: Float
                        val toolY: Float
                        val toolZ: Float

                        if (isDryRunning || (gcodeList.isNotEmpty() && scrubIndex != activeLineIndex)) {
                            val activeSeg = gcodeList[effectiveIndex.coerceIn(0, gcodeList.size - 1)]
                            toolX = activeSeg.endX
                            toolY = activeSeg.endY
                            toolZ = activeSeg.endZ
                        } else {
                            toolX = axes["X"]?.workPos?.toFloat() ?: 0f
                            toolY = axes["Y"]?.workPos?.toFloat() ?: 0f
                            toolZ = axes["Z"]?.workPos?.toFloat() ?: 0f
                        }

                        val toolPos = projectPoint(toolX, toolY, toolZ, zoomScale, panOffset, perspective)

                        // Follow tool camera tracking
                        if (followTool) {
                            val unpanned = projectPoint(toolX, toolY, toolZ, zoomScale, Offset.Zero, perspective)
                            panOffset = Offset(canvasWidth / 2f - unpanned.x, canvasHeight / 2f - unpanned.y)
                        }

                        // Draw Realistic 3D Toolholder & Carbide Endmill Flute
                        val toolRadiusPx = ((activeToolDiameter.toFloat() / 2f) * zoomScale).coerceIn(4f, 18f)

                        // 1. Toolholder ISO Cone Body (Silver/Steel Gradient)
                        val holderPath = Path().apply {
                            moveTo(toolPos.x - (toolRadiusPx * 2.2f), toolPos.y - 45f)
                            lineTo(toolPos.x + (toolRadiusPx * 2.2f), toolPos.y - 45f)
                            lineTo(toolPos.x + (toolRadiusPx * 1.3f), toolPos.y - 20f)
                            lineTo(toolPos.x - (toolRadiusPx * 1.3f), toolPos.y - 20f)
                            close()
                        }
                        drawPath(
                            path = holderPath,
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFF6B7280), Color(0xFFE5E7EB), Color(0xFF4B5563)),
                                startX = toolPos.x - 20f,
                                endX = toolPos.x + 20f
                            )
                        )

                        // 2. Collet Nut (ER20 / ISO30 Black Oxide)
                        drawRect(
                            color = Color(0xFF1F2937),
                            topLeft = Offset(toolPos.x - toolRadiusPx * 1.4f, toolPos.y - 20f),
                            size = Size(toolRadiusPx * 2.8f, 10f)
                        )

                        // 3. Carbide Cutter Shaft & Flute (Gold/Bronze TiN or Cyan Glow)
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFFB45309), Color(0xFFFBBF24), Color(0xFF92400E)),
                                startX = toolPos.x - toolRadiusPx,
                                endX = toolPos.x + toolRadiusPx
                            ),
                            topLeft = Offset(toolPos.x - toolRadiusPx, toolPos.y - 10f),
                            size = Size(toolRadiusPx * 2f, 10f)
                        )

                        // 4. Cutting Tip & Spindle Contact Halo
                        drawCircle(
                            color = CncEstopRed,
                            radius = toolRadiusPx.coerceAtLeast(3.5f),
                            center = toolPos
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    (if (isDryRunning) CncActiveGreen else CncCyberCyan).copy(alpha = 0.65f),
                                    Color.Transparent
                                ),
                                center = toolPos,
                                radius = 24f
                            ),
                            radius = 24f,
                            center = toolPos
                        )
                    }

                    // HUD Heads-Up Display Overlay (Industrial Status & Metrology)
                    if (showHud) {
                        Surface(
                            color = Color(0xDD0B131F),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, Color(0x3300E5FF)),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(6.dp)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)) {
                                // Row 1: Z-MIN & Z-SAFE
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.tp_hud_zmin, toolpathMetrics.zMinCut),
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = if (toolpathMetrics.zMinCut < -5.0f) CncWarningAmber else CncCyberCyan
                                    )
                                    Text(
                                        text = stringResource(R.string.tp_hud_zsafe, toolpathMetrics.zSafeRapid),
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = CncActiveGreen
                                    )
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                // Row 2: Tool Dia, LOD, Vc
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.tp_hud_tool_dia, activeToolDiameter),
                                        fontSize = 8.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = CncDroDigits
                                    )
                                    Text(
                                        text = if (lodStride > 1) "LOD 1:$lodStride" else "LOD 1:1",
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = if (lodStride > 1) Color(0xFF80D8FF) else CncTextMuted
                                    )
                                    Text(
                                        text = "Vc:${surfaceSpeedMMin.toInt()}",
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = CncTextSecondary
                                    )
                                }

                                // Row 3: Active Block Delta
                                if (activeSeg != null) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = stringResource(R.string.tp_hud_delta, deltaX, deltaY, deltaZ),
                                        fontSize = 8.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (activeSeg.isCut) CncActiveGreen else CncWarningAmber
                                    )
                                }
                            }
                        }
                    }

                    // Floating Zoom / Overlay Controls
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { zoomScale = (zoomScale * 1.3f).coerceAtMost(20f) },
                            modifier = Modifier
                                .size(26.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(CncSurfaceVariant.copy(alpha = 0.85f))
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = CncTextPrimary, modifier = Modifier.size(15.dp))
                        }
                        IconButton(
                            onClick = { zoomScale = (zoomScale / 1.3f).coerceAtLeast(0.5f) },
                            modifier = Modifier
                                .size(26.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(CncSurfaceVariant.copy(alpha = 0.85f))
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = CncTextPrimary, modifier = Modifier.size(15.dp))
                        }
                        IconButton(
                            onClick = performAutoFit,
                            modifier = Modifier
                                .size(26.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(CncSurfaceVariant.copy(alpha = 0.85f))
                        ) {
                            Icon(Icons.Default.FitScreen, contentDescription = stringResource(R.string.tp_autofit), tint = CncCyberCyan, modifier = Modifier.size(14.dp))
                        }
                        IconButton(
                            onClick = {
                                zoomScale = 3.2f
                                panOffset = Offset(canvasSize.width / 2f, canvasSize.height / 2f)
                            },
                            modifier = Modifier
                                .size(26.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(CncSurfaceVariant.copy(alpha = 0.85f))
                        ) {
                            Icon(Icons.Default.CenterFocusStrong, contentDescription = "Reset View", tint = CncTextPrimary, modifier = Modifier.size(14.dp))
                        }
                    }
                }

                // G-Code Line-By-Line Tracker List (Collapsible on mobile)
                if (showCodePanel) {
                    Surface(
                        color = CncSurface,
                        shape = RoundedCornerShape(8.dp),
                        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(CncCardBorder)),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp)
                        ) {
                            itemsIndexed(gcodeList) { index, seg ->
                                val isCurrent = index == effectiveIndex
                                val isArc = seg.rawText.contains("G2") || seg.rawText.contains("G3") ||
                                        seg.rawText.contains("G02") || seg.rawText.contains("G03")

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            when {
                                                isCurrent -> CncActiveGreen.copy(alpha = 0.25f)
                                                index < effectiveIndex -> Color(0xFF1B5E20).copy(alpha = 0.15f)
                                                else -> Color.Transparent
                                            }
                                        )
                                        .clickable {
                                            isDryRunning = false
                                            scrubIndex = index
                                        }
                                        .padding(horizontal = 5.dp, vertical = 2.5f.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = String.format(Locale.US, "%03d", seg.lineNumber),
                                        fontSize = 8.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (isCurrent) CncActiveGreen else CncTextMuted,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = seg.rawText,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = when {
                                            isCurrent -> CncTextPrimary
                                            seg.isRapid -> CncWarningAmber
                                            isArc -> Color(0xFFE040FB)
                                            seg.isCut -> CncCyberCyan
                                            else -> CncTextSecondary
                                        },
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Dry-Run Simulation & Touch Scrubbing Control Bar
            Surface(
                color = CncSurfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)) {
                    // Top row: Play/Pause, Speed multipliers, Slider, and Line counter
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Play / Pause Dry-Run
                        FilledTonalButton(
                            onClick = { isDryRunning = !isDryRunning },
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (isDryRunning) CncWarningAmber else CncActiveGreen,
                                contentColor = Color(0xFF00363D)
                            ),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Icon(
                                imageVector = if (isDryRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isDryRunning) stringResource(R.string.tp_sim_pause) else stringResource(R.string.tp_sim_play),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (isDryRunning) stringResource(R.string.tp_sim_pause) else stringResource(R.string.tp_sim_play),
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        // Simulation Speeds (1X, 2X, 5X, 10X)
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF080D16))
                                .padding(1.dp),
                            horizontalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            listOf(1, 2, 5, 10).forEach { spd ->
                                val isSelected = simSpeed == spd
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(if (isSelected) CncCyberCyan else Color.Transparent)
                                        .clickable { simSpeed = spd }
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${spd}X",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color(0xFF00363D) else CncTextSecondary
                                    )
                                }
                            }
                        }

                        // Scrubbing Slider
                        Slider(
                            value = effectiveIndex.toFloat(),
                            onValueChange = {
                                isDryRunning = false
                                scrubIndex = it.toInt()
                            },
                            valueRange = 0f..max(1, gcodeList.size - 1).toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = CncActiveGreen,
                                activeTrackColor = CncCyberCyan,
                                inactiveTrackColor = CncCardBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(22.dp)
                        )

                        // Distance to Go & Line badge
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.tp_block_info, effectiveIndex + 1, gcodeList.size, (progressPct * 100).toInt()),
                                fontSize = 9.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = CncCyberCyan
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    // Bottom Telemetry Row: Run/Rem Time, DTG, Surface Speed, Active Raw Command
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Timer, contentDescription = null, tint = CncCyberCyan, modifier = Modifier.size(12.dp))
                            Text(
                                text = stringResource(
                                    R.string.toolpath_run_time,
                                    String.format(Locale.US, "%02d:%02d", elapsedSeconds / 60, elapsedSeconds % 60)
                                ),
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = CncTextPrimary
                            )
                            Text(
                                text = stringResource(
                                    R.string.toolpath_rem_time,
                                    String.format(Locale.US, "%02d:%02d", remainingSeconds / 60, remainingSeconds % 60)
                                ),
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = CncWarningAmber
                            )
                            Text(
                                text = "DTG: ${String.format(Locale.US, "%.1f", distanceToGoMm)} mm",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF69F0AE)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (gcodeList.isNotEmpty() && effectiveIndex in gcodeList.indices) {
                                Text(
                                    text = gcodeList[effectiveIndex].rawText,
                                    fontSize = 8.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = CncTextSecondary,
                                    maxLines = 1
                                )
                            }
                            Text(
                                text = stringResource(R.string.tp_vc_info, surfaceSpeedMMin.toInt()),
                                fontSize = 8.5.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CncTextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    LinearProgressIndicator(
                        progress = { progressPct },
                        color = CncActiveGreen,
                        trackColor = CncCardBorder,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(1.5f.dp))
                    )
                }
            }
        }
    }
}
}

/**
 * Highly optimized 2.5D Isometric and Orthogonal projection function.
 * Avoids any heap allocation or Matrix class overhead.
 */
private fun projectPoint(
    x: Float,
    y: Float,
    z: Float,
    scale: Float,
    pan: Offset,
    persp: ViewPerspective
): Offset {
    return when (persp) {
        ViewPerspective.TOP_XY -> Offset(pan.x + (x * scale), pan.y - (y * scale))
        ViewPerspective.FRONT_XZ -> Offset(pan.x + (x * scale), pan.y - (z * scale))
        ViewPerspective.SIDE_YZ -> Offset(pan.x + (y * scale), pan.y - (z * scale))
        ViewPerspective.ISO_3D -> {
            val isoX = pan.x + ((x - (y * 0.707f)) * scale * 0.85f)
            val isoY = pan.y - ((z + ((x + y) * 0.353f)) * scale * 0.85f)
            Offset(isoX, isoY)
        }
    }
}

@Composable
private fun FilterToggleChip(
    label: String,
    isActive: Boolean,
    activeColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (isActive) activeColor.copy(alpha = 0.2f) else Color.Transparent)
            .border(
                1.dp,
                if (isActive) activeColor else CncCardBorder.copy(alpha = 0.6f),
                RoundedCornerShape(4.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 2.5f.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isActive) activeColor else CncTextSecondary,
                    modifier = Modifier.size(10.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
            }
            Text(
                text = label,
                fontSize = 8.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                color = if (isActive) activeColor else CncTextSecondary
            )
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(text = label, fontSize = 7.5.sp, color = CncTextMuted)
    }
}
