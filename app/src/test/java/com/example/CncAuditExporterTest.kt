package com.example

import com.example.model.AxisCoord
import com.example.model.CapabilitiesManifest
import com.example.model.CncEventLog
import com.example.model.EtherCatMasterInfo
import com.example.model.FeedInfo
import com.example.model.HardwareArchitecture
import com.example.model.LogSeverity
import com.example.model.MachineStateEnum
import com.example.model.SpindleInfo
import com.example.service.CncAuditExporter
import org.junit.Assert.assertTrue
import org.junit.Test

class CncAuditExporterTest {

    @Test
    fun generateTextReport_containsAllRequiredIndustrialSections() {
        val axes = mapOf(
            "X" to AxisCoord(name = "X", workPos = 125.4321, machinePos = 125.4321, isHomed = true),
            "Y" to AxisCoord(name = "Y", workPos = -45.0, machinePos = -45.0, isHomed = true),
            "Z" to AxisCoord(name = "Z", workPos = 15.0, machinePos = 15.0, isHomed = true),
        )
        val spindle = SpindleInfo(isEnabled = true, commandedRpm = 12000.0, actualRpm = 12000.0, overridePct = 100)
        val feed = FeedInfo(commandedFeed = 800.0, actualFeed = 800.0, feedOverridePct = 100, rapidOverridePct = 100)
        val capabilities = CapabilitiesManifest(architecture = HardwareArchitecture.ETHERCAT_DELTA, hasEtherCat = true)
        val master = EtherCatMasterInfo(masterState = "OP (Operational)", slaveCount = 4, busCycleTimeUs = 1000, packetLossPct = 0.0)
        val logs = listOf(
            CncEventLog(severity = LogSeverity.CRITICAL, tag = "SAFETY", message = "ESTOP Tripped by operator"),
            CncEventLog(severity = LogSeverity.INFO, tag = "CYCLE", message = "Program completed successfully"),
        )

        val report = CncAuditExporter.generateTextReport(
            machineState = MachineStateEnum.ON,
            capabilities = capabilities,
            axes = axes,
            spindle = spindle,
            feed = feed,
            etherCatMaster = master,
            etherCatSlaves = emptyList(),
            logs = logs,
        )

        assertTrue(report.contains("INFORME DE AUDITORÍA INDUSTRIAL"))
        assertTrue(report.contains("TELEMETRÍA DE EJES CINEMÁTICOS"))
        assertTrue(report.contains("Eje X: Pos. Trabajo=+125.4321 mm"))
        assertTrue(report.contains("CABEZAL / HUSILLO Y AVANCES"))
        assertTrue(report.contains("DIAGNÓSTICO BUS ETHERCAT"))
        assertTrue(report.contains("HISTORIAL DE ALARMAS"))
        assertTrue(report.contains("ESTOP Tripped by operator"))
    }
}
