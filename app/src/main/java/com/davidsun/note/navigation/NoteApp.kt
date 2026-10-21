package com.davidsun.note.navigation

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.davidsun.note.feature.notes.NoteEditorActions
import com.davidsun.note.feature.notes.NoteEditorScreen
import com.davidsun.note.feature.notes.NoteEditorViewModel
import com.davidsun.note.feature.notes.NoteListActions
import com.davidsun.note.feature.notes.NoteListScreen
import com.davidsun.note.feature.notes.NoteListViewModel
import com.davidsun.note.feature.settings.GoogleIdTokenRequester
import com.davidsun.note.feature.settings.SettingsActions
import com.davidsun.note.feature.settings.SettingsScreen
import com.davidsun.note.feature.settings.SettingsViewModel
import com.davidsun.note.feature.settings.rememberExactAlarmsAllowed
import com.davidsun.note.feature.settings.rememberNotificationsEnabled
import com.davidsun.note.feature.tasks.HiddenTasksActions
import com.davidsun.note.feature.tasks.HiddenTasksScreen
import com.davidsun.note.feature.tasks.HiddenTasksViewModel
import com.davidsun.note.feature.tasks.TaskEditorActions
import com.davidsun.note.feature.tasks.TaskEditorScreen
import com.davidsun.note.feature.tasks.TaskEditorViewModel
import com.davidsun.note.feature.tasks.TaskListActions
import com.davidsun.note.feature.tasks.TaskListScreen
import com.davidsun.note.feature.tasks.TaskListViewModel
import kotlinx.coroutines.launch

@Composable
fun NoteApp(
    pendingRoute: String? = null,
    focusId: String? = null,
    onPendingRouteConsumed: () -> Unit = {},
    onFocusConsumed: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    var coverStartup by remember { mutableStateOf(pendingRoute != null) }
    LaunchedEffect(pendingRoute) {
        try {
            if (pendingRoute == AppRoute.Tasks || pendingRoute == AppRoute.Notes) {
                navController.openWidgetList(pendingRoute)
            }
            if (pendingRoute != null) onPendingRouteConsumed()
        } finally {
            coverStartup = false
        }
    }
    Box(modifier = modifier) {
        NavHost(
            navController = navController,
            startDestination = AppRoute.Tasks,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(AppRoute.Tasks) { TaskListRoute(navController, focusId, onFocusConsumed) }
            composable(AppRoute.Notes) { NoteListRoute(navController, focusId, onFocusConsumed) }
            composable(AppRoute.Settings) { SettingsRoute(navController) }
            composable(AppRoute.HiddenTasks) { HiddenTasksRoute(navController) }
            composable(
                route = AppRoute.TaskEditor,
                arguments = listOf(navArgument("taskId") { type = NavType.StringType }),
            ) {
                TaskEditorRoute(navController)
            }
            composable(
                route = AppRoute.NoteEditor,
                arguments = listOf(navArgument("noteId") { type = NavType.StringType }),
            ) {
                NoteEditorRoute(navController)
            }
        }
        if (coverStartup) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
            )
        }
    }
}

@Composable
private fun TaskListRoute(
    navController: NavHostController,
    focusId: String?,
    onFocusConsumed: () -> Unit,
) {
    val viewModel: TaskListViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TaskListScreen(
        state = state,
        actions = TaskListActions(
            onTaskClick = { id -> navController.navigate(AppRoute.taskEditor(id)) },
            onAddClick = { navController.navigate(AppRoute.taskEditor(null)) },
            onToggleComplete = viewModel::setCompleted,
            onHide = viewModel::hide,
            onDeleteRequest = viewModel::onDeleteRequest,
            onConfirmDelete = viewModel::onConfirmDelete,
            onDismissDelete = viewModel::onDismissDelete,
            onOpenHidden = { navController.navigate(AppRoute.HiddenTasks) },
        ),
        focusId = focusId,
        onFocusConsumed = onFocusConsumed,
        bottomBar = {
            NoteBottomBar(
                selected = AppTab.Tasks,
                onSelect = { tab -> navController.navigateToTab(tab) },
            )
        },
    )
}

@Composable
private fun NoteListRoute(
    navController: NavHostController,
    focusId: String?,
    onFocusConsumed: () -> Unit,
) {
    val viewModel: NoteListViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    NoteListScreen(
        state = state,
        actions = NoteListActions(
            onNoteClick = { id -> navController.navigate(AppRoute.noteEditor(id)) },
            onAddClick = { navController.navigate(AppRoute.noteEditor(null)) },
            onDeleteRequest = viewModel::onDeleteRequest,
            onConfirmDelete = viewModel::onConfirmDelete,
            onDismissDelete = viewModel::onDismissDelete,
        ),
        focusId = focusId,
        onFocusConsumed = onFocusConsumed,
        bottomBar = {
            NoteBottomBar(
                selected = AppTab.Notes,
                onSelect = { tab -> navController.navigateToTab(tab) },
            )
        },
    )
}

