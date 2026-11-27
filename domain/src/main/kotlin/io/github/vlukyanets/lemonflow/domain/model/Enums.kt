package io.github.vlukyanets.lemonflow.domain.model

/** How a catalog item lives (spec: Data model, Item fields). */
enum class ItemKind {
    /** Closes when done. */
    ONE_OFF,

    /** Never leaves the catalog. */
    RECURRING,

    /** Time given to a project; never "done" in the catalog. */
    TIME_BOX,
}

enum class Measure {
    /** Minutes; laid out in the schedule. */
    DURATION,

    /** Yes/no only; no time, not in the schedule. */
    CHECK,
}

enum class Attention {
    /** Takes the owner's time; one at a time. */
    FOCUS,

    /** Runs on its own once started; takes none of the owner's time. */
    BACKGROUND,
}

/** Catalog state of an item. DONE is used by one-off items only. */
enum class ItemState { BACKLOG, PLANNED, DONE, ARCHIVED }

/** Status of an item on one day. "Active" is derived from a running entry, never stored. */
enum class PlannedStatus { PLANNED, DONE, RETURNED }

enum class EntryKind {
    /** Work on a planned item. */
    PLANNED,

    /** Work that is not on the plan. */
    UNPLANNED,

    /** Unplanned work that paused a running planned entry. */
    INTERRUPTION,
}
