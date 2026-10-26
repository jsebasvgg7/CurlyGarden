package com.curlygarden.app.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

const val OFFLINE_MESSAGE = "Sin conexión. Inténtalo de nuevo"

object NetworkMonitor {
    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()
    private var started = false

    @Synchronized
    fun start(context: Context) {
        if (started) return
        started = true
        val manager = context.applicationContext
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        _isOnline.value = manager.activeNetwork
            ?.let { manager.getNetworkCapabilities(it) }
            ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        runCatching {
            manager.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    _isOnline.value = true
                }

                override fun onLost(network: Network) {
                    _isOnline.value = false
                }
            })
        }
    }
}

fun MutableStateFlow<String?>.rejectIfOffline(): Boolean {
    if (NetworkMonitor.isOnline.value) return false
    value = OFFLINE_MESSAGE
    return true
}
