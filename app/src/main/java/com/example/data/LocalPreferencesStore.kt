package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sajids_zone_prefs")

class LocalPreferencesStore(private val context: Context) {

  private object Keys {
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val USE_24_HOUR = booleanPreferencesKey("use_24_hour")
    val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    val HEADS_UP_MINUTES = longPreferencesKey("heads_up_minutes")
    val OVERDUE_NUDGE_ENABLED = booleanPreferencesKey("overdue_nudge_enabled")
    val PIN_ENABLED = booleanPreferencesKey("pin_enabled")
    val PIN_HASH = stringPreferencesKey("pin_hash")
    val LOCK_JOURNAL_ONLY = booleanPreferencesKey("lock_journal_only")
  }

  val preferencesFlow: Flow<UserPreferences> =
    context.dataStore.data.map { prefs ->
      UserPreferences(
        themeMode = prefs[Keys.THEME_MODE] ?: ThemeMode.System.label,
        use24HourTime = prefs[Keys.USE_24_HOUR] ?: false,
        notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
        headsUpMinutes = prefs[Keys.HEADS_UP_MINUTES] ?: 15L,
        overdueNudgeEnabled = prefs[Keys.OVERDUE_NUDGE_ENABLED] ?: true,
        pinEnabled = prefs[Keys.PIN_ENABLED] ?: false,
        pinHash = prefs[Keys.PIN_HASH] ?: "",
        lockJournalOnly = prefs[Keys.LOCK_JOURNAL_ONLY] ?: true,
      )
    }

  suspend fun save(prefs: UserPreferences) {
    context.dataStore.edit { store ->
      store[Keys.THEME_MODE] = ThemeMode.from(prefs.themeMode).label
      store[Keys.USE_24_HOUR] = prefs.use24HourTime
      store[Keys.NOTIFICATIONS_ENABLED] = prefs.notificationsEnabled
      store[Keys.HEADS_UP_MINUTES] = prefs.headsUpMinutes.coerceIn(0L, 120L)
      store[Keys.OVERDUE_NUDGE_ENABLED] = prefs.overdueNudgeEnabled
      store[Keys.PIN_ENABLED] = prefs.pinEnabled
      store[Keys.PIN_HASH] = prefs.pinHash
      store[Keys.LOCK_JOURNAL_ONLY] = prefs.lockJournalOnly
    }
  }
}
