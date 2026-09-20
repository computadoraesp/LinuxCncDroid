package com.example.service

import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

object ConversationalCamEngine {

    /**
     * Generates standard G-Code for Facing (Planear Superficie).
     */
    fun generateFacingCycle(
        xOrigin: Double = 0.0,
        yOrigin: Double = 0.0,
        lengthX: Double = 100.0,
        widthY: Double = 80.0,
        totalDepthZ: Double = 1.0,
        stepdownZ: Double = 0.5,
        toolDiameter: Double = 50.0,
        stepoverPct: Double = 70.0,
        feedrate: Double = 1500.0,
        plungeFeed: Double = 400.0,
        spindleRpm: Double = 5000.0,
        safeZ: Double = 5.0,
        isZigZag: Boolean = true,
        toolNumber: Int = 3
    ): String {
        val sb = StringBuilder()
        val stepoverMm = toolDiameter * (stepoverPct / 100.0)
        val toolRadius = toolDiameter / 2.0

        sb.append("; ==========================================================\n")
        sb.append("; LINUXCNC CONVERSATIONAL: PLANEAR SUPERFICIE (FACING)\n")
        sb.append(String.format(Locale.US, "; Area: %.1f x %.1f mm | Profundidad: -%.2f mm\n", lengthX, widthY, totalDepthZ))
        sb.append(String.format(Locale.US, "; Fresa T%d (D=%.1f mm) | Paso: %.1f mm (%.0f%%)\n", toolNumber, toolDiameter, stepoverMm, stepoverPct))
        sb.append("; ==========================================================\n")
        sb.append("G21 G90 G17 G40 G49 (Configuracion modal segura)\n")
        sb.append(String.format(Locale.US, "T%d M6 G43\n", toolNumber))
        sb.append(String.format(Locale.US, "S%.0f M3 (Arrancar husillo)\n", spindleRpm))
        sb.append("M8 (Refrigeracion ON)\n")
        sb.append(String.format(Locale.US, "G0 Z%.3f (Altura de seguridad)\n", safeZ))

        var currentZ = 0.0
        val targetZ = -Math.abs(totalDepthZ)
        val zStep = Math.abs(stepdownZ)

        // Bounding box with tool radius lead-in/lead-out
        val startX = xOrigin - toolRadius - 2.0
        val endX = xOrigin + lengthX + toolRadius + 2.0

        while (currentZ > targetZ) {
            currentZ -= zStep
            if (currentZ < targetZ) currentZ = targetZ

            sb.append(String.format(Locale.US, "\n; --- Pase Z = %.3f mm ---\n", currentZ))

            var currentY = yOrigin + toolRadius
            val maxY = yOrigin + widthY - toolRadius
            var directionForward = true

            // Position at initial XY
            sb.append(String.format(Locale.US, "G0 X%.3f Y%.3f\n", startX, currentY))
            sb.append(String.format(Locale.US, "G1 Z%.3f F%.0f\n", currentZ, plungeFeed))

            while (currentY <= maxY + stepoverMm * 0.5) {
                if (directionForward) {
                    sb.append(String.format(Locale.US, "G1 X%.3f F%.0f\n", endX, feedrate))
                } else {
                    sb.append(String.format(Locale.US, "G1 X%.3f F%.0f\n", startX, feedrate))
                }

                currentY += stepoverMm
                if (currentY > maxY + stepoverMm * 0.5) break

                if (isZigZag) {
                    // Shift Y at feed
                    sb.append(String.format(Locale.US, "G1 Y%.3f F%.0f\n", currentY, feedrate))
                    directionForward = !directionForward
                } else {
                    // Retract, move rapid, plunge
                    sb.append(String.format(Locale.US, "G0 Z%.3f\n", safeZ))
                    sb.append(String.format(Locale.US, "G0 X%.3f Y%.3f\n", startX, currentY))
                    sb.append(String.format(Locale.US, "G1 Z%.3f F%.0f\n", currentZ, plungeFeed))
                    directionForward = true
                }
            }

            // Retract at end of pass
            sb.append(String.format(Locale.US, "G0 Z%.3f\n", safeZ))
        }

        sb.append("\n(--- Fin de Ciclo ---)\n")
        sb.append(String.format(Locale.US, "G0 Z%.3f\n", safeZ + 15.0))
        sb.append("M9 (Refrigeracion OFF)\n")
        sb.append("M5 (Parada de husillo)\n")
        sb.append("G0 X0 Y0 (Retorno a origen)\n")
        sb.append("M30\n")

        return sb.toString()
    }

