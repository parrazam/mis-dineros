package com.parra.misdineros.di

import com.parra.misdineros.data.backup.BackupRepositoryImpl
import com.parra.misdineros.data.repository.CategoryRepositoryImpl
import com.parra.misdineros.data.repository.FxRepositoryImpl
import com.parra.misdineros.data.repository.SubscriptionRepositoryImpl
import com.parra.misdineros.data.settings.SettingsDataStore
import com.parra.misdineros.domain.repository.BackupRepository
import com.parra.misdineros.domain.repository.CategoryRepository
import com.parra.misdineros.domain.repository.FxRepository
import com.parra.misdineros.domain.repository.SettingsRepository
import com.parra.misdineros.domain.repository.SubscriptionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSubscriptionRepository(impl: SubscriptionRepositoryImpl): SubscriptionRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindFxRepository(impl: FxRepositoryImpl): FxRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsDataStore): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindBackupRepository(impl: BackupRepositoryImpl): BackupRepository
}
