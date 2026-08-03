package com.zeubicardgames.app.di

import android.content.Context
import androidx.room.Room
import com.zeubicardgames.app.core.database.GameDao
import com.zeubicardgames.app.core.database.ZeubiDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton fun database(@ApplicationContext context: Context): ZeubiDatabase = Room.databaseBuilder(context, ZeubiDatabase::class.java, "zeubicardgames.db").build()
    @Provides fun dao(db: ZeubiDatabase): GameDao = db.gameDao()
}
