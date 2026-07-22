package com.gamepad.controller.di

import com.gamepad.controller.data.service.GamepadInputManager
import com.gamepad.controller.data.service.UdpControlChannel
import com.gamepad.controller.domain.service.ControlChannel
import com.gamepad.controller.domain.service.GamepadInputService
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideControlChannel(): ControlChannel = UdpControlChannel()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ServiceModule {

    @Binds
    @Singleton
    abstract fun bindGamepadInputService(
        impl: GamepadInputManager
    ): GamepadInputService
}
