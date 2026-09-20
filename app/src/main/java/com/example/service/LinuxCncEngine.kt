package com.example.service

import com.example.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.*
import kotlin.time.Duration.Companion.milliseconds

class LinuxCncEngine {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Machine State
    private val _machineState = MutableStateFlow(MachineStateEnum.IDLE)
    val machineState: StateFlow<MachineStateEnum> = _machineState.asStateFlow()

    private val _taskMode = MutableStateFlow(TaskMode.MANUAL)
    val taskMode: StateFlow<TaskMode> = _taskMode.asStateFlow()

    private val _currentCoordSystem = MutableStateFlow("G54")
    val currentCoordSystem: StateFlow<String> = _currentCoordSystem.asStateFlow()

    // Work Coordinate Systems (G54 through G59.3)
    private val initialWcsOffsets = linkedMapOf(
        "G54" to WcsOffset(name = "G54", pIndex = 1, x = 100.000, y = 70.000, z = -30.000, a = 0.0, comment = "Vise 1 - Front Left Jaw"),
        "G55" to WcsOffset(name = "G55", pIndex = 2, x = 250.000, y = 70.000, z = -30.000, a = 0.0, comment = "Vise 2 - Center Plate"),
        "G56" to WcsOffset(name = "G56", pIndex = 3, x = 400.000, y = 70.000, z = -30.000, a = 0.0, comment = "Vise 3 - 4th Axis Chuck"),
        "G57" to WcsOffset(name = "G57", pIndex = 4, x = 0.000, y = 0.000, z = 0.000, a = 0.0, comment = "Fixture 4"),
        "G58" to WcsOffset(name = "G58", pIndex = 5, x = 0.000, y = 0.000, z = 0.000, a = 0.0, comment = "Fixture 5"),
        "G59" to WcsOffset(name = "G59", pIndex = 6, x = 0.000, y = 0.000, z = 0.000, a = 0.0, comment = "Fixture 6"),
        "G59.1" to WcsOffset(name = "G59.1", pIndex = 7, x = 0.000, y = 0.000, z = 0.000, a = 0.0, comment = "Auxiliary 1"),
        "G59.2" to WcsOffset(name = "G59.2", pIndex = 8, x = 0.000, y = 0.000, z = 0.000, a = 0.0, comment = "Auxiliary 2"),
        "G59.3" to WcsOffset(name = "G59.3", pIndex = 9, x = 0.000, y = 0.000, z = 0.000, a = 0.0, comment = "Toolsetter Reference")
    )

    private val _wcsOffsets = MutableStateFlow<Map<String, WcsOffset>>(initialWcsOffsets)
    val wcsOffsets: StateFlow<Map<String, WcsOffset>> = _wcsOffsets.asStateFlow()

    // Real-time HAL Pins & Signals
    private val initialHalPins = listOf(
        HalPin("hal.air-pressure-ok", HalPinCategory.SAFETY, HalPinType.BIT, booleanValue = true, description = "Main Pneumatic Pressure > 6.0 Bar (Pneumatics OK)"),
        HalPin("hal.cabinet-door-closed", HalPinCategory.SAFETY, HalPinType.BIT, booleanValue = true, description = "Enclosure Safety Interlock (Door Closed)"),
        HalPin("hal.lube-level-ok", HalPinCategory.SAFETY, HalPinType.BIT, booleanValue = true, description = "Automatic Ways Lubrication Tank Level Normal"),
        HalPin("motion.feed-inhibit", HalPinCategory.SAFETY, HalPinType.BIT, booleanValue = false, description = "Feed Inhibit Active (Holds motion if triggered)"),

        HalPin("joint.0.home-sw-in", HalPinCategory.LIMIT_SWITCHES, HalPinType.BIT, booleanValue = false, description = "X-Axis Reference Limit Switch"),
        HalPin("joint.1.home-sw-in", HalPinCategory.LIMIT_SWITCHES, HalPinType.BIT, booleanValue = false, description = "Y-Axis Reference Limit Switch"),
        HalPin("joint.2.home-sw-in", HalPinCategory.LIMIT_SWITCHES, HalPinType.BIT, booleanValue = false, description = "Z-Axis Reference Limit Switch (Top)"),
        HalPin("joint.3.home-sw-in", HalPinCategory.LIMIT_SWITCHES, HalPinType.BIT, booleanValue = false, description = "A-Axis (Rotary) Index Pulse Switch"),

        HalPin("spindle.0.at-speed", HalPinCategory.SPINDLE, HalPinType.BIT, booleanValue = true, description = "VFD Inverter Ready & Commanded RPM Reached"),
        HalPin("spindle.0.is-oriented", HalPinCategory.SPINDLE, HalPinType.BIT, booleanValue = true, description = "Spindle M19 Orientation Locked for ATC"),
        HalPin("spindle.0.brake", HalPinCategory.SPINDLE, HalPinType.BIT, booleanValue = false, description = "Pneumatic Spindle Shaft Brake Engaged"),

        HalPin("motion.adaptive-feed", HalPinCategory.MOTION, HalPinType.FLOAT, floatValue = 1.0, description = "Real-Time Adaptive Feed Override Multiplier"),
        HalPin("motion.in-position", HalPinCategory.MOTION, HalPinType.BIT, booleanValue = true, description = "Trajectory Planner In-Position Flag"),

        HalPin("iocontrol.0.tool-prep-ok", HalPinCategory.IO_EXPANSION, HalPinType.BIT, booleanValue = true, description = "Carousel Pocket Ready for Tool Exchange"),
        HalPin("iocontrol.0.tool-change-ok", HalPinCategory.IO_EXPANSION, HalPinType.BIT, booleanValue = true, description = "ATC Arm Cycle Complete & Tool Clamped")
    )

    private val _halPins = MutableStateFlow<List<HalPin>>(initialHalPins)
    val halPins: StateFlow<List<HalPin>> = _halPins.asStateFlow()

    val mdiValidator = MdiSyntaxValidator()
    val softLimitsPreChecker = SoftLimitsPreChecker()

    // Axes
    private val _axes = MutableStateFlow(
        mapOf(
            "X" to AxisCoord(name = "X", machinePos = 120.450, workPos = 20.450, dtgPos = 0.0, loadTorquePct = 14.5, motorTempC = 36.2, driveTempC = 41.0),
            "Y" to AxisCoord(name = "Y", machinePos = 85.320, workPos = 15.320, dtgPos = 0.0, loadTorquePct = 12.8, motorTempC = 35.8, driveTempC = 39.5),
            "Z" to AxisCoord(name = "Z", machinePos = -42.100, workPos = -12.100, dtgPos = 0.0, loadTorquePct = 18.2, motorTempC = 38.0, driveTempC = 42.1),
            "A" to AxisCoord(name = "A", machinePos = 0.000, workPos = 0.000, dtgPos = 0.0, loadTorquePct = 8.5, motorTempC = 32.5, driveTempC = 35.0),
        ),
    )
    val axes: StateFlow<Map<String, AxisCoord>> = _axes.asStateFlow()

    // Spindle & Feed
    private val _spindle = MutableStateFlow(SpindleInfo())
    val spindle: StateFlow<SpindleInfo> = _spindle.asStateFlow()

    private val _feed = MutableStateFlow(FeedInfo())
    val feed: StateFlow<FeedInfo> = _feed.asStateFlow()

    private val _coolant = MutableStateFlow(CoolantInfo())
    val coolant: StateFlow<CoolantInfo> = _coolant.asStateFlow()

    private val _probe = MutableStateFlow(ProbeInfo())
    val probe: StateFlow<ProbeInfo> = _probe.asStateFlow()

    private val defaultTools = listOf(
        CncToolItem(
            id = 1,
            pocket = 1,
            description = "6mm 3-Flute Carbide Endmill",
            diameter = 6.000,
            lengthOffset = 45.230,
            toolType = ToolType.ENDMILL,
            flutes = 3,
            maxRpm = 24000.0,
            lifeMinutesCurrent = 42.0,
            lifeMinutesMax = 180.0,
            isActive = true,
        ),
        CncToolItem(
            id = 2,
            pocket = 2,
            description = "3mm 2-Flute Ball Nose 3D",
            diameter = 3.000,
            lengthOffset = 38.110,
            toolType = ToolType.BALLNOSE,
            flutes = 2,
            maxRpm = 24000.0,
            lifeMinutesCurrent = 18.5,
            lifeMinutesMax = 120.0,
            isActive = false,
        ),
        CncToolItem(
            id = 3,
            pocket = 3,
            description = "50mm 4-Insert Face Mill",
            diameter = 50.000,
            lengthOffset = 62.450,
            toolType = ToolType.FACE_MILL,
            flutes = 4,
            maxRpm = 10000.0,
            lifeMinutesCurrent = 65.0,
            lifeMinutesMax = 240.0,
            isActive = false,
        ),
        CncToolItem(
            id = 4,
            pocket = 4,
            description = "4.2mm HSS Twist Drill (M5 Prep)",
            diameter = 4.200,
            lengthOffset = 52.800,
            toolType = ToolType.DRILL,
            flutes = 2,
            maxRpm = 6000.0,
            lifeMinutesCurrent = 12.0,
            lifeMinutesMax = 90.0,
            isActive = false,
        ),
        CncToolItem(
            id = 5,
            pocket = 5,
            description = "M5x0.8 Spiral Point Machine Tap",
            diameter = 5.000,
            lengthOffset = 48.300,
            toolType = ToolType.TAP,
            flutes = 3,
            maxRpm = 1200.0,
            lifeMinutesCurrent = 8.0,
            lifeMinutesMax = 60.0,
            isActive = false,
        ),
        CncToolItem(
            id = 6,
            pocket = 6,
            description = "10mm 45° Chamfer Deburr Mill",
            diameter = 10.000,
            lengthOffset = 41.500,
            toolType = ToolType.CHAMFER,
            flutes = 4,
            maxRpm = 18000.0,
            lifeMinutesCurrent = 29.0,
            lifeMinutesMax = 150.0,
            isActive = false,
        ),
        CncToolItem(
            id = 7,
            pocket = 7,
            description = "Renishaw 3D Touch Probe",
            diameter = 4.000,
            lengthOffset = 75.000,
            toolType = ToolType.TOUCH_PROBE,
            flutes = 1,
            maxRpm = 0.0,
            lifeMinutesCurrent = 150.0,
            lifeMinutesMax = 9999.0,
            isActive = false,
        ),
    )

