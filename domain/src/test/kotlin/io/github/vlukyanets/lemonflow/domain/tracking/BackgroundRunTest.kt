package io.github.vlukyanets.lemonflow.domain.tracking

import io.github.vlukyanets.lemonflow.domain.at
import io.github.vlukyanets.lemonflow.domain.min
import io.github.vlukyanets.lemonflow.domain.plannedId
import io.github.vlukyanets.lemonflow.domain.runId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BackgroundRunTest {

    private val laundry = BackgroundRun.start(runId("laundry"), plannedId("Laundry"), at("10:00"), min(90))
    private val build = BackgroundRun.start(runId("build"), plannedId("Build"), at("10:30"), min(40))

    @Test
    fun `run expects to end after its duration`() {
        assertEquals(at("11:30"), laundry.expectedEnd)
    }

    @Test
    fun `run should be ready after its expected end until ticked done`() {
        assertFalse(laundry.shouldBeReady(at("11:29")))
        assertTrue(laundry.shouldBeReady(at("11:30")))
        assertFalse(laundry.copy(end = at("11:40")).shouldBeReady(at("12:00")))
    }

    @Test
    fun `notification counts down to the soonest-ending running run`() {
        assertEquals(build, listOf(laundry, build).soonestEnding())
        assertEquals(laundry, listOf(laundry, build.copy(end = at("11:00"))).soonestEnding())
        assertNull(emptyList<BackgroundRun>().soonestEnding())
    }
}
