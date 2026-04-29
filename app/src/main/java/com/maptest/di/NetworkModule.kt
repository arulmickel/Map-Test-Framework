package com.maptest.di

import com.maptest.util.NetworkMonitor
import com.maptest.util.RealNetworkMonitor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Lives in its own module so tests can @UninstallModules(NetworkModule::class)
// and @BindValue a FakeNetworkMonitor without wiping out the database, Retrofit,
// OkHttp, and PlacesApiService bindings that AppModule provides.
//
//   @UninstallModules(NetworkModule::class)
//   @HiltAndroidTest
//   class OfflineModeTest : BaseTestCase() {
//       @BindValue val networkMonitor: NetworkMonitor = FakeNetworkMonitor()
//   }

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkModule {

    @Binds
    @Singleton
    abstract fun bindNetworkMonitor(impl: RealNetworkMonitor): NetworkMonitor
}
