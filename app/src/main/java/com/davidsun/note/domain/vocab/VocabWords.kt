package com.davidsun.note.domain.vocab

private val ListedForms = Regex("""^(\S+)\s*\((.+)\)""")
private val SuffixForm = Regex("""^([^(]+)\(([^)]+)\)""")

fun vocabHeadwordForms(word: String): List<String> {
    val forms = mutableListOf<String>()
    word.split("/").forEach { piece ->
        val part = piece.trim()
        if (part.isEmpty()) return@forEach
        val listed = ListedForms.matchEntire(part)
        if (listed != null && "," in listed.groupValues[2]) {
            forms += listed.groupValues[1]
            listed.groupValues[2].split(",").forEach { item ->
                val form = item.trim()
                if (form.isNotEmpty()) forms += form
            }
            return@forEach
        }
        val suffix = SuffixForm.matchEntire(part)
        if (suffix != null) {
            val base = suffix.groupValues[1].trim()
            forms += base
            forms += base + suffix.groupValues[2].trim()
            return@forEach
        }
        forms += part
    }
    return forms.distinct()
}

fun vocabPronunciation(word: String): String {
    val head = vocabHeadwordForms(word).firstOrNull().orEmpty()
        .replace('\u2019', '\'')
    return head.filter { char ->
        char.isLetter() || char == '\'' || char == '-' || char == ' '
    }.trim()
}
