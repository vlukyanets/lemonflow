# AGENTS.md

Instructions for AI coding agents (Claude Code, Codex, Cursor, Copilot, Gemini, ...).
This is the single source of truth: tool-specific files (e.g. `CLAUDE.md`) import it and
add only tool-specific extras.

> Only write what an agent can't learn by reading the repo, or would waste time
> rediscovering every session. Rules with zero exceptions belong in permission deny rules
> or CI, not here.

## Project

**LemonFlow** - an offline, encrypted Android habit tracker that adaptively compresses the
day's plan down to a rescue minimum during force majeure and fatigue, protecting streaks
and freeing the user from guilt.

- Stack: Kotlin 2.4.21, Jetpack Compose (BOM 2026.09.00) + Material 3, Hilt 2.60.1 (KSP
  2.3.13), AGP 9.5.1 on Gradle 9.9.0, JDK 21; minSdk 34, compileSdk 37, targetSdk 36.
  Planned for persistence: Room + SQLCipher, DataStore, WorkManager.
- Quality: Spotless + ktlint 1.8.0 (`android_studio` style), detekt 1.23.8, Android Lint
  (warnings are errors), JUnit 4 + Robolectric 4.17.
- Application ID and root package: `io.github.vlukyanets.lemonflow`.

## Commands

Use exactly these - don't invent equivalents. Prerequisites: JDK 21 and an Android SDK
with `platforms/android-37.0`, found via `sdk.dir` in `local.properties` or `ANDROID_HOME`.

| Task                                | Command |
| ----------------------------------- | ------- |
| Install deps                        | `./gradlew assembleDebug` |
| Run the app                         | `./gradlew :app:installDebug && adb shell am start -n io.github.vlukyanets.lemonflow/.MainActivity` |
| Format                              | `./gradlew spotlessApply` |
| Run one test                        | `./gradlew :app:testDebugUnitTest --tests "io.github.vlukyanets.lemonflow.ui.main.MainScreenTest"` |
| **Quick check** - while iterating   | `./gradlew :app:testDebugUnitTest` |
| **Full check** - before finishing   | `./gradlew check assembleDebug` |

A task isn't finished until the full check passes. `check` runs `spotlessCheck`, `detekt`,
`lintDebug` and the unit tests; there are no instrumented tests in the check.

## Workflow - Spec-Driven Development (Spec Kit)

The steps below are Spec Kit skills; invocation depends on the agent (Claude Code:
`/speckit-plan`).

- **Features and non-trivial changes** go through the full cycle, stopping after each
  step for my review:
  `specify` -> `clarify` -> `plan` -> `checklist` -> `tasks` -> `analyze` -> `implement` ->
  `converge`. Repeat `implement` + `converge` until converge reports `Converged`.
  `clarify`, `checklist` and `analyze` may be skipped for small, unambiguous features.
- **Bugs** go through the `bug` extension: `speckit-bug-assess` -> `speckit-bug-fix` ->
  `speckit-bug-test`. **Unsure ideas** can be vetted with the `assess` extension
  (`speckit-assess-intake` ... `speckit-assess-decide`) before `specify`.
- **Small fixes** (typo, config tweak, obvious one-line bug) skip the cycle: just make the change.
- **One feature per worktree** - develop each feature in its own git worktree under
  `.claude/worktrees/<name>/` (gitignored), not in the main checkout. A worktree is a
  fresh checkout: run *Install deps* there first.
- **Feature folders are timestamped** - `specs/<YYYYMMDD-HHMMSS>-<name>/`, with `<name>`
  the same as the worktree name (Spec Kit's `timestamp` numbering in
  `.specify/init-options.json`), so parallel worktrees never collide.
- **Constitution** - `.specify/memory/constitution.md` holds the project's principles;
  every plan is checked against it. Read it before planning. Change it only via
  `speckit-constitution`, and only when I ask.
- **Feature artifacts** live in `specs/<timestamp>-<name>/` (`spec.md`, `plan.md`, `research.md`,
  `data-model.md`, `contracts/`, `quickstart.md`, `tasks.md`). If implementation has to
  deviate, update the spec/plan first - don't let code and specs drift apart.

## Where the code misleads

