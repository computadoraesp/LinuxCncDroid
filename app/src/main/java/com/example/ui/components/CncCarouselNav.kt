package com.example.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CncCardBorder
import com.example.ui.theme.CncCyberCyan
import com.example.ui.theme.CncSurfaceVariant
import com.example.ui.theme.CncTextMuted
import kotlinx.coroutines.launch

/**
 * Intuitive `<` and `>` arrow button for horizontal scrollable carousels.
 * Proportioned specifically to the height of the row and matching CNC cyber theme.
 */
@Composable
fun CarouselNavButton(
    direction: String, // "<" or ">"
    enabled: Boolean = true,
    height: Dp = 32.dp,
    width: Dp = 22.dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(width = width, height = height)
            .clip(RoundedCornerShape(4.dp))
            .background(CncSurfaceVariant.copy(alpha = if (enabled) 0.9f else 0.25f))
            .border(
                width = 1.dp,
                color = if (enabled) CncCyberCyan.copy(alpha = 0.65f) else CncCardBorder.copy(alpha = 0.35f),
                shape = RoundedCornerShape(4.dp)
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = direction,
            color = if (enabled) CncCyberCyan else CncTextMuted.copy(alpha = 0.6f),
            fontSize = (height.value * 0.42f).coerceIn(10f, 15f).sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
        )
    }
}
