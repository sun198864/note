package com.davidsun.note.data.vocab

import android.content.Context
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import com.davidsun.note.domain.vocab.VocabSpeaker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidVocabSpeaker @Inject constructor(
    @ApplicationContext context: Context,
) : VocabSpeaker {
    private val appContext = context.applicationContext
    private val lock = Any()
    private var engine: TextToSpeech? = null
    private var ready = false
    private var pending: String? = null

    override fun speak(text: String) {
        val spoken = text.trim()
        if (spoken.isEmpty()) return
        synchronized(lock) {
            val current = engine
            if (current != null && ready) {
                current.speak(spoken, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
                return
            }
            pending = spoken
            if (current == null) {
                engine = TextToSpeech(appContext, this::onInit)
            }
        }
    }

    override fun stop() {
        synchronized(lock) {
            pending = null
            engine?.stop()
        }
    }

    private fun onInit(status: Int) {
        synchronized(lock) {
            val current = engine ?: return
            if (status != TextToSpeech.SUCCESS) {
                current.shutdown()
                engine = null
                ready = false
                pending = null
                return
            }
            val language = current.setLanguage(Locale.US)
            if (language == TextToSpeech.LANG_MISSING_DATA || language == TextToSpeech.LANG_NOT_SUPPORTED) {
                current.setLanguage(Locale.ENGLISH)
            }
            current.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            ready = true
            val queued = pending
            pending = null
            if (!queued.isNullOrBlank()) {
                current.speak(queued, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
            }
        }
    }

    private companion object {
        const val UTTERANCE_ID = "vocab"
    }
}
