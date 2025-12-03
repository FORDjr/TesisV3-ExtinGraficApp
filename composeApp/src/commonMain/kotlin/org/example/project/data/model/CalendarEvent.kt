package org.example.project.data.model

import kotlinx.datetime.*
import kotlinx.serialization.Serializable

// YearMonth simple propio (kotlinx-datetime aún no expone YearMonth estable)
data class YearMonth(val year: Int, val monthNumber: Int) {
    fun previous(): YearMonth {
        val m = monthNumber - 1
        return if (m < 1) YearMonth(year - 1, 12) else YearMonth(year, m)
    }
    fun next(): YearMonth {
        val m = monthNumber + 1
        return if (m > 12) YearMonth(year + 1, 1) else YearMonth(year, m)
    }
}

private fun daysInMonth(year: Int, month: Int): Int = when (month) {
    1,3,5,7,8,10,12 -> 31
    4,6,9,11 -> 30
    2 -> if (isLeap(year)) 29 else 28
    else -> 30
}
private fun isLeap(year: Int): Boolean = (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)

@Serializable
data class CalendarEvent(
    val id: Int,
    val title: String,
    val date: String, // ISO (yyyy-MM-dd)
    val rawDateTime: String? = null, // ISO original si viene con hora
    val daysToExpire: Long? = null,
    val color: String = "gris",
    val type: String = "EXTINTOR"
) {
    fun localDate(): LocalDate? = try {
        when {
            date.length >= 10 -> LocalDate.parse(date.substring(0,10))
            rawDateTime != null && rawDateTime.length >= 10 -> LocalDate.parse(rawDateTime.substring(0,10))
            else -> null
        }
    } catch (_: Exception) { null }
}

data class CalendarDay(
    val date: LocalDate,
    val inMonth: Boolean,
    val isToday: Boolean,
    val events: List<CalendarEvent> = emptyList()
)

object CalendarUtils {
    fun monthDays(yearMonth: YearMonth, events: List<CalendarEvent>): List<CalendarDay> {
        val firstDay = LocalDate(yearMonth.year, yearMonth.monthNumber, 1)
        val firstDayOfWeekIndex = (firstDay.dayOfWeek.isoDayNumber % 7) // 0 Dom, 6 Sáb
        val totalDays = daysInMonth(yearMonth.year, yearMonth.monthNumber)
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val eventMap = events.groupBy { it.localDate() }
        val days = mutableListOf<CalendarDay>()
        // Días previos relleno
        for (i in 0 until firstDayOfWeekIndex) {
            val d = firstDay.minus(DatePeriod(days = firstDayOfWeekIndex - i))
            days += CalendarDay(d, inMonth = false, isToday = d == today, events = eventMap[d] ?: emptyList())
        }
        // Días del mes
        for (i in 0 until totalDays) {
            val d = firstDay.plus(DatePeriod(days = i))
            days += CalendarDay(d, inMonth = true, isToday = d == today, events = eventMap[d] ?: emptyList())
        }
        // Completar hasta múltiplo de 7 (máx 42)
        while (days.size % 7 != 0 || days.size < 35) { // al menos 5 semanas
            val last = days.last().date
            val d = last.plus(DatePeriod(days = 1))
            days += CalendarDay(d, inMonth = false, isToday = d == today, events = eventMap[d] ?: emptyList())
            if (days.size >= 42) break
        }
        return days
    }
}
