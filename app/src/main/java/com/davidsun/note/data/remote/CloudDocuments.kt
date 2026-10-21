package com.davidsun.note.data.remote

import com.davidsun.note.domain.model.Note
import com.davidsun.note.domain.model.ReminderType
import com.davidsun.note.domain.model.SyncStatus
import com.davidsun.note.domain.model.Task
import com.google.firebase.firestore.DocumentSnapshot

internal fun Note.toFirestoreMap(): Map<String, Any> = cloudMap(
    "title" to title,
    "content" to content,
    "createdAt" to createdAt,
    "updatedAt" to updatedAt,
    "deletedAt" to deletedAt,
    "revision" to revision,
)

internal fun Task.toFirestoreMap(): Map<String, Any> = cloudMap(
    "title" to title,
    "description" to description,
    "reminderType" to reminderType.name,
    "reminderAt" to reminderAt,
    "intervalDays" to intervalDays,
    "weekdays" to weekdays,
    "isCompleted" to isCompleted,
    "completedAt" to completedAt,
    "isHidden" to isHidden,
    "createdAt" to createdAt,
    "updatedAt" to updatedAt,
    "deletedAt" to deletedAt,
    "revision" to revision,
)

internal fun DocumentSnapshot.toNote(): Note? = data?.let { noteFromMap(id, it) }

internal fun DocumentSnapshot.toTask(): Task? = data?.let { taskFromMap(id, it) }

internal fun noteFromMap(id: String, data: Map<String, Any?>): Note? {
    if (id.isEmpty()) return null
    val title = data.string("title") ?: return null
    val content = data.string("content") ?: return null
    val createdAt = data.long("createdAt") ?: return null
    val updatedAt = data.long("updatedAt") ?: return null
    val revision = data.long("revision") ?: return null
    if (title.length > 2000 || content.length > 200_000) return null
    if (createdAt < 0 || updatedAt < 0 || revision < 0) return null
    val deletedAt = data.optionalLong("deletedAt") ?: return null
    return Note(
        id = id,
        title = title,
        content = content,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt.value,
        revision = revision,
        syncStatus = SyncStatus.SYNCED,
    )
}

internal fun taskFromMap(id: String, data: Map<String, Any?>): Task? {
    if (id.isEmpty()) return null
    val title = data.string("title") ?: return null
    val reminderType = data.string("reminderType")?.let { raw ->
        ReminderType.entries.firstOrNull { it.name == raw }
    } ?: return null
    val createdAt = data.long("createdAt") ?: return null
    val updatedAt = data.long("updatedAt") ?: return null
    val revision = data.long("revision") ?: return null
    val isCompleted = data.bool("isCompleted") ?: return null
    val isHidden = data.bool("isHidden") ?: return null
    if (title.isEmpty() || title.length > 2000) return null
    if (createdAt < 0 || updatedAt < 0 || revision < 0) return null
    val description = data.optionalString("description") ?: return null
    val reminderAt = data.optionalLong("reminderAt") ?: return null
    val intervalDays = data.optionalInt("intervalDays") ?: return null
    val weekdays = data.optionalInt("weekdays") ?: return null
    val completedAt = data.optionalLong("completedAt") ?: return null
    val deletedAt = data.optionalLong("deletedAt") ?: return null
    if (description.value != null && description.value.length > 20_000) return null
    return Task(
        id = id,
        title = title,
        description = description.value,
        reminderType = reminderType,
        reminderAt = reminderAt.value,
        intervalDays = intervalDays.value,
        weekdays = weekdays.value,
        isCompleted = isCompleted,
        completedAt = completedAt.value,
        isHidden = isHidden,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt.value,
        revision = revision,
        syncStatus = SyncStatus.SYNCED,
    )
}

private fun cloudMap(vararg pairs: Pair<String, Any?>): Map<String, Any> {
    val values = HashMap<String, Any?>()
    pairs.forEach { (key, value) -> values[key] = value }
    @Suppress("UNCHECKED_CAST")
    return values as Map<String, Any>
}

private fun Map<String, Any?>.string(key: String): String? = this[key] as? String

private fun Map<String, Any?>.bool(key: String): Boolean? = this[key] as? Boolean

private fun Map<String, Any?>.long(key: String): Long? = when (val value = this[key]) {
    is Long -> value
    is Int -> value.toLong()
    is Double -> value.toLong()
    else -> null
}

private class Optional<T>(val value: T?)

private fun Map<String, Any?>.optionalString(key: String): Optional<String>? = when (val value = this[key]) {
    null -> Optional(null)
    is String -> Optional(value)
    else -> null
}

private fun Map<String, Any?>.optionalLong(key: String): Optional<Long>? = when (val value = this[key]) {
    null -> Optional(null)
    is Long -> Optional(value)
    is Int -> Optional(value.toLong())
    is Double -> Optional(value.toLong())
    else -> null
}

private fun Map<String, Any?>.optionalInt(key: String): Optional<Int>? = when (val value = this[key]) {
    null -> Optional(null)
    is Int -> Optional(value)
    is Long -> Optional(value.toInt())
    is Double -> Optional(value.toInt())
    else -> null
}
