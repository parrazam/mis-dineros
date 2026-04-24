package com.parra.misdineros.di

import android.content.Context
import androidx.room.Room
import com.parra.misdineros.data.db.MisDinerosDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MisDinerosDatabase {
        return Room.databaseBuilder(
            context,
            MisDinerosDatabase::class.java,
            "mis_dineros.db",
        )
            .addCallback(MisDinerosDatabase.seedCallback)
            .build()
    }

    @Provides
    fun provideSubscriptionDao(db: MisDinerosDatabase) = db.subscriptionDao()

    @Provides
    fun provideCategoryDao(db: MisDinerosDatabase) = db.categoryDao()

    @Provides
    fun provideFxRateDao(db: MisDinerosDatabase) = db.fxRateDao()
}
