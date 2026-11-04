// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 [cenliey]

package com.gamepad.controller.di

import android.content.Context
import com.gamepad.controller.data.config.ConnectionConfig
import com.gamepad.controller.data.service.GamepadInputManager
import com.gamepad.controller.data.service.WssJsonControlChannel
import com.gamepad.controller.domain.service.ControlChannel
import com.gamepad.controller.domain.service.GamepadInputService
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideControlChannel(
        @ApplicationContext context: Context,
        config: ConnectionConfig
    ): ControlChannel = WssJsonControlChannel(context, config)
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
