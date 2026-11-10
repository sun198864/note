package com.davidsun.note.domain.vocab

interface VocabSpeaker {
    fun speak(text: String)

    fun stop()
}
