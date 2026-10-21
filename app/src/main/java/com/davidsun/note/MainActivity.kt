package com.davidsun.note

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.davidsun.note.navigation.NoteApp
import com.davidsun.note.ui.theme.NoteTheme
import com.davidsun.note.widget.WidgetRoutes
import com.davidsun.note.widget.readWidgetOpen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val appViewModel: AppViewModel by viewModels()
    private val pendingRoute = mutableStateOf<String?>(null)
    private val pendingFocusId = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) {
            acceptWidgetIntent(intent)
        }
        setContent {
            val fontScale by appViewModel.fontScale.collectAsStateWithLifecycle()
            val route by pendingRoute
            val focusId by pendingFocusId
            NoteTheme(fontScale = fontScale) {
                NoteApp(
                    pendingRoute = route,
                    focusId = focusId,
                    onPendingRouteConsumed = { pendingRoute.value = null },
                    onFocusConsumed = { pendingFocusId.value = null },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        acceptWidgetIntent(intent)
    }

    private fun acceptWidgetIntent(intent: Intent) {
        val request = intent.readWidgetOpen() ?: return
        pendingRoute.value = request.route
        pendingFocusId.value = request.focusId
        intent.removeExtra(WidgetRoutes.Extra)
        intent.removeExtra(WidgetRoutes.ExtraFocusId)
    }
}
