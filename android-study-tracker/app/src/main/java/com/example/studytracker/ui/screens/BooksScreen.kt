package com.example.studytracker.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
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
import androidx.compose.material.icons.filled.PictureAsPdf
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
import com.example.studytracker.data.Book
import com.example.studytracker.data.Subject
import com.example.studytracker.ui.StudyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BooksScreen(
    viewModel: StudyViewModel,
    books: List<Book>,
    subjects: List<Subject>,
    onOpen: (Long) -> Unit
) {
    val context = LocalContext.current
    val subjectNames = remember(subjects) { subjects.associateBy({ it.id }, { it.name }) }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    var pendingName by remember { mutableStateOf("") }
    var bookToDelete by remember { mutableStateOf<Book?>(null) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
            }
            pendingName = queryDisplayName(context, uri)
            pendingUri = uri
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.books_title)) }) },
        floatingActionButton = {
            if (subjects.isNotEmpty()) {
                FloatingActionButton(onClick = { picker.launch(arrayOf("application/pdf")) }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add))
                }
            }
        }
    ) { padding ->
        if (books.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(
                        if (subjects.isEmpty()) R.string.tasks_need_subject else R.string.books_empty
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
                items(books, key = { it.id }) { book ->
                    Card(Modifier.fillMaxWidth().clickable { onOpen(book.id) }) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                Text(book.title, style = MaterialTheme.typography.titleMedium)
                                val subjectName = subjectNames[book.subjectId] ?: ""
                                val pageLabel = if (book.lastPage > 0) {
                                    stringResource(R.string.page_number, book.lastPage + 1)
                                } else ""
                                val subtitle = listOf(subjectName, pageLabel)
                                    .filter { it.isNotEmpty() }
                                    .joinToString(" • ")
                                Text(
                                    subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { bookToDelete = book }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                            }
                        }
                    }
                }
            }
        }
    }

    val uri = pendingUri
    if (uri != null && subjects.isNotEmpty()) {
        AddBookDialog(
            initialTitle = pendingName,
            subjects = subjects,
            onConfirm = { subjectId, title ->
                viewModel.addBook(subjectId, title, uri.toString())
                pendingUri = null
            },
            onDismiss = { pendingUri = null }
        )
    }

    bookToDelete?.let { book ->
        AlertDialog(
            onDismissRequest = { bookToDelete = null },
            title = { Text(stringResource(R.string.delete_book_title)) },
            text = { Text(stringResource(R.string.delete_book_message, book.title)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteBook(book)
                    bookToDelete = null
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { bookToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddBookDialog(
    initialTitle: String,
    subjects: List<Subject>,
    onConfirm: (Long, String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var selectedSubject by remember { mutableStateOf(subjects.first()) }
    var menuExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.new_book)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.book_title)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(
                    expanded = menuExpanded,
                    onExpandedChange = { menuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedSubject.name,
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
                Text(
                    stringResource(R.string.reading_counts_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedSubject.id, title) },
                enabled = title.isNotBlank()
            ) { Text(stringResource(R.string.add)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

private fun queryDisplayName(context: Context, uri: Uri): String {
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (cursor.moveToFirst() && index >= 0) {
            return cursor.getString(index).removeSuffix(".pdf")
        }
    }
    return "PDF"
}
