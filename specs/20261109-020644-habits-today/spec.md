# Feature Specification: Habits and Today

**Feature Branch**: `worktree-habits-today`

**Created**: 2026-11-09

**Status**: Draft

**Input**: User description: "Habits and today: create/edit/delete a habit with name, Target, Baseline and isCore; a Today screen listing habits with completion at Target or Baseline; a manual "hard day" toggle that switches today's plan to Baseline and counts Baseline as complete; per-day DailyHabitInstance records; all data in a SQLCipher-encrypted Room database with a Keystore-wrapped key."

## Clarifications

### Session 2026-11-09

- Q: How are Target and Baseline entered? -> A: A positive whole number plus a unit (minutes,
  times, pages, steps, or a custom unit); Target and Baseline may use different units.
- Q: What happens to history when a habit is removed? -> A: Archive is the primary action and
  keeps history; "delete permanently" is a separate, confirmed action that removes the habit and
  its history.
- Q: Does a Baseline mark count on a normal day? -> A: Yes, always; the record keeps whether it
  was Target or Baseline, so Baseline days stay distinguishable for future features.
- Q: Should opening the app require a lock (PIN, biometrics or device unlock)? -> A: No, not
  in this feature; the data key is not bound to user authentication, and an app lock is a
  separate future feature.
