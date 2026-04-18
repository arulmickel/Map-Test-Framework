package com.maptest.di

import com.maptest.util.PermissionChecker
import com.maptest.util.RealPermissionChecker
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// =============================================================================
// PERMISSION MODULE
// =============================================================================
// Same tiny-module pattern as NetworkModule: isolated so permission tests can
// @UninstallModules(PermissionModule::class) without wiping unrelated
// bindings. See NetworkModule.kt for the full rationale.
// =============================================================================

@Module
@InstallIn(SingletonComponent::class)
abstract class PermissionModule {

    @Binds
    @Singleton
    abstract fun bindPermissionChecker(impl: RealPermissionChecker): PermissionChecker
}
