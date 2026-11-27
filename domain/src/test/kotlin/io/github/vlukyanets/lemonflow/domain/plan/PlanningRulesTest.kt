package io.github.vlukyanets.lemonflow.domain.plan

import io.github.vlukyanets.lemonflow.domain.TEST_DATE
import io.github.vlukyanets.lemonflow.domain.model.ItemKind
import io.github.vlukyanets.lemonflow.domain.model.ItemState
import io.github.vlukyanets.lemonflow.domain.model.PlannedStatus
import io.github.vlukyanets.lemonflow.domain.plannedId
import kotlin.test.Test
import kotlin.test.assertEquals

class PlanningRulesTest {

    private val today = TEST_DATE
    private val tomorrow = TEST_DATE.plusDays(1)

    private fun check(
        kind: ItemKind = ItemKind.ONE_OFF,
        state: ItemState = ItemState.BACKLOG,
        target: java.time.LocalDate = today,
        placements: List<Placement> = emptyList(),
    ) = PlanningRules.checkAdd(kind, state, target, today, placements)

    @Test
    fun `item can be added to today or tomorrow only`() {
        assertEquals(AddOutcome.Add, check(target = today))
        assertEquals(AddOutcome.Add, check(target = tomorrow))
        assertEquals(AddOutcome.Refused(AddOutcome.Reason.NOT_TODAY_OR_TOMORROW), check(target = today.plusDays(2)))
        assertEquals(AddOutcome.Refused(AddOutcome.Reason.NOT_TODAY_OR_TOMORROW), check(target = today.minusDays(1)))
    }

    @Test
    fun `item appears at most once per day`() {
        val onToday = Placement(plannedId("x"), today, PlannedStatus.PLANNED)
        assertEquals(AddOutcome.AlreadyOnDay, check(kind = ItemKind.RECURRING, placements = listOf(onToday)))
    }

    @Test
    fun `one-off item planned on the other day moves instead of duplicating`() {
        val onTomorrow = Placement(plannedId("x"), tomorrow, PlannedStatus.PLANNED)
        assertEquals(AddOutcome.MoveFrom(plannedId("x"), tomorrow), check(target = today, placements = listOf(onTomorrow)))
    }

    @Test
    fun `recurring and time-box items can sit on both days`() {
        val onToday = Placement(plannedId("x"), today, PlannedStatus.PLANNED)
        assertEquals(AddOutcome.Add, check(kind = ItemKind.RECURRING, target = tomorrow, placements = listOf(onToday)))
        assertEquals(AddOutcome.Add, check(kind = ItemKind.TIME_BOX, target = tomorrow, placements = listOf(onToday)))
    }

    @Test
    fun `a one-off item returned on a past day can be planned again`() {
        val returned = Placement(plannedId("old"), today.minusDays(3), PlannedStatus.RETURNED)
        assertEquals(AddOutcome.Add, check(placements = listOf(returned)))
    }

    @Test
    fun `archived and done items are refused`() {
        assertEquals(AddOutcome.Refused(AddOutcome.Reason.ARCHIVED), check(state = ItemState.ARCHIVED))
        assertEquals(AddOutcome.Refused(AddOutcome.Reason.DONE), check(state = ItemState.DONE))
    }
}
