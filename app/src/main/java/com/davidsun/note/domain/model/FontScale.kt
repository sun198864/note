package com.davidsun.note.domain.model

enum class FontScale(val storageKey: String, val multiplier: Float) {
    SMALL("small", 0.9f),
    MEDIUM("medium", 1f),
    LARGE("large", 1.2f),
    ;

    companion object {
        fun fromStorageKey(key: String?): FontScale =
            entries.firstOrNull { it.storageKey == key } ?: MEDIUM
    }
}
