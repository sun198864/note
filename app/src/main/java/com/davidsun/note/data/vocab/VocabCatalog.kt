package com.davidsun.note.data.vocab

import android.content.Context
import com.davidsun.note.domain.model.VocabEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VocabCatalog @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    @Volatile
    private var cache: List<VocabEntry>? = null

    suspend fun entries(): List<VocabEntry> = withContext(Dispatchers.IO) {
        cache ?: synchronized(this@VocabCatalog) {
            cache ?: read().also { loaded -> cache = loaded }
        }
    }

    private fun read(): List<VocabEntry> {
        val text = context.assets.open(ASSET).bufferedReader().use { reader -> reader.readText() }
        val array = JSONArray(text)
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                val id = item.optString("id")
                val word = item.optString("word")
                if (id.isBlank() || word.isBlank()) continue
                add(
                    VocabEntry(
                        id = id,
                        word = word,
                        partOfSpeech = item.optString("pos"),
                        level = item.optInt("level"),
                        meaning = item.optString("meaning"),
                        phonetic = item.optString("phonetic"),
                        definition = item.optString("definition"),
                        example = item.optString("example"),
                        exampleZh = item.optString("exampleZh"),
                    ),
                )
            }
        }
    }

    private companion object {
        const val ASSET = "vocab.json"
    }
}
