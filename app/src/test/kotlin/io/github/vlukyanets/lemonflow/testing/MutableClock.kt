package io.github.vlukyanets.lemonflow.testing

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** A clock tests can move; the domain only ever reads time through an injected Clock. */
class MutableClock(var now: Instant, private val zoneId: ZoneId = KYIV) : Clock() {
    override fun getZone(): ZoneId = zoneId

    override fun withZone(zone: ZoneId): Clock = MutableClock(now, zone)

    override fun instant(): Instant = now

    fun advance(duration: Duration) {
        now = now.plus(duration)
    }

    /** Sets the clock to a local date-time in this clock's zone, e.g. "2026-11-09T10:00". */
    fun setLocal(text: String) {
        now = ZonedDateTime.of(LocalDateTime.parse(text), zoneId).toInstant()
    }

    companion object {
        val KYIV: ZoneId = ZoneId.of("Europe/Kyiv")

        fun atLocal(text: String, zone: ZoneId = KYIV): MutableClock =
            MutableClock(ZonedDateTime.of(LocalDateTime.parse(text), zone).toInstant(), zone)
    }
}
