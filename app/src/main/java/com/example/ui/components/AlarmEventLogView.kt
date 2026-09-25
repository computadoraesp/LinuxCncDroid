package com.example.ui.components

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
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
import com.example.model.CncEventLog
import com.example.model.LogSeverity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AlarmEventLogView(
    logs: List<CncEventLog>,
    onClearLogs: () -> Unit,
    onGenerateReport: (() -> String)? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var showAuditDialog by remember { mutableStateOf(false) }
    var generatedReportText by remember { mutableStateOf("") }

    val filterOptions = listOf(
        stringResource(R.string.logs_filter_all),
        stringResource(R.string.logs_filter_errors),
        stringResource(R.string.logs_filter_security),
        stringResource(R.string.logs_filter_warnings)
    )
    var selectedFilter by remember { mutableStateOf(filterOptions[0]) }
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.US) }

    val filteredLogs = remember(logs, selectedFilter) {
        when (selectedFilter) {
            filterOptions[1] -> logs.filter { it.severity == LogSeverity.ERROR || it.severity == LogSeverity.CRITICAL }
            filterOptions[2] -> logs.filter { it.severity == LogSeverity.SECURITY }
            filterOptions[3] -> logs.filter { it.severity == LogSeverity.WARNING }
            else -> logs
        }
    }

    if (showAuditDialog) {
        IndustrialAuditReportDialog(
            reportText = generatedReportText,
            onDismiss = { showAuditDialog = false },
            onCopy = {
                clipboardManager.setText(AnnotatedString(generatedReportText))
                Toast.makeText(context, context.getString(R.string.logs_copied_to_clipboard), Toast.LENGTH_SHORT).show()
            },
            onShare = {
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, generatedReportText)
                    putExtra(Intent.EXTRA_SUBJECT, "LinuxCNC_Audit_Report_${System.currentTimeMillis()}.txt")
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, context.getString(R.string.logs_share_report))
                context.startActivity(shareIntent)
            }
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = CncCardBg),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CncCardBorder)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = stringResource(R.string.logs_title), tint = CncWarningAmber, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.logs_title), fontWeight = FontWeight.Black, fontSize = 12.sp, color = CncTextPrimary)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (onGenerateReport != null) {
                        FilledTonalButton(
                            onClick = {
                                generatedReportText = onGenerateReport()
                                showAuditDialog = true
                            },
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = CncCyberCyan.copy(alpha = 0.15f),
                                contentColor = CncCyberCyan
                            ),
                            modifier = Modifier.height(28.dp).testTag("logs_export_audit_button")
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.Assignment, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.logs_export_audit), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    FilledTonalButton(
                        onClick = onClearLogs,
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = CncSurfaceVariant,
                            contentColor = CncTextSecondary
                        ),
                        modifier = Modifier.height(28.dp).testTag("logs_clear_button")
                    ) {
                        Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.logs_clear), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Filter Chips
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                filterOptions.forEach { filterName ->
                    val isSelected = selectedFilter == filterName
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(26.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isSelected) CncCyberCyan else CncSurfaceVariant)
                            .clickable { selectedFilter = filterName }
                    ) {
                        Text(
                            filterName,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color(0xFF00363D) else CncTextSecondary
                        )
                    }
                }
            }

            // Event Logs Listing
            Surface(
                color = CncSurface,
                shape = RoundedCornerShape(8.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CncCardBorder)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            ) {
                if (filteredLogs.isEmpty()) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(stringResource(R.string.logs_no_entries, selectedFilter), fontSize = 11.sp, color = CncTextMuted)
                    }
                } else {
                    LazyColumn(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(filteredLogs) { log ->
                            val (badgeColor, textColor) = when (log.severity) {
                                LogSeverity.CRITICAL -> Pair(CncEstopRed, CncEstopRed)
                                LogSeverity.ERROR -> Pair(CncEstopRed, CncEstopRed)
                                LogSeverity.SECURITY -> Pair(CncWarningAmber, CncWarningAmber)
                                LogSeverity.WARNING -> Pair(CncWarningAmber, CncTextPrimary)
                                LogSeverity.INFO -> Pair(CncCyberCyan, CncTextPrimary)
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(CncBackground.copy(alpha = 0.5f))
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = dateFormat.format(Date(log.timestamp)),
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = CncTextMuted
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(badgeColor.copy(alpha = 0.2f))
                                        .border(1.dp, badgeColor, RoundedCornerShape(3.dp))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = stringResource(log.severity.displayNameRes),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        color = badgeColor,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "[${log.tag}] ${log.message}",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = textColor,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IndustrialAuditReportDialog(
    reportText: String,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
) {
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
            Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(14.dp),
            color = CncSurface,
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CncCardBorder))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Assessment, contentDescription = null, tint = CncCyberCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.logs_audit_modal_title),
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            color = CncTextPrimary
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = stringResource(R.string.logs_close), tint = CncTextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CncBackground)
                        .border(1.dp, CncCardBorder, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = reportText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = CncTextPrimary,
                        lineHeight = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onCopy,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CncTextPrimary),
                        modifier = Modifier.testTag("audit_copy_button")
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.logs_copy_report), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = onShare,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CncCyberCyan, contentColor = Color(0xFF00363D)),
                        modifier = Modifier.testTag("audit_share_button")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.logs_share_report), fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}
}