    /**
     * Generates standard G-Code for Rectangular Pocket (Cajeado Rectangular).
     */
    fun generateRectangularPocket(
        xCenter: Double = 50.0,
        yCenter: Double = 40.0,
        lengthX: Double = 60.0,
        widthY: Double = 40.0,
        cornerRad: Double = 5.0,
        totalDepthZ: Double = 5.0,
        stepdownZ: Double = 1.0,
        toolDiameter: Double = 6.0,
        stepoverPct: Double = 60.0,
        feedrate: Double = 1200.0,
        plungeFeed: Double = 350.0,
        spindleRpm: Double = 12000.0,
        safeZ: Double = 5.0,
        toolNumber: Int = 1
    ): String {
        val sb = StringBuilder()
        val toolRad = toolDiameter / 2.0
        val stepover = toolDiameter * (stepoverPct / 100.0)

        sb.append("; ==========================================================\n")
        sb.append("; LINUXCNC CONVERSATIONAL: CAJEADO RECTANGULAR (POCKET)\n")
        sb.append(String.format(Locale.US, "; Centro: (%.1f, %.1f) | Tam: %.1f x %.1f mm\n", xCenter, yCenter, lengthX, widthY))
        sb.append(String.format(Locale.US, "; Profundidad Z: -%.2f mm | Fresa T%d (D=%.1f mm)\n", totalDepthZ, toolNumber, toolDiameter))
        sb.append("; ==========================================================\n")
        sb.append("G21 G90 G17 G40 G49\n")
        sb.append(String.format(Locale.US, "T%d M6 G43\n", toolNumber))
        sb.append(String.format(Locale.US, "S%.0f M3\n", spindleRpm))
        sb.append("M8\n")
        sb.append(String.format(Locale.US, "G0 Z%.3f\n", safeZ))

        var currentZ = 0.0
        val targetZ = -Math.abs(totalDepthZ)
        val zStep = Math.abs(stepdownZ)

        // Internal bounds for tool center
        val minX = xCenter - (lengthX / 2.0) + toolRad
        val maxX = xCenter + (lengthX / 2.0) - toolRad
        val minY = yCenter - (widthY / 2.0) + toolRad
        val maxY = yCenter + (widthY / 2.0) - toolRad

        while (currentZ > targetZ) {
            currentZ -= zStep
            if (currentZ < targetZ) currentZ = targetZ

            sb.append(String.format(Locale.US, "\n; --- Profundidad Z = %.3f mm ---\n", currentZ))
            // Plunge at center
            sb.append(String.format(Locale.US, "G0 X%.3f Y%.3f\n", xCenter, yCenter))
            sb.append(String.format(Locale.US, "G1 Z%.3f F%.0f\n", currentZ, plungeFeed))

            // Expand outward in concentric rectangles
            var currentSpanX = stepover
            var currentSpanY = stepover
            val maxSpanX = maxX - minX
            val maxSpanY = maxY - minY

            while (currentSpanX <= maxSpanX || currentSpanY <= maxSpanY) {
                val curMinX = (xCenter - currentSpanX / 2.0).coerceAtLeast(minX)
                val curMaxX = (xCenter + currentSpanX / 2.0).coerceAtMost(maxX)
                val curMinY = (yCenter - currentSpanY / 2.0).coerceAtLeast(minY)
                val curMaxY = (yCenter + currentSpanY / 2.0).coerceAtMost(maxY)

                sb.append(String.format(Locale.US, "G1 X%.3f Y%.3f F%.0f\n", curMinX, curMinY, feedrate))
                sb.append(String.format(Locale.US, "G1 X%.3f Y%.3f\n", curMaxX, curMinY))
                sb.append(String.format(Locale.US, "G1 X%.3f Y%.3f\n", curMaxX, curMaxY))
                sb.append(String.format(Locale.US, "G1 X%.3f Y%.3f\n", curMinX, curMaxY))
                sb.append(String.format(Locale.US, "G1 X%.3f Y%.3f\n", curMinX, curMinY))

                if (currentSpanX >= maxSpanX && currentSpanY >= maxSpanY) break
                currentSpanX = (currentSpanX + stepover).coerceAtMost(maxSpanX)
                currentSpanY = (currentSpanY + stepover).coerceAtMost(maxSpanY)
            }

            // Retract
            sb.append(String.format(Locale.US, "G0 Z%.3f\n", safeZ))
        }

        sb.append("\n(--- Fin de Cajeado ---)\n")
        sb.append(String.format(Locale.US, "G0 Z%.3f\n", safeZ + 20.0))
        sb.append("M9\n")
        sb.append("M5\n")
        sb.append("G0 X0 Y0\n")
        sb.append("M30\n")

        return sb.toString()
    }

