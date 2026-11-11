# Quickstart: validating Habits and Today

## Prerequisites

- JDK 21, Android SDK with `platforms/android-37.0` (see `AGENTS.md`).
- For the device checks: an Android 14+ device or emulator with `adb` access; the debug build
  (`run-as` needs a debuggable app).

## 1. Automated checks (no device)

```sh
./gradlew :app:testDebugUnitTest   # domain rules, DAOs (Robolectric), screens
./gradlew check assembleDebug      # full check, same as CI
```

Expected: all green. The domain tests cover the day boundary (01:30 with day end 03:00), the
Europe/Kyiv DST days with day end 03:00, mark derivation for all four types, the avoid default
rule, timer settling at day end and validation (see `data-model.md`, "Derived rules").

## 2. Install and run

```sh
./gradlew :app:installDebug && adb shell am start -n io.github.vlukyanets.lemonflow/.MainActivity
```

## 3. Manual scenarios (map to spec user stories)

| # | Steps | Expected |
|---|-------|----------|
| 1 | Fresh install, open the app | Today shows the empty state "Add first habit"; no passphrase or account prompt (US4-3) |
| 2 | Create Reading (Check, 30 min / 1 page, core), Water (Count, 2500/500 ml, step 250), Meditation (Timer, 10/1 min), Added sugar (Avoid, limit 0 / 1) | All four on Today, Reading and core habits first (US1, FR-006). Time the Reading habit with a stopwatch from tapping "Add habit" to seeing it in the list: under 30 s (SC-001) |
| 3 | Count: Baseline 3000 > Target 2500 | Save disabled, gentle hint under Minimum (US1-1a) |
| 4 | Water +250 twice | "500 of 2500 ml", `minimum` tag; keep adding to 2500 -> done (US2-10) |
| 5 | Start Meditation, close the app, reopen after 2 min | Timer still running, ~2:00, `minimum` tag; start another timer -> both run (US2-10a) |
| 6 | Added sugar: +1, then +1 | done -> `minimum` -> "2 times today", grey badge, no warning (US2-10b) |
| 7 | Switch Hard day on | One tap; goals change to Baselines with no visible delay (under 1 s, SC-002); Reading at minimum shows as done; switch off keeps marks (US3) |
| 7a | After 2-7 (a timer still running, hard day on): `adb shell am force-stop io.github.vlukyanets.lemonflow`, then open the app again | Habits, marks, amounts, the running timer (time kept growing), the hard-day switch and the "Day ends at" value are all unchanged (SC-004) |
| 8 | Settings -> Day ends at -> 05:00 | Today shows "Day closes at 05:00"; earlier marks keep their dates (US2-12) |
| 9 | Tap an earlier day in the strip | Read-only sheet; a day with no records says "Nothing recorded" (US2-8/9) |
| 10 | Archive a habit, open Archive, Restore; then Delete permanently with confirmation | Archive keeps history; delete removes it (US1-3..5) |
| 11 | Settings -> Language -> Русский, then Українська, then System default | The app switches at once; all screens translated, plurals correct, nothing clipped (SC-007, FR-022). Force-stop and reopen: the chosen language is kept; the device language is unchanged |
| 12 | Airplane mode, repeat 2-10 | Everything works (SC-006) |

## 4. Encryption check on the device (SC-005)

Create a habit named `Therapy`, then:

Recent writes live in the write-ahead log, not in `lemonflow.db` (which can be a single
4 KB page), so check all three files:

```sh
for f in lemonflow.db lemonflow.db-wal lemonflow.db-shm; do
  adb exec-out run-as io.github.vlukyanets.lemonflow cat databases/$f | strings | grep -c Therapy
done
adb exec-out run-as io.github.vlukyanets.lemonflow cat databases/lemonflow.db | head -c 16 | xxd
adb exec-out run-as io.github.vlukyanets.lemonflow ls no_backup
```

Expected: every count is `0`; the first 16 bytes are random, not `SQLite format 3`;
`no_backup` contains `db_key.bin` (60 bytes: the wrapped key, not the passphrase).
`aapt2 dump permissions` of the APK lists no `android.permission.INTERNET`.

## 4a. Key survives a lock-screen change (FR-013a)

Create a habit, change the device's screen lock (PIN -> new PIN, or add/remove a
fingerprint), reboot the device and open the app. Expected: the habit and its marks are
there; the "Your data can't be opened" screen does not appear.

## 5. Data-unavailable path (FR-015)

```sh
adb shell run-as io.github.vlukyanets.lemonflow rm no_backup/db_key.bin
adb shell am force-stop io.github.vlukyanets.lemonflow
```

Reopen the app: the calm "Your data can't be opened" screen appears, with no crash and no
raw error text. Leaving and reopening shows it again; "Start fresh" -> confirm -> empty Today.
