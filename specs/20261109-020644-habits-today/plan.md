# Implementation Plan: Habits and Today

**Branch**: `worktree-habits-today` | **Date**: 2026-11-09 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/20261109-020644-habits-today/spec.md`

## Summary

The first real feature of LemonFlow covers habits of four types (Check, Count, Timer, Avoid),
each with a Target and a rescue-minimum Baseline. Users get a Today screen with per-type
controls, a manual hard-day switch, a read-only 7-day strip, archive/restore/delete,
a "Day ends at" setting, an app language setting (FR-022, R4) and an Insights placeholder.

Everything is stored in a SQLCipher-encrypted Room database whose random key is wrapped by an
Android Keystore key. All rules (user day, marks, timers, avoid default, validation, week
progress) live in a pure-Kotlin `domain` package, are driven by an injected `Clock` and are
written test-first. The UI follows the "LemonFlow" design canvas: a fixed palette and a
bundled Roboto font (Apache-2.0).

## Technical Context

**Language/Version**: Kotlin 2.4.21 (AGP 9.5.1 built-in Kotlin), JDK 21.

**Primary Dependencies**: existing Compose BOM 2026.09.00 / Material 3 / Hilt 2.60.1, plus the
new ones in the table below.

**Storage**: Room over SQLCipher (`lemonflow.db`); wrapped key in `noBackupFilesDir`.

**Testing**: JUnit 4 (domain, JVM), Robolectric 4.17 (Room in-memory, Compose UI),
`kotlinx-coroutines-test`; manual device checks in [quickstart.md](quickstart.md).

**Target Platform**: Android 14+ (minSdk 34, compileSdk 37, targetSdk 36).

**Project Type**: mobile app, single `:app` module.

**Performance Goals**: Today reacts to a tap within 1 frame budget (no blocking I/O on the main
thread); hard-day switch updates the plan within 1 s (SC-002); timer text updates once a
second while a timer runs.

**Constraints**: fully offline (no `INTERNET`), no background work or new permissions, no
plaintext user data, every touch target at least 48dp, en/ru/uk.

**Scale/Scope**: a single user, tens of habits and years of daily records (~10^4 rows);
8 screens or sheets.

### New dependencies (principle III)

| Artifact | Version | License | Why |
|----------|---------|---------|-----|
| `androidx.room:room-runtime`, `room-ktx`, `room-compiler` (KSP) | 2.8.5 | Apache-2.0 | persistence (R1) |
| `net.zetetic:sqlcipher-android` | 4.20.0 | BSD-3-Clause-style (SQLCipher Community) | encryption (R1) |
| `androidx.navigation:navigation-compose` | 2.10.2 | Apache-2.0 | tabs + editor/archive routes (R6) |
| `org.jetbrains.kotlin.plugin.serialization` + `kotlinx-serialization-core` | 2.4.21 / 1.12.0 | Apache-2.0 | type-safe routes (R6) |
| `androidx.hilt:hilt-lifecycle-viewmodel-compose` | 1.4.0 | Apache-2.0 | `hiltViewModel()` (R7) |
| `androidx.lifecycle:lifecycle-viewmodel-compose`, `lifecycle-runtime-compose` | 2.11.0 | Apache-2.0 | ViewModels, `collectAsStateWithLifecycle` (R7) |
| `org.jetbrains.kotlinx:kotlinx-coroutines-test` (test) | 1.11.0 | Apache-2.0 | Flow tests (R9) |
| Roboto font (Regular/Medium/Bold, `res/font`) | googlefonts/roboto v2.138 | Apache-2.0 | design typography, Cyrillic (R8) |

Not added, even though `AGENTS.md` lists them as planned: DataStore (R4) and WorkManager (R5).

## Constitution Check

*GATE: checked before Phase 0 research and again after Phase 1 design.*

| Principle | Status | How |
|-----------|--------|-----|
| I. Offline-first, sync-ready | PASS | no `INTERNET`; UUID TEXT keys; instants in UTC millis; user date and zone id stored separately ([data-model.md](data-model.md)) |
| II. Encrypted and private | PASS | SQLCipher with a random key wrapped by an AndroidKeyStore AES-GCM key, not auth-bound (R2); settings in the encrypted DB, not DataStore (R4); `allowBackup=false` and extraction rules unchanged; no logging of user data; no analytics |
| III. FOSS-only | PASS | all libraries and the bundled Roboto font are Apache-2.0 or BSD-style, each listed above |
| IV. Test-first domain (NON-NEGOTIABLE) | PASS | pure-Kotlin `domain`, no `android.*`; `Clock` injected; tests for day boundary, Kyiv DST, all mark rules and timer settling come before the implementation tasks |
| V. Compassionate by design | PASS | Baseline always counts; hard day presents Baseline as done; Avoid above the limit shows the count only; banned-word list for all locales (FR-016) |
| VI. Simplicity first | PASS | one `:app` module, packages `ui -> domain <- data`; no flavors; no WorkManager/DataStore; no archive-period table (R10) |
| VII. Localized and accessible | PASS | strings and plurals in values, values-ru, values-uk; 48dp targets; content descriptions on icon buttons |

Post-design re-check: still PASS - the data model, ports and screens add no new modules,
permissions or plaintext stores.

## Project Structure

### Documentation (this feature)

```text
specs/20261109-020644-habits-today/
├── spec.md
├── plan.md              # this file
├── research.md          # Phase 0
├── data-model.md        # Phase 1
├── quickstart.md        # Phase 1
├── contracts/
│   ├── domain-ports.md
│   └── ui-navigation.md
├── checklists/requirements.md
└── tasks.md             # Phase 2 (/speckit-tasks)
```

### Source Code

```text
app/src/main/kotlin/io/github/vlukyanets/lemonflow/
├── LemonFlowApplication.kt, MainActivity.kt
├── domain/
│   ├── model/        # Habit, HabitType, AmountUnit, Mark, DailyRecord, DayState, HabitDraft, ValidationError
│   ├── time/         # UserDayCalculator (R3)
│   ├── rules/        # MarkRules, TimerRules, AvoidRules, WeekProgress, HabitValidator
│   └── port/         # HabitRepository, DayRepository, SettingsRepository, StorageGate
├── data/
│   ├── db/           # entities, DAOs, LemonFlowDatabase, converters
│   ├── crypto/       # DatabaseKeyManager, KeyStoreGateway (R2)
│   └── repo/         # port implementations, StorageGate implementation
├── di/               # Hilt modules: Clock, database holder, repositories
└── ui/
    ├── theme/        # palette, typography (Roboto), LemonFlowTheme
    ├── navigation/   # routes, NavHost, floating tab bar (R6 revision)
    ├── today/        # TodayScreen, rows per type, week strip, past-day sheet, TodayViewModel
    ├── habits/       # HabitsScreen, ArchiveScreen, ViewModels
    ├── editor/       # HabitEditorScreen, EditorViewModel
    ├── settings/     # SettingsScreen, hour and language sheets, About section
    ├── about/        # open source licenses list and license text screens (R11)
    ├── insights/     # placeholder
    └── storage/      # DataUnavailableScreen, StartupViewModel
app/src/main/res/     # font/, drawable/ (stroke icons), values{,-ru,-uk}/strings.xml
app/src/test/kotlin/io/github/vlukyanets/lemonflow/
├── domain/           # JVM tests for every rule (written first)
├── data/             # Robolectric Room DAO + repository tests (no SQLCipher), key manager with fake gateway
└── ui/               # Robolectric Compose tests with fake ports
app/schemas/          # Room schema export (committed)
```

**Structure Decision**: the existing single `:app` module. New code goes into the `domain`,
`data` and `ui` package layers the constitution prescribes. The skeleton's
`ui/main/MainScreen` is replaced by the Today screen, and its test moves with it.

## Complexity Tracking

| Item | Why needed | Simpler alternative rejected because |
|------|------------|--------------------------------------|
| `kotlinx-serialization` compiler plugin | Type-safe navigation routes with a habit id argument | String routes give up compile-time checks for little gain |
