package io.github.vlukyanets.lemonflow.domain.plan

import io.github.vlukyanets.lemonflow.domain.model.ItemKind
import io.github.vlukyanets.lemonflow.domain.model.ItemState
import io.github.vlukyanets.lemonflow.domain.model.PlannedItemId
import io.github.vlukyanets.lemonflow.domain.model.PlannedStatus
import java.time.LocalDate

/** Where an item already sits on a day plan. */
data class Placement(
    val plannedItem: PlannedItemId,
    val date: LocalDate,
    val status: PlannedStatus,
)

sealed interface AddOutcome {
    /** Add a new planned item on the target day. */
    data object Add : AddOutcome

    /** PLAN-4: a one-off item moves from the other day instead of being duplicated. */
    data class MoveFrom(val plannedItem: PlannedItemId, val date: LocalDate) : AddOutcome

    /** PLAN-4: an item appears at most once per day. */
    data object AlreadyOnDay : AddOutcome

    data class Refused(val reason: Reason) : AddOutcome

    enum class Reason { NOT_TODAY_OR_TOMORROW, ARCHIVED, DONE }
}

/** Planning rules (spec: PLAN-4; only today and tomorrow can be planned). */
object PlanningRules {

    fun checkAdd(
        kind: ItemKind,
        state: ItemState,
        target: LocalDate,
        today: LocalDate,
        placements: List<Placement>,
    ): AddOutcome {
        if (target != today && target != today.plusDays(1)) {
            return AddOutcome.Refused(AddOutcome.Reason.NOT_TODAY_OR_TOMORROW)
        }
        if (state == ItemState.ARCHIVED) return AddOutcome.Refused(AddOutcome.Reason.ARCHIVED)
        if (kind == ItemKind.ONE_OFF && state == ItemState.DONE) return AddOutcome.Refused(AddOutcome.Reason.DONE)
        if (placements.any { it.date == target }) return AddOutcome.AlreadyOnDay
        if (kind == ItemKind.ONE_OFF) {
            placements
                .firstOrNull { it.status == PlannedStatus.PLANNED && it.date >= today }
                ?.let { return AddOutcome.MoveFrom(it.plannedItem, it.date) }
        }
        return AddOutcome.Add
    }
}
