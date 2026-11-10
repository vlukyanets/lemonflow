# Research: Habits and Today

All Technical Context unknowns are resolved below. Versions were checked against Google Maven
and Maven Central on 2026-11-09 (latest stable).

## R1. Encrypted storage: Room + SQLCipher

- **Decision**: Room 2.8.5 (`room-runtime`, `room-ktx`, `room-compiler` via KSP) on top of
  `net.zetetic:sqlcipher-android` 4.20.0, wired in through its `SupportOpenHelperFactory`
  (`Room.databaseBuilder(...).openHelperFactory(factory)`); `androidx.sqlite` 2.7.1 comes with
  Room.
- **Rationale**: the constitution (principle II) names SQLCipher-encrypted Room. The
  `SupportOpenHelperFactory` path is the one SQLCipher documents for Room 2.x and keeps DAOs,
  migrations and Flow queries unchanged.
- **Alternatives considered**: Room's KMP `SQLiteDriver` path - SQLCipher has no stable driver
  for it yet; DataStore for settings - rejected, see R4; `androidx.security:security-crypto` -
  deprecated, and it only covers files and preferences, not SQLite.

## R2. Database key handling

- **Decision**: on first launch, generate 32 random bytes with `SecureRandom` as the SQLCipher
  passphrase. Wrap them with an AES-256-GCM key kept in `AndroidKeyStore` under the alias
  `lemonflow_db_wrap` (`PURPOSE_ENCRYPT | PURPOSE_DECRYPT`, `setUserAuthenticationRequired(false)`,
  no StrongBox requirement). Store `IV + ciphertext` in `noBackupFilesDir/db_key.bin`. Unwrap
  on every start; the passphrase is held only in memory and zeroed after the database opens.
- **Rationale**: FR-013/FR-013a - the key is device-generated, Keystore-protected and not bound
  to user authentication, so a PIN change never invalidates it. `noBackupFilesDir` plus the
  existing `data_extraction_rules.xml` keep the wrapped key out of backups and device transfer.
- **Failure handling (FR-015)**: any of (wrapped-key file present but Keystore entry missing,
  `KeyPermanentlyInvalidatedException`, `AEADBadTagException`, SQLCipher "file is not a
  database") puts the app into the *data unavailable* state. "Start fresh" deletes the database
  files, the key file and the Keystore entry, then runs first-launch setup. A database file
  with no key file at all is treated the same way. Nothing is deleted before the user confirms.
- **Alternatives considered**: user passphrase - violates FR-013a (no setup step); deriving the
  key from `ANDROID_ID` - not secret.

## R3. Time and the user's day

- **Decision**: the domain receives time only through an injected `java.time.Clock` (Hilt
  provides `Clock.systemDefaultZone()`; tests use fixed or mutable clocks). The user's date of
  an instant is `local = instant.atZone(zone)`; if `local.toLocalTime() < dayEnd` then
  `local.toLocalDate().minusDays(1)` else `local.toLocalDate()`. The end of date `D` is
  `ZonedDateTime.of(D.plusDays(1), dayEnd, zone).toInstant()`.
- **Rationale**: `ZonedDateTime.of` already resolves a DST gap to the first moment after it
  and an overlap to the earlier offset, which is exactly the spec's edge-case rule (Kyiv moves
  03:00 -> 04:00 in spring, the default day end). java.time is in the platform at minSdk 34, so
  no extra library is needed.
- **Alternatives considered**: kotlinx-datetime - an extra dependency with no benefit on
  Android-only code.

## R4. Where settings live

- **Decision**: "Day ends at" is a column in an encrypted `settings` table (single row) in the
  same Room database.
- **Rationale**: principle II forbids user data in plaintext DataStore/SharedPreferences; one
  encrypted store is simpler than adding DataStore for one value (principle VI).
- **Alternatives considered**: DataStore - extra dependency, plaintext.
- **Exception - app language (FR-022)**: stored by Android's per-app language setting
  (`LocaleManager.applicationLocales`, API 33+, so no AppCompat dependency), not in our
  database. The system must know the language before the app opens its database (and the
  "Your data can't be opened" screen must be in the chosen language too); the value is a
  language code, not user data. `generateLocaleConfig` publishes en/ru/uk so the choice also
  appears in the system's App languages screen.

## R5. Timers without background work

- **Decision**: a running timer is a daily record with `timerStartedAt` set; elapsed time =
  `amountSeconds + (now - timerStartedAt)`, never negative (a clock jump backwards adds 0).
  Whenever the app reads records (start, resume, the Today ticker), it *settles* every running
  timer whose day has ended: it adds the time up to that day's end and clears `timerStartedAt`.
  The Today screen ticks once a second only while some timer is running.
- **Rationale**: FR-007d needs timers to survive app closure and reboots and to stop at the
  day end; computing from the stored instant does that without WorkManager, a foreground
  service or the notification permission. Reaching the Target needs no action, because the
  mark is derived from the amount.
- **Alternatives considered**: WorkManager to stop timers at day end - unnecessary; foreground
  service with a notification - needs `POST_NOTIFICATIONS`, out of scope.

## R6. Navigation

