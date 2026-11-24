package com.example.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

interface ConnectivityObserver {
    val isSystemOnline: StateFlow<Boolean>
    val simulatedOffline: StateFlow<Boolean>
    val isOnline: StateFlow<Boolean>
    fun setSimulatedOffline(offline: Boolean)
}

/**
 * Observes Android's active network state automatically via [ConnectivityManager].
 * Also supports an explicit [setSimulatedOffline] override for testing offline SMS/tel: fallback
 * on devices/emulators with persistent network interfaces.
 */
class AndroidConnectivityObserver(context: Context) : ConnectivityObserver {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isSystemOnline = MutableStateFlow(checkCurrentConnectivity())
    override val isSystemOnline: StateFlow<Boolean> = _isSystemOnline.asStateFlow()

    private val _simulatedOffline = MutableStateFlow(false)
    override val simulatedOffline: StateFlow<Boolean> = _simulatedOffline.asStateFlow()

    override val isOnline: StateFlow<Boolean> = combine(
        _isSystemOnline,
        _simulatedOffline
    ) { systemOnline, forcedOffline ->
        systemOnline && !forcedOffline
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = _isSystemOnline.value
    )

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isSystemOnline.value = checkCurrentConnectivity()
        }

        override fun onLost(network: Network) {
            _isSystemOnline.value = checkCurrentConnectivity()
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            _isSystemOnline.value = hasUsableInternet(networkCapabilities)
        }
    }

    init {
        runCatching {
            connectivityManager.registerDefaultNetworkCallback(networkCallback)
        }
    }

    override fun setSimulatedOffline(offline: Boolean) {
        _simulatedOffline.value = offline
    }

    private fun checkCurrentConnectivity(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(network) ?: return false
        return hasUsableInternet(caps)
    }

    private fun hasUsableInternet(caps: NetworkCapabilities): Boolean {
        val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val isValidated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        val hasTransport = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
        return hasInternet && (isValidated || hasTransport)
    }
}
