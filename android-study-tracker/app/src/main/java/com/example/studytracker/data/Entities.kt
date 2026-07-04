package com.example.studytracker.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "subjects")
data class Subject(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: Long = 0xFF4CAF50
)

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = Subject::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("subjectId")]
)
data class StudyTask(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val title: String,
    val dueDate: Long? = null,
    val isDone: Boolean = false
)

@Entity(
    tableName = "sessions",
    foreignKeys = [
        ForeignKey(
            entity = Subject::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("subjectId")]
)
data class StudySession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val startTime: Long,
    val durationSeconds: Long
)

@Entity(
    tableName = "books",
    foreignKeys = [
        ForeignKey(
            entity = Subject::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("subjectId")]
)
data class Book(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val title: String,
    val uri: String,
    val lastPage: Int = 0
)

data class SubjectStats(
    val subjectId: Long,
    val totalSeconds: Long
)
