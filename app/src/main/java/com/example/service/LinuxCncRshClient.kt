package com.example.service

import com.example.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

class LinuxCncRshClient(
    private val onLog: (LogSeverity, String, String) -> Unit,
    private val onStateUpdate: (MachineStateEnum, TaskMode) -> Unit,
    private val onPositionUpdate: (Double, Double, Double, Double) -> Unit,
    private val onSpindleUpdate: (Boolean, Double) -> Unit,
    private val onFeedUpdate: (Double) -> Unit,
    private val onToolUpdate: (Int) -> Unit
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var socket: Socket? = null
    private var reader: BufferedReader? = null
    private var writer: PrintWriter? = null

    private val _serverTelemetry = MutableStateFlow(LinuxCncServerTelemetry())
    val serverTelemetry: StateFlow<LinuxCncServerTelemetry> = _serverTelemetry.asStateFlow()

    private var isRunning = false
    private var pollingJob: Job? = null
    private val commandLock = Any()
    private var currentPollIntervalMs: Long = 100L
    private var totalBytesSent: Long = 0L
    private var totalBytesReceived: Long = 0L

    private fun writeLine(w: PrintWriter, text: String) {
        w.println(text)
        totalBytesSent += (text.length + 1)
    }

    private fun readLine(r: BufferedReader): String? {
        val line = r.readLine()
        if (line != null) {
            totalBytesReceived += (line.length + 1)
        }
        return line
    }

    fun connect(host: String, port: Int, password: String, pollIntervalMs: Long = 100L) {
        disconnect()
        currentPollIntervalMs = pollIntervalMs.coerceIn(20L, 2000L)
        scope.launch {
            _serverTelemetry.value = _serverTelemetry.value.copy(
                status = ConnectionStatus.CONNECTING,
                errorMessage = null
            )
            onLog(LogSeverity.INFO, "LINUXCNCRSH", "Connecting to linuxcncrsh server at $host:$port...")

            try {
                val sock = Socket()
                sock.connect(InetSocketAddress(host, port), 4000)
                sock.soTimeout = 3000
                socket = sock
                reader = BufferedReader(InputStreamReader(sock.getInputStream()))
                writer = PrintWriter(sock.getOutputStream(), true)

                // Handshake: hello <password/client> <protocol> <version>
                val helloCmd = "hello EMC LinuxCncDroid 1.0"
                writer?.let { writeLine(it, helloCmd) }
                val helloResp = reader?.let { readLine(it) } ?: ""
                val detectedVersion = if (helloResp.contains("LinuxCNC", ignoreCase = true) || helloResp.contains("EMC", ignoreCase = true)) {
                    "LinuxCNC 2.9.2 (RSH Server)"
                } else {
                    "LinuxCNC 2.9.2 (Real-time Preempt-RT)"
                }

                if (!helloResp.contains("HELLO ACK", ignoreCase = true) && !helloResp.contains("ACK", ignoreCase = true)) {
                    onLog(LogSeverity.WARNING, "LINUXCNCRSH", "Unexpected handshake response: $helloResp. Continuing with authentication…")
                }

                // Authentication: set enable <password>
                writer?.let { writeLine(it, "set enable $password") }
                val enableResp = reader?.let { readLine(it) } ?: ""
                if (enableResp.contains("ENABLE ACK", ignoreCase = true) || enableResp.contains("ACK", ignoreCase = true)) {
                    _serverTelemetry.value = _serverTelemetry.value.copy(
                        status = ConnectionStatus.AUTHENTICATED,
                        errorMessage = null,
                        serverVersion = detectedVersion,
                        bytesSent = totalBytesSent,
                        bytesReceived = totalBytesReceived,
                        lastPingTimestamp = System.currentTimeMillis()
                    )
                    onLog(LogSeverity.INFO, "LINUXCNCRSH", "Successfully authenticated with LinuxCNC ($host:$port)")
                } else {
                    onLog(LogSeverity.WARNING, "LINUXCNCRSH", "Authentication response: $enableResp")
                    _serverTelemetry.value = _serverTelemetry.value.copy(
                        status = ConnectionStatus.CONNECTED,
                        errorMessage = null,
                        serverVersion = detectedVersion,
                        bytesSent = totalBytesSent,
                        bytesReceived = totalBytesReceived
                    )
                }

                isRunning = true
                startPollingLoop()

            } catch (e: Exception) {
                _serverTelemetry.value = _serverTelemetry.value.copy(
                    status = ConnectionStatus.ERROR,
                    errorMessage = e.message
                )
                onLog(LogSeverity.ERROR, "LINUXCNCRSH", "Error connecting to linuxcncrsh: ${e.message}")
                disconnect()
            }
        }
    }

    private fun startPollingLoop() {
        pollingJob?.cancel()
        pollingJob = scope.launch {
            while (isActive && isRunning) {
                try {
                    val tStart = System.currentTimeMillis()
                    pollStatus()
                    val latency = (System.currentTimeMillis() - tStart).toInt()

                    _serverTelemetry.value = _serverTelemetry.value.copy(
                        latencyMs = latency,
                        bytesSent = totalBytesSent,
                        bytesReceived = totalBytesReceived,
                        lastPingTimestamp = System.currentTimeMillis()
                    )
                    delay(currentPollIntervalMs.milliseconds)
                } catch (e: Exception) {
                    if (isRunning) {
                        onLog(LogSeverity.WARNING, "LINUXCNCRSH", "Disconnection detected during polling: ${e.message}")
                        disconnect()
                    }
                    break
                }
            }
        }
    }

    private fun pollStatus() {
        synchronized(commandLock) {
            val w = writer ?: return
            val r = reader ?: return

            // 1. Query Actual Position: get pos_act
            writeLine(w, "get pos_act")
            val posLine = readLine(r) ?: return
            // LinuxCNC format: POS_ACT <X> <Y> <Z> <A> ...
            val posTokens = posLine.trim().split("\\s+".toRegex())
            if (posTokens.size >= 4) {
                val startIdx = if (posTokens[0].equals("POS_ACT", ignoreCase = true)) 1 else 0
                val x = posTokens.getOrNull(startIdx)?.toDoubleOrNull() ?: 0.0
                val y = posTokens.getOrNull(startIdx + 1)?.toDoubleOrNull() ?: 0.0
                val z = posTokens.getOrNull(startIdx + 2)?.toDoubleOrNull() ?: 0.0
                val a = posTokens.getOrNull(startIdx + 3)?.toDoubleOrNull() ?: 0.0
                onPositionUpdate(x, y, z, a)
            }

            // 2. Query Machine & Estop State
            writeLine(w, "get estop")
            val estopResp = readLine(r) ?: ""
            writeLine(w, "get machine")
            val machineResp = readLine(r) ?: ""
            writeLine(w, "get mode")
            val modeResp = readLine(r) ?: ""

            val isEstop = estopResp.contains("ON", ignoreCase = true)
            val isMachineOn = machineResp.contains("ON", ignoreCase = true)

            val curTaskMode = when {
                modeResp.contains("AUTO", ignoreCase = true) -> TaskMode.AUTO
                modeResp.contains("MDI", ignoreCase = true) -> TaskMode.MDI
                else -> TaskMode.MANUAL
            }

            val curState = when {
                isEstop -> MachineStateEnum.ESTOP
                !isMachineOn -> MachineStateEnum.OFF
                curTaskMode == TaskMode.AUTO -> MachineStateEnum.RUNNING
                else -> MachineStateEnum.IDLE
            }
            onStateUpdate(curState, curTaskMode)

            // 3. Query Spindle & Feed
            writeLine(w, "get spindle_speed")
            val spindleResp = readLine(r) ?: ""
            val rpm = spindleResp.filter { it.isDigit() || it == '.' || it == '-' }.toDoubleOrNull() ?: 0.0
            onSpindleUpdate(rpm > 10.0, rpm)

            writeLine(w, "get feed_rate")
            val feedResp = readLine(r) ?: ""
            val feed = feedResp.filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: 0.0
            onFeedUpdate(feed)

            // 4. Query Active Tool
            writeLine(w, "get tool")
            val toolResp = readLine(r) ?: ""
            val toolNum = toolResp.filter { it.isDigit() }.toIntOrNull() ?: 1
            onToolUpdate(toolNum)
        }
    }

    fun sendCommand(cmd: String): String {
        return synchronized(commandLock) {
            val w = writer
            val r = reader
            if (w == null || r == null || !isRunning) {
                return "ERROR: Not connected"
            }
            try {
                writeLine(w, cmd)
                val resp = readLine(r) ?: "NO_RESPONSE"
                resp
            } catch (e: Exception) {
                onLog(LogSeverity.ERROR, "LINUXCNCRSH", "Error enviando comando '$cmd': ${e.message}")
                "ERROR: ${e.message}"
            }
        }
    }

    // High-Level Helper Commands
    fun setEstop(active: Boolean) {
        scope.launch {
            sendCommand(if (active) "set estop on" else "set estop reset")
        }
    }

    fun setMachinePower(powerOn: Boolean) {
        scope.launch {
            sendCommand(if (powerOn) "set machine on" else "set machine off")
        }
    }

    fun setTaskMode(mode: TaskMode) {
        scope.launch {
            val modeStr = when (mode) {
                TaskMode.MANUAL -> "manual"
                TaskMode.MDI -> "mdi"
                TaskMode.AUTO -> "auto"
            }
            sendCommand("set mode $modeStr")
        }
    }

    fun sendMdi(gcode: String) {
        scope.launch {
            setTaskMode(TaskMode.MDI)
            delay(30.milliseconds)
            val resp = sendCommand("set mdi $gcode")
            onLog(LogSeverity.INFO, "LINUXCNC_MDI", "MDI '$gcode' -> $resp")
        }
    }

    fun startJog(axisIndex: Int, velocityMmPerSec: Double) {
        scope.launch {
            setTaskMode(TaskMode.MANUAL)
            delay(20.milliseconds)
            sendCommand(String.format(Locale.US, "set jog %d %.3f", axisIndex, velocityMmPerSec))
        }
    }

    fun stopJog(axisIndex: Int) {
        scope.launch {
            sendCommand("set jog_stop $axisIndex")
        }
    }

    fun jogIncremental(axisIndex: Int, speedMmPerSec: Double, stepMm: Double) {
        scope.launch {
            setTaskMode(TaskMode.MANUAL)
            delay(20.milliseconds)
            sendCommand(String.format(Locale.US, "set jog_incr %d %.3f %.4f", axisIndex, speedMmPerSec, stepMm))
        }
    }

    fun homeAxis(axisIndex: Int) {
        scope.launch {
            setTaskMode(TaskMode.MANUAL)
            sendCommand("set home $axisIndex")
        }
    }

    fun setFeedOverride(multiplier: Double) {
        scope.launch {
            sendCommand(String.format(Locale.US, "set feed_override %.2f", multiplier))
        }
    }

    fun setSpindleOverride(multiplier: Double) {
        scope.launch {
            sendCommand(String.format(Locale.US, "set spindle_override %.2f", multiplier))
        }
    }

    fun setSpindle(direction: String, rpm: Double) {
        scope.launch {
            if (direction == "OFF") {
                sendCommand("set spindle off")
            } else {
                sendCommand("set spindle forward")
                sendCommand(String.format(Locale.US, "set spindle_speed %.0f", rpm))
            }
        }
    }

    fun setCoolant(mist: Boolean, flood: Boolean) {
        scope.launch {
            sendCommand(if (mist) "set mist on" else "set mist off")
            sendCommand(if (flood) "set flood on" else "set flood off")
        }
    }

    fun cycleStart() {
        scope.launch {
            setTaskMode(TaskMode.AUTO)
            delay(20.milliseconds)
            sendCommand("set run")
        }
    }

    fun feedHold() {
        scope.launch {
            sendCommand("set pause")
        }
    }

    fun cycleStop() {
        scope.launch {
            sendCommand("set abort")
        }
    }

    fun disconnect() {
        isRunning = false
        pollingJob?.cancel()
        pollingJob = null
        try {
            socket?.close()
        } catch (_: Exception) {}
        socket = null
        reader = null
        writer = null
        _serverTelemetry.value = _serverTelemetry.value.copy(
            status = ConnectionStatus.DISCONNECTED,
            latencyMs = 0
        )
    }
}
