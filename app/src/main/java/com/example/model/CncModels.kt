package com.example.model

import androidx.annotation.StringRes
import androidx.annotation.ArrayRes
import java.util.Locale

import com.example.R

enum class MachineStateEnum(@get:StringRes val displayNameRes: Int) {
    ESTOP(R.string.state_estop_disp),
    OFF(R.string.state_off_disp),
    ON(R.string.state_on_disp),
    IDLE(R.string.state_idle_disp),
    RUNNING(R.string.state_running_disp),
    PAUSED(R.string.state_paused_disp),
    HOMING(R.string.state_homing_disp),
    ERROR(R.string.state_error_disp),
}

enum class TaskMode(@get:StringRes val displayNameRes: Int) {
    MANUAL(R.string.mode_manual_disp),
    MDI(R.string.mode_mdi_disp),
    AUTO(R.string.mode_auto_disp)
}

enum class CncNavigationTab(@get:StringRes val titleRes: Int) {
    CONTROL(R.string.tab_control),
    TOOLPATH(R.string.tab_toolpath),
    CAMERA(R.string.tab_camera),
    PROBING(R.string.tab_probing),
    ETHERCAT(R.string.tab_ethercat),
    MDI(R.string.tab_mdi),
    LOGS(R.string.tab_logs),
    CONFIG(R.string.tab_config)
}

enum class HardwareArchitecture(
    @get:StringRes val displayNameRes: Int,
    val level: Int,
    @get:StringRes val descriptionRes: Int,
) {
    ETHERCAT_DELTA(R.string.arch_ethercat_name, 3, R.string.arch_ethercat_desc),
    MESA_FPGA(R.string.arch_mesa_name, 2, R.string.arch_mesa_desc),
    PARPORT_LEGACY(R.string.arch_parport_name, 1, R.string.arch_parport_desc),
    STEP_DIR_CLOSED_LOOP(R.string.arch_closed_loop_name, 2, R.string.arch_closed_loop_desc)
}

enum class UserRole(@get:StringRes val displayNameRes: Int) {
    OPERATOR(R.string.role_operator_disp),
    VIEWER(R.string.role_viewer_disp),
    ADMIN(R.string.role_admin_disp)
}

enum class ScreenTimeoutPolicy(
    @get:StringRes val displayNameRes: Int,
    @get:StringRes val descriptionRes: Int,
) {
    ALWAYS_ON(R.string.screen_policy_always_on, R.string.screen_policy_always_on_desc),
    MACHINE_ACTIVE(R.string.screen_policy_machine_active, R.string.screen_policy_machine_active_desc),
    SYSTEM_TIMEOUT(R.string.screen_policy_system_timeout, R.string.screen_policy_system_timeout_desc)
}

enum class SimulatedFaultType(
    @get:StringRes val displayNameRes: Int,
    @get:StringRes val descriptionRes: Int,
) {
    SERVO_OVERTORQUE(R.string.fault_servo_name, R.string.fault_servo_desc),
    LIMIT_SWITCH_X(R.string.fault_limit_name, R.string.fault_limit_desc),
    SPINDLE_THERMAL(R.string.fault_thermal_name, R.string.fault_thermal_desc),
    DOOR_INTERLOCK(R.string.fault_interlock_name, R.string.fault_interlock_desc),
    LOW_COOLANT(R.string.fault_coolant_name, R.string.fault_coolant_desc),
}

data class ConnectionTelemetry(
    val isConnected: Boolean = true,
    val isWeakSignal: Boolean = false,
    val isReconnecting: Boolean = false,
    val reconnectAttempt: Int = 0,
    val secondsUntilReconnect: Int = 0,
    val latencyMs: Int = 2,
    val lastDisconnectReason: String? = null,
)

data class BatterySafetyState(
    val levelPct: Int = 100,
    val isCharging: Boolean = false,
    val isLowBattery: Boolean = false,
    val isCriticalBattery: Boolean = false,
    val isSimulated: Boolean = false,
)

data class AxisCoord(
    val name: String,
    val machinePos: Double = 0.0,
    val workPos: Double = 0.0,
    val dtgPos: Double = 0.0,
    val isHomed: Boolean = true,
    val minLimit: Double = -500.0,
    val maxLimit: Double = 500.0,
    val loadTorquePct: Double = 12.5,
    val motorTempC: Double = 34.0,
    val driveTempC: Double = 38.5,
    val encoderCounts: Long = 0L,
)

data class SpindleInfo(
    val commandedRpm: Double = 12000.0,
    val actualRpm: Double = 11985.0,
    val isEnabled: Boolean = false,
    val isClockwise: Boolean = true,
    val overridePct: Int = 100,
    val loadAmps: Double = 2.4,
)

