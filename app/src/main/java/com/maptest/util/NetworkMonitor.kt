package com.maptest.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

// Reactive StateFlow of network state. Production code depends on the
// `NetworkMonitor` interface; tests swap in `FakeNetworkMonitor` via Hilt's
// @BindValue. The interface boundary keeps the real ConnectivityManager
// callback registration out of test code.
//
// StateFlow (not Flow) because the UI needs an immediate value on collect
// and `value` is readable without suspending.

interface NetworkMonitor {
    /** Hot stream of connectivity state. Always has a current value. */
    val isOnline: StateFlow<Boolean>

    /** Non-suspending snapshot. Equivalent to `isOnline.value`. */
    fun isCurrentlyOnline(): Boolean = isOnline.value
}

// Registers a single NetworkCallback at construction (app scope) and forwards
// every change into a MutableStateFlow. @Singleton keeps the callback alive
// for the lifetime of the process — no unregister needed.

@Singleton
class RealNetworkMonitor @Inject constructor(
    @ApplicationContext context: Context
) : NetworkMonitor {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isOnline = MutableStateFlow(readCurrentState())
    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    init {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(
            request,
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    _isOnline.value = true
                }

                override fun onLost(network: Network) {
                    _isOnline.value = false
                }

                override fun onCapabilitiesChanged(
                    network: Network,
                    capabilities: NetworkCapabilities
                ) {
                    _isOnline.value = capabilities.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_INTERNET
                    )
                }
            }
        )
    }

    private fun readCurrentState(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}

// Test fake. Lives in `main` (not `androidTest`) so both JVM unit tests and
// instrumented tests can construct it — no Android framework dependencies.
//
//   @UninstallModules(NetworkModule::class)
//   @HiltAndroidTest
//   class OfflineModeTest : BaseTestCase() {
//       @BindValue @JvmField
//       val networkMonitor: NetworkMonitor = FakeNetworkMonitor(initialOnline = true)
//
//       @Test fun goOffline() {
//           (networkMonitor as FakeNetworkMonitor).setOnline(false)
//           // ...
//       }
//   }

class FakeNetworkMonitor(initialOnline: Boolean = true) : NetworkMonitor {

    private val _isOnline = MutableStateFlow(initialOnline)
    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    /**
     * Programmatically toggle connectivity. Emits to every observer.
     * Call this from tests to simulate going offline/online.
     */
    fun setOnline(online: Boolean) {
        _isOnline.value = online
    }
}