    private val _toolTable = MutableStateFlow(defaultTools)
    val toolTable: StateFlow<List<CncToolItem>> = _toolTable.asStateFlow()

    private val _activeTool = MutableStateFlow(defaultTools.first())
    val activeTool: StateFlow<CncToolItem> = _activeTool.asStateFlow()

    private val _tool = MutableStateFlow(
        ToolInfo(
            toolNumber = 1,
            description = "6mm 3-Flute Carbide Endmill",
            lengthOffset = 45.230,
            diameterOffset = 6.000,
            atcSlot = 1
        )
    )
    val tool: StateFlow<ToolInfo> = _tool.asStateFlow()

    // Program execution timing
    private val _cycleElapsedSeconds = MutableStateFlow(0L)
    val cycleElapsedSeconds: StateFlow<Long> = _cycleElapsedSeconds.asStateFlow()

    private val _cycleEstimatedTotalSeconds = MutableStateFlow(185L)
    val cycleEstimatedTotalSeconds: StateFlow<Long> = _cycleEstimatedTotalSeconds.asStateFlow()

    // EtherCAT & Hardware Diagnostics
    private val _etherCatMaster = MutableStateFlow(EtherCatMasterInfo())
    val etherCatMaster: StateFlow<EtherCatMasterInfo> = _etherCatMaster.asStateFlow()

    private val _etherCatSlaves = MutableStateFlow(
        listOf(
            EtherCatSlaveInfo(0, "Delta ASDA-B3-E Axis X", actualTorquePct = 14.5, driveTempC = 41.0),
            EtherCatSlaveInfo(1, "Delta ASDA-B3-E Axis Y", actualTorquePct = 12.8, driveTempC = 39.5),
            EtherCatSlaveInfo(2, "Delta ASDA-B3-E Axis Z (Brake)", actualTorquePct = 18.2, driveTempC = 42.1),
            EtherCatSlaveInfo(3, "Delta ASDA-B3-E Axis A", actualTorquePct = 8.5, driveTempC = 35.0)
        )
    )
    val etherCatSlaves: StateFlow<List<EtherCatSlaveInfo>> = _etherCatSlaves.asStateFlow()

    // Capabilities
    private val _capabilities = MutableStateFlow(CapabilitiesManifest())
    val capabilities: StateFlow<CapabilitiesManifest> = _capabilities.asStateFlow()

    // GCode & Program execution
    private val _activeGCodeLine = MutableStateFlow(0)
    val activeGCodeLine: StateFlow<Int> = _activeGCodeLine.asStateFlow()

    // PRO MODULE 5: Active Modal G-Code State & Execution Modifiers
    private val _modalState = MutableStateFlow(ModalGCodeState())
    val modalState: StateFlow<ModalGCodeState> = _modalState.asStateFlow()

    private val _executionModifiers = MutableStateFlow(ExecutionModifiers())
    val executionModifiers: StateFlow<ExecutionModifiers> = _executionModifiers.asStateFlow()

    private val _loadedGCode = MutableStateFlow<List<GCodeSegment>>(emptyList())
    val loadedGCode: StateFlow<List<GCodeSegment>> = _loadedGCode.asStateFlow()

    private val _loadedFileName = MutableStateFlow("pocket_demo.ngc")
    val loadedFileName: StateFlow<String> = _loadedFileName.asStateFlow()

    private val _eventLogs = MutableStateFlow(
        listOf(
            CncEventLog(severity = LogSeverity.INFO, tag = "KERNEL", message = "LinuxCNC Motion Kernel initialized in real-time mode"),
            CncEventLog(severity = LogSeverity.INFO, tag = "ETHERCAT", message = "EtherCAT Master [OP]: 4 slaves operational, DC synch 1000us"),
            CncEventLog(severity = LogSeverity.SECURITY, tag = "SECURITY", message = "Cybersecurity monitor active: Trojan & macro inspection enabled"),
            CncEventLog(severity = LogSeverity.INFO, tag = "SYSTEM", message = "Machine in IDLE state. Ready for operation")
        )
    )
    val eventLogs: StateFlow<List<CncEventLog>> = _eventLogs.asStateFlow()

    private val _isSimulatedMode = MutableStateFlow(value = true)
    val isSimulatedMode: StateFlow<Boolean> = _isSimulatedMode.asStateFlow()

    private val _networkLatencyMs = MutableStateFlow(2)
    val networkLatencyMs: StateFlow<Int> = _networkLatencyMs.asStateFlow()

    private val _connectionTelemetry = MutableStateFlow(
        ConnectionTelemetry(
            isConnected = true,
            isWeakSignal = false,
            isReconnecting = false,
            reconnectAttempt = 0,
            secondsUntilReconnect = 0,
            latencyMs = 2,
            lastDisconnectReason = null
        )
    )
    val connectionTelemetry: StateFlow<ConnectionTelemetry> = _connectionTelemetry.asStateFlow()

    private val _activeJogAxis = MutableStateFlow<String?>(null)
    private var jogDirection = 0
    private var jogSpeed = 1000.0

    // LinuxCNC Real Protocol Connection
    private val _connectionConfig = MutableStateFlow(LinuxCncConnectionConfig())
    val connectionConfig: StateFlow<LinuxCncConnectionConfig> = _connectionConfig.asStateFlow()

    private var rshClient: LinuxCncRshClient? = null

    val serverTelemetry: StateFlow<LinuxCncServerTelemetry>
        get() = getOrCreateRshClient().serverTelemetry

    // OkHttp WebSocket client for real hardware
    private var okHttpClient: OkHttpClient? = null
    private var webSocket: WebSocket? = null
    private var isConnectedToRealServer = false

    private var reconnectJob: Job? = null
    private var reconnectAttemptCounter = 0
    private var currentBackoffMs = 2000L
    private val maxBackoffMs = 30000L

    private fun getOrCreateRshClient(): LinuxCncRshClient {
        if (rshClient == null) {
            rshClient = LinuxCncRshClient(
                onLog = { sev, tag, msg -> logEvent(sev, tag, msg) },
                onStateUpdate = { state, mode ->
                    if (!_isSimulatedMode.value) {
                        _machineState.value = state
                        _taskMode.value = mode
                    }
                },
                onPositionUpdate = { x, y, z, a ->
                    if (!_isSimulatedMode.value) {
                        val activeOffset = _wcsOffsets.value[_currentCoordSystem.value]
                        val xOff = activeOffset?.x ?: 0.0
                        val yOff = activeOffset?.y ?: 0.0
                        val zOff = activeOffset?.z ?: 0.0
                        val aOff = activeOffset?.a ?: 0.0

                        val curMap = _axes.value.toMutableMap()
                        curMap["X"]?.let { curMap["X"] = it.copy(machinePos = x, workPos = round((x - xOff) * 1000.0) / 1000.0) }
                        curMap["Y"]?.let { curMap["Y"] = it.copy(machinePos = y, workPos = round((y - yOff) * 1000.0) / 1000.0) }
                        curMap["Z"]?.let { curMap["Z"] = it.copy(machinePos = z, workPos = round((z - zOff) * 1000.0) / 1000.0) }
                        curMap["A"]?.let { curMap["A"] = it.copy(machinePos = a, workPos = round((a - aOff) * 1000.0) / 1000.0) }
                        _axes.value = curMap
                    }
                },
                onSpindleUpdate = { isEnabled, rpm ->
                    if (!_isSimulatedMode.value) {
                        _spindle.value = _spindle.value.copy(isEnabled = isEnabled, actualRpm = rpm)
                    }
                },
                onFeedUpdate = { actualFeed ->
                    if (!_isSimulatedMode.value) {
                        _feed.value = _feed.value.copy(actualFeed = actualFeed)
                    }
                },
                onToolUpdate = { toolNum ->
                    if (!_isSimulatedMode.value) {
                        val found = _toolTable.value.find { it.id == toolNum }
                        if (found != null) {
                            _activeTool.value = found
                        }
                    }
                }
            )
        }
        return rshClient!!
    }

    init {
        loadSampleGCode()
        startKinematicsLoop()
    }

    fun logEvent(severity: LogSeverity, tag: String, message: String) {
        val updated = _eventLogs.value.toMutableList()
        updated.add(0, CncEventLog(severity = severity, tag = tag, message = message))
        _eventLogs.value = updated.take(100)
    }

    fun clearEventLogs() {
        _eventLogs.value = emptyList()
    }

    fun loadGCodeContent(fileName: String, content: String) {
        _loadedFileName.value = fileName
        val lines = content.lines()
        val segments = mutableListOf<GCodeSegment>()

        var currentX = 0f
        var currentY = 0f
        var currentZ = 0f

        lines.forEachIndexed { index, rawLine ->
            val trimmed = rawLine.trim()
            if (trimmed.isNotBlank()) {
                val isRapid = trimmed.startsWith("G0", ignoreCase = true) || trimmed.contains(" G0 ", ignoreCase = true)
                val isCut = trimmed.startsWith("G1", ignoreCase = true) || trimmed.startsWith("G2", ignoreCase = true) || trimmed.startsWith("G3", ignoreCase = true)

                val xMatch = Regex("""\bX\s*([-+]?[0-9]*\.?[0-9]+)""", RegexOption.IGNORE_CASE).find(trimmed)
                val yMatch = Regex("""\bY\s*([-+]?[0-9]*\.?[0-9]+)""", RegexOption.IGNORE_CASE).find(trimmed)
                val zMatch = Regex("""\bZ\s*([-+]?[0-9]*\.?[0-9]+)""", RegexOption.IGNORE_CASE).find(trimmed)

                val nextX = xMatch?.groupValues?.get(1)?.toFloatOrNull() ?: currentX
                val nextY = yMatch?.groupValues?.get(1)?.toFloatOrNull() ?: currentY
                val nextZ = zMatch?.groupValues?.get(1)?.toFloatOrNull() ?: currentZ

                segments.add(
                    GCodeSegment(
                        lineNumber = index + 1,
                        rawText = trimmed,
                        isRapid = isRapid,
                        isCut = isCut,
                        startX = currentX,
                        startY = currentY,
                        startZ = currentZ,
                        endX = nextX,
                        endY = nextY,
                        endZ = nextZ
                    )
                )

                currentX = nextX
                currentY = nextY
                currentZ = nextZ
            }
        }

        _loadedGCode.value = segments
        _activeGCodeLine.value = 0
        logEvent(LogSeverity.INFO, "GCODE", "Loaded program '$fileName' (${segments.size} blocks)")
    }


