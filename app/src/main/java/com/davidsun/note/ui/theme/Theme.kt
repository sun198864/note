package com.davidsun.note.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.davidsun.note.domain.model.FontScale

private val LightColorScheme = lightColorScheme(
    primary = NotePrimary,
    onPrimary = NoteOnPrimary,
    primaryContainer = NotePrimaryContainer,
    onPrimaryContainer = NoteOnPrimaryContainer,
    secondary = NoteSecondary,
    onSecondary = NoteOnSecondary,
    background = NoteBackground,
    onBackground = NoteOnBackground,
    surface = NoteSurface,
    onSurface = NoteOnSurface,
    surfaceVariant = NoteSurfaceVariant,
    onSurfaceVariant = NoteOnSurfaceVariant,
    error = NoteError,
    onError = NoteOnError,
    outline = NoteOutline,
)

private val DarkColorScheme = darkColorScheme(
    primary = NotePrimaryDark,
    onPrimary = NoteOnPrimaryDark,
    primaryContainer = NotePrimaryContainerDark,
    onPrimaryContainer = NoteOnPrimaryContainerDark,
    secondary = NoteSecondaryDark,
    onSecondary = NoteOnSecondaryDark,
    background = NoteBackgroundDark,
    onBackground = NoteOnBackgroundDark,
    surface = NoteSurfaceDark,
    onSurface = NoteOnSurfaceDark,
    surfaceVariant = NoteSurfaceVariantDark,
    onSurfaceVariant = NoteOnSurfaceVariantDark,
    error = NoteErrorDark,
    onError = NoteOnErrorDark,
    outline = NoteOutlineDark,
)

@Composable
fun NoteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    fontScale: FontScale = FontScale.MEDIUM,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography.withScale(fontScale),
        content = content,
    )
}
