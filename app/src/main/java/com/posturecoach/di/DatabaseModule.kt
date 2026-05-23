package com.posturecoach.di

import android.content.Context
import androidx.room.Room
import com.posturecoach.data.db.AppDatabase
import com.posturecoach.data.db.ActivityLogDao
import com.posturecoach.data.db.PostureScanDao
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
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "posturecoach.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun providePostureScanDao(db: AppDatabase): PostureScanDao = db.postureScanDao()

    @Provides
    fun provideActivityLogDao(db: AppDatabase): ActivityLogDao = db.activityLogDao()
}
