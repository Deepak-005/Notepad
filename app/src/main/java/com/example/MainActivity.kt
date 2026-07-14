package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.NoteDatabase
import com.example.data.NoteRepository
import com.example.data.SettingsManager
import com.example.ui.localization.localize
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.NoteViewModel
import com.example.ui.viewmodel.NoteViewModelFactory

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: NoteViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val context = LocalContext.current
            val database = remember { NoteDatabase.getDatabase(context) }
            val repository = remember { NoteRepository(database.noteDao()) }
            val settingsManager = remember { SettingsManager(context) }

            viewModel = viewModel(
                factory = NoteViewModelFactory(
                    app = application,
                    repository = repository,
                    settingsManager = settingsManager
                )
            )

            // Dynamic theme configuration based on SettingsManager state
            val appTheme by viewModel.appTheme.collectAsStateWithLifecycle()
            val darkTheme = when (appTheme) {
                "Dark" -> true
                "Light" -> false
                else -> isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = darkTheme) {
                val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
                val isAppLocked by viewModel.isAppLocked.collectAsStateWithLifecycle()

                if (!isLoggedIn) {
                    LoginScreen(viewModel = viewModel)
                } else if (isAppLocked) {
                    LockScreen(viewModel = viewModel)
                } else {
                    MainAppContent(viewModel = viewModel)
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Record last active time for auto-lock if app backgrounded
        val context = applicationContext
        val settingsManager = SettingsManager(context)
        if (settingsManager.pinLockEnabled) {
            settingsManager.lastActiveTime = System.currentTimeMillis()
        }
    }

    override fun onResume() {
        super.onResume()
        // Auto-lock on resume if backgrounded for more than 1 minute (60,000 ms)
        if (::viewModel.isInitialized) {
            val settingsManager = viewModel.settingsManager
            if (settingsManager.pinLockEnabled && settingsManager.lastActiveTime > 0L) {
                val elapsed = System.currentTimeMillis() - settingsManager.lastActiveTime
                if (elapsed > 60000) {
                    viewModel.lockApp()
                }
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: NoteViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()

    // Hide navigation bar when entering the distraction-free Editor
    val shouldShowBottomBar = when {
        currentRoute == null -> true
        currentRoute.startsWith("note_editor") -> false
        else -> true
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (shouldShowBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == "note_list",
                        onClick = {
                            if (currentRoute != "note_list") {
                                navController.navigate("note_list") {
                                    popUpTo("note_list") { inclusive = true }
                                }
                            }
                        },
                        icon = {
                            Icon(
                                if (currentRoute == "note_list") Icons.Filled.Description else Icons.Outlined.Description,
                                contentDescription = "Notes"
                            )
                        },
                        label = { Text("Notes") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == "archive",
                        onClick = {
                            if (currentRoute != "archive") {
                                navController.navigate("archive") {
                                    popUpTo("note_list")
                                }
                            }
                        },
                        icon = {
                            Icon(
                                if (currentRoute == "archive") Icons.Filled.Archive else Icons.Outlined.Archive,
                                contentDescription = "Archive"
                            )
                        },
                        label = { Text("Archive") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == "trash",
                        onClick = {
                            if (currentRoute != "trash") {
                                navController.navigate("trash") {
                                    popUpTo("note_list")
                                }
                            }
                        },
                        icon = {
                            Icon(
                                if (currentRoute == "trash") Icons.Filled.Delete else Icons.Outlined.Delete,
                                contentDescription = "Trash"
                            )
                        },
                        label = { Text("Trash") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == "statistics",
                        onClick = {
                            if (currentRoute != "statistics") {
                                navController.navigate("statistics") {
                                    popUpTo("note_list")
                                }
                            }
                        },
                        icon = {
                            Icon(
                                if (currentRoute == "statistics") Icons.Filled.BarChart else Icons.Outlined.BarChart,
                                contentDescription = "statistics".localize(appLanguage)
                            )
                        },
                        label = { Text("statistics".localize(appLanguage)) }
                    )
                    NavigationBarItem(
                        selected = currentRoute == "settings",
                        onClick = {
                            if (currentRoute != "settings") {
                                navController.navigate("settings") {
                                    popUpTo("note_list")
                                }
                            }
                        },
                        icon = {
                            if (updateState is com.example.ui.viewmodel.UpdateState.UpdateAvailable) {
                                BadgedBox(
                                    badge = { Badge() }
                                ) {
                                    Icon(
                                        if (currentRoute == "settings") Icons.Filled.Settings else Icons.Outlined.Settings,
                                        contentDescription = "Settings"
                                    )
                                }
                            } else {
                                Icon(
                                    if (currentRoute == "settings") Icons.Filled.Settings else Icons.Outlined.Settings,
                                    contentDescription = "Settings"
                                )
                            }
                        },
                        label = { Text("Settings") }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "note_list",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("note_list") {
                NoteListScreen(
                    viewModel = viewModel,
                    onNavigateToEditor = { noteId ->
                        navController.navigate("note_editor/$noteId")
                    }
                )
            }
            composable(
                route = "note_editor/{noteId}",
                arguments = listOf(navArgument("noteId") { type = NavType.IntType })
            ) { backStackEntry ->
                val noteId = backStackEntry.arguments?.getInt("noteId") ?: -1
                NoteEditorScreen(
                    viewModel = viewModel,
                    noteId = noteId,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            composable("archive") {
                ArchiveScreen(
                    viewModel = viewModel,
                    onNavigateToEditor = { noteId ->
                        navController.navigate("note_editor/$noteId")
                    }
                )
            }
            composable("trash") {
                TrashScreen(viewModel = viewModel)
            }
            composable("statistics") {
                StatisticsScreen(viewModel = viewModel)
            }
            composable("settings") {
                SettingsScreen(viewModel = viewModel)
            }
        }
    }
}
