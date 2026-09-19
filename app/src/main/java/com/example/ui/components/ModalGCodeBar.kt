package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.ModalGCodeState
import com.example.ui.theme.CncActiveGreen
import com.example.ui.theme.CncCardBg
import com.example.ui.theme.CncCardBorder
import com.example.ui.theme.CncCyberCyan
import com.example.ui.theme.CncDroDigits
import com.example.ui.theme.CncTextMuted
import com.example.ui.theme.CncTextSecondary
import com.example.ui.theme.CncWarningAmber
import java.util.Locale

@Composable
fun ModalGCodeBar(
    modifier: Modifier = Modifier,
    modalState: ModalGCodeState,
) {
    Surface(
        color = CncCardBg,
        shape = RoundedCornerShape(8.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CncCardBorder)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = CncCyberCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.modal_bar_title),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CncTextSecondary,
                        letterSpacing = 0.5.sp
                    )
                }

                // Tool length offset indicator
                if (modalState.toolLengthComp.contains("G43")) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color(0xFF003822), RoundedCornerShape(4.dp))
                            .border(1.dp, CncActiveGreen.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Straighten,
                            contentDescription = null,
                            tint = CncActiveGreen,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "G43 H${modalState.activeToolNumber} (Z+${String.format(Locale.US, "%.3f", modalState.toolLengthZOffsetMm)}mm)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = CncActiveGreen
                        )
                    }
                }
            }

            // Horizontally scrollable chip row for active codes
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ModalCodeChip(
                    label = "MOTION",
                    code = modalState.motionMode,
                    chipColor = if (modalState.motionMode.contains("G0")) CncWarningAmber else CncCyberCyan
                )
                ModalCodeChip(
                    label = "PLANE",
                    code = modalState.planeSelect,
                    chipColor = CncDroDigits
                )
                ModalCodeChip(
                    label = "DIST",
                    code = modalState.distanceMode,
                    chipColor = Color(0xFF64B5F6)
                )
                ModalCodeChip(
                    label = "FEED",
                    code = modalState.feedMode,
                    chipColor = Color(0xFF81C784)
                )
                ModalCodeChip(
                    label = "UNITS",
                    code = modalState.unitsMode,
                    chipColor = Color(0xFFFFB74D)
                )
                ModalCodeChip(
                    label = "RADIUS",
                    code = modalState.cutterRadiusComp,
                    chipColor = if (modalState.cutterRadiusComp.contains("G40")) CncTextMuted else Color(0xFFBA68C8)
                )
                ModalCodeChip(
                    label = "SPINDLE",
                    code = modalState.spindleMode,
                    chipColor = if (modalState.spindleMode.contains("M5")) CncTextMuted else CncActiveGreen
                )
                ModalCodeChip(
                    label = "COOLANT",
                    code = modalState.coolantMode,
                    chipColor = if (modalState.coolantMode.contains("M9")) CncTextMuted else Color(0xFF4DD0E1)
                )
            }
        }
    }
}

@Composable
private fun ModalCodeChip(
    label: String,
    code: String,
    chipColor: Color,
) {
    Box(
        modifier = Modifier
            .background(Color(0xFF141923), RoundedCornerShape(4.dp))
            .border(0.5.dp, Color(0xFF2C3440), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$label:",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF7E8B9B)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = code,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = chipColor
            )
        }
    }
}
