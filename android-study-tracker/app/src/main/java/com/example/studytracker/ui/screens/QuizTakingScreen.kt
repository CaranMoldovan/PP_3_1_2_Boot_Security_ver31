package com.example.studytracker.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.studytracker.R
import com.example.studytracker.data.QuizParser
import com.example.studytracker.ui.StudyViewModel

private val correctColor = Color(0xFF2E7D32)
private val wrongColor = Color(0xFFC62828)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizTakingScreen(viewModel: StudyViewModel, quizId: Long, onBack: () -> Unit) {
    val quizzes by viewModel.quizzes.collectAsState()
    val quiz = quizzes.find { it.id == quizId }
    if (quiz == null) {
        Box(Modifier.fillMaxSize())
        return
    }

    val questions = remember(quiz.questionsJson) {
        try {
            QuizParser.questionsFromJson(quiz.questionsJson)
        } catch (e: Exception) {
            emptyList()
        }
    }

    var index by rememberSaveable { mutableIntStateOf(0) }
    var selected by rememberSaveable { mutableStateOf<Int?>(null) }
    var correctCount by rememberSaveable { mutableIntStateOf(0) }
    var finished by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(finished) {
        if (finished) {
            viewModel.saveQuizResult(quiz.id, correctCount)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(quiz.title, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        when {
            questions.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.import_error_title))
                }
            }
            finished -> {
                ResultContent(
                    score = correctCount,
                    total = questions.size,
                    onRetry = {
                        index = 0
                        selected = null
                        correctCount = 0
                        finished = false
                    },
                    onBack = onBack,
                    modifier = Modifier.fillMaxSize().padding(padding)
                )
            }
            else -> {
                val question = questions[index]
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        stringResource(R.string.question_progress, index + 1, questions.size),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LinearProgressIndicator(
                        progress = { (index + 1f) / questions.size },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(question.text, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(4.dp))

                    question.options.forEachIndexed { optionIndex, option ->
                        val answered = selected != null
                        val isCorrect = optionIndex == question.correct
                        val isSelected = optionIndex == selected
                        val borderColor = when {
                            answered && isCorrect -> correctColor
                            answered && isSelected -> wrongColor
                            else -> MaterialTheme.colorScheme.outlineVariant
                        }
                        val containerColor = when {
                            answered && isCorrect -> correctColor.copy(alpha = 0.12f)
                            answered && isSelected -> wrongColor.copy(alpha = 0.12f)
                            else -> MaterialTheme.colorScheme.surface
                        }
                        Card(
                            onClick = {
                                if (selected == null) {
                                    selected = optionIndex
                                    if (isCorrect) correctCount++
                                }
                            },
                            enabled = selected == null,
                            border = BorderStroke(1.dp, borderColor),
                            colors = CardDefaults.cardColors(
                                containerColor = containerColor,
                                disabledContainerColor = containerColor
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                option,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.fillMaxWidth().padding(16.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (index == questions.size - 1) {
                                finished = true
                            } else {
                                index++
                                selected = null
                            }
                        },
                        enabled = selected != null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            stringResource(
                                if (index == questions.size - 1) R.string.finish else R.string.next
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultContent(
    score: Int,
    total: Int,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val percent = if (total > 0) score * 100 / total else 0
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                when {
                    percent >= 90 -> "🏆"
                    percent >= 70 -> "🎉"
                    percent >= 50 -> "👍"
                    else -> "📖"
                },
                style = MaterialTheme.typography.displayLarge
            )
            Text(
                stringResource(R.string.score_of, score, total),
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                "$percent%",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onBack) {
                    Text(stringResource(R.string.to_list))
                }
                Button(onClick = onRetry) {
                    Text(stringResource(R.string.try_again))
                }
            }
        }
    }
}
