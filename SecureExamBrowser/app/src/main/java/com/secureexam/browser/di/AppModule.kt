package com.secureexam.browser.di

import android.content.Context
import com.secureexam.browser.data.remote.MoodleApiService
import com.secureexam.browser.security.KioskManager
import com.secureexam.browser.security.RootDetector
import com.secureexam.browser.security.ScreenMirrorDetector
import com.secureexam.browser.security.SecurityChecker
import com.secureexam.browser.security.ViolationLogger
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

/**
 * Hilt module providing security and network dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://placeholder.moodle.edu/") // Will be replaced dynamically
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideMoodleApiService(retrofit: Retrofit): MoodleApiService {
        return retrofit.create(MoodleApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideRootDetector(
        @ApplicationContext context: Context
    ): RootDetector {
        return RootDetector(context)
    }

    @Provides
    @Singleton
    fun provideScreenMirrorDetector(
        @ApplicationContext context: Context
    ): ScreenMirrorDetector {
        return ScreenMirrorDetector(context)
    }

    @Provides
    @Singleton
    fun provideSecurityChecker(
        @ApplicationContext context: Context,
        rootDetector: RootDetector,
        screenMirrorDetector: ScreenMirrorDetector
    ): SecurityChecker {
        return SecurityChecker(context, rootDetector, screenMirrorDetector)
    }

    @Provides
    @Singleton
    fun provideKioskManager(
        @ApplicationContext context: Context
    ): KioskManager {
        return KioskManager(context)
    }

    @Provides
    @Singleton
    fun provideViolationLogger(
        @ApplicationContext context: Context
    ): ViolationLogger {
        return ViolationLogger(context)
    }
}