    private fun startKinematicsLoop() {
        scope.launch {
            while (isActive) {
                if (_isSimulatedMode.value) {
                    stepSimulation()
                }
                delay(33.milliseconds) // ~30Hz Telemetry Rate
            }
        }
    }

    private fun stepSimulation() {
        val currentAxes = _axes.value.toMutableMap()
        val currentState = _machineState.value
        val activeAxis = _activeJogAxis.value

        // Handle Active Jogging
        if ((activeAxis != null) && ((currentState == MachineStateEnum.ON) || (currentState == MachineStateEnum.IDLE))) {
            val axis = currentAxes[activeAxis]
            if (axis != null) {
                val delta = (jogDirection * jogSpeed * 0.033) / 60.0 // mm, per tick
                val newMachinePos = (axis.machinePos + delta).coerceIn(axis.minLimit, axis.maxLimit)
                val newWorkPos = (axis.workPos + delta)
                val dynamicTorque = 10.0 + ((abs(jogSpeed) / 5000.0) * 45.0) + (Math.random() * 3.0)
                val dynamicTemp = 36.0 + ((dynamicTorque / 100.0) * 8.0)

                currentAxes[activeAxis] = axis.copy(
                    machinePos = round(newMachinePos * 1000.0) / 1000.0,
                    workPos = round(newWorkPos * 1000.0) / 1000.0,
                    loadTorquePct = round(dynamicTorque * 10.0) / 10.0,
                    motorTempC = round(dynamicTemp * 10.0) / 10.0,
                    driveTempC = round((dynamicTemp + 4.5) * 10.0) / 10.0
                )
            }
        }

        // Handle G-Code Program Execution in AUTO mode
        if (currentState == MachineStateEnum.RUNNING) {
            val gcodeList = _loadedGCode.value
            val currentLine = _activeGCodeLine.value
            if (gcodeList.isNotEmpty()) {
                if (currentLine < gcodeList.size) {
                    val seg = gcodeList[currentLine]
                    // Interpolate towards end point
                    val x = currentAxes["X"]
                    val y = currentAxes["Y"]
                    val z = currentAxes["Z"]
                    if ((x != null) && (y != null) && (z != null)) {
                        val dx = seg.endX - x.workPos
                        val dy = seg.endY - y.workPos
                        val dz = seg.endZ - z.workPos
                        val dist = sqrt((dx * dx) + (dy * dy) + (dz * dz))

                        if (dist > 0.5) {
                            val step = 0.8
                            val nextX = x.workPos + ((dx / dist) * step)
                            val nextY = y.workPos + ((dy / dist) * step)
                            val nextZ = z.workPos + ((dz / dist) * step)
                            currentAxes["X"] = x.copy(
                                workPos = round(nextX * 1000.0) / 1000.0,
                                machinePos = round((nextX + 100.0) * 1000.0) / 1000.0,
                                dtgPos = round(dist * 1000.0) / 1000.0,
                                loadTorquePct = 25.0 + (Math.random() * 8.0)
                            )
                            currentAxes["Y"] = y.copy(
                                workPos = round(nextY * 1000.0) / 1000.0,
                                machinePos = round((nextY + 70.0) * 1000.0) / 1000.0,
                                dtgPos = round(dist * 1000.0) / 1000.0,
                                loadTorquePct = 22.0 + (Math.random() * 6.0)
                            )
                            currentAxes["Z"] = z.copy(
                                workPos = round(nextZ * 1000.0) / 1000.0,
                                machinePos = round((nextZ - 30.0) * 1000.0) / 1000.0,
                                dtgPos = round(abs(dz) * 1000.0) / 1000.0,
                                loadTorquePct = 30.0 + (Math.random() * 5.0)
                            )
                        } else {
                            // Update modal state based on completed line
                            updateModalStateFromSegment(seg)

                            // Check Execution Modifiers (Single Block, Optional Stop M1, Block Delete)
                            val modifiers = _executionModifiers.value
                            val isM1 = seg.rawText.contains("M1") || seg.rawText.contains("M01")

                            if (modifiers.singleBlockMode) {
                                _machineState.value = MachineStateEnum.PAUSED
                                logEvent(LogSeverity.INFO, "CYCLE", "SINGLE BLOCK: Stepped to block ${seg.lineNumber}. Paused for next cycle.")
                            } else if (modifiers.optionalStopM1 && isM1) {
                                _machineState.value = MachineStateEnum.PAUSED
                                logEvent(LogSeverity.WARNING, "CYCLE", "OPTIONAL STOP (M1): Paused at line ${seg.lineNumber} by operator condition.")
                            }

                            // Advance to next valid line, respecting Block Delete ('/' skip)
                            var nextLine = currentLine + 1
                            while (nextLine < gcodeList.size && modifiers.blockDelete && gcodeList[nextLine].rawText.trim().startsWith("/")) {
                                logEvent(LogSeverity.INFO, "CYCLE", "BLOCK DELETE: Skipped block ${gcodeList[nextLine].rawText.trim()}")
                                nextLine++
                            }

                            if (nextLine >= gcodeList.size) {
                                _machineState.value = MachineStateEnum.IDLE
                                logEvent(LogSeverity.INFO, "CYCLE", "Program Completed Successfully")
                            } else {
                                _activeGCodeLine.value = nextLine
                            }
                        }
                    }
                }
            }
        }

        _axes.value = currentAxes

        // Spindle dynamics
        val currentSpindle = _spindle.value
        if (currentSpindle.isEnabled) {
            val target = currentSpindle.commandedRpm * (currentSpindle.overridePct / 100.0)
            val currentActual = currentSpindle.actualRpm
            val newActual = currentActual + ((target - currentActual) * 0.15) + ((Math.random() * 20.0) - 10.0)
            _spindle.value = currentSpindle.copy(actualRpm = round(newActual))
        } else {
            val currentActual = currentSpindle.actualRpm
            if (currentActual > 50) {
                _spindle.value = currentSpindle.copy(actualRpm = round(currentActual * 0.85))
            } else {
                _spindle.value = currentSpindle.copy(actualRpm = 0.0)
            }
        }

        // Slight jitter in EtherCAT Telemetry
        _etherCatMaster.value = _etherCatMaster.value.copy(
            dcOffsetNs = (10L + (Math.random() * 8.0).toLong())
        )

        // Sync EtherCAT Slave telemetry with simulated axes
        if (capabilities.value.hasEtherCat) {
            val updatedSlaves = _etherCatSlaves.value.map { slave ->
                val axisName = when (slave.slaveIndex) {
                    0 -> "X"
                    1 -> "Y"
                    2 -> "Z"
                    3 -> "A"
                    else -> null
                }
                axisName?.let { name ->
                    currentAxes[name]?.let { axisData ->
                        slave.copy(
                            actualTorquePct = axisData.loadTorquePct,
                            driveTempC = axisData.driveTempC
                        )
                    }
                } ?: slave
            }
            _etherCatSlaves.value = updatedSlaves
        }
    }

    // Safety and State Commands
    fun toggleEstop() {
        val willBeEstop = _machineState.value != MachineStateEnum.ESTOP
        if (willBeEstop) {
            _machineState.value = MachineStateEnum.ESTOP
            stopJog()
            _spindle.value = _spindle.value.copy(isEnabled = false)
            logEvent(LogSeverity.CRITICAL, "SAFETY", "EMERGENCY STOP (ESTOP) TRIPPED - Motion Aborted")
        } else {
            _machineState.value = MachineStateEnum.OFF
            logEvent(LogSeverity.INFO, "SAFETY", "E-Stop circuit reset. Machine in OFF state")
        }
        if (!_isSimulatedMode.value && _connectionConfig.value.protocolType == LinuxCncProtocolType.LINUXCNCRSH_TCP) {
            rshClient?.setEstop(willBeEstop)
        }
        sendRemoteCommand("ESTOP_TOGGLE", emptyMap())
    }

    fun powerOn() {
        if (_machineState.value != MachineStateEnum.ESTOP) {
            _machineState.value = MachineStateEnum.ON
            logEvent(LogSeverity.INFO, "POWER", "Main Servo Drive Bus ON. Machine Ready")
            if (!_isSimulatedMode.value && _connectionConfig.value.protocolType == LinuxCncProtocolType.LINUXCNCRSH_TCP) {
                rshClient?.setMachinePower(true)
            }
            sendRemoteCommand("POWER_ON", emptyMap())
        } else {
            logEvent(LogSeverity.WARNING, "SAFETY", "Cannot power ON while ESTOP is engaged")
        }
    }

    fun powerOff() {
        _machineState.value = MachineStateEnum.OFF
        _spindle.value = _spindle.value.copy(isEnabled = false)
        stopJog()
        logEvent(LogSeverity.INFO, "POWER", "Servo Drives Powered OFF")
        if (!_isSimulatedMode.value && _connectionConfig.value.protocolType == LinuxCncProtocolType.LINUXCNCRSH_TCP) {
            rshClient?.setMachinePower(false)
        }
        sendRemoteCommand("POWER_OFF", emptyMap())
    }

    @Suppress("unused")
    fun setTaskMode(mode: TaskMode) {
        _taskMode.value = mode
        sendRemoteCommand("SET_MODE", mapOf("mode" to mode.name))
    }


    fun setCoordinateSystem(gSystem: String) {
        _currentCoordSystem.value = gSystem
        recalculateWorkPositionsForActiveWcs()
        logEvent(LogSeverity.INFO, "WCS", "Active Work Coordinate System switched to $gSystem")
        sendRemoteCommand("SET_G_COORD", mapOf("coord" to gSystem))
    }

