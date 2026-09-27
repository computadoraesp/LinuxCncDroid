package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.service.BoltHoleCircleParams
import com.example.service.ConversationalCamEngine
import com.example.service.FacingCycleParams
import com.example.service.RectangularPocketParams
import com.example.ui.theme.CncActiveGreen
import com.example.ui.theme.CncCardBorder
import com.example.ui.theme.CncCyberCyan
import com.example.ui.theme.CncSurfaceBg
import com.example.ui.theme.CncSurfaceVariant
import com.example.ui.theme.CncTextPrimary
import com.example.ui.theme.CncTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationalCamDialog(
    onDismiss: () -> Unit,
    onLoadToController: (title: String, gcode: String) -> Unit,
) {
    var selectedCycleTab by remember { mutableIntStateOf(0) } // 0 = Facing, 1 = Pocket, 2 = Bolt Circle
    val clipboardManager = LocalClipboardManager.current

    // Facing Parameters
    var facingLengthX by remember { mutableStateOf("100.0") }
    var facingWidthY by remember { mutableStateOf("80.0") }
    var facingToolDiam by remember { mutableStateOf("12.0") }
    var facingStepover by remember { mutableStateOf("60.0") }
    var facingTotalDepth by remember { mutableStateOf("1.5") }
    var facingDepthPerPass by remember { mutableStateOf("0.5") }
    var facingFeed by remember { mutableStateOf("1200.0") }
    var facingRpm by remember { mutableStateOf("6000.0") }
    var facingToolNum by remember { mutableStateOf("1") }

    // Pocket Parameters
    var pocketLengthX by remember { mutableStateOf("60.0") }
    var pocketWidthY by remember { mutableStateOf("40.0") }
    var pocketCornerRad by remember { mutableStateOf("5.0") }
    var pocketTotalDepth by remember { mutableStateOf("5.0") }
    var pocketDepthPerPass by remember { mutableStateOf("1.0") }
    var pocketToolDiam by remember { mutableStateOf("6.0") }
    var pocketStepover by remember { mutableStateOf("50.0") }
    var pocketFeedXY by remember { mutableStateOf("800.0") }
    var pocketFeedZ by remember { mutableStateOf("250.0") }
    var pocketRpm by remember { mutableStateOf("12000.0") }
    var pocketToolNum by remember { mutableStateOf("2") }

    // Bolt Circle Parameters
    var boltCenterX by remember { mutableStateOf("0.0") }
    var boltCenterY by remember { mutableStateOf("0.0") }
    var boltDiameter by remember { mutableStateOf("75.0") }
    var boltHoleCount by remember { mutableStateOf("6") }
    var boltStartAngle by remember { mutableStateOf("0.0") }
    var boltDepthZ by remember { mutableStateOf("15.0") }
    var boltPeckQ by remember { mutableStateOf("3.0") }
    var boltRetractR by remember { mutableStateOf("2.0") }
    var boltFeed by remember { mutableStateOf("250.0") }
    var boltRpm by remember { mutableStateOf("3500.0") }
    var boltToolNum by remember { mutableStateOf("3") }

    // Generated G-Code State
    var generatedGCode by remember { mutableStateOf("") }
    var programTitle by remember { mutableStateOf("") }

    // Recompute G-Code whenever active cycle or inputs change
    fun recomputeGCode() {
        when (selectedCycleTab) {
            0 -> {
                programTitle = "CAM_FACING_${facingLengthX.take(4)}x${facingWidthY.take(4)}.ngc"
                val params = FacingCycleParams(
                    lengthX = facingLengthX.toDoubleOrNull() ?: 100.0,
                    widthY = facingWidthY.toDoubleOrNull() ?: 80.0,
                    toolDiameter = facingToolDiam.toDoubleOrNull() ?: 12.0,
                    stepoverPercent = facingStepover.toDoubleOrNull() ?: 60.0,
                    totalDepth = facingTotalDepth.toDoubleOrNull() ?: 1.5,
                    depthPerPass = facingDepthPerPass.toDoubleOrNull() ?: 0.5,
                    feedRate = facingFeed.toDoubleOrNull() ?: 1200.0,
                    spindleRpm = facingRpm.toDoubleOrNull() ?: 6000.0,
                    toolNumber = facingToolNum.toIntOrNull() ?: 1
                )
                generatedGCode = ConversationalCamEngine.generateFacingGCode(params)
            }
            1 -> {
                programTitle = "CAM_POCKET_${pocketLengthX.take(4)}x${pocketWidthY.take(4)}.ngc"
                val params = RectangularPocketParams(
                    pocketLengthX = pocketLengthX.toDoubleOrNull() ?: 60.0,
                    pocketWidthY = pocketWidthY.toDoubleOrNull() ?: 40.0,
                    cornerRad = pocketCornerRad.toDoubleOrNull() ?: 5.0,
                    totalDepthZ = pocketTotalDepth.toDoubleOrNull() ?: 5.0,
                    depthPerPass = pocketDepthPerPass.toDoubleOrNull() ?: 1.0,
                    toolDiameter = pocketToolDiam.toDoubleOrNull() ?: 6.0,
                    stepoverPercent = pocketStepover.toDoubleOrNull() ?: 50.0,
                    feedRateXY = pocketFeedXY.toDoubleOrNull() ?: 800.0,
                    plungeFeedZ = pocketFeedZ.toDoubleOrNull() ?: 250.0,
                    spindleRpm = pocketRpm.toDoubleOrNull() ?: 12000.0,
                    toolNumber = pocketToolNum.toIntOrNull() ?: 2
                )
                generatedGCode = ConversationalCamEngine.generateRectangularPocketGCode(params)
            }
            2 -> {
                programTitle = "CAM_BOLT_CIRCLE_${boltHoleCount}H_D${boltDiameter.take(4)}.ngc"
                val params = BoltHoleCircleParams(
                    centerX = boltCenterX.toDoubleOrNull() ?: 0.0,
                    centerY = boltCenterY.toDoubleOrNull() ?: 0.0,
                    circleDiameter = boltDiameter.toDoubleOrNull() ?: 75.0,
                    numberOfHoles = boltHoleCount.toIntOrNull() ?: 6,
                    startAngleDeg = boltStartAngle.toDoubleOrNull() ?: 0.0,
                    holeDepthZ = boltDepthZ.toDoubleOrNull() ?: 15.0,
                    peckIncrementQ = boltPeckQ.toDoubleOrNull() ?: 3.0,
                    retractPlaneR = boltRetractR.toDoubleOrNull() ?: 2.0,
                    feedRate = boltFeed.toDoubleOrNull() ?: 250.0,
                    spindleRpm = boltRpm.toDoubleOrNull() ?: 3500.0,
                    toolNumber = boltToolNum.toIntOrNull() ?: 3
                )
                generatedGCode = ConversationalCamEngine.generateBoltHoleCircleGCode(params)
            }
        }
    }

    // Run initial generation
    LaunchedEffect(selectedCycleTab) {
        recomputeGCode()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            val isCompact = this.maxWidth < 600.dp

            Card(
                colors = CardDefaults.cardColors(containerColor = CncSurfaceBg),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(CncCardBorder)),
                modifier = Modifier
                    .fillMaxWidth(if (isCompact) 1f else 0.95f)
                    .fillMaxHeight(if (isCompact) 0.98f else 0.92f)
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
                                imageVector = Icons.Default.AutoFixHigh,
                                contentDescription = null,
                                tint = CncActiveGreen,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.cam_dialog_title),
                                    fontWeight = FontWeight.Black,
                                    fontSize = if (isCompact) 12.sp else 15.sp,
                                    color = CncTextPrimary
                                )
                                Text(
                                    text = stringResource(R.string.cam_dialog_desc),
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
                                .testTag("btn_close_cam_dialog")
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = stringResource(R.string.common_close), tint = CncTextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Cycle Tab Selector
                    TabRow(
                        selectedTabIndex = selectedCycleTab,
                        containerColor = CncSurfaceVariant,
                        contentColor = CncActiveGreen,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    ) {
                        Tab(
                            selected = selectedCycleTab == 0,
                            onClick = { selectedCycleTab = 0 },
                            text = { Text(stringResource(R.string.cam_tab_facing), fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedCycleTab == 1,
                            onClick = { selectedCycleTab = 1 },
                            text = { Text(stringResource(R.string.cam_tab_pocket), fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedCycleTab == 2,
                            onClick = { selectedCycleTab = 2 },
                            text = { Text(stringResource(R.string.cam_tab_bolt_circle), fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Main 2-Column or Stack Layout (Inputs vs Preview)
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Left Column: Parameters Input
                        Column(
                            modifier = Modifier
                                .weight(if (isCompact) 1.2f else 1f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            when (selectedCycleTab) {
                                0 -> {
                                    CamInputField(stringResource(R.string.cam_label_length_x), facingLengthX) { facingLengthX = it; recomputeGCode() }
                                    CamInputField(stringResource(R.string.cam_label_width_y), facingWidthY) { facingWidthY = it; recomputeGCode() }
                                    CamInputField(stringResource(R.string.cam_label_tool_diam), facingToolDiam) { facingToolDiam = it; recomputeGCode() }
                                    CamInputField(stringResource(R.string.cam_label_stepover), facingStepover) { facingStepover = it; recomputeGCode() }
                                    CamInputField(stringResource(R.string.cam_label_total_depth), facingTotalDepth) { facingTotalDepth = it; recomputeGCode() }
                                    CamInputField(stringResource(R.string.cam_label_depth_per_pass), facingDepthPerPass) { facingDepthPerPass = it; recomputeGCode() }
                                    CamInputField(stringResource(R.string.cam_label_feed), facingFeed) { facingFeed = it; recomputeGCode() }
                                    CamInputField(stringResource(R.string.cam_label_rpm), facingRpm) { facingRpm = it; recomputeGCode() }
                                    CamInputField(stringResource(R.string.cam_label_tool_num), facingToolNum) { facingToolNum = it; recomputeGCode() }
                                }
                                1 -> {
                                    CamInputField(stringResource(R.string.cam_label_pocket_length_x), pocketLengthX) { pocketLengthX = it; recomputeGCode() }
                                    CamInputField(stringResource(R.string.cam_label_pocket_width_y), pocketWidthY) { pocketWidthY = it; recomputeGCode() }
                                    CamInputField(stringResource(R.string.cam_label_corner_radius), pocketCornerRad) { pocketCornerRad = it; recomputeGCode() }
                                    CamInputField(stringResource(R.string.cam_label_total_depth), pocketTotalDepth) { pocketTotalDepth = it; recomputeGCode() }
                                    CamInputField(stringResource(R.string.cam_label_step_z), pocketDepthPerPass) { pocketDepthPerPass = it; recomputeGCode() }
                                    CamInputField(stringResource(R.string.cam_label_tool_diam), pocketToolDiam) { pocketToolDiam = it; recomputeGCode() }
                                    CamInputField(stringResource(R.string.cam_label_stepover), pocketStepover) { pocketStepover = it; recomputeGCode() }
                                    CamInputField(stringResource(R.string.cam_label_feed_xy), pocketFeedXY) { pocketFeedXY = it; recomputeGCode() }
                                    CamInputField(stringResource(R.string.cam_label_feed_z), pocketFeedZ) { pocketFeedZ = it; recomputeGCode() }
                                    CamInputField(stringResource(R.string.cam_label_rpm), pocketRpm) { pocketRpm = it; recomputeGCode() }
                                    CamInputField(stringResource(R.string.cam_label_tool_num), pocketToolNum) { pocketToolNum = it; recomputeGCode() }
                                }
                                2 -> {
                                    CamInputField(stringResource(R.string.cam_label_center_x), boltCenterX) { boltCenterX = it; recomputeGCode() }
                                    CamInputField(stringResource(R.string.cam_label_center_y), boltCenterY) { boltCenterY = it; recomputeGCode() }
                                    CamInputField(stringResource(R.string.cam_label_pcd_diameter), boltDiameter) { boltDiameter = it; recomputeGCode() }
                                    CamInputField(stringResource(R.string.cam_label_hole_count), boltHoleCount) { boltHoleCount = it; recomputeGCode() }
                                    CamInputField(stringResource(R.string.cam_label_start_angle), boltStartAngle) { boltStartAngle = it; recomputeGCode() }
                                    CamInputField(stringResource(R.string.cam_label_hole_depth), boltDepthZ) { boltDepthZ = it; recomputeGCode() }
                                    CamInputField(stringResource(R.string.cam_label_peck_q), boltPeckQ) { boltPeckQ = it; recomputeGCode() }
                                    CamInputField(stringResource(R.string.cam_label_retract_r), boltRetractR) { boltRetractR = it; recomputeGCode() }
                                    CamInputField(stringResource(R.string.cam_label_feed), boltFeed) { boltFeed = it; recomputeGCode() }
                                    CamInputField(stringResource(R.string.cam_label_rpm), boltRpm) { boltRpm = it; recomputeGCode() }
                                    CamInputField(stringResource(R.string.cam_label_tool_num), boltToolNum) { boltToolNum = it; recomputeGCode() }
                                }
                            }
                        }

                        // Right Column: G-Code Live Viewer
                        Column(
                            modifier = Modifier
                                .weight(if (isCompact) 1.2f else 1.3f)
                                .fillMaxHeight()
                        ) {
                            Text(
                                text = "${stringResource(R.string.cam_preview_title)}: $programTitle",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CncCyberCyan
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                color = CncSurfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, CncCardBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp)
                                        .verticalScroll(rememberScrollState())
                                        .horizontalScroll(rememberScrollState())
                                ) {
                                    Text(
                                        text = generatedGCode,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = CncTextPrimary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Bottom Execution Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(generatedGCode))
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("btn_copy_cam_gcode")
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.cam_copy_gcode), fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                onLoadToController(programTitle, generatedGCode)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CncActiveGreen),
                            modifier = Modifier
                                .weight(1.5f)
                                .height(48.dp)
                                .testTag("btn_load_cam_to_cnc")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.cam_load_to_controller),
                                color = Color.Black,
                                fontWeight = FontWeight.Black,
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
private fun CamInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, fontSize = 10.sp, color = CncTextSecondary, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(2.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            textStyle = LocalTextStyle.current.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = CncTextPrimary
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CncActiveGreen,
                unfocusedBorderColor = CncCardBorder,
                focusedContainerColor = CncSurfaceVariant,
                unfocusedContainerColor = CncSurfaceVariant
            )
        )
    }
}
