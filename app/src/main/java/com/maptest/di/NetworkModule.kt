package com.maptest.di

import com.maptest.util.NetworkMonitor
import com.maptest.util.RealNetworkMonitor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// =============================================================================
// NETWORK MODULE
// =============================================================================
// WHY THIS IS SEPARATE FROM AppModule:
//
// Tests replace NetworkMonitor with a FakeNetworkMonitor using:
//
//   @UninstallModules(NetworkModule::class)
//   @HiltAndroidTest
//   class OfflineModeTest : BaseTestCase() {
//       @BindValue val networkMonitor: NetworkMonitor = FakeNetworkMonitor()
//   }
//
// @UninstallModules removes an ENTIRE Hilt module from the test graph. If
// this binding lived inside AppModule, uninstalling it would also wipe out
// the database, Retrofit, OkHttp, and PlacesApiService bindings. Splitting
// the NetworkMonitor binding into its own tiny module keeps the blast radius
// of @UninstallModules exactly one binding wide.
//
// INTERVIEW QUESTION: "How do you swap a Hilt dependency in tests?"
// ANSWER: "Two main options. For simple cases I use @BindValue on a field in
// the test class — it binds that instance into the Hilt graph. But if the
// dependency already has a production binding, I first @UninstallModules the
// module that provides it, then @BindValue my fake. I keep production Hilt
// modules small and focused so uninstalling one doesn't break unrelated
// bindings."
// =============================================================================

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkModule {

    @Binds
    @Singleton
    abstract fun bindNetworkMonitor(impl: RealNetworkMonitor): NetworkMonitor
}
