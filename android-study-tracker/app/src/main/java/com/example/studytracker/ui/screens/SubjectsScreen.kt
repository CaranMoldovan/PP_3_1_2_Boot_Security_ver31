package com.example.studytracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.studytracker.R
import com.example.studytracker.data.Subject
import com.example.studytracker.ui.StudyViewModel

private val subjectColors = listOf(
    0xFF4CAF50, 0xFF2196F3, 0xFFFF9800, 0xFF9C27B0,
    0xFFF44336, 0xFF009688, 0xFF795548, 0xFF607D8B
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectsScreen(viewModel: StudyViewModel, subjects: List<Subject>) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var subjectToDelete by androidx.compose.runtime.remember { mutableStateOf<Subject?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.subjects_title)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add))
            }
        }
    ) { padding ->
        if (subjects.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.subjects_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(subjects, key = { it.id }) { subject ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .size(16.dp)
                                    .background(Color(subject.color), CircleShape)
                            )
                            Text(
                                subject.name,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f).padding(start = 12.dp)
                            )
                            IconButton(onClick = { subjectToDelete = subject }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AddSubjectDialog(
            onConfirm = { name, color ->
                viewModel.addSubject(name, color)
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }

    subjectToDelete?.let { subject ->
        AlertDialog(
            onDismissRequest = { subjectToDelete = null },
            title = { Text(stringResource(R.string.delete_subject_title)) },
            text = { Text(stringResource(R.string.delete_subject_message, subject.name)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSubject(subject)
                    subjectToDelete = null
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { subjectToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun AddSubjectDialog(onConfirm: (String, Long) -> Unit, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var selectedColor by rememberSaveable { mutableStateOf(subjectColors.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.new_subject)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.subject_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.size(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    subjectColors.forEach { color ->
                        Box(
                            Modifier
                                .size(if (color == selectedColor) 32.dp else 24.dp)
                                .background(Color(color), CircleShape)
                                .clickable { selectedColor = color },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, selectedColor) },
                enabled = name.isNotBlank()
            ) { Text(stringResource(R.string.add)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