    /**
     * Recalculates work positions for all axes: WorkPos = MachinePos - ActiveWcsOffset
     */
    fun recalculateWorkPositionsForActiveWcs() {
        val activeOffset = _wcsOffsets.value[_currentCoordSystem.value] ?: return
        val currentAxes = _axes.value.toMutableMap()
        currentAxes["X"]?.let {
            currentAxes["X"] = it.copy(workPos = round((it.machinePos - activeOffset.x) * 1000.0) / 1000.0)
        }
        currentAxes["Y"]?.let {
            currentAxes["Y"] = it.copy(workPos = round((it.machinePos - activeOffset.y) * 1000.0) / 1000.0)
        }
        currentAxes["Z"]?.let {
            currentAxes["Z"] = it.copy(workPos = round((it.machinePos - activeOffset.z) * 1000.0) / 1000.0)
        }
        currentAxes["A"]?.let {
            currentAxes["A"] = it.copy(workPos = round((it.machinePos - activeOffset.a) * 1000.0) / 1000.0)
        }
        _axes.value = currentAxes
    }

    /**
     * Workshop Touch-Off for an axis against the active WCS:
     * Calculates the offset so that the current physical position equals [targetWorkValue].
     * Offset = MachinePos - TargetWorkValue
     */
    fun touchOff(axis: String, targetWorkValue: Double = 0.0) {
        val axisObj = _axes.value[axis] ?: return
        val currentWcsName = _currentCoordSystem.value
        val existingOffset = _wcsOffsets.value[currentWcsName] ?: return

        val newOffsetVal = round((axisObj.machinePos - targetWorkValue) * 1000.0) / 1000.0
        val updatedOffset = when (axis.uppercase(Locale.ROOT)) {
            "X" -> existingOffset.copy(x = newOffsetVal)
            "Y" -> existingOffset.copy(y = newOffsetVal)
            "Z" -> existingOffset.copy(z = newOffsetVal)
            "A" -> existingOffset.copy(a = newOffsetVal)
            else -> existingOffset
        }

        val updatedMap = _wcsOffsets.value.toMutableMap()
        updatedMap[currentWcsName] = updatedOffset
        _wcsOffsets.value = updatedMap

        recalculateWorkPositionsForActiveWcs()
        logEvent(LogSeverity.INFO, "TOUCH-OFF", "Touch-Off on $axis: Set to $targetWorkValue mm in $currentWcsName (Offset=$newOffsetVal)")
        sendRemoteCommand("TOUCH_OFF", mapOf("coord" to currentWcsName, "axis" to axis, "target" to targetWorkValue, "pIndex" to updatedOffset.pIndex))
    }

    /**
     * Direct update of a coordinate offset for any WCS (G54..G59.3)
     */
    fun setWcsOffset(name: String, axis: String, offsetValue: Double) {
        val existingOffset = _wcsOffsets.value[name] ?: return
        val roundedVal = round(offsetValue * 1000.0) / 1000.0
        val updatedOffset = when (axis.uppercase(Locale.ROOT)) {
            "X" -> existingOffset.copy(x = roundedVal)
            "Y" -> existingOffset.copy(y = roundedVal)
            "Z" -> existingOffset.copy(z = roundedVal)
            "A" -> existingOffset.copy(a = roundedVal)
            else -> existingOffset
        }

        val updatedMap = _wcsOffsets.value.toMutableMap()
        updatedMap[name] = updatedOffset
        _wcsOffsets.value = updatedMap

        if (name == _currentCoordSystem.value) {
            recalculateWorkPositionsForActiveWcs()
        }
        logEvent(LogSeverity.INFO, "WCS", "Updated $name offset $axis = $roundedVal mm")
        sendRemoteCommand("SET_WCS_OFFSET", mapOf("coord" to name, "axis" to axis, "offset" to roundedVal, "pIndex" to updatedOffset.pIndex))
    }

    fun syncWcsOffsetsFromDatabase(offsets: List<WcsOffset>) {
        if (offsets.isEmpty()) return
        val map = _wcsOffsets.value.toMutableMap()
        for (item in offsets) {
            map[item.name] = item
        }
        _wcsOffsets.value = map
        recalculateWorkPositionsForActiveWcs()
    }

    /**
     * Real-time HAL Pin state toggle (for simulation and shop floor troubleshooting)
     */
    fun toggleHalPin(pinName: String) {
        val currentPins = _halPins.value.toMutableList()
        val index = currentPins.indexOfFirst { it.name == pinName }
        if (index != -1) {
            val pin = currentPins[index]
            val toggled = pin.copy(booleanValue = !pin.booleanValue)
            currentPins[index] = toggled
            _halPins.value = currentPins
            logEvent(LogSeverity.INFO, "HAL", "Pin ${pin.name} set to ${toggled.booleanValue}")
            sendRemoteCommand("SET_HAL_PIN", mapOf("pin" to pinName, "val" to toggled.booleanValue))
        }
    }

    /**
     * Professional MDI Command Execution with prior syntax validation and hardware dispatch
     */
    fun executeMdiCommand(rawCmd: String): Result<String> {
        val validation = mdiValidator.validate(rawCmd)
        if (!validation.isValid) {
            val errorMsg = validation.errorMessage ?: "Invalid MDI Command"
            logEvent(LogSeverity.ERROR, "MDI_ERROR", errorMsg)
            return Result.failure(IllegalArgumentException(errorMsg))
        }

        val upper = rawCmd.trim().uppercase(Locale.ROOT)

        // Parse Spindle commands
        if (upper.contains("M3") || upper.contains("M03")) {
            val sMatch = Regex("S(\\d+(?:\\.\\d+)?)").find(upper)
            val rpm = sMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: _spindle.value.commandedRpm
            setSpindleRpm(rpm)
            _spindle.value = _spindle.value.copy(isEnabled = true, isClockwise = true)
            sendRemoteCommand("SPINDLE_CW", mapOf("rpm" to rpm))
        } else if (upper.contains("M4") || upper.contains("M04")) {
            val sMatch = Regex("S(\\d+(?:\\.\\d+)?)").find(upper)
            val rpm = sMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: _spindle.value.commandedRpm
            setSpindleRpm(rpm)
            _spindle.value = _spindle.value.copy(isEnabled = true, isClockwise = false)
            sendRemoteCommand("SPINDLE_CCW", mapOf("rpm" to rpm))
        } else if (upper.contains("M5") || upper.contains("M05")) {
            _spindle.value = _spindle.value.copy(isEnabled = false)
            sendRemoteCommand("SPINDLE_STOP", emptyMap())
        }

        // Parse WCS selection commands
        val wcsCommand = listOf("G54", "G55", "G56", "G57", "G58", "G59", "G59.1", "G59.2", "G59.3").find { upper.contains(it) }
        if (wcsCommand != null) {
            setCoordinateSystem(wcsCommand)
        }

        // Parse Motion commands in simulation mode
        if (_isSimulatedMode.value && (upper.contains("G0") || upper.contains("G00") || upper.contains("G1") || upper.contains("G01"))) {
            val xMatch = Regex("X([+-]?\\d+(?:\\.\\d+)?)").find(upper)
            val yMatch = Regex("Y([+-]?\\d+(?:\\.\\d+)?)").find(upper)
            val zMatch = Regex("Z([+-]?\\d+(?:\\.\\d+)?)").find(upper)
            val aMatch = Regex("A([+-]?\\d+(?:\\.\\d+)?)").find(upper)

            val currentAxes = _axes.value.toMutableMap()
            val activeOffset = _wcsOffsets.value[_currentCoordSystem.value]

            xMatch?.groupValues?.get(1)?.toDoubleOrNull()?.let { targetWorkX ->
                currentAxes["X"]?.let { axis ->
                    val offset = activeOffset?.x ?: 0.0
                    val newMach = (targetWorkX + offset).coerceIn(axis.minLimit, axis.maxLimit)
                    currentAxes["X"] = axis.copy(workPos = targetWorkX, machinePos = round(newMach * 1000.0) / 1000.0)
                }
            }
            yMatch?.groupValues?.get(1)?.toDoubleOrNull()?.let { targetWorkY ->
                currentAxes["Y"]?.let { axis ->
                    val offset = activeOffset?.y ?: 0.0
                    val newMach = (targetWorkY + offset).coerceIn(axis.minLimit, axis.maxLimit)
                    currentAxes["Y"] = axis.copy(workPos = targetWorkY, machinePos = round(newMach * 1000.0) / 1000.0)
                }
            }
            zMatch?.groupValues?.get(1)?.toDoubleOrNull()?.let { targetWorkZ ->
                currentAxes["Z"]?.let { axis ->
                    val offset = activeOffset?.z ?: 0.0
                    val newMach = (targetWorkZ + offset).coerceIn(axis.minLimit, axis.maxLimit)
                    currentAxes["Z"] = axis.copy(workPos = targetWorkZ, machinePos = round(newMach * 1000.0) / 1000.0)
                }
            }
            aMatch?.groupValues?.get(1)?.toDoubleOrNull()?.let { targetWorkA ->
                currentAxes["A"]?.let { axis ->
                    val offset = activeOffset?.a ?: 0.0
                    val newMach = (targetWorkA + offset).coerceIn(axis.minLimit, axis.maxLimit)
                    currentAxes["A"] = axis.copy(workPos = targetWorkA, machinePos = round(newMach * 1000.0) / 1000.0)
                }
            }
            _axes.value = currentAxes
        }

        logEvent(LogSeverity.INFO, "MDI", "MDI Executed: $rawCmd")
        if (!_isSimulatedMode.value && _connectionConfig.value.protocolType == LinuxCncProtocolType.LINUXCNCRSH_TCP) {
            rshClient?.sendMdi(rawCmd)
        }
        sendRemoteCommand("MDI", mapOf("command" to rawCmd))
        return Result.success(rawCmd)
    }

