package com.xiaoguangdong.orbit.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.xiaoguangdong.orbit.domain.model.OrbitSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "orbit_settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val CHECK_IN_SOUND = booleanPreferencesKey("check_in_sound")
        val SORT_INCOMPLETE_FIRST = booleanPreferencesKey("sort_incomplete_first")
        val SHOW_IN_SEVEN_DAY_STRIP = booleanPreferencesKey("show_in_seven_day_strip")
        val INCLUDE_IN_BADGE = booleanPreferencesKey("include_in_badge")
        val HIDE_COMPLETED = booleanPreferencesKey("hide_completed")
        val COMPACT_VIEW = booleanPreferencesKey("compact_view")
        val WEEK_STARTS_MONDAY = booleanPreferencesKey("week_starts_monday")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    }

    val settings: Flow<OrbitSettings> = context.settingsDataStore.data.map { prefs ->
        OrbitSettings(
            checkInSound = prefs[Keys.CHECK_IN_SOUND] ?: true,
            sortIncompleteFirst = prefs[Keys.SORT_INCOMPLETE_FIRST] ?: true,
            showInSevenDayStrip = prefs[Keys.SHOW_IN_SEVEN_DAY_STRIP] ?: true,
            includeInBadgeCount = prefs[Keys.INCLUDE_IN_BADGE] ?: true,
            hideCompleted = prefs[Keys.HIDE_COMPLETED] ?: false,
            compactView = prefs[Keys.COMPACT_VIEW] ?: false,
            weekStartsOnMonday = prefs[Keys.WEEK_STARTS_MONDAY] ?: true,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
        )
    }

    suspend fun updateSettings(transform: OrbitSettings.() -> OrbitSettings) {
        val settingsValue = settings.first().transform()
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.CHECK_IN_SOUND] = settingsValue.checkInSound
            prefs[Keys.SORT_INCOMPLETE_FIRST] = settingsValue.sortIncompleteFirst
            prefs[Keys.SHOW_IN_SEVEN_DAY_STRIP] = settingsValue.showInSevenDayStrip
            prefs[Keys.INCLUDE_IN_BADGE] = settingsValue.includeInBadgeCount
            prefs[Keys.HIDE_COMPLETED] = settingsValue.hideCompleted
            prefs[Keys.COMPACT_VIEW] = settingsValue.compactView
            prefs[Keys.WEEK_STARTS_MONDAY] = settingsValue.weekStartsOnMonday
            prefs[Keys.NOTIFICATIONS_ENABLED] = settingsValue.notificationsEnabled
        }
    }
}
