package com.dcfrederique.pizzabread.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

object TimeFormat {
    private val clock = DateTimeFormatter.ofPattern("HH:mm")
    private val days = listOf("ma", "di", "wo", "do", "vr", "za", "zo")

    /** "17:45", of "morgen 09:30" / "vr 09:30" als het niet vandaag is. */
    fun clock(epochMs: Long, now: Long = System.currentTimeMillis(), zone: ZoneId = ZoneId.systemDefault()): String {
        val t = Instant.ofEpochMilli(epochMs).atZone(zone)
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val date: LocalDate = t.toLocalDate()
        val hm = clock.format(t)
        return when (date) {
            today -> hm
            today.plusDays(1) -> "morgen $hm"
            today.minusDays(1) -> "gisteren $hm"
            else -> "${days[t.dayOfWeek.value - 1]} $hm"
        }
    }

    /** "45 min", "4 u 30", "2 u". */
    fun duration(minutes: Int): String {
        val m = abs(minutes)
        val sign = if (minutes < 0) "–" else ""
        return when {
            m < 60 -> "$sign$m min"
            m % 60 == 0 -> "$sign${m / 60} u"
            else -> "$sign${m / 60} u ${(m % 60).toString().padStart(2, '0')}"
        }
    }

    /** Aftelling "1:23:45" of "12:34". */
    fun countdown(ms: Long): String {
        val total = (maxOf(0L, ms) + 999) / 1000
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }
}
