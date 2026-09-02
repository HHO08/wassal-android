package com.wassal.app.di

import android.content.Context
import androidx.room.Room
import com.wassal.app.data.local.AppDatabase
import com.wassal.app.data.local.MessageDao
import com.wassal.app.data.local.ProfileDao
import com.wassal.app.data.local.RoomDao
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
        Room.databaseBuilder(context, AppDatabase::class.java, "wassal.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideProfileDao(db: AppDatabase): ProfileDao = db.profileDao()

    @Provides
    fun provideMessageDao(db: AppDatabase): MessageDao = db.messageDao()

    @Provides
    fun provideRoomDao(db: AppDatabase): RoomDao = db.roomDao()
}
