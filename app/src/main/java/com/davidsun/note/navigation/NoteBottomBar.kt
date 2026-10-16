package com.davidsun.note.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.davidsun.note.ui.theme.NoteTheme

@Composable
fun NoteBottomBar(
    selected: AppTab,
    onSelect: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        AppTab.entries.forEach { tab ->
            val icon = tabIcon(tab = tab, selected = tab == selected)
            NavigationBarItem(
                selected = tab == selected,
                onClick = { onSelect(tab) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = stringResource(tab.labelRes),
                    )
                },
                label = { Text(text = stringResource(tab.labelRes)) },
            )
        }
    }
}

private fun tabIcon(tab: AppTab, selected: Boolean): ImageVector = when (tab) {
    AppTab.Tasks -> if (selected) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle
    AppTab.Notes -> if (selected) Icons.Filled.Description else Icons.Outlined.Description
    AppTab.Settings -> if (selected) Icons.Filled.Settings else Icons.Outlined.Settings
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun NoteBottomBarPreview() {
    NoteTheme {
        NoteBottomBar(selected = AppTab.Tasks, onSelect = {})
    }
}