@Composable
private fun SettingsRoute(navController: NavHostController) {
    val viewModel: SettingsViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val notificationsEnabled = rememberNotificationsEnabled()
    val exactAlarmsAllowed = rememberExactAlarmsAllowed()
    SettingsScreen(
        state = state,
        notificationGranted = notificationsEnabled,
        exactAlarmGranted = exactAlarmsAllowed,
        actions = SettingsActions(
            onFontScaleSelected = viewModel::onFontScaleSelected,
            onOpenNotificationSettings = {
                val intent = Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
                context.startActivity(intent)
            },
            onOpenExactAlarmSettings = {
                val intent = Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = android.net.Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
            },
            onSignIn = {
                scope.launch {
                    viewModel.onSignInAttempt(GoogleIdTokenRequester(context).request())
                }
            },
            onSignOut = viewModel::onSignOut,
        ),
        bottomBar = {
            NoteBottomBar(
                selected = AppTab.Settings,
                onSelect = { tab -> navController.navigateToTab(tab) },
            )
        },
    )
}

@Composable
private fun HiddenTasksRoute(navController: NavHostController) {
    val viewModel: HiddenTasksViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HiddenTasksScreen(
        state = state,
        actions = HiddenTasksActions(
            onBack = { navController.popBackStack() },
            onTaskClick = { id -> navController.navigate(AppRoute.taskEditor(id)) },
            onToggleComplete = viewModel::setCompleted,
            onRestore = viewModel::restore,
            onDeleteRequest = viewModel::onDeleteRequest,
            onConfirmDelete = viewModel::onConfirmDelete,
            onDismissDelete = viewModel::onDismissDelete,
        ),
    )
}

@Composable
private fun TaskEditorRoute(navController: NavHostController) {
    val viewModel: TaskEditorViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.closeEvents.collect { navController.popBackStack() }
    }
    TaskEditorScreen(
        state = state,
        actions = TaskEditorActions(
            onTitleChange = viewModel::onTitleChange,
            onDescriptionChange = viewModel::onDescriptionChange,
            onOpenReminder = viewModel::onOpenReminder,
            onDismissReminderOptions = viewModel::onDismissReminderOptions,
            onChooseOnce = viewModel::onChooseOnce,
            onChooseInterval = viewModel::onChooseInterval,
            onChooseWeekly = viewModel::onChooseWeekly,
            onDismissDatePicker = viewModel::onDismissDatePicker,
            onDatePicked = viewModel::onDatePicked,
            onDismissTimePicker = viewModel::onDismissTimePicker,
            onTimePicked = viewModel::onTimePicked,
            onIntervalDaysChange = viewModel::onIntervalDaysChange,
            onOpenIntervalTime = viewModel::onOpenIntervalTime,
            onConfirmInterval = viewModel::onConfirmInterval,
            onDismissInterval = viewModel::onDismissInterval,
            onToggleWeekday = viewModel::onToggleWeekday,
            onOpenWeeklyTime = viewModel::onOpenWeeklyTime,
            onConfirmWeekly = viewModel::onConfirmWeekly,
            onDismissWeekly = viewModel::onDismissWeekly,
            onClearReminder = viewModel::onClearReminder,
            onSave = viewModel::onSave,
            onBack = viewModel::onBack,
            onConfirmDiscard = viewModel::onConfirmDiscard,
            onDismissDiscard = viewModel::onDismissDiscard,
        ),
    )
}

@Composable
private fun NoteEditorRoute(navController: NavHostController) {
    val viewModel: NoteEditorViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    NoteEditorScreen(
        state = state,
        actions = NoteEditorActions(
            onTitleChange = viewModel::onTitleChange,
            onContentChange = viewModel::onContentChange,
            onBack = {
                scope.launch {
                    if (viewModel.onLeave()) {
                        navController.popBackStack()
                    }
                }
            },
        ),
    )
}

private fun NavHostController.navigateToTab(tab: AppTab) {
    navigate(tab.route) {
        popUpTo(AppRoute.Tasks) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun NavHostController.openWidgetList(route: String) {
    navigate(route) {
        popUpTo(AppRoute.Tasks) { inclusive = false }
        launchSingleTop = true
    }
}
