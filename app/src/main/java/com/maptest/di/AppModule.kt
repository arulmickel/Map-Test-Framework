package com.maptest.di

import android.content.Context
import androidx.room.Room
import com.maptest.data.local.LocationDao
import com.maptest.data.local.LocationDatabase
import com.maptest.data.remote.PlacesApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

// =============================================================================
// HILT DEPENDENCY INJECTION MODULE
// =============================================================================
// WHY DI IS CRITICAL FOR TESTING:
//
// Without DI (bad):
//   class MapViewModel {
//     val repo = LocationRepository(LocationDatabase.create(), RetrofitClient())
//   }
//   → Can't test ViewModel without a real database and real network!
//
// With DI (good):
//   class MapViewModel @Inject constructor(val repo: LocationRepository)
//   → In tests, Hilt injects a FakeRepository. No real DB or network.
//
// INTERVIEW QUESTION: "Explain how DI helps testability."
// ANSWER: "DI decouples creation from usage. My ViewModel says 'I need a
// Repository' but doesn't create it. In production, Hilt provides the real
// one. In tests, I provide a fake one. Same ViewModel code, different behavior."
//
// @Module: This class provides dependencies Hilt can't create automatically
// @InstallIn(SingletonComponent): These dependencies live as long as the app
// @Provides: Each function creates one dependency
// @Singleton: Only one instance exists across the whole app
// =============================================================================

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // =========================================================================
    // DATABASE
    // =========================================================================
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LocationDatabase {
        return Room.databaseBuilder(
            context,
            LocationDatabase::class.java,
            "maptest_database"
        ).build()
    }

    @Provides
    @Singleton
    fun provideLocationDao(database: LocationDatabase): LocationDao {
        return database.locationDao()
    }

    // =========================================================================
    // NETWORKING
    // =========================================================================
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.example.com/v1/") // Replace with real API
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun providePlacesApiService(retrofit: Retrofit): PlacesApiService {
        return retrofit.create(PlacesApiService::class.java)
    }
}
