package com.example.studytracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Subject::class, StudyTask::class, StudySession::class, Book::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun subjectDao(): SubjectDao
    abstract fun taskDao(): TaskDao
    abstract fun sessionDao(): SessionDao
    abstract fun bookDao(): BookDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `books` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`subjectId` INTEGER NOT NULL, " +
                        "`title` TEXT NOT NULL, " +
                        "`uri` TEXT NOT NULL, " +
                        "`lastPage` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`subjectId`) REFERENCES `subjects`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_books_subjectId` ON `books` (`subjectId`)"
                )
            }
        }

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "study_tracker.db"
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
    }
}