    // Motion & Jog Commands
    fun startJog(axis: String, direction: Int, speedMmMin: Double = 1500.0) {
        if ((_machineState.value == MachineStateEnum.ON) || (_machineState.value == MachineStateEnum.IDLE)) {
            _activeJogAxis.value = axis
            jogDirection = direction
            jogSpeed = speedMmMin
            if (!_isSimulatedMode.value && _connectionConfig.value.protocolType == LinuxCncProtocolType.LINUXCNCRSH_TCP) {
                val axisIdx = when (axis.uppercase(Locale.ROOT)) {
                    "X" -> 0
                    "Y" -> 1
                    "Z" -> 2
                    "A" -> 3
                    else -> 0
                }
                val speedMmSec = (speedMmMin / 60.0) * direction
                rshClient?.startJog(axisIdx, speedMmSec)
            }
            sendRemoteCommand("JOG", mapOf("axis" to axis, "direction" to direction, "speed" to speedMmMin))
        }
    }

    fun stopJog() {
        _activeJogAxis.value?.let { axis ->
            _activeJogAxis.value = null
            jogDirection = 0
            if (!_isSimulatedMode.value && _connectionConfig.value.protocolType == LinuxCncProtocolType.LINUXCNCRSH_TCP) {
                val axisIdx = when (axis.uppercase(Locale.ROOT)) {
                    "X" -> 0
                    "Y" -> 1
                    "Z" -> 2
                    "A" -> 3
                    else -> 0
                }
                rshClient?.stopJog(axisIdx)
            }
            sendRemoteCommand("JOG_STOP", mapOf("axis" to axis))
        }
    }

    fun stepJog(axis: String, direction: Int, stepSizeMm: Double) {
        if ((_machineState.value == MachineStateEnum.ON) || (_machineState.value == MachineStateEnum.IDLE)) {
            val currentMap = _axes.value.toMutableMap()
            val axisObj = currentMap[axis]
            if (axisObj != null) {
                val delta = direction * stepSizeMm
                val newMachinePos = (axisObj.machinePos + delta).coerceIn(axisObj.minLimit, axisObj.maxLimit)
                val newWorkPos = axisObj.workPos + delta
                currentMap[axis] = axisObj.copy(
                    machinePos = round(newMachinePos * 1000.0) / 1000.0,
                    workPos = round(newWorkPos * 1000.0) / 1000.0
                )
                _axes.value = currentMap
            }
            if (!_isSimulatedMode.value && _connectionConfig.value.protocolType == LinuxCncProtocolType.LINUXCNCRSH_TCP) {
                val axisIdx = when (axis.uppercase(Locale.ROOT)) {
                    "X" -> 0
                    "Y" -> 1
                    "Z" -> 2
                    "A" -> 3
                    else -> 0
                }
                val speedMmSec = jogSpeed / 60.0
                rshClient?.jogIncremental(axisIdx, speedMmSec, direction * stepSizeMm)
            }
            sendRemoteCommand("JOG_STEP", mapOf("axis" to axis, "direction" to direction, "step" to stepSizeMm))
        }
    }

    fun zeroAxis(axis: String) {
        val currentMap = _axes.value.toMutableMap()
        val axisObj = currentMap[axis]
        if (axisObj != null) {
            currentMap[axis] = axisObj.copy(workPos = 0.0)
            _axes.value = currentMap
        }
        sendRemoteCommand("ZERO_AXIS", mapOf("axis" to axis, "coord" to _currentCoordSystem.value))
    }

    fun setAxisWorkPosition(axis: String, position: Double) {
        val currentMap = _axes.value.toMutableMap()
        val axisObj = currentMap[axis]
        if (axisObj != null) {
            currentMap[axis] = axisObj.copy(workPos = position)
            _axes.value = currentMap
        }
        sendRemoteCommand("SET_AXIS_POS", mapOf("axis" to axis, "pos" to position, "coord" to _currentCoordSystem.value))
    }

    fun zeroAllAxes() {
        val currentMap = _axes.value.toMutableMap()
        currentMap.keys.forEach { key ->
            currentMap[key] = currentMap[key]?.copy(workPos = 0.0) ?: return@forEach
        }
        _axes.value = currentMap
        logEvent(LogSeverity.INFO, "WCS", "All Axes Zeroed to ${_currentCoordSystem.value} origin")
        sendRemoteCommand("ZERO_ALL", mapOf("coord" to _currentCoordSystem.value))
    }

    fun homeAxis(axis: String) {
        val currentMap = _axes.value.toMutableMap()
        val axisObj = currentMap[axis]
        if (axisObj != null) {
            val prevState = _machineState.value
            _machineState.value = MachineStateEnum.HOMING
            scope.launch {
                delay(800.milliseconds) // Simulating physical motion
                currentMap[axis] = axisObj.copy(isHomed = true, machinePos = 0.0, workPos = 0.0)
                _axes.value = currentMap
                _machineState.value = prevState
                logEvent(LogSeverity.INFO, "HOMING", "Axis $axis Homed successfully")
            }
        }
        sendRemoteCommand("HOME_AXIS", mapOf("axis" to axis))
    }

    fun homeAllAxes() {
        val currentMap = _axes.value.toMutableMap()
        val prevState = _machineState.value
        _machineState.value = MachineStateEnum.HOMING
        scope.launch {
            delay(1500.milliseconds) // Simulating multi-axis motion
            currentMap.keys.forEach { key ->
                currentMap[key]?.let { obj ->
                    currentMap[key] = obj.copy(isHomed = true, machinePos = 0.0, workPos = 0.0)
                }
            }
            _axes.value = currentMap
            _machineState.value = prevState
            logEvent(LogSeverity.INFO, "HOMING", "All machine axes homed to physical index switches")
        }
        sendRemoteCommand("HOME_ALL", emptyMap())
    }

    // Spindle & Feed Controls
    fun toggleSpindle() {
        val cur = _spindle.value
        val newState = !cur.isEnabled
        _spindle.value = cur.copy(isEnabled = newState)
        logEvent(LogSeverity.INFO, "SPINDLE", if (newState) "Spindle ON at ${cur.commandedRpm.toInt()} RPM" else "Spindle STOP")
        sendRemoteCommand("SPINDLE_TOGGLE", mapOf("enabled" to _spindle.value.isEnabled, "rpm" to _spindle.value.commandedRpm))
    }

    fun setSpindleRpm(rpm: Double) {
        _spindle.value = _spindle.value.copy(commandedRpm = rpm)
        sendRemoteCommand("SET_SPINDLE_RPM", mapOf("rpm" to rpm))
    }

    fun setSpindleOverride(pct: Int) {
        _spindle.value = _spindle.value.copy(overridePct = pct.coerceIn(10, 200))
        if (!_isSimulatedMode.value && _connectionConfig.value.protocolType == LinuxCncProtocolType.LINUXCNCRSH_TCP) {
            rshClient?.setSpindleOverride(pct / 100.0)
        }
        sendRemoteCommand("SPINDLE_OVERRIDE", mapOf("override" to pct))
    }

    fun setFeedOverride(pct: Int) {
        _feed.value = _feed.value.copy(feedOverridePct = pct.coerceIn(0, 200))
        if (!_isSimulatedMode.value && _connectionConfig.value.protocolType == LinuxCncProtocolType.LINUXCNCRSH_TCP) {
            rshClient?.setFeedOverride(pct / 100.0)
        }
        sendRemoteCommand("FEED_OVERRIDE", mapOf("override" to pct))
    }

    fun toggleMistCoolant() {
        val cur = _coolant.value
        _coolant.value = cur.copy(mist = !cur.mist)
        sendRemoteCommand("COOLANT_MIST", mapOf("mist" to _coolant.value.mist))
    }

    fun toggleFloodCoolant() {
        val cur = _coolant.value
        _coolant.value = cur.copy(flood = !cur.flood)
        sendRemoteCommand("COOLANT_FLOOD", mapOf("flood" to _coolant.value.flood))
    }

    // Program Cycle Controls
    fun cycleStart() {
        if ((_machineState.value == MachineStateEnum.ON) || (_machineState.value == MachineStateEnum.IDLE) || (_machineState.value == MachineStateEnum.PAUSED)) {
            _machineState.value = MachineStateEnum.RUNNING
            _taskMode.value = TaskMode.AUTO
            _spindle.value = _spindle.value.copy(isEnabled = true)
            logEvent(LogSeverity.INFO, "CYCLE", "Cycle Started: Executing program '${_loadedFileName.value}'")
            if (!_isSimulatedMode.value && _connectionConfig.value.protocolType == LinuxCncProtocolType.LINUXCNCRSH_TCP) {
                rshClient?.cycleStart()
            }
            sendRemoteCommand("CYCLE_START", emptyMap())
        } else {
            logEvent(LogSeverity.WARNING, "CYCLE", "Cannot start cycle: Machine is ${_machineState.value.name}")
        }
    }

    fun feedHold() {
        if (_machineState.value == MachineStateEnum.RUNNING) {
            _machineState.value = MachineStateEnum.PAUSED
            logEvent(LogSeverity.WARNING, "CYCLE", "FEED HOLD / PAUSED by Operator")
            if (!_isSimulatedMode.value && _connectionConfig.value.protocolType == LinuxCncProtocolType.LINUXCNCRSH_TCP) {
                rshClient?.feedHold()
            }
            sendRemoteCommand("FEEDHOLD", emptyMap())
        }
    }

    fun cycleStop() {
        _machineState.value = MachineStateEnum.IDLE
        _activeGCodeLine.value = 0
        logEvent(LogSeverity.INFO, "CYCLE", "Cycle Aborted / Stopped")
        if (!_isSimulatedMode.value && _connectionConfig.value.protocolType == LinuxCncProtocolType.LINUXCNCRSH_TCP) {
            rshClient?.cycleStop()
        }
        sendRemoteCommand("ABORT", emptyMap())
    }

