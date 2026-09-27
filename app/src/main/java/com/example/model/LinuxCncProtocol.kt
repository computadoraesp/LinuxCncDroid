package com.example.model

import androidx.annotation.StringRes
import com.example.R

enum class LinuxCncProtocolType(
    @get:StringRes val displayNameRes: Int,
    val defaultPort: Int,
    @get:StringRes val descriptionRes: Int
) {
    LINUXCNCRSH_TCP(
        displayNameRes = R.string.protocol_linuxcncrsh_name,
        defaultPort = 5007,
        descriptionRes = R.string.protocol_linuxcncrsh_description
    ),
    WEBSOCKET_JSON(
        displayNameRes = R.string.protocol_websocket_name,
        defaultPort = 8000,
        descriptionRes = R.string.protocol_websocket_description
    ),
    SIMULATION_LOCAL(
        displayNameRes = R.string.protocol_sim_name,
        defaultPort = 0,
        descriptionRes = R.string.protocol_simulation_description
    )
}

data class LinuxCncConnectionConfig(
    val protocolType: LinuxCncProtocolType = LinuxCncProtocolType.SIMULATION_LOCAL,
    val hostIp: String = "192.168.1.100",
    val port: Int = 5007,
    val password: String = "EMC",
    val autoReconnect: Boolean = true,
    val pollIntervalMs: Long = 100L
)

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    AUTHENTICATED,
    ERROR
}

data class LinuxCncServerTelemetry(
    val status: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val latencyMs: Int = 0,
    val bytesReceived: Long = 0L,
    val bytesSent: Long = 0L,
    val serverVersion: String = "LinuxCNC 2.9.2 (Real-time Preempt-RT)",
    val lastPingTimestamp: Long = 0L,
    val errorMessage: String? = null
) {
    val isConnected: Boolean get() = status == ConnectionStatus.CONNECTED || status == ConnectionStatus.AUTHENTICATED
}
