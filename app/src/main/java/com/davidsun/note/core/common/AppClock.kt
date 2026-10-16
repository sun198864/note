package com.davidsun.note.core.common

import java.time.LocalDate
import java.time.ZoneId

interface AppClock {
    fun nowMillis(): Long

    fun today(zone: ZoneId = ZoneId.systemDefault()): LocalDate
}
