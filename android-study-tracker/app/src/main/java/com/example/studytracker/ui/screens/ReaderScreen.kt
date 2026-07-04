package com.example.studytracker.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.studytracker.R
import com.example.studytracker.ui.StudyViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(viewModel: StudyViewModel, bookId: Long, onBack: () -> Unit) {
    val books by viewModel.books.collectAsState()
    val book = books.find { it.id == bookId }
    if (book == null) {
        // Список ещё загружается либо книга удалена
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (books.isNotEmpty()) {
                Text(stringResource(R.string.pdf_open_error))
            }
        }
        return
    }

    val context = LocalContext.current
    val pdf = remember(book.uri) { PdfDocument(context, Uri.parse(book.uri)) }

    // Счётчик времени чтения: тикает только пока экран на переднем плане.
    var elapsedSeconds by remember { mutableLongStateOf(0L) }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(Unit) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                delay(1000)
                elapsedSeconds++
            }
        }
    }

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex =
            book.lastPage.coerceIn(0, (pdf.pageCount - 1).coerceAtLeast(0))
    )

    val startTime = remember { System.currentTimeMillis() }
    DisposableEffect(Unit) {
        onDispose {
            viewModel.finishReading(
                book = book,
                page = listState.firstVisibleItemIndex,
                startTime = startTime,
                durationSeconds = elapsedSeconds
            )
            pdf.close()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(book.title, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    Text(
                        formatDuration(elapsedSeconds),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            )
        }
    ) { padding ->
        if (pdf.pageCount == 0) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.pdf_open_error),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(32.dp)
                )
            }
        } else {
            BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
                val widthPx = constraints.maxWidth
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(pdf.pageCount) { index ->
                        var bitmap by remember { mutableStateOf<Bitmap?>(null) }
                        LaunchedEffect(index, widthPx) {
                            bitmap = pdf.renderPage(index, widthPx)
                        }
                        val bmp = bitmap
                        if (bmp != null) {
                            Image(
                                bmp.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxWidth(),
                                contentScale = ContentScale.FillWidth
                            )
                        } else {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(0.707f)
                                    .background(Color.White)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Обёртка над системным PdfRenderer: открывает документ по URI и
 * отрисовывает страницы в Bitmap. Все обращения к renderer
 * сериализованы мьютексом — PdfRenderer не потокобезопасен.
 */
private class PdfDocument(context: Context, uri: Uri) {
    private val mutex = Mutex()
    private var fileDescriptor: ParcelFileDescriptor? = null
    private var renderer: PdfRenderer? = null

    var pageCount: Int = 0
        private set

    init {
        try {
            fileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
            renderer = fileDescriptor?.let { PdfRenderer(it) }
            pageCount = renderer?.pageCount ?: 0
        } catch (e: Exception) {
            close()
        }
    }

    suspend fun renderPage(index: Int, width: Int): Bitmap? = withContext(Dispatchers.IO) {
        mutex.withLock {
            val r = renderer ?: return@withLock null
            try {
                val page = r.openPage(index)
                try {
                    val w = width.coerceIn(1, 2048)
                    val h = (w.toLong() * page.height / page.width).toInt().coerceAtLeast(1)
                    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(android.graphics.Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmap
                } finally {
                    page.close()
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    fun close() {
        runBlocking {
            mutex.withLock {
                try {
                    renderer?.close()
                } catch (_: Exception) {
                }
                try {
                    fileDescriptor?.close()
                } catch (_: Exception) {
                }
                renderer = null
                fileDescriptor = null
            }
        }
    }
}
