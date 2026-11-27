package io.github.vlukyanets.lemonflow.domain.catalog

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * When a recurring item is due (spec: PLAN-5). Due items are only marked in the catalog
 * sheet, never added to a day automatically. Dates are logical dates.
 */
sealed interface Recurrence {
    fun isDue(date: LocalDate): Boolean

    data object Daily : Recurrence {
        override fun isDue(date: LocalDate): Boolean = true
    }

    data class Weekdays(val days: Set<DayOfWeek>) : Recurrence {
        init {
            require(days.isNotEmpty()) { "Pick at least one weekday" }
        }

        override fun isDue(date: LocalDate): Boolean = date.dayOfWeek in days
    }

    /** Due on [anchor] and every [interval] days after it; never before it. */
    data class EveryNDays(val interval: Int, val anchor: LocalDate) : Recurrence {
        init {
            require(interval >= 1) { "Interval must be at least 1 day" }
        }

        override fun isDue(date: LocalDate): Boolean {
            val days = ChronoUnit.DAYS.between(anchor, date)
            return days >= 0 && days % interval == 0L
        }
    }
}
