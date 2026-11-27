# AGENTS.md

Instructions for AI coding agents. Only what can't be learned by reading the code.

## Project

**LemonFlow** - a personal, offline Android workday planner: a catalog of items -> a day
plan -> tracking -> a live schedule of what still fits before the day closes.

- Stack: Kotlin 2.4.21, AGP 9.5.1 (built-in Kotlin), Gradle 9.9.0, Jetpack Compose (BOM
  2026.09.00), Room 2.8.5 + SQLCipher 4.20.0, minSdk 34, compileSdk/targetSdk 37, JDK 21.
- Modules: `:domain` - pure Kotlin/JVM business logic, no Android imports; `:app` - Android.

## Spec

The spec is **not** in this repository. It is a Claude Doc, "LemonFlow - Workday Planner
Spec v1": https://claude.ai/artifact/6WuFjx4ivtg9jEzyvCrmcF. Requirement ids in code
comments (`TRK-6`, `DAY-4`, ...) refer to it. Never copy the spec into the repo.

## Commands

| Task | Command |
| --- | --- |
| Domain tests | `./gradlew :domain:test` |
| One test class | `./gradlew :domain:test --tests 'io.github.vlukyanets.lemonflow.domain.schedule.ScheduleEngineTest'` |
| Debug APK | `./gradlew :app:assembleDebug` |
| **Full check** | `./gradlew :domain:test :app:assembleDebug :app:lintDebug` |

A task isn't finished until the full check passes.

## Rules

- **Offline**: the app has no `INTERNET` permission; no dependency may add one (check the
  merged manifest).
- **Licensing**: the project is dual-licensed `Apache-2.0 OR MIT` (see `README.md`). Every
  dependency, font and asset must allow that: permissive licenses only (Apache-2.0, MIT,
  BSD, ISC, ...), no GPL/LGPL/AGPL. The bundled font is Roboto (Apache-2.0). Ask before
  adding anything under another license (OFL, CC, ...).
- **Third-party notices**: the AboutLibraries plugin generates the in-app licenses screen
  at build time, offline, and fails the build on a license outside the allow-list in
  `app/build.gradle.kts`. A new dependency whose POM lacks a license name or whose license
  text isn't bundled needs a JSON override in `app/aboutlibraries/` (see SQLCipher there).
  Don't widen the allow-list without asking.
- **Icons**: Material Symbols (Apache-2.0, Outlined, 24dp) copied as vector drawables into
  `res/drawable/ic_*.xml` from github.com/google/material-design-icons
  (`symbols/android/<name>/materialsymbolsoutlined/<name>_24px.xml`), with
  `?android:attr/colorControlNormal` as the tint. No `material-icons-*` dependency.
- **Data**: all user data lives only in the SQLCipher database. UUID string keys, UTC
  epoch-millis instants, logical dates and zones stored separately (see `Entities.kt`).
- **Business logic** goes into `:domain` as pure functions with time passed in, and gets
  JVM unit tests: schedule, day boundaries (DST and zone cases), tracking, day close,
  planning rules. UI is not tested for now.
- Ask first before adding dependencies, changing the DB schema after v1 ships, or adding
  manifest permissions.

## Text style

Code, comments, docs and commit messages in English, plain ASCII punctuation (`-`, `->`,
`...`), except user-facing copy that needs other characters.

## Git

- Commit subject: past-tense verb + what, capitalised, no trailing period, <= 72 chars
  (`Added schedule engine`, `Fixed overflow at day close`).
- Don't commit or push unless asked.
