package com.davidsun.note.widget

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.davidsun.note.MainActivity
import com.davidsun.note.R
import com.davidsun.note.navigation.AppRoute
import com.davidsun.note.ui.theme.NoteOnPrimaryContainer
import com.davidsun.note.ui.theme.NoteOnPrimaryContainerDark
import com.davidsun.note.ui.theme.NoteOnSurface
import com.davidsun.note.ui.theme.NoteOnSurfaceDark
import com.davidsun.note.ui.theme.NoteOnSurfaceVariant
import com.davidsun.note.ui.theme.NoteOnSurfaceVariantDark
import com.davidsun.note.ui.theme.NoteOutline
import com.davidsun.note.ui.theme.NoteOutlineDark
import com.davidsun.note.ui.theme.NotePrimary
import com.davidsun.note.ui.theme.NotePrimaryContainer
import com.davidsun.note.ui.theme.NotePrimaryContainerDark
import com.davidsun.note.ui.theme.NotePrimaryDark
import com.davidsun.note.ui.theme.NoteSurface
import com.davidsun.note.ui.theme.NoteSurfaceDark
import dagger.hilt.android.EntryPointAccessors

private val ModeKey = stringPreferencesKey("list_mode")

private enum class WidgetListMode {
    Tasks,
    Notes,
    ;

    fun toggled(): WidgetListMode = if (this == Tasks) Notes else Tasks

    companion object {
        fun from(value: String?): WidgetListMode =
            entries.firstOrNull { it.name == value } ?: Tasks
    }
}

class NoteListWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java,
        ).snapshotLoader().load()
        provideContent {
            val mode = WidgetListMode.from(currentState<Preferences>()[ModeKey])
            NoteListWidgetContent(
                mode = mode,
                rows = if (mode == WidgetListMode.Tasks) snapshot.tasks else snapshot.notes,
            )
        }
    }
}

class NoteListWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NoteListWidget()
}

class ToggleWidgetModeAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: androidx.glance.action.ActionParameters) {
        updateAppWidgetState(context, glanceId) { prefs ->
            val next = WidgetListMode.from(prefs[ModeKey]).toggled()
            prefs[ModeKey] = next.name
        }
        NoteListWidget().update(context, glanceId)
    }
}

fun Intent.readWidgetOpen(): WidgetOpenRequest? {
    val route = getStringExtra(WidgetRoutes.Extra) ?: return null
    val request = WidgetRoutes.openRequest(route) ?: return null
    val focusId = getStringExtra(WidgetRoutes.ExtraFocusId)?.takeIf { it.isNotBlank() && !it.contains('/') }
    return if (focusId == null) request else request.copy(focusId = focusId)
}

