package com.example.studytracker.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.studytracker.data.AppDatabase
import com.example.studytracker.data.Book
import com.example.studytracker.data.StudySession
import com.example.studytracker.data.StudyTask
import com.example.studytracker.data.Subject
import com.example.studytracker.data.SubjectStats
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StudyViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.get(app)

    val subjects: StateFlow<List<Subject>> = db.subjectDao().getAll().stateInVm(emptyList())
    val tasks: StateFlow<List<StudyTask>> = db.taskDao().getAll().stateInVm(emptyList())
    val sessions: StateFlow<List<StudySession>> = db.sessionDao().getAll().stateInVm(emptyList())
    val stats: StateFlow<List<SubjectStats>> = db.sessionDao().getStatsBySubject().stateInVm(emptyList())
    val books: StateFlow<List<Book>> = db.bookDao().getAll().stateInVm(emptyList())

    // --- Таймер занятия ---
    var timerSubjectId by mutableStateOf<Long?>(null)
        private set
    var timerRunning by mutableStateOf(false)
        private set
    var elapsedSeconds by mutableLongStateOf(0L)
        private set

    private var timerJob: Job? = null
    private var timerStartMillis = 0L

    fun startTimer(subjectId: Long) {
        if (timerRunning) return
        timerSubjectId = subjectId
        timerStartMillis = System.currentTimeMillis()
        elapsedSeconds = 0
        timerRunning = true
        timerJob = viewModelScope.launch {
            while (true) {
                elapsedSeconds = (System.currentTimeMillis() - timerStartMillis) / 1000
                delay(1000)
            }
        }
    }

    fun stopTimer() {
        if (!timerRunning) return
        timerJob?.cancel()
        timerJob = null
        timerRunning = false
        val subjectId = timerSubjectId
        val duration = (System.currentTimeMillis() - timerStartMillis) / 1000
        if (subjectId != null && duration > 0) {
            viewModelScope.launch {
                db.sessionDao().insert(
                    StudySession(
                        subjectId = subjectId,
                        startTime = timerStartMillis,
                        durationSeconds = duration
                    )
                )
            }
        }
        elapsedSeconds = 0
        timerSubjectId = null
    }

    // --- Предметы ---
    fun addSubject(name: String, color: Long) = viewModelScope.launch {
        db.subjectDao().insert(Subject(name = name.trim(), color = color))
    }

    fun deleteSubject(subject: Subject) = viewModelScope.launch {
        db.subjectDao().delete(subject)
    }

    // --- Задания ---
    fun addTask(subjectId: Long, title: String, dueDate: Long?) = viewModelScope.launch {
        db.taskDao().insert(StudyTask(subjectId = subjectId, title = title.trim(), dueDate = dueDate))
    }

    fun toggleTask(task: StudyTask) = viewModelScope.launch {
        db.taskDao().update(task.copy(isDone = !task.isDone))
    }

    fun deleteTask(task: StudyTask) = viewModelScope.launch {
        db.taskDao().delete(task)
    }

    // --- Чтение PDF ---
    fun addBook(subjectId: Long, title: String, uri: String) = viewModelScope.launch {
        db.bookDao().insert(Book(subjectId = subjectId, title = title.trim(), uri = uri))
    }

    fun deleteBook(book: Book) = viewModelScope.launch {
        db.bookDao().delete(book)
    }

    /**
     * Вызывается при закрытии читалки: запоминает страницу и записывает
     * время чтения как учебную сессию по предмету книги.
     */
    fun finishReading(book: Book, page: Int, startTime: Long, durationSeconds: Long) =
        viewModelScope.launch {
            db.bookDao().updatePage(book.id, page)
            if (durationSeconds >= 5) {
                db.sessionDao().insert(
                    StudySession(
                        subjectId = book.subjectId,
                        startTime = startTime,
                        durationSeconds = durationSeconds
                    )
                )
            }
        }

    private fun <T> Flow<T>.stateInVm(initial: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initial)
}
