package com.example.bloodsugar.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [BloodSugarRecord::class, NotificationSetting::class, EventRecord::class], version = 6, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun bloodSugarDao(): BloodSugarDao
    abstract fun notificationSettingDao(): NotificationSettingDao
    abstract fun eventDao(): EventDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notification_settings ADD COLUMN startTime TEXT")
                db.execSQL("ALTER TABLE notification_settings ADD COLUMN endTime TEXT")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE `notification_settings_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `type` TEXT NOT NULL, `time` TEXT NOT NULL, `intervalMinutes` INTEGER NOT NULL, `message` TEXT NOT NULL, `isEnabled` INTEGER NOT NULL, `startTime` TEXT, `endTime` TEXT)")
                db.execSQL("INSERT INTO notification_settings_new (id, type, time, intervalMinutes, message, isEnabled, startTime, endTime) SELECT id, type, time, intervalHours, message, isEnabled, startTime, endTime FROM notification_settings")
                db.execSQL("DROP TABLE notification_settings")
                db.execSQL("ALTER TABLE notification_settings_new RENAME TO notification_settings")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE `notification_settings_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `type` TEXT NOT NULL, `time` TEXT NOT NULL, `intervalHours` INTEGER NOT NULL, `message` TEXT NOT NULL, `isEnabled` INTEGER NOT NULL, `startTime` TEXT, `endTime` TEXT)")
                db.execSQL("INSERT INTO notification_settings_new (id, type, time, intervalHours, message, isEnabled, startTime, endTime) SELECT id, type, time, intervalMinutes, message, isEnabled, startTime, endTime FROM notification_settings")
                db.execSQL("DROP TABLE notification_settings")
                db.execSQL("ALTER TABLE notification_settings_new RENAME TO notification_settings")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE `notification_settings_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `type` TEXT NOT NULL, `time` TEXT NOT NULL, `intervalMinutes` INTEGER NOT NULL, `message` TEXT NOT NULL, `isEnabled` INTEGER NOT NULL, `startTime` TEXT, `endTime` TEXT)")
                db.execSQL("INSERT INTO notification_settings_new (id, type, time, intervalMinutes, message, isEnabled, startTime, endTime) SELECT id, type, time, intervalHours * 60, message, isEnabled, startTime, endTime FROM notification_settings")
                db.execSQL("DROP TABLE notification_settings")
                db.execSQL("ALTER TABLE notification_settings_new RENAME TO notification_settings")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "blood_sugar_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
