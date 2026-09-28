package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.model.ProbeInfo
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
import com.example.ui.theme.CncTextPrimary
import com.example.ui.theme.CncTextSecondary
import com.example.ui.theme.CncWarningAmber
import java.util.Locale

data class ProbeRoutineItem(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val gcodeMacro: String,
)

@Composable
fun ProbingView(
    probeInfo: ProbeInfo,
    onExecuteRoutine: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val routines = listOf(
        ProbeRoutineItem("bore_center", stringResource(R.string.probe_bore), stringResource(R.string.probe_desc_bore), Icons.Default.RadioButtonUnchecked, "O100 CALL [BORE_CENTER]"),
        ProbeRoutineItem("boss_center", stringResource(R.string.probe_boss), stringResource(R.string.probe_desc_boss), Icons.Default.Adjust, "O101 CALL [BOSS_CENTER]"),
        ProbeRoutineItem("corner_out", stringResource(R.string.probe_corner_xy), stringResource(R.string.probe_desc_corner_out), Icons.Default.CropFree, "O102 CALL [CORNER_OUT]"),
        ProbeRoutineItem("corner_in", stringResource(R.string.probe_corner_in_title), stringResource(R.string.probe_desc_corner_in), Icons.Default.FullscreenExit, "O103 CALL [CORNER_IN]"),
        ProbeRoutineItem(
            id = "edge_x",
            title = stringResource(R.string.probe_edge_x_title),
            description = stringResource(R.string.probe_desc_edge_x),
            icon = Icons.AutoMirrored.Filled.CompareArrows,
            gcodeMacro = "O104 CALL [EDGE_X]",
        ),
        ProbeRoutineItem("edge_y", stringResource(R.string.probe_edge_y_title), stringResource(R.string.probe_desc_edge_y), Icons.Default.SwapVert, "O105 CALL [EDGE_Y]"),
        ProbeRoutineItem("toolsetter_z", stringResource(R.string.probe_surface_z), stringResource(R.string.probe_desc_toolsetter), Icons.Default.VerticalAlignBottom, "O106 CALL [TOOLSETTER_Z]")
    )

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val isCompact = this.maxWidth < 500.dp

        Card(
            colors = CardDefaults.cardColors(containerColor = CncCardBg),
            shape = RoundedCornerShape(12.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CncCardBorder)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(if (isCompact) 10.dp else 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Header & Sensor Tripped Status LED
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = stringResource(R.string.probing_title),
                            tint = CncCyberCyan,
                            modifier = Modifier.size(if (isCompact) 16.dp else 18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.probing_title),
                            fontWeight = FontWeight.Black,
                            fontSize = if (isCompact) 11.sp else 12.sp,
                            color = CncTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Probe Tripped Sensor Indicator
                    val isTripped = probeInfo.isTripped
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isTripped) CncEstopRed.copy(alpha = 0.2f) else CncActiveGreen.copy(alpha = 0.15f))
                            .border(1.dp, if (isTripped) CncEstopRed else CncActiveGreen, RoundedCornerShape(6.dp))
                            .padding(horizontal = if (isCompact) 6.dp else 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (isCompact) 7.dp else 8.dp)
                                .clip(CircleShape)
                                .background(if (isTripped) CncEstopRed else CncActiveGreen)
                        )
                        Spacer(modifier = Modifier.width(if (isCompact) 4.dp else 6.dp))
                        Text(
                            text = if (isTripped) stringResource(R.string.probe_status_tripped) else stringResource(R.string.probe_status_clear),
                            fontSize = if (isCompact) 9.sp else 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isTripped) CncEstopRed else CncActiveGreen,
                            maxLines = 1
                        )
                    }
                }

                // Last Contact Coords Card
                Surface(
                    color = CncSurface,
                    shape = RoundedCornerShape(8.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CncCardBorder)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(
                            Triple("X", AxisXColor, probeInfo.lastContactX),
                            Triple("Y", AxisYColor, probeInfo.lastContactY),
                            Triple("Z", AxisZColor, probeInfo.lastContactZ)
                        ).forEach { (axisName, color, value) ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(axisName, fontSize = 11.sp, color = color, fontWeight = FontWeight.Black)
                                Text(
                                    String.format(Locale.US, "%+07.3f", value),
                                    fontSize = if (isCompact) 12.sp else 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = CncDroDigits
                                )
                            }
                        }
                    }
                }

                // Routine Selection Grid
                Text(stringResource(R.string.probe_routines_header), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CncTextSecondary)

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    routines.forEach { routine ->
                        val isExecuting = probeInfo.activeRoutine == routine.id
                        Surface(
                            color = if (isExecuting) CncSurfaceVariant else CncSurface,
                            shape = RoundedCornerShape(8.dp),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(if (isExecuting) CncCyberCyan else CncCardBorder)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onExecuteRoutine(routine.id) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(if (isCompact) 8.dp else 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .size(if (isCompact) 32.dp else 36.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(CncSurfaceVariant)
                                            .border(1.dp, CncCardBorder, RoundedCornerShape(6.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(imageVector = routine.icon, contentDescription = routine.title, tint = CncCyberCyan, modifier = Modifier.size(if (isCompact) 18.dp else 20.dp))
                                    }
                                    Spacer(modifier = Modifier.width(if (isCompact) 8.dp else 10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = routine.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = if (isCompact) 11.sp else 12.sp,
                                            color = CncTextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = routine.description,
                                            fontSize = if (isCompact) 9.sp else 10.sp,
                                            color = CncTextSecondary,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                FilledTonalButton(
                                    onClick = { onExecuteRoutine(routine.id) },
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = if (isCompact) 8.dp else 10.dp, vertical = 2.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = if (isExecuting) CncWarningAmber else CncSurfaceVariant,
                                        contentColor = if (isExecuting) Color.Black else CncCyberCyan
                                    ),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text(if (isExecuting) stringResource(R.string.probe_executing) else stringResource(R.string.common_execute), fontSize = if (isCompact) 9.sp else 10.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
