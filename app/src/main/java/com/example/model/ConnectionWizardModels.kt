package com.example.model

enum class ConnectionStepId {
    INTERFACE_DETECTION,
    LINUX_INI_CONFIG,
    NETWORK_PING_TEST,
    AUTHENTICATION_HANDSHAKE,
    SECURITY_ESTOP_CHECK
}

enum class InterfaceType {
    WIFI_LAN,
    ETHERNET_DIRECT,
    USB_HOTSPOT,
    OFFLINE_SIMULATOR
}

data class ConnectionStepInfo(
    val stepId: ConnectionStepId,
    val title: String,
    val subtitle: String,
    val instructions: List<String>,
    val codeSnippet: String? = null,
    val isCrucial: Boolean = true
)
