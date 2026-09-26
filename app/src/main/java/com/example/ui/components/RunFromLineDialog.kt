package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.model.GCodeSegment
import com.example.ui.theme.CncActiveGreen
import com.example.ui.theme.CncCardBg
import com.example.ui.theme.CncCardBorder
import com.example.ui.theme.CncCyberCyan
import com.example.ui.theme.CncDroDigits
import com.example.ui.theme.CncTextMuted
import com.example.ui.theme.CncTextPrimary
import com.example.ui.theme.CncTextSecondary
import com.example.ui.theme.CncWarningAmber

@Composable
fun RunFromLineDialog(
    gcodeList: List<GCodeSegment>,
    currentLineIndex: Int,
    onDismiss: () -> Unit,
    onConfirmRunFromLine: (Int) -> Unit,
) {
    var selectedIndex by remember(currentLineIndex) { mutableIntStateOf(currentLineIndex.coerceAtLeast(0)) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CncCardBg),
            shape = RoundedCornerShape(16.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CncCardBorder)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = null,
                            tint = CncCyberCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.run_from_line_title),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = CncTextPrimary
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.run_from_line_desc),
                    fontSize = 11.sp,
                    color = CncTextSecondary,
                    lineHeight = 15.sp
                )

                // Safety checklist badge
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F2027), RoundedCornerShape(8.dp))
                        .border(1.dp, CncCyberCyan.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = CncActiveGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SAFETY AUTOMATION: Z retracts to +10.0mm clearance -> Spindle restarts at commanded RPM -> Trajectory arms.",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CncActiveGreen
                    )
                }

                // G-Code block selector
                Text(
                    text = stringResource(R.string.rfl_select_block),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = CncTextSecondary
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                        .background(Color(0xFF0C1017), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF202633), RoundedCornerShape(8.dp))
                        .padding(4.dp)
                ) {
                    if (gcodeList.isEmpty()) {
                        Text(
                            text = stringResource(R.string.rfl_no_program),
                            fontSize = 12.sp,
                            color = CncTextMuted,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            itemsIndexed(gcodeList) { index, segment ->
                                val isSelected = index == selectedIndex
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            if (isSelected) CncCyberCyan.copy(alpha = 0.2f)
                                            else Color.Transparent
                                        )
                                        .border(
                                            if (isSelected) 1.dp else 0.dp,
                                            if (isSelected) CncCyberCyan else Color.Transparent,
                                            RoundedCornerShape(4.dp)
                                        )
                                        .clickable { selectedIndex = index }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "N${segment.lineNumber.toString().padStart(4, '0')}:",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) CncCyberCyan else Color(0xFF6272A4)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = segment.rawText.trim(),
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal,
                                        color = if (isSelected) Color.White else CncTextPrimary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isSelected) {
                                        Text(
                                            text = stringResource(R.string.rfl_resume_here),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = CncDroDigits
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = stringResource(R.string.run_from_line_cancel), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            onConfirmRunFromLine(selectedIndex)
                            onDismiss()
                        },
                        enabled = gcodeList.isNotEmpty() && selectedIndex in gcodeList.indices,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CncActiveGreen,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = stringResource(R.string.run_from_line_btn), fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}
