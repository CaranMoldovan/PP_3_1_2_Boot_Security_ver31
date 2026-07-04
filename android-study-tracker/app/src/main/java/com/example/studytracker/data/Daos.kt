package com.example.studytracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects ORDER BY name")
    fun getAll(): Flow<List<Subject>>

    @Insert
    suspend fun insert(subject: Subject): Long

    @Delete
    suspend fun delete(subject: Subject)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY isDone, dueDate IS NULL, dueDate")
    fun getAll(): Flow<List<StudyTask>>

    @Insert
    suspend fun insert(task: StudyTask): Long

    @Update
    suspend fun update(task: StudyTask)

    @Delete
    suspend fun delete(task: StudyTask)
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY startTime DESC")
    fun getAll(): Flow<List<StudySession>>

    @Query("SELECT subjectId, SUM(durationSeconds) AS totalSeconds FROM sessions GROUP BY subjectId")
    fun getStatsBySubject(): Flow<List<SubjectStats>>

    @Insert
    suspend fun insert(session: StudySession): Long
}