data class FeedInfo(
    val commandedFeed: Double = 1500.0,
    val actualFeed: Double = 1495.0,
    val feedOverridePct: Int = 100,
    val rapidOverridePct: Int = 100,
)

data class CoolantInfo(
    val mist: Boolean = false,
    val flood: Boolean = false,
)

data class ProbeInfo(
    val isTripped: Boolean = false,
    val lastContactX: Double = 0.0,
    val lastContactY: Double = 0.0,
    val lastContactZ: Double = 0.0,
    val activeRoutine: String? = null,
)

data class ToolInfo(
    val toolNumber: Int = 1,
    val description: String = "6mm 2-Flute Carbide Endmill",
    val lengthOffset: Double = 45.230,
    val diameterOffset: Double = 6.000,
    val atcSlot: Int = 1,
)

enum class ToolType(@get:StringRes val displayNameRes: Int, val iconName: String) {
    ENDMILL(R.string.tool_type_endmill_disp, "ic_endmill"),
    BALLNOSE(R.string.tool_type_ballnose_disp, "ic_ballnose"),
    FACE_MILL(R.string.tool_type_facemill_disp, "ic_facemill"),
    DRILL(R.string.tool_type_drill_disp, "ic_drill"),
    CHAMFER(R.string.tool_type_chamfer_disp, "ic_chamfer"),
    TAP(R.string.tool_type_tap_disp, "ic_tap"),
    TOUCH_PROBE(R.string.tool_type_probe_disp, "ic_probe"),
    FLY_CUTTER(R.string.tool_type_flycutter_disp, "ic_flycutter")
}

data class CncToolItem(
    val id: Int,
    val pocket: Int,
    val description: String,
    val diameter: Double,
    val lengthOffset: Double,
    val wearLength: Double = 0.0,
    val wearDiameter: Double = 0.0,
    val toolType: ToolType = ToolType.ENDMILL,
    val flutes: Int = 3,
    val maxRpm: Double = 24000.0,
    val lifeMinutesCurrent: Double = 24.5,
    val lifeMinutesMax: Double = 120.0,
    val isActive: Boolean = false,
    val holderType: String = "ER20 / ISO30",
)

enum class UnitSystem(
    val code: String,
    val shortLabel: String,
    val lengthUnit: String,
    val speedUnit: String,
    val precisionDecimals: Int,
) {
    METRIC("G21", "MM", "mm", "mm/min", 3),
    IMPERIAL("G20", "INCH", "in", "IPM", 4);

    fun formatPosition(posMm: Double): String {
        return if (this == IMPERIAL) {
            val inches = posMm / 25.4
            java.lang.String.format(Locale.US, "%+08.${precisionDecimals}f", inches)
        } else {
            java.lang.String.format(Locale.US, "%+08.${precisionDecimals}f", posMm)
        }
    }

    fun formatSpeed(speedMmMin: Double): String {
        return if (this == IMPERIAL) {
            val ipm = speedMmMin / 25.4
            "${java.lang.String.format(Locale.US, "%.1f", ipm)} $speedUnit"
        } else {
            "${speedMmMin.toInt()} $speedUnit"
        }
    }

    fun toDisplayLength(lengthMm: Double): Double {
        return if (this == IMPERIAL) lengthMm / 25.4 else lengthMm
    }

    fun toMm(displayLength: Double): Double {
        return if (this == IMPERIAL) displayLength * 25.4 else displayLength
    }
}

enum class MpgMultiplier(val stepMm: Double, val metricLabel: String, val imperialLabel: String) {
    X1(0.001, "x1 (0.001 mm)", "x1 (0.0001 in)"),
    X10(0.010, "x10 (0.010 mm)", "x10 (0.001 in)"),
    X100(0.100, "x100 (0.100 mm)", "x100 (0.010 in)"),
    X1000(1.000, "x1000 (1.000 mm)", "x1000 (0.100 in)");

    val label: String get() = metricLabel

    fun getStep(unit: UnitSystem): Double {
        return if (unit == UnitSystem.IMPERIAL) {
            when (this) {
                X1 -> 0.00254 // 0.0001" in mm
                X10 -> 0.0254 // 0.001" in mm
                X100 -> 0.254 // 0.010" in mm
                X1000 -> 2.54 // 0.100" in mm
            }
        } else {
            stepMm
        }
    }

    fun getLabel(unit: UnitSystem): String {
        return if (unit == UnitSystem.IMPERIAL) imperialLabel else metricLabel
    }
}

enum class JogControlStyle(@get:StringRes val displayNameRes: Int) {
    BUTTON_PAD(R.string.jog_style_pad_disp),
    VIRTUAL_MPG(R.string.jog_style_mpg_disp)
}

data class EtherCatSlaveInfo(
    val slaveIndex: Int,
    val name: String,
    val state: String = "OP (Operational)",
    val actualTorquePct: Double = 14.2,
    val driveTempC: Double = 39.0,
    val alarmCode: String = "AL.000 (NORMAL)",
    val isFault: Boolean = false,
)

