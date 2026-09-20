package com.example.viewmodel

import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.R
import com.example.data.local.CncAppDatabase
import com.example.data.local.MachineProfileEntity
import com.example.data.local.MdiHistoryEntity
import com.example.data.local.MdiMacroEntity
import com.example.data.local.WcsOffsetEntity
import com.example.model.*
import com.example.service.CncFeedbackManager
import com.example.service.CncSecurityScanner
import com.example.service.ConnectivityObserver
import com.example.service.LinuxCncEngine
import com.example.service.LinuxCncMachineConfig
import com.example.service.NetworkConnectivityObserver
import java.util.Locale
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CncViewModel(application: Application, private val savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        engine.logEvent(LogSeverity.ERROR, "RUNTIME", "Unhandled Error: ${throwable.localizedMessage}")
    }

    val engine = LinuxCncEngine()
    val feedbackManager = CncFeedbackManager(application)
    val securityScanner = CncSecurityScanner()
    private val connectivityObserver = NetworkConnectivityObserver(application)

    private val db = Room.databaseBuilder(
        application,
        CncAppDatabase::class.java,
        "linuxcnc_hmi.db",
    ).fallbackToDestructiveMigration(dropAllTables = true).build()

    // State flows from engine
    val machineState: StateFlow<MachineStateEnum> = engine.machineState
    val taskMode: StateFlow<TaskMode> = engine.taskMode
    val currentCoordSystem: StateFlow<String> = engine.currentCoordSystem
    val axes: StateFlow<Map<String, AxisCoord>> = engine.axes
    val spindle: StateFlow<SpindleInfo> = engine.spindle
    val feed: StateFlow<FeedInfo> = engine.feed
    val coolant: StateFlow<CoolantInfo> = engine.coolant
    val probe: StateFlow<ProbeInfo> = engine.probe
    val tool: StateFlow<ToolInfo> = engine.tool
    val etherCatMaster: StateFlow<EtherCatMasterInfo> = engine.etherCatMaster
    val etherCatSlaves: StateFlow<List<EtherCatSlaveInfo>> = engine.etherCatSlaves
    val capabilities: StateFlow<CapabilitiesManifest> = engine.capabilities
    val activeGCodeLine: StateFlow<Int> = engine.activeGCodeLine
    val loadedGCode: StateFlow<List<GCodeSegment>> = engine.loadedGCode
    val loadedFileName: StateFlow<String> = engine.loadedFileName
    val isSimulatedMode: StateFlow<Boolean> = engine.isSimulatedMode
    val eventLogs: StateFlow<List<CncEventLog>> = engine.eventLogs
    val networkLatencyMs: StateFlow<Int> = engine.networkLatencyMs
    val toolTable: StateFlow<List<CncToolItem>> = engine.toolTable
    val activeTool: StateFlow<CncToolItem> = engine.activeTool
    val cycleElapsedSeconds: StateFlow<Long> = engine.cycleElapsedSeconds
    val cycleEstimatedTotalSeconds: StateFlow<Long> = engine.cycleEstimatedTotalSeconds

    // --- Battery Monitoring (no permission required — sticky broadcast) ---
    private val _batteryLevelPct = MutableStateFlow(100)
    val batteryLevelPct: StateFlow<Int> = _batteryLevelPct.asStateFlow()

    private val _isCharging = MutableStateFlow(false)
    val isCharging: StateFlow<Boolean> = _isCharging.asStateFlow()

    private val _batterySafety = MutableStateFlow(BatterySafetyState())
    val batterySafety: StateFlow<BatterySafetyState> = _batterySafety.asStateFlow()

    // Screen Sleep / Wake Lock policy
    val screenTimeoutPolicy: MutableStateFlow<ScreenTimeoutPolicy> =
        MutableStateFlow(ScreenTimeoutPolicy.ALWAYS_ON)

    /**
     * Controls whether the Android window flag FLAG_KEEP_SCREEN_ON is active.
     * ALWAYS_ON keeps the screen on at all times while the app is foregrounded.
     * MACHINE_ACTIVE keeps the screen on whenever the machine is ON, RUNNING, HOMING, or PAUSED.
     * SYSTEM_TIMEOUT allows Android system screen sleep to trigger normally.
     */
    val keepScreenOn: StateFlow<Boolean> = combine(
        engine.machineState,
        screenTimeoutPolicy,
    ) { state, policy ->
        when (policy) {
            ScreenTimeoutPolicy.ALWAYS_ON -> true
            ScreenTimeoutPolicy.MACHINE_ACTIVE ->
                state == MachineStateEnum.RUNNING ||
                state == MachineStateEnum.HOMING ||
                state == MachineStateEnum.PAUSED ||
                state == MachineStateEnum.ON ||
                state == MachineStateEnum.IDLE
            ScreenTimeoutPolicy.SYSTEM_TIMEOUT -> false
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val connectionTelemetry: StateFlow<ConnectionTelemetry> = engine.connectionTelemetry

    // Jog style: BUTTON_PAD vs VIRTUAL_MPG
    private val _jogStyle = MutableStateFlow(JogControlStyle.BUTTON_PAD)
    val jogStyle: StateFlow<JogControlStyle> = _jogStyle.asStateFlow()

    // MPG selected axis & multiplier
    private val _mpgAxis = MutableStateFlow("X")
    val mpgAxis: StateFlow<String> = _mpgAxis.asStateFlow()

    private val _mpgMultiplier = MutableStateFlow(MpgMultiplier.X100)
    val mpgMultiplier: StateFlow<MpgMultiplier> = _mpgMultiplier.asStateFlow()

    // User Role
    private val _userRole = MutableStateFlow(UserRole.OPERATOR)
    val userRole: StateFlow<UserRole> = _userRole.asStateFlow()

    // Active Unit System: METRIC (G21 / mm) vs IMPERIAL (G20 / inch)
    private val _unitSystem = MutableStateFlow(
        savedStateHandle.get<String>("unit_system")?.let { runCatching { UnitSystem.valueOf(it) }.getOrNull() } ?: UnitSystem.METRIC
    )
    val unitSystem: StateFlow<UnitSystem> = _unitSystem.asStateFlow()

    // Selected Jog increment step
    private val _jogStep = MutableStateFlow(
        savedStateHandle.get<Double>("jog_step") ?: 1.0
    ) // 1.0mm or 0.1in default
    val jogStep: StateFlow<Double> = _jogStep.asStateFlow()

    // Jog mode: Continuous vs Step
    private val _isContinuousJog = MutableStateFlow(
        savedStateHandle.get<Boolean>("is_continuous_jog") ?: true
    )
    val isContinuousJog: StateFlow<Boolean> = _isContinuousJog.asStateFlow()

    // Jog Speed slider
    private val _jogSpeedMmMin = MutableStateFlow(
        savedStateHandle.get<Double>("jog_speed") ?: 1500.0
    )
    val jogSpeedMmMin: StateFlow<Double> = _jogSpeedMmMin.asStateFlow()

    // MDI Input
    private val _mdiCommandText = MutableStateFlow("")
    val mdiCommandText: StateFlow<String> = _mdiCommandText.asStateFlow()

    private val _mdiHistory = MutableStateFlow(
        listOf("G0 X0 Y0 Z10", "G1 Z-5 F300", "M3 S12000", "G54", "G28", "T1 M6"),
    )
    val mdiHistory: StateFlow<List<String>> = _mdiHistory.asStateFlow()

    // Navigation and UI States (Saved in SavedStateHandle for process death survival)
    val selectedTab: StateFlow<CncNavigationTab> = savedStateHandle.getStateFlow("selected_tab", CncNavigationTab.CONTROL)
    
    // Pro Dialog States
    val showCyberScanDialog: MutableStateFlow<Boolean> = MutableStateFlow(value = false)
    val showCalculatorDialog: MutableStateFlow<Boolean> = MutableStateFlow(value = false)
    val showToolTableDialog: MutableStateFlow<Boolean> = MutableStateFlow(value = false)
    val showCalibrationDialog: MutableStateFlow<Boolean> = MutableStateFlow(value = false)
    val showManualDialog: MutableStateFlow<Boolean> = MutableStateFlow(value = false)
    val showWcsTableDialog: MutableStateFlow<Boolean> = MutableStateFlow(value = false)
    val showHalMonitorDialog: MutableStateFlow<Boolean> = MutableStateFlow(value = false)
    val showSoftLimitsDialog: MutableStateFlow<Boolean> = MutableStateFlow(value = false)
    val showRunFromLineDialog: MutableStateFlow<Boolean> = MutableStateFlow(value = false)
    val showIniDialog: MutableStateFlow<Boolean> = MutableStateFlow(value = false)
    val showConversationalCamDialog: MutableStateFlow<Boolean> = MutableStateFlow(value = false)
    val showConnectionWizardDialog: MutableStateFlow<Boolean> = MutableStateFlow(value = false)

    // Tutorial preference: whether the tutorial has been permanently skipped
    private val _isWizardPermanentlyDismissed = MutableStateFlow(
        savedStateHandle.get<Boolean>("wizard_permanently_dismissed") ?: false
    )
    val isWizardPermanentlyDismissed: StateFlow<Boolean> = _isWizardPermanentlyDismissed.asStateFlow()

    // LinuxCNC Real Protocol & Telemetry
    val connectionConfig: StateFlow<LinuxCncConnectionConfig> = engine.connectionConfig
    val serverTelemetry: StateFlow<LinuxCncServerTelemetry> = engine.serverTelemetry

    // PRO MODULE 1: WCS Offsets
    val wcsOffsets: StateFlow<Map<String, WcsOffset>> = engine.wcsOffsets

    // PRO MODULE 5: Active Modal G-Code State & Execution Modifiers
    val modalState: StateFlow<ModalGCodeState> = engine.modalState
    val executionModifiers: StateFlow<ExecutionModifiers> = engine.executionModifiers

    // PRO MODULE 2: MDI Real-time validation and Database History
    val mdiValidationLive: StateFlow<MdiValidationResult> = _mdiCommandText
        .map { text -> engine.mdiValidator.validate(text) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MdiValidationResult(isValid = false, errorMessage = "Empty"))

    val mdiHistoryEntities: StateFlow<List<MdiHistoryEntity>> = db.mdiHistoryDao().getRecentHistory(30)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mdiFavorites: StateFlow<List<MdiHistoryEntity>> = db.mdiHistoryDao().getFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // PRO MODULE 3: HAL Signal Monitor
    val halPins: StateFlow<List<HalPin>> = engine.halPins

    // PRO MODULE 4: Soft Limits Pre-Check
    val softLimitsCheck: StateFlow<SoftLimitsCheckResult> = combine(
        engine.loadedGCode,
        engine.currentCoordSystem,
        engine.wcsOffsets,
        engine.axes
    ) { gcode, wcsName, offsets, axes ->
        val activeOffset = offsets[wcsName] ?: WcsOffset(name = wcsName, pIndex = 1)
        engine.softLimitsPreChecker.checkProgramLimits(gcode, activeOffset, axes)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SoftLimitsCheckResult(
            isWithinLimits = true,
            activeWcs = "G54",
            boundingBoxWork = GCodeBoundingBox(AxisRange(0.0, 0.0), AxisRange(0.0, 0.0), AxisRange(0.0, 0.0)),
            boundingBoxMachine = GCodeBoundingBox(AxisRange(0.0, 0.0), AxisRange(0.0, 0.0), AxisRange(0.0, 0.0))
        )
    )

    // Persistent Profiles and Macros from DB
    val machineProfiles: StateFlow<List<MachineProfileEntity>> = db.profileDao().getAllProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val macros: StateFlow<List<MdiMacroEntity>> = db.macroDao().getAllMacros()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        seedInitialData()
        observeEngineStates()
        observeConnectivity()
        readBatteryStatus()
    }

    /** Reads the sticky battery broadcast once at startup to populate initial state. */
    private fun readBatteryStatus() {
        try {
            val ctx = getApplication<Application>()
            val intent: Intent? = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            intent?.let { updateBatteryState(it) }
        } catch (_: Exception) {}
    }

    /** Called from MainActivity whenever the system sends a battery-changed broadcast. */
    fun onBatteryChanged(intent: Intent) {
        updateBatteryState(intent)
    }

    private fun updateBatteryState(intent: Intent) {
        if (_batterySafety.value.isSimulated) return
        val level  = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale  = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)

        val pct = if (scale > 0) (level * 100 / scale) else 0
        val isChargingNow = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        applyBatteryLevel(pct, isChargingNow, isSimulated = false)
    }

    fun applyBatteryLevel(pct: Int, isChargingNow: Boolean, isSimulated: Boolean = false) {
        val prevLow = _batterySafety.value.isLowBattery
        val prevCritical = _batterySafety.value.isCriticalBattery
        val isLow = pct <= 20 && !isChargingNow
        val isCritical = pct <= 10 && !isChargingNow

        _batteryLevelPct.value = pct
        _isCharging.value = isChargingNow
        _batterySafety.value = BatterySafetyState(
            levelPct = pct,
            isCharging = isChargingNow,
            isLowBattery = isLow,
            isCriticalBattery = isCritical,
            isSimulated = isSimulated,
        )

        // Critical alarm threshold (<= 10%)
        if (!prevCritical && isCritical) {
            val isRunning = engine.machineState.value == MachineStateEnum.RUNNING
            val message = if (isRunning) {
                "¡PELIGRO CRÍTICO! Batería al $pct% durante ciclo de mecanizado. Apagado inminente: Pause ciclo o conecte cargador."
            } else {
                "¡ALARMA CRÍTICA DE BATERÍA! Nivel al $pct%. Conecte el cargador de inmediato."
            }
            engine.logEvent(LogSeverity.CRITICAL, "BATERÍA", message)
            feedbackManager.playErrorAlarm()
            feedbackManager.triggerEstopHaptic()
        } else if (!prevLow && isLow) {
            // Low battery warning threshold (<= 20%)
            engine.logEvent(LogSeverity.WARNING, "BATERÍA", "Alarma de batería baja: $pct%. Conecte cargador al dispositivo.")
            feedbackManager.playLowBatteryAlert()
        }
    }

    private val _isWeakSignalDismissed = MutableStateFlow(false)
    val isWeakSignalDismissed: StateFlow<Boolean> = _isWeakSignalDismissed.asStateFlow()

    fun dismissWeakSignalAlert() {
        feedbackManager.triggerActionClick()
        _isWeakSignalDismissed.value = true
    }

    private val _isBatteryAlertDismissed = MutableStateFlow(false)
    val isBatteryAlertDismissed: StateFlow<Boolean> = _isBatteryAlertDismissed.asStateFlow()

    fun dismissBatteryAlert() {
        feedbackManager.triggerActionClick()
        _isBatteryAlertDismissed.value = true
    }

    fun setEngineSimulatedMode(enabled: Boolean) {
        feedbackManager.triggerActionClick()
        engine.setSimulatedMode(enabled)
    }

    fun simulateBatteryLevel(pct: Int, isCharging: Boolean = false) {
        applyBatteryLevel(pct, isCharging, isSimulated = true)
        feedbackManager.triggerActionClick()
    }

    fun setBatteryChargingSimulation(isCharging: Boolean) {
        feedbackManager.triggerActionClick()
        val currentLevel = _batterySafety.value.levelPct
        applyBatteryLevel(currentLevel, isCharging, isSimulated = true)
    }

    fun restoreBatterySensor() {
        _batterySafety.value = _batterySafety.value.copy(isSimulated = false)
        readBatteryStatus()
        feedbackManager.triggerActionClick()
        engine.logEvent(LogSeverity.INFO, "BATERÍA", "Sensor de batería restaurado a lecturas reales del sistema.")
    }

    fun setScreenTimeoutPolicy(policy: ScreenTimeoutPolicy) {
        screenTimeoutPolicy.value = policy
        feedbackManager.triggerActionClick()
        if (policy == ScreenTimeoutPolicy.SYSTEM_TIMEOUT) {
            engine.logEvent(
                LogSeverity.WARNING,
                "PANTALLA",
                "Aviso de seguridad: Apagado de pantalla según sistema activado. Se recomienda 'Pantalla Siempre Encendida'.",
            )
        } else {
            engine.logEvent(
                LogSeverity.INFO,
                "PANTALLA",
                "Política de pantalla actualizada a: ${policy.name} (Wake Lock activo).",
            )
        }
    }

    fun retryConnectionNow() {
        feedbackManager.triggerActionClick()
        engine.reconnectNow()
    }

    fun simulateWeakSignal(enable: Boolean) {
        feedbackManager.triggerActionClick()
        _isWeakSignalDismissed.value = false
        engine.setWeakSignalSimulation(enable)
    }

    fun simulateConnectionLoss() {
        feedbackManager.triggerActionClick()
        engine.simulateConnectionLoss()
    }

    fun restoreConnectionSimulation() {
        feedbackManager.triggerActionClick()
        _isWeakSignalDismissed.value = false
        engine.restoreConnectionSimulation()
    }

    fun simulateTouchProbe() {
        feedbackManager.triggerActionClick()
        feedbackManager.triggerSuccessHaptic()
        engine.simulateProbeTouch()
    }

    fun injectSimulatedFault(faultType: SimulatedFaultType) {
        when (faultType) {
            SimulatedFaultType.SERVO_OVERTORQUE -> {
                feedbackManager.triggerWarningHaptic()
                feedbackManager.playErrorAlarm()
                engine.logEvent(LogSeverity.ERROR, "ETHERCAT", "ALARMA AL.006: Sobrepar de protección disparado en Servo Eje Z")
            }
            SimulatedFaultType.LIMIT_SWITCH_X -> {
                feedbackManager.triggerEstopHaptic()
                feedbackManager.playErrorAlarm()
                engine.logEvent(LogSeverity.CRITICAL, "LIMIT", "FINAL DE CARRERA DISPARADO: Interruptor hardware Eje X+ activado!")
            }
            SimulatedFaultType.SPINDLE_THERMAL -> {
                feedbackManager.triggerWarningHaptic()
                feedbackManager.playErrorAlarm()
                engine.logEvent(LogSeverity.ERROR, "SPINDLE", "FALLO TÉRMICO VFD: Temperatura de devanado de husillo > 85°C. Parada preventiva.")
            }
            SimulatedFaultType.DOOR_INTERLOCK -> {
                feedbackManager.triggerWarningHaptic()
                feedbackManager.playErrorAlarm()
                engine.logEvent(LogSeverity.WARNING, "SAFETY", "ENCLAVAMIENTO DE SEGURIDAD: Puerta de cabina abierta durante ciclo activo.")
            }
            SimulatedFaultType.LOW_COOLANT -> {
                feedbackManager.triggerWarningHaptic()
                feedbackManager.playWarningBeep()
                engine.logEvent(LogSeverity.WARNING, "COOLANT", "NIVEL DE REFRIGERANTE BAJO: Presión de bomba insuficiente (0.4 bar).")
            }
        }
    }

    private fun observeConnectivity() {
        connectivityObserver.observe()
            .onEach { status ->
                when (status) {
                    ConnectivityObserver.Status.Available -> {
                        engine.logEvent(LogSeverity.INFO, "NETWORK", "Enlace de red disponible")
                        if (!engine.connectionTelemetry.value.isConnected) {
                            engine.reconnectNow()
                        }
                    }
                    ConnectivityObserver.Status.Weak -> {
                        engine.logEvent(
                            LogSeverity.WARNING,
                            "NETWORK",
                            "Alarma de red: Señal Wi-Fi débil o degradada. Riesgo de caída de telemetría.",
                        )
                        feedbackManager.playWarningBeep()
                        feedbackManager.triggerWarningHaptic()
                        engine.setWeakSignalSimulation(true)
                    }
                    ConnectivityObserver.Status.Losing -> {
                        engine.logEvent(
                            LogSeverity.WARNING,
                            "NETWORK",
                            "Enlace de red inestable (Losing). Reintentando paquetes...",
                        )
                        feedbackManager.playWarningBeep()
                    }
                    ConnectivityObserver.Status.Lost, ConnectivityObserver.Status.Unavailable -> {
                        engine.logEvent(
                            LogSeverity.ERROR,
                            "NETWORK",
                            "Conexión de red perdida. Activando protocolo de reconexión automática...",
                        )
                        feedbackManager.playErrorAlarm()
                        feedbackManager.triggerEstopHaptic()
                        engine.handleDisconnect("Pérdida de conectividad de red del dispositivo")
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observeEngineStates() {
        engine.machineState
            .onEach { state ->
                when (state) {
                 MachineStateEnum.ERROR -> feedbackManager.playErrorAlarm()
                    MachineStateEnum.IDLE -> {
                        // Logic to detect actual program completion in simulation
                        if ((engine.activeGCodeLine.value > 0) && engine.loadedGCode.value.isNotEmpty()) {
                            if (engine.activeGCodeLine.value >= (engine.loadedGCode.value.size - 1)) {
                                feedbackManager.playCycleCompleteSound()
                                feedbackManager.playCycleCompleteHaptic()
                            }
                        }
                    }
                    else -> {}
                }
            }
            .launchIn(viewModelScope)

        engine.probe
            .map { it.isTripped }
            .distinctUntilChanged()
            .filter { it }
            .onEach { feedbackManager.playProbeTripSound() }
            .launchIn(viewModelScope)

        // Tutorial trigger: When LinuxCNC connects for the first time, offer the wizard unless permanently skipped
        engine.serverTelemetry
            .map { it.isConnected }
            .distinctUntilChanged()
            .filter { it }
            .onEach { isConnected ->
                if (isConnected && !_isWizardPermanentlyDismissed.value) {
                    showConnectionWizardDialog.value = true
                }
            }
            .launchIn(viewModelScope)
    }

    private fun seedInitialData() {
        viewModelScope.launch(exceptionHandler) {
            val context = getApplication<Application>()
            // Seed sample profiles if empty
            val initialProfiles = listOf(
                MachineProfileEntity(name = context.getString(R.string.profile_workshop_vmc), hostIp = "192.168.1.100", architecture = "ETHERCAT_DELTA", isDefault = true),
                MachineProfileEntity(name = context.getString(R.string.profile_prototype_router), hostIp = "10.42.0.1", architecture = "MESA_FPGA"),
                MachineProfileEntity(name = context.getString(R.string.profile_mini_mill), hostIp = "192.168.1.150", architecture = "PARPORT_LEGACY"),
            )
            initialProfiles.forEach { db.profileDao().insertProfile(it) }

            val initialMacros = listOf(
                MdiMacroEntity("m1", context.getString(R.string.macro_zero_all_label), "G10 L20 P1 X0 Y0 Z0", context.getString(R.string.macro_zero_all_desc), "SETUP"),
                MdiMacroEntity("m2", context.getString(R.string.macro_park_label), "G0 G53 Z0\nG0 G53 X0 Y300", context.getString(R.string.macro_park_desc), "MOTION"),
                MdiMacroEntity("m3", context.getString(R.string.macro_warmup_label), "M3 S3000\nG4 P5\nM3 S8000\nG4 P5\nM3 S15000", context.getString(R.string.macro_warmup_desc), "SPINDLE"),
                MdiMacroEntity("m4", context.getString(R.string.macro_laser_label), "M64 P0", context.getString(R.string.macro_laser_desc), "TOOLING"),
                MdiMacroEntity("m5", context.getString(R.string.macro_probe_z_label), "G38.2 Z-50 F100\nG91 G0 Z2\nG90", context.getString(R.string.macro_probe_z_desc), "PROBING"),
            )
            db.macroDao().insertMacros(initialMacros)

            // Seed default WCS offsets if not present
            val defaultWcs = listOf(
                WcsOffsetEntity("G54", 1, 100.0, 70.0, -30.0, 0.0, comment = "Vise 1 - Front Left Jaw"),
                WcsOffsetEntity("G55", 2, 250.0, 70.0, -30.0, 0.0, comment = "Vise 2 - Center Plate"),
                WcsOffsetEntity("G56", 3, 400.0, 70.0, -30.0, 0.0, comment = "Vise 3 - 4th Axis Chuck"),
                WcsOffsetEntity("G57", 4, 0.0, 0.0, 0.0, 0.0, comment = "Fixture 4"),
                WcsOffsetEntity("G58", 5, 0.0, 0.0, 0.0, 0.0, comment = "Fixture 5"),
                WcsOffsetEntity("G59", 6, 0.0, 0.0, 0.0, 0.0, comment = "Fixture 6"),
                WcsOffsetEntity("G59.1", 7, 0.0, 0.0, 0.0, 0.0, comment = "Auxiliary 1"),
                WcsOffsetEntity("G59.2", 8, 0.0, 0.0, 0.0, 0.0, comment = "Auxiliary 2"),
                WcsOffsetEntity("G59.3", 9, 0.0, 0.0, 0.0, 0.0, comment = "Toolsetter Reference")
            )
            db.wcsOffsetDao().insertAll(defaultWcs)

            // Listen to DB WCS offsets and sync into engine
            db.wcsOffsetDao().getAllOffsets().collect { list ->
                if (list.isNotEmpty()) {
                    val domainList = list.map {
                        WcsOffset(
                            name = it.name,
                            pIndex = it.pIndex,
                            x = it.x,
                            y = it.y,
                            z = it.z,
                            a = it.a,
                            comment = it.comment
                        )
                    }
                    engine.syncWcsOffsetsFromDatabase(domainList)
                }
            }
        }
    }

    // Role switching
    fun setUserRole(role: UserRole) {
        _userRole.value = role
        feedbackManager.triggerActionClick()
        engine.logEvent(LogSeverity.INFO, "AUTH", "Active security level switched to ${role.name}")
    }

    // Unit System switching (G21 MM <-> G20 INCH)
    fun toggleUnitSystem() {
        val newUnit = if (_unitSystem.value == UnitSystem.METRIC) UnitSystem.IMPERIAL else UnitSystem.METRIC
        setUnitSystem(newUnit)
        feedbackManager.triggerSuccessHaptic()
    }

    fun setUnitSystem(unit: UnitSystem) {
        _unitSystem.value = unit
        val newStep = if (unit == UnitSystem.IMPERIAL) 0.010 else 1.000
        _jogStep.value = newStep
        savedStateHandle["unit_system"] = unit.name
        savedStateHandle["jog_step"] = newStep
        feedbackManager.triggerActionClick()
        engine.logEvent(
            LogSeverity.INFO,
            "MODAL",
            "Active Unit System set to ${unit.code} (${unit.shortLabel} / ${unit.lengthUnit})",
        )
    }

    // Wrapped actions with Haptics and Audio
    fun toggleEstop() {
        feedbackManager.triggerEstopHaptic()
        if (engine.machineState.value != MachineStateEnum.ESTOP) {
            feedbackManager.playEstopSound()
        }
        engine.toggleEstop()
    }

    fun powerOn() {
        feedbackManager.triggerActionClick()
        engine.powerOn()
    }

    fun powerOff() {
        feedbackManager.triggerActionClick()
        engine.powerOff()
    }

    fun stepJog(axis: String, direction: Int) {
        feedbackManager.triggerJogTick()
        val stepInMm = unitSystem.value.toMm(_jogStep.value)
        engine.stepJog(axis, direction, stepInMm)
    }

    fun startJog(axis: String, direction: Int) {
        feedbackManager.triggerActionClick()
        engine.startJog(axis, direction, _jogSpeedMmMin.value)
    }

    fun stopJog() {
        engine.stopJog()
    }

    fun zeroAxis(axis: String) {
        feedbackManager.triggerSuccessHaptic()
        engine.zeroAxis(axis)
    }

    fun setAxisWorkPosition(axis: String, position: Double) {
        feedbackManager.triggerSuccessHaptic()
        engine.setAxisWorkPosition(axis, position)
    }

    fun setWorkOriginWithCameraOffset(offsetX: Double, offsetY: Double) {
        feedbackManager.triggerSuccessHaptic()
        engine.setAxisWorkPosition("X", -offsetX)
        engine.setAxisWorkPosition("Y", -offsetY)
    }

    fun zeroAllAxes() {
        feedbackManager.triggerSuccessHaptic()
        engine.zeroAllAxes()
    }

    fun homeAxis(axis: String) {
        feedbackManager.triggerActionClick()
        engine.homeAxis(axis)
    }

    fun homeAllAxes() {
        feedbackManager.triggerActionClick()
        engine.homeAllAxes()
    }

    fun cycleStart(forceOverrideLimits: Boolean = false) {
        val limits = softLimitsCheck.value
        if (!limits.isWithinLimits && !forceOverrideLimits) {
            feedbackManager.triggerWarningHaptic()
            feedbackManager.playWarningBeep()
            showSoftLimitsDialog.value = true
            engine.logEvent(
                LogSeverity.WARNING,
                "SAFETY_LOCK",
                "Cycle Start blocked: Program exceeds soft limits on axis ${limits.violations.firstOrNull()?.axis}. Resolve or override."
            )
            return
        }
        feedbackManager.triggerActionClick()
        engine.cycleStart()
    }

    fun feedHold() {
        feedbackManager.triggerWarningHaptic()
        feedbackManager.playWarningBeep()
        engine.feedHold()
    }

    fun cycleStop() {
        feedbackManager.triggerActionClick()
        engine.cycleStop()
    }

    fun setSingleBlockMode(enabled: Boolean) {
        feedbackManager.triggerActionClick()
        engine.setSingleBlockMode(enabled)
    }

    fun setOptionalStop(enabled: Boolean) {
        feedbackManager.triggerActionClick()
        engine.setOptionalStop(enabled)
    }

    fun setBlockDelete(enabled: Boolean) {
        feedbackManager.triggerActionClick()
        engine.setBlockDelete(enabled)
    }

    fun singleBlockStep() {
        feedbackManager.triggerActionClick()
        engine.singleBlockStep()
    }

    fun runFromLine(targetIndex: Int) {
        feedbackManager.triggerActionClick()
        engine.runFromLine(targetIndex)
    }

    fun triggerProbe(routine: String) {
        feedbackManager.triggerActionClick()
        engine.triggerProbeRoutine(routine)
    }

    fun applySpeedsFeeds(rpm: Double, feedMmMin: Double) {
        feedbackManager.triggerSuccessHaptic()
        engine.setSpindleRpm(rpm)
        engine.logEvent(LogSeverity.INFO, "METROLOGY", "Applied Speeds & Feeds: ${rpm.toInt()} RPM, Feed: ${feedMmMin.toInt()} mm/min")
    }

    fun loadAndScanGCode(fileName: String, content: String) {
        val result = securityScanner.scanGCode(fileName, content)
        if (result.isExecutable) {
            feedbackManager.triggerSuccessHaptic()
            engine.loadGCodeContent(fileName, content)
            val fp = if (result.sha256Fingerprint.length >= 8) result.sha256Fingerprint.substring(0, 8) else result.sha256Fingerprint
            engine.logEvent(LogSeverity.INFO, "SECURITY", "G-Code verification PASSED [${result.threatLevel.name}]. SHA-256: $fp")
        } else {
            feedbackManager.triggerEstopHaptic()
            feedbackManager.playErrorAlarm()
            engine.logEvent(LogSeverity.CRITICAL, "SECURITY", "THREAT BLOCKED: Program '$fileName' contains unauthorized exploit payloads!")
        }
    }

    fun simulateDiagnosticAlarm() {
        feedbackManager.triggerWarningHaptic()
        feedbackManager.playErrorAlarm()
        engine.logEvent(LogSeverity.ERROR, "ETHERCAT", "ALARM AL.006 (Over-Torque Protection Tripped on Axis Z)")
    }

    fun clearLogs() {
        feedbackManager.triggerActionClick()
        engine.clearEventLogs()
    }

    fun setSelectedTab(tab: CncNavigationTab) {
        if (selectedTab.value != tab) {
            feedbackManager.triggerActionClick()
            savedStateHandle["selected_tab"] = tab
        }
    }

    fun setShowCyberScanDialog(show: Boolean) { showCyberScanDialog.value = show }
    fun setShowCalculatorDialog(show: Boolean) { showCalculatorDialog.value = show }
    fun setShowToolTableDialog(show: Boolean) { showToolTableDialog.value = show }
    fun setShowCalibrationDialog(show: Boolean) { showCalibrationDialog.value = show }
    fun setShowManualDialog(show: Boolean) { showManualDialog.value = show }
    fun setShowConnectionWizardDialog(show: Boolean) { showConnectionWizardDialog.value = show }

    fun skipConnectionWizard(dontShowAgain: Boolean) {
        feedbackManager.triggerActionClick()
        if (dontShowAgain) {
            _isWizardPermanentlyDismissed.value = true
            savedStateHandle["wizard_permanently_dismissed"] = true
        }
        showConnectionWizardDialog.value = false
    }

    fun connectFromWizard(config: LinuxCncConnectionConfig) {
        feedbackManager.triggerSuccessHaptic()
        connectLinuxCnc(config)
    }

    fun setJogStyle(style: JogControlStyle) {
        feedbackManager.triggerActionClick()
        _jogStyle.value = style
    }

    fun setMpgAxis(axis: String) {
        feedbackManager.triggerActionClick()
        _mpgAxis.value = axis
    }

    fun setMpgMultiplier(mult: MpgMultiplier) {
        feedbackManager.triggerActionClick()
        _mpgMultiplier.value = mult
    }

    fun sendMpgStep(axis: String, direction: Int, multiplier: MpgMultiplier) {
        feedbackManager.triggerJogTick()
        val stepMm = multiplier.getStep(_unitSystem.value)
        engine.stepJog(axis, direction, stepMm)
    }

    fun mountTool(toolId: Int) {
        feedbackManager.triggerSuccessHaptic()
        engine.mountTool(toolId)
    }

    fun updateTool(tool: CncToolItem) {
        feedbackManager.triggerSuccessHaptic()
        engine.updateToolItem(tool)
    }

    fun deleteTool(toolId: Int) {
        feedbackManager.triggerActionClick()
        engine.deleteTool(toolId)
    }

    fun touchOffToolZ(toolId: Int) {
        feedbackManager.triggerSuccessHaptic()
        val currentZ = engine.axes.value["Z"]?.workPos ?: 0.0
        val zDisplay = _unitSystem.value.toDisplayLength(currentZ)
        engine.logEvent(LogSeverity.INFO, "TOUCH_OFF", "T$toolId Z-Pos: ${_unitSystem.value.formatPosition(currentZ)} (${String.format(Locale.US, "%.4f", zDisplay)} ${_unitSystem.value.lengthUnit})")
        engine.touchOffToolZ(toolId, currentZ)
    }

    fun setJogStep(step: Double) {
        feedbackManager.triggerJogTick()
        _jogStep.value = step
        savedStateHandle["jog_step"] = step
    }

    fun setJogMode(continuous: Boolean) {
        feedbackManager.triggerActionClick()
        _isContinuousJog.value = continuous
        savedStateHandle["is_continuous_jog"] = continuous
    }

    fun setJogSpeed(speed: Double) {
        _jogSpeedMmMin.value = speed
        savedStateHandle["jog_speed"] = speed
    }

    fun setMdiText(text: String) {
        _mdiCommandText.value = text
    }

    fun executeMdiCommand(cmd: String = _mdiCommandText.value) {
        val trimmed = cmd.trim()
        if (trimmed.isNotBlank()) {
            feedbackManager.triggerActionClick()
            val result = engine.executeMdiCommand(trimmed)
            viewModelScope.launch(exceptionHandler) {
                db.mdiHistoryDao().insertHistoryItem(
                    MdiHistoryEntity(
                        command = trimmed,
                        executionStatus = if (result.isSuccess) "SUCCESS" else "SYNTAX_ERROR"
                    )
                )
            }
            if (result.isSuccess) {
                val updated = _mdiHistory.value.toMutableList()
                updated.remove(trimmed)
                updated.add(0, trimmed)
                _mdiHistory.value = updated.take(25)
                _mdiCommandText.value = ""
                feedbackManager.triggerSuccessHaptic()
            } else {
                feedbackManager.triggerWarningHaptic()
                feedbackManager.playWarningBeep()
            }
        }
    }

    fun toggleMdiFavorite(id: Long, isFavorite: Boolean) {
        viewModelScope.launch(exceptionHandler) {
            db.mdiHistoryDao().setFavorite(id, isFavorite)
            feedbackManager.triggerActionClick()
        }
    }

    // PRO MODULE 1: WCS Operations
    fun setCoordinateSystem(gSystem: String) {
        feedbackManager.triggerActionClick()
        engine.setCoordinateSystem(gSystem)
    }

    fun touchOffAxis(axis: String, targetWorkValue: Double = 0.0) {
        feedbackManager.triggerSuccessHaptic()
        engine.touchOff(axis, targetWorkValue)
        viewModelScope.launch(exceptionHandler) {
            val currentWcs = engine.currentCoordSystem.value
            val offset = engine.wcsOffsets.value[currentWcs]
            if (offset != null) {
                db.wcsOffsetDao().insertOrUpdate(
                    WcsOffsetEntity(
                        name = offset.name,
                        pIndex = offset.pIndex,
                        x = offset.x,
                        y = offset.y,
                        z = offset.z,
                        a = offset.a,
                        comment = offset.comment
                    )
                )
            }
        }
    }

    fun setWcsOffset(name: String, axis: String, offsetValue: Double) {
        feedbackManager.triggerActionClick()
        engine.setWcsOffset(name, axis, offsetValue)
        viewModelScope.launch(exceptionHandler) {
            val offset = engine.wcsOffsets.value[name]
            if (offset != null) {
                db.wcsOffsetDao().insertOrUpdate(
                    WcsOffsetEntity(
                        name = offset.name,
                        pIndex = offset.pIndex,
                        x = offset.x,
                        y = offset.y,
                        z = offset.z,
                        a = offset.a,
                        comment = offset.comment
                    )
                )
            }
        }
    }

    // PRO MODULE 3: HAL Operations
    fun toggleHalPin(pinName: String) {
        feedbackManager.triggerActionClick()
        engine.toggleHalPin(pinName)
    }

    fun saveProfile(name: String, ip: String, port: Int, arch: String) {
        viewModelScope.launch(exceptionHandler) {
            db.profileDao().insertProfile(
                MachineProfileEntity(
                    name = name,
                    hostIp = ip,
                    port = port,
                    architecture = arch,
                ),
            )
            feedbackManager.triggerSuccessHaptic()
        }
    }

    fun deleteProfile(id: Long) {
        viewModelScope.launch(exceptionHandler) {
            db.profileDao().deleteProfile(id)
            feedbackManager.triggerActionClick()
        }
    }

    fun wipeAllAppData() {
        viewModelScope.launch(exceptionHandler) {
            // Delete all profiles and macros
            machineProfiles.value.forEach { db.profileDao().deleteProfile(it.id) }
            macros.value.forEach { db.macroDao().deleteMacro(it.id) }
            
            // Re-seed with default data
            seedInitialData()
            
            feedbackManager.triggerEstopHaptic()
            engine.logEvent(LogSeverity.CRITICAL, "SYSTEM", "ALL USER DATA WIPED - Defaults Restored")
        }
    }

    // --- Metrological Calibration Bridge ---
    val activeCalibrationSession: StateFlow<AxisCalibrationSession?> = engine.activeCalibrationSession

    fun startAxisCalibration(
        axis: String = "X",
        totalTravelMm: Double = 600.0,
        stepIntervalPercent: Double = 10.0,
        instrumentName: String = "Dial Indicator (0.001mm Resolution)",
        instrumentUncertaintyMm: Double = 0.003,
    ) {
        feedbackManager.triggerActionClick()
        engine.startAxisCalibration(axis, totalTravelMm, stepIntervalPercent, instrumentName, instrumentUncertaintyMm)
    }

    fun recordCalibrationMeasurement(stepIndex: Int, measuredValueMm: Double) {
        feedbackManager.triggerSuccessHaptic()
        engine.recordCalibrationMeasurement(stepIndex, measuredValueMm)
    }

    fun moveAxisToNominal(stepIndex: Int) {
        feedbackManager.triggerJogTick()
        engine.moveAxisToNominal(stepIndex)
    }

    fun generateLinuxCncCompTable(session: AxisCalibrationSession): String {
        return engine.generateLinuxCncCompTable(session)
    }

    // --- LinuxCNC Real Hardware Protocol Operations ---
    fun connectLinuxCnc(config: LinuxCncConnectionConfig) {
        feedbackManager.triggerActionClick()
        engine.connectLinuxCnc(config)
    }

    fun disconnectLinuxCnc() {
        feedbackManager.triggerActionClick()
        engine.disconnectLinuxCnc()
    }

    fun importToolTable(content: String): Int {
        feedbackManager.triggerSuccessHaptic()
        return engine.importToolTable(content)
    }

    fun exportToolTable(): String {
        feedbackManager.triggerActionClick()
        return engine.exportToolTable()
    }

    fun mountToolWithG43(toolId: Int) {
        feedbackManager.triggerSuccessHaptic()
        engine.mountToolWithG43(toolId)
    }

    fun applyIniConfig(config: LinuxCncMachineConfig) {
        feedbackManager.triggerSuccessHaptic()
        engine.applyIniConfig(config)
    }

    fun loadConversationalGCode(title: String, gcode: String) {
        feedbackManager.triggerSuccessHaptic()
        engine.loadGCodeContent(title, gcode)
    }
}
