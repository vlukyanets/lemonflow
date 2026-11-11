# LemonFlow

An offline, encrypted Android habit tracker that adaptively compresses the day's plan down
to a rescue minimum during force majeure and fatigue - protecting streaks from burning out
and freeing you from guilt.

- Works fully offline; no accounts, no analytics, no cloud backup.
- All data stays on the device, encrypted.
- Free software (GPL-3.0-or-later) with FOSS-only dependencies.

Status: early development - the app is a skeleton; features arrive one spec at a time.

## Prerequisites

- JDK 21
- Android SDK with `platforms/android-37.0` (compileSdk 37; the app targets SDK 36 and
  runs on Android 14+). Point Gradle at it with `sdk.dir=<path>` in `local.properties`
  (gitignored) or the `ANDROID_HOME` environment variable.

## Build, run, test

```sh
./gradlew assembleDebug            # build the debug APK (downloads Gradle and dependencies)
./gradlew :app:installDebug        # install on a connected device or emulator
./gradlew spotlessApply            # format
./gradlew :app:testDebugUnitTest   # unit tests (JVM + Robolectric, no device needed)
./gradlew check assembleDebug      # full check: format, detekt, lint, tests, build - same as CI
```

## Development workflow

Features are built spec-first with [GitHub Spec Kit](https://github.com/github/spec-kit):
`/speckit-specify` -> `/speckit-plan` -> `/speckit-tasks` -> `/speckit-implement`, each
feature in its own git worktree, with specs under `specs/<timestamp>-<name>/`. Every plan
is checked against the project principles in `.specify/memory/constitution.md`; agent
instructions live in `AGENTS.md`.

## License

Licensed under the GNU General Public License v3.0 or later (see `LICENSE`), except
`.specify/` and `.claude/skills/speckit-*` (MIT, GitHub Spec Kit) - see
`THIRD_PARTY_NOTICES.md`.
