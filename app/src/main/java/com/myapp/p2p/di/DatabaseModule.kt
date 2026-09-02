package com.myapp.p2p.di

import android.content.Context
import androidx.room.Room
import com.myapp.p2p.data.local.AppDatabase
import com.myapp.p2p.data.local.MessageDao
import com.myapp.p2p.data.local.ProfileDao
import com.myapp.p2p.data.local.RoomDao
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
        Room.databaseBuilder(context, AppDatabase::class.java, "myapp.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideProfileDao(db: AppDatabase): ProfileDao = db.profileDao()

    @Provides
    fun provideMessageDao(db: AppDatabase): MessageDao = db.messageDao()

    @Provides
    fun provideRoomDao(db: AppDatabase): RoomDao = db.roomDao()
}
