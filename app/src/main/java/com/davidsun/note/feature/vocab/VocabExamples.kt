package com.davidsun.note.feature.vocab

private val LeadingNumber = Regex("""^\d+\.\s*""")

internal fun pairedExamples(example: String, exampleZh: String): List<Pair<String, String>> {
    val english = example.lines().map { line -> line.trim() }.filter { line -> line.isNotEmpty() }
    val chinese = exampleZh.lines().map { line -> line.trim() }.filter { line -> line.isNotEmpty() }
    return english.mapIndexed { index, line ->
        line.replace(LeadingNumber, "") to chinese.getOrElse(index) { "" }.replace(LeadingNumber, "")
    }
}

internal fun exampleLineSpeech(line: String): String =
    line.replace(LeadingNumber, "").trim()

internal fun exampleSpeech(example: String): String =
    pairedExamples(example, "").joinToString(separator = " ") { (line, _) ->
        exampleLineSpeech(line)
    }