data class EtherCatMasterInfo(
    val masterState: String = "OP (Operational)",
    val slaveCount: Int = 4,
    val busCycleTimeUs: Int = 1000,
    val packetLossPct: Double = 0.00,
    val dcOffsetNs: Long = 12,
)

data class CapabilitiesManifest(
    val machineName: String = "Industrial VMC-850",
    val architecture: HardwareArchitecture = HardwareArchitecture.ETHERCAT_DELTA,
    val compatibilityLevel: Int = 3,
    val axes: List<String> = listOf("X", "Y", "Z", "A"),
    val hasProbe: Boolean = true,
    val hasToolChanger: Boolean = true,
    val hasSpindleEncoder: Boolean = true,
    val hasServoTorque: Boolean = true,
    val hasDriveTemp: Boolean = true,
    val hasEtherCat: Boolean = true,
    val hostIp: String = "192.168.1.100",
    val port: Int = 8000,
    val isConnected: Boolean = true,
    val pingMs: Int = 4,
)

data class GCodeSegment(
    val lineNumber: Int,
    val rawText: String,
    val isRapid: Boolean = false,
    val isCut: Boolean = true,
    val startX: Float = 0f,
    val startY: Float = 0f,
    val startZ: Float = 0f,
    val endX: Float = 0f,
    val endY: Float = 0f,
    val endZ: Float = 0f,
)


enum class LogSeverity(@get:StringRes val displayNameRes: Int) {
    INFO(R.string.log_info_disp),
    WARNING(R.string.log_warn_disp),
    ERROR(R.string.log_error_disp),
    CRITICAL(R.string.log_crit_disp),
    SECURITY(R.string.log_security_disp)
}

data class CncEventLog(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val severity: LogSeverity = LogSeverity.INFO,
    val tag: String = "SYSTEM",
    val message: String = "",
)

enum class ThreatLevel(@get:StringRes val displayNameRes: Int) {
    CLEAN(R.string.threat_clean_disp),
    SUSPICIOUS(R.string.threat_suspicious_disp),
    MALWARE_BLOCKED(R.string.threat_blocked_disp)
}

data class SecurityThreat(
    val code: String,
    val title: String,
    val description: String,
    val lineNumber: Int = -1,
    val lineContent: String = "",
    val severity: LogSeverity = LogSeverity.SECURITY,
)

data class SecurityScanResult(
    val fileName: String = "program.ngc",
    val fileSizeBytes: Long = 0L,
    val totalLines: Int = 0,
    val threatLevel: ThreatLevel = ThreatLevel.CLEAN,
    val threats: List<SecurityThreat> = emptyList(),
    val isExecutable: Boolean = true,
    val scanDurationMs: Long = 0L,
    val sha256Fingerprint: String = "",
    val hasMotion: Boolean = false,
    val boundingBoxX: Pair<Float, Float> = Pair(0f, 0f),
    val boundingBoxY: Pair<Float, Float> = Pair(0f, 0f),
    val boundingBoxZ: Pair<Float, Float> = Pair(0f, 0f),
)

data class MaterialPreset(
    val id: String,
    @get:StringRes val nameRes: Int,
    @get:StringRes val categoryRes: Int,
    val surfaceSpeedMMin: Double, // Vc (m/min)
    val feedPerToothMm: Double,   // Fz (mm/tooth for 6mm standard)
    val powerFactor: Double,       // specific cutting force factor,
)

data class SpeedFeedCalculation(
    val material: MaterialPreset,
    val toolDiameterMm: Double,
    val flutes: Int,
    val calculatedRpm: Double,
    val calculatedFeedMmMin: Double,
    val recommendedDocMm: Double,
    val recommendedWocMm: Double,
    val spindlePowerKw: Double,
)

data class AxisCalibrationPoint(
    val stepIndex: Int,
    val percentOfTravel: Double, // 0.0 to 100.0%
    val nominalPositionMm: Double,
    val measuredPositionMm: Double? = null,
    val errorMm: Double? = null,
    val sectorUncertaintyMm: Double = 0.002,
)

data class AxisCalibrationSession(
    val id: String = java.util.UUID.randomUUID().toString(),
    val axis: String = "X",
    val totalTravelMm: Double = 600.0,
    val stepIntervalPercent: Double = 10.0,
    val totalSteps: Int = 11, // 0%, 10%, 20%, ..., 100%
    val instrumentName: String = "Dial Indicator / Micrometer (Grade 0)",
    val instrumentUncertaintyMm: Double = 0.003,
    val points: List<AxisCalibrationPoint> = emptyList(),
    val isCompleted: Boolean = false,
    val maxErrorMm: Double = 0.0,
    val meanErrorMm: Double = 0.0,
    val expandedUncertaintyMm: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
)

