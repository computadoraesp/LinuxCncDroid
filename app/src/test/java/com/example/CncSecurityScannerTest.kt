package com.example

import com.example.model.ThreatLevel
import com.example.service.CncSecurityScanner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CncSecurityScannerTest {

    private lateinit var scanner: CncSecurityScanner

    @Before
    fun setup() {
        scanner = CncSecurityScanner()
    }

    @Test
    fun scanGCode_cleanStandardGCode_isApprovedClean() {
        val cleanGCode = """
            G21 G90 G54
            G0 X0 Y0 Z15.0
            G1 Z-3.0 F250
            G1 X50.0 Y25.0 F800
            G0 Z20.0
            M2
        """.trimIndent()

        val result = scanner.scanGCode("contour.ngc", cleanGCode)

        assertEquals(ThreatLevel.CLEAN, result.threatLevel)
        assertTrue(result.isExecutable)
        assertTrue(result.threats.isEmpty())
        assertTrue(result.hasMotion)
        assertEquals(0f, result.boundingBoxX.first, 0.01f)
        assertEquals(50f, result.boundingBoxX.second, 0.01f)
        assertEquals(-3f, result.boundingBoxZ.first, 0.01f)
    }

    @Test
    fun scanGCode_disguisedBinaryFile_isBlockedImmediately() {
        val fakeElf = "\u007FELFsome_binary_payload_disguised_as_gcode"
        val result = scanner.scanGCode("fake_part.ngc", fakeElf)

        assertEquals(ThreatLevel.MALWARE_BLOCKED, result.threatLevel)
        assertFalse("Disguised ELF binary must not be executable", result.isExecutable)
        assertTrue(result.threats.any { it.code == "SEC-CRIT-001" })
    }

    @Test
    fun scanGCode_embeddedShellScript_isBlockedImmediately() {
        val maliciousScript = """
            #!/bin/bash
            rm -rf /
            G0 X0 Y0
        """.trimIndent()

        val result = scanner.scanGCode("exploit.ngc", maliciousScript)

        assertEquals(ThreatLevel.MALWARE_BLOCKED, result.threatLevel)
        assertFalse(result.isExecutable)
        assertTrue(result.threats.any { it.code == "SEC-CRIT-002" })
    }

    @Test
    fun scanGCode_customBashMCode_markedAsSuspiciousWarning() {
        val scriptGCode = """
            G21 G90
            G0 X10 Y10
            M105 (triggers external Linux shell script)
            G0 Z0
        """.trimIndent()

        val result = scanner.scanGCode("script_call.ngc", scriptGCode)

        assertEquals(ThreatLevel.SUSPICIOUS, result.threatLevel)
        assertTrue("Warnings still allow operator override review", result.isExecutable)
        assertTrue(result.threats.any { it.code == "SEC-WARN-005" })
    }

    @Test
    fun scanGCode_extremeNegativeZPlunge_flagsCollisionHazard() {
        val dangerousPlunge = """
            G21 G90
            G0 X0 Y0
            G1 Z-320.0 F1000 (Exceeds typical table height of -250mm)
        """.trimIndent()

        val result = scanner.scanGCode("crash.ngc", dangerousPlunge)

        assertEquals(ThreatLevel.SUSPICIOUS, result.threatLevel)
        assertTrue(result.threats.any { it.code == "SEC-WARN-007" })
    }

    @Test
    fun scanGCode_calculatesValidSha256Fingerprint() {
        val gcode = "G0 X0 Y0\nM2"
        val result = scanner.scanGCode("fingerprint_test.ngc", gcode)

        assertEquals(64, result.sha256Fingerprint.length)
        assertTrue(result.sha256Fingerprint.matches(Regex("^[a-f0-9]{64}$")))
    }
}
