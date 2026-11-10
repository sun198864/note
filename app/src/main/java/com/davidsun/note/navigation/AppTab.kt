package com.davidsun.note.navigation

import androidx.annotation.StringRes
import com.davidsun.note.R

enum class AppTab(
    @StringRes val labelRes: Int,
    val route: String,
) {
    Tasks(R.string.tab_tasks, AppRoute.Tasks),
    Notes(R.string.tab_notes, AppRoute.Notes),
    Vocab(R.string.tab_vocab, AppRoute.Vocab),
    Settings(R.string.tab_settings, AppRoute.Settings),
}
