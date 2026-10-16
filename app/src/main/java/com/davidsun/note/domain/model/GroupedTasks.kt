package com.davidsun.note.domain.model

data class GroupedTasks(
    val today: List<Task>,
    val upcoming: List<Task>,
    val undated: List<Task>,
    val completed: List<Task>,
    val hidden: List<Task>,
)