    /**
     * Generates standard G-Code for Bolt Hole Circle (Barrenado de Círculo de Agujeros - G81/G83).
     */
    fun generateBoltHoleCircle(
        centerX: Double = 0.0,
        centerY: Double = 0.0,
        pcdDiameter: Double = 80.0,
        numHoles: Int = 6,
        startAngleDeg: Double = 0.0,
        drillDepthZ: Double = 15.0,
        retractR: Double = 2.0,
        peckQ: Double = 3.0,
        feedrate: Double = 250.0,
        spindleRpm: Double = 3500.0,
        safeZ: Double = 10.0,
        toolNumber: Int = 4
    ): String {
        val sb = StringBuilder()
        val radius = pcdDiameter / 2.0
        val isPeck = peckQ > 0.0
        val cycleCode = if (isPeck) "G83" else "G81"

        sb.append("; ==========================================================\n")
        sb.append("; LINUXCNC CONVERSATIONAL: CIRCULO DE AGUJEROS (BOLT CIRCLE)\n")
        sb.append(String.format(Locale.US, "; Centro: (%.1f, %.1f) | PCD: %.1f mm | Orificios: %d\n", centerX, centerY, pcdDiameter, numHoles))
        sb.append(String.format(Locale.US, "; Profundidad Z: -%.2f mm | Ciclo: %s | Fresa T%d\n", drillDepthZ, cycleCode, toolNumber))
        sb.append("; ==========================================================\n")
        sb.append("G21 G90 G17 G40 G49 (Modal seguro)\n")
        sb.append(String.format(Locale.US, "T%d M6 G43\n", toolNumber))
        sb.append(String.format(Locale.US, "S%.0f M3\n", spindleRpm))
        sb.append("M8\n")
        sb.append(String.format(Locale.US, "G0 Z%.3f\n", safeZ))

        val angleStep = 360.0 / numHoles.coerceAtLeast(1)

        for (i in 0 until numHoles) {
            val angleDeg = startAngleDeg + (i * angleStep)
            val angleRad = Math.toRadians(angleDeg)
            val holeX = centerX + (radius * cos(angleRad))
            val holeY = centerY + (radius * sin(angleRad))

            if (i == 0) {
                // First hole initiates the canned cycle
                if (isPeck) {
                    sb.append(
                        String.format(
                            Locale.US,
                            "%s X%.3f Y%.3f Z-%.3f R%.3f Q%.3f F%.0f (Orificio #1 a %.1f deg)\n",
                            cycleCode, holeX, holeY, drillDepthZ, retractR, peckQ, feedrate, angleDeg
                        )
                    )
                } else {
                    sb.append(
                        String.format(
                            Locale.US,
                            "%s X%.3f Y%.3f Z-%.3f R%.3f F%.0f (Orificio #1 a %.1f deg)\n",
                            cycleCode, holeX, holeY, drillDepthZ, retractR, feedrate, angleDeg
                        )
                    )
                }
            } else {
                // Subsequent holes repeat canned cycle at new coordinate
                sb.append(
                    String.format(
                        Locale.US,
                        "X%.3f Y%.3f (Orificio #%d a %.1f deg)\n",
                        holeX, holeY, i + 1, angleDeg
                    )
                )
            }
        }

        sb.append("G80 (Cancelar ciclo enlatado)\n")
        sb.append(String.format(Locale.US, "G0 Z%.3f\n", safeZ + 15.0))
        sb.append("M9\n")
        sb.append("M5\n")
        sb.append("G0 X0 Y0\n")
        sb.append("M30\n")

        return sb.toString()
    }

