---

description: "Task list for Habits and Today"
---

# Tasks: Habits and Today

**Input**: Design documents from `specs/20261109-020644-habits-today/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: REQUIRED. Constitution principle IV is non-negotiable: in every phase, the test
tasks come first and must fail before the implementation tasks start.

**Organization**: tasks are grouped by user story (US1-US4 from spec.md) so that each story
can be implemented and tested on its own.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: can run in parallel (different files, no dependency on an unfinished task)
- **[Story]**: the user story the task belongs to (US1, US2, US3, US4)

## Path Conventions

Single Android module `:app`. Paths below are written in full; the package root is
`io.github.vlukyanets.lemonflow`, so:
- main code: `app/src/main/kotlin/io/github/vlukyanets/lemonflow/`
- tests: `app/src/test/kotlin/io/github/vlukyanets/lemonflow/`
- resources: `app/src/main/res/`

Layering (constitution VI): `ui -> domain <- data`. Nothing in `domain/` imports `android.*`
(constitution IV).

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: dependencies, plugins and assets from plan.md.

- [X] T001 Add the plan's dependencies to `gradle/libs.versions.toml`:
  - versions: `room = "2.8.5"`, `sqlcipher = "4.20.0"`, `navigationCompose = "2.10.2"`,
    `kotlinxSerialization = "1.12.0"`, `hiltLifecycleViewmodelCompose = "1.4.0"`,
    `lifecycle = "2.11.0"`, `coroutinesTest = "1.11.0"`;
  - libraries: `room-runtime`, `room-ktx`, `room-compiler`, `sqlcipher-android`
    (`net.zetetic:sqlcipher-android`), `navigation-compose`, `kotlinx-serialization-core`,
    `hilt-lifecycle-viewmodel-compose`, `lifecycle-viewmodel-compose`,
    `lifecycle-runtime-compose`, `kotlinx-coroutines-test`;
  - plugin: `kotlin-serialization` (`org.jetbrains.kotlin.plugin.serialization`, version ref
    `kotlin`).
- [X] T002 Wire the dependencies in:
  - `build.gradle.kts`: declare `kotlin-serialization` with `apply false`;
  - `app/build.gradle.kts`: apply it and add the libraries (`ksp(room-compiler)`,
    `testImplementation(kotlinx-coroutines-test)`);
  - configure `ksp { arg("room.schemaLocation", "$projectDir/schemas"); arg("room.generateKotlin", "true") }`;
  - in `config/detekt/detekt.yml` enable `style > ForbiddenMethodCall` with a comment
    pointing to FR-014 / constitution II, forbidding `android.util.Log.v`, `.d`, `.i`, `.w`,
    `.e`, `.wtf`, `kotlin.io.println`, `kotlin.io.print` and `java.lang.Throwable.printStackTrace`
    (a reason text per method), so every later task is checked from the start;
  - run `./gradlew assembleDebug detekt` - must pass.
- [X] T003 [P] Bundle Roboto from googlefonts/roboto release v2.138 (Apache-2.0):
  - Download the release zip into the scratchpad and check that `LICENSE` is Apache-2.0.
  - Copy the static Regular, Medium and Bold TTFs to
    `app/src/main/res/font/roboto_regular.ttf`, `roboto_medium.ttf`, `roboto_bold.ttf`.
  - These are binary files: copy them with `cp`, as the binary-asset exception in
    `AGENTS.md` (Boundaries) allows - the owner approved it on 2026-11-09.
  - Add a Roboto entry (source, version, Apache-2.0) to `THIRD_PARTY_NOTICES.md`.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: domain types, user-day time, the encrypted database, theme and navigation shell.
Every story needs these. No story work starts before this phase passes `./gradlew check`.

### Tests first (must fail before T008-T019)

- [X] T004 [P] Write `app/src/test/kotlin/io/github/vlukyanets/lemonflow/domain/time/UserDayCalculatorTest.kt`
  (JUnit 4, fixed `ZoneId`s, no Android), covering:
  - `dayEndHour = 3`, Europe/Kyiv, 2026-11-10T01:30 local -> user date 2026-11-09;
  - 03:00:00 local -> 2026-11-10;
  - `dayEndHour = 0` -> plain local midnight;
  - Europe/Kyiv spring gap: on 2027-03-28 the clock jumps 03:00 -> 04:00, so the end of
    2027-03-27 is the first instant after the gap (04:00 local, 01:00Z);
  - autumn overlap: on 2026-10-25 the hour 03:00-04:00 repeats, and the end of 2026-10-24
    is the first 03:00 (00:00Z);
  - `dayStart(date)` equals `dayEnd(date - 1)`.
- [X] T005 [P] Write `app/src/test/kotlin/io/github/vlukyanets/lemonflow/data/crypto/DatabaseKeyManagerTest.kt`
  with a `FakeKeyStoreGateway` and a temp directory:
  - the first run generates a 32-byte passphrase and writes `db_key.bin` as IV +
    ciphertext, never the raw passphrase;
  - the second run unwraps the same bytes;
  - key file present but Keystore entry missing -> `Unavailable`;
  - tampered file (GCM tag failure) -> `Unavailable`;
  - a database file present without a key file -> `Unavailable`;
  - `startFresh()` deletes the database files, `db_key.bin` and the Keystore entry, and the
    next `open()` creates a new key.
  - In a separate Robolectric test, `AndroidKeyStoreGatewayTest.kt`, check the
    `KeyGenParameterSpec` returned by `AndroidKeyStoreGateway.wrapKeySpec()` (FR-013a):
    `isUserAuthenticationRequired == false`, purposes ENCRYPT | DECRYPT, block mode GCM,
    padding NONE, key size 256, alias `lemonflow_db_wrap`.
- [X] T006 [P] Write `app/src/test/kotlin/io/github/vlukyanets/lemonflow/data/db/LemonFlowDatabaseTest.kt`
  (Robolectric, `Room.inMemoryDatabaseBuilder`, no SQLCipher - research R9):
  - inserting a daily record whose `habitId` does not exist fails (FK);
  - deleting a habit cascades to its daily records;
  - (`habitId`, `date`) is unique;
  - `day.date` is unique;
  - persistence across reopen (SC-004): with a file-backed database (`Room.databaseBuilder`
    on a temp file, still without SQLCipher), write a habit, a daily record, a `day` row with
    `hardDay = true` and `settings.dayEndHour = 5`, close the database, open it again and
    read every value back unchanged.

### Implementation

- [X] T007 [P] Create the domain models in `app/src/main/kotlin/io/github/vlukyanets/lemonflow/domain/model/`
  (one file per type, pure Kotlin), as in data-model.md:
  - `HabitId` (value class over a UUID string);
  - `HabitType { CHECK, COUNT, TIMER, AVOID }` and `AmountUnit { MINUTES, TIMES, PAGES, STEPS, CUSTOM }`;
  - `Amount(value: Int, unit: AmountUnit, customLabel: String?)` and `Mark { OPEN, TARGET, BASELINE }`;
  - `Habit` (id, name, type, target, baseline, `step: Int?`, isCore, createdAt: Instant,
    archivedAt: Instant?) and `HabitDraft`;
  - `DailyRecord` (id, habitId, date: LocalDate, mark, amount, timerStartedAt: Instant?,
    updatedAt, zoneId), `DayState` and `ValidationError`;
  - `SaveResult<out T>` with `Saved<T>(value)` and `Invalid(errors: List<ValidationError>)`,
    as in contracts/domain-ports.md.
- [X] T008 Implement `app/src/main/kotlin/io/github/vlukyanets/lemonflow/domain/time/UserDayCalculator.kt`
  (`userDate(instant, zone, dayEndHour)`, `dayStart(date, zone, dayEndHour)`,
  `dayEnd(date, zone, dayEndHour)`) per research R3 until T004 passes.
- [X] T009 [P] Create the ports in `app/src/main/kotlin/io/github/vlukyanets/lemonflow/domain/port/`:
  - `HabitRepository.kt`, `DayRepository.kt`, `SettingsRepository.kt`;
  - `StorageGate.kt` with `StorageState { Ready, Unavailable }`;
  - signatures exactly as in contracts/domain-ports.md.
- [X] T010 Create the Room entities in `app/src/main/kotlin/io/github/vlukyanets/lemonflow/data/db/entity/`
  (`HabitEntity`, `DailyRecordEntity`, `DayEntity`, `SettingsEntity`). Follow data-model.md
  verbatim:
  - every PK is a UUID TEXT, every instant epoch millis, every user date ISO `yyyy-MM-dd`
    TEXT;
  - `habit`: `name` "trimmed, 1..120 chars"; indexes (`archivedAt`) and (`isCore`,
    `createdAt`);
  - `daily_record`: FK `habitId` -> `habit.id` ON DELETE CASCADE, unique (`habitId`,
    `date`), `amount` INT ("TIMER: accumulated seconds"), `timerStartedAt` nullable,
    `zoneId` TEXT;
  - `day`: `date` unique, `hardDay` BOOLEAN;
  - `settings`: single row, `dayEndHour` INT "0..6, default 3".
  - Also add `Converters.kt` for the enums.
- [X] T011 Create the DAO skeletons in `app/src/main/kotlin/io/github/vlukyanets/lemonflow/data/db/dao/`
  (`HabitDao`, `DailyRecordDao`, `DayDao`, `SettingsDao`), with the insert, delete and
  by-id queries T006 needs. Each story adds its own queries.
- [X] T012 Create `app/src/main/kotlin/io/github/vlukyanets/lemonflow/data/db/LemonFlowDatabase.kt`
  (version 1, `exportSchema = true`) and commit the generated `app/schemas/` JSON; make T006
  pass.
- [X] T013 Create `app/src/main/kotlin/io/github/vlukyanets/lemonflow/data/crypto/KeyStoreGateway.kt`
  (interface) and `AndroidKeyStoreGateway.kt`:
  - AES-256-GCM key in `AndroidKeyStore`, alias `lemonflow_db_wrap`;
  - `PURPOSE_ENCRYPT | PURPOSE_DECRYPT`, `setUserAuthenticationRequired(false)`, no
    StrongBox requirement (research R2, FR-013a);
  - build the spec in a separate function `wrapKeySpec(): KeyGenParameterSpec` so the
    `AndroidKeyStoreGatewayTest` from T005 can check it.
- [X] T014 Implement `app/src/main/kotlin/io/github/vlukyanets/lemonflow/data/crypto/DatabaseKeyManager.kt`
  until T005 passes:
  - `noBackupFilesDir/db_key.bin` holds IV + ciphertext;
  - the passphrase byte array is zeroed after use;
  - it never logs.
- [X] T015 Implement `app/src/main/kotlin/io/github/vlukyanets/lemonflow/data/repo/DatabaseHolder.kt`
  (the `StorageGate` implementation):
  - load `System.loadLibrary("sqlcipher")` once;
  - open Room with `SupportOpenHelperFactory(passphrase)`, then run a probe query;
  - map "file is not a database" and key errors to `Unavailable`;
  - expose the DAOs only when `Ready`;
  - `startFresh()` deletes `lemonflow.db*`, then delegates to the key manager.
- [X] T016 Create the Hilt modules in `app/src/main/kotlin/io/github/vlukyanets/lemonflow/di/`:
  - `TimeModule.kt` provides `Clock.systemDefaultZone()`;
  - `StorageModule.kt` provides the singleton `DatabaseHolder` as `StorageGate` and the DAOs
    through it.
- [X] T017 [P] Replace the theme in `app/src/main/kotlin/io/github/vlukyanets/lemonflow/ui/theme/`
  (`Color.kt`, `Type.kt`, `Theme.kt`); remove dynamic color.
  - Light / dark palette from the design:

    | Token | Light | Dark |
    |-------|-------|------|
    | bg | #F4F4F1 | #0D0E11 |
    | surface | #FFFFFF | #17181C |
    | surface2 | #ECECE7 | #222429 |
    | line | #E2E2DC | #2A2C33 |
    | ink | #15161A | #EDEEF1 |
    | muted | #5C606B | #9A9EAA |
    | blue | #2448E8 | #7D93FF |
    | blueSoft | #E5EAFE | #1D2550 |
    | runBg | #F3F5FF | #151A33 |
    | warn | #A8470A | #FF9F5A |
    | warnSoft | #FCEBDD | #3B2516 |
    | danger | #B3261E | #FF8A80 |

  - Typography: Roboto 400 body, 500 labels, 700 headings, plus a numeric style with
    `fontFeatureSettings = "tnum"`.
  - Expose the extra tokens through a `LocalLemonFlowColors` composition local.
- [X] T018 [P] Add the stroke icons as vector drawables in `app/src/main/res/drawable/`,
  traced from the design canvas SVGs: `ic_today`, `ic_habits`, `ic_insights`,
  `ic_settings`, `ic_check`, `ic_plus`, `ic_minus`, `ic_play`, `ic_pause`, `ic_core`,
  `ic_archive`, `ic_restore`, `ic_close`, `ic_chevron`, `ic_clock`, `ic_lock`, `ic_sprout`.
- [X] T019 Build the navigation shell and remove the skeleton screen:
  - `app/src/main/kotlin/io/github/vlukyanets/lemonflow/ui/navigation/Routes.kt`:
    `@Serializable` objects `Today`, `Habits`, `Insights`, `Settings`, `Archive` and
    `HabitEditor(val habitId: String? = null)`.
  - `LemonFlowNavHost.kt`: bottom bar with 4 tabs that save and restore state; 48dp
    targets; tab labels in en/ru/uk.
  - `ui/insights/InsightsScreen.kt` with the "Coming soon" copy (FR-020); placeholder
    composables for the other tabs.
  - `MainActivity.kt` calls `StorageGate.open()` and shows the NavHost when `Ready`; when
    `Unavailable` it shows a temporary blank screen, which US4 replaces.
  - Delete `ui/main/MainScreen.kt` and `app/src/test/.../ui/main/MainScreenTest.kt`, and move
    the remaining strings into `app/src/main/res/values{,-ru,-uk}/strings.xml`.
  - Add the shared test helper `app/src/test/kotlin/io/github/vlukyanets/lemonflow/ui/A11yAssertions.kt`:
    `ComposeTestRule.assertAllClickablesAccessible()` walks every node with a click action
    and asserts (FR-017) a touch target of at least 48x48dp and a non-empty text or
    `contentDescription`. Use it first in
    `app/src/test/kotlin/io/github/vlukyanets/lemonflow/ui/navigation/LemonFlowNavHostTest.kt`
    on the bottom bar. The same test also checks that tapping each of the four tabs opens
    its screen and marks that tab as selected, and that the Insights tab shows "Coming
    soon" with its one-line explanation and no data or buttons (FR-020).
  - In the same change, point the "Run one test" command in `AGENTS.md` at
    `./gradlew :app:testDebugUnitTest --tests "io.github.vlukyanets.lemonflow.domain.time.UserDayCalculatorTest"`
    and run it once, so the documented command never refers to a deleted test.
  - Finish with `./gradlew check assembleDebug` green.

**Checkpoint**: the app starts with an encrypted empty database and four tabs.

---

## Phase 3: User Story 1 - Define habits with a norm and a rescue minimum (Priority: P1) MVP

**Goal**: create, edit, archive, restore and permanently delete habits of all four types.

**Independent Test**: from the Habits tab, create one habit of each type, edit one, archive
and restore one, delete one permanently, restart the app - the list matches (spec US1).

### Tests for User Story 1 (write first, must fail)

- [X] T020 [P] [US1] Write `app/src/test/kotlin/io/github/vlukyanets/lemonflow/domain/rules/HabitValidatorTest.kt`
  covering FR-002/FR-005 verbatim:
  - name "trimmed, 1..120 chars" (`NAME_TOO_LONG` above 120);
  - CHECK: both amounts >= 1, the units may differ;
  - COUNT: the same unit, `1 <= baseline <= target`, step >= 1;
  - TIMER: unit always MINUTES, `1 <= baseline <= target`, step >= 1;
  - AVOID: the same unit, target >= 0, baseline >= target, step null;
  - a CUSTOM unit needs a trimmed label of 1..30 chars (`CUSTOM_UNIT_TOO_LONG` above 30);
  - each failure yields the right code (`NAME_EMPTY`, `TARGET_EMPTY`, `BASELINE_EMPTY`,
    `BASELINE_ABOVE_TARGET`, `LIMIT_BELOW_TARGET`, `CUSTOM_UNIT_EMPTY`).
- [X] T021 [P] [US1] Write `app/src/test/kotlin/io/github/vlukyanets/lemonflow/data/repo/RoomHabitRepositoryTest.kt`
  (Robolectric, in-memory Room, fixed `Clock`):
  - `create` returns `SaveResult.Saved` with a new UUID and `createdAt` from the clock; an
    invalid draft returns `SaveResult.Invalid` with the validator's errors and writes
    nothing;
  - `update` cannot change the type;
  - `observeActive` lists core habits first, then by `createdAt`;
  - `archive` sets `archivedAt` and keeps the records;
  - `restore` clears `archivedAt`;
  - `observeArchived` sorts by `archivedAt` descending;
  - `deletePermanently` removes the habit and its records.
- [X] T022 [P] [US1] Write `app/src/test/kotlin/io/github/vlukyanets/lemonflow/ui/editor/HabitEditorScreenTest.kt`
  (Robolectric Compose, fake `HabitRepository`):
  - the type picker shows the per-type fields (Check: two amount+unit pairs, no step;
    Count: one unit, Target/Minimum, Step; Timer: minutes only, Step; Avoid: unit, good-day
    and hard-day limits);
  - Save stays disabled until name, Target and Minimum are set;
  - Count with Minimum > Target shows the gentle hint;
  - edit mode shows the type read-only plus Archive and "Delete permanently" (with a
    confirmation dialog);
  - `assertAllClickablesAccessible()` passes in add and edit mode and with the delete
    dialog open.
- [X] T023 [P] [US1] Write `app/src/test/kotlin/io/github/vlukyanets/lemonflow/ui/habits/HabitsScreenTest.kt`
  and `ArchiveScreenTest.kt`:
  - the list shows each habit's type and goal line ("Avoid · Limit 0 · hard day 1"), the
    core icon and the archive count;
  - Archive offers Restore, and "Delete permanently" behind a confirmation dialog (FR-004b);
  - `assertAllClickablesAccessible()` passes on both screens.

### Implementation for User Story 1

- [X] T024 [US1] Implement `app/src/main/kotlin/io/github/vlukyanets/lemonflow/domain/rules/HabitValidator.kt`
  until T020 passes.
- [X] T025 [US1] Add habit queries to `data/db/dao/HabitDao.kt` and implement
  `app/src/main/kotlin/io/github/vlukyanets/lemonflow/data/repo/RoomHabitRepository.kt`, with
  a Hilt binding in `di/RepositoryModule.kt`, until T021 passes.
- [X] T026 [US1] Add the editor, list, archive and validation strings to
  `app/src/main/res/values/strings.xml`, `values-ru/strings.xml` and
  `values-uk/strings.xml`:
  - labels "Target · a good day" / "Minimum · a hard day" and the Avoid limits;
  - the hints "Add an amount for a good day" and "The minimum should be no more than the
    target";
  - no banned words (FR-016).
- [X] T027 [US1] Implement `app/src/main/kotlin/io/github/vlukyanets/lemonflow/ui/editor/EditorViewModel.kt`
  and `HabitEditorScreen.kt`, matching the design's Editor artboards, until T022 passes.
- [X] T028 [US1] Implement `app/src/main/kotlin/io/github/vlukyanets/lemonflow/ui/habits/HabitsViewModel.kt`,
  `HabitsScreen.kt`, `ArchiveViewModel.kt` and `ArchiveScreen.kt` until T023 passes.
- [X] T029 [US1] Register the `Habits` tab and the `HabitEditor` and `Archive` routes in
  `ui/navigation/LemonFlowNavHost.kt`; back from the editor returns to its caller.

**Checkpoint**: US1 is fully usable from the Habits tab.

---

## Phase 4: User Story 2 - See today's plan and mark progress (Priority: P2)

**Goal**: the Today screen with per-type controls, timers, the 7-day strip, past days and the
"Day ends at" setting.

**Independent Test**: quickstart.md scenarios 4-6, 8 and 9; the marks survive a restart, and
the next day starts fresh.

### Tests for User Story 2 (write first, must fail)

- [X] T030 [P] [US2] Write `app/src/test/kotlin/io/github/vlukyanets/lemonflow/domain/rules/MarkRulesTest.kt`:
  - COUNT/TIMER: `amount >= target` -> TARGET, `>= baseline` -> BASELINE, otherwise OPEN;
  - TIMER compares seconds with minutes x 60;
  - AVOID: `count <= target` -> TARGET, `<= baseline` -> BASELINE, otherwise OPEN;
  - a BASELINE mark counts as complete (FR-009).
- [X] T031 [P] [US2] Write `app/src/test/kotlin/io/github/vlukyanets/lemonflow/domain/rules/TimerRulesTest.kt`:
  - elapsed = `amount + (now - timerStartedAt)`;
  - a clock jump backwards adds 0, and the result is never negative;
  - elapsed is capped at the day end;
  - `settle` stops only the timers whose day ended and folds in the time up to the day end;
  - two running timers stay independent (FR-007d).
- [X] T032 [P] [US2] Write `app/src/test/kotlin/io/github/vlukyanets/lemonflow/domain/rules/WeekProgressTest.kt`:
  - the Avoid default counts as TARGET today;
  - on a past date it counts only if another habit has a record that day;
  - a date with no records at all -> "nothing recorded";
  - "active on date" follows research R10;
  - the share is completed / active over the last 7 user dates ending today.
- [X] T033 [P] [US2] Write `app/src/test/kotlin/io/github/vlukyanets/lemonflow/data/repo/RoomDayRepositoryTest.kt`
  (Robolectric, in-memory, mutable `Clock`):
  - `markCheck` and undo;
  - `changeAmount(id, steps = +1/-1)` applies the right size per type: COUNT adds `step`
    units (Water, step 250 -> +250), TIMER adds `step` x 60 seconds (step 1 -> +60 s),
    AVOID adds 1; the amount floors at 0 and the mark is re-derived;
  - `startTimer` / `pauseTimer`;
  - only today's records change;
  - `updatedAt` and `zoneId` come from the clock;
  - one record per habit and date;
  - `settleTimers` at the day end;
  - with day end 03:00, a mark at 01:30 lands on the previous date;
  - day-end boundary: a mark at 02:59:59 stays on Monday, a mark at 03:00:00 lands on
    Tuesday;
  - `pastDay` is read-only data.
- [X] T034 [P] [US2] Write `app/src/test/kotlin/io/github/vlukyanets/lemonflow/data/repo/RoomSettingsRepositoryTest.kt`:
  - the default `dayEndHour` is 3;
  - 0..6 are accepted, other values are rejected;
  - changing it never rewrites stored dates (FR-019).
- [X] T035 [P] [US2] Write `app/src/test/kotlin/io/github/vlukyanets/lemonflow/ui/today/TodayScreenTest.kt`
  (Robolectric Compose, fake ports):
  - the header shows "Day closes at 03:00" and "N/M done";
  - day rollover while the screen stays open: with a mutable clock at Monday 02:59:59 the
    screen shows Monday; after moving the clock to 03:00:00 and advancing the test
    dispatcher past the scheduled day-end tick, it shows Tuesday with every habit open
    (spec edge case "app is open across the day-end time");
  - changing "Day ends at" moves "now" to another date: with the clock at Tuesday 04:00 and
    day end 03:00 the screen shows Tuesday; after setting day end to 05:00 it shows Monday
    with Monday's marks, and any marks made on Tuesday stay on Tuesday (spec edge case,
    FR-019);
  - each row type matches contracts/ui-navigation.md:
    - Check: check + `min` toggle;
    - Count: ring + `-` `+`;
    - Timer: play/pause, live time, done-and-paused shown as a filled check (a tap
      continues; the corner play badge was dropped after device review, T068);
    - Avoid: `-` `+1` and "1 time today · limit 0, hard day 1";
  - the `min` tag shows on BASELINE;
  - the empty state shows "Add first habit";
  - the week strip opens a read-only past-day sheet, where an empty day reads
    "Nothing recorded";
  - tapping a habit opens the editor (FR-021);
  - `assertAllClickablesAccessible()` passes with every row type on screen and with the
    past-day sheet open;
  - one tap per action (SC-003): a check mark, `min`, `+`/`-`, timer start/pause and `+1`
    each change the row after a single click and open no dialog or other screen.
- [X] T036 [P] [US2] Write `app/src/test/kotlin/io/github/vlukyanets/lemonflow/ui/settings/SettingsScreenTest.kt`:
  - the picker lists 00:00-06:00;
  - picking a value updates the row and closes the sheet;
  - `assertAllClickablesAccessible()` passes with the sheet closed and open.

### Implementation for User Story 2

- [X] T037 [P] [US2] Implement `app/src/main/kotlin/io/github/vlukyanets/lemonflow/domain/rules/MarkRules.kt`
  until T030 passes.
- [X] T038 [P] [US2] Implement `app/src/main/kotlin/io/github/vlukyanets/lemonflow/domain/rules/TimerRules.kt`
  until T031 passes.
- [X] T039 [P] [US2] Implement `app/src/main/kotlin/io/github/vlukyanets/lemonflow/domain/rules/WeekProgress.kt`
  until T032 passes.
- [X] T040 [US2] Add the record, day and settings queries to `DailyRecordDao`, `DayDao` and
  `SettingsDao`. Implement `data/repo/RoomDayRepository.kt` and
  `data/repo/RoomSettingsRepository.kt` with their bindings in `di/RepositoryModule.kt`,
  until T033 and T034 pass.
- [X] T041 [US2] Add the Today, past-day and settings strings to `values{,-ru,-uk}/strings.xml`:
  - `<plurals>` for "N times today" (ru/uk use one/few/many);
  - "Day closes at %s", "Nothing recorded", "Goal today: %s";
  - the Avoid limits wording without "minimum";
  - no banned words.
- [X] T042 [US2] Implement `app/src/main/kotlin/io/github/vlukyanets/lemonflow/ui/today/TodayViewModel.kt`:
  - combine the ports into an immutable `TodayUiState`;
  - call `settleTimers()` on start and resume;
  - keep the current user date as its own `Flow`: recompute it on start and resume, and
    schedule one tick at the exact day-end instant (`UserDayCalculator.dayEnd`) so the
    screen switches to the new date even while it stays open; reschedule after each
    rollover and whenever `dayEndHour` changes;
  - run a 1 s ticker only while a timer runs;
  - take the time from the injected `Clock`.
- [X] T043 [US2] Implement the Today composables in `app/src/main/kotlin/io/github/vlukyanets/lemonflow/ui/today/`
  (`TodayScreen.kt`, `CheckRow.kt`, `CountRow.kt`, `TimerRow.kt`, `AvoidRow.kt`,
  `WeekStrip.kt`, `PastDaySheet.kt`), matching the Today artboards, until T035 passes.
- [X] T044 [US2] Implement `app/src/main/kotlin/io/github/vlukyanets/lemonflow/ui/settings/SettingsViewModel.kt`,
  `SettingsScreen.kt` and `HourPickerSheet.kt` until T036 passes.
- [X] T045 [US2] In `ui/navigation/LemonFlowNavHost.kt`, make Today the start tab, register
  Settings, and route a tap on a habit to `HabitEditor(id)`.

**Checkpoint**: US1 + US2 make a working daily loop.

---

## Phase 5: User Story 3 - Declare a hard day (Priority: P3)

**Goal**: a manual hard-day switch that turns today's goals into Baselines.

**Independent Test**: quickstart.md scenario 7; the switch is off again the next day.

### Tests for User Story 3 (write first, must fail)

- [X] T046 [P] [US3] Add hard-day tests to `app/src/test/kotlin/io/github/vlukyanets/lemonflow/data/repo/RoomDayRepositoryTest.kt`:
  - `setHardDay` writes only today's `day` row (with a UUID, `updatedAt` and `zoneId`);
  - the next user date defaults to off;
  - toggling keeps all marks (FR-010, FR-012).
- [X] T047 [P] [US3] Add hard-day tests to `app/src/test/kotlin/io/github/vlukyanets/lemonflow/ui/today/TodayScreenTest.kt`:
  - the switch is labelled "Hard day";
  - with it on, Check rows show "Goal today: 1 page", Count and Timer rings fill to the
    Baseline, and Avoid shows "limit 1";
  - BASELINE shows as done without the `min` tag;
  - no warning copy anywhere (FR-011, US3-5);
  - `assertAllClickablesAccessible()` passes with hard day on;
  - a single click on the switch shows the Baseline goals right away, with no dialog or
    extra screen in between (SC-002).

### Implementation for User Story 3

- [X] T048 [US3] Implement `setHardDay` and the hard-day flag in `observeToday()` in
  `app/src/main/kotlin/io/github/vlukyanets/lemonflow/data/repo/RoomDayRepository.kt` until
  T046 passes.
- [X] T049 [US3] Add the hard-day switch (a `switch` role, 48dp, hint "Today counts at the
  minimum") and the hard-day presentation to `ui/today/TodayScreen.kt` and the row
  composables, plus the strings in `values{,-ru,-uk}/strings.xml`, until T047 passes.

**Checkpoint**: the product's core promise works end to end.

---

## Phase 6: User Story 4 - Data stays private and encrypted (Priority: P1, cross-cutting)

**Goal**: a calm path when the data cannot be opened, plus proof that nothing leaks.
Encryption itself ships in Phase 2.

**Independent Test**: quickstart.md sections 4 and 5.

### Tests for User Story 4 (write first, must fail)

- [X] T050 [P] [US4] Write `app/src/test/kotlin/io/github/vlukyanets/lemonflow/ui/storage/DataUnavailableScreenTest.kt`
  (fake `StorageGate`):
  - `Unavailable` shows "Your data can't be opened", with no raw error text;
  - "Start fresh" opens a confirmation dialog;
  - Cancel erases nothing;
  - Confirm calls `startFresh()`, then shows the empty Today;
  - a new launch while still `Unavailable` shows the screen again (FR-015);
  - `assertAllClickablesAccessible()` passes with and without the confirmation dialog.
- [X] T051 [P] [US4] Write `app/src/test/kotlin/io/github/vlukyanets/lemonflow/ManifestPrivacyTest.kt`
  (Robolectric `PackageManager`):
  - the merged manifest requests no `android.permission.INTERNET`;
  - `allowBackup` is false (constitution I and II).

### Implementation for User Story 4

- [X] T052 [US4] Implement `app/src/main/kotlin/io/github/vlukyanets/lemonflow/ui/storage/StartupViewModel.kt`,
  `DataUnavailableScreen.kt` and its confirmation dialog, with strings in
  `values{,-ru,-uk}/strings.xml`, until T050 passes.
- [X] T053 [US4] In `MainActivity.kt`, replace the temporary blank screen with
  `DataUnavailableScreen` and switch to the NavHost once storage becomes `Ready`; make T051
  pass.

**Checkpoint**: all four stories are done.

---

## Phase 7: Polish & Cross-Cutting Concerns

- [X] T054 [P] Write `app/src/test/kotlin/io/github/vlukyanets/lemonflow/ui/CopyRulesTest.kt`.
  It reads `app/src/main/res/values{,-ru,-uk}/strings.xml` and fails on banned words (FR-016):
  - en: failed, missed, broken, streak lost;
  - ru: провал, пропущ, пропуст, сорвал;
  - uk: провал, пропущ, пропуст, зірвал.
  Then fix any hit.
- [X] T055 [P] Write `app/src/test/kotlin/io/github/vlukyanets/lemonflow/ui/StringsCompletenessTest.kt`:
  every string and plural in `values/` also exists in `values-ru/` and `values-uk/`
  (constitution VII).
- [X] T056 [P] Confirm FR-014 holds: `./gradlew detekt` passes with the `ForbiddenMethodCall`
  rule from T002, and `config/detekt/detekt.yml` and the code contain no suppression of it
  (`@Suppress("ForbiddenMethodCall")` must not appear anywhere).
- [X] T057 [P] Update `AGENTS.md`:
  - the Project line: Room 2.8.5 + SQLCipher 4.20.0, Navigation Compose; remove DataStore
    and WorkManager from "planned";
  - "Where the code misleads": Robolectric tests use plain in-memory Room because
    SQLCipher's native library does not load on the JVM; `app/schemas/` is generated and
    committed.
- [X] T058 Run `./gradlew check assembleDebug` - it must be green.
- [X] T059 Run quickstart.md sections 2-5 on a device or emulator, including the restart
  check (scenario 7a), the timings in scenarios 2 and 7 (SC-001, SC-002), the lock-screen
  change (section 4a) and the
  `strings | grep -c Therapy` check that must return 0. Record anything that could not be
  verified.
  - Progress 2026-11-10 (Samsung SM-A576B, Android 17): sections 2-3 run during the device
    review (habits of all four types, marks, timers, hard day, past day, language, 130% and
    150% font, light and dark). Section 4 passed: "Therapy", "Reading" and "Water" occur 0
    times in `lemonflow.db`, `-wal` and `-shm`; the database starts with random bytes; the
    key file is 60 bytes; the APK requests no INTERNET; `allowBackup=false`. The quickstart
    command checked only `lemonflow.db` although data lived in the WAL - fixed there.
    4a passed (owner): a fingerprint was added, the phone rebooted, and the app opened
    with all habits and marks; no "data can't be opened" screen.
    5 passed: with `db_key.bin` removed the calm screen appears (no crash, no raw error, no
    habit names in logcat); reopening shows it again and erases nothing; "Start fresh" asks
    first and then opens an empty Today. Found and fixed on the way: the screen's title was
    black on the dark background (no Surface above StartupHost).
    Timings (T074): SC-001 passed - the owner created "Reading" (Check, 30 min / 1 page) by
    hand in about 15-20 s, estimated rather than stopwatched (limit 30 s). SC-002 passed -
    measured on a screen recording with "show touches": the touch indicator appears at
    1.465 s and the plan shows "Goal today: 1 page" at 1.505 s, about 40 ms (limit 1 s).

### App language (FR-022, added after device review)

- [X] T060 Write `app/src/test/kotlin/io/github/vlukyanets/lemonflow/ui/settings/SettingsScreenTest.kt`
  cases: a Language row shows the current choice; the sheet lists System default, English,
  Русский, Українська as a radio group; picking one calls the port and closes the sheet;
  accessible with the sheet open.
- [X] T061 Add `domain/model/AppLanguage.kt`, the `LanguageSetting` port, its
  `data/system/SystemLanguageSetting.kt` implementation over `LocaleManager` (R4 exception)
  and a Hilt binding; turn on `generateLocaleConfig` with `res/resources.properties`.
- [X] T062 Add the Language row and `LanguagePickerSheet` to settings, with strings in
  `values{,-ru,-uk}/strings.xml` (language names `translatable="false"`), until T060 passes;
  run `./gradlew check assembleDebug`.
- [X] T063 Add the "About" section to `SettingsScreen` (FR-023): app name, version from
  the package, "Made by Valentyn Lukianets"; strings in `values{,-ru,-uk}` (the name
  `translatable="false"`); a `SettingsScreenTest` case; settings content scrolls and
  clears the floating tab bar.
- [X] T064 Add `licenses/Apache-2.0.txt` and `licenses/SQLCipher.txt` (verified downloads,
  R11) and a Gradle task that copies them with `LICENSE` into generated assets.
- [X] T065 Add `ui/about/` - `OssComponents.kt`, `LicensesScreen`, `LicenseTextScreen` -
  routes `LicensesRoute` / `LicenseRoute(id)` under the Settings tab, and About rows
  License / Source code / Open source licenses (FR-024), with strings in
  `values{,-ru,-uk}`.
- [X] T067 Device review fixes: write "minimum" in full in every locale ("min"/"мин" mean
  minutes; `MinimumWordingTest`); remove the Check row's `min` button - one tap marks the
  day's goal, Baseline on a normal day is raised to Target, otherwise undo (FR-007 as
  revised); update Today tests.
- [X] T068 Device review, layout: rows never add lines - the name keeps one line and the
  "minimum" text tag became a lifebuoy icon next to it, with the minimum part of the meta
  line highlighted; the Habits title and Today's day line wrap whole items (FlowRow)
  instead of breaking words; a finished timer is a filled check without a corner badge.
- [X] T066 Tests: every license asset loads and is non-empty; the licenses screen lists
  the components and opens a license; paragraph reflow; run the full check.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: none.
- **Foundational (Phase 2)**: needs Setup; blocks every story.
- **US1 (Phase 3)**: needs Foundational.
- **US2 (Phase 4)**: needs Foundational; it uses habits, so test data comes from the fake
  `HabitRepository`. Doing US1 first gives a real end-to-end loop.
- **US3 (Phase 5)**: needs US2 (Today screen and `RoomDayRepository`).
- **US4 (Phase 6)**: needs Foundational only; it can run in parallel with US1-US3.
- **Polish (Phase 7)**: needs every story.

### Within Each Story

- The tests (T020-T023, T030-T036, T046-T047, T050-T051) are written and fail first.
- Then the domain rules, the repositories, the strings, the ViewModels, the screens and the
  navigation wiring, in that order.

### Parallel Opportunities

- Setup: T003 can run alongside T001-T002.
- Foundational: T004, T005 and T006 together; T007, T009, T017 and T018 together.
- US1 tests T020-T023 together; US2 tests T030-T036 together; US2 rules T037-T039 together.
- US4 (T050-T053) can be built alongside US1-US3.
- Polish: T054-T057 together.

---

## Parallel Example: User Story 2

```bash
Task: "MarkRulesTest in app/src/test/kotlin/io/github/vlukyanets/lemonflow/domain/rules/MarkRulesTest.kt"
Task: "TimerRulesTest in app/src/test/kotlin/io/github/vlukyanets/lemonflow/domain/rules/TimerRulesTest.kt"
Task: "WeekProgressTest in app/src/test/kotlin/io/github/vlukyanets/lemonflow/domain/rules/WeekProgressTest.kt"
Task: "TodayScreenTest in app/src/test/kotlin/io/github/vlukyanets/lemonflow/ui/today/TodayScreenTest.kt"
```

---

## Implementation Strategy

### MVP First

1. Phase 1 + Phase 2: an encrypted, empty app with tabs.
2. Phase 3 (US1): habits can be defined. Stop and validate them on the Habits tab.
3. Phase 4 (US2): the daily loop. This is the first build worth installing for daily use.

### Incremental Delivery

US3 (hard day) and US4 (data-unavailable path) each add one self-contained increment.
Polish closes the feature. Commit after each task or logical group, and only with a green
`./gradlew check`.

---

## Notes

- [P] means a different file and no dependency on an unfinished task.
- Every field constraint above is quoted from data-model.md; do not relax it during
  implementation without updating the spec and plan first (AGENTS.md).
- Ask before adding any dependency that is not in plan.md, or any permission.

## Phase 8: Convergence

- [X] T069 CRITICAL: move the user-facing text of `ui/about/OssComponents.kt` (component
  names, authors, license titles, "(part of SQLCipher)") into `values{,-ru,-uk}/strings.xml` -
  proper names `translatable="false"`, descriptive parts translated - and read them with
  `stringResource` per Constitution VII (contradicts)
- [X] T070 Update the Today row table in `contracts/ui-navigation.md`: one round check for
  CHECK (no `min` toggle), "minimum" never shortened to "min", the lifebuoy icon plus the
  highlighted minimum part of the meta line instead of the `min` tag, a finished timer as a
  filled check without a play badge per contracts/ui-navigation.md (contradicts)
- [X] T071 Align FR-002b with the one-line habit name on Today (owner decision in T068: no
  row grows a line; long names end with an ellipsis) per FR-002b (contradicts)
- [X] T072 Reword User Story 2 (its description and AC3) to match the revised FR-007: on a
  normal day a Baseline mark comes from the amount for Count/Timer/Avoid, and a Check habit
  is marked at Baseline through the hard day per US2/AC3 (contradicts)
- [X] T073 Record the floating tab bar in `plan.md` and `contracts/ui-navigation.md` (pill
  that wraps label-width tabs, icons-only fallback with spoken labels, sliding highlight,
  content scrolling under it via `LocalBottomBarSpace`) per FR-020 / plan: navigation
  (unrequested)
- [X] T074 Time SC-001 (create a habit by hand in under 30 s) and SC-002 (hard day redraws
  within 1 s) on the device with a stopwatch and record the results under T059 per SC-001,
  SC-002 (partial)
- [X] T075 Update the "Settings" key entity in `spec.md`: "Day ends at" in the database, the
  app language kept by the system (research R4), and the About section per spec: Key
  Entities (partial)
