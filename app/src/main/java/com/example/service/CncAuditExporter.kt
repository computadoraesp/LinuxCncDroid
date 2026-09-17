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
        val now = Date()
        val builder = StringBuilder()

        builder.appendLine("================================================================================")
        builder.appendLine("                     LINUXCNC DROID - INFORME DE AUDITORÍA INDUSTRIAL")
        builder.appendLine("================================================================================")
        builder.appendLine("Máquina:             ${capabilities.machineName}")
        builder.appendLine("Fecha de Generación: ${humanFormat.format(now)} (${isoFormat.format(now)})")
        builder.appendLine("Estado de Máquina:   ${machineState.name}")
        builder.appendLine("Arquitectura HAL:    ${capabilities.architecture.name}")
        builder.appendLine("Soporte EtherCAT:    ${if (capabilities.hasEtherCat) "ACTIVO" else "NO APLICABLE"}")
        builder.appendLine("--------------------------------------------------------------------------------")
        builder.appendLine("1. TELEMETRÍA DE EJES CINEMÁTICOS")
        builder.appendLine("--------------------------------------------------------------------------------")
        axes.toSortedMap().forEach { (name, axis) ->
            val homedStr = if (axis.isHomed) "HOMED" else "NO-HOMED"
            builder.appendLine("  Eje $name: Pos. Trabajo=${String.format(Locale.US, "%+08.4f", axis.workPos)} mm | Pos. Máquina=${String.format(Locale.US, "%+08.4f", axis.machinePos)} mm | Ref=$homedStr | Lím.=[${axis.minLimit} .. ${axis.maxLimit}] | Carga=${axis.loadTorquePct}% | Temp=${axis.driveTempC}°C")
        }

        builder.appendLine("--------------------------------------------------------------------------------")
        builder.appendLine("2. CABEZAL / HUSILLO Y AVANCES")
        builder.appendLine("--------------------------------------------------------------------------------")
        builder.appendLine("  Husillo: ${if (spindle.isEnabled) "EN MARCHA" else "DETENIDO"} | RPM Actual=${spindle.actualRpm.toInt()} (Cmd: ${spindle.commandedRpm.toInt()}) | Override=${spindle.overridePct}% | Carga=${spindle.loadAmps}A")
        builder.appendLine("  Avance:  ${feed.actualFeed.toInt()} mm/min (Cmd: ${feed.commandedFeed.toInt()}) | Override=${feed.feedOverridePct}% | Override Rápido=${feed.rapidOverridePct}%")

        if (capabilities.hasEtherCat) {
            builder.appendLine("--------------------------------------------------------------------------------")
            builder.appendLine("3. DIAGNÓSTICO BUS ETHERCAT MAESTRO & ESCLAVOS")
            builder.appendLine("--------------------------------------------------------------------------------")
            builder.appendLine("  Maestro: ${etherCatMaster.masterState} | Esclavos=${etherCatMaster.slaveCount} | Ciclo=${etherCatMaster.busCycleTimeUs}µs | Pérdidas=${etherCatMaster.packetLossPct}% | Offset DC=${etherCatMaster.dcOffsetNs}ns")
            etherCatSlaves.forEach { slave ->
                builder.appendLine("  [ID ${slave.slaveIndex}] ${slave.name} (${slave.state}): Par=${String.format(Locale.US, "%.1f", slave.actualTorquePct)}% | Temp=${String.format(Locale.US, "%.1f", slave.driveTempC)}°C | Alarma=${slave.alarmCode}")
            }
        }

        builder.appendLine("--------------------------------------------------------------------------------")
        builder.appendLine("4. HISTORIAL DE ALARMAS Y REGISTRO DE EVENTOS (ÚLTIMOS ${logs.size})")
        builder.appendLine("--------------------------------------------------------------------------------")
        if (logs.isEmpty()) {
            builder.appendLine("  (Sin registros de alarma o eventos en buffer)")
        } else {
            logs.forEach { log ->
                val timeStr = humanFormat.format(Date(log.timestamp))
                builder.appendLine("  [$timeStr] [${log.severity.name.padEnd(8)}] [${log.tag.padEnd(10)}] ${log.message}")
            }
        }
        builder.appendLine("================================================================================")
        builder.appendLine("FIN DEL INFORME - HMI INDUSTRIAL LINUXCNC")
        builder.appendLine("================================================================================")

        return builder.toString()
    }
}
