# LemonFlow Constitution

LemonFlow is an offline, encrypted Android habit tracker that adaptively compresses the day's
plan down to a rescue minimum during force majeure and fatigue, protecting streaks and freeing
the user from guilt.

## Core Principles

### I. Offline-First, Sync-Ready

- The app MUST work fully without a network.
- The manifest MUST NOT declare the `INTERNET` permission until a sync feature is approved
  through its own spec.
- Every persisted entity MUST have a client-generated UUID primary key; auto-increment IDs are
  forbidden.
- Every timestamp MUST be stored as a UTC `Instant`. Where the day boundary matters, the local
  date and the time zone MUST be stored as separate fields.

Rationale: the product promise is autonomy, and stable IDs plus unambiguous time are what make a
later end-to-end encrypted sync between devices possible without a data migration.

### II. Encrypted and Private by Default

- All user data MUST live in the SQLCipher-encrypted Room database. The database key MUST be
  random, generated on the device and wrapped by an Android Keystore key.
- User data MUST NOT appear in plaintext in DataStore, SharedPreferences, files, logs or crash
  output.
- `allowBackup` MUST be `false`, so OS cloud backup is off.
- Data MUST leave the device only through a user-initiated export via the Storage Access
  Framework, and that export MUST be encrypted with a user passphrase.
- Analytics, telemetry and crash-reporting SDKs are forbidden.

Rationale: habits, energy levels and bad days are intimate data; the user must be able to trust
that nothing leaves the phone unless they explicitly take it out.

### III. FOSS-Only Dependencies

- Every dependency MUST have an OSI-approved license compatible with GPL-3.0-or-later.
- Google Play Services, Firebase and proprietary SDKs are forbidden.
- Each new dependency MUST be listed with its license in the feature's `plan.md`.
- Store-specific behaviour MUST sit behind an interface in `domain`, so a `foss` build never
  needs proprietary code.

Rationale: the app is licensed GPL-3.0-or-later and must stay publishable on F-Droid as well as
Google Play.

### IV. Test-First Domain Logic (NON-NEGOTIABLE)

- Every `tasks.md` MUST include test tasks for each user story, written and failing before the
  implementation tasks.
- The `domain` package MUST be pure Kotlin with no `android.*` imports, and MUST get time only
  through an injected `Clock`.
- Plan compression and streak rules MUST be covered by JVM unit tests, including day-boundary
  and time-zone edge cases.

Rationale: plan compression and streak protection are the core of the product; a bug there
silently breaks the user's trust, and time-dependent logic is only reliable when it is tested
deterministically.

### V. Compassionate by Design

- A forced or low-energy day MUST NOT cost the user a streak if they complete the minimum plan.
- Every feature spec MUST include an acceptance scenario for the "minimum plan" / low-energy
  case.
- User-facing copy MUST NOT blame or shame: no "failed", "broken" or "missed" wording toward the
  user, and no guilt-inducing notifications.

Rationale: freeing the user from guilt is the product's purpose, not a styling choice; a feature
that punishes a bad day contradicts it.

### VI. Simplicity First

- The project MUST stay one Gradle module `:app`, with layers in packages:
  `ui -> domain <- data`. `data` and `ui` depend on `domain`, never the other way.
- New Gradle modules, product flavors or abstraction layers MUST NOT be added without a concrete
  need stated in `plan.md`.

Rationale: a single-developer app gains nothing from speculative structure; boundaries are added
when a real need appears, not in advance.

### VII. Localized and Accessible

- Every user-facing string MUST live in resources and MUST be added to `values/` (en),
  `values-ru/` and `values-uk/` in the same change.
- Interactive elements MUST have a content description or label and a touch target of at least
  48dp.

Rationale: a supportive app has to be readable and usable by everyone it is built for, from the
first release, not after a later translation pass.

## Governance

- This constitution supersedes other practices where they conflict. `AGENTS.md` is the runtime
  guidance file for commands, workflow and boundaries; the constitution holds principles only and
  does not repeat them.
- Every `plan.md` MUST pass a Constitution Check against these principles; any deviation MUST be
  recorded with its justification in the plan's Complexity Tracking.
- Amendments are made only through `speckit-constitution`, at the owner's request, and are
  versioned semantically: MAJOR for removing or redefining a principle, MINOR for adding a
  principle or materially expanding one, PATCH for clarifications and wording.

**Version**: 0.1.0 | **Ratified**: 2026-11-08 | **Last Amended**: 2026-11-08
