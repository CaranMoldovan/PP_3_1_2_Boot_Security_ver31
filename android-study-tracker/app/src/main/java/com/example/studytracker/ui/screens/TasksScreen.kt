package com.example.studytracker.ui.screens

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
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
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.studytracker.R
import com.example.studytracker.data.StudyTask
import com.example.studytracker.data.Subject
import com.example.studytracker.ui.StudyViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(viewModel: StudyViewModel, tasks: List<StudyTask>, subjects: List<Subject>) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    val subjectNames = remember(subjects) { subjects.associateBy({ it.id }, { it.name }) }
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.tasks_title)) }) },
        floatingActionButton = {
            if (subjects.isNotEmpty()) {
                FloatingActionButton(onClick = { showDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add))
                }
            }
        }
    ) { padding ->
        if (tasks.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(
                        if (subjects.isEmpty()) R.string.tasks_need_subject else R.string.tasks_empty
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tasks, key = { it.id }) { task ->
                    val overdue = task.dueDate != null && !task.isDone &&
                        task.dueDate < System.currentTimeMillis()
                    Card(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = task.isDone,
                                onCheckedChange = { viewModel.toggleTask(task) }
                            )
                            Column(Modifier.weight(1f)) {
                                Text(
                                    task.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    textDecoration = if (task.isDone) TextDecoration.LineThrough else null
                                )
                                val subtitle = buildString {
                                    append(subjectNames[task.subjectId] ?: "")
                                    task.dueDate?.let {
                                        if (isNotEmpty()) append(" • ")
                                        append(dateFormat.format(Date(it)))
                                    }
                                }
                                if (subtitle.isNotEmpty()) {
                                    Text(
                                        subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (overdue) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(onClick = { viewModel.deleteTask(task) }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AddTaskDialog(
            subjects = subjects,
            onConfirm = { subjectId, title, dueDate ->
                viewModel.addTask(subjectId, title, dueDate)
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTaskDialog(
    subjects: List<Subject>,
    onConfirm: (Long, String, Long?) -> Unit,
    onDismiss: () -> Unit
) {
    var title by rememberSaveable { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf(subjects.first()) }
    var subjectMenuExpanded by remember { mutableStateOf(false) }
    var dueDate by rememberSaveable { mutableStateOf<Long?>(null) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.new_task)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.task_title)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(
                    expanded = subjectMenuExpanded,
                    onExpandedChange = { subjectMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedSubject.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.subject)) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectMenuExpanded)
                        },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = subjectMenuExpanded,
                        onDismissRequest = { subjectMenuExpanded = false }
                    ) {
                        subjects.forEach { subject ->
                            DropdownMenuItem(
                                text = { Text(subject.name) },
                                onClick = {
                                    selectedSubject = subject
                                    subjectMenuExpanded = false
                                }
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        dueDate?.let { dateFormat.format(Date(it)) }
                            ?: stringResource(R.string.no_deadline),
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { showDatePicker = true }) {
                        Text(stringResource(R.string.pick_date))
                    }
                    if (dueDate != null) {
                        TextButton(onClick = { dueDate = null }) {
                            Text(stringResource(R.string.clear))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedSubject.id, title, dueDate) },
                enabled = title.isNotBlank()
            ) { Text(stringResource(R.string.add)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dueDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dueDate = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