- AGP 9 has built-in Kotlin: never apply `org.jetbrains.kotlin.android` in `app/`. The root
  `build.gradle.kts` declares it `apply false` only to pin the Kotlin version; to bump
  Kotlin, change `kotlin` in `gradle/libs.versions.toml` (the Compose compiler follows).
- compileSdk 37 with targetSdk 36 is deliberate: current AndroidX needs compileSdk 37, while
  raising targetSdk opts into new runtime behaviour and is its own reviewed change.
- The `--add-exports=java.base/jdk.internal.access=ALL-UNNAMED` JVM arg for unit tests is
  required by Robolectric's SDK 36 sandbox on JDK 21 - removing it breaks every
  Robolectric test.
- Gradle prints "Deprecated Gradle features were used": it comes from the detekt 1.23.x
  plugin (detekt 2.0 is still alpha), not from our build scripts.
- No product flavors yet. When the first store-specific difference appears, put it behind
  an interface in `domain` and add a `distribution` flavor dimension with `foss` and `play`.

## Text style

Write code, comments, docs, specs, commit messages and PR descriptions in English, even
when I talk to you in another language. Use plain ASCII punctuation: `-` instead of a long dash (`—`, `–`), `->` instead of an arrow (`→`),
`...` instead of a single-character ellipsis (`…`). Exception: text where the character
itself is the point (user-facing copy that needs it, tests for Unicode handling).
User-facing strings in `values-ru/` and `values-uk/` are written in Russian and Ukrainian.

## Git

- Commit subject: past-tense verb + what, capitalised, no trailing period, <= 72 chars.
  - `Added <component>` - new feature, module or file
  - `Fixed <short description of the bug>`
  - `Updated <thing>` - changed behaviour, dependencies or docs
  - `Refactored <component>` - no behaviour change
  - `Removed <thing>`
  - e.g. `Added CSV export to reports`, `Fixed crash on empty input in parser`
- Optional body: *why*, not what.
- One logical change per commit, only when the full check passes.
- Don't commit or push unless asked.

### AI attribution

- When an agent wrote or substantially rewrote code, tests or docs in a commit, add a
  trailer crediting it: the tool's standard `Co-Authored-By:` line if it has one,
  otherwise `Assisted-by: <Tool> (<model>)`. Trivial autocomplete needs none.
- The human is always the commit author and is accountable for the change.
- Name the real tool and model. Never remove, forge, or invent attribution.
- No session links: no `Claude-Session:` (or similar) trailers and no agent session URLs
  in commits or PR descriptions.
- Keep trailers when squashing commits.
- PR descriptions say which AI tools were used, for what, and how the output was verified.

## Boundaries

- Ask first - unless the approved `plan.md` / `tasks.md` already covers it: adding
  dependencies, changing public APIs or DB schema, deleting files, touching CI, auth,
  encryption or key handling, manifest permissions, or minSdk / compileSdk / targetSdk.
- Never -> do this instead:
  - Change `applicationId` -> it identifies the app on every install and store forever; ask me.
  - Read, print or commit secrets (`.env`, keys, `*.jks` / `*.keystore`, signing
    passwords) -> keep signing config in `~/.gradle/gradle.properties`, outside the repo.
  - Skip, disable or weaken a failing test -> fix the code, or ask if the test is wrong.
  - Suppress a lint, detekt or ktlint finding to get the check green -> fix the code, or
    ask me; an agreed suppression goes in `config/detekt/detekt.yml` or the `lint {}` block
    with a comment saying why.
  - Run destructive commands (`rm -rf`, `git reset --hard`, DB drops) -> ask me.
  - Edit files through shell commands (heredoc `cat > file`, `sed -i`, `tee`, scripts that
    write files) -> use the agent's file-editing tool, so every change is a reviewable
    diff. Running shell or Python for anything else (tests, checks, search) is fine.
  - Edit Spec Kit's managed files (`.specify/templates/`, `.specify/scripts/`,
    `.specify/extensions/`, `.claude/skills/speckit-*`) -> they're overwritten on upgrade;
    customise via a Spec Kit preset, or ask me.

## Gotchas

- **Searching from the main checkout**: `.claude/worktrees/` holds full checkouts of other
  features - exclude it from `grep` / `find`, or you'll read and edit another feature's code.