data class DocSectionItem(
    val id: String,
    @get:StringRes val titleRes: Int,
    val category: String,
    val iconName: String,
    @get:StringRes val summaryRes: Int,
    @get:StringRes val detailedContentRes: Int,
    @get:ArrayRes val standardStepsRes: Int = 0,
    @get:ArrayRes val safetyTipsRes: Int = 0,
)

// ============================================================================
// PRO MODULE 1: WCS OFFSETS (G54 - G59.3)
// ============================================================================
data class WcsOffset(
    val name: String, // "G54", "G55", "G56", "G57", "G58", "G59", "G59.1", "G59.2", "G59.3"
    val pIndex: Int, // 1 through 9
    val x: Double = 0.0,
    val y: Double = 0.0,
    val z: Double = 0.0,
    val a: Double = 0.0,
    val b: Double = 0.0,
    val c: Double = 0.0,
    val comment: String = "",
)

// ============================================================================
// PRO MODULE 2: MDI VALIDATION & HISTORY
// ============================================================================
data class MdiValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null,
    val parsedTokens: List<String> = emptyList(),
    val isMotionCommand: Boolean = false,
    val isSpindleCommand: Boolean = false,
)

data class MdiHistoryItem(
    val id: Long = 0,
    val command: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val executionStatus: String = "SUCCESS",
)

// ============================================================================
// PRO MODULE 3: HAL SIGNALS & REAL-TIME PIN MONITOR
// ============================================================================
enum class HalPinType {
    BIT, FLOAT, S32, U32
}

enum class HalPinCategory {
    SAFETY,
    LIMIT_SWITCHES,
    SPINDLE,
    MOTION,
    IO_EXPANSION
}

data class HalPin(
    val name: String,
    val category: HalPinCategory,
    val type: HalPinType = HalPinType.BIT,
    val booleanValue: Boolean = false,
    val floatValue: Double = 0.0,
    val description: String,
    val isAlarmTrigger: Boolean = false,
    val invertSignal: Boolean = false,
)

// ============================================================================
// PRO MODULE 4: SOFT LIMITS PRE-CHECK & BOUNDING BOX
// ============================================================================
data class AxisRange(
    val min: Double,
    val max: Double,
) {
    val span: Double get() = max - min
}

data class GCodeBoundingBox(
    val x: AxisRange,
    val y: AxisRange,
    val z: AxisRange,
    val a: AxisRange? = null,
    val totalMotionLengthMm: Double = 0.0,
)

data class AxisLimitViolation(
    val axis: String,
    val programMin: Double,
    val programMax: Double,
    val machineMinLimit: Double,
    val machineMaxLimit: Double,
    val excessMinMm: Double = 0.0, // > 0 if overtravel below min
    val excessMaxMm: Double = 0.0, // > 0 if overtravel above max
)

data class SoftLimitsCheckResult(
    val isWithinLimits: Boolean,
    val activeWcs: String,
    val boundingBoxWork: GCodeBoundingBox,
    val boundingBoxMachine: GCodeBoundingBox,
    val violations: List<AxisLimitViolation> = emptyList(),
    val checkedAt: Long = System.currentTimeMillis(),
)

// ============================================================================
// PRO MODULE 5: ACTIVE G-CODE MODAL GROUPS & EXECUTION MODIFIERS
// ============================================================================
data class ModalGCodeState(
    val motionMode: String = "G0",            // G0, G1, G2, G3, G33, G38.2
    val planeSelect: String = "G17 (XY)",     // G17, G18, G19
    val distanceMode: String = "G90 (ABS)",   // G90, G91
    val arcDistanceMode: String = "G91.1",    // G90.1, G91.1
    val feedMode: String = "G94 (UNITS/MIN)", // G93, G94, G95
    val unitsMode: String = "G21 (MM)",       // G20 (INCH), G21 (MM)
    val cutterRadiusComp: String = "G40 (OFF)", // G40, G41, G42
    val toolLengthComp: String = "G49 (OFF)", // G43, G49
    val toolLengthZOffsetMm: Double = 0.0,
    val activeToolNumber: Int = 1,
    val activeWcs: String = "G54",            // G54..G59.3
    val spindleMode: String = "M5 (STOP)",    // M3 (CW), M4 (CCW), M5 (STOP)
    val coolantMode: String = "M9 (OFF)",     // M7 (MIST), M8 (FLOOD), M9 (OFF)
)

data class ExecutionModifiers(
    val singleBlockMode: Boolean = false,      // Paso a paso (una sola línea por Cycle Start)
    val optionalStopM1: Boolean = true,        // Detenerse en M1 si está activo
    val blockDelete: Boolean = false,          // Saltar líneas que inician con '/'
    val runFromLineNumber: Int = 1,            // Reanudar en línea seleccionada
)

