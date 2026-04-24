package com.parra.misdineros.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

// WorkerFactory se provee automáticamente por hilt-work con @HiltWorker en cada Worker
@Module
@InstallIn(SingletonComponent::class)
object WorkerModule