- Q: Can past days be corrected in this feature (the design's "Day correction" sheet)? -> A:
  No; only today can be marked. The Today screen shows a strip of the last 7 days, and past
  days open read-only. Day correction, streaks and freezes come together in a later feature.
- Q: When does "today" end? -> A: At a user setting "Day ends at", default 03:00 local time,
  included in this feature.
- Q: Can partial progress be recorded during the day? -> A: Yes; marks stay the main action,
  and the user can also add amounts in steps; the habit is marked automatically when the
  amount reaches Baseline or Target.
- Q: What happens when the encrypted data cannot be opened (e.g. the protected key is gone)?
  -> A: A calm explanation and a "Start fresh" action that, after confirmation, erases the
  unreadable data and creates a new empty store.
- Q: Does every habit track an amount, or are there habit types? -> A: Four types in this
  feature: Check, Count, Timer and Avoid (supersedes the earlier "every habit has a step"
  answer; different units for Target and Baseline remain only for Check).
- Q: How does an Avoid habit work? -> A: A limit with a counter: Target is the good-day limit
  (often 0), Baseline the hard-day limit; the user adds 1 when it happens; the day counts as
  done while nothing has been added.
- Q: How many timers can run at once? -> A: Several in parallel, one per habit.
- Q: Does an Avoid habit count as done on a past day the app was not used? -> A: No; it counts
  as done by default only today and on days with at least one other record. A day with no
  records at all is "nothing recorded" for every habit.
- Q: Is the habit detail screen from the design in scope? -> A: No; opening a habit opens its
  edit form. The detail screen comes with streaks and Insights.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Define habits with a norm and a rescue minimum (Priority: P1)

The user adds the habits they want to keep. For each habit they give a name, a Target (the
amount for a good day, e.g. "45 minutes of reading") and a Baseline (the rescue minimum for a
hard day, e.g. "1 page"). They can mark a habit as core - an anchor of their day, such as sleep
or medication - and later edit or delete any habit.

**Why this priority**: every other part of the app works on habits; without them there is
nothing to plan, mark or protect. Asking for the Baseline up front is what makes a hard day
survivable later.

**Independent Test**: can be fully tested by creating, editing and deleting habits, closing and
reopening the app, and checking the list matches what was entered.

**Acceptance Scenarios**:

1. **Given** no habits exist, **When** the user creates a Check habit "Reading" with Target 45
   minutes, Baseline 1 page and core off, **Then** the habit appears in the habit list with
   those values.
1a. **Given** the user creates a Count habit, **When** they enter Baseline 3000 ml and Target
   2000 ml, **Then** it cannot be saved and the Baseline field gets a gentle hint.
2. **Given** a habit exists, **When** the user changes its Baseline and marks it core, **Then**
   the habit shows the new Baseline and a core indicator.
3. **Given** a habit with marks on earlier days exists, **When** the user archives it, **Then**
   it no longer appears in the habit list or on the Today screen, it is listed in the archive,
   and its earlier marks are kept.
4. **Given** a habit is archived, **When** the user restores it, **Then** it is back in the habit
   list and in today's plan as open.
5. **Given** a habit exists (active or archived), **When** the user chooses "delete permanently"
   and confirms the warning that its history will be removed, **Then** the habit and all its
   marks are gone.
6. **Given** the user is creating a habit, **When** they leave the name, Target or Baseline empty,
   **Then** the habit cannot be saved and the empty field is pointed out without blaming wording.
7. **Given** habits were created, **When** the app is closed and reopened, **Then** all habits are
   still there unchanged.
8. **Given** the user has little energy, **When** they create a habit, **Then** they can save it
   with only a name, Target and Baseline - core is optional and off by default.

---

### User Story 2 - See today's plan and mark progress (Priority: P2)

The user opens the app and sees today's plan: every habit with its Target. For each habit they
can record that they did it at Target or did the Baseline, and undo a mark made by mistake: a
Count, Timer or Avoid habit is marked by its amount, a Check habit by one check that marks the
day's goal (the Baseline once hard day is on, FR-007). Core habits are listed first.

**Why this priority**: this is the daily loop - the reason to open the app. It depends on habits
existing (Story 1).

**Independent Test**: with a few habits created, mark one at Target and one at Baseline, restart
the app on the same day and check the marks are kept; on the next day check the plan
starts fresh while the previous day's marks are still stored.

**Acceptance Scenarios**:

1. **Given** three habits exist, one of them core, **When** the user opens the Today screen,
   **Then** all three are listed with their Target amounts and the core habit is listed first.
2. **Given** a habit is open for today, **When** the user marks it done at Target, **Then** the
   habit shows as done for today.
3. **Given** a normal day and a habit open for today, **When** it is marked done at Baseline
   (a Count, Timer or Avoid habit reaching its Baseline amount; a Check habit marked while hard
   day was on, after hard day is switched off), **Then** the habit counts as complete for today
   and shows as "minimum done", distinct from Target completion.
4. **Given** a habit was marked by mistake, **When** the user undoes the mark, **Then** the habit
   is open again for today.
5. **Given** habits were marked yesterday, **When** the user opens the app today, **Then** today's
   plan starts with every habit open and yesterday's marks remain stored for that date.
6. **Given** the user has little energy, **When** they open the Today screen, **Then** each habit's
   Baseline is visible, and with the hard day on a Check habit is marked at Baseline in a single
   tap.
7. **Given** habits were marked on earlier days of the week, **When** the user opens the Today
   screen, **Then** a strip of the last 7 days shows each day's share of completed habits, with
   today highlighted.
8. **Given** the week strip is shown, **When** the user opens an earlier day, **Then** that day's
   habits and marks are shown read-only, with no way to change them.
9. **Given** an earlier day has no marks, **When** it is shown in the strip or opened, **Then** it
   reads as "nothing recorded" in neutral wording, never as a failure.
10. **Given** a Count habit "Water" with Target 2000 ml, Baseline 500 ml and step 250, **When**
    the user adds a step twice, **Then** the progress shows 500 of 2000 ml and the habit is
    marked "minimum done" automatically; **When** they keep adding up to 2000 ml, **Then** it is
    marked done at Target.
10a. **Given** a Timer habit "Meditation" with Target 10 min and Baseline 1 min, **When** the
    user starts its timer, closes the app and returns 2 minutes later, **Then** the timer is
    still running, shows about 2 minutes and the habit is "minimum done"; **When** they also
    start the "Reading" timer, **Then** both run at the same time.
10b. **Given** an Avoid habit "Added sugar" with Target 0 times and Baseline 1 time, **When** the
    user opens Today and has added nothing, **Then** the habit shows as done; **When** they add
    1, **Then** it shows "minimum done"; **When** they add another, **Then** it shows the count
    "2 times today" in neutral wording, with no verdict.
11. **Given** "Day ends at" is 03:00 and it is 01:30 on Tuesday, **When** the user opens the Today
    screen, **Then** it still shows Monday, and a mark made now belongs to Monday.
12. **Given** the user changes "Day ends at" to 05:00 in settings, **When** they return to the
    Today screen, **Then** it shows the new closing time and earlier marks stay on their dates.

---

### User Story 3 - Declare a hard day (Priority: P3)

On a day of illness, fatigue or force majeure the user switches on "hard day". Today's plan then
shows each habit's Baseline instead of its Target, and doing the Baseline counts as the habit
being complete for the day. The user can switch it back off the same day.

**Why this priority**: this is the product's core promise - a bad day is compressed to a rescue
minimum instead of being lost. It builds on the daily loop (Story 2).

**Independent Test**: with habits created, switch hard day on, mark habits at Baseline, and check
they show as complete; restart the app the same day and check hard day and the marks are kept;
on the next day check hard day is off.

**Acceptance Scenarios**:

1. **Given** today is a normal day, **When** the user switches hard day on, **Then** today's plan
   shows each habit's Baseline as the goal.
2. **Given** hard day is on, **When** the user marks a habit done at Baseline, **Then** the habit
   shows as complete for today, the same as Target completion on a normal day.
3. **Given** hard day is on and some habits are marked, **When** the user switches hard day off,
   **Then** existing marks are kept and the plan shows Targets again.
4. **Given** hard day was on yesterday, **When** the user opens the app today, **Then** today is a
   normal day unless they switch hard day on again.
5. **Given** hard day is on, **When** the user reads any text on the Today screen, **Then** no
   wording blames, shames or warns them about the reduced plan.

---

### User Story 4 - Data stays private and encrypted (Priority: P1, cross-cutting)

Everything the user enters - habits and daily marks - is stored only on the device, encrypted,
and never leaves it. The user does not set up anything for this to happen.

**Why this priority**: habits and bad days are intimate data; the constitution makes encryption
mandatory from the first stored record, so it ships with Story 1.

**Independent Test**: create habits and marks, then inspect the app's storage on the device and
confirm no habit name or mark is readable without the app; turn off all networks and confirm
every flow above still works.

**Acceptance Scenarios**:

1. **Given** the user created a habit named "Therapy", **When** the app's stored files are
   inspected outside the app, **Then** the word "Therapy" cannot be found in readable form.
2. **Given** the device has no network, **When** the user uses every flow in this feature,
   **Then** each one works.
3. **Given** the first launch, **When** the user creates their first habit, **Then** no
   passphrase, account or setup step is asked of them.

### Edge Cases

- The app is open across the day-end time: the Today screen moves to the new date the next
  time it is shown or refreshed; marks made before the day-end time stay on the earlier date.
- A mark is made at 01:30 with day end at 03:00: it belongs to the previous calendar date.
- The day-end time falls into a daylight-saving gap (the clock skips that hour): the day ends
  at the first moment after the gap; when the hour repeats, the day ends at its first
  occurrence.
- The user changes "Day ends at": existing marks keep their dates; only the current and
  future days follow the new time. If the change moves "now" into a different date, the
  Today screen switches to that date.
- The device time zone changes (travel): a mark belongs to the user date on which it was
  made, computed in the zone in effect at that moment;
  the instant and the time zone are stored so the date can be recomputed later.
- A habit is created in the middle of the day: it appears in today's plan as open.
- A habit is edited after being marked today: today's mark is kept; the plan shows the new
  values.
- A habit is archived on a day it was already marked: today's mark is kept in its history; the
  habit leaves today's plan.
- A habit is archived and restored on the same day: it returns with today's mark, if any.
- The stored data cannot be opened (e.g. the device's protected key was invalidated by a
  security reset): see FR-015. If the user leaves the explanation without confirming, it is
  shown again on the next launch.
- A timer is still running when the day ends: it stops at the day-end moment, its time stays
  on the day it was started, and the new day starts with that habit at 0.
- The device clock or time zone changes while a timer runs: elapsed time is measured from the
  stored start instant, never negative; a jump backwards adds no time.
- An Avoid habit on a past day without its own record: it counts as done at Target if the user
  recorded any other habit that day; if the day has no records at all, the whole day, Avoid
  included, reads "nothing recorded" (FR-008).
- The user lowers a Count, Timer or Avoid amount: the mark follows the new amount (e.g. back to
  open below Baseline).
- Very long habit names or amounts: the text wraps or truncates without breaking the layout.
- No habits exist yet: the Today screen invites the user to add their first habit.

## Requirements *(mandatory)*

### Functional Requirements

"Date", "day" and "user date" below mean the user's day: it runs from the "Day ends at"
time on one calendar date to the same time on the next, in local time, and is named by the
calendar date it starts on (FR-018).

- **FR-001**: Users MUST be able to create a habit with a name, a type, a Target, a Baseline and
  a core flag (off by default). The type is one of:
  - **Check** - done or not; Target and Baseline describe what counts (e.g. "30 minutes" /
    "1 page").
  - **Count** - an amount added in steps (e.g. water: Target 2500 ml, Baseline 500 ml).
  - **Timer** - minutes measured with a start/stop timer (e.g. meditation: Target 10 min,
    Baseline 1 min).
  - **Avoid** - something to keep within a limit (e.g. added sugar: Target 0 times, Baseline at
    most 1 time).
  The default type is Check.
- **FR-002**: Target and Baseline MUST each be a whole number plus a unit chosen from minutes,
  times, pages, steps or a custom unit the user names, with these rules per type:
  - Check: both positive; they MAY use different units (e.g. Target 45 minutes, Baseline 1 page).
  - Count: both positive, same unit, Baseline not greater than Target.
  - Timer: both positive, unit always minutes, Baseline not greater than Target.
  - Avoid: same unit; Target is the limit for a good day and MAY be 0; Baseline is the limit for
    a hard day and MUST NOT be smaller than Target.
- **FR-002a**: A habit's type MUST NOT change after it is created; the edit form shows it
  read-only.
- **FR-002b**: A habit name MUST be 1 to 120 characters after trimming spaces, and a custom
  unit name 1 to 30 characters. Input is never cut silently: above the limit the field shows
  a gentle hint and Save stays disabled. On Today a name keeps one line and a long one ends
  with an ellipsis, so a row never grows (decided during the device review); the habit list
  allows two lines, and the editor shows the full name.
- **FR-003**: Users MUST be able to edit every other field of an existing habit.
- **FR-004**: Users MUST be able to archive a habit as the primary removal action: an archived
  habit disappears from the habit list and from Today's plan from that day on, and its past
  daily records are kept.
- **FR-004a**: Users MUST be able to see archived habits, restore one (it returns to the habit
  list and to Today's plan from that day on), or delete one permanently.
- **FR-004b**: Permanent deletion MUST be a separate action, available from the habit list and
  from the archive, behind a confirmation step that states the habit's history will be removed;
  it MUST remove the habit and all its daily records.
- **FR-005**: The system MUST refuse to save a habit with an empty name, Target or Baseline, or
  with values that break FR-002, and MUST point to the field with a gentle hint (no blaming
  wording).
- **FR-006**: The Today screen MUST list every active (not archived) habit for the current user
  date, core habits first, then the rest in the order they were created.
- **FR-006a**: The Today screen MUST show a strip of the last 7 user dates ending with today.
  Each date shows the share of habits completed (at Target or Baseline) out of the habits that
  were active on that date; today is highlighted.
- **FR-006b**: Users MUST be able to open any earlier date in the strip to see its habits and
  marks read-only; marks on earlier dates MUST NOT be changeable in this feature. A date with no
  marks MUST be presented in neutral wording ("nothing recorded"), not as a shortfall.
- **FR-007**: Check habits have a single check on the Today screen (no separate "minimum"
  button - it confused users on device). One tap on an open habit marks it done at the day's
  goal: Target on a normal day, Baseline when the hard day is on. On a normal day a tap on a
  habit marked at Baseline raises it to Target; any other tap on a marked habit undoes it.
  At most one mark per habit per day is kept.
- **FR-007a**: Count and Timer habits MUST have a step - the amount one "+" or "-" action adds
  or removes (Count: in the habit's unit, default 1, e.g. 250 ml; Timer: in minutes, default
  1). Avoid habits always change by 1.
- **FR-007b**: Count habits: users MUST be able to add one step to today's amount and take one
  step back off (never below 0). The Today screen MUST show today's amount against the current
  goal (Target, or Baseline on a hard day) as a progress indicator. The mark follows the amount:
  done at Target once it reaches Target, done at Baseline once it reaches Baseline, open below
  that - there is no separate manual mark.
- **FR-007c**: Timer habits: users MUST be able to start and stop a timer for the habit from the
  Today screen; while it runs, today's time grows live and the progress indicator updates. The
  user MAY also add or remove time in steps (e.g. when they forgot to start the timer), never
  below 0. The mark follows today's total time exactly as for Count habits.
- **FR-007d**: Several timers MAY run at the same time, one per habit. A running timer MUST
  keep counting while the app is closed or the device restarts, and MUST stop by itself at the
  end of the day it was started on; the time up to that moment belongs to that day.
- **FR-007e**: Avoid habits: users MUST be able to add 1 each time the thing happens and take 1
  back (never below 0). The mark follows today's count: done at Target while the count is
  within the Target limit (including when nothing has been added), done at Baseline while it is
  within the Baseline limit, and open above that. Above the limit, the screen shows only the
  count, in neutral wording, with no verdict.
- **FR-008**: The system MUST keep one daily record per habit per user date holding the mark
  (open, done at Target, done at Baseline), the amount recorded that day (Count: amount; Timer:
  accumulated time; Avoid: count), for a Timer the instant its current run started (if
  running), the instant of the last change and the time zone in effect. An Avoid habit with no
  record counts as done at Target for today, and for an earlier date only if that date has at
  least one record of another habit; on a date with no records at all, every habit, Avoid
  included, is "nothing recorded".
- **FR-009**: A Baseline mark MUST always count as the habit being complete for the day, on a
  normal day as well as on a hard day. It MUST be shown as "minimum done" - visibly different
  from Target completion, without wording that suggests a shortfall - and the daily record MUST
  keep which of the two it was.
- **FR-010**: Users MUST be able to switch hard day on and off for the current user date; the
  setting is stored per date and defaults to off on every new day.
- **FR-011**: While hard day is on, the Today screen MUST show each habit's Baseline as the goal
  (for Count and Timer the progress indicator fills towards Baseline; for Avoid the Baseline
  limit applies) and MUST present a Baseline mark as the plan being fully done, not as
  "minimum done".
- **FR-012**: Switching hard day on or off MUST NOT change or remove existing marks.
- **FR-013**: All habits, daily records and hard-day settings MUST be stored only on the device,
  encrypted, with a key generated on the device and protected by the device's secure key store;
  no part of this feature may use the network.
- **FR-013a**: Opening the app MUST NOT require any authentication, and the data key MUST NOT
  be bound to user authentication, so changing the device's PIN, pattern or biometrics never
  makes the data unreadable.
- **FR-014**: The system MUST NOT write habit names, amounts or marks to logs, crash output or any
  unencrypted storage.
- **FR-015**: If the encrypted data cannot be opened, the system MUST NOT crash and MUST show a
  calm explanation (no raw error text) with a single "Start fresh" action. After a confirmation
  that states the existing data cannot be recovered, it MUST erase the unreadable data and the
  old key and start with a new empty store; until the user confirms, nothing is erased.
- **FR-016**: All user-facing text MUST be available in English, Russian and Ukrainian, and MUST
  NOT use blaming or shaming wording ("failed", "missed", "broken" or equivalents).
- **FR-017**: Every interactive element MUST have an accessible label and a touch target of at
  least 48dp.
- **FR-018**: Users MUST be able to set "Day ends at" in settings to any whole hour from 00:00
  to 06:00 local time; the default is 03:00. The Today screen MUST show when the current day
  ends (e.g. "Day closes at 03:00").
- **FR-019**: Changing "Day ends at" MUST NOT move existing marks or hard-day settings to other
  dates.
- **FR-020**: The main navigation MUST offer Today, Habits, Insights and Settings. Insights
  MUST show a neutral "coming soon" placeholder in this feature.
- **FR-021**: Opening a habit from the Today screen or the habit list MUST open its edit form.
- **FR-022**: Users MUST be able to choose the app language in settings: "System default",
  English, Russian or Ukrainian. Each language is listed by its own name (English, Русский,
  Українська). The choice applies to the whole app at once, survives a restart and does not
  change the device language; "System default" follows the device language.
- **FR-023**: Settings MUST end with an "About" section showing the app name, its version
  and the author credit "Made by Valentyn Lukianets" (passport spelling; the name is not translated).
- **FR-024**: The "About" section MUST also offer: the app's license (GNU GPL v3.0 or later)
  with its full text readable offline; a "Source code" link
  (https://github.com/vlukyanets/lemonflow) that opens in the user's browser; and an "Open
  source licenses" screen listing every bundled third-party component with its license,
  each license text readable offline. The app still requests no INTERNET permission.

### Key Entities

- **Habit**: a habit the user wants to keep. Attributes: unique identifier generated on the
  device, name, type (Check, Count, Timer, Avoid; fixed at creation), Target (amount and unit),
  Baseline (amount and unit), step (Count and Timer), core flag, archived flag with the instant
  it was archived, creation instant.
- **Daily habit record** (DailyHabitInstance): one habit on one user date. Attributes: unique
  identifier generated on the device, the habit it belongs to, the user date, the mark (open,
  done at Target, done at Baseline), the amount recorded that day (amount, accumulated time or
  count, by type), the start instant of a running timer, the instant of the last change and
  the time zone in effect.
- **Settings**: user preferences. Stored in the encrypted database: "Day ends at" (a whole
  hour, default 03:00). The app language (FR-022) is kept by Android's per-app language
  setting, not in the database (research R4). The About section (FR-023, FR-024) is
  read-only and stores nothing.
- **Day**: the user's settings for one user date. Attributes: user date, hard-day flag, the
  instant it was last changed and the time zone in effect.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can create a habit with name, Target and Baseline in under 30 seconds.
- **SC-002**: Switching hard day on takes one action and the plan shows Baselines within 1
  second.
- **SC-003**: Every Today-screen action (mark a Check habit, add a step, start or stop a timer,
  add 1 to an Avoid habit) takes one tap, with no extra screens.
- **SC-004**: 100% of habits, marks and hard-day settings survive closing and reopening the app.
- **SC-005**: Inspecting the app's storage outside the app reveals none of the user's habit names
  or marks in readable form.
- **SC-006**: Every flow in this feature works with all networks disabled.
- **SC-007**: Every screen in this feature is fully translated in English, Russian and Ukrainian,
  with no blaming wording in any language.

## Assumptions

- Every habit is planned for every day; schedules (days of week, N times per week) are out of
  scope.
- Running timers show no system notification in this feature (notifications need a separate
  permission and spec); the time is computed from the stored start instant.
- The day boundary is the "Day ends at" setting (whole hours 00:00-06:00, default 03:00);
  minute precision and later hours are out of scope.
- Only today can be marked; past days are view-only. Correcting past days (statuses such as
  partial or excused, reasons, a lock window) is out of scope and comes with streaks and
  freezes in a later feature.
- A habit counts as active on a date if it was created before that day ended and is not
  archived, or was archived after that day started. Only the latest archive instant is kept,
  so earlier archive-and-restore periods are not reflected in past days of the week strip.
- Hard day is switched on manually; automatic compression (curfew hour, time left before sleep,
  dropping secondary habits), energy domains, context and attention modes are out of scope.
- The core flag is shown and sorts core habits first; it has no other effect in this feature
  (the future rescue engine will protect core habits).
- Streaks, Grace Shield and Ramp-up are out of scope; daily records are kept so a later feature
  can compute them.
- Reminders, notifications, export/import and per-day notes or links are out of scope.
- Insights (statistics) is out of scope; its tab exists only as a placeholder (FR-020).
- A per-habit detail screen (month calendar, done / minimum counts) is out of scope; it comes
  with streaks and Insights.
- Losing the device or clearing the app's data loses the data: there is no backup by design
  until encrypted export is specified.
- Single user per device; no accounts.
- An app lock (PIN, biometrics or device unlock to open the app) is out of scope; it is a
  separate future feature.
