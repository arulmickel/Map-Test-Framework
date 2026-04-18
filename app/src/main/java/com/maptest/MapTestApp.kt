package com.maptest

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

// @HiltAndroidApp triggers Hilt's code generation and sets up the DI container.
// Every Hilt app needs exactly one of these.
@HiltAndroidApp
class MapTestApp : Application()