- **Decision**: Navigation Compose 2.10.2 with type-safe `@Serializable` routes (Kotlin
  serialization plugin at the Kotlin version, `kotlinx-serialization-core` 1.12.0), a bottom bar
  with four top-level destinations that save and restore state, plus pushed routes for the
  editor and the archive. The past day and the "Day ends at" picker are bottom sheets, not
  routes.
- **Rationale**: multiple top-level destinations with state restore are built in; type-safe
  routes avoid stringly-typed arguments (the habit id for the editor).
- **Alternatives considered**: Navigation 3 (1.2.1) - per-tab back stacks must be built by
  hand; string routes - no compile-time checks.
- **Revision after device review**: the bottom bar is a floating pill with label-width tabs
  (`ui/navigation/FloatingTabBar.kt`), not Material's `NavigationBar`: equal-width tabs
  wrapped the Ukrainian "Налаштування", and shrinking the label looked wrong. Below a
  minimum side padding it falls back to icons only. The license screens are pushed routes
  under the Settings tab. Details: contracts/ui-navigation.md, "Bottom navigation". No new
  dependency (a blurred "glass" background would need Haze and was not taken).

## R7. ViewModels and DI

- **Decision**: `hilt-lifecycle-viewmodel-compose` 1.4.0 (`hiltViewModel()`),
  `lifecycle-viewmodel-compose` and `lifecycle-runtime-compose` 2.11.0
  (`collectAsStateWithLifecycle`). Screens expose an immutable UI state via `StateFlow`.
- **Rationale**: the standard Hilt + Compose path, already in use with Hilt 2.60.1.

## R8. Theme and fonts

- **Decision**: replace dynamic color with a fixed light/dark palette taken from the design
  canvas (background #F4F4F1 / #0D0E11, accent #2448E8 / #7D93FF, and so on), defined as
  Material 3 `ColorScheme`s plus a small `LemonFlowColors` extension for design-only tokens
  (minimum-state fill, running-timer row). Bundle Roboto from the googlefonts/roboto release
  v2.138 (Apache-2.0) in `res/font/`: static Regular (400), Medium (500) and Bold (700) files.
  Body text uses Regular, emphasis Medium, headings Bold; numbers use tabular figures
  (`fontFeatureSettings = "tnum"`). The Apache-2.0 notice is added to
  `THIRD_PARTY_NOTICES.md`.
- **Rationale**: principle III needs a GPL-compatible license. Apache-2.0 is compatible with
  GPL-3.0; Inter and Manrope (SIL OFL 1.1) are not on the FSF list of GPL-compatible
  licenses, so the owner chose Roboto. Roboto covers Cyrillic, and bundling it makes the app
  look the same on devices whose system font differs (Samsung, Xiaomi). Fonts must be bundled
  because the app has no network. Icons are hand-made vector drawables traced from the
  design's stroke SVGs, so `material-icons-extended` is not needed.
- **Alternatives considered**: Inter + Manrope (OFL) - licence question under principle III;
  Roboto + Roboto Slab (Apache-2.0) - changes the design's character more; the system font -
  differs between vendors; GNU FreeSans (GPL with font exception) - dated, no tabular figures;
  downloadable Google Fonts - needs Play Services and network.

## R9. Testing approach

- **Decision**:
  - Domain rules (day calculation, marks, timers, avoid, validation, week progress) - plain
    JVM JUnit 4 tests with fixed clocks and zones, including Europe/Kyiv DST dates.
  - DAOs and repositories - Robolectric with `Room.inMemoryDatabaseBuilder` and no SQLCipher
    factory, because SQLCipher's native library cannot load on the JVM.
  - Key manager - unit-tested behind a `KeyStoreGateway` interface with a fake; the real
    Keystore and the "no readable text in the DB file" check are verified on a device through
    `quickstart.md`.
  - Screens - Robolectric Compose tests with fake repositories.
  - `kotlinx-coroutines-test` 1.11.0 for Flows.
- **Rationale**: keeps `./gradlew check` device-free (AGENTS.md) while covering every rule the
  constitution calls out (principle IV).
- **Alternatives considered**: instrumented tests in `check` - would need an emulator in CI;
  Turbine - not needed for the few Flow assertions.

## R11. License notices (FR-024)

- **Decision**: license texts live once in the repository - `LICENSE` (GPL-3.0) and
  `licenses/` (Apache-2.0 from apache.org, SQLCipher's BSD-style license from
  sqlcipher/sqlcipher-android) - and a Gradle task copies them into generated assets.
  The component list is hand-written in `ui/about/OssComponents.kt` from the release
  runtime classpath (all Apache-2.0 per their POMs, except SQLCipher; SQLite and
  LibTomCrypt inside SQLCipher are public domain).
- **Rationale**: no new dependency or plugin (principle III/VI); texts work offline.
- **Alternatives considered**: AboutLibraries or Google's oss-licenses plugin - a new
  dependency for one screen. Keep the list in sync by hand when a runtime dependency is
  added (noted in `AGENTS.md`).

## R10. Archive history for past days

- **Decision**: store only `archivedAt` (null when active). A habit is active on date D if it
  was created before D ended and (`archivedAt` is null or `archivedAt` is after D started).
- **Rationale**: enough for the 7-day strip; a full archive-period table is speculative
  (principle VI). The spec's assumption was updated to say so.
