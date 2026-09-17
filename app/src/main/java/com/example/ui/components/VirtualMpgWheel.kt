package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.model.AxisCoord
import com.example.model.MpgMultiplier
import com.example.model.UnitSystem
import com.example.ui.theme.CncAxisA
import com.example.ui.theme.CncAxisBlue
import com.example.ui.theme.CncAxisGreen
import com.example.ui.theme.CncAxisRed
import com.example.ui.theme.CncCardBg
import com.example.ui.theme.CncCardBorder
import com.example.ui.theme.CncCyberCyan
import com.example.ui.theme.CncEstopRed
import com.example.ui.theme.CncRunningGreen
import com.example.ui.theme.CncSurfaceBg
import com.example.ui.theme.CncSurfaceVariant
import com.example.ui.theme.CncTextMuted
import com.example.ui.theme.CncTextPrimary
import com.example.ui.theme.CncTextSecondary
import com.example.ui.theme.CncWarningAmber
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun VirtualMpgWheel(
    axes: Map<String, AxisCoord>,
    selectedAxis: String,
    selectedMultiplier: MpgMultiplier,
    onSelectAxis: (String) -> Unit,
    onSelectMultiplier: (MpgMultiplier) -> Unit,
    onMpgStep: (axis: String, direction: Int, multiplier: MpgMultiplier) -> Unit,
    onZeroSelectedAxis: (String) -> Unit,
    modifier: Modifier = Modifier,
    unitSystem: UnitSystem = UnitSystem.METRIC,
    isEnabled: Boolean = true,
) {
    var rotationAngleDeg by remember { mutableFloatStateOf(0f) }
    var accumulatedAngleDelta by remember { mutableFloatStateOf(0f) }
    var lastTickDirection by remember { mutableIntStateOf(0) } // +1 CW, -1 CCW

    val currentAxisCoord = axes[selectedAxis]?.workPos ?: 0.0
    val textMeasurer = rememberTextMeasurer()

    Card(
        colors = CardDefaults.cardColors(containerColor = CncCardBg),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(CncCardBorder)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.RotateRight,
                        contentDescription = "MPG Wheel",
                        tint = CncCyberCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.mpg_header),
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp,
                        color = CncTextPrimary
                    )
                }

                Surface(
                    color = CncSurfaceVariant,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.mpg_detents),
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace,
                        color = CncTextSecondary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Axis Selector Buttons (X, Y, Z, A)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("X", "Y", "Z", "A").forEach { axisName ->
                    val isAvailable = axes.containsKey(axisName)
                    val isSelected = selectedAxis == axisName
                    val axisColor = when (axisName) {
                        "X" -> CncAxisRed
                        "Y" -> CncAxisGreen
                        "Z" -> CncAxisBlue
                        else -> CncAxisA
                    }

                    Surface(
                        onClick = { if (isAvailable) onSelectAxis(axisName) },
                        enabled = isAvailable && isEnabled,
                        color = if (isSelected) axisColor else CncSurfaceVariant,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "AXIS $axisName",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isSelected) Color.Black else if (isAvailable) CncTextPrimary else CncTextMuted
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Multiplier Selector Buttons (x1, x10, x100, x1000)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MpgMultiplier.entries.forEach { mult ->
                    val isSelected = selectedMultiplier == mult
                    Surface(
                        onClick = { onSelectMultiplier(mult) },
                        enabled = isEnabled,
                        color = if (isSelected) CncCyberCyan else CncSurfaceVariant,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize().padding(horizontal = 2.dp)
                        ) {
                            Text(
                                text = mult.getLabel(unitSystem).substringBefore(" "),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (isSelected) Color(0xFF00363D) else CncTextSecondary
                            )
                            Text(
                                text = mult.getLabel(unitSystem).substringAfter("(").substringBefore(")"),
                                fontSize = 7.5.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (isSelected) Color(0xFF00363D).copy(alpha = 0.8f) else CncTextMuted
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Central MPG Rotary Wheel and Live Readout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Interactive MPG Dial Canvas
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(190.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFF2A3342), Color(0xFF141923), Color(0xFF0D1117))
                            )
                        )
                        .border(3.dp, Brush.sweepGradient(listOf(Color(0xFF4B5563), Color(0xFF1F2937), Color(0xFF6B7280), Color(0xFF1F2937))), CircleShape)
                        .pointerInput(selectedAxis, selectedMultiplier, isEnabled) {
                            if (!isEnabled) return@pointerInput
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val touchPos = change.position
                                val prevPos = touchPos - dragAmount

                                val angleCurrent = atan2(touchPos.y - center.y, touchPos.x - center.x)
                                val anglePrev = atan2(prevPos.y - center.y, prevPos.x - center.x)
                                var deltaAngle = (angleCurrent - anglePrev) * (180.0 / Math.PI).toFloat()

                                // Normalize angle jump across 180/-180 boundary
                                if (deltaAngle > 180f) deltaAngle -= 360f
                                if (deltaAngle < -180f) deltaAngle += 360f

                                rotationAngleDeg = (rotationAngleDeg + deltaAngle) % 360f
                                accumulatedAngleDelta += deltaAngle

                                // Each detent click is 3.6 degrees (360 / 100 divisions)
                                val stepThresholdDeg = 3.6f
                                while (abs(accumulatedAngleDelta) >= stepThresholdDeg) {
                                    val dir = if (accumulatedAngleDelta > 0) 1 else -1
                                    accumulatedAngleDelta -= (dir * stepThresholdDeg)
                                    lastTickDirection = dir
                                    onMpgStep(selectedAxis, dir, selectedMultiplier)
                                }
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val radius = (size.minDimension / 2f) - 8.dp.toPx()

                        // Draw outer graduated ring ticks (100 divisions)
                        rotate(rotationAngleDeg, pivot = center) {
                            for (i in 0 until 100) {
                                val angleRad = ((i * 3.6 * Math.PI) / 180.0).toFloat()
                                val isMajor = ((i % 10) == 0)
                                val isMedium = (((i % 5) == 0) && !isMajor)

                                val tickLength = if (isMajor) 14.dp.toPx() else if (isMedium) 9.dp.toPx() else 5.dp.toPx()
                                val tickWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx()
                                val tickColor = if (isMajor) CncCyberCyan else if (isMedium) Color.White.copy(alpha = 0.8f) else Color.Gray.copy(alpha = 0.5f)

                                val startX = center.x + (radius - tickLength) * cos(angleRad)
                                val startY = center.y + (radius - tickLength) * sin(angleRad)
                                val endX = center.x + radius * cos(angleRad)
                                val endY = center.y + radius * sin(angleRad)

                                drawLine(
                                    color = tickColor,
                                    start = Offset(startX, startY),
                                    end = Offset(endX, endY),
                                    strokeWidth = tickWidth,
                                    cap = StrokeCap.Round
                                )

                                // Draw numbers on major increments (0, 20, 40, 60, 80)
                                if (i % 20 == 0) {
                                    val textRad = (i * 3.6 * Math.PI / 180.0).toFloat()
                                    val textX = center.x + (radius - 22.dp.toPx()) * cos(textRad)
                                    val textY = center.y + (radius - 22.dp.toPx()) * sin(textRad)

                                    drawText(
                                        textMeasurer = textMeasurer,
                                        text = i.toString(),
                                        topLeft = Offset(textX - 8.dp.toPx(), textY - 6.dp.toPx()),
                                        style = TextStyle(
                                            color = CncCyberCyan,
                                            fontSize = 7.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    )
                                }
                            }

                            // Draw finger crank dimple (machined aluminum socket)
                            val crankDistance = radius * 0.55f
                            val crankAngle = 0.0f
                            val crankCenter = Offset(
                                center.x + crankDistance * cos(crankAngle),
                                center.y + crankDistance * sin(crankAngle)
                            )

                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0xFF4B5563), Color(0xFF1E293B), Color(0xFF0F172A)),
                                    center = crankCenter,
                                    radius = 16.dp.toPx()
                                ),
                                radius = 16.dp.toPx(),
                                center = crankCenter
                            )
                            drawCircle(
                                color = CncCyberCyan,
                                radius = 16.dp.toPx(),
                                center = crankCenter,
                                style = Stroke(width = 1.5.dp.toPx())
                            )
                            drawCircle(
                                color = CncCyberCyan.copy(alpha = 0.8f),
                                radius = 5.dp.toPx(),
                                center = crankCenter
                            )
                        }

                        // Static reference index line at top (12 o'clock position)
                        drawLine(
                            color = CncEstopRed,
                            start = Offset(center.x, center.y - radius - 6.dp.toPx()),
                            end = Offset(center.x, center.y - radius + 16.dp.toPx()),
                            strokeWidth = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }

                    // Center Hub Display
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(CncSurfaceBg)
                            .border(1.5.dp, CncCardBorder, CircleShape)
                            .padding(4.dp)
                    ) {
                        Text(
                            text = selectedAxis,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = when (selectedAxis) {
                                "X" -> CncAxisRed
                                "Y" -> CncAxisGreen
                                "Z" -> CncAxisBlue
                                else -> CncAxisA
                            }
                        )
                        Text(
                            text = selectedMultiplier.getLabel(unitSystem).substringBefore(" "),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = CncCyberCyan
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (lastTickDirection > 0) "CW ↷" else if (lastTickDirection < 0) "CCW ↶" else "STOP",
                                fontSize = 7.5.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (lastTickDirection != 0) CncRunningGreen else CncTextSecondary
                            )
                        }
                    }
                }

                // Side Telemetry & Quick Action Column
                Column(
                    modifier = Modifier.width(135.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Work Position Display
                    Surface(
                        color = CncSurfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(CncCardBorder)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val isRotary = selectedAxis in listOf("A", "B", "C")
                        val formattedPos = if (isRotary) {
                            String.format(java.util.Locale.US, "%+08.3f°", currentAxisCoord)
                        } else {
                            "${unitSystem.formatPosition(currentAxisCoord)} ${unitSystem.lengthUnit}"
                        }
                        val stepStr = if (isRotary) {
                            "±${selectedMultiplier.stepMm}°"
                        } else if (unitSystem == UnitSystem.IMPERIAL) {
                            "±${java.lang.String.format(java.util.Locale.US, "%.4f", selectedMultiplier.getStep(unitSystem) / 25.4)} in"
                        } else {
                            "±${selectedMultiplier.stepMm} mm"
                        }

                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = stringResource(R.string.mpg_axis_select, selectedAxis),
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = CncTextSecondary
                            )
                            Text(
                                text = formattedPos,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = CncTextPrimary
                            )
                            Text(
                                text = "INC: $stepStr",
                                fontSize = 8.5.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CncWarningAmber
                            )
                        }
                    }

                    // Zero Axis Button
                    Button(
                        onClick = { onZeroSelectedAxis(selectedAxis) },
                        enabled = isEnabled,
                        colors = ButtonDefaults.buttonColors(containerColor = CncSurfaceVariant),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Adjust, contentDescription = stringResource(R.string.common_zero), tint = CncCyberCyan, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.common_zero) + " $selectedAxis (G92)", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = CncCyberCyan)
                    }

                    // Rapid Stepping Direction Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { onMpgStep(selectedAxis, -1, selectedMultiplier) },
                            enabled = isEnabled,
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = CncSurfaceVariant),
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                        ) {
                            Text(stringResource(R.string.mpg_step_neg), fontSize = 10.sp, fontWeight = FontWeight.Black, color = CncAxisRed)
                        }

                        FilledTonalButton(
                            onClick = { onMpgStep(selectedAxis, 1, selectedMultiplier) },
                            enabled = isEnabled,
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = CncSurfaceVariant),
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                        ) {
                            Text(stringResource(R.string.mpg_step_pos), fontSize = 10.sp, fontWeight = FontWeight.Black, color = CncAxisGreen)
                        }
                    }
                }
            }
        }
    }
}
