package com.kernelbreach.core.database.repo

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** How the app chooses light/dark. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** All user settings, backed by DataStore. */
data class Settings(
    val onboardingDone: Boolean = false,
    val chosenGoal: String? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val largerText: Boolean = false,
    val reduceMotion: Boolean = false,
    val readAloud: Boolean = false,
    val dailyReminder: Boolean = false,
    val placementPassed: Boolean = false,
)

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    val settings: Flow<Settings> = dataStore.data.map { it.toSettings() }

    suspend fun setOnboardingDone(done: Boolean) = put { it[Keys.onboardingDone] = done }
    suspend fun setChosenGoal(goal: String?) = put {
        if (goal == null) it.remove(Keys.chosenGoal) else it[Keys.chosenGoal] = goal
    }
    suspend fun setThemeMode(mode: ThemeMode) = put { it[Keys.themeMode] = mode.name }
    suspend fun setLargerText(value: Boolean) = put { it[Keys.largerText] = value }
    suspend fun setReduceMotion(value: Boolean) = put { it[Keys.reduceMotion] = value }
    suspend fun setReadAloud(value: Boolean) = put { it[Keys.readAloud] = value }
    suspend fun setDailyReminder(value: Boolean) = put { it[Keys.dailyReminder] = value }
    suspend fun setPlacementPassed(value: Boolean) = put { it[Keys.placementPassed] = value }

    private suspend fun put(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        dataStore.edit(block)
    }

    private fun Preferences.toSettings() = Settings(
        onboardingDone = this[Keys.onboardingDone] ?: false,
        chosenGoal = this[Keys.chosenGoal],
        themeMode = this[Keys.themeMode]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: ThemeMode.SYSTEM,
        largerText = this[Keys.largerText] ?: false,
        reduceMotion = this[Keys.reduceMotion] ?: false,
        readAloud = this[Keys.readAloud] ?: false,
        dailyReminder = this[Keys.dailyReminder] ?: false,
        placementPassed = this[Keys.placementPassed] ?: false,
    )

    private object Keys {
        val onboardingDone = booleanPreferencesKey("onboarding_done")
        val chosenGoal = stringPreferencesKey("chosen_goal")
        val themeMode = stringPreferencesKey("theme_mode")
        val largerText = booleanPreferencesKey("larger_text")
        val reduceMotion = booleanPreferencesKey("reduce_motion")
        val readAloud = booleanPreferencesKey("read_aloud")
        val dailyReminder = booleanPreferencesKey("daily_reminder")
        val placementPassed = booleanPreferencesKey("placement_passed")
    }
}
