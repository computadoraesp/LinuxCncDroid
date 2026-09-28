package com.example.service

import com.example.model.AxisCoord
import com.example.model.CapabilitiesManifest
import com.example.model.CncEventLog
import com.example.model.EtherCatMasterInfo
import com.example.model.EtherCatSlaveInfo
import com.example.model.FeedInfo
import com.example.model.MachineStateEnum
import com.example.model.SpindleInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Generates structured, standard industrial audit logs for machine tool maintenance,
 * ISO 9001 quality audits, and root-cause fault diagnosis.
 */
object CncAuditExporter {

    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
    private val humanFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    fun generateTextReport(
        machineState: MachineStateEnum,
        capabilities: CapabilitiesManifest,
        axes: Map<String, AxisCoord>,
        spindle: SpindleInfo,
        feed: FeedInfo,
        etherCatMaster: EtherCatMasterInfo,
        etherCatSlaves: List<EtherCatSlaveInfo>,
        logs: List<CncEventLog>,
    ): String {
        val isEs = Locale.getDefault().language == "es"
        val now = Date()
        val builder = StringBuilder()

        builder.appendLine("================================================================================")
        builder.appendLine(if (isEs) "                     LINUXCNC DROID - INFORME DE AUDITORÍA INDUSTRIAL"
                           else "                     LINUXCNC DROID - INDUSTRIAL AUDIT REPORT")
        builder.appendLine("================================================================================")
        builder.appendLine(if (isEs) "Máquina:             ${capabilities.machineName}" else "Machine:             ${capabilities.machineName}")
        builder.appendLine(if (isEs) "Fecha de Generación: ${humanFormat.format(now)} (${isoFormat.format(now)})" else "Generation Date:     ${humanFormat.format(now)} (${isoFormat.format(now)})")
        builder.appendLine(if (isEs) "Estado de Máquina:   ${machineState.name}" else "Machine State:       ${machineState.name}")
        builder.appendLine(if (isEs) "Arquitectura HAL:    ${capabilities.architecture.name}" else "HAL Architecture:    ${capabilities.architecture.name}")
        builder.appendLine(if (isEs) "Soporte EtherCAT:    ${if (capabilities.hasEtherCat) "ACTIVO" else "NO APLICABLE"}" else "EtherCAT Support:    ${if (capabilities.hasEtherCat) "ACTIVE" else "NOT APPLICABLE"}")
        builder.appendLine("--------------------------------------------------------------------------------")
        builder.appendLine(if (isEs) "1. TELEMETRÍA DE EJES CINEMÁTICOS" else "1. KINEMATIC AXES TELEMETRY")
        builder.appendLine("--------------------------------------------------------------------------------")
        axes.toSortedMap().forEach { (name, axis) ->
            val homedStr = if (axis.isHomed) "HOMED" else "NO-HOMED"
            if (isEs) {
                builder.appendLine("  Eje $name: Pos. Trabajo=${String.format(Locale.US, "%+08.4f", axis.workPos)} mm | Pos. Máquina=${String.format(Locale.US, "%+08.4f", axis.machinePos)} mm | Ref=$homedStr | Lím.=[${axis.minLimit} .. ${axis.maxLimit}] | Carga=${axis.loadTorquePct}% | Temp=${axis.driveTempC}°C")
            } else {
                builder.appendLine("  Axis $name: Work Pos=${String.format(Locale.US, "%+08.4f", axis.workPos)} mm | Machine Pos=${String.format(Locale.US, "%+08.4f", axis.machinePos)} mm | Ref=$homedStr | Limits=[${axis.minLimit} .. ${axis.maxLimit}] | Load=${axis.loadTorquePct}% | Temp=${axis.driveTempC}°C")
            }
        }

        builder.appendLine("--------------------------------------------------------------------------------")
        builder.appendLine(if (isEs) "2. CABEZAL / HUSILLO Y AVANCES" else "2. SPINDLE & FEEDS")
        builder.appendLine("--------------------------------------------------------------------------------")
        if (isEs) {
            builder.appendLine("  Husillo: ${if (spindle.isEnabled) "EN MARCHA" else "DETENIDO"} | RPM Actual=${spindle.actualRpm.toInt()} (Cmd: ${spindle.commandedRpm.toInt()}) | Override=${spindle.overridePct}% | Carga=${spindle.loadAmps}A")
            builder.appendLine("  Avance:  ${feed.actualFeed.toInt()} mm/min (Cmd: ${feed.commandedFeed.toInt()}) | Override=${feed.feedOverridePct}% | Override Rápido=${feed.rapidOverridePct}%")
        } else {
            builder.appendLine("  Spindle: ${if (spindle.isEnabled) "RUNNING" else "STOPPED"} | Actual RPM=${spindle.actualRpm.toInt()} (Cmd: ${spindle.commandedRpm.toInt()}) | Override=${spindle.overridePct}% | Load=${spindle.loadAmps}A")
            builder.appendLine("  Feed:    ${feed.actualFeed.toInt()} mm/min (Cmd: ${feed.commandedFeed.toInt()}) | Override=${feed.feedOverridePct}% | Rapid Override=${feed.rapidOverridePct}%")
        }

        if (capabilities.hasEtherCat) {
            builder.appendLine("--------------------------------------------------------------------------------")
            builder.appendLine(if (isEs) "3. DIAGNÓSTICO BUS ETHERCAT MAESTRO & ESCLAVOS" else "3. ETHERCAT MASTER & SLAVES DIAGNOSTICS")
            builder.appendLine("--------------------------------------------------------------------------------")
            if (isEs) {
                builder.appendLine("  Maestro: ${etherCatMaster.masterState} | Esclavos=${etherCatMaster.slaveCount} | Ciclo=${etherCatMaster.busCycleTimeUs}µs | Pérdidas=${etherCatMaster.packetLossPct}% | Offset DC=${etherCatMaster.dcOffsetNs}ns")
                etherCatSlaves.forEach { slave ->
                    builder.appendLine("  [ID ${slave.slaveIndex}] ${slave.name} (${slave.state}): Par=${String.format(Locale.US, "%.1f", slave.actualTorquePct)}% | Temp=${String.format(Locale.US, "%.1f", slave.driveTempC)}°C | Alarma=${slave.alarmCode}")
                }
            } else {
                builder.appendLine("  Master: ${etherCatMaster.masterState} | Slaves=${etherCatMaster.slaveCount} | Cycle=${etherCatMaster.busCycleTimeUs}µs | Packet Loss=${etherCatMaster.packetLossPct}% | DC Offset=${etherCatMaster.dcOffsetNs}ns")
                etherCatSlaves.forEach { slave ->
                    builder.appendLine("  [ID ${slave.slaveIndex}] ${slave.name} (${slave.state}): Torque=${String.format(Locale.US, "%.1f", slave.actualTorquePct)}% | Temp=${String.format(Locale.US, "%.1f", slave.driveTempC)}°C | Alarm=${slave.alarmCode}")
                }
            }
        }

        builder.appendLine("--------------------------------------------------------------------------------")
        builder.appendLine(if (isEs) "4. HISTORIAL DE ALARMAS Y REGISTRO DE EVENTOS (ÚLTIMOS ${logs.size})" else "4. ALARM HISTORY & EVENT LOGS (LAST ${logs.size})")
        builder.appendLine("--------------------------------------------------------------------------------")
        if (logs.isEmpty()) {
            builder.appendLine(if (isEs) "  (Sin registros de alarma o eventos en buffer)" else "  (No alarms or events recorded in buffer)")
        } else {
            logs.forEach { log ->
                val timeStr = humanFormat.format(Date(log.timestamp))
                builder.appendLine("  [$timeStr] [${log.severity.name.padEnd(8)}] [${log.tag.padEnd(10)}] ${log.message}")
            }
        }
        builder.appendLine("================================================================================")
        builder.appendLine(if (isEs) "FIN DEL INFORME - HMI INDUSTRIAL LINUXCNC" else "END OF REPORT - LINUXCNC INDUSTRIAL HMI")
        builder.appendLine("================================================================================")

        return builder.toString()
    }
}
