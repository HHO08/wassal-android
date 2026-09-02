package com.wassal.app.di

import com.wassal.app.net.signaling.SignalingClient
import com.wassal.app.net.rtc.RtcManager
import com.wassal.app.util.AppConstants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    @Singleton
    fun provideSignalingUrl(): String = AppConstants.SIGNALING_URL

    @Provides
    @Singleton
    fun provideStunServers(): List<String> = AppConstants.STUN_SERVERS
}