    // Metrology & Probing Simulation
    fun triggerProbeRoutine(routineType: String) {
        scope.launch {
            logEvent(LogSeverity.INFO, "PROBE", "Starting Metrology routine: $routineType")
            _probe.value = _probe.value.copy(activeRoutine = routineType, isTripped = false)
            delay(600.milliseconds)
            _probe.value = _probe.value.copy(
                isTripped = true,
                lastContactX = _axes.value["X"]?.workPos ?: 0.0,
                lastContactY = _axes.value["Y"]?.workPos ?: 0.0,
                lastContactZ = _axes.value["Z"]?.workPos ?: 0.0
            )
            logEvent(LogSeverity.INFO, "PROBE", "Touch Contact Confirmed at (${String.format(Locale.US, "%.3f", _probe.value.lastContactX)}, ${String.format(Locale.US, "%.3f", _probe.value.lastContactY)}, ${String.format(Locale.US, "%.3f", _probe.value.lastContactZ)})")
            delay(400.milliseconds)
            _probe.value = _probe.value.copy(isTripped = false, activeRoutine = null)
        }
        sendRemoteCommand("PROBE_CYCLE", mapOf("routine" to routineType))
    }

    // Tool Table & ATC Operations
    fun mountTool(toolId: Int) {
        val currentList = _toolTable.value
        val target = currentList.find { it.id == toolId } ?: return
        val updated = currentList.map { it.copy(isActive = (it.id == toolId)) }
        _toolTable.value = updated
        _activeTool.value = target.copy(isActive = true)
        _tool.value = ToolInfo(
            toolNumber = target.id,
            description = target.description,
            lengthOffset = target.lengthOffset,
            diameterOffset = target.diameter,
            atcSlot = target.pocket
        )
        logEvent(LogSeverity.INFO, "ATC", "Tool changed to T${target.id} (${target.description}), Offset G43 H${target.id} applied")
        sendRemoteCommand("TOOL_CHANGE", mapOf("tool" to toolId, "pocket" to target.pocket))
    }

