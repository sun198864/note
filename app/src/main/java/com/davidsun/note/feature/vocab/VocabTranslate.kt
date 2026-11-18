package com.davidsun.note.feature.vocab

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

private const val GoogleTranslatePackage = "com.google.android.apps.translate"

fun Context.openVocabTranslation(text: String) {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return
    val (source, target) = translationLanguages(trimmed)
    val openedApp = startIfAvailable(googleTranslateIntent(trimmed, source, target)) ||
        startIfAvailable(processTextIntent(trimmed))
    if (openedApp) return
    startIfAvailable(Intent(Intent.ACTION_VIEW, vocabTranslateUri(trimmed, source, target)))
}

private fun Context.startIfAvailable(intent: Intent): Boolean =
    try {
        startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }

internal fun googleTranslateIntent(text: String, source: String, target: String): Intent =
    Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        setPackage(GoogleTranslatePackage)
        putExtra(Intent.EXTRA_TEXT, text)
        putExtra("key_text_input", text)
        putExtra("key_language_from", source)
        putExtra("key_language_to", target)
        putExtra("from", source)
        putExtra("to", target)
    }

internal fun processTextIntent(text: String): Intent =
    Intent(Intent.ACTION_PROCESS_TEXT).apply {
        type = "text/plain"
        setPackage(GoogleTranslatePackage)
        putExtra(Intent.EXTRA_PROCESS_TEXT, text)
        putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, true)
    }

internal fun vocabTranslateUri(text: String, source: String, target: String): Uri =
    Uri.Builder()
        .scheme("https")
        .authority("translate.google.com")
        .path("/")
        .appendQueryParameter("sl", source)
        .appendQueryParameter("tl", target)
        .appendQueryParameter("text", text)
        .appendQueryParameter("op", "translate")
        .build()

internal fun translationLanguages(text: String): Pair<String, String> {
    val chinese = text.any { character -> character.code in 0x4E00..0x9FFF }
    return if (chinese) "zh-TW" to "en" else "en" to "zh-TW"
}
