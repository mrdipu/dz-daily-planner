package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [NoteEntity::class, EventEntity::class, ReminderEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class PlannerDatabase : RoomDatabase() {
    abstract fun plannerDao(): PlannerDao

    companion object {
        @Volatile
        private var INSTANCE: PlannerDatabase? = null

        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_notes_isDeleted_isArchived_isPinned_updatedAtMillis` ON `notes` (`isDeleted`, `isArchived`, `isPinned`, `updatedAtMillis`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_notes_attachedDateEpochDay` ON `notes` (`attachedDateEpochDay`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_events_startEpochMillis_endEpochMillis` ON `events` (`startEpochMillis`, `endEpochMillis`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_events_isCompleted` ON `events` (`isCompleted`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_reminders_isCompleted_triggerAtMillis` ON `reminders` (`isCompleted`, `triggerAtMillis`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_reminders_linkedEventId` ON `reminders` (`linkedEventId`)"
                )
            }
        }

        fun getInstance(context: Context): PlannerDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PlannerDatabase::class.java,
                    "dz_daily_planner.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
