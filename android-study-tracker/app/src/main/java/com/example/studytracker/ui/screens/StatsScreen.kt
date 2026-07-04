package com.example.studytracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.studytracker.R
import com.example.studytracker.data.StudyTask
import com.example.studytracker.data.Subject
import com.example.studytracker.data.SubjectStats
import com.example.studytracker.ui.StudyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: StudyViewModel,
    subjects: List<Subject>,
    tasks: List<StudyTask>,
    stats: List<SubjectStats>
) {
    val totalSeconds = stats.sumOf { it.totalSeconds }
    val doneTasks = tasks.count { it.isDone }
    val maxSeconds = stats.maxOfOrNull { it.totalSeconds } ?: 0L
    val statsBySubject = stats.associateBy { it.subjectId }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.stats_title)) }) }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SummaryCard(
                        title = stringResource(R.string.total_time),
                        value = formatHoursMinutes(totalSeconds),
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        title = stringResource(R.string.tasks_done),
                        value = "$doneTasks / ${tasks.size}",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            if (subjects.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(R.string.stats_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(subjects, key = { it.id }) { subject ->
                    val seconds = statsBySubject[subject.id]?.totalSeconds ?: 0L
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier
                                        .size(12.dp)
                                        .background(Color(subject.color), CircleShape)
                                )
                                Text(
                                    subject.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                                )
                                Text(
                                    formatHoursMinutes(seconds),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (maxSeconds > 0) {
                                val fraction = seconds.toFloat() / maxSeconds
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp)
                                        .height(8.dp)
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            RoundedCornerShape(4.dp)
                                        )
                                ) {
                                    if (fraction > 0f) {
                                        Box(
                                            Modifier
                                                .fillMaxWidth(fraction)
                                                .height(8.dp)
                                                .background(
                                                    Color(subject.color),
                                                    RoundedCornerShape(4.dp)
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(value, style = MaterialTheme.typography.headlineSmall)
        }
    }
}

private fun formatHoursMinutes(totalSeconds: Long): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    return if (h > 0) "${h} ч ${m} мин" else "${m} мин"
}
