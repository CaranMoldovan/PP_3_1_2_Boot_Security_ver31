package com.example.studytracker.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.studytracker.R
import com.example.studytracker.data.ParsedQuiz
import com.example.studytracker.data.Quiz
import com.example.studytracker.data.QuizParser
import com.example.studytracker.data.Subject
import com.example.studytracker.ui.StudyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizzesScreen(
    viewModel: StudyViewModel,
    quizzes: List<Quiz>,
    subjects: List<Subject>,
    onOpen: (Long) -> Unit
) {
    val context = LocalContext.current
    val subjectNames = remember(subjects) { subjects.associateBy({ it.id }, { it.name }) }
    var pendingQuiz by remember { mutableStateOf<ParsedQuiz?>(null) }
    var importError by remember { mutableStateOf<String?>(null) }
    var quizToDelete by remember { mutableStateOf<Quiz?>(null) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val text = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()?.use { it.readText() }
                    ?: throw IllegalArgumentException("Не удалось прочитать файл")
                pendingQuiz = QuizParser.parse(text)
            } catch (e: Exception) {
                importError = e.message ?: "Не удалось прочитать файл"
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.quizzes_title)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                picker.launch(
                    arrayOf("application/json", "text/plain", "application/octet-stream")
                )
            }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add))
            }
        }
    ) { padding ->
        if (quizzes.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.quizzes_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(32.dp)
                )
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(quizzes, key = { it.id }) { quiz ->
                    Card(Modifier.fillMaxWidth().clickable { onOpen(quiz.id) }) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Quiz,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                Text(quiz.title, style = MaterialTheme.typography.titleMedium)
                                val parts = mutableListOf<String>()
                                subjectNames[quiz.subjectId]?.let { parts.add(it) }
                                parts.add(
                                    stringResource(R.string.questions_count, quiz.questionCount)
                                )
                                quiz.bestScore?.let {
                                    parts.add(
                                        stringResource(R.string.best_score, it, quiz.questionCount)
                                    )
                                }
                                Text(
                                    parts.joinToString(" • "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { quizToDelete = quiz }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                            }
                        }
                    }
                }
            }
        }
    }

    pendingQuiz?.let { parsed ->
        ImportQuizDialog(
            parsed = parsed,
            subjects = subjects,
            onConfirm = { title, subjectId ->
                viewModel.addQuiz(parsed, title, subjectId)
                pendingQuiz = null
            },
            onDismiss = { pendingQuiz = null }
        )
    }

    importError?.let { message ->
        AlertDialog(
            onDismissRequest = { importError = null },
            title = { Text(stringResource(R.string.import_error_title)) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { importError = null }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }

    quizToDelete?.let { quiz ->
        AlertDialog(
            onDismissRequest = { quizToDelete = null },
            title = { Text(stringResource(R.string.delete_quiz_title)) },
            text = { Text(stringResource(R.string.delete_quiz_message, quiz.title)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteQuiz(quiz)
                    quizToDelete = null
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { quizToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImportQuizDialog(
    parsed: ParsedQuiz,
    subjects: List<Subject>,
    onConfirm: (String, Long?) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(parsed.title ?: "") }
    // Если в JSON указан предмет с таким же названием — подставляем его сразу
    var selectedSubject by remember {
        mutableStateOf(
            subjects.find { it.name.equals(parsed.subjectName ?: "", ignoreCase = true) }
        )
    }
    var menuExpanded by remember { mutableStateOf(false) }
    val noSubjectLabel = stringResource(R.string.no_subject)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.new_quiz)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.quiz_title_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(
                    expanded = menuExpanded,
                    onExpandedChange = { menuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedSubject?.name ?: noSubjectLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.subject)) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuExpanded)
                        },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(noSubjectLabel) },
                            onClick = {
                                selectedSubject = null
                                menuExpanded = false
                            }
                        )
                        subjects.forEach { subject ->
                            DropdownMenuItem(
                                text = { Text(subject.name) },
                                onClick = {
                                    selectedSubject = subject
                                    menuExpanded = false
                                }
                            )
                        }
                    }
                }
                Text(
                    stringResource(R.string.questions_count, parsed.questions.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(title, selectedSubject?.id) },
                enabled = title.isNotBlank()
            ) { Text(stringResource(R.string.add)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
