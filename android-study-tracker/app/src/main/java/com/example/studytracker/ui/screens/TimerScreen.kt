package com.example.studytracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.studytracker.R
import com.example.studytracker.data.Subject
import com.example.studytracker.ui.StudyViewModel

fun formatDuration(totalSeconds: Long): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return "%02d:%02d:%02d".format(h, m, s)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(viewModel: StudyViewModel, subjects: List<Subject>) {
    var selectedSubject by remember(subjects) { mutableStateOf(subjects.firstOrNull()) }
    var menuExpanded by remember { mutableStateOf(false) }

    val running = viewModel.timerRunning
    val runningSubject = subjects.find { it.id == viewModel.timerSubjectId }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.timer_title)) }) }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            if (subjects.isEmpty()) {
                Text(
                    stringResource(R.string.tasks_need_subject),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text(
                        formatDuration(viewModel.elapsedSeconds),
                        style = MaterialTheme.typography.displayLarge
                    )
                    if (running && runningSubject != null) {
                        Text(
                            runningSubject.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        ExposedDropdownMenuBox(
                            expanded = menuExpanded,
                            onExpandedChange = { menuExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedSubject?.name ?: "",
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
                    }
                    Button(
                        onClick = {
                            if (running) {
                                viewModel.stopTimer()
                            } else {
                                selectedSubject?.let { viewModel.startTimer(it.id) }
                            }
                        },
                        enabled = running || selectedSubject != null
                    ) {
                        Icon(
                            if (running) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = null
                        )
                        Text(
                            stringResource(if (running) R.string.stop else R.string.start),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    if (running) {
                        Text(
                            stringResource(R.string.timer_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
