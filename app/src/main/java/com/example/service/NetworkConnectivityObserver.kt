package com.example.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

interface ConnectivityObserver {
    fun observe(): Flow<Status>

    enum class Status {
        Available, Weak, Unavailable, Losing, Lost
    }
}

class NetworkConnectivityObserver(
    context: Context,
): ConnectivityObserver {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    override fun observe(): Flow<ConnectivityObserver.Status> {
        return callbackFlow {
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    launch { send(ConnectivityObserver.Status.Available) }
                }

                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities,
                ) {
                    super.onCapabilitiesChanged(network, networkCapabilities)
                    val signalStrength = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        networkCapabilities.signalStrength
                    } else {
                        0
                    }
                    val isWeakSignal = signalStrength != 0 && signalStrength <= -80
                    if (isWeakSignal) {
                        launch { send(ConnectivityObserver.Status.Weak) }
                    }
                }

                override fun onLosing(network: Network, maxMsToLive: Int) {
                    super.onLosing(network, maxMsToLive)
                    launch { send(ConnectivityObserver.Status.Losing) }
                }

                override fun onLost(network: Network) {
                    super.onLost(network)
                    launch { send(ConnectivityObserver.Status.Lost) }
                }

                override fun onUnavailable() {
                    super.onUnavailable()
                    launch { send(ConnectivityObserver.Status.Unavailable) }
                }
            }

            try {
                connectivityManager.registerDefaultNetworkCallback(callback)
            } catch (_: Exception) {
                launch { send(ConnectivityObserver.Status.Available) }
            }
            awaitClose {
                try {
                    connectivityManager.unregisterNetworkCallback(callback)
                } catch (_: Exception) {}
            }
        }.distinctUntilChanged()
    }
}
