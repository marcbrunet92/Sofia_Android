package com.lemarc.sofia.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.lemarc.sofia.TEST_MODE_KEY
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "sofia_settings")

class SettingsRepository(
    private val context: Context,
) {
    val testMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[TEST_MODE_KEY] ?: false
    }

    suspend fun setTestMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[TEST_MODE_KEY] = enabled
        }
    }
}
