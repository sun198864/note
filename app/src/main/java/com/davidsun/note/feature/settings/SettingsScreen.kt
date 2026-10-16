package com.davidsun.note.feature.settings

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.davidsun.note.R
import com.davidsun.note.domain.model.FontScale
import com.davidsun.note.navigation.AppTab
import com.davidsun.note.navigation.NoteBottomBar
import com.davidsun.note.ui.theme.NoteTheme

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
                FontScale.entries.forEach { scale ->
                    FontScaleOption(
                        label = stringResource(scale.labelRes()),
                        selected = state.fontScale == scale,
                        onClick = { actions.onFontScaleSelected(scale) },
                    )
                }
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
                Text(
                    text = stringResource(R.string.settings_cloud_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.settings_cloud_sync_time),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
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
fun FontScaleOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
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

@Preview(showBackground = true)
@Composable
private fun FontScaleOptionPreview() {
    NoteTheme {
        FontScaleOption(label = "中", selected = true, onClick = {})
    }
}

private fun previewSettingsActions() = SettingsActions(
    onFontScaleSelected = {},
    onOpenNotificationSettings = {},
    onOpenExactAlarmSettings = {},
)
