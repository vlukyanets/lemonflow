# Contract: screens and navigation

Source of truth for layout and copy: the "LemonFlow" design canvas (pages Today, Habits,
Settings & Insights, System states, Localization). Screens below map 1:1 to its artboards.

## Routes

| Route | Kind | Screen | Spec |
|-------|------|--------|------|
| `Today` | top-level tab (start) | Today: hard-day switch, 7-day strip, habit rows by type | US2, US3, FR-006..012 |
| `Habits` | top-level tab | Active list + Archive entry | FR-001..004 |
| `Insights` | top-level tab | "Coming soon" placeholder | FR-020 |
| `Settings` | top-level tab | "Day ends at", "Language", then "About": app card (name, version, "Made by Valentyn Lukianets"), License, Source code, Open source licenses | FR-018, FR-022..024 |
| `HabitEditor(habitId: String?)` | pushed | New (type picker) / Edit (type read-only, Archive, Delete permanently) | FR-001..005, FR-021 |
| `Archive` | pushed (Habits tab) | Archived habits with Restore / Delete permanently (confirm dialog) | FR-004a/b |
| `Licenses` | pushed (Settings tab) | Bundled components with authors and license; public-domain items not pressable | FR-024 |
| `License(id)` | pushed (Settings tab) | Full license text, English original, reflowed paragraphs | FR-024 |
| `DataUnavailable` | replaces the whole graph | Calm explanation + "Start fresh" with confirmation | FR-015 |

Sheets (not routes): past day (from a strip cell, read-only), hour picker, language picker.

Navigation rules:
- Tapping a habit on Today or in Habits opens `HabitEditor(id)` (FR-021).
- Back from the editor returns to where it was opened from.
- Bottom-tab switches save and restore each tab's state.
- "Source code" opens https://github.com/vlukyanets/lemonflow in the browser; the app has no
  INTERNET permission.

## Bottom navigation (FR-020)

Decided during the device review (it replaces a full-width Material `NavigationBar`, whose
equal tab widths wrapped "Налаштування"):

- A floating pill, centered, 12dp from the screen sides and above the gesture bar; opaque
  `surface`, 1dp `line` border, light shadow. Hidden on the habit editor.
- It wraps its tabs: each tab is its label's width plus up to 14dp per side (side padding
  shrinks first); a tab is at least 56x52dp. Icon 22dp above a 13sp label.
- When the labels can't keep 6dp per side (narrow phone, large text), every tab shows its
  icon only (24dp), with the label as its spoken description.
- The selected tab has one `blueSoft` pill that slides and resizes to it; press ripples are
  clipped to the pill shape.
- Screens draw under the pill; scrolling screens end with `LocalBottomBarSpace` so the last
  item scrolls clear of it.

## Today row by type (all targets >= 48dp, FR-017)

| Type | Leading control | Meta line | Trailing controls |
|------|-----------------|-----------|-------------------|
| CHECK | one round check: a tap marks the day's goal (Target, or Baseline on a hard day), raises a normal-day Baseline mark to Target, else undoes (FR-007) | `Target 30 min · minimum 1 page` (hard day: `Goal today: 1 page`) | none |
| COUNT | progress ring, check when done | `1500 of 2500 ml · minimum 500 ml` | `-` `+` (step) |
| TIMER | ring with play/pause; a finished, paused timer is a filled check (a tap continues it) | `4:12 of 10 min · minimum 1 min`; running row tinted | `-` `+` (1 step, minutes) |
| AVOID | badge: check when within Target, else count | `1 time today · limit 0, hard day 1` (hard day: `limit 1`) | `-` `+1` |

"minimum" is always written in full: "min" / "мин" / "хв" mean minutes.

A BASELINE mark on a normal day ("minimum done", FR-009) shows a lifebuoy icon after the
habit name (spoken "minimum") and highlights the minimum part of the meta line in
`primary` (for Avoid, the count). On a hard day it looks fully done (FR-011). The name keeps
one line (ellipsis); a row never grows a line for the minimum state.

Header shows `Day closes at HH:00` and `N/M done`; with large text the count moves to its
own line rather than squeezing.

## Strings

All copy lives in `values/`, `values-ru/`, `values-uk/strings.xml`; counts use `<plurals>`
(ru/uk have one/few/many forms). Banned words in any locale: failed, missed, broken, streak
lost and their translations (FR-016).