    fun generateFacingGCode(params: FacingCycleParams): String {
        return generateFacingCycle(
            lengthX = params.lengthX,
            widthY = params.widthY,
            totalDepthZ = params.totalDepth,
            stepdownZ = params.depthPerPass,
            toolDiameter = params.toolDiameter,
            stepoverPct = params.stepoverPercent,
            feedrate = params.feedRate,
            spindleRpm = params.spindleRpm,
            toolNumber = params.toolNumber
        )
    }

    fun generateRectangularPocketGCode(params: RectangularPocketParams): String {
        return generateRectangularPocket(
            lengthX = params.pocketLengthX,
            widthY = params.pocketWidthY,
            totalDepthZ = params.totalDepthZ,
            stepdownZ = params.depthPerPass,
            toolDiameter = params.toolDiameter,
            stepoverPct = params.stepoverPercent,
            feedrate = params.feedRateXY,
            plungeFeed = params.plungeFeedZ,
            spindleRpm = params.spindleRpm,
            toolNumber = params.toolNumber
        )
    }

    fun generateBoltHoleCircleGCode(params: BoltHoleCircleParams): String {
        return generateBoltHoleCircle(
            centerX = params.centerX,
            centerY = params.centerY,
            pcdDiameter = params.circleDiameter,
            numHoles = params.numberOfHoles,
            startAngleDeg = params.startAngleDeg,
            drillDepthZ = params.holeDepthZ,
            retractR = params.retractPlaneR,
            peckQ = params.peckIncrementQ,
            feedrate = params.feedRate,
            spindleRpm = params.spindleRpm,
            toolNumber = params.toolNumber
        )
    }
}

data class FacingCycleParams(
    val lengthX: Double = 100.0,
    val widthY: Double = 80.0,
    val toolDiameter: Double = 12.0,
    val stepoverPercent: Double = 60.0,
    val totalDepth: Double = 1.5,
    val depthPerPass: Double = 0.5,
    val feedRate: Double = 1200.0,
    val spindleRpm: Double = 6000.0,
    val toolNumber: Int = 1
)

data class RectangularPocketParams(
    val pocketLengthX: Double = 60.0,
    val pocketWidthY: Double = 40.0,
    val totalDepthZ: Double = 5.0,
    val depthPerPass: Double = 1.0,
    val toolDiameter: Double = 6.0,
    val stepoverPercent: Double = 50.0,
    val feedRateXY: Double = 800.0,
    val plungeFeedZ: Double = 250.0,
    val spindleRpm: Double = 12000.0,
    val toolNumber: Int = 2
)

data class BoltHoleCircleParams(
    val centerX: Double = 0.0,
    val centerY: Double = 0.0,
    val circleDiameter: Double = 75.0,
    val numberOfHoles: Int = 6,
    val startAngleDeg: Double = 0.0,
    val holeDepthZ: Double = 15.0,
    val peckIncrementQ: Double = 3.0,
    val retractPlaneR: Double = 2.0,
    val feedRate: Double = 250.0,
    val spindleRpm: Double = 3500.0,
    val toolNumber: Int = 3
)
