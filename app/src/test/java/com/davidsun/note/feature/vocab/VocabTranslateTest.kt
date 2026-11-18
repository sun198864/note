package com.davidsun.note.feature.vocab

import org.junit.Assert.assertEquals
import org.junit.Test

class VocabTranslateTest {
    @Test
    fun englishGoesToTraditionalChinese() {
        assertEquals("en" to "zh-TW", translationLanguages("abandon"))
    }

    @Test
    fun chineseGoesToEnglish() {
        assertEquals("zh-TW" to "en", translationLanguages("放棄"))
    }
}
