package com.davidsun.note.domain

import com.davidsun.note.domain.model.Note

object ListOrder {
    fun <T> move(items: List<T>, fromKey: String, toKey: String, key: (T) -> String): List<T>? {
        val fromIndex = items.indexOfFirst { key(it) == fromKey }
        val toIndex = items.indexOfFirst { key(it) == toKey }
        if (fromIndex < 0 || toIndex < 0 || fromIndex == toIndex) return null
        return items.toMutableList().apply {
            add(toIndex, removeAt(fromIndex))
        }
    }

    fun merge(allIds: List<String>, reorderedVisibleIds: List<String>): List<String> {
        if (reorderedVisibleIds.isEmpty()) return allIds
        val pending = ArrayDeque(reorderedVisibleIds)
        val visible = reorderedVisibleIds.toSet()
        return allIds.map { id ->
            if (id in visible && pending.isNotEmpty()) pending.removeFirst() else id
        }
    }

    fun notes(notes: List<Note>): List<Note> =
        notes.sortedWith(compareBy<Note> { it.sortIndex }.thenByDescending { it.updatedAt })
}
