package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CncBackground
import com.example.ui.theme.CncCardBorder
import com.example.ui.theme.CncCyberCyan
import com.example.ui.theme.CncEstopRed
import com.example.ui.theme.CncSurface
import com.example.ui.theme.CncTextPrimary
import com.example.ui.theme.CncWarningAmber

class CncErrorBoundaryState {
    var hasError by mutableStateOf(false)
    var errorDescription by mutableStateOf<String?>(null)

    fun reportFault(error: Throwable) {
        hasError = true
        errorDescription = error.localizedMessage ?: "Fallo interno en el subsistema"
    }

    fun reportFault(message: String) {
        hasError = true
        errorDescription = message
    }

    fun reset() {
        hasError = false
        errorDescription = null
    }
}

@Composable
fun rememberCncErrorBoundaryState(): CncErrorBoundaryState {
    return remember { CncErrorBoundaryState() }
}

/**
 * Industrial Grade Error Boundary for Compose views.
 * Allows graceful component-level degradation when a subsystem (camera, telemetry, 3D) encounters a fault.
 */
@Composable
fun CncErrorBoundary(
    componentName: String,
    modifier: Modifier = Modifier,
    state: CncErrorBoundaryState = rememberCncErrorBoundaryState(),
    onReset: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    if (!state.hasError) {
        Box(modifier = modifier) {
            content()
        }
    } else {
        CncComponentFallbackCard(
            componentName = componentName,
            errorMessage = state.errorDescription,
            onRetry = {
                state.reset()
                onReset?.invoke()
            },
            modifier = modifier,
        )
    }
}

@Composable
fun CncComponentFallbackCard(
    componentName: String,
    errorMessage: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
            .testTag("error_boundary_card_${componentName.lowercase().replace(" ", "_")}"),
        colors = CardDefaults.cardColors(containerColor = CncSurface),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CncEstopRed.copy(alpha = 0.6f))),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(CncEstopRed.copy(alpha = 0.15f))
                    .border(1.dp, CncEstopRed, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = "Component Fault",
                    tint = CncEstopRed,
                    modifier = Modifier.size(28.dp),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "AISLAMIENTO DE FALLO: $componentName",
                fontWeight = FontWeight.Black,
                fontSize = 13.sp,
                color = CncEstopRed,
                fontFamily = FontFamily.Monospace,
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "El subsistema se detuvo de forma preventiva para proteger la HMI y los ejes de la máquina.",
                fontSize = 11.sp,
                color = CncTextPrimary,
                lineHeight = 15.sp,
            )

            if (!errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(CncBackground)
                        .border(1.dp, CncCardBorder, RoundedCornerShape(6.dp))
                        .padding(8.dp),
                ) {
                    Text(
                        text = errorMessage,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = CncWarningAmber,
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedButton(
                onClick = onRetry,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = CncCyberCyan,
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("error_boundary_retry_button"),
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reiniciar", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("REINICIAR SUBSISTEMA", fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }
    }
}
