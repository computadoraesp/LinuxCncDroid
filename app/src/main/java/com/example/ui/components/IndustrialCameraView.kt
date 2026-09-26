package com.example.ui.components

import android.Manifest
import androidx.annotation.StringRes
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CropRotate
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.model.AxisCoord
import com.example.model.MachineStateEnum
import com.example.model.UnitSystem
import com.example.ui.theme.CncActiveGreen
import com.example.ui.theme.CncBackground
import com.example.ui.theme.CncCardBorder
import com.example.ui.theme.CncCyberCyan
import com.example.ui.theme.CncDroDigits
import com.example.ui.theme.CncSurface
import com.example.ui.theme.CncSurfaceVariant
import com.example.ui.theme.CncTextMuted
import com.example.ui.theme.CncTextPrimary
import com.example.ui.theme.CncTextSecondary
import com.example.ui.theme.CncWarningAmber
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

enum class ReticleType(@get:StringRes val labelRes: Int) {
    CROSSHAIR(R.string.reticle_crosshair),
    CONCENTRIC_CIRCLES(R.string.reticle_concentric),
    METROLOGY_GRID(R.string.reticle_grid),
    CORNER_FINDER(R.string.reticle_corner),
    ANGULAR_PROTRACTOR(R.string.reticle_protractor),
    BULLS_EYE(R.string.reticle_bullseye),
    NONE(R.string.reticle_none)
}

enum class CameraFilterMode(@get:StringRes val labelRes: Int) {
    NORMAL(R.string.camera_filter_normal),
    HIGH_CONTRAST(R.string.camera_filter_contrast),
    INVERTED(R.string.camera_filter_invert),
    EDGE_HIGHLIGHT(R.string.camera_filter_edge)
}

