package com.davidsun.note.feature.settings

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.davidsun.note.R
import com.davidsun.note.domain.model.CloudSyncIssue
import com.davidsun.note.domain.model.FontScale
import com.davidsun.note.navigation.AppTab
import com.davidsun.note.navigation.NoteBottomBar
import com.davidsun.note.ui.theme.NoteTheme
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    notificationGranted: Boolean,
    exactAlarmGranted: Boolean,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
    bottomBar: @Composable () -> Unit = {},
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.tab_settings)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        bottomBar = bottomBar,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SettingsCard(title = stringResource(R.string.font_size)) {
                FontScaleSlider(
                    scale = state.fontScale,
                    onScaleSelected = actions.onFontScaleSelected,
                )
            }
            SettingsCard(title = stringResource(R.string.settings_notification)) {
                if (notificationGranted) {
                    Text(
                        text = stringResource(R.string.notification_granted),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.notification_denied),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                    OutlinedButton(onClick = actions.onOpenNotificationSettings) {
                        Text(text = stringResource(R.string.open_notification_settings))
                    }
                }
                if (!exactAlarmGranted) {
                    Text(
                        text = stringResource(R.string.exact_alarm_denied),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                    OutlinedButton(onClick = actions.onOpenExactAlarmSettings) {
                        Text(text = stringResource(R.string.open_exact_alarm_settings))
                    }
                }
            }
            SettingsCard(title = stringResource(R.string.settings_cloud)) {
                CloudBackupContent(state = state, actions = actions)
            }
        }
    }
}

@Composable
private fun CloudBackupContent(
    state: SettingsUiState,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
) {
    if (state.isSignedIn) {
        Text(
            text = state.accountLabel?.let { stringResource(R.string.settings_cloud_signed_in, it) }
                ?: stringResource(R.string.settings_cloud_signed_in_no_email),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = when {
                state.isSyncing -> stringResource(R.string.settings_cloud_syncing)
                state.lastSyncedLabel != null -> stringResource(R.string.settings_cloud_last_sync, state.lastSyncedLabel)
                else -> stringResource(R.string.settings_cloud_not_synced)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (state.switchedAccount) {
            Text(
                text = stringResource(R.string.settings_cloud_switched),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        CloudIssueText(issue = state.issue)
        OutlinedButton(onClick = actions.onSignOut, enabled = !state.isSigningIn) {
            Text(text = stringResource(R.string.settings_cloud_sign_out))
        }
    } else {
        Text(
            text = stringResource(R.string.settings_cloud_signed_out),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (state.signInFailed) {
            Text(
                text = stringResource(R.string.settings_cloud_sign_in_failed),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Button(onClick = actions.onSignIn, enabled = !state.isSigningIn, modifier = modifier) {
            Text(
                text = stringResource(
                    if (state.isSigningIn) R.string.settings_cloud_signing_in else R.string.settings_cloud_sign_in,
                ),
            )
        }
    }
}

@Composable
private fun CloudIssueText(issue: CloudSyncIssue) {
    val text = when (issue) {
        CloudSyncIssue.None -> return
        CloudSyncIssue.Retrying -> stringResource(R.string.settings_cloud_retrying)
        CloudSyncIssue.PermissionDenied -> stringResource(R.string.settings_cloud_permission)
    }
    val color = when (issue) {
        CloudSyncIssue.PermissionDenied -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = color)
}

@Composable
private fun SettingsCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun FontScaleSlider(
    scale: FontScale,
    onScaleSelected: (FontScale) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lastIndex = FontScale.entries.lastIndex
    Column(modifier = modifier.fillMaxWidth()) {
        Slider(
            value = scale.ordinal.toFloat(),
            onValueChange = { value ->
                val next = FontScale.entries[value.roundToInt().coerceIn(0, lastIndex)]
                if (next != scale) onScaleSelected(next)
            },
            valueRange = 0f..lastIndex.toFloat(),
            steps = (FontScale.entries.size - 2).coerceAtLeast(0),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            FontScale.entries.forEach { item ->
                Text(
                    text = stringResource(item.labelRes()),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (item == scale) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

private fun FontScale.labelRes(): Int = when (this) {
    FontScale.SMALL -> R.string.font_small
    FontScale.MEDIUM -> R.string.font_medium
    FontScale.LARGE -> R.string.font_large
}

@Preview(name = "Light", showBackground = true, widthDp = 360, heightDp = 800)
@Preview(name = "Dark", showBackground = true, widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SettingsScreenPreview() {
    NoteTheme {
        SettingsScreen(
            state = SettingsUiState(fontScale = FontScale.MEDIUM),
            notificationGranted = true,
            exactAlarmGranted = true,
            actions = previewSettingsActions(),
            bottomBar = { NoteBottomBar(selected = AppTab.Settings, onSelect = {}) },
        )
    }
}

@Preview(name = "Signed in", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun SettingsSignedInPreview() {
    NoteTheme {
        SettingsScreen(
            state = SettingsUiState(
                isSignedIn = true,
                accountLabel = "sun@example.com",
                lastSyncedLabel = "10月20日 週二 15:40",
            ),
            notificationGranted = true,
            exactAlarmGranted = true,
            actions = previewSettingsActions(),
        )
    }
}

@Preview(name = "Sign-in failed", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun SettingsSignInFailedPreview() {
    NoteTheme {
        SettingsScreen(
            state = SettingsUiState(signInFailed = true, issue = CloudSyncIssue.Retrying),
            notificationGranted = true,
            exactAlarmGranted = false,
            actions = previewSettingsActions(),
        )
    }
}

@Preview(name = "Notifications off", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun SettingsNotificationsOffPreview() {
    NoteTheme {
        SettingsScreen(
            state = SettingsUiState(),
            notificationGranted = false,
            exactAlarmGranted = false,
            actions = previewSettingsActions(),
        )
    }
}

@Preview(name = "Large font", showBackground = true, widthDp = 360, heightDp = 800, fontScale = 1.3f)
@Composable
private fun SettingsLargeFontPreview() {
    NoteTheme(fontScale = FontScale.LARGE) {
        SettingsScreen(
            state = SettingsUiState(fontScale = FontScale.LARGE),
            notificationGranted = true,
            exactAlarmGranted = true,
            actions = previewSettingsActions(),
        )
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FontScaleSliderPreview() {
    NoteTheme {
        FontScaleSlider(scale = FontScale.MEDIUM, onScaleSelected = {})
    }
}

private fun previewSettingsActions() = SettingsActions(
    onFontScaleSelected = {},
    onOpenNotificationSettings = {},
    onOpenExactAlarmSettings = {},
    onSignIn = {},
    onSignOut = {},
)
