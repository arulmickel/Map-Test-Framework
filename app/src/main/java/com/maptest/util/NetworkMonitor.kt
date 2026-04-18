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

// =============================================================================
// NETWORK MONITOR
// =============================================================================
// WHY THIS EXISTS:
// A maps app MUST handle offline scenarios gracefully. Users lose connectivity
// in tunnels, basements, airplanes, rural areas. This monitor provides a
// reactive StateFlow of network state so the UI can adapt instantly.
//
// WHY StateFlow (not Flow):
// - Always has a current value → the UI can render immediately on collect
// - `value` is readable without suspending → handy for one-shot checks
// - Hot: every observer sees the same state at the same time
// - Testable: the fake just exposes a MutableStateFlow we can poke
//
// WHY AN INTERFACE:
// The production code depends on the `NetworkMonitor` interface, not the
// real class. In tests we swap in `FakeNetworkMonitor` via Hilt's @BindValue.
// Without the interface, we'd be stuck subclassing a class that registers
// real Android NetworkCallbacks in its constructor — painful and flaky.
//
// INTERVIEW QUESTION: "How do you test offline behavior?"
// ANSWER: "I extract NetworkMonitor as an interface with a StateFlow<Boolean>.
// Production uses a real impl that bridges ConnectivityManager callbacks into
// the StateFlow. Tests use a FakeNetworkMonitor with setOnline(true/false),
// injected via Hilt's @BindValue. My ViewModel observes the interface, so
// the same code runs in both places — the only difference is who's pushing
// values into the flow."
// =============================================================================

interface NetworkMonitor {
    /** Hot stream of connectivity state. Always has a current value. */
    val isOnline: StateFlow<Boolean>

    /** Non-suspending snapshot. Equivalent to `isOnline.value`. */
    fun isCurrentlyOnline(): Boolean = isOnline.value
}

// =============================================================================
// REAL IMPLEMENTATION
// =============================================================================
// Registers a single NetworkCallback at construction (app scope) and forwards
// every change into a MutableStateFlow. Because this is @Singleton, the
// callback lives for the lifetime of the process — no unregister needed.
// =============================================================================

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

// =============================================================================
// FAKE IMPLEMENTATION (for tests)
// =============================================================================
// Lives in `main` (not `androidTest`) so both unit tests and instrumented
// tests can use it. It's a plain class — no Android framework dependencies —
// so JVM unit tests can construct it directly.
//
// USAGE IN INSTRUMENTED TESTS:
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
//
// INTERVIEW QUESTION: "Why does the fake live in main, not androidTest?"
// ANSWER: "So both the JVM unit tests (src/test) and the instrumented tests
// (src/androidTest) can share the same fake. Putting it in androidTest would
// make it invisible to unit tests; putting it in a shared testFixtures module
// is overkill for one class."
// =============================================================================

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
