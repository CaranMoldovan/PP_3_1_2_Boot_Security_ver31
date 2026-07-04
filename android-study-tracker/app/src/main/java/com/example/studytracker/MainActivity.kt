package com.example.studytracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.studytracker.ui.StudyViewModel
import com.example.studytracker.ui.screens.BooksScreen
import com.example.studytracker.ui.screens.QuizTakingScreen
import com.example.studytracker.ui.screens.QuizzesScreen
import com.example.studytracker.ui.screens.ReaderScreen
import com.example.studytracker.ui.screens.StatsScreen
import com.example.studytracker.ui.screens.SubjectsScreen
import com.example.studytracker.ui.screens.TasksScreen
import com.example.studytracker.ui.screens.TimerScreen
import com.example.studytracker.ui.theme.StudyTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StudyTrackerTheme {
                Surface {
                    StudyTrackerApp()
                }
            }
        }
    }
}

private data class NavItem(val route: String, val labelRes: Int, val icon: ImageVector)

private val navItems = listOf(
    NavItem("subjects", R.string.nav_subjects, Icons.Default.MenuBook),
    NavItem("tasks", R.string.nav_tasks, Icons.Default.Checklist),
    NavItem("timer", R.string.nav_timer, Icons.Default.Timer),
    NavItem("books", R.string.nav_reading, Icons.Default.AutoStories),
    NavItem("quizzes", R.string.nav_quizzes, Icons.Default.Quiz),
    NavItem("stats", R.string.nav_stats, Icons.Default.BarChart)
)

@Composable
fun StudyTrackerApp(viewModel: StudyViewModel = viewModel()) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val subjects by viewModel.subjects.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val books by viewModel.books.collectAsState()
    val quizzes by viewModel.quizzes.collectAsState()

    // В читалке нижняя панель скрыта, чтобы не мешать чтению
    val showBottomBar = navItems.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    navItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(stringResource(item.labelRes)) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "subjects",
            modifier = Modifier.padding(padding)
        ) {
            composable("subjects") { SubjectsScreen(viewModel, subjects) }
            composable("tasks") { TasksScreen(viewModel, tasks, subjects) }
            composable("timer") { TimerScreen(viewModel, subjects) }
            composable("books") {
                BooksScreen(viewModel, books, subjects) { bookId ->
                    navController.navigate("reader/$bookId")
                }
            }
            composable(
                "reader/{bookId}",
                arguments = listOf(navArgument("bookId") { type = NavType.LongType })
            ) { entry ->
                val bookId = entry.arguments?.getLong("bookId") ?: 0L
                ReaderScreen(viewModel, bookId) { navController.popBackStack() }
            }
            composable("quizzes") {
                QuizzesScreen(viewModel, quizzes, subjects) { quizId ->
                    navController.navigate("quiz/$quizId")
                }
            }
            composable(
                "quiz/{quizId}",
                arguments = listOf(navArgument("quizId") { type = NavType.LongType })
            ) { entry ->
                val quizId = entry.arguments?.getLong("quizId") ?: 0L
                QuizTakingScreen(viewModel, quizId) { navController.popBackStack() }
            }
            composable("stats") { StatsScreen(viewModel, subjects, tasks, stats) }
        }
    }
}
