package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
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
import com.example.model.MaterialPreset
import com.example.model.SpeedFeedCalculation
import com.example.ui.theme.*
import java.util.Locale
import kotlin.math.PI
import kotlin.math.roundToInt

val STANDARD_MATERIALS = listOf(
    MaterialPreset("mat_al6061", R.string.mat_al6061_name, R.string.mat_al6061_cat, 220.0, 0.045, 0.8),
    MaterialPreset("mat_steel1018", R.string.mat_steel1018_name, R.string.mat_steel1018_cat, 90.0, 0.035, 1.8),
    MaterialPreset("mat_ss304", R.string.mat_ss304_name, R.string.mat_ss304_cat, 55.0, 0.025, 2.2),
    MaterialPreset("mat_wood", R.string.mat_wood_name, R.string.mat_wood_cat, 350.0, 0.080, 0.3),
    MaterialPreset("mat_pom", R.string.mat_pom_name, R.string.mat_pom_cat, 180.0, 0.060, 0.4),
)

@Composable
fun SpeedsFeedsCalculatorDialog(
    onDismiss: () -> Unit,
    onApplyToCnc: (rpm: Double, feedMmMin: Double) -> Unit,
) {
    var selectedMaterial by remember { mutableStateOf(STANDARD_MATERIALS[0]) }
    var toolDiameter by remember { mutableDoubleStateOf(6.0) }
    var flutes by remember { mutableIntStateOf(2) }
    var isRoughing by remember { mutableStateOf(value = false) }

    // Formula Calculations
    val vc = if (isRoughing) selectedMaterial.surfaceSpeedMMin * 0.85 else selectedMaterial.surfaceSpeedMMin
    val calculatedRpm = ((vc * 1000.0) / (PI * toolDiameter)).coerceIn(500.0, 24000.0)
    val fz = if (isRoughing) selectedMaterial.feedPerToothMm * 1.3 else selectedMaterial.feedPerToothMm
    val calculatedFeed = (calculatedRpm * flutes * fz).coerceIn(50.0, 10000.0)
    val docAp = if (isRoughing) toolDiameter * 0.75 else toolDiameter * 0.25
    val wocAe = if (isRoughing) toolDiameter * 0.4 else toolDiameter * 0.1
    val estimatedPowerKw = (calculatedFeed * docAp * wocAe * selectedMaterial.powerFactor / 60000.0).coerceAtLeast(0.1)

    val currentCalculation = SpeedFeedCalculation(
        material = selectedMaterial,
        toolDiameterMm = toolDiameter,
        flutes = flutes,
        calculatedRpm = calculatedRpm,
        calculatedFeedMmMin = calculatedFeed,
        recommendedDocMm = docAp,
        recommendedWocMm = wocAe,
        spindlePowerKw = estimatedPowerKw,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Calculate, contentDescription = stringResource(R.string.calc_header), tint = CncCyberCyan, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.calc_header), fontWeight = FontWeight.Black, fontSize = 14.sp, color = CncTextPrimary)
                }

                IconButton(
                    onClick = {
                        println("SpeedFeedCalculation: $currentCalculation")
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = "Log Detail", tint = CncCyberCyan, modifier = Modifier.size(18.dp))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Material Selector
                Text(stringResource(R.string.calc_material_header), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CncTextSecondary)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    STANDARD_MATERIALS.forEach { mat ->
                        val isSelected = selectedMaterial.id == mat.id
                        Surface(
                            color = if (isSelected) CncSurfaceVariant else CncSurface,
                            shape = RoundedCornerShape(6.dp),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(if (isSelected) CncCyberCyan else CncCardBorder)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedMaterial = mat }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(stringResource(mat.nameRes), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = CncTextPrimary)
                                    Text("Vc: ${mat.surfaceSpeedMMin.toInt()} m/min • ${stringResource(mat.categoryRes)}", fontSize = 9.sp, color = CncTextSecondary)
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = "Selected", tint = CncActiveGreen, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                // Tool Diameter and Flutes Row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.calc_diameter_label), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CncTextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(3.0, 6.0, 8.0, 12.0).forEach { dia ->
                                val isSel = toolDiameter == dia
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(30.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isSel) CncCyberCyan else CncSurface)
                                        .border(1.dp, CncCardBorder, RoundedCornerShape(4.dp))
                                        .clickable { toolDiameter = dia }
                                ) {
                                    Text("${dia.toInt()}mm", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color(0xFF00363D) else CncTextPrimary)
                                }
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.calc_flutes_label), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CncTextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(1, 2, 3, 4).forEach { f ->
                                val isSel = flutes == f
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(30.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isSel) CncCyberCyan else CncSurface)
                                        .border(1.dp, CncCardBorder, RoundedCornerShape(4.dp))
                                        .clickable { flutes = f }
                                ) {
                                    Text("${f}F", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color(0xFF00363D) else CncTextPrimary)
                                }
                            }
                        }
                    }
                }

                // Results Summary Display Card
                Surface(
                    color = CncSurface,
                    shape = RoundedCornerShape(8.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CncCyberCyan)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(stringResource(R.string.calc_spindle_speed), fontSize = 9.sp, color = CncWarningAmber, fontWeight = FontWeight.Bold)
                                Text("${calculatedRpm.roundToInt()} RPM", fontSize = 14.sp, fontWeight = FontWeight.Black, color = CncTextPrimary, fontFamily = FontFamily.Monospace)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(stringResource(R.string.calc_feedrate), fontSize = 9.sp, color = CncActiveGreen, fontWeight = FontWeight.Bold)
                                Text("${calculatedFeed.roundToInt()} mm/min", fontSize = 14.sp, fontWeight = FontWeight.Black, color = CncTextPrimary, fontFamily = FontFamily.Monospace)
                            }
                        }

                        HorizontalDivider(
                            Modifier,
                            DividerDefaults.Thickness,
                            color = CncCardBorder
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.calc_depth_ap, String.format(Locale.US, "%.2f", docAp)), fontSize = 10.sp, color = CncTextSecondary)
                            Text(stringResource(R.string.calc_stepover_ae, String.format(Locale.US, "%.2f", wocAe)), fontSize = 10.sp, color = CncTextSecondary)
                            Text(stringResource(R.string.calc_power, String.format(Locale.US, "%.2f", estimatedPowerKw)), fontSize = 10.sp, color = CncCyberCyan)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onApplyToCnc(calculatedRpm, calculatedFeed)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = CncCyberCyan,
                    contentColor = Color(0xFF00363D)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Bolt, contentDescription = stringResource(R.string.calc_apply_btn))
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.calc_apply_btn), fontWeight = FontWeight.Black, fontSize = 11.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_close), color = CncTextSecondary)
            }
        },
        containerColor = CncCardBg
    )
}