@Composable
fun IndustrialCameraView(
    modifier: Modifier = Modifier,
    machineState: MachineStateEnum,
    axes: Map<String, AxisCoord>,
    currentWcs: String,
    unitSystem: UnitSystem = UnitSystem.METRIC,
    onJogAxis: (String, Double) -> Unit,
    onZeroAxis: (String) -> Unit,
    onZeroWithOffset: ((Double, Double) -> Unit)? = null,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val attributionContext = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.createAttributionContext("camera")
        } else {
            context
        }
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    // Camera Controls State
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var isFlashOn by remember { mutableStateOf(value = false) }
    var zoomRatio by remember { mutableFloatStateOf(1.0f) }
    var maxZoomRatio by remember { mutableFloatStateOf(5.0f) }
    var minZoomRatio by remember { mutableFloatStateOf(1.0f) }

    // Reticle & Overlay Configuration
    var selectedReticle by remember { mutableStateOf(ReticleType.CROSSHAIR) }
    var reticleColor by remember { mutableStateOf(CncCyberCyan) }
    var reticleScaleMm by remember { mutableFloatStateOf(10f) } // mm, equivalent scale
    var reticleOffsetX by remember { mutableFloatStateOf(0f) }
    var reticleOffsetY by remember { mutableFloatStateOf(0f) }
    var showTelemetryOverlay by remember { mutableStateOf(value = true) }
    var activeFilterMode by remember { mutableStateOf(CameraFilterMode.NORMAL) }
    var protractorAngleDeg by remember { mutableFloatStateOf(0f) }
    var cameraSpindleOffsetX by remember { mutableDoubleStateOf(-50.0) }
    var cameraSpindleOffsetY by remember { mutableDoubleStateOf(0.0) }
    var showSpindleOffsetDialog by remember { mutableStateOf(value = false) }
    val reticleListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var lastCapturedSnapshotMessage by remember { mutableStateOf<String?>(null) }

    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = CncSurface),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CncCardBorder),
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = null,
                        tint = CncCyberCyan,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = stringResource(R.string.camera_header),
                        color = CncTextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                // Macro Zoom Presets and Quick Controls
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Macro Zoom Presets
                    listOf(1.0f, 2.0f, 4.0f, 8.0f).forEach { presetZoom ->
                        val isCurrent = abs(zoomRatio - presetZoom) < 0.25f
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isCurrent) CncCyberCyan else CncSurfaceVariant)
                                .border(1.dp, if (isCurrent) CncCyberCyan else CncCardBorder, RoundedCornerShape(4.dp))
                                .clickable {
                                    zoomRatio = presetZoom
                                    cameraControl?.setZoomRatio(presetZoom)
                                }
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "${presetZoom.toInt()}X",
                                color = if (isCurrent) Color.Black else CncTextSecondary,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(2.dp))

                    // Camera Spindle Offset Dialog Button
                    IconButton(
                        onClick = { showSpindleOffsetDialog = true },
                        modifier = Modifier.size(30.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = stringResource(R.string.camera_spindle_offset),
                            tint = if (cameraSpindleOffsetX != 0.0 || cameraSpindleOffsetY != 0.0) CncWarningAmber else CncTextSecondary,
                            modifier = Modifier.size(16.dp),
                        )
                    }

                    // Flash / Torch toggle
                    IconButton(
                        onClick = {
                            isFlashOn = !isFlashOn
                            cameraControl?.enableTorch(isFlashOn)
                        },
                        modifier = Modifier.size(30.dp),
                    ) {
                        Icon(
                            imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Flash",
                            tint = if (isFlashOn) CncWarningAmber else CncTextSecondary,
                            modifier = Modifier.size(16.dp),
                        )
                    }

                    // Flip camera
                    IconButton(
                        onClick = {
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                CameraSelector.LENS_FACING_FRONT
                            } else {
                                CameraSelector.LENS_FACING_BACK
                            }
                        },
                        modifier = Modifier.size(30.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlipCameraAndroid,
                            contentDescription = "Flip Camera",
                            tint = CncTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Telemetry toggle
                    IconButton(
                        onClick = { showTelemetryOverlay = !showTelemetryOverlay },
                        modifier = Modifier.size(30.dp),
                    ) {
                        Icon(
                            imageVector = if (showTelemetryOverlay) Icons.Default.Layers else Icons.Default.LayersClear,
                            contentDescription = "Telemetry",
                            tint = if (showTelemetryOverlay) CncCyberCyan else CncTextSecondary,
                            modifier = Modifier.size(16.dp),
                        )
                    }

                    // Snapshot capture
                    IconButton(
                        onClick = {
                            val cap = imageCapture
                            if (cap != null) {
                                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                                val photoFile = File(attributionContext.cacheDir, "CNC_ALIGN_$timeStamp.jpg")
                                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                                cap.takePicture(
                                    outputOptions,
                                    cameraExecutor,
                                    object : ImageCapture.OnImageSavedCallback {
                                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                            lastCapturedSnapshotMessage = "Captura guardada: CNC_ALIGN_$timeStamp.jpg"
                                        }

                                        override fun onError(exc: ImageCaptureException) {
                                            Log.e("CncCamera", "Snapshot error: ${exc.message}", exc)
                                            lastCapturedSnapshotMessage = "Error en captura: ${exc.message}"
                                        }
                                    }
                                )
                            }
                        },
                        enabled = machineState != MachineStateEnum.RUNNING,
                        modifier = Modifier.size(30.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Take Snapshot",
                            tint = if (machineState != MachineStateEnum.RUNNING) CncActiveGreen else CncTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (!hasCameraPermission) {
                // Request Permission State
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(CncBackground)
                        .border(1.dp, CncCardBorder, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VideocamOff,
                            contentDescription = null,
                            tint = CncWarningAmber,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = stringResource(R.string.camera_permission_required),
                            color = CncTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = stringResource(R.string.camera_permission_text),
                            color = CncTextSecondary,
                            fontSize = 11.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            colors = ButtonDefaults.buttonColors(containerColor = CncCyberCyan, contentColor = CncBackground),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.camera_grant_permission_btn), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            } else {
                // Main Camera Feed Box with Overlays
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black)
                        .border(1.dp, CncCyberCyan.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val newZoom = (zoomRatio * zoom).coerceIn(minZoomRatio, maxZoomRatio)
                                zoomRatio = newZoom
                                cameraControl?.setZoomRatio(newZoom)

                                reticleOffsetX += pan.x * 0.5f
                                reticleOffsetY += pan.y * 0.5f
                            }
                        }
                ) {
                    // Android CameraX Preview View
                    AndroidView(
                        factory = { _ ->
                            val previewView = PreviewView(attributionContext).apply {
                                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                                scaleType = PreviewView.ScaleType.FILL_CENTER
                            }

                            val cameraProviderFuture = ProcessCameraProvider.getInstance(attributionContext)
                            cameraProviderFuture.addListener(
                                {
                                    val cameraProvider = cameraProviderFuture.get()

                                val preview = Preview.Builder().build().also {
                                    it.surfaceProvider = previewView.surfaceProvider
                                }

                                val capture = ImageCapture.Builder()
                                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                    .build()
                                imageCapture = capture

                                val selector = CameraSelector.Builder()
                                    .requireLensFacing(lensFacing)
                                    .build()

                                try {
                                    cameraProvider.unbindAll()
                                    val cam = cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        selector,
                                        preview,
                                        capture,
                                    )
                                    cameraControl = cam.cameraControl
                                    cam.cameraInfo.zoomState.observe(lifecycleOwner) { zState ->
                                        if (zState != null) {
                                            minZoomRatio = zState.minZoomRatio
                                            maxZoomRatio = zState.maxZoomRatio.coerceAtMost(8f)
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("CncCamera", "Failed to bind camera: ${e.message}", e)
                                }
                            },
                                ContextCompat.getMainExecutor(attributionContext),
                            )

                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Reticle Canvas Overlay
                    Canvas(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val centerX = (size.width / 2f) + reticleOffsetX
                        val centerY = (size.height / 2f) + reticleOffsetY

                        when (selectedReticle) {
                            ReticleType.CROSSHAIR -> {
                                // Full Axis Crosshair
                                drawLine(
                                    color = reticleColor,
                                    start = Offset(0f, centerY),
                                    end = Offset(size.width, centerY),
                                    strokeWidth = 1.5f
                                )
                                drawLine(
                                    color = reticleColor,
                                    start = Offset(centerX, 0f),
                                    end = Offset(centerX, size.height),
                                    strokeWidth = 1.5f
                                )

                                // Micro-tick graduation marks
                                val tickSpacingPx = 40f
                                for (i in -10..10) {
                                    if (i != 0) {
                                        val tickH = if ((i % 5) == 0) 18f else 8f
                                        // X ticks
                                        drawLine(
                                            color = reticleColor.copy(alpha = 0.8f),
                                            start = Offset((centerX + (i * tickSpacingPx)), (centerY - (tickH / 2))),
                                            end = Offset((centerX + (i * tickSpacingPx)), (centerY + (tickH / 2))),
                                            strokeWidth = 1f
                                        )
                                        // Y ticks
                                        drawLine(
                                            color = reticleColor.copy(alpha = 0.8f),
                                            start = Offset((centerX - (tickH / 2)), (centerY + (i * tickSpacingPx))),
                                            end = Offset((centerX + (tickH / 2)), (centerY + (i * tickSpacingPx))),
                                            strokeWidth = 1f
                                        )
                                    }
                                }

                                // Center target ring
                                drawCircle(
                                    color = reticleColor,
                                    radius = 16f,
                                    center = Offset(centerX, centerY),
                                    style = Stroke(width = 1.5f)
                                )
                                drawCircle(
                                    color = reticleColor,
                                    radius = 2.5f,
                                    center = Offset(centerX, centerY)
                                )
                            }

                            ReticleType.CONCENTRIC_CIRCLES -> {
                                val radii = listOf(30f, 60f, 100f, 160f, 240f)
                                radii.forEachIndexed { idx, r ->
                                    val isMajor = (idx % 2) == 1
                                    drawCircle(
                                        color = reticleColor.copy(alpha = if (isMajor) 0.9f else 0.5f),
                                        radius = r,
                                        center = Offset(centerX, centerY),
                                        style = Stroke(
                                            width = if (isMajor) 1.5f else 1.0f,
                                            pathEffect = if (!isMajor) PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f) else null
                                        )
                                    )
                                }
                                drawLine(
                                    color = reticleColor.copy(alpha = 0.7f),
                                    start = Offset(centerX - 120f, centerY),
                                    end = Offset(centerX + 120f, centerY),
                                    strokeWidth = 1f
                                )
                                drawLine(
                                    color = reticleColor.copy(alpha = 0.7f),
                                    start = Offset(centerX, centerY - 120f),
                                    end = Offset(centerX, centerY + 120f),
                                    strokeWidth = 1f
                                )
                            }

                            ReticleType.METROLOGY_GRID -> {
                                val gridSize = 50f
                                var gx = (centerX % gridSize)
                                while (gx < size.width) {
                                    drawLine(
                                        color = reticleColor.copy(alpha = 0.25f),
                                        start = Offset(gx, 0f),
                                        end = Offset(gx, size.height),
                                        strokeWidth = 0.75f
                                    )
                                    gx += gridSize
                                }
                                var gy = (centerY % gridSize)
                                while (gy < size.height) {
                                    drawLine(
                                        color = reticleColor.copy(alpha = 0.25f),
                                        start = Offset(0f, gy),
                                        end = Offset(size.width, gy),
                                        strokeWidth = 0.75f
                                    )
                                    gy += gridSize
                                }

                                // Main axes highlight
                                drawLine(
                                    color = reticleColor,
                                    start = Offset(centerX, 0f),
                                    end = Offset(centerX, size.height),
                                    strokeWidth = 1.5f
                                )
                                drawLine(
                                    color = reticleColor,
                                    start = Offset(0f, centerY),
                                    end = Offset(size.width, centerY),
                                    strokeWidth = 1.5f
                                )
                            }

                            ReticleType.CORNER_FINDER -> {
                                // 90-degree corner alignment reticle for part square edge
                                val armLen = 140f
                                drawLine(
                                    color = reticleColor,
                                    start = Offset(centerX, centerY),
                                    end = Offset(centerX + armLen, centerY),
                                    strokeWidth = 2f
                                )
                                drawLine(
                                    color = reticleColor,
                                    start = Offset(centerX, centerY),
                                    end = Offset(centerX, centerY + armLen),
                                    strokeWidth = 2f
                                )
                                drawLine(
                                    color = CncWarningAmber,
                                    start = Offset(centerX, centerY),
                                    end = Offset(centerX - 40f, centerY - 40f),
                                    strokeWidth = 1f,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                                )
                                drawCircle(
                                    color = CncWarningAmber,
                                    radius = 5f,
                                    center = Offset(centerX, centerY)
                                )
                            }

                            ReticleType.ANGULAR_PROTRACTOR -> {
                                val radius = 180f
                                // Circular dial
                                drawCircle(
                                    color = reticleColor.copy(alpha = 0.6f),
                                    radius = radius,
                                    center = Offset(centerX, centerY),
                                    style = Stroke(width = 1.2f)
                                )
                                drawCircle(
                                    color = reticleColor.copy(alpha = 0.3f),
                                    radius = radius * 0.5f,
                                    center = Offset(centerX, centerY),
                                    style = Stroke(width = 0.8f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f))
                                )

                                // Angular graduation tick marks around the circle
                                for (deg in 0 until 360 step 5) {
                                    val rad = Math.toRadians(deg.toDouble())
                                    val isMajor = (deg % 30) == 0
                                    val isMedium = (deg % 10) == 0
                                    val tickLen = if (isMajor) 16f else if (isMedium) 10f else 5f

                                    val x1 = centerX + (radius - tickLen) * cos(rad).toFloat()
                                    val y1 = centerY + (radius - tickLen) * sin(rad).toFloat()
                                    val x2 = centerX + radius * cos(rad).toFloat()
                                    val y2 = centerY + radius * sin(rad).toFloat()

                                    drawLine(
                                        color = reticleColor.copy(alpha = if (isMajor) 0.95f else 0.5f),
                                        start = Offset(x1, y1),
                                        end = Offset(x2, y2),
                                        strokeWidth = if (isMajor) 1.5f else 0.8f
                                    )
                                }

                                // Active rotatable protractor hairline
                                val angleRad = Math.toRadians(protractorAngleDeg.toDouble())
                                val lineLen = size.width.coerceAtLeast(size.height)
                                val dirX = cos(angleRad).toFloat()
                                val dirY = sin(angleRad).toFloat()

                                drawLine(
                                    color = CncWarningAmber,
                                    start = Offset(centerX - lineLen * dirX, centerY - lineLen * dirY),
                                    end = Offset(centerX + lineLen * dirX, centerY + lineLen * dirY),
                                    strokeWidth = 2.0f
                                )

                                // Perpendicular crosshair
                                val perpX = -dirY
                                val perpY = dirX
                                drawLine(
                                    color = CncWarningAmber.copy(alpha = 0.65f),
                                    start = Offset(centerX - radius * 1.2f * perpX, centerY - radius * 1.2f * perpY),
                                    end = Offset(centerX + radius * 1.2f * perpX, centerY + radius * 1.2f * perpY),
                                    strokeWidth = 1.0f,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                                )

                                // Center alignment ring
                                drawCircle(
                                    color = CncWarningAmber,
                                    radius = 6f,
                                    center = Offset(centerX, centerY),
                                    style = Stroke(width = 1.5f)
                                )
                            }

                            ReticleType.BULLS_EYE -> {
                                val ringRadii = listOf(15f, 35f, 65f, 105f, 155f, 215f)
                                ringRadii.forEachIndexed { index, r ->
                                    val isTargetCore = index < 2
                                    drawCircle(
                                        color = if (isTargetCore) Color.Red else reticleColor.copy(alpha = (0.85f - index * 0.1f).coerceAtLeast(0.2f)),
                                        radius = r,
                                        center = Offset(centerX, centerY),
                                        style = Stroke(width = if (isTargetCore) 2.2f else 1.2f)
                                    )
                                }
                                drawCircle(
                                    color = Color.Red,
                                    radius = 4f,
                                    center = Offset(centerX, centerY)
                                )
                                val armGap = 20f
                                val armLength = 280f
                                drawLine(color = reticleColor, start = Offset(centerX + armGap, centerY), end = Offset(centerX + armLength, centerY), strokeWidth = 1.5f)
                                drawLine(color = reticleColor, start = Offset(centerX - armGap, centerY), end = Offset(centerX - armLength, centerY), strokeWidth = 1.5f)
                                drawLine(color = reticleColor, start = Offset(centerX, centerY + armGap), end = Offset(centerX, centerY + armLength), strokeWidth = 1.5f)
                                drawLine(color = reticleColor, start = Offset(centerX, centerY - armGap), end = Offset(centerX, centerY - armLength), strokeWidth = 1.5f)
                            }

                            ReticleType.NONE -> {}
                        }

                        // Optical Filter Canvas Effect Layer
                        when (activeFilterMode) {
                            CameraFilterMode.NORMAL -> {}
                            CameraFilterMode.HIGH_CONTRAST -> {
                                drawRect(
                                    color = Color.Black.copy(alpha = 0.22f),
                                    size = size
                                )
                            }
                            CameraFilterMode.INVERTED -> {
                                drawRect(
                                    color = Color(0xFF00E5FF).copy(alpha = 0.18f),
                                    size = size,
                                    blendMode = BlendMode.ColorDodge
                                )
                            }
                            CameraFilterMode.EDGE_HIGHLIGHT -> {
                                var y = 0f
                                while (y < size.height) {
                                    drawLine(
                                        color = CncCyberCyan.copy(alpha = 0.12f),
                                        start = Offset(0f, y),
                                        end = Offset(size.width, y),
                                        strokeWidth = 1f
                                    )
                                    y += 8f
                                }
                            }
                        }
                    }

                    // Live Telemetry Overlay HUD (Top-Left)
                    if (showTelemetryOverlay) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.75f))
                                .border(1.dp, CncCardBorder, RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.camera_telemetry_header),
                                color = CncCyberCyan,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.camera_telemetry_info, currentWcs, zoomRatio, reticleScaleMm.toInt()),
                                color = CncTextPrimary,
                                fontSize = 8.5.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            axes.forEach { (axis, data) ->
                                val isRotary = axis in listOf("A", "B", "C")
                                val formattedVal = if (isRotary) {
                                    "${String.format(Locale.US, "%+08.3f", data.workPos)}°"
                                } else {
                                    "${unitSystem.formatPosition(data.workPos)} ${unitSystem.lengthUnit}"
                                }
                                Text(
                                    text = "$axis: $formattedVal",
                                    color = if (axis == "Z") CncWarningAmber else CncDroDigits,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    // Quick Reticle Centering & Reset (Top-Right)
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if ((reticleOffsetX != 0f) || (reticleOffsetY != 0f)) {
                            Button(
                                onClick = {
                                    reticleOffsetX = 0f
                                    reticleOffsetY = 0f
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CncSurfaceVariant, contentColor = CncWarningAmber),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Icon(imageVector = Icons.Default.CenterFocusStrong, contentDescription = stringResource(R.string.camera_recentre_btn), modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.camera_recentre_btn), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Notification banner for captured photo
                    lastCapturedSnapshotMessage?.let { msg ->
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(12.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(CncActiveGreen.copy(alpha = 0.9f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(text = msg, color = CncBackground, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Protractor Angle Control Bar (when Angular Protractor reticle is active)
                if (selectedReticle == ReticleType.ANGULAR_PROTRACTOR) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(CncSurfaceVariant)
                            .border(1.dp, CncCardBorder, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(imageVector = Icons.Default.CropRotate, contentDescription = null, tint = CncWarningAmber, modifier = Modifier.size(14.dp))
                            Text(
                                text = stringResource(R.string.camera_angle_label, protractorAngleDeg),
                                color = CncWarningAmber,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            listOf(-10f, -1f, -0.1f, 0f, 0.1f, 1f, 10f).forEach { delta ->
                                Button(
                                    onClick = {
                                        if (delta == 0f) {
                                            protractorAngleDeg = 0f
                                        } else {
                                            protractorAngleDeg = ((protractorAngleDeg + delta) % 360f + 360f) % 360f
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CncSurface, contentColor = CncDroDigits),
                                    shape = RoundedCornerShape(3.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 1.dp),
                                    modifier = Modifier.height(22.dp)
                                ) {
                                    Text(
                                        text = if (delta == 0f) "0°" else if (delta > 0) "+$delta°" else "$delta°",
                                        fontSize = 7.5.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }

                // Optical Filters and Reticle Colors Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Optical Filter Mode Selector Chips
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.camera_optical_filters),
                            color = CncTextSecondary,
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        CameraFilterMode.entries.forEach { fMode ->
                            val isSel = activeFilterMode == fMode
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(if (isSel) CncCyberCyan else CncSurfaceVariant)
                                    .border(1.dp, if (isSel) CncCyberCyan else CncCardBorder, RoundedCornerShape(3.dp))
                                    .clickable { activeFilterMode = fMode }
                                    .padding(horizontal = 6.dp, vertical = 2.5.dp)
                            ) {
                                Text(
                                    text = stringResource(fMode.labelRes),
                                    color = if (isSel) Color.Black else CncTextSecondary,
                                    fontSize = 7.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Reticle Colors
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(CncCyberCyan, CncActiveGreen, CncWarningAmber, Color.Red, Color.White).forEach { c ->
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .border(if (reticleColor == c) 2.dp else 0.5.dp, if (reticleColor == c) Color.White else Color.DarkGray, CircleShape)
                                    .clickable { reticleColor = c }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Reticle Carousel with Navigation Buttons < >
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CarouselNavButton(
                        direction = "<",
                        height = 26.dp,
                        onClick = {
                            coroutineScope.launch {
                                reticleListState.animateScrollBy(-220f)
                            }
                        }
                    )

                    LazyRow(
                        state = reticleListState,
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(ReticleType.entries.toTypedArray()) { rType ->
                            val isSel = selectedReticle == rType
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isSel) CncCyberCyan else CncSurfaceVariant)
                                    .border(1.dp, if (isSel) CncCyberCyan else CncCardBorder, RoundedCornerShape(4.dp))
                                    .clickable { selectedReticle = rType }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = stringResource(rType.labelRes),
                                    color = if (isSel) CncBackground else CncTextSecondary,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    CarouselNavButton(
                        direction = ">",
                        height = 26.dp,
                        onClick = {
                            coroutineScope.launch {
                                reticleListState.animateScrollBy(220f)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Micro-Jogging for Optical Part Edge Alignment and Spindle Offset Zeroing
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(CncSurfaceVariant)
                        .border(1.dp, CncCardBorder, RoundedCornerShape(6.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val microStepMm = if (unitSystem == UnitSystem.IMPERIAL) 0.002 * 25.4 else 0.05
                    val microLabelNeg = if (unitSystem == UnitSystem.IMPERIAL) "-0.002\"" else "-0.05"
                    val microLabelPos = if (unitSystem == UnitSystem.IMPERIAL) "+0.002\"" else "+0.05"

                    val isEnabled = (machineState != MachineStateEnum.RUNNING) &&
                            (machineState != MachineStateEnum.ESTOP) &&
                            (machineState != MachineStateEnum.ERROR)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(stringResource(R.string.camera_micro_alignment_label), color = CncTextSecondary, fontSize = 8.sp, fontWeight = FontWeight.Bold)

                        // X Axis Micro Steps
                        Button(
                            onClick = { onJogAxis("X", -microStepMm) },
                            enabled = isEnabled,
                            colors = ButtonDefaults.buttonColors(containerColor = CncSurface, contentColor = CncDroDigits),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 5.dp, vertical = 2.dp),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Text("X $microLabelNeg", fontSize = 7.5.sp, fontFamily = FontFamily.Monospace)
                        }

                        Button(
                            onClick = { onJogAxis("X", microStepMm) },
                            enabled = isEnabled,
                            colors = ButtonDefaults.buttonColors(containerColor = CncSurface, contentColor = CncDroDigits),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 5.dp, vertical = 2.dp),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Text("X $microLabelPos", fontSize = 7.5.sp, fontFamily = FontFamily.Monospace)
                        }

                        // Y Axis Micro Steps
                        Button(
                            onClick = { onJogAxis("Y", -microStepMm) },
                            enabled = isEnabled,
                            colors = ButtonDefaults.buttonColors(containerColor = CncSurface, contentColor = CncDroDigits),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 5.dp, vertical = 2.dp),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Text("Y $microLabelNeg", fontSize = 7.5.sp, fontFamily = FontFamily.Monospace)
                        }

                        Button(
                            onClick = { onJogAxis("Y", microStepMm) },
                            enabled = isEnabled,
                            colors = ButtonDefaults.buttonColors(containerColor = CncSurface, contentColor = CncDroDigits),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 5.dp, vertical = 2.dp),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Text("Y $microLabelPos", fontSize = 7.5.sp, fontFamily = FontFamily.Monospace)
                        }
                    }

                    // Zero X/Y and Spindle Offset Zero
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = { onZeroAxis("X") },
                            enabled = isEnabled,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CncCyberCyan),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CncCyberCyan),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Text(stringResource(R.string.camera_zero_x), fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { onZeroAxis("Y") },
                            enabled = isEnabled,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CncCyberCyan),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CncCyberCyan),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Text(stringResource(R.string.camera_zero_y), fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
                        }

                        if (onZeroWithOffset != null) {
                            Button(
                                onClick = {
                                    onZeroWithOffset(cameraSpindleOffsetX, cameraSpindleOffsetY)
                                    lastCapturedSnapshotMessage = String.format(Locale.US, "Cero G54 fijado con offset: X=%.2f, Y=%.2f", cameraSpindleOffsetX, cameraSpindleOffsetY)
                                },
                                enabled = isEnabled,
                                colors = ButtonDefaults.buttonColors(containerColor = CncWarningAmber, contentColor = Color.Black),
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier.height(24.dp)
                            ) {
                                Icon(imageVector = Icons.Default.GpsFixed, contentDescription = null, modifier = Modifier.size(11.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(stringResource(R.string.camera_zero_with_offset), fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Camera to Spindle Offset Calibration Dialog
    if (showSpindleOffsetDialog) {
        var tempOffsetX by remember { mutableStateOf(cameraSpindleOffsetX.toString()) }
        var tempOffsetY by remember { mutableStateOf(cameraSpindleOffsetY.toString()) }

        AlertDialog(
            onDismissRequest = { showSpindleOffsetDialog = false },
            containerColor = CncSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = CncCyberCyan, modifier = Modifier.size(20.dp))
                    Text(stringResource(R.string.camera_spindle_offset_title), color = CncTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.camera_spindle_offset_desc),
                        color = CncTextSecondary,
                        fontSize = 9.sp
                    )
                    OutlinedTextField(
                        value = tempOffsetX,
                        onValueChange = { tempOffsetX = it },
                        label = { Text(stringResource(R.string.cam_offset_x_label), fontSize = 8.5.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CncCyberCyan,
                            unfocusedBorderColor = CncCardBorder,
                            focusedTextColor = CncDroDigits,
                            unfocusedTextColor = CncDroDigits
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tempOffsetY,
                        onValueChange = { tempOffsetY = it },
                        label = { Text(stringResource(R.string.cam_offset_y_label), fontSize = 8.5.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CncCyberCyan,
                            unfocusedBorderColor = CncCardBorder,
                            focusedTextColor = CncDroDigits,
                            unfocusedTextColor = CncDroDigits
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        cameraSpindleOffsetX = tempOffsetX.toDoubleOrNull() ?: cameraSpindleOffsetX
                        cameraSpindleOffsetY = tempOffsetY.toDoubleOrNull() ?: cameraSpindleOffsetY
                        showSpindleOffsetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CncCyberCyan, contentColor = Color.Black)
                ) {
                    Text(stringResource(R.string.common_save), fontWeight = FontWeight.Bold, fontSize = 9.5.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSpindleOffsetDialog = false }) {
                    Text(stringResource(R.string.common_cancel), color = CncTextMuted, fontSize = 9.5.sp)
                }
            }
        )
    }
}
