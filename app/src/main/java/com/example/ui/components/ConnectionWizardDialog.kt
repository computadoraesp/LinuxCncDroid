package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.model.ConnectionStepId
import com.example.model.ConnectionStepInfo
import com.example.model.InterfaceType
import com.example.model.LinuxCncConnectionConfig
import com.example.model.LinuxCncServerTelemetry
import com.example.ui.theme.CncActiveGreen
import com.example.ui.theme.CncCardBg
import com.example.ui.theme.CncCardBorder
import com.example.ui.theme.CncCyberCyan
import com.example.ui.theme.CncEstopRed
import com.example.ui.theme.CncRunningGreen
import com.example.ui.theme.CncSurface
import com.example.ui.theme.CncSurfaceVariant
import com.example.ui.theme.CncTextMuted
import com.example.ui.theme.CncTextPrimary
import com.example.ui.theme.CncTextSecondary
import com.example.ui.theme.CncWarningAmber

@Composable
fun ConnectionWizardDialog(
    serverTelemetry: LinuxCncServerTelemetry,
    connectionConfig: LinuxCncConnectionConfig,
    onDismiss: () -> Unit,
    onSkipTutorial: (dontShowAgain: Boolean) -> Unit,
    onConnectDirect: (LinuxCncConnectionConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    var currentStepIndex by remember { mutableIntStateOf(0) }
    var dontShowAgainChecked by remember { mutableStateOf(false) }
    var detectedInterface by remember { mutableStateOf(InterfaceType.WIFI_LAN) }
    var copiedSnippetMessage by remember { mutableStateOf<String?>(null) }

    val steps = remember {
        listOf(
            ConnectionStepInfo(
                stepId = ConnectionStepId.INTERFACE_DETECTION,
                title = "PASO 1: DETECCIÓN DE ENLACE FÍSICO",
                subtitle = "Verificación de la interfaz de red entre Android y el CNC",
                instructions = listOf(
                    "El dispositivo Android debe estar en la misma subred que la máquina LinuxCNC.",
                    "Wi-Fi LAN: Conecta el móvil/tablet a la misma red inalámbrica del taller que el PC LinuxCNC.",
                    "Ethernet / Adaptador USB: Si usas adaptador USB-C a RJ45, comprueba que Android muestre el icono de Ethernet.",
                    "Hotspot / USB Tethering: Puedes compartir red desde Android al PC si no hay router en el taller."
                ),
                codeSnippet = "# En la terminal de LinuxCNC (PC/Raspberry Pi) comprueba tu IP:\nhostname -I\n# Ejemplo de IP obtenida: 192.168.1.120",
                isCrucial = true
            ),
            ConnectionStepInfo(
                stepId = ConnectionStepId.LINUX_INI_CONFIG,
                title = "PASO 2: HABILITAR linuxcncrsh EN machine.ini",
                subtitle = "Configurar LinuxCNC para que escuche comandos remotos por red",
                instructions = listOf(
                    "Por seguridad industrial, LinuxCNC viene por defecto sin aceptar conexiones de red.",
                    "Abre el archivo .ini de tu máquina (ejemplo: ~/linuxcnc/configs/my_machine/my_machine.ini).",
                    "Añade la sección [APPLICATIONS] con el parámetro APP = linuxcncrsh.",
                    "El flag '-w' es obligatorio para permitir comandos de movimiento (jog, mdi, cycle start).",
                    "El flag '-p 5007' define el puerto TCP estándar."
                ),
                codeSnippet = "[APPLICATIONS]\n# Inicia el demonio de control remoto al arrancar LinuxCNC:\nAPP = linuxcncrsh -- -w -p 5007 -d",
                isCrucial = true
            ),
            ConnectionStepInfo(
                stepId = ConnectionStepId.NETWORK_PING_TEST,
                title = "PASO 3: REGLAS DE FIREWALL Y PING",
                subtitle = "Permitir el puerto 5007 a través del cortafuegos de Linux",
                instructions = listOf(
                    "Si Debian / Ubuntu tiene activado el firewall (ufw), rechazará los paquetes entrantes.",
                    "Abre una consola en Linux y permite el puerto TCP 5007.",
                    "Puedes verificar la apertura del puerto con 'ss -tulpn | grep 5007'."
                ),
                codeSnippet = "# En la terminal de Linux ejecuta:\nsudo ufw allow 5007/tcp\n# Comprobar que linuxcncrsh está escuchando:\nss -tulpn | grep 5007",
                isCrucial = true
            ),
            ConnectionStepInfo(
                stepId = ConnectionStepId.AUTHENTICATION_HANDSHAKE,
                title = "PASO 4: HANDSHAKE Y AUTENTICACIÓN RSH",
                subtitle = "Negociación del protocolo nativo y clave de acceso",
                instructions = listOf(
                    "Al pulsar 'Conectar', la app envía: 'hello EMC LinuxCncDroid 1.0'.",
                    "LinuxCNC responde con 'HELLO ACK EMC 1.0'.",
                    "Luego la app envía 'set enable pwd' (o tu contraseña personalizada).",
                    "Una vez autenticada, la app pasa a monitorear posición (X, Y, Z), E-Stop y cabezal en tiempo real."
                ),
                codeSnippet = "Host IP: ${connectionConfig.hostIp}\nPuerto: ${connectionConfig.port}\nContraseña: ${connectionConfig.password}",
                isCrucial = true
            ),
            ConnectionStepInfo(
                stepId = ConnectionStepId.SECURITY_ESTOP_CHECK,
                title = "PASO 5: SEGURIDAD Y PRIMER MOVIMIENTO",
                subtitle = "Protocolo de verificación operacional antes de mecanizar",
                instructions = listOf(
                    "¡ATENCIÓN! Mantén siempre accesible la seta física de E-STOP de la máquina.",
                    "1. Pulsa RESET E-STOP en la barra superior.",
                    "2. Pulsa ENCENDER MÁQUINA (icono verde).",
                    "3. Haz Home / Referenciado de ejes si la cinemática lo requiere.",
                    "4. Prueba un movimiento fino en modo PASO (Step Jog 0.1 mm) en el Eje Z alejándote de la mesa."
                ),
                codeSnippet = null,
                isCrucial = true
            )
        )
    }

    val currentStep = steps[currentStepIndex]
    val isLastStep = currentStepIndex == steps.size - 1

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
            modifier = modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(16.dp))
                .border(2.dp, CncCyberCyan, RoundedCornerShape(16.dp)),
            color = CncSurface,
            tonalElevation = 10.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = CncCyberCyan.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, CncCyberCyan),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Router,
                                    contentDescription = null,
                                    tint = CncCyberCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = stringResource(R.string.wizard_title),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = CncCyberCyan,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = stringResource(R.string.wizard_step_indicator, currentStepIndex + 1, steps.size, currentStep.title),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CncTextSecondary
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Quick Skip button
                        OutlinedButton(
                            onClick = { onSkipTutorial(dontShowAgainChecked) },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = CncTextSecondary,
                                containerColor = CncSurfaceVariant
                            ),
                            border = BorderStroke(1.dp, CncCardBorder),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.wizard_btn_skip),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(CncSurfaceVariant)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = CncTextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Progress Indicator
                LinearProgressIndicator(
                    progress = { (currentStepIndex + 1).toFloat() / steps.size.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = CncCyberCyan,
                    trackColor = CncSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Hardware Detection Banner (Interactive)
                Surface(
                    color = CncCardBg,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, CncCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "INTERFAZ DE RED SELECCIONADA EN ANDROID",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = CncTextSecondary
                            )

                            // Status Live badge
                            Surface(
                                color = if (serverTelemetry.isConnected) CncRunningGreen.copy(alpha = 0.2f) else CncEstopRed.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(1.dp, if (serverTelemetry.isConnected) CncRunningGreen else CncEstopRed)
                            ) {
                                Text(
                                    text = if (serverTelemetry.isConnected) "EN LÍNEA (${serverTelemetry.latencyMs} ms)" else "SIN CONEXIÓN",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (serverTelemetry.isConnected) CncRunningGreen else CncEstopRed,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            InterfacePill(
                                title = "Wi-Fi LAN",
                                icon = Icons.Default.Wifi,
                                isSelected = detectedInterface == InterfaceType.WIFI_LAN,
                                onClick = { detectedInterface = InterfaceType.WIFI_LAN },
                                modifier = Modifier.weight(1f)
                            )
                            InterfacePill(
                                title = "Ethernet RJ45",
                                icon = Icons.Default.Lan,
                                isSelected = detectedInterface == InterfaceType.ETHERNET_DIRECT,
                                onClick = { detectedInterface = InterfaceType.ETHERNET_DIRECT },
                                modifier = Modifier.weight(1f)
                            )
                            InterfacePill(
                                title = "USB / Hotspot",
                                icon = Icons.Default.Usb,
                                isSelected = detectedInterface == InterfaceType.USB_HOTSPOT,
                                onClick = { detectedInterface = InterfaceType.USB_HOTSPOT },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Step Content (Scrollable)
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Surface(
                            color = CncCardBg,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, CncCyberCyan.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = currentStep.subtitle,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CncCyberCyan
                                )

                                currentStep.instructions.forEachIndexed { idx, instruction ->
                                    Row(
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 4.dp)
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(CncCyberCyan)
                                        )
                                        Text(
                                            text = instruction,
                                            fontSize = 11.sp,
                                            lineHeight = 16.sp,
                                            color = CncTextPrimary
                                        )
                                    }
                                }

                                if (currentStep.codeSnippet != null) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Surface(
                                        color = Color(0xFF0F172A),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(0xFF334155)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Terminal,
                                                        contentDescription = null,
                                                        tint = CncCyberCyan,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Text(
                                                        text = "COMANDO / CONFIGURACIÓN LINUX",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = CncCyberCyan
                                                    )
                                                }

                                                IconButton(
                                                    onClick = {
                                                        currentStep.codeSnippet?.let {
                                                            clipboardManager.setText(AnnotatedString(it))
                                                            copiedSnippetMessage = "¡Copiado al portapapeles!"
                                                        }
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.ContentCopy,
                                                        contentDescription = "Copiar",
                                                        tint = CncCyberCyan,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))

                                            Text(
                                                text = currentStep.codeSnippet,
                                                fontSize = 10.5.sp,
                                                lineHeight = 15.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = Color(0xFFE2E8F0)
                                            )

                                            if (copiedSnippetMessage != null) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = copiedSnippetMessage ?: "",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = CncActiveGreen
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Bottom Navigation & Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Checkbox "No volver a mostrar"
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { dontShowAgainChecked = !dontShowAgainChecked }
                    ) {
                        Checkbox(
                            checked = dontShowAgainChecked,
                            onCheckedChange = { dontShowAgainChecked = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = CncCyberCyan,
                                uncheckedColor = CncTextMuted,
                                checkmarkColor = Color.Black
                            ),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.wizard_dont_show_again),
                            fontSize = 9.5.sp,
                            color = CncTextSecondary
                        )
                    }

                    // Navigation Buttons (Prev / Next or Connect)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentStepIndex > 0) {
                            OutlinedButton(
                                onClick = {
                                    copiedSnippetMessage = null
                                    currentStepIndex--
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = CncTextPrimary,
                                    containerColor = CncSurfaceVariant
                                ),
                                border = BorderStroke(1.dp, CncCardBorder),
                                modifier = Modifier.height(42.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.wizard_btn_prev),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (!isLastStep) {
                            Button(
                                onClick = {
                                    copiedSnippetMessage = null
                                    currentStepIndex++
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CncCyberCyan,
                                    contentColor = Color(0xFF00363D)
                                ),
                                modifier = Modifier.height(42.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.wizard_btn_next),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else {
                            Button(
                                onClick = {
                                    if (dontShowAgainChecked) {
                                        onSkipTutorial(true)
                                    }
                                    onConnectDirect(connectionConfig)
                                    onDismiss()
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CncRunningGreen,
                                    contentColor = Color.Black
                                ),
                                modifier = Modifier.height(42.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.wizard_btn_finish),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
private fun InterfacePill(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) CncCyberCyan.copy(alpha = 0.2f) else CncSurfaceVariant,
        border = BorderStroke(1.dp, if (isSelected) CncCyberCyan else CncCardBorder),
        modifier = modifier.height(36.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) CncCyberCyan else CncTextSecondary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title,
                fontSize = 9.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) CncCyberCyan else CncTextSecondary,
                maxLines = 1
            )
        }
    }
}
