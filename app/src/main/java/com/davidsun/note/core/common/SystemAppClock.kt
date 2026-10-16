package com.davidsun.note.core.common

import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class SystemAppClock @Inject constructor() : AppClock {
    override fun nowMillis(): Long = System.currentTimeMillis()

    override fun today(zone: ZoneId): LocalDate = LocalDate.now(zone)
}