    fun updateToolItem(updatedTool: CncToolItem) {
        val currentList = _toolTable.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == updatedTool.id }
        if (index >= 0) {
            currentList[index] = updatedTool
        } else {
            currentList.add(updatedTool)
        }
        _toolTable.value = currentList
        if (updatedTool.isActive) {
            _activeTool.value = updatedTool
            _tool.value = ToolInfo(
                toolNumber = updatedTool.id,
                description = updatedTool.description,
                lengthOffset = updatedTool.lengthOffset,
                diameterOffset = updatedTool.diameter,
                atcSlot = updatedTool.pocket
            )
        }
        logEvent(LogSeverity.INFO, "TOOL_TABLE", "Tool T${updatedTool.id} parameters updated")
    }

    fun deleteTool(toolId: Int) {
        val currentList = _toolTable.value.filter { it.id != toolId }
        _toolTable.value = currentList
        logEvent(LogSeverity.INFO, "TOOL_TABLE", "Tool T$toolId removed from carousel")
    }

    fun touchOffToolZ(toolId: Int, currentSpindleZ: Double) {
        val target = _toolTable.value.find { it.id == toolId } ?: return
        val updated = target.copy(lengthOffset = round(abs(currentSpindleZ) * 1000.0) / 1000.0)
        updateToolItem(updated)
        logEvent(LogSeverity.INFO, "TOOL_OFFSET", "T$toolId Tool Length Offset (TLO) set to ${updated.lengthOffset} mm via Touch-Off")
    }

    // Switch Architecture Profile dynamically
    fun switchArchitecture(arch: HardwareArchitecture) {
        val axesList = when (arch) {
            HardwareArchitecture.ETHERCAT_DELTA -> listOf("X", "Y", "Z", "A")
            HardwareArchitecture.MESA_FPGA -> listOf("X", "Y", "Z")
            HardwareArchitecture.PARPORT_LEGACY -> listOf("X", "Y", "Z")
            HardwareArchitecture.STEP_DIR_CLOSED_LOOP -> listOf("X", "Y", "Z")
        }

        _capabilities.value = _capabilities.value.copy(
            architecture = arch,
            compatibilityLevel = arch.level,
            axes = axesList,
            hasServoTorque = (arch == HardwareArchitecture.ETHERCAT_DELTA),
            hasDriveTemp = (arch == HardwareArchitecture.ETHERCAT_DELTA),
            hasEtherCat = (arch == HardwareArchitecture.ETHERCAT_DELTA),
            hasProbe = (true)
        )

        // Adjust axes map if needed
        val newMap = mutableMapOf<String, AxisCoord>()
        axesList.forEach { ax ->
            newMap[ax] = _axes.value[ax] ?: AxisCoord(name = ax)
        }
        _axes.value = newMap
    }

    // Network / Live Server Connection
    fun connectLinuxCnc(config: LinuxCncConnectionConfig) {
        _connectionConfig.value = config
        if (config.protocolType == LinuxCncProtocolType.SIMULATION_LOCAL) {
            disconnectLinuxCnc()
            _isSimulatedMode.value = true
            logEvent(LogSeverity.INFO, "NETWORK", "Cambiado a Modo Simulación Local (Offline)")
            return
        }

        _isSimulatedMode.value = false
        if (config.protocolType == LinuxCncProtocolType.LINUXCNCRSH_TCP) {
            try {
                webSocket?.close(1000, "Switching to linuxcncrsh")
            } catch (_: Exception) {}
            val client = getOrCreateRshClient()
            client.connect(config.hostIp, config.port, config.password)
            isConnectedToRealServer = true
            _capabilities.value = _capabilities.value.copy(hostIp = config.hostIp, port = config.port, isConnected = true)
        } else if (config.protocolType == LinuxCncProtocolType.WEBSOCKET_JSON) {
            rshClient?.disconnect()
            connectToHost(config.hostIp, config.port)
        }
    }

    fun disconnectLinuxCnc() {
        rshClient?.disconnect()
        try {
            webSocket?.close(1000, "User disconnected")
        } catch (_: Exception) {}
        isConnectedToRealServer = false
        _isSimulatedMode.value = true
        _capabilities.value = _capabilities.value.copy(isConnected = false)
        logEvent(LogSeverity.INFO, "NETWORK", "Desconectado de LinuxCNC. Modo Simulación activo.")
    }

    fun applyIniConfig(config: LinuxCncMachineConfig) {
        val currentMap = _axes.value.toMutableMap()
        currentMap["X"]?.let { currentMap["X"] = it.copy(minLimit = config.xMinLimit, maxLimit = config.xMaxLimit) }
        currentMap["Y"]?.let { currentMap["Y"] = it.copy(minLimit = config.yMinLimit, maxLimit = config.yMaxLimit) }
        currentMap["Z"]?.let { currentMap["Z"] = it.copy(minLimit = config.zMinLimit, maxLimit = config.zMaxLimit) }
        currentMap["A"]?.let { currentMap["A"] = it.copy(minLimit = config.aMinLimit, maxLimit = config.aMaxLimit) }
        _axes.value = currentMap

        logEvent(
            LogSeverity.INFO,
            "CONFIG",
            "Configuración INI aplicada: ${config.machineName} (Límites: X[${config.xMinLimit}..${config.xMaxLimit}], Y[${config.yMinLimit}..${config.yMaxLimit}], Z[${config.zMinLimit}..${config.zMaxLimit}])"
        )
    }

    fun importToolTable(content: String): Int {
        val tools = LinuxCncToolTableParser.parseToolTable(content)
        if (tools.isNotEmpty()) {
            _toolTable.value = tools
            _activeTool.value = tools.firstOrNull { it.isActive } ?: tools.first()
            _tool.value = _tool.value.copy(
                toolNumber = _activeTool.value.id,
                description = _activeTool.value.description,
                lengthOffset = _activeTool.value.lengthOffset,
                diameterOffset = _activeTool.value.diameter,
                atcSlot = _activeTool.value.pocket
            )
            logEvent(LogSeverity.INFO, "TOOL", "Tabla de herramientas importada con éxito: ${tools.size} herramientas cargadas.")
            return tools.size
        }
        return 0
    }

    fun exportToolTable(): String {
        return LinuxCncToolTableParser.exportToolTable(_toolTable.value)
    }

    fun mountToolWithG43(toolId: Int) {
        val found = _toolTable.value.find { it.id == toolId } ?: return
        val updated = _toolTable.value.map { it.copy(isActive = it.id == toolId) }
        _toolTable.value = updated
        _activeTool.value = found
        _tool.value = _tool.value.copy(
            toolNumber = found.id,
            description = found.description,
            lengthOffset = found.lengthOffset,
            diameterOffset = found.diameter,
            atcSlot = found.pocket
        )
        logEvent(
            LogSeverity.INFO,
            "TOOL",
            "Herramienta montada: T${found.id} (${found.description}), compensación G43 H${found.id} (Offset Z: ${found.lengthOffset} mm)"
        )
        executeMdiCommand("M6 T${found.id} G43 H${found.id}")
    }

    fun connectToHost(hostIp: String, port: Int = 8000) {
        reconnectJob?.cancel()
        currentBackoffMs = 1000L
        _capabilities.value = _capabilities.value.copy(hostIp = hostIp, port = port)
        internalConnect()
    }

    private fun internalConnect() {
        val hostIp = _capabilities.value.hostIp
        val port = _capabilities.value.port
        if (hostIp.isEmpty()) return

        scope.launch {
            try {
                if (okHttpClient == null) {
                    okHttpClient = OkHttpClient.Builder()
                        .readTimeout(0, TimeUnit.MILLISECONDS)
                        .build()
                }

                val request = Request.Builder()
                    .url("ws://$hostIp:$port/ws/telemetry")
                    .build()

                webSocket = okHttpClient?.newWebSocket(
                    request,
                    object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        isConnectedToRealServer = true
                        _isSimulatedMode.value = false
                        _capabilities.value = _capabilities.value.copy(isConnected = true, pingMs = 5)
                        currentBackoffMs = 2000L // Reset backoff on success
                        reconnectAttemptCounter = 0
                        _connectionTelemetry.value = ConnectionTelemetry(
                            isConnected = true,
                            isWeakSignal = false,
                            isReconnecting = false,
                            reconnectAttempt = 0,
                            secondsUntilReconnect = 0,
                            latencyMs = 5,
                            lastDisconnectReason = null,
                        )
                        logEvent(LogSeverity.INFO, "NETWORK", "Connected to LinuxCNC Host")
                    }

                    override fun onMessage(webSocket: WebSocket, text: String) {
                        parseIncomingTelemetry(text)
                    }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        handleDisconnect("WebSocket Failure: ${t.message}")
                    }

                    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                        handleDisconnect("WebSocket Closed")
                    }
                })
            } catch (e: Exception) {
                handleDisconnect("Connection failed: ${e.message}")
            }
        }
    }

    fun handleDisconnect(reason: String) {
        isConnectedToRealServer = false
        _isSimulatedMode.value = true
        _capabilities.value = _capabilities.value.copy(isConnected = false)
        reconnectAttemptCounter++
        val waitSeconds = (currentBackoffMs / 1000L).coerceIn(2L, 30L).toInt()

        _connectionTelemetry.value = _connectionTelemetry.value.copy(
            isConnected = false,
            isReconnecting = true,
            reconnectAttempt = reconnectAttemptCounter,
            secondsUntilReconnect = waitSeconds,
            lastDisconnectReason = reason,
        )

        logEvent(LogSeverity.WARNING, "NETWORK", "$reason. Reconexión automática en ${waitSeconds}s (Intento #$reconnectAttemptCounter)...")

        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            for (sec in waitSeconds downTo 1) {
                _connectionTelemetry.value = _connectionTelemetry.value.copy(
                    secondsUntilReconnect = sec,
                )
                delay(1000.milliseconds)
            }
            _connectionTelemetry.value = _connectionTelemetry.value.copy(secondsUntilReconnect = 0)
            currentBackoffMs = (currentBackoffMs * 2).coerceAtMost(maxBackoffMs)
            internalConnect()
        }
    }

    fun reconnectNow() {
        reconnectJob?.cancel()
        _connectionTelemetry.value = _connectionTelemetry.value.copy(
            secondsUntilReconnect = 0,
            isReconnecting = true,
        )
        logEvent(LogSeverity.INFO, "NETWORK", "Reconexión manual iniciada por el operador...")
        currentBackoffMs = 2000L
        internalConnect()
    }

    fun setWeakSignalSimulation(isWeak: Boolean) {
        val latency = if (isWeak) 380 else 2
        _networkLatencyMs.value = latency
        _connectionTelemetry.value = _connectionTelemetry.value.copy(
            isWeakSignal = isWeak,
            latencyMs = latency,
        )
        if (isWeak) {
            logEvent(
                LogSeverity.WARNING,
                "NETWORK",
                "Alarma de red: Señal débil detectada. Latencia elevada ($latency ms). Riesgo de pérdida de sincronismo.",
            )
        } else {
            logEvent(
                LogSeverity.INFO,
                "NETWORK",
                "Señal de red normalizada (Latencia: $latency ms).",
            )
        }
    }

    fun simulateConnectionLoss() {
        handleDisconnect("Pérdida de señal de red simulada por el operador")
    }

    fun setSimulatedMode(enabled: Boolean) {
        _isSimulatedMode.value = enabled
        if (enabled) {
            logEvent(LogSeverity.INFO, "SYSTEM", "Modo Simulación Virtual ACTIVADO (Cinemática y emulación HAL/NML)")
        } else {
            logEvent(LogSeverity.WARNING, "SYSTEM", "Modo Simulación DESACTIVADO: Operando con máquina física LinuxCNC")
            if (!isConnectedToRealServer) {
                internalConnect()
            }
        }
    }

    fun simulateProbeTouch() {
        scope.launch {
            val curX = _axes.value["X"]?.workPos ?: 0.0
            val curY = _axes.value["Y"]?.workPos ?: 0.0
            val curZ = _axes.value["Z"]?.workPos ?: 0.0
            _probe.value = _probe.value.copy(
                isTripped = true,
                lastContactX = curX,
                lastContactY = curY,
                lastContactZ = curZ
            )
            logEvent(
                LogSeverity.INFO,
                "PROBE",
                "Disparo de palpador 3D simulado en X:${String.format(Locale.US, "%.3f", curX)} Y:${String.format(Locale.US, "%.3f", curY)} Z:${String.format(Locale.US, "%.3f", curZ)}"
            )
            delay(1200.milliseconds)
            _probe.value = _probe.value.copy(isTripped = false)
        }
    }

    fun restoreConnectionSimulation() {
        reconnectJob?.cancel()
        reconnectAttemptCounter = 0
        currentBackoffMs = 2000L
        _isSimulatedMode.value = true
        _networkLatencyMs.value = 2
        _connectionTelemetry.value = ConnectionTelemetry(
            isConnected = true,
            isWeakSignal = false,
            isReconnecting = false,
            reconnectAttempt = 0,
            secondsUntilReconnect = 0,
            latencyMs = 2,
            lastDisconnectReason = null,
        )
        logEvent(LogSeverity.INFO, "NETWORK", "Enlace de red restaurado con éxito.")
    }

    private fun parseIncomingTelemetry(jsonText: String) {
        try {
            val json = JSONObject(jsonText)
            if (json.has("state")) {
                val stateStr = json.getString("state")
                _machineState.value = when (stateStr) {
                    "ON" -> MachineStateEnum.ON
                    "ESTOP" -> MachineStateEnum.ESTOP
                    "RUNNING" -> MachineStateEnum.RUNNING
                    "PAUSED" -> MachineStateEnum.PAUSED
                    else -> MachineStateEnum.OFF
                }
            }
        } catch (e: Exception) {
            logEvent(LogSeverity.ERROR, "TELEMETRY", "Parse error: ${e.message}")
        }
    }

    private fun sendRemoteCommand(cmdName: String, args: Map<String, Any>) {
        if (isConnectedToRealServer && (webSocket != null)) {
            val json = JSONObject()
            json.put("command", cmdName)
            val argsObj = JSONObject()
            args.forEach { (k, v) -> argsObj.put(k, v) }
            json.put("args", argsObj)
            webSocket?.send(json.toString())
        }
    }

    private fun loadSampleGCode() {
        val sampleCode = listOf(
            GCodeSegment(1, "G21 (Metric Units)", isRapid = false, isCut = false),
            GCodeSegment(2, "G90 G54 (Absolute Coord System)", isRapid = false, isCut = false),
            GCodeSegment(3, "G0 Z10.000 (Safe Height)", isRapid = true, isCut = false, startX = 0f, startY = 0f, startZ = 0f, endX = 0f, endY = 0f, endZ = 10f),
            GCodeSegment(4, "G0 X10.000 Y10.000", isRapid = true, isCut = false, startX = 0f, startY = 0f, startZ = 10f, endX = 10f, endY = 10f, endZ = 10f),
            GCodeSegment(5, "G1 Z-2.500 F500", isRapid = false, isCut = true, startX = 10f, startY = 10f, startZ = 10f, endX = 10f, endY = 10f, endZ = -2.5f),
            GCodeSegment(6, "G1 X60.000 Y10.000 F1500", isRapid = false, isCut = true, startX = 10f, startY = 10f, startZ = -2.5f, endX = 60f, endY = 10f, endZ = -2.5f),
            GCodeSegment(7, "G1 X60.000 Y60.000", isRapid = false, isCut = true, startX = 60f, startY = 10f, startZ = -2.5f, endX = 60f, endY = 60f, endZ = -2.5f),
            GCodeSegment(8, "G1 X10.000 Y60.000", isRapid = false, isCut = true, startX = 60f, startY = 60f, startZ = -2.5f, endX = 10f, endY = 60f, endZ = -2.5f),
            GCodeSegment(9, "G1 X10.000 Y10.000", isRapid = false, isCut = true, startX = 10f, startY = 60f, startZ = -2.5f, endX = 10f, endY = 10f, endZ = -2.5f),
            GCodeSegment(10, "G0 Z15.000 M5 (Retract & Spindle Off)", isRapid = true, isCut = false, startX = 10f, startY = 10f, startZ = -2.5f, endX = 10f, endY = 10f, endZ = 15f),
            GCodeSegment(11, "G0 X0.000 Y0.000 (Return Home)", isRapid = true, isCut = false, startX = 10f, startY = 10f, startZ = 15f, endX = 0f, endY = 0f, endZ = 15f),
            GCodeSegment(12, "M30 (End of Program)", isRapid = false, isCut = false)
        )
        _loadedGCode.value = sampleCode
    }

    // --- Metrological Axis Calibration Engine ---
    private val _activeCalibrationSession = MutableStateFlow<AxisCalibrationSession?>(null)
    val activeCalibrationSession: StateFlow<AxisCalibrationSession?> = _activeCalibrationSession.asStateFlow()

    fun startAxisCalibration(
        axis: String = "X",
        totalTravelMm: Double = 600.0,
        stepIntervalPercent: Double = 10.0,
        instrumentName: String = "Dial Indicator (0.001mm Resolution)",
        instrumentUncertaintyMm: Double = 0.003
    ) {
        val totalSteps = ((100.0 / stepIntervalPercent).toInt() + 1).coerceAtLeast(2)
        val stepSizeMm = totalTravelMm / (totalSteps - 1)

        val points = (0 until totalSteps).map { i ->
            val percent = i * stepIntervalPercent
            val nominalPos = i * stepSizeMm
            AxisCalibrationPoint(
                stepIndex = i,
                percentOfTravel = percent,
                nominalPositionMm = nominalPos,
                measuredPositionMm = null,
                errorMm = null,
                sectorUncertaintyMm = instrumentUncertaintyMm
            )
        }

        val session = AxisCalibrationSession(
            axis = axis,
            totalTravelMm = totalTravelMm,
            stepIntervalPercent = stepIntervalPercent,
            totalSteps = totalSteps,
            instrumentName = instrumentName,
            instrumentUncertaintyMm = instrumentUncertaintyMm,
            points = points,
            isCompleted = false
        )
        _activeCalibrationSession.value = session
        logEvent(LogSeverity.INFO, "METROLOGY", "Started $axis-Axis Calibration session with $totalSteps points ($stepIntervalPercent% step)")
    }

    fun recordCalibrationMeasurement(stepIndex: Int, measuredValueMm: Double) {
        val current = _activeCalibrationSession.value ?: return
        val updatedPoints = current.points.map { pt ->
            if (pt.stepIndex == stepIndex) {
                val error = measuredValueMm - pt.nominalPositionMm
                pt.copy(
                    measuredPositionMm = measuredValueMm,
                    errorMm = error,
                    sectorUncertaintyMm = current.instrumentUncertaintyMm
                )
            } else pt
        }

        val measuredPoints = updatedPoints.filter { it.measuredPositionMm != null }
        val isAllCompleted = measuredPoints.size == updatedPoints.size

        val maxErr = if (measuredPoints.isNotEmpty()) measuredPoints.maxOf {
            abs(
                it.errorMm ?: 0.0
            )
        } else 0.0
        val meanErr = if (measuredPoints.isNotEmpty()) measuredPoints.asSequence().map { it.errorMm ?: 0.0 }.average() else 0.0

        // Expanded uncertainty k=2 calculation: Uc = sqrt(u_inst^2 + u_repeat^2) * 2
        val repeatUncertainty = if (measuredPoints.size > 1) {
            val variance = measuredPoints.sumOf { ((it.errorMm ?: 0.0) - meanErr).pow(2.0) } / (measuredPoints.size - 1)
            sqrt(variance)
        } else current.instrumentUncertaintyMm

        val combinedUncertainty = sqrt(
            current.instrumentUncertaintyMm.pow(2.0) + repeatUncertainty.pow(2.0)
        )
        val expandedUncertainty = combinedUncertainty * 2.0

        _activeCalibrationSession.value = current.copy(
            points = updatedPoints,
            isCompleted = isAllCompleted,
            maxErrorMm = maxErr,
            meanErrorMm = meanErr,
            expandedUncertaintyMm = expandedUncertainty
        )

        logEvent(LogSeverity.INFO, "METROLOGY", "Recorded ${current.axis} point [$stepIndex] Nominal=${updatedPoints[stepIndex].nominalPositionMm}mm, Measured=${measuredValueMm}mm, Err=${String.format(Locale.US, "%.4f", updatedPoints[stepIndex].errorMm)}mm")
    }

    fun moveAxisToNominal(stepIndex: Int) {
        val current = _activeCalibrationSession.value ?: return
        val point = current.points.getOrNull(stepIndex) ?: return
        val axisName = current.axis
        val targetPos = point.nominalPositionMm

        val currentAxes = _axes.value.toMutableMap()
        val currentAxisData = currentAxes[axisName] ?: AxisCoord(name = axisName)
        currentAxes[axisName] = currentAxisData.copy(
            workPos = targetPos,
            machinePos = targetPos,
            isHomed = true
        )
        _axes.value = currentAxes
        logEvent(LogSeverity.INFO, "METROLOGY", "Moved Axis $axisName to Nominal Pos $targetPos mm for Calibration Step $stepIndex")
    }

    fun generateLinuxCncCompTable(session: AxisCalibrationSession): String {
        val sb = StringBuilder()
        sb.append("# LinuxCNC Screw Pitch Compensation Table (comp.tbl)\n")
        sb.append("# Axis: ${session.axis} | Total Travel: ${session.totalTravelMm} mm\n")
        sb.append("# Instrument: ${session.instrumentName} (±${session.instrumentUncertaintyMm} mm)\n")
        sb.append("# Max Deviation: ${String.format(Locale.US, "%.4f", session.maxErrorMm)} mm | ")
        sb.append(
            "Expanded Uncertainty U (k=2): ±${String.format(
                Locale.US,
                "%.4f",
                session.expandedUncertaintyMm,
            )} mm\n"
        )
        sb.append("# Format: Nominal_Position_Pos_Forward  Compensation_Forward  Nominal_Position_Neg_Reverse  Compensation_Reverse\n\n")

        session.points.forEach { pt ->
            val nominal = pt.nominalPositionMm
            val comp = if (pt.errorMm != null) -pt.errorMm else 0.0 // Negative of error is compensation
            sb.append(String.format(Locale.US, "%10.4f  %10.4f  %10.4f  %10.4f\n", nominal, comp, nominal, comp))
        }
        return sb.toString()
    }

    // ============================================================================
    // PRO MODULE 5: MODAL STATE PARSER & RUN-FROM-LINE IMPLEMENTATION
    // ============================================================================
    fun setSingleBlockMode(enabled: Boolean) {
        _executionModifiers.value = _executionModifiers.value.copy(singleBlockMode = enabled)
        logEvent(LogSeverity.INFO, "CYCLE", "Single Block Mode ${if (enabled) "ENABLED" else "DISABLED"}")
        sendRemoteCommand("SET_SINGLE_BLOCK", mapOf("enabled" to enabled))
    }

    fun setOptionalStop(enabled: Boolean) {
        _executionModifiers.value = _executionModifiers.value.copy(optionalStopM1 = enabled)
        logEvent(LogSeverity.INFO, "CYCLE", "Optional Stop (M1) ${if (enabled) "ENABLED" else "DISABLED"}")
        sendRemoteCommand("SET_OPTIONAL_STOP", mapOf("enabled" to enabled))
    }

    fun setBlockDelete(enabled: Boolean) {
        _executionModifiers.value = _executionModifiers.value.copy(blockDelete = enabled)
        logEvent(LogSeverity.INFO, "CYCLE", "Block Delete (/) ${if (enabled) "ENABLED" else "DISABLED"}")
        sendRemoteCommand("SET_BLOCK_DELETE", mapOf("enabled" to enabled))
    }

    fun singleBlockStep() {
        if (_machineState.value == MachineStateEnum.PAUSED || _machineState.value == MachineStateEnum.IDLE || _machineState.value == MachineStateEnum.ON) {
            _machineState.value = MachineStateEnum.RUNNING
            _taskMode.value = TaskMode.AUTO
            logEvent(LogSeverity.INFO, "CYCLE", "SINGLE BLOCK: Step commanded")
            sendRemoteCommand("SINGLE_BLOCK_STEP", emptyMap())
        }
    }

    fun runFromLine(targetLineIndex: Int) {
        val gcodeList = _loadedGCode.value
        if (targetLineIndex in gcodeList.indices) {
            _activeGCodeLine.value = targetLineIndex
            val seg = gcodeList[targetLineIndex]
            
            // Retract Z to safe clearance, start spindle, then reposition
            val currentMap = _axes.value.toMutableMap()
            val zAxis = currentMap["Z"]
            if (zAxis != null) {
                // Raise Z safely
                currentMap["Z"] = zAxis.copy(workPos = 10.0, machinePos = (10.0 + (_wcsOffsets.value[_currentCoordSystem.value]?.z ?: 0.0)))
                _axes.value = currentMap
            }
            
            _spindle.value = _spindle.value.copy(isEnabled = true)
            _machineState.value = MachineStateEnum.PAUSED
            _taskMode.value = TaskMode.AUTO
            
            logEvent(LogSeverity.WARNING, "CYCLE", "RUN FROM LINE: Positioned at block ${seg.lineNumber} ('${seg.rawText.trim()}'). Spindle started, Z safe. Press CYCLE START to engage.")
            sendRemoteCommand("RUN_FROM_LINE", mapOf("line" to targetLineIndex, "lineNumber" to seg.lineNumber))
        }
    }

    private fun updateModalStateFromSegment(seg: GCodeSegment) {
        val text = seg.rawText.uppercase(Locale.ROOT)
        var cur = _modalState.value

        // Motion group 1
        when {
            text.contains("G00") || text.contains("G0 ") -> cur = cur.copy(motionMode = "G0 (RAPID)")
            text.contains("G01") || text.contains("G1 ") -> cur = cur.copy(motionMode = "G1 (FEED)")
            text.contains("G02") || text.contains("G2 ") -> cur = cur.copy(motionMode = "G2 (CW ARC)")
            text.contains("G03") || text.contains("G3 ") -> cur = cur.copy(motionMode = "G3 (CCW ARC)")
            text.contains("G38.2") -> cur = cur.copy(motionMode = "G38.2 (PROBE)")
        }

        // Plane select
        when {
            text.contains("G17") -> cur = cur.copy(planeSelect = "G17 (XY)")
            text.contains("G18") -> cur = cur.copy(planeSelect = "G18 (XZ)")
            text.contains("G19") -> cur = cur.copy(planeSelect = "G19 (YZ)")
        }

        // Distance mode
        when {
            text.contains("G90") && !text.contains("G90.1") -> cur = cur.copy(distanceMode = "G90 (ABS)")
            text.contains("G91") && !text.contains("G91.1") -> cur = cur.copy(distanceMode = "G91 (INC)")
        }

        // Units
        when {
            text.contains("G20") -> cur = cur.copy(unitsMode = "G20 (INCH)")
            text.contains("G21") -> cur = cur.copy(unitsMode = "G21 (MM)")
        }

        // Cutter comp
        when {
            text.contains("G40") -> cur = cur.copy(cutterRadiusComp = "G40 (OFF)")
            text.contains("G41") -> cur = cur.copy(cutterRadiusComp = "G41 (LEFT)")
            text.contains("G42") -> cur = cur.copy(cutterRadiusComp = "G42 (RIGHT)")
        }

        // Tool length comp
        when {
            text.contains("G43") -> {
                val activeT = _activeTool.value
                cur = cur.copy(toolLengthComp = "G43 (ON)", toolLengthZOffsetMm = activeT.lengthOffset)
            }
            text.contains("G49") -> cur = cur.copy(toolLengthComp = "G49 (OFF)", toolLengthZOffsetMm = 0.0)
        }

        // Spindle
        when {
            text.contains("M3") || text.contains("M03") -> cur = cur.copy(spindleMode = "M3 (CW)")
            text.contains("M4") || text.contains("M04") -> cur = cur.copy(spindleMode = "M4 (CCW)")
            text.contains("M5") || text.contains("M05") -> cur = cur.copy(spindleMode = "M5 (STOP)")
        }

        // Coolant
        when {
            text.contains("M7") || text.contains("M07") -> cur = cur.copy(coolantMode = "M7 (MIST)")
            text.contains("M8") || text.contains("M08") -> cur = cur.copy(coolantMode = "M8 (FLOOD)")
            text.contains("M9") || text.contains("M09") -> cur = cur.copy(coolantMode = "M9 (OFF)")
        }

        _modalState.value = cur.copy(
            activeWcs = _currentCoordSystem.value,
            activeToolNumber = _activeTool.value.id
        )
    }
}
