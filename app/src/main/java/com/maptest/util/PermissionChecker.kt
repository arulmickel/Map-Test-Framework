package com.maptest.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

// Three runtime permissions matter for this app:
//   - ACCESS_FINE_LOCATION        (always — "show me where I am")
//   - ACCESS_BACKGROUND_LOCATION  (Android 10+, geofencing)
//   - POST_NOTIFICATIONS          (Android 13+, geofence/nav alerts)
//
// API 33 promoted POST_NOTIFICATIONS from implicit to runtime; API 34 added
// partial media grants and stricter foreground service types; API 35
// tightened background location prompts further.
//
// The interface exists so tests can flip each permission between
// GRANTED / DENIED / SHOULD_SHOW_RATIONALE — `ContextCompat` alone cannot
// be faked. Production uses the Real impl backed by ContextCompat; tests
// inject a Fake via Hilt @BindValue.

enum class PermissionStatus {
    /** User granted the permission — app can use the protected API. */
    GRANTED,

    /** User has not granted yet, or denied without picking "don't ask again". */
    DENIED,

    /**
     * User denied once and we should show an in-app rationale before asking
     * again. Corresponds to ActivityCompat.shouldShowRequestPermissionRationale.
     */
    SHOULD_SHOW_RATIONALE,

    /**
     * Not applicable on this API level. Used for POST_NOTIFICATIONS on
     * API < 33 and ACCESS_BACKGROUND_LOCATION on API < 29. The ViewModel
     * should treat this as "implicitly granted, nothing to prompt".
     */
    NOT_APPLICABLE
}

interface PermissionChecker {
    val fineLocation: StateFlow<PermissionStatus>
    val backgroundLocation: StateFlow<PermissionStatus>
    val postNotifications: StateFlow<PermissionStatus>

    /**
     * Re-read the current permission state from the system. Call after
     * returning from a system permission dialog or from app settings.
     * The real impl updates its StateFlows; the fake is a no-op (tests
     * drive state with setStatus instead).
     */
    fun refresh()
}

// =============================================================================
// REAL IMPLEMENTATION
// =============================================================================
// Reads from ContextCompat.checkSelfPermission at construction and on every
// refresh() call. Note: this impl can only distinguish GRANTED vs DENIED —
// SHOULD_SHOW_RATIONALE requires an Activity reference (not an Application
// Context), which we can't inject here. The activity layer is responsible
// for upgrading DENIED → SHOULD_SHOW_RATIONALE when it sees fit; this impl
// intentionally keeps the simpler app-scope check.
// =============================================================================

@Singleton
class RealPermissionChecker @Inject constructor(
    @ApplicationContext private val context: Context
) : PermissionChecker {

    private val _fineLocation = MutableStateFlow(readFineLocation())
    override val fineLocation: StateFlow<PermissionStatus> = _fineLocation.asStateFlow()

    private val _backgroundLocation = MutableStateFlow(readBackgroundLocation())
    override val backgroundLocation: StateFlow<PermissionStatus> = _backgroundLocation.asStateFlow()

    private val _postNotifications = MutableStateFlow(readPostNotifications())
    override val postNotifications: StateFlow<PermissionStatus> = _postNotifications.asStateFlow()

    override fun refresh() {
        _fineLocation.value = readFineLocation()
        _backgroundLocation.value = readBackgroundLocation()
        _postNotifications.value = readPostNotifications()
    }

    private fun readFineLocation(): PermissionStatus =
        check(Manifest.permission.ACCESS_FINE_LOCATION)

    private fun readBackgroundLocation(): PermissionStatus =
        // ACCESS_BACKGROUND_LOCATION is API 29+. Below that it's implicit.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            check(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            PermissionStatus.NOT_APPLICABLE
        }

    private fun readPostNotifications(): PermissionStatus =
        // POST_NOTIFICATIONS is API 33+. Below that, notifications are
        // allowed by default without a runtime prompt.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            check(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            PermissionStatus.NOT_APPLICABLE
        }

    private fun check(permission: String): PermissionStatus {
        val granted = ContextCompat.checkSelfPermission(context, permission) ==
            PackageManager.PERMISSION_GRANTED
        return if (granted) PermissionStatus.GRANTED else PermissionStatus.DENIED
    }
}

// =============================================================================
// FAKE IMPLEMENTATION (for tests)
// =============================================================================
// Exposes per-permission setters so tests can walk the full state matrix.
// Lives in `main` so both JVM unit tests and instrumented tests can share it,
// same pattern as FakeNetworkMonitor.
//
// USAGE IN INSTRUMENTED TESTS:
//
//   @UninstallModules(PermissionModule::class)
//   @HiltAndroidTest
//   class PermissionTest : BaseTestCase() {
//       @BindValue @JvmField
//       val permissionChecker: PermissionChecker = FakePermissionChecker()
//
//       @Test fun locationDenied_showsRationale() {
//           (permissionChecker as FakePermissionChecker)
//               .setFineLocation(PermissionStatus.SHOULD_SHOW_RATIONALE)
//           // ...
//       }
//   }
// =============================================================================

class FakePermissionChecker(
    fineLocation: PermissionStatus = PermissionStatus.GRANTED,
    backgroundLocation: PermissionStatus = PermissionStatus.GRANTED,
    postNotifications: PermissionStatus = PermissionStatus.GRANTED
) : PermissionChecker {

    private val _fineLocation = MutableStateFlow(fineLocation)
    override val fineLocation: StateFlow<PermissionStatus> = _fineLocation.asStateFlow()

    private val _backgroundLocation = MutableStateFlow(backgroundLocation)
    override val backgroundLocation: StateFlow<PermissionStatus> = _backgroundLocation.asStateFlow()

    private val _postNotifications = MutableStateFlow(postNotifications)
    override val postNotifications: StateFlow<PermissionStatus> = _postNotifications.asStateFlow()

    fun setFineLocation(status: PermissionStatus) { _fineLocation.value = status }
    fun setBackgroundLocation(status: PermissionStatus) { _backgroundLocation.value = status }
    fun setPostNotifications(status: PermissionStatus) { _postNotifications.value = status }

    /** No-op — tests drive state with the per-permission setters above. */
    override fun refresh() = Unit
}
