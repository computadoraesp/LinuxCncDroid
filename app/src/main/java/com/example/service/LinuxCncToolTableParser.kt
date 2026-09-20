package com.example.service

import com.example.model.CncToolItem
import com.example.model.ToolType
import java.util.Locale

object LinuxCncToolTableParser {

    /**
     * Parses a standard LinuxCNC tool.tbl file content into a list of CncToolItem.
     */
    fun parseToolTable(content: String): List<CncToolItem> {
        val tools = mutableListOf<CncToolItem>()

        content.lines().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#")) return@forEach
            // Separate comment
            val parts = line.split(";", limit = 2)
            val codePart = parts[0].trim()
            val commentPart = if (parts.size > 1) parts[1].trim() else ""

            if (codePart.isEmpty()) return@forEach

            // Tokens like T1 P1 D6.000000 Z+45.230000
            val tokens = codePart.split("\\s+".toRegex())
            var toolId = 0
            var pocket = 0
            var diameter = 0.0
            var lengthZ = 0.0

            tokens.forEach { token ->
                val upper = token.uppercase(Locale.ROOT)
                when {
                    upper.startsWith("T") -> toolId = upper.substring(1).toIntOrNull() ?: toolId
                    upper.startsWith("P") -> pocket = upper.substring(1).toIntOrNull() ?: pocket
                    upper.startsWith("D") -> diameter = upper.substring(1).toDoubleOrNull() ?: diameter
                    upper.startsWith("Z") -> lengthZ = upper.substring(1).toDoubleOrNull() ?: lengthZ
                }
            }

            if (toolId > 0) {
                // Infer tool type from comment or diameter
                val inferredType = inferToolType(commentPart)
                tools.add(
                    CncToolItem(
                        id = toolId,
                        pocket = if (pocket > 0) pocket else toolId,
                        description = if (commentPart.isNotBlank()) commentPart else "Tool T$toolId",
                        diameter = diameter,
                        lengthOffset = lengthZ,
                        toolType = inferredType,
                        flutes = inferFlutes(inferredType),
                        maxRpm = inferMaxRpm(diameter, inferredType),
                        isActive = (toolId == 1)
                    )
                )
            }
        }

        return if (tools.isNotEmpty()) tools else emptyList()
    }

    /**
     * Formats a list of CncToolItem into valid LinuxCNC tool.tbl text format.
     */
    fun exportToolTable(tools: List<CncToolItem>): String {
        val sb = StringBuilder()
        sb.append("; ==========================================================================\n")
        sb.append("; LinuxCNC Tool Table File (tool.tbl)\n")
        sb.append("; Exported from LinuxCncDroid Industrial Mobile Station\n")
        sb.append("; Format: T<tool> P<pocket> D<diameter> Z<offset> ; <description>\n")
        sb.append("; ==========================================================================\n\n")

        tools.sortedBy { it.id }.forEach { t ->
            sb.append(
                String.format(
                    Locale.US,
                    "T%-3d P%-3d D%8.4f Z%+8.4f ; %s\n",
                    t.id,
                    t.pocket,
                    t.diameter,
                    t.lengthOffset,
                    t.description
                )
            )
        }

        return sb.toString()
    }

    private fun inferToolType(comment: String): ToolType {
        val lower = comment.lowercase(Locale.ROOT)
        return when {
            lower.contains("ball") || lower.contains("esferic") -> ToolType.BALLNOSE
            lower.contains("face") || lower.contains("planear") -> ToolType.FACE_MILL
            lower.contains("drill") || lower.contains("broca") -> ToolType.DRILL
            lower.contains("tap") || lower.contains("macho") -> ToolType.TAP
            lower.contains("chamfer") || lower.contains("chaflan") -> ToolType.CHAMFER
            lower.contains("probe") || lower.contains("palpador") -> ToolType.TOUCH_PROBE
            else -> ToolType.ENDMILL
        }
    }

    private fun inferFlutes(type: ToolType): Int = when (type) {
        ToolType.ENDMILL -> 3
        ToolType.BALLNOSE -> 2
        ToolType.FACE_MILL -> 4
        ToolType.DRILL -> 2
        ToolType.TAP -> 3
        ToolType.CHAMFER -> 4
        ToolType.TOUCH_PROBE -> 1
        ToolType.FLY_CUTTER -> 1
    }

    fun generateSampleToolTable(): String {
        return """
; LinuxCNC tool.tbl sample
T1 P1 D6.0000 Z+50.0000 ; 6mm 3-Flute Carbide Endmill
T2 P2 D3.0000 Z+45.2000 ; 3mm 2-Flute Ballnose
T3 P3 D50.0000 Z+32.1000 ; 50mm 4-Insert Face Mill
T4 P4 D4.2000 Z+62.5000 ; 4.2mm HSS Drill (M5 Tap Prep)
T5 P5 D5.0000 Z+58.0000 ; M5 Spiral Tap
T6 P6 D10.0000 Z+40.0000 ; 90-deg Chamfer Mill
T99 P99 D2.0000 Z+80.0000 ; 3D Touch Probe Renishaw
""".trimIndent()
    }

    private fun inferMaxRpm(diameter: Double, type: ToolType): Double = when (type) {
        ToolType.TOUCH_PROBE -> 0.0
        ToolType.TAP -> 1200.0
        ToolType.DRILL -> 4500.0
        ToolType.FACE_MILL -> 8000.0
        else -> if (diameter <= 3.0) 24000.0 else if (diameter <= 6.0) 18000.0 else 12000.0
    }
}
