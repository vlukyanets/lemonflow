# Data Model: Habits and Today

Two layers:
- **Domain models** (`domain/model`) - pure Kotlin types used by rules and UI.
- **Room entities** (`data/db`) - their persisted form in the SQLCipher database
  `lemonflow.db`, schema version 1, with Room schema export enabled (`app/schemas/`).

Conventions (constitution I):
- every primary key is a client-generated UUID, stored as TEXT;
- every instant is stored as epoch milliseconds in UTC (`Long`);
- a user date is stored as ISO `yyyy-MM-dd` TEXT;
- every record changed by a user action also stores the zone id in effect.

## Enums

| Enum | Values | Notes |
|------|--------|-------|
| `HabitType` | `CHECK`, `COUNT`, `TIMER`, `AVOID` | fixed at creation (FR-002a) |
| `AmountUnit` | `MINUTES`, `TIMES`, `PAGES`, `STEPS`, `CUSTOM` | `CUSTOM` carries a user label |
| `Mark` | `OPEN`, `TARGET`, `BASELINE` | stored; for COUNT/TIMER/AVOID it is derived (see rules) |

## Habit (`habit` table)

| Field | Type | Rules |
|-------|------|-------|
| `id` | UUID (TEXT, PK) | generated on create |
| `name` | TEXT | trimmed, 1..120 chars |
| `type` | `HabitType` | immutable after create |
| `targetAmount` | INT | CHECK/COUNT/TIMER: >= 1; AVOID: >= 0 (good-day limit) |
| `targetUnit` | `AmountUnit` | TIMER: always `MINUTES` |
| `targetUnitLabel` | TEXT? | required, trimmed, 1..30 chars, when unit is `CUSTOM` |
| `baselineAmount` | INT | CHECK: >= 1; COUNT/TIMER: 1..targetAmount; AVOID: >= targetAmount |
| `baselineUnit` | `AmountUnit` | CHECK: any; COUNT/TIMER/AVOID: equals `targetUnit` |
| `baselineUnitLabel` | TEXT? | as for target |
| `step` | INT? | COUNT: >= 1, default 1; TIMER: minutes >= 1, default 1; CHECK/AVOID: null |
| `isCore` | BOOLEAN | default false |
| `createdAt` | Instant | set on create |
| `archivedAt` | Instant? | null = active; set on archive, cleared on restore |

Indexes: (`archivedAt`), (`isCore`, `createdAt`) for Today ordering (FR-006).

Lifecycle: `active -> archived` (archive) `-> active` (restore); in either state ->
permanently deleted, which cascades to its daily records (FR-004b).

## DailyHabitRecord (`daily_record` table) - the spec's DailyHabitInstance

| Field | Type | Rules |
|-------|------|-------|
| `id` | UUID (TEXT, PK) | |
| `habitId` | UUID (FK -> habit.id, ON DELETE CASCADE) | |
| `date` | user date (TEXT) | per R3 |
| `mark` | `Mark` | CHECK: set by the user; others: derived from `amount` on every change |
| `amount` | INT | COUNT: units; TIMER: accumulated **seconds**; AVOID: count; CHECK: 0 |
| `timerStartedAt` | Instant? | TIMER only; non-null while running |
| `updatedAt` | Instant | |
| `zoneId` | TEXT | zone in effect at `updatedAt` |

Unique index: (`habitId`, `date`). A missing record means OPEN with amount 0, except for AVOID
(see the avoid default rule).

## Day (`day` table)

| Field | Type | Rules |
|-------|------|-------|
| `id` | UUID (TEXT, PK) | |
| `date` | user date (TEXT, unique) | |
| `hardDay` | BOOLEAN | missing row = false (FR-010) |
| `updatedAt` | Instant | |
| `zoneId` | TEXT | |

## Settings (`settings` table, single row)

| Field | Type | Rules |
|-------|------|-------|
| `id` | UUID (TEXT, PK) | fixed after first creation |
| `dayEndHour` | INT | 0..6, default 3 (FR-018) |
| `updatedAt` | Instant | |

## Derived rules (domain, pure Kotlin, test-first)

- **User date** of an instant: research R3. **Day end** of date D: R3.
- **Goal for today**: Target, or Baseline when `Day.hardDay` (FR-011). For AVOID the goal is a
  limit.
- **Mark from amount**:
  - COUNT/TIMER: `amount >= target` -> TARGET; `>= baseline` -> BASELINE; else OPEN (TIMER
    compares seconds with minutes x 60).
  - AVOID: `count <= target` -> TARGET; `<= baseline` -> BASELINE; else OPEN (shown neutrally).
  - On a hard day a BASELINE mark is *presented* as done; the stored mark stays BASELINE
    (FR-009, FR-011).
- **Avoid default**: an AVOID habit with no record is TARGET for today, and for an earlier date
  only if that date has at least one record of another habit (FR-008).
- **Timer elapsed**: `amount + max(0, min(now, dayEnd(date)) - timerStartedAt)`. Settling
  stops timers whose day ended (R5). Several timers may run at once (FR-007d).
- **Active on date D**: R10.
- **Week strip share** for date D: completed (mark != OPEN, including the avoid default) out of
  the habits active on D; a date where nothing counts as completed and no records exist is
  "nothing recorded" (FR-006a/b).
- **Validation** (FR-002/FR-005): returns a list of field errors (`NAME_EMPTY`,
  `NAME_TOO_LONG`, `TARGET_EMPTY`, `BASELINE_EMPTY`, `BASELINE_ABOVE_TARGET`,
  `LIMIT_BELOW_TARGET`, `CUSTOM_UNIT_EMPTY`, `CUSTOM_UNIT_TOO_LONG`), each mapped to a
  gentle string in en/ru/uk.

## State transitions on Today (per type)

| Type | Actions | Effect |
|------|---------|--------|
| CHECK | mark Target / mark Baseline / undo | set mark; undo -> OPEN |
| COUNT | `changeAmount(id, +1 / -1)` | amount +/- `step` units (>= 0); mark re-derived |
| TIMER | start / pause / `changeAmount(id, +1 / -1)` | start sets `timerStartedAt`; pause folds elapsed into `amount`; +/- changes `amount` by `step` x 60 seconds (>= 0); mark re-derived |
| AVOID | `changeAmount(id, +1 / -1)` | count +/- 1 (>= 0); mark re-derived |

The UI only passes the direction (`steps = +1` or `-1`); the repository converts it to the
stored unit, so callers never deal with seconds or step sizes.

Only today's records can change (FR-006b). Changing `dayEndHour` never rewrites stored dates
(FR-019).
