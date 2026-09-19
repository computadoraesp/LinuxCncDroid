package com.example.service

import com.example.model.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Industrial Soft Limits Pre-Checker.
 * Evaluates the entire toolpath trajectory against machine soft limits (travel envelopes)
 * projecting Work Coordinates through the active WCS offset (G54..G59.3).
 *
 * Prevents catastrophic joint overtravel crashes before Cycle Start is allowed.
 */
class SoftLimitsPreChecker {

    fun checkProgramLimits(
        segments: List<GCodeSegment>,
        activeWcsOffset: WcsOffset,
        machineAxes: Map<String, AxisCoord>
    ): SoftLimitsCheckResult {
        if (segments.isEmpty()) {
            val emptyRange = AxisRange(0.0, 0.0)
            val emptyBox = GCodeBoundingBox(emptyRange, emptyRange, emptyRange)
            return SoftLimitsCheckResult(
                isWithinLimits = true,
                activeWcs = activeWcsOffset.name,
                boundingBoxWork = emptyBox,
                boundingBoxMachine = emptyBox,
                violations = emptyList()
            )
        }

        var minX = Double.MAX_VALUE
        var maxX = -Double.MAX_VALUE
        var minY = Double.MAX_VALUE
        var maxY = -Double.MAX_VALUE
        var minZ = Double.MAX_VALUE
        var maxZ = -Double.MAX_VALUE

        var hasX = false
        var hasY = false
        var hasZ = false
        var totalMotionLength = 0.0

        for (segment in segments) {
            val sx = segment.startX.toDouble()
            val sy = segment.startY.toDouble()
            val sz = segment.startZ.toDouble()
            val ex = segment.endX.toDouble()
            val ey = segment.endY.toDouble()
            val ez = segment.endZ.toDouble()

            minX = min(minX, min(sx, ex))
            maxX = max(maxX, max(sx, ex))
            hasX = true

            minY = min(minY, min(sy, ey))
            maxY = max(maxY, max(sy, ey))
            hasY = true

            minZ = min(minZ, min(sz, ez))
            maxZ = max(maxZ, max(sz, ez))
            hasZ = true

            val dx = ex - sx
            val dy = ey - sy
            val dz = ez - sz
            totalMotionLength += sqrt(dx * dx + dy * dy + dz * dz)
        }

        val workXRange = if (hasX) AxisRange(minX, maxX) else AxisRange(0.0, 0.0)
        val workYRange = if (hasY) AxisRange(minY, maxY) else AxisRange(0.0, 0.0)
        val workZRange = if (hasZ) AxisRange(minZ, maxZ) else AxisRange(0.0, 0.0)

        val workBoundingBox = GCodeBoundingBox(
            x = workXRange,
            y = workYRange,
            z = workZRange,
            totalMotionLengthMm = totalMotionLength
        )

        // Project into Machine Coordinates (G53 = Work + WCS Offset)
        val machXMin = workXRange.min + activeWcsOffset.x
        val machXMax = workXRange.max + activeWcsOffset.x

        val machYMin = workYRange.min + activeWcsOffset.y
        val machYMax = workYRange.max + activeWcsOffset.y

        val machZMin = workZRange.min + activeWcsOffset.z
        val machZMax = workZRange.max + activeWcsOffset.z

        val machineBoundingBox = GCodeBoundingBox(
            x = AxisRange(machXMin, machXMax),
            y = AxisRange(machYMin, machYMax),
            z = AxisRange(machZMin, machZMax),
            totalMotionLengthMm = totalMotionLength
        )

        val violations = mutableListOf<AxisLimitViolation>()

        // Check X Axis
        machineAxes["X"]?.let { axis ->
            val excessMin = if (machXMin < axis.minLimit) axis.minLimit - machXMin else 0.0
            val excessMax = if (machXMax > axis.maxLimit) machXMax - axis.maxLimit else 0.0
            if (excessMin > 0.001 || excessMax > 0.001) {
                violations.add(
                    AxisLimitViolation(
                        axis = "X",
                        programMin = machXMin,
                        programMax = machXMax,
                        machineMinLimit = axis.minLimit,
                        machineMaxLimit = axis.maxLimit,
                        excessMinMm = excessMin,
                        excessMaxMm = excessMax
                    )
                )
            }
        }

        // Check Y Axis
        machineAxes["Y"]?.let { axis ->
            val excessMin = if (machYMin < axis.minLimit) axis.minLimit - machYMin else 0.0
            val excessMax = if (machYMax > axis.maxLimit) machYMax - axis.maxLimit else 0.0
            if (excessMin > 0.001 || excessMax > 0.001) {
                violations.add(
                    AxisLimitViolation(
                        axis = "Y",
                        programMin = machYMin,
                        programMax = machYMax,
                        machineMinLimit = axis.minLimit,
                        machineMaxLimit = axis.maxLimit,
                        excessMinMm = excessMin,
                        excessMaxMm = excessMax
                    )
                )
            }
        }

        // Check Z Axis
        machineAxes["Z"]?.let { axis ->
            val excessMin = if (machZMin < axis.minLimit) axis.minLimit - machZMin else 0.0
            val excessMax = if (machZMax > axis.maxLimit) machZMax - axis.maxLimit else 0.0
            if (excessMin > 0.001 || excessMax > 0.001) {
                violations.add(
                    AxisLimitViolation(
                        axis = "Z",
                        programMin = machZMin,
                        programMax = machZMax,
                        machineMinLimit = axis.minLimit,
                        machineMaxLimit = axis.maxLimit,
                        excessMinMm = excessMin,
                        excessMaxMm = excessMax
                    )
                )
            }
        }

        return SoftLimitsCheckResult(
            isWithinLimits = violations.isEmpty(),
            activeWcs = activeWcsOffset.name,
            boundingBoxWork = workBoundingBox,
            boundingBoxMachine = machineBoundingBox,
            violations = violations
        )
    }
}
