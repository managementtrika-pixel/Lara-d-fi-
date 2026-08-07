package com.zeubicardgames.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `pack_openings` (
                    `id` TEXT NOT NULL,
                    `setId` TEXT NOT NULL,
                    `cardIdsCsv` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `acknowledged` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
        }
    }

    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): ZeubiDatabase =
        Room.databaseBuilder(context, ZeubiDatabase::class.java, "zeubicardgames.db")
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides
    fun dao(db: ZeubiDatabase): GameDao = db.gameDao()
}