@Composable
private fun NoteListWidgetContent(
    mode: WidgetListMode,
    rows: List<WidgetRow>,
) {
    val context = LocalContext.current
    val listRoute = if (mode == WidgetListMode.Tasks) AppRoute.Tasks else AppRoute.Notes
    val title = context.getString(if (mode == WidgetListMode.Tasks) R.string.tab_tasks else R.string.tab_notes)
    val empty = context.getString(
        if (mode == WidgetListMode.Tasks) R.string.widget_empty_tasks else R.string.widget_empty_notes,
    )
    val toggleDescription = context.getString(
        if (mode == WidgetListMode.Tasks) R.string.widget_show_notes else R.string.widget_show_tasks,
    )
    val toggleIcon = if (mode == WidgetListMode.Tasks) R.drawable.ic_widget_note else R.drawable.ic_widget_task
    val titleColor = ColorProvider(NoteOnSurface, NoteOnSurfaceDark)
    val subtitleColor = ColorProvider(NoteOnSurfaceVariant, NoteOnSurfaceVariantDark)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(NoteSurface, NoteSurfaceDark))
            .cornerRadius(20.dp),
    ) {
        LazyColumn(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            item(itemId = HEADER_ID) {
                Column(modifier = GlanceModifier.fillMaxWidth()) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = title,
                            modifier = GlanceModifier
                                .defaultWeight()
                                .clickable(actionStart(context, listRoute)),
                            maxLines = 1,
                            style = TextStyle(
                                color = titleColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                        Box(
                            modifier = GlanceModifier
                                .size(32.dp)
                                .clickable(actionRunCallback<ToggleWidgetModeAction>()),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = GlanceModifier
                                    .size(22.dp)
                                    .cornerRadius(11.dp)
                                    .background(ColorProvider(NotePrimaryContainer, NotePrimaryContainerDark)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Image(
                                    provider = ImageProvider(toggleIcon),
                                    contentDescription = toggleDescription,
                                    modifier = GlanceModifier.size(14.dp),
                                    colorFilter = ColorFilter.tint(
                                        ColorProvider(NoteOnPrimaryContainer, NoteOnPrimaryContainerDark),
                                    ),
                                )
                            }
                        }
                    }
                    Spacer(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .padding(top = 14.dp)
                            .height(1.dp)
                            .background(ColorProvider(NoteOutline, NoteOutlineDark)),
                    )
                }
            }
            if (rows.isEmpty()) {
                item(itemId = EMPTY_ID) {
                    Text(
                        text = empty,
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .clickable(actionStart(context, listRoute)),
                        style = TextStyle(color = subtitleColor, fontSize = 13.sp),
                    )
                }
            } else {
                items(items = rows, itemId = { row -> row.id.hashCode().toLong() }) { row ->
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .clickable(actionStart(context, listRoute, row.id))
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = GlanceModifier.defaultWeight()) {
                            Text(
                                text = row.title,
                                maxLines = 1,
                                style = TextStyle(
                                    color = titleColor,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                ),
                            )
                            if (row.subtitle.isNotBlank()) {
                                Text(
                                    text = row.subtitle,
                                    maxLines = 2,
                                    style = TextStyle(
                                        color = subtitleColor,
                                        fontSize = 12.sp,
                                    ),
                                )
                            }
                        }
                        Image(
                            provider = ImageProvider(
                                if (row.important) R.drawable.ic_star else R.drawable.ic_star_border,
                            ),
                            contentDescription = null,
                            modifier = GlanceModifier
                                .padding(start = 8.dp)
                                .size(18.dp),
                            colorFilter = ColorFilter.tint(
                                if (row.important) {
                                    ColorProvider(NotePrimary, NotePrimaryDark)
                                } else {
                                    ColorProvider(NoteOnSurfaceVariant, NoteOnSurfaceVariantDark)
                                },
                            ),
                        )
                    }
                }
            }
        }
    }
}

private fun actionStart(context: Context, route: String, focusId: String? = null) = actionStartActivity(
    Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
            Intent.FLAG_ACTIVITY_SINGLE_TOP
        putExtra(WidgetRoutes.Extra, route)
        if (!focusId.isNullOrBlank()) putExtra(WidgetRoutes.ExtraFocusId, focusId)
    },
)

private const val HEADER_ID = -1L
private const val EMPTY_ID = -2L

@Preview(name = "待辦 Light", widthDp = 110, heightDp = 110)
@Preview(name = "待辦 Dark", widthDp = 300, heightDp = 180, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "待辦 Large font", widthDp = 300, heightDp = 180, fontScale = 1.3f)
@Composable
private fun NoteListWidgetTasksPreview() {
    NoteListWidgetContent(
        mode = WidgetListMode.Tasks,
        rows = listOf(
            WidgetRow(id = "1", title = "買菜", subtitle = "超市營業到九點", important = true),
            WidgetRow(id = "2", title = "回覆信件", subtitle = ""),
        ),
    )
}

@Preview(name = "筆記 Light", widthDp = 300, heightDp = 180)
@Preview(name = "筆記 Dark", widthDp = 300, heightDp = 180, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "筆記 Large font", widthDp = 300, heightDp = 180, fontScale = 1.3f)
@Composable
private fun NoteListWidgetNotesPreview() {
    NoteListWidgetContent(
        mode = WidgetListMode.Notes,
        rows = listOf(
            WidgetRow(id = "1", title = "會議紀錄", subtitle = "下週先確認議程", important = true),
            WidgetRow(id = "2", title = "未命名筆記", subtitle = "沒有內容"),
        ),
    )
}

@Preview(name = "空列表", widthDp = 110, heightDp = 110)
@Composable
private fun NoteListWidgetEmptyPreview() {
    NoteListWidgetContent(mode = WidgetListMode.Tasks, rows = emptyList())
}
