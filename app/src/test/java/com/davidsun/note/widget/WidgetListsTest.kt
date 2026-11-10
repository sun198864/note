package com.davidsun.note.widget

import com.davidsun.note.domain.model.GroupedTasks
import com.davidsun.note.domain.model.Note
import com.davidsun.note.domain.model.ReminderType
import com.davidsun.note.domain.model.SyncStatus
import com.davidsun.note.domain.model.Task
import com.davidsun.note.domain.model.VocabEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetListsTest {
    @Test
    fun openTasksKeepTodayUpcomingThenUndated() {
        val grouped = GroupedTasks(
            today = listOf(task("1", "今天")),
            upcoming = listOf(task("2", "之後")),
            undated = listOf(task("3", "未定")),
            completed = listOf(task("4", "完成", isCompleted = true)),
            hidden = listOf(task("5", "隱藏", isHidden = true)),
        )

        assertEquals(listOf("今天", "之後", "未定"), WidgetLists.openTasks(grouped).map { it.title })
    }

    @Test
    fun recentNotesDropDeletedAndKeepNewestFirst() {
        val notes = WidgetLists.recentNotes(
            listOf(
                note("old", "舊", updatedAt = 1),
                note("gone", "已刪", updatedAt = 9, deletedAt = 9),
                note("new", "新", updatedAt = 5),
            ),
        )

        assertEquals(listOf("新", "舊"), notes.map { it.title })
    }

    @Test
    fun recentNotesPreferSortIndexOverUpdatedAt() {
        val notes = WidgetLists.recentNotes(
            listOf(
                note("new", "新", updatedAt = 5, sortIndex = 1),
                note("old", "舊", updatedAt = 1, sortIndex = 0),
            ),
        )

        assertEquals(listOf("舊", "新"), notes.map { it.title })
    }

    @Test
    fun summaryCollapsesBlankLines() {
        assertEquals("先寫這句 再寫這句", WidgetLists.summary("先寫這句\n再寫這句"))
        assertEquals("", WidgetLists.summary("  \n "))
    }

    @Test
    fun modeCyclesTasksNotesThenVocab() {
        assertEquals(WidgetListMode.Notes, WidgetListMode.Tasks.next())
        assertEquals(WidgetListMode.Vocab, WidgetListMode.Notes.next())
        assertEquals(WidgetListMode.Tasks, WidgetListMode.Vocab.next())
        assertEquals(WidgetListMode.Tasks, WidgetListMode.from(null))
        assertEquals(WidgetListMode.Vocab, WidgetListMode.from("Vocab"))
    }

    @Test
    fun sampledWordsFollowTheSharedSampleOrder() {
        val entries = listOf(
            word("1", "a"),
            word("2", "abandon"),
            word("3", "ability"),
        )

        assertEquals(
            listOf("ability", "a"),
            WidgetLists.sampledWords(entries, listOf("3", "missing", "1")).map { it.word },
        )
    }

    @Test
    fun sampledWordsStopAtFifteen() {
        val entries = List(20) { index -> word(index.toString(), "w$index") }
        val ids = entries.map { it.id }

        assertEquals(15, WidgetLists.sampledWords(entries, ids).size)
        assertEquals("w0", WidgetLists.sampledWords(entries, ids).first().word)
    }

    @Test
    fun vocabSubtitleUsesPartOfSpeechAndMeaning() {
        assertEquals("v. 放棄", WidgetLists.vocabSubtitle(word("1", "abandon", meaning = "放棄")))
        assertEquals("v. shorten", WidgetLists.vocabSubtitle(word("1", "abbreviate", meaning = "", definition = "shorten")))
        assertEquals("", WidgetLists.vocabSubtitle(word("1", "able", partOfSpeech = " ", meaning = "")))
    }

    private fun task(
        id: String,
        title: String,
        isCompleted: Boolean = false,
        isHidden: Boolean = false,
    ) = Task(
        id = id,
        title = title,
        description = null,
        reminderType = ReminderType.NONE,
        reminderAt = null,
        intervalDays = null,
        isCompleted = isCompleted,
        completedAt = null,
        isHidden = isHidden,
        createdAt = 0,
        updatedAt = 0,
        deletedAt = null,
        revision = 1,
        syncStatus = SyncStatus.PENDING,
    )

    private fun note(
        id: String,
        title: String,
        updatedAt: Long,
        deletedAt: Long? = null,
        sortIndex: Long = 0,
    ) = Note(
        id = id,
        title = title,
        content = "",
        createdAt = 0,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        revision = 1,
        syncStatus = SyncStatus.PENDING,
        sortIndex = sortIndex,
    )

    private fun word(
        id: String,
        headword: String,
        partOfSpeech: String = "v.",
        meaning: String = "意思",
        definition: String = "",
    ) = VocabEntry(
        id = id,
        word = headword,
        partOfSpeech = partOfSpeech,
        level = 1,
        meaning = meaning,
        phonetic = "",
        definition = definition,
        example = "",
    )
}
