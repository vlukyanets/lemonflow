package io.github.vlukyanets.lemonflow.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object TodayRoute

@Serializable
object HabitsRoute

@Serializable
object InsightsRoute

@Serializable
object SettingsRoute

@Serializable
object ArchiveRoute

/** Third-party licenses; part of the Settings tab. */
@Serializable
object LicensesRoute

/** One license text by [LicenseText.id][io.github.vlukyanets.lemonflow.ui.about.LicenseText]. */
@Serializable
data class LicenseRoute(val id: String)

/** `habitId == null` creates a new habit. */
@Serializable
data class HabitEditorRoute(val habitId: String? = null)
